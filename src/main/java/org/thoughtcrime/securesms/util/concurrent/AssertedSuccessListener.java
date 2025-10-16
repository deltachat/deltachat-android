package org.thoughtcrime.securesms.util.concurrent;

import java.util.concurrent.ExecutionException;

import chat.delta.util.ListenableFuture;

public abstract class AssertedSuccessListener<T> implements ListenableFuture.Listener<T> {
  @Override
  public void onFailure(ExecutionException e) {
    throw new AssertionError(e);
  }
}
