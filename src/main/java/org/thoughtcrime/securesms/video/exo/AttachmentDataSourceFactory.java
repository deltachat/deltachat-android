package org.thoughtcrime.securesms.video.exo;


import androidx.annotation.NonNull;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

public class AttachmentDataSourceFactory implements DataSource.Factory {

  private final DefaultDataSourceFactory defaultDataSourceFactory;

  public AttachmentDataSourceFactory(@NonNull DefaultDataSourceFactory defaultDataSourceFactory)
  {
    this.defaultDataSourceFactory = defaultDataSourceFactory;
  }

  @Override
  public AttachmentDataSource createDataSource() {
    return new AttachmentDataSource(defaultDataSourceFactory.createDataSource());
  }
}
