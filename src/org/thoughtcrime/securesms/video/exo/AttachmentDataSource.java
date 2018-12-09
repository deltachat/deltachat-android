package org.thoughtcrime.securesms.video.exo;


import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;

import org.thoughtcrime.securesms.mms.PartAuthority;

import java.io.IOException;

public class AttachmentDataSource implements DataSource {

  private final DefaultDataSource defaultDataSource;

  private DataSource dataSource;

  public AttachmentDataSource(DefaultDataSource defaultDataSource) {
    this.defaultDataSource = defaultDataSource;
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    dataSource = defaultDataSource;
    return dataSource.open(dataSpec);
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) throws IOException {
    return dataSource.read(buffer, offset, readLength);
  }

  @Override
  public Uri getUri() {
    return dataSource.getUri();
  }

  @Override
  public void close() throws IOException {
    dataSource.close();
  }
}
