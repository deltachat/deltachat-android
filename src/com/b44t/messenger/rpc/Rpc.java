package com.b44t.messenger.rpc;

import com.b44t.messenger.DcJsonrpcInstance;
import com.b44t.messenger.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class Rpc {
    private final Map<Integer, SettableFuture<JsonElement>> requestFutures = new ConcurrentHashMap<>();
    private final DcJsonrpcInstance dcJsonrpcInstance;
    private int requestId = 0;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public Rpc(DcJsonrpcInstance dcJsonrpcInstance) {
        this.dcJsonrpcInstance = dcJsonrpcInstance;
    }

    private void processResponse() throws JsonSyntaxException {
        String jsonResponse = dcJsonrpcInstance.getNextResponse();
        Response response = gson.fromJson(jsonResponse, Response.class);

        if (response.id == 0) { // Got JSON-RPC notification/event, ignore
            return;
        }

        SettableFuture<JsonElement> future = requestFutures.remove(response.id);
        if (future == null) { // Got a response with unknown ID, ignore
            return;
        }

        if (response.error != null) {
            future.setException(new RpcException(response.error.toString()));
        } else if (response.result != null) {
            future.set(response.result);
        } else {
            future.setException(new RpcException("Got JSON-RPC response witout result or error: " + jsonResponse));
        }
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    processResponse();
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }, "jsonrpcThread").start();
    }

    public SettableFuture<JsonElement> call(String method, Object... params) {
        int id;
        synchronized (this) {
            id = ++requestId;
        }
        String jsonRequest = gson.toJson(new Request(method, params, id));
        SettableFuture<JsonElement> future = new SettableFuture<>();
        requestFutures.put(id, future);
        dcJsonrpcInstance.request(jsonRequest);
        return future;
    }

    public JsonElement getResult(String method, Object... params) throws RpcException {
        try {
            return call(method, params).get();
        } catch (ExecutionException e) {
            throw (RpcException)e.getCause();
        } catch (InterruptedException e) {
            throw new RpcException(e.getMessage());
        }
    }

    public int addAccount() throws RpcException {
        return gson.fromJson(getResult("add_account"), int.class);
    }

    public void startIO() throws RpcException {
        getResult("start_io_for_all_accounts");
    }

    public void stopIO() throws RpcException {
        getResult("stop_io_for_all_accounts");
    }

    public Map<String, String> getSystemInfo() throws RpcException {
        TypeToken<Map<String, String>> mapType = new TypeToken<Map<String, String>>(){};
        return gson.fromJson(getResult("get_system_info"), mapType);
    }


    private static class Request {
        public String jsonrpc = "2.0";
        public String method;
        public Object[] params;
        public int id;

        public Request(String method, Object[] params, int id) {
            this.method = method;
            this.params = params;
            this.id = id;
        }
    }

    private static class Response {
        public int id = 0;
        public JsonElement result;
        public JsonElement error;

        public Response(int id, JsonElement result, JsonElement error) {
            this.id = id;
            this.result = result;
            this.error = error;
        }
    }
}
