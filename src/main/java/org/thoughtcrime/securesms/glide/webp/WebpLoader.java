package org.thoughtcrime.securesms.glide.webp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.github.penfeizhou.animation.loader.ByteBufferLoader;
import com.github.penfeizhou.animation.io.StreamReader;
import com.github.penfeizhou.animation.loader.Loader;
import com.github.penfeizhou.animation.webp.decode.WebPDecoder;
import com.github.penfeizhou.animation.webp.decode.WebPParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class WebpLoader implements ResourceDecoder<InputStream, WebPDecoder> {

  @Override
  public boolean handles(@NonNull InputStream source, @NonNull Options options) {
    return WebPParser.isAWebP(new StreamReader(source));
  }

  @Nullable
  @Override
  public Resource<WebPDecoder> decode(@NonNull final InputStream source, int width, int height, @NonNull Options options) throws IOException {
    byte[] data = inputStreamToBytes(source);
    if (data == null) {
      return null;
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    Loader loader = new ByteBufferLoader() {
        @Override
        public ByteBuffer getByteBuffer() {
          byteBuffer.position(0);
          return byteBuffer;
        }
      };
    return new WebPDecoderResource(new WebPDecoder(loader, null), byteBuffer.limit());
  }

  private static byte[] inputStreamToBytes(InputStream is) {
    final int bufferSize = 16384;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
    try {
      int nRead;
      byte[] data = new byte[bufferSize];
      while ((nRead = is.read(data)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
    } catch (IOException e) {
      return null;
    }
    return buffer.toByteArray();
  }

  private static class WebPDecoderResource implements Resource<WebPDecoder> {
    private final WebPDecoder decoder;
    private final int size;

    WebPDecoderResource(WebPDecoder decoder, int size) {
      this.decoder = decoder;
      this.size = size;
    }

    @NonNull
    @Override
    public Class<WebPDecoder> getResourceClass() {
      return WebPDecoder.class;
    }

    @NonNull
    @Override
    public WebPDecoder get() {
      return this.decoder;
    }

    @Override
    public int getSize() {
      return this.size;
    }

    @Override
    public void recycle() {
      this.decoder.stop();
    }
  }
}
