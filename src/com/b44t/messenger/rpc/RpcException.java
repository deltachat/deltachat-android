package com.b44t.messenger.rpc;

/**
 * An exception occurred while processing a request in Delta Chat core.
 **/
public class RpcException extends Exception {

    public RpcException(String message) {
        super(message);
    }
}
