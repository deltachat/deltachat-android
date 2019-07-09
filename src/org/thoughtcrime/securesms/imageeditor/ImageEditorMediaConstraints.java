package org.thoughtcrime.securesms.imageeditor;

import android.content.Context;

import org.thoughtcrime.securesms.mms.MediaConstraints;
import org.thoughtcrime.securesms.util.Util;

public class ImageEditorMediaConstraints extends MediaConstraints {

    private static final int MAX_IMAGE_DIMEN_LOWMEM = 768;
    private static final int MAX_IMAGE_DIMEN        = 1536;
    private static final int KB                     = 1024;
    private static final int MB                     = 1024 * KB;

    @Override
    public int getImageMaxWidth(Context context) {
        return Util.isLowMemory(context) ? MAX_IMAGE_DIMEN_LOWMEM : MAX_IMAGE_DIMEN;
    }

    @Override
    public int getImageMaxHeight(Context context) {
        return getImageMaxWidth(context);
    }

    @Override
    public int getImageMaxSize(Context context) {
        return 4 * MB;
    }
}
