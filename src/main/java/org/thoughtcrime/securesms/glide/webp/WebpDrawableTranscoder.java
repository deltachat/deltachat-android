package org.thoughtcrime.securesms.glide.webp;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.github.penfeizhou.animation.webp.WebPDrawable;
import com.github.penfeizhou.animation.webp.decode.WebPDecoder;

public class WebpDrawableTranscoder implements ResourceTranscoder<WebPDecoder, Drawable> {
  @Nullable
  @Override
  public Resource<Drawable> transcode(@NonNull Resource<WebPDecoder> toTranscode, @NonNull Options options) {
    final WebPDrawable webPDrawable = new WebPDrawable(toTranscode.get());
    webPDrawable.setAutoPlay(true);
    return new DrawableResource<Drawable>(webPDrawable) {
      @NonNull
      @Override
      public Class<Drawable> getResourceClass() {
        return Drawable.class;
      }

      @Override
      public int getSize() {
        return webPDrawable.getMemorySize();
      }

      @Override
      public void recycle() {
        webPDrawable.stop();
      }

      @Override
      public void initialize() {
        super.initialize();
      }
    };
  }
}
