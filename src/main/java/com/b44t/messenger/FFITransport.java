package com.b44t.messenger;

import chat.delta.rpc.BaseTransport;

/* RPC transport over C FFI */
public class FFITransport extends BaseTransport {
  private final DcJsonrpcInstance dcJsonrpcInstance;

  public FFITransport(DcJsonrpcInstance dcJsonrpcInstance) {
    this.dcJsonrpcInstance = dcJsonrpcInstance;
  }

  @Override
  protected void sendRequest(String jsonRequest) {
    dcJsonrpcInstance.request(jsonRequest);
  }

  @Override
  protected String getResponse() {
    return dcJsonrpcInstance.getNextResponse();
  }
}