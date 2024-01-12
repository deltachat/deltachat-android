package org.thoughtcrime.securesms.connect;

import android.content.Context;
import androidx.annotation.NonNull;

import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.util.AsyncLoader;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

public class DcContactsLoader extends AsyncLoader<DcContactsLoader.Ret> {

    private static final String TAG = DcContactsLoader.class.getName();

    private final int     listflags;
    private final String  query;
    private final boolean addScanQRLink;
    private final boolean addCreateGroupLinks;
    private final boolean addCreateContactLink;
    private final boolean blockedContacts;

    public DcContactsLoader(Context context, int listflags, String query, boolean addCreateGroupLinks, boolean addCreateContactLink, boolean addScanQRLink, boolean blockedContacts) {
        super(context);
        this.listflags           = listflags;
        this.query               = (query==null||query.isEmpty())? null : query;
        this.addScanQRLink       = addScanQRLink;
        this.addCreateGroupLinks = addCreateGroupLinks;
        this.addCreateContactLink= addCreateContactLink;
        this.blockedContacts     = blockedContacts;
    }

    @Override
    public @NonNull
    DcContactsLoader.Ret loadInBackground() {
        DcContext dcContext = DcHelper.getContext(getContext());
        if (blockedContacts) {
            int[] blocked_ids = dcContext.getBlockedContacts();
            return new DcContactsLoader.Ret(blocked_ids, query);
        }

        int[] contact_ids = dcContext.getContacts(listflags, query);
        int[] additional_items = new int[0];
        if (query == null && addScanQRLink)
        {
          additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_QR_INVITE);
        }
        if (query == null && addCreateGroupLinks) {
            additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_NEW_GROUP);
            final boolean broadcastsEnabled = Prefs.isNewBroadcastListAvailable(getContext());
            if (broadcastsEnabled) additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_NEW_BROADCAST_LIST);
        }
        if (addCreateContactLink)
        {
            additional_items = Util.appendInt(additional_items, DcContact.DC_CONTACT_ID_NEW_CONTACT);
        }
        int all_ids[] = new int[contact_ids.length + additional_items.length];
        System.arraycopy(additional_items, 0, all_ids, 0, additional_items.length);
        System.arraycopy(contact_ids, 0, all_ids, additional_items.length, contact_ids.length);
        return new DcContactsLoader.Ret(all_ids, query);
    }

    public class Ret {
        public final int[]  ids;
        public final String query;

        Ret(int[] ids, String query) {
            this.ids   = ids;
            this.query = query;
        }
    }
}
