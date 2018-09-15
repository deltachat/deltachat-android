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
        int[] ids = dcContext.getContacts(listflags, query);
        if(query!=null) {
            // show the "new" link also for partly typed e-mail addresses, so that the user knows he can continue
            if( dcContext.lookupContactIdByAddr(query)==0) {
                ids = ArrayUtils.appendInt(ids, DcContact.DC_CONTACT_ID_NEW_CONTACT);
            }
        }
        return new DcContactsLoader.Ret(ids, query);
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
