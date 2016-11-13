/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.tgnet;

public class TLObject {

    //public boolean disableFree = false;
    /*
    private static final ThreadLocal<NativeByteBuffer> sizeCalculator = new ThreadLocal<NativeByteBuffer>() {
        @Override
        protected NativeByteBuffer initialValue() {
            return new NativeByteBuffer(true);
        }
    };
    */

    public TLObject() {

    }

    public void readParams(AbstractSerializedData stream, boolean exception) {

    }

    public void serializeToStream(AbstractSerializedData stream) {

    }

    public int getObjectSize() {
        return 0;
    }
}
