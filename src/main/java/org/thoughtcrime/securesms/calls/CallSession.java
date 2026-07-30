package org.thoughtcrime.securesms.calls;

import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.telecom.CallControlScope;
import androidx.core.telecom.CallEndpointCompat;

@RequiresApi(api = Build.VERSION_CODES.O)
final class CallSession {
  final int accId;
  Integer callId;
  Integer chatId;
  final boolean isIncoming;
  boolean startsWithVideo;
  String offerSdp;
  boolean answerInProgress;
  boolean hasNotifiedBackend;
  volatile String cachedIceServersJson;

  CallControlScope callControlScope;
  CallEndpointCompat preferredStartingEndpoint;

  CallSession(int accId, Integer callId, boolean isIncoming) {
    this.accId = accId;
    this.callId = callId;
    this.isIncoming = isIncoming;
  }

  boolean matches(int accId, int callId) {
    return this.accId == accId && this.callId != null && this.callId == callId;
  }
}
