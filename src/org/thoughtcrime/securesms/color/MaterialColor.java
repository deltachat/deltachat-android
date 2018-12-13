package org.thoughtcrime.securesms.color;

import android.content.Context;
import android.support.annotation.NonNull;

import org.thoughtcrime.securesms.R;

import static org.thoughtcrime.securesms.util.ThemeUtil.isDarkTheme;

public enum MaterialColor {
  RED        (R.color.conversation_red,    R.color.conversation_red_shade,    "red"),
  PINK       (R.color.conversation_pink,   R.color.conversation_pink_shade,   "pink"),
  PURPLE     (R.color.conversation_purple, R.color.conversation_purple_shade, "purple"),
  INDIGO     (R.color.conversation_indigo, R.color.conversation_indigo_shade, "indigo"),
  BLUE       (R.color.conversation_blue,   R.color.conversation_blue_shade,   "blue"),
  CYAN       (R.color.conversation_cyan,   R.color.conversation_cyan_shade,   "cyan"),
  TEAL       (R.color.conversation_teal,   R.color.conversation_teal_shade,   "teal"),
  GREEN      (R.color.conversation_green,  R.color.conversation_green_shade,  "green"),
  ORANGE     (R.color.conversation_orange, R.color.conversation_orange_shade, "orange"),
  GREY       (R.color.conversation_grey,   R.color.conversation_grey_shade,   "grey");

  private final int conversationColorLight;
  private final int actionBarColorLight;
  private final int statusBarColorLight;
  private final int conversationColorDark;
  private final int actionBarColorDark;
  private final int statusBarColorDark;
  private final String serialized;

  MaterialColor(int conversationColorLight, int actionBarColorLight,
                int statusBarColorLight, int conversationColorDark,
                int actionBarColorDark, int statusBarColorDark,
                String serialized)
  {
    this.conversationColorLight = conversationColorLight;
    this.actionBarColorLight    = actionBarColorLight;
    this.statusBarColorLight    = statusBarColorLight;
    this.conversationColorDark  = conversationColorDark;
    this.actionBarColorDark     = actionBarColorDark;
    this.statusBarColorDark     = statusBarColorDark;
    this.serialized             = serialized;
  }

  MaterialColor(int conversationColor, int statusBarColor, String serialized)
  {
    this(conversationColor, conversationColor, statusBarColor,
         conversationColor, conversationColor, statusBarColor, serialized);
  }

  public int toConversationColor(@NonNull Context context) {
    return context.getResources().getColor(isDarkTheme(context) ? conversationColorDark
                                                                : conversationColorLight);
  }

  public boolean represents(Context context, int colorValue) {
    return context.getResources().getColor(conversationColorDark)  == colorValue ||
           context.getResources().getColor(conversationColorLight) == colorValue ||
           context.getResources().getColor(actionBarColorDark) == colorValue ||
           context.getResources().getColor(actionBarColorLight) == colorValue ||
           context.getResources().getColor(statusBarColorLight) == colorValue ||
           context.getResources().getColor(statusBarColorDark) == colorValue;
  }

  public String serialize() {
    return serialized;
  }

}
