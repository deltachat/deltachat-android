package org.thoughtcrime.securesms.mms;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bumptech.glide.load.data.StreamLocalUriFetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class DecryptableStreamLocalUriFetcher extends StreamLocalUriFetcher {

  private static final String TAG = DecryptableStreamLocalUriFetcher.class.getSimpleName();

  private final Context context;

  DecryptableStreamLocalUriFetcher(Context context, Uri uri) {
    super(context.getContentResolver(), uri);
    this.context      = context;
  }

  @Override
  protected InputStream loadResource(Uri uri, ContentResolver contentResolver) throws FileNotFoundException {
    try {
      return PartAuthority.getAttachmentStream(context, uri);
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      throw new FileNotFoundException("PartAuthority couldn't load Uri resource.");
    }
  }
}
