package com.b44t.messenger;

import android.graphics.Color;
import android.util.Log;

public class DcContact {

    public final static int DC_CONTACT_ID_SELF               = 1;
    public final static int DC_CONTACT_ID_INFO               = 2;
    public final static int DC_CONTACT_ID_DEVICE             = 5;
    public final static int DC_CONTACT_ID_LAST_SPECIAL       = 9;
    public final static int DC_CONTACT_ID_NEW_CONTACT        = -1; // used by the UI, not valid to the core
    public final static int DC_CONTACT_ID_NEW_GROUP          = -2; //      - " -
    public final static int DC_CONTACT_ID_NEW_VERIFIED_GROUP = -3; //      - " -
    public final static int DC_CONTACT_ID_ADD_MEMBER         = -4; //      - " -
    public final static int DC_CONTACT_ID_QR_INVITE          = -5; //      - " -

    public DcContact(long contactCPtr) {
        this.contactCPtr = contactCPtr;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        unrefContactCPtr();
        contactCPtr = 0;
    }


    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DcContact)) {
            return false;
        }

        DcContact that = (DcContact) other;
        return this.getId()==that.getId();
    }

    @Override
    public int hashCode() {
        return this.getId();
    }

    @Override
    public String toString() {
        return getAddr();
    }

    public native int     getId          ();
    public native String  getName        ();
    public native String  getDisplayName ();
    public native String  getFirstName   ();
    public native String  getAddr        ();
    public native String  getNameNAddr   ();
    public native String  getProfileImage();
    public native int     getColor       ();
    public native boolean isBlocked      ();
    public native boolean isVerified     ();

    // working with raw c-data
    private long        contactCPtr;    // CAVE: the name is referenced in the JNI
    private native void unrefContactCPtr();

    public int getArgbColor() {
        int rgb = getColor();
        return Color.argb(0xFF, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }
}
