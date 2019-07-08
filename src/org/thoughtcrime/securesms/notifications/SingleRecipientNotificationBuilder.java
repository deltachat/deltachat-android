package org.thoughtcrime.securesms.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.RemoteInput;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.contacts.avatars.ContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.FallbackContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.GeneratedContactPhoto;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.preferences.widgets.NotificationPrivacyPreference;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.BitmapUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.ThemeUtil;
import org.thoughtcrime.securesms.util.Util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SingleRecipientNotificationBuilder extends AbstractNotificationBuilder {

  private static final String TAG = SingleRecipientNotificationBuilder.class.getSimpleName();

  private final List<CharSequence> messageBodies = new LinkedList<>();

  private SlideDeck    slideDeck;
  private CharSequence contentTitle;
  private CharSequence contentText;

  SingleRecipientNotificationBuilder(@NonNull Context context, @NonNull NotificationPrivacyPreference privacy)
  {
    super(context, privacy);

    setSmallIcon(R.drawable.icon_notification);
    setColor(context.getResources().getColor(R.color.delta_primary));
    setPriority(Prefs.getNotificationPriority(context));
    setCategory(NotificationCompat.CATEGORY_MESSAGE);
  }

  public void setChat(@NonNull Recipient recipient) {
    if (privacy.isDisplayContact()) {
      setContentTitle(recipient.toShortString());

      if (recipient.getContactUri() != null) {
        addPerson(recipient.getContactUri().toString());
      }

      ContactPhoto         contactPhoto         = recipient.getContactPhoto(context);
      FallbackContactPhoto fallbackContactPhoto = recipient.getFallbackContactPhoto();

      if (contactPhoto != null) {
        try {
          setLargeIcon(GlideApp.with(context.getApplicationContext())
                               .load(contactPhoto)
                               .diskCacheStrategy(DiskCacheStrategy.NONE)
                               .circleCrop()
                               .submit(context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                       context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height))
                               .get());
        } catch (Exception e) {
          Log.w(TAG, e);
          setLargeIcon(fallbackContactPhoto.asDrawable(context, recipient.getFallbackAvatarColor(context)));
        }
      } else {
        setLargeIcon(fallbackContactPhoto.asDrawable(context, recipient.getFallbackAvatarColor(context)));
      }

    } else {
      setContentTitle(context.getString(R.string.app_name));
      setLargeIcon(new GeneratedContactPhoto("Unknown").asDrawable(context, ThemeUtil.getDummyContactColor(context)));
    }
  }

  public void setMessageCount(int messageCount) {
    setContentInfo(String.valueOf(messageCount));
    setNumber(messageCount);
  }

  public void setPrimaryMessageBody(@NonNull  Recipient chatRecipients,
                                    @NonNull  Recipient individualRecipient,
                                    @NonNull  CharSequence message,
                                    @Nullable SlideDeck slideDeck)
  {
    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

    if (privacy.isDisplayContact() && chatRecipients.isGroupRecipient()) {
      stringBuilder.append(Util.getBoldedString(individualRecipient.toShortString() + ": "));
    }

    if (privacy.isDisplayMessage()) {
      setContentText(stringBuilder.append(message));
      this.slideDeck = slideDeck;
    } else {
      setContentText(stringBuilder.append(context.getString(R.string.notify_new_message)));
    }
  }

  void addActions(@NonNull PendingIntent markReadIntent,
                         @NonNull PendingIntent inNotificationReplyIntent)
  {
    Action markAsReadAction = new Action(R.drawable.check,
                                         context.getString(R.string.notify_mark_read),
                                         markReadIntent);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Action replyAction = new Action.Builder(R.drawable.ic_reply_white_36dp,
              context.getString(R.string.notify_reply_button),
              inNotificationReplyIntent)
              .addRemoteInput(new RemoteInput.Builder(MessageNotifierCompat.EXTRA_REMOTE_REPLY)
                      .setLabel(context.getString(R.string.notify_reply_button)).build())
              .build();
      addAction(replyAction);
    }

    Action wearableReplyAction = new Action.Builder(R.drawable.ic_reply,
                                                    context.getString(R.string.notify_reply_button),
                                                    inNotificationReplyIntent)
        .addRemoteInput(new RemoteInput.Builder(MessageNotifierCompat.EXTRA_REMOTE_REPLY)
                            .setLabel(context.getString(R.string.notify_reply_button)).build())
        .build();

    addAction(markAsReadAction);

    extend(new NotificationCompat.WearableExtender().addAction(markAsReadAction)
                                                    .addAction(wearableReplyAction));
  }

  void addMessageBody(@NonNull Recipient chatRecipient,
                             @NonNull Recipient individualRecipient,
                             @Nullable CharSequence messageBody)
  {
    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

    if (privacy.isDisplayContact() && chatRecipient.isGroupRecipient()) {
      stringBuilder.append(Util.getBoldedString(individualRecipient.toShortString() + ": "));
    }

    if (privacy.isDisplayMessage()) {
      messageBodies.add(stringBuilder.append(messageBody == null ? "" : messageBody));
    } else {
      messageBodies.add(stringBuilder.append(context.getString(R.string.notify_new_message)));
    }
  }

  @Override
  public Notification build() {
    // the filtering whether or not to display messages and contacts is done in addMessageBody
    // and setPrimaryMessageBody, no need to do it here again.
    NotificationCompat.Style style;
    if (Build.VERSION.SDK_INT < 23) {
      style = new NotificationCompat.InboxStyle();
      for (CharSequence messageBody : messageBodies) {
        ((NotificationCompat.InboxStyle) style).addLine(messageBody);
      }
    } else if (messageBodies.size() == 1 && hasBigPictureSlide(slideDeck)) {
      style = new NotificationCompat.BigPictureStyle()
                .bigPicture(getBigPicture(slideDeck))
                .setSummaryText(getBigText(messageBodies));
    } else {
      style = new NotificationCompat.BigTextStyle().bigText(getBigText(messageBodies));
    }

    setStyle(style);
    return super.build();
  }

  private void setLargeIcon(@Nullable Drawable drawable) {
    if (drawable != null) {
      int    largeIconTargetSize  = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);
      Bitmap recipientPhotoBitmap = BitmapUtil.createFromDrawable(drawable, largeIconTargetSize, largeIconTargetSize);

      if (recipientPhotoBitmap != null) {
        setLargeIcon(recipientPhotoBitmap);
      }
    }
  }

  private boolean hasBigPictureSlide(@Nullable SlideDeck slideDeck) {
    if (slideDeck == null || Build.VERSION.SDK_INT < 16) {
      return false;
    }

    Slide thumbnailSlide = slideDeck.getThumbnailSlide();

    return thumbnailSlide != null         &&
           thumbnailSlide.hasImage()      &&
           thumbnailSlide.getThumbnailUri() != null;
  }

  private Bitmap getBigPicture(@NonNull SlideDeck slideDeck)
  {
    try {
      @SuppressWarnings("ConstantConditions")
      Uri uri = slideDeck.getThumbnailSlide().getThumbnailUri();

      return GlideApp.with(context.getApplicationContext())
                     .asBitmap()
                     .load(new DecryptableStreamUriLoader.DecryptableUri(uri))
                     .diskCacheStrategy(DiskCacheStrategy.NONE)
                     .submit(500, 500)
                     .get();
    } catch (InterruptedException | ExecutionException e) {
      Log.w(TAG, e);
      return Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565);
    }
  }

  @Override
  public NotificationCompat.Builder setContentTitle(CharSequence contentTitle) {
    this.contentTitle = contentTitle;
    return super.setContentTitle(contentTitle);
  }

  public NotificationCompat.Builder setContentText(CharSequence contentText) {
    this.contentText = contentText;
    return super.setContentText(contentText);
  }

  private CharSequence getBigText(List<CharSequence> messageBodies) {
    SpannableStringBuilder content = new SpannableStringBuilder();

    for (CharSequence message : messageBodies) {
      content.append(message);
      content.append('\n');
    }

    return content;
  }


}
