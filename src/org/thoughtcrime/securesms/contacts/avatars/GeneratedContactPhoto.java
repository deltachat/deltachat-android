package org.thoughtcrime.securesms.contacts.avatars;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.content.res.AppCompatResources;

import com.amulyakhare.textdrawable.TextDrawable;

import org.thoughtcrime.securesms.R;

import java.util.regex.Pattern;

public class GeneratedContactPhoto implements FallbackContactPhoto {

  private static final Pattern PATTERN = Pattern.compile("[^\\p{L}\\p{Nd}\\p{P}\\p{S}]+");

  private final String name;

  public GeneratedContactPhoto(@NonNull String name) {
    this.name  = name;
  }

  @Override
  public Drawable asDrawable(Context context, int color) {
    int targetSize = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);

    return TextDrawable.builder()
                       .beginConfig()
                       .width(targetSize)
                       .height(targetSize)
                       .textColor(Color.WHITE)
                       .bold()
                       .toUpperCase()
                       .endConfig()
                       .buildRound(getCharacter(name), color);
  }

  private String getCharacter(String name) {
    String cleanedName = PATTERN.matcher(name).replaceFirst("");

    if (cleanedName.isEmpty()) {
      return "#";
    } else {
      return new StringBuilder().appendCodePoint(cleanedName.codePointAt(0)).toString();
    }
  }

  @Override
  public Drawable asCallCard(Context context) {
    return AppCompatResources.getDrawable(context, R.drawable.ic_person_large);

  }
}
