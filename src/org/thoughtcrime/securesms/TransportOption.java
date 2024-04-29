package org.thoughtcrime.securesms;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class TransportOption {

  public enum Type {
    NORMAL_MAIL
  }

  private final int                             drawable;
  private final @NonNull String                 text;
  private final @NonNull String                 composeHint;

  public TransportOption(@NonNull  Type type,
                         @DrawableRes int drawable,
                         @NonNull String text,
                         @NonNull String composeHint)
  {
    this.drawable            = drawable;
    this.text                = text;
    this.composeHint         = composeHint;
  }

  public @NonNull Type getType() {
    return Type.NORMAL_MAIL;
  }

  public @DrawableRes int getDrawable() {
    return drawable;
  }

  public @NonNull String getComposeHint() {
    return composeHint;
  }

  public @NonNull String getDescription() {
    return text;
  }
}
