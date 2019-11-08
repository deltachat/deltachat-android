package org.thoughtcrime.securesms.video.exo;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;

public class AttachmentDataSourceFactory implements DataSource.Factory {

  private final DefaultDataSourceFactory defaultDataSourceFactory;

  public AttachmentDataSourceFactory(@NonNull Context context,
                                     @NonNull DefaultDataSourceFactory defaultDataSourceFactory,
                                     @Nullable TransferListener listener)
  {
    this.defaultDataSourceFactory = defaultDataSourceFactory;
  }

  @Override
  public AttachmentDataSource createDataSource() {
    return new AttachmentDataSource(defaultDataSourceFactory.createDataSource());
  }
}
