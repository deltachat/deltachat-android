package org.thoughtcrime.securesms.glide.lottie;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieResult;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class LottieDecoder implements ResourceDecoder<InputStream, LottieComposition> {

  @Override
  public boolean handles(@NonNull InputStream source, @NonNull Options options) {
    return true;
  }

  public Resource<LottieComposition> decode(
      @NonNull InputStream source, int width, int height, @NonNull Options options)
      throws IOException {
    try {
      LottieResult<LottieComposition> result = LottieCompositionFactory.fromJsonInputStreamSync(new GZIPInputStream(source), null);
      return new SimpleResource<>(result.getValue());
    } catch (Exception ex) {
      throw new IOException("Cannot load Lottie animation from stream", ex);
    }
  }
}
