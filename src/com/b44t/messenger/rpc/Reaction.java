package com.b44t.messenger.rpc;

import androidx.annotation.Nullable;

public class Reaction {
    // The reaction emoji string.
    private final String emoji;
    // The count of users that have reacted with this reaction.
    private final int count;
    // true if self-account reacted with this reaction, false otherwise.
    private final boolean isFromSelf;

    public Reaction(String emoji, int count, boolean isFromSelf) {
        this.emoji = emoji;
        this.count = count;
        this.isFromSelf = isFromSelf;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getCount() {
        return count;
    }

    public boolean isFromSelf() {
        return isFromSelf;
    }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof Reaction) {
      Reaction reaction = (Reaction) obj;
      return emoji.equals(reaction.getEmoji()) && count == reaction.getCount() && isFromSelf == reaction.isFromSelf();
    }
    return false;
  }
}
