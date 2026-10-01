package org.thoughtcrime.securesms.connect;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import org.thoughtcrime.securesms.search.QrInviteData;
import org.thoughtcrime.securesms.util.AsyncLoader;
import org.thoughtcrime.securesms.util.Util;

public class DcContactsLoader extends AsyncLoader<DcContactsLoader.Ret> {

  private final int listflags;
  private final @Nullable String query;
  private final boolean detectInviteLink;
  private final boolean addScanQRLink;
  private final boolean addCreateGroupLinks;
  private final boolean addCreateContactLink;
  private final boolean blockedContacts;

  public DcContactsLoader(
      Context context,
      int listflags,
      String query,
      boolean detectInviteLink,
      boolean addCreateGroupLinks,
      boolean addCreateContactLink,
      boolean addScanQRLink,
      boolean blockedContacts) {
    super(context);
    this.listflags = listflags;
    this.query = (query == null || query.isEmpty()) ? null : query;
    this.detectInviteLink = detectInviteLink;
    this.addScanQRLink = addScanQRLink;
    this.addCreateGroupLinks = addCreateGroupLinks;
    this.addCreateContactLink = addCreateContactLink;
    this.blockedContacts = blockedContacts;
  }

  @Override
  public @NonNull DcContactsLoader.Ret loadInBackground() {
    DcContext dcContext = DcHelper.getContext(getContext());
    if (blockedContacts) {
      int[] blocked_ids = dcContext.getBlockedContacts();
      return new Ret(blocked_ids, null);
    }

    QrInviteData qrInviteData = null;
    if (detectInviteLink && query != null && query.contains(":")) {
      DcLot qrParsed = dcContext.checkQr(query);
      qrInviteData = QrInviteData.from(getContext(), dcContext, qrParsed, query);
    }

    int[] contact_ids = dcContext.getContacts(listflags, query);
    int[] additional_items = new int[0];
    if (qrInviteData != null) {
      additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_INVITE_LINK);
    }
    if (query == null && addScanQRLink) {
      additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_QR_INVITE);
    }
    if (addCreateContactLink && dcContext.getConfigInt(DcHelper.CONFIG_FORCE_ENCRYPTION) == 0) {
      additional_items =
          Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_NEW_CLASSIC_CONTACT);
    }
    if (query == null && addCreateGroupLinks) {
      additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_NEW_GROUP);
      additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_NEW_BROADCAST);

      if (dcContext.getConfigInt(DcHelper.CONFIG_FORCE_ENCRYPTION) == 0) {
        additional_items =
            Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_NEW_UNENCRYPTED_GROUP);
      }
    }
    int[] all_ids = new int[contact_ids.length + additional_items.length];
    System.arraycopy(additional_items, 0, all_ids, 0, additional_items.length);
    System.arraycopy(contact_ids, 0, all_ids, additional_items.length, contact_ids.length);
    return new Ret(all_ids, qrInviteData);
  }

  public static class Ret {
    public final int[] ids;
    public final QrInviteData qrInviteData;

    Ret(int[] ids, QrInviteData qrInviteData) {
      this.ids = ids;
      this.qrInviteData = qrInviteData;
    }
  }
}
