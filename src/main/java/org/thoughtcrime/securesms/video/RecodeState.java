package org.thoughtcrime.securesms.video;

import com.google.common.collect.ImmutableSet;

public final class RecodeState {

  public enum Phase {
    IDLE,
    INGEST,
    RECODING
  }

  public static final RecodeState IDLE = new RecodeState(Phase.IDLE, 0, ImmutableSet.of(), 0, 0, 0);

  public final Phase phase;
  public final int accId;
  public final ImmutableSet<Integer> chatIds;
  public final int done;
  public final int total;
  public final int progress;

  RecodeState(
      Phase phase, int accId, ImmutableSet<Integer> chatIds, int done, int total, int progress) {
    this.phase = phase;
    this.accId = accId;
    this.chatIds = chatIds;
    this.done = done;
    this.total = total;
    this.progress = progress;
  }

  public boolean isIdle() {
    return phase == Phase.IDLE;
  }

  public boolean sendsTo(int accId, int chatId) {
    return !isIdle() && this.accId == accId && chatIds.contains(chatId);
  }

  public int position() {
    return Math.min(done + 1, total);
  }
}
