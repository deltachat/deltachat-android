package org.thoughtcrime.securesms.videochat;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Collections;

@RequiresApi(api = Build.VERSION_CODES.M)
public class CallIntegrationService extends ConnectionService {
  private final static String TAG = CallIntegrationService.class.getSimpleName();

  private static final String EXTRA_ACCOUNT_ID = "accid";
  private static final String EXTRA_CHAT_ID = "chatid";
  private static final String EXTRA_CALL_ID = "callid";
  private static final String EXTRA_CALL_PAYLOAD = "callpayload";

  @Override
  public Connection onCreateOutgoingConnection(final PhoneAccountHandle phoneAccountHandle, final ConnectionRequest request) {
    Log.d(TAG, "onCreateOutgoingConnection(" + phoneAccountHandle.getId() + ", " + request.getAddress() + ")");
    final Uri uri = request.getAddress();
    final Bundle extras = request.getExtras();
    if (uri == null || !Arrays.asList("dc", "tel").contains(uri.getScheme())) {
      return Connection.createFailedConnection(new DisconnectCause(DisconnectCause.ERROR, "invalid address"));
    }
    final int chatId = extras.getInt(EXTRA_CHAT_ID);
    return new Connection() {
      @Override
      public void onAnswer() {
        super.onAnswer();
      }
    };
  }

  private static PhoneAccountHandle getHandle(final Context context, int accountId) {
    final ComponentName componentName = new ComponentName(context, CallIntegrationService.class);
    return new PhoneAccountHandle(componentName, accountId+"");
  }

  public static void registerPhoneAccount(final Context context, final int accountId) {
    final PhoneAccountHandle handle = getHandle(context, accountId);
    final TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (telecomManager.getOwnSelfManagedPhoneAccounts().contains(handle)) {
        Log.d(TAG, "a phone account for " + accountId + " already exists");
        return;
      }
    }
    final PhoneAccount.Builder builder =
      PhoneAccount.builder(getHandle(context, accountId), accountId+"");
    builder.setSupportedUriSchemes(Collections.singletonList("dc"));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setCapabilities(
                              PhoneAccount.CAPABILITY_SELF_MANAGED
                              | PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING);
    }
    final PhoneAccount phoneAccount = builder.build();
    telecomManager.registerPhoneAccount(phoneAccount);
  }

  public static void placeCall(Context context, int accountId, int chatId) {
    final Bundle extras = new Bundle();
    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, getHandle(context, accountId));
    final Bundle outgoingCallExtras = new Bundle();
    outgoingCallExtras.putInt(EXTRA_ACCOUNT_ID, accountId);
    outgoingCallExtras.putInt(EXTRA_CHAT_ID, chatId);
    extras.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, outgoingCallExtras);

    final Uri address = Uri.parse("tel:0");
    try {
      context.getSystemService(TelecomManager.class).placeCall(address, extras);
    } catch (final SecurityException e) {
      Log.e(TAG, "call integration not available", e);
      Toast.makeText(context, "call integration not available", Toast.LENGTH_LONG).show();
    }
  }

  public static void addNewIncomingCall(final Context context, int accId, int chatId, int callId, String payload) {
    final PhoneAccountHandle phoneAccountHandle = getHandle(context, accId);
    final Bundle bundle = new Bundle();
    bundle.putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, chatId+"");
    final Bundle extras = new Bundle();
    extras.putInt(EXTRA_CALL_ID, callId);
    extras.putString(EXTRA_CALL_PAYLOAD, payload);
    bundle.putBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, extras);

    try {
      context.getSystemService(TelecomManager.class).addNewIncomingCall(phoneAccountHandle, bundle);
    } catch (final SecurityException e) {
      Log.e(TAG, "call integration not available", e);
      Toast.makeText(context, "call integration not available", Toast.LENGTH_LONG).show();
    }
  }

}
