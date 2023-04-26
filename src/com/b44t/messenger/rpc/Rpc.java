package com.b44t.messenger.rpc;

import com.b44t.messenger.DcJsonrpcInstance;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class Rpc {
    private Map<Integer, CompletableFuture<JsonElement>> requestFutures = new ConcurrentHashMap<>();
    private DcJsonrpcInstance dcJsonrpcInstance;
    private int requestId = 0;
    private Gson gson = new GsonBuilder().serializeNulls().create();

    public Rpc(DcJsonrpcInstance dcJsonrpcInstance) {
        this.dcJsonrpcInstance = dcJsonrpcInstance;
    }

    private void processResponse() throws JsonSyntaxException {
        String jsonResponse = dcJsonrpcInstance.getNextResponse();
        Response response = gson.fromJson(jsonResponse, Response.class);

        if (response.id == 0) { // Got JSON-RPC notification, ignore
            return;
        }

        CompletableFuture<JsonElement> future = requestFutures.remove(response.id);
        if (response.error != null) {
            future.completeExceptionally(new RpcError(response.error.toString()));
        } else if (response.result != null) {
            future.complete(response.result);
        } else {
            future.completeExceptionally(new RpcError("Got JSON-RPC response witout result or error: " + jsonResponse));
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

    public CompletableFuture<JsonElement> call(String method, Object... params) {
        int id;
        synchronized (this) {
            id = ++requestId;
        }
        String jsonRequest = gson.toJson(new Request(method, params, id));
        CompletableFuture<JsonElement> future = new CompletableFuture<>();
        requestFutures.put(id, future);
        dcJsonrpcInstance.request(jsonRequest);
        return future;
    }

    public JsonElement getResult(String method, Object... params) throws RpcError {
        try {
            return call(method, params).get();
        } catch (ExecutionException e) {
            throw (RpcError)e.getCause();
        } catch (InterruptedException e) {
            throw new RpcError(e.getMessage());
        }
    }

    public int addAccount() throws RpcError {
        return gson.fromJson(getResult("add_account"), int.class);
    }

    public void startIO() throws RpcError {
        getResult("start_io_for_all_accounts");
    }

    public void stopIO() throws RpcError {
        getResult("stop_io_for_all_accounts");
    }

    public Map<String, String> getSystemInfo() throws RpcError {
        TypeToken<Map<String, String>> mapType = new TypeToken<Map<String, String>>(){};
        return gson.fromJson(getResult("get_system_info"), mapType);
    }


    private class Request {
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

    private class Response {
        public int id = 0;
        public int id2 = 0;
        public JsonElement result;
        public JsonElement error;

        public Response(int id, JsonElement result, JsonElement error) {
            this.id = id;
            this.result = result;
            this.error = error;
        }
    }
}
