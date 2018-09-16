package org.thoughtcrime.securesms.connect;


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.internal.util.ArrayUtils;
import com.b44t.messenger.DcContact;
import com.b44t.messenger.DcContext;

import org.thoughtcrime.securesms.util.AsyncLoader;

public class DcContactsLoader extends AsyncLoader<DcContactsLoader.Ret> {

    private static final String TAG = DcContactsLoader.class.getName();

    private final int    listflags;
    private final String query;

    public DcContactsLoader(Context context, int listflags, String query) {
        super(context);
        this.listflags = listflags;
        this.query     = (query==null||query.isEmpty())? null : query;
    }

    @Override
    public @NonNull
    DcContactsLoader.Ret loadInBackground() {
        DcContext dcContext = DcHelper.getContext(getContext());
        int[] contact_ids = dcContext.getContacts(listflags, query);
        if(query!=null) {
            // show the "new contact" link also for partly typed e-mail addresses, so that the user knows he can continue
            if( dcContext.lookupContactIdByAddr(query)==0) {
                contact_ids = ArrayUtils.appendInt(contact_ids, DcContact.DC_CONTACT_ID_NEW_CONTACT);
            }
            return new DcContactsLoader.Ret(contact_ids, query);
        }
        else {
            // add "new group" and "new verified group" links
            final int additional_items = 2; // if someone knows an easier way to prepend sth. to int[] please pr :)
            int all_ids[] = new int[contact_ids.length+additional_items];
            all_ids[0] = DcContact.DC_CONTACT_ID_NEW_GROUP;
            all_ids[1] = DcContact.DC_CONTACT_ID_NEW_VERIFIED_GROUP;
            for(int i=0; i<contact_ids.length; i++) {
                all_ids[i+additional_items] = contact_ids[i];
            }
            return new DcContactsLoader.Ret(all_ids, query);
        }
    }

    public class Ret {
        public int[]  ids;
        public String query;

        Ret(int[] ids, String query) {
            this.ids   = ids;
            this.query = query;
        }
    }
}
