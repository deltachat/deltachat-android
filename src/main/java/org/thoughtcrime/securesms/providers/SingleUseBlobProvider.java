package org.thoughtcrime.securesms.providers;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SingleUseBlobProvider {

  public  static final String AUTHORITY   = "org.thoughtcrime.securesms";
  public  static final String PATH        = "memory/*/#";

  private final Map<Long, byte[]> cache = new HashMap<>();

  private static final SingleUseBlobProvider instance = new SingleUseBlobProvider();

  public static SingleUseBlobProvider getInstance() {
    return instance;
  }

  private SingleUseBlobProvider() {}

  public synchronized @NonNull InputStream getStream(long id) throws IOException {
    byte[] cached = cache.get(id);
    cache.remove(id);

    if (cached != null) return new ByteArrayInputStream(cached);
    else                throw new IOException("ID not found: " + id);

  }

}
