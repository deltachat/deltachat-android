package org.thoughtcrime.securesms;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

public class TransportOption {

  public enum Type {
    NORMAL_MAIL
  }

  private final int                             drawable;
  private final int                             backgroundColor;
  private final @NonNull String                 text;
  private final @NonNull Type                   type;
  private final @NonNull String                 composeHint;

  public TransportOption(@NonNull  Type type,
                         @DrawableRes int drawable,
                         int backgroundColor,
                         @NonNull String text,
                         @NonNull String composeHint)
  {
    this.type                = type;
    this.drawable            = drawable;
    this.backgroundColor     = backgroundColor;
    this.text                = text;
    this.composeHint         = composeHint;
  }

  public @NonNull Type getType() {
    return type;
  }

  public boolean isType(Type type) {
    return this.type == type;
  }

  public @DrawableRes int getDrawable() {
    return drawable;
  }

  public int getBackgroundColor() {
    return backgroundColor;
  }

  public @NonNull String getComposeHint() {
    return composeHint;
  }

  public @NonNull String getDescription() {
    return text;
  }
}
