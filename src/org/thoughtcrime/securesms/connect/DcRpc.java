package org.thoughtcrime.securesms.connect;

import android.util.Log;
import android.util.JsonWriter;

import com.b44t.messenger.DcJsonrpcInstance;

import org.json.JSONObject;
import org.json.JSONException;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;
import org.thoughtcrime.securesms.util.JsonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class DcRpc {
    private static final String TAG = DcRpc.class.getSimpleName();

    private Map<Integer, SettableFuture<Object>> requestFutures = new HashMap<>();
    private DcJsonrpcInstance dcJsonrpcInstance;
    private int requestId = 0;

    public DcRpc(DcJsonrpcInstance dcJsonrpcInstance) {
        this.dcJsonrpcInstance = dcJsonrpcInstance;
    }

    private void processResponse() throws JSONException {
        JSONObject response = new JSONObject(dcJsonrpcInstance.getNextResponse());
        int responseId = response.optInt("id", 0);

        if (responseId == 0) {
            Log.i(TAG, "Got JSON-RPC notification: " + response.toString());
            return;
        }

        SettableFuture<Object> future = requestFutures.remove(responseId);
        Object error = response.opt("error");
        Object result = response.opt("result");
        if (error != null) {
            future.setException(new Throwable(error.toString()));
        } else if (result != null) {
            future.set(result);
        } else {
            future.setException(new Throwable("Got JSON-RPC response witout result or error: " + response.toString()));
        }
    }

    public void start() {
        new Thread(() -> {
                while (true) {
                    try {
                        processResponse();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
        }, "jsonrpcThread").start();
    }

    public SettableFuture<Object> call(String method, Object... params) {
        requestId++;
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("params", params);
        request.put("id", requestId);
        SettableFuture<Object> future = new SettableFuture<>();
        try {
            String requestStr = JsonUtils.toJson(request);
            Log.i(TAG, "Sending request: " + requestStr);
            requestFutures.put(requestId, future);
            dcJsonrpcInstance.request(requestStr);
        } catch (IOException e) {
            e.printStackTrace();
            future.setException(e);
        }
        return future;
    }
}
