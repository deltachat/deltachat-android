package org.thoughtcrime.securesms.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.recipients.Recipient;

public class CommunicationActions {

  public static void startVoiceCall(@NonNull Activity activity, @NonNull Recipient recipient) {
  }

  public static void startConversation(@NonNull  Context   context,
                                       @NonNull  Recipient recipient,
                                       @Nullable String    text)
  {
    new AsyncTask<Void, Void, Integer>() {
      @Override
      protected Integer doInBackground(Void... voids) {
        return recipient.getAddress().getDcChatId();
      }

      @Override
      protected void onPostExecute(Integer threadId) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);

        if (!TextUtils.isEmpty(text)) {
          intent.putExtra(ConversationActivity.TEXT_EXTRA, text);
        }

        context.startActivity(intent);
      }
    }.execute();
  }

  public static void composeSmsThroughDefaultApp(@NonNull Context context, @NonNull Address address, @Nullable String text) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + address.serialize()));
    if (text != null) {
      intent.putExtra("sms_body", text);
    }
    context.startActivity(intent);
  }
}
