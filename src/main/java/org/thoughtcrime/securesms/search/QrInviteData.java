package org.thoughtcrime.securesms.search;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import org.thoughtcrime.securesms.R;

public class QrInviteData {

  private final String displayTitle;
  private final String displaySubtitle;
  private final String rawQrString;
  private final int contactId;

  private QrInviteData(
      @NonNull String displayTitle,
      @NonNull String displaySubtitle,
      @NonNull String rawQrString,
      int contactId) {
    this.displayTitle = displayTitle;
    this.displaySubtitle = displaySubtitle;
    this.rawQrString = rawQrString;
    this.contactId = contactId;
  }

  @Nullable
  public static QrInviteData from(
      @NonNull Context context,
      @NonNull DcContext dcContext,
      @NonNull DcLot qrParsed,
      @NonNull String rawQrString) {
    int state = qrParsed.getState();
    String title;
    String subtitle;
    int contactId = 0;

    switch (state) {
      case DcContext.DC_QR_ASK_VERIFYCONTACT:
      case DcContext.DC_QR_FPR_OK:
      case DcContext.DC_QR_ADDR:
        contactId = qrParsed.getId();
        DcContact contact = dcContext.getContact(contactId);
        title = contact.getDisplayName();
        subtitle = context.getString(R.string.start_chat);
        break;
      case DcContext.DC_QR_ASK_VERIFYGROUP:
        title = qrParsed.getText1();
        subtitle = context.getString(R.string.join_group);
        break;
      case DcContext.DC_QR_ASK_JOIN_BROADCAST:
        title = qrParsed.getText1();
        subtitle = context.getString(R.string.join_channel);
        break;
      case DcContext.DC_QR_ACCOUNT:
      case DcContext.DC_QR_LOGIN:
        title = qrParsed.getText1();
        subtitle = context.getString(R.string.add_transport);
        break;
      case DcContext.DC_QR_PROXY:
        title = qrParsed.getText1();
        subtitle = context.getString(R.string.proxy_use_proxy);
        break;
      default:
        return null;
    }

    return new QrInviteData(title, subtitle, rawQrString, contactId);
  }

  @NonNull
  public String getDisplayTitle() {
    return displayTitle;
  }

  @NonNull
  public String getDisplaySubtitle() {
    return displaySubtitle;
  }

  @NonNull
  public String getRawQrString() {
    return rawQrString;
  }

  public int getContactId() {
    return contactId;
  }
}
