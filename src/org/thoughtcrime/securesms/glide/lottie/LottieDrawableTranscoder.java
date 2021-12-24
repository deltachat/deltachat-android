package org.thoughtcrime.securesms.glide.lottie;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

public class LottieDrawableTranscoder implements ResourceTranscoder<LottieComposition, LottieDrawable> {
  @Nullable
  @Override
  public Resource<LottieDrawable> transcode(
      @NonNull Resource<LottieComposition> toTranscode, @NonNull Options options) {
    LottieComposition composition = toTranscode.get();
    LottieDrawable drawable = new LottieDrawable();
    drawable.setComposition(composition);
    drawable.setSafeMode(true);
    drawable.setRepeatCount(LottieDrawable.INFINITE);
    return new SimpleResource<>(drawable);
  }
}
