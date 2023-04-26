package com.b44t.messenger.rpc;

/**
 * An exception occurred while processing a request.
 **/
public class RpcError extends Throwable {

    public RpcError(String message) {
        super(message);
    }
}
