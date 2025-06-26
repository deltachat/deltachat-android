package com.b44t.messenger.rpc;

import android.util.Log;

import com.b44t.messenger.DcJsonrpcInstance;
import com.b44t.messenger.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class Rpc {
    private final static String TAG = Rpc.class.getSimpleName();

    private final Map<Integer, SettableFuture<JsonElement>> requestFutures = new ConcurrentHashMap<>();
    private final DcJsonrpcInstance dcJsonrpcInstance;
    private int requestId = 0;
    private boolean started = false;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public Rpc(DcJsonrpcInstance dcJsonrpcInstance) {
        this.dcJsonrpcInstance = dcJsonrpcInstance;
    }

    private void processResponse() throws JsonSyntaxException {
        String jsonResponse = dcJsonrpcInstance.getNextResponse();

        Response response = gson.fromJson(jsonResponse, Response.class);
        if (response == null) {
            Log.e(TAG, "Error parsing JSON: " + jsonResponse);
            return;
        } else if (response.id == 0) {
            // Got JSON-RPC notification/event, ignore
            return;
        }

        SettableFuture<JsonElement> future = requestFutures.remove(response.id);
        if (future == null) { // Got a response with unknown ID, ignore
            return;
        }

        if (response.error != null) {
            String message;
            try {
                message = response.error.getAsJsonObject().get("message").getAsString();
            } catch (Exception e) {
                Log.e(TAG, "Can't get response error message: " + e);
                message = response.error.toString();
            }
            future.setException(new RpcException(message));
        } else if (response.result != null) {
            future.set(response.result);
        } else {
            future.setException(new RpcException("Got JSON-RPC response without result or error: " + jsonResponse));
        }
    }

    public void start() {
        started = true;
        new Thread(() -> {
            while (true) {
                try {
                    processResponse();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "jsonrpcThread").start();
    }

    public SettableFuture<JsonElement> call(String method, Object... params) throws RpcException {
        if (!started) throw new RpcException("RPC not started yet.");

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

    public List<VcardContact> parseVcard(String path) throws RpcException {
        TypeToken<List<VcardContact>> listType = new TypeToken<List<VcardContact>>(){};
        return gson.fromJson(getResult("parse_vcard", path), listType.getType());
    }

    public String makeVcard(int accountId, int... contacts) throws RpcException {
        return gson.fromJson(getResult("make_vcard", accountId, contacts), String.class);
    }

    public List<Integer> importVcard(int accountId, String path) throws RpcException {
        TypeToken<List<Integer>> listType = new TypeToken<List<Integer>>(){};
        return gson.fromJson(getResult("import_vcard", accountId, path), listType.getType());
    }

    public HttpResponse getHttpResponse(int accountId, String url) throws RpcException {
        return gson.fromJson(getResult("get_http_response", accountId, url), HttpResponse.class);
    }

    public Reactions getMsgReactions(int accountId, int msgId) throws RpcException {
        return gson.fromJson(getResult("get_message_reactions", accountId, msgId), Reactions.class);
    }

    public int sendReaction(int accountId, int msgId, String... reaction) throws RpcException {
        return getResult("send_reaction", accountId, msgId, reaction).getAsInt();
    }

    public int draftSelfReport(int accountId) throws RpcException {
        return getResult("draft_self_report", accountId).getAsInt();
    }

    public void sendWebxdcRealtimeData(Integer accountId, Integer instanceMsgId, List<Integer> data) throws RpcException {
        getResult("send_webxdc_realtime_data", accountId, instanceMsgId, data);
    }

    public void sendWebxdcRealtimeAdvertisement(Integer accountId, Integer instanceMsgId) throws RpcException {
        getResult("send_webxdc_realtime_advertisement", accountId, instanceMsgId);
    }

    public void leaveWebxdcRealtime(Integer accountId, Integer instanceMessageId) throws RpcException {
        getResult("leave_webxdc_realtime", accountId, instanceMessageId);
    }

    public int getAccountFileSize(int accountId) throws RpcException {
        return getResult("get_account_file_size", accountId).getAsInt();
    }

    public void changeContactName(int accountId, int contactId, String name) throws RpcException {
        getResult("change_contact_name", accountId, contactId, name);
    }

    public int addAccount() throws RpcException {
        return getResult("add_account").getAsInt();
    }

    public void addTransportFromQr(int accountId, String qrCode) throws RpcException {
        getResult("add_transport_from_qr", accountId, qrCode);
    }

    public void addOrUpdateTransport(int accountId, EnteredLoginParam param) throws RpcException {
        getResult("add_or_update_transport", accountId, param);
    }

    private static class Request {
        private final String jsonrpc = "2.0";
        public final String method;
        public final Object[] params;
        public final int id;

        public Request(String method, Object[] params, int id) {
            this.method = method;
            this.params = params;
            this.id = id;
        }
    }

    public String getMigrationError(int accountId) throws RpcException {
        return gson.fromJson(getResult("get_migration_error", accountId), String.class);
    }

    private static class Response {
        public final int id;
        public final JsonElement result;
        public final JsonElement error;

        public Response(int id, JsonElement result, JsonElement error) {
            this.id = id;
            this.result = result;
            this.error = error;
        }
    }
}
