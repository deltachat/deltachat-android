package org.thoughtcrime.securesms.video.recode;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcMsg;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.util.Matrix;
import com.googlecode.mp4parser.util.Path;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.Util;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

public class VideoRecoder {

  private static final String TAG = VideoRecoder.class.getSimpleName();

  private final static String MIME_TYPE = "video/avc";
  private final boolean cancelCurrentVideoConversion = false;
  private final Object videoConvertSync = new Object();

  private void checkConversionCanceled() throws Exception {
    boolean cancelConversion;
    synchronized (videoConvertSync) {
      cancelConversion = cancelCurrentVideoConversion;
    }
    if (cancelConversion) {
      throw new RuntimeException("canceled conversion");
    }
  }

  private int selectTrack(MediaExtractor extractor, boolean audio) {
    int numTracks = extractor.getTrackCount();
    for (int i = 0; i < numTracks; i++) {
      MediaFormat format = extractor.getTrackFormat(i);
      String mime = format.getString(MediaFormat.KEY_MIME);
      if (audio) {
        if (mime.startsWith("audio/")) {
          return i;
        }
      } else {
        if (mime.startsWith("video/")) {
          return i;
        }
      }
    }
    return -5;
  }

  private long readAndWriteTrack(MediaExtractor extractor, MP4Builder mediaMuxer, MediaCodec.BufferInfo info, long start, long end, File file, boolean isAudio) throws Exception {
    int trackIndex = selectTrack(extractor, isAudio);
    if (trackIndex >= 0) {
      extractor.selectTrack(trackIndex);
      MediaFormat trackFormat = extractor.getTrackFormat(trackIndex);
      int muxerTrackIndex = mediaMuxer.addTrack(trackFormat, isAudio);
      int maxBufferSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
      boolean inputDone = false;
      if (start > 0) {
        extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
      } else {
        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
      }
      ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
      long startTime = -1;

      checkConversionCanceled();
      long lastTimestamp = -100;

      while (!inputDone) {
        checkConversionCanceled();

        boolean eof = false;
        int index = extractor.getSampleTrackIndex();
        if (index == trackIndex) {
          info.size = extractor.readSampleData(buffer, 0);
          if (info.size >= 0) {
            info.presentationTimeUs = extractor.getSampleTime();
          } else {
            info.size = 0;
            eof = true;
          }

          if (info.size > 0 && !eof) {
            if (start > 0 && startTime == -1) {
              startTime = info.presentationTimeUs;
            }
            if (end < 0 || info.presentationTimeUs < end) {
              if (info.presentationTimeUs > lastTimestamp) {
                info.offset = 0;
                info.flags = extractor.getSampleFlags();
                if (mediaMuxer.writeSampleData(muxerTrackIndex, buffer, info, isAudio)) {
                  //didWriteData(messageObject, file, false, false);
                }
              }
              lastTimestamp = info.presentationTimeUs;
            } else {
              eof = true;
            }
          }
          if (!eof) {
            extractor.advance();
          }
        } else if (index == -1) {
          eof = true;
        } else {
          extractor.advance();
        }
        if (eof) {
          inputDone = true;
        }
      }

      extractor.unselectTrack(trackIndex);
      return startTime;
    }
    return -1;
  }

  private boolean convertVideo(final VideoEditedInfo videoEditedInfo, String destPath) {

    long startTime = videoEditedInfo.startTime;
    long endTime = videoEditedInfo.endTime;
    int resultWidth = videoEditedInfo.resultWidth;
    int resultHeight = videoEditedInfo.resultHeight;
    int rotationValue = videoEditedInfo.rotationValue;
    int originalWidth = videoEditedInfo.originalWidth;
    int originalHeight = videoEditedInfo.originalHeight;
    int originalVideoBitrate = videoEditedInfo.originalVideoBitrate;
    int resultVideoBitrate = videoEditedInfo.resultVideoBitrate;
    int rotateRender = 0;
    File cacheFile = new File(destPath);

    if (rotationValue == 90) {
      int temp = resultHeight;
      resultHeight = resultWidth;
      resultWidth = temp;
      rotationValue = 0;
      rotateRender = 270;
    } else if (rotationValue == 180) {
      rotateRender = 180;
      rotationValue = 0;
    } else if (rotationValue == 270) {
      int temp = resultHeight;
      resultHeight = resultWidth;
      resultWidth = temp;
      rotationValue = 0;
      rotateRender = 90;
    }

    File inputFile = new File(videoEditedInfo.originalPath);
    if (!inputFile.canRead()) {
      //didWriteData(messageObject, cacheFile, true, true);
      Log.w(TAG, "Could not read video file to be recoded");
      return false;
    }

    boolean error = false;
    long videoStartTime = startTime;

    long time = System.currentTimeMillis();

    if (resultWidth != 0 && resultHeight != 0) {
      MP4Builder mediaMuxer = null;
      MediaExtractor extractor = null;

      try {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        Mp4Movie movie = new Mp4Movie();
        movie.setCacheFile(cacheFile);
        movie.setRotation(rotationValue);
        movie.setSize(resultWidth, resultHeight);
        mediaMuxer = new MP4Builder().createMovie(movie);
        extractor = new MediaExtractor();
        extractor.setDataSource(inputFile.toString());

        checkConversionCanceled();

        if (resultVideoBitrate<originalVideoBitrate || resultWidth != originalWidth || resultHeight != originalHeight || rotateRender != 0) {
          int videoIndex;
          videoIndex = selectTrack(extractor, false);
          if (videoIndex >= 0) {
            MediaCodec decoder = null;
            MediaCodec encoder = null;
            InputSurface inputSurface = null;
            OutputSurface outputSurface = null;

            try {
              long videoTime = -1;
              boolean outputDone = false;
              boolean inputDone = false;
              boolean decoderDone = false;
              int videoTrackIndex = -5;

              int colorFormat;
              colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
              //Log.i("DeltaChat", "colorFormat = " + colorFormat);

              extractor.selectTrack(videoIndex);
              if (startTime > 0) {
                extractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
              } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
              }
              MediaFormat inputFormat = extractor.getTrackFormat(videoIndex);

              MediaFormat outputFormat = MediaFormat.createVideoFormat(MIME_TYPE, resultWidth, resultHeight);
              outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
              outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, resultVideoBitrate != 0 ? resultVideoBitrate : 921600);
              outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
              outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

              encoder = MediaCodec.createEncoderByType(MIME_TYPE);
              encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
              inputSurface = new InputSurface(encoder.createInputSurface());
              inputSurface.makeCurrent();
              encoder.start();

              decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
              outputSurface = new OutputSurface();
              decoder.configure(inputFormat, outputSurface.getSurface(), null, 0);
              decoder.start();

              final int TIMEOUT_USEC = 2500;
              ByteBuffer[] decoderInputBuffers = null;
              ByteBuffer[] encoderOutputBuffers = null;

              checkConversionCanceled();

              while (!outputDone) {
                checkConversionCanceled();
                if (!inputDone) {
                  boolean eof = false;
                  int index = extractor.getSampleTrackIndex();
                  if (index == videoIndex) {
                    int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufIndex >= 0) {
                      ByteBuffer inputBuf;
                      inputBuf = decoder.getInputBuffer(inputBufIndex);
                      int chunkSize = extractor.readSampleData(inputBuf, 0);
                      if (chunkSize < 0) {
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                      } else {
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                      }
                    }
                  } else if (index == -1) {
                    eof = true;
                  }
                  if (eof) {
                    int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                    if (inputBufIndex >= 0) {
                      decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                      inputDone = true;
                    }
                  }
                }

                boolean decoderOutputAvailable = !decoderDone;
                boolean encoderOutputAvailable = true;
                while (decoderOutputAvailable || encoderOutputAvailable) {
                  checkConversionCanceled();
                  int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                  if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    encoderOutputAvailable = false;
                  } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                  } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = encoder.getOutputFormat();
                    if (videoTrackIndex == -5) {
                      videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
                    }
                  } else if (encoderStatus < 0) {
                    throw new RuntimeException("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                  } else {
                    ByteBuffer encodedData;
                    encodedData = encoder.getOutputBuffer(encoderStatus);
                    if (encodedData == null) {
                      throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                    }
                    if (info.size > 1) {
                      if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (mediaMuxer.writeSampleData(videoTrackIndex, encodedData, info, false)) {
                          //didWriteData(messageObject, cacheFile, false, false);
                        }
                      } else if (videoTrackIndex == -5) {
                        byte[] csd = new byte[info.size];
                        encodedData.limit(info.offset + info.size);
                        encodedData.position(info.offset);
                        encodedData.get(csd);
                        ByteBuffer sps = null;
                        ByteBuffer pps = null;
                        for (int a = info.size - 1; a >= 0; a--) {
                          if (a > 3) {
                            if (csd[a] == 1 && csd[a - 1] == 0 && csd[a - 2] == 0 && csd[a - 3] == 0) {
                              sps = ByteBuffer.allocate(a - 3);
                              pps = ByteBuffer.allocate(info.size - (a - 3));
                              sps.put(csd, 0, a - 3).position(0);
                              pps.put(csd, a - 3, info.size - (a - 3)).position(0);
                              break;
                            }
                          } else {
                            break;
                          }
                        }

                        MediaFormat newFormat = MediaFormat.createVideoFormat(MIME_TYPE, resultWidth, resultHeight);
                        if (sps != null && pps != null) {
                          newFormat.setByteBuffer("csd-0", sps);
                          newFormat.setByteBuffer("csd-1", pps);
                        }
                        videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
                      }
                    }
                    outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    encoder.releaseOutputBuffer(encoderStatus, false);
                  }
                  if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    continue;
                  }

                  if (!decoderDone) {
                    int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                      decoderOutputAvailable = false;
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                      MediaFormat newFormat = decoder.getOutputFormat();
                      //Log.i("DeltaChat", "newFormat = " + newFormat);
                    } else if (decoderStatus < 0) {
                      throw new RuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                    } else {
                      boolean doRender;
                      doRender = info.size != 0;
                      if (endTime > 0 && info.presentationTimeUs >= endTime) {
                        inputDone = true;
                        decoderDone = true;
                        doRender = false;
                        info.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                      }
                      if (startTime > 0 && videoTime == -1) {
                        if (info.presentationTimeUs < startTime) {
                          doRender = false;
                          //Log.i("DeltaChat", "drop frame startTime = " + startTime + " present time = " + info.presentationTimeUs);
                        } else {
                          videoTime = info.presentationTimeUs;
                        }
                      }
                      decoder.releaseOutputBuffer(decoderStatus, doRender);
                      if (doRender) {
                        boolean errorWait = false;
                        try {
                          outputSurface.awaitNewImage();
                        } catch (Exception e) {
                          errorWait = true;
                          Log.w(TAG, "error while waiting for recording output surface", e);
                        }
                        if (!errorWait) {
                          outputSurface.drawImage(false);
                          inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
                          inputSurface.swapBuffers();
                        }
                      }
                      if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decoderOutputAvailable = false;
                        //Log.i("DeltaChat", "decoder stream end");
                        encoder.signalEndOfInputStream();
                      }
                    }
                  }
                }
              }
              if (videoTime != -1) {
                videoStartTime = videoTime;
              }
            } catch (Exception e) {
              Log.w(TAG,"Recoding video failed unexpectedly", e);
              error = true;
            }

            extractor.unselectTrack(videoIndex);

            if (outputSurface != null) {
              outputSurface.release();
            }
            if (inputSurface != null) {
              inputSurface.release();
            }
            if (decoder != null) {
              decoder.stop();
              decoder.release();
            }
            if (encoder != null) {
              encoder.stop();
              encoder.release();
            }

            checkConversionCanceled();
          }
        } else {
          long videoTime = readAndWriteTrack(extractor, mediaMuxer, info, startTime, endTime, cacheFile, false);
          if (videoTime != -1) {
            videoStartTime = videoTime;
          }
        }
        if (!error) {
          readAndWriteTrack(extractor, mediaMuxer, info, videoStartTime, endTime, cacheFile, true);
        }
      } catch (Exception e) {
        Log.w(TAG,"Recoding video failed unexpectedly/2", e);
        error = true;
      } finally {
        if (extractor != null) {
          extractor.release();
        }
        if (mediaMuxer != null) {
          try {
            mediaMuxer.finishMovie(false);
          } catch (Exception e) {
            Log.w(TAG,"Flushing video failed unexpectedly", e);
          }
        }
        //Log.i("DeltaChat", "time = " + (System.currentTimeMillis() - time));
      }
    } else {
      //didWriteData(messageObject, cacheFile, true, true);
      Log.w(TAG,"Video width or height are 0, refusing recode.");
      return false;
    }
    //didWriteData(messageObject, cacheFile, true, error);
    return true;
  }

  private static class VideoEditedInfo {
    String originalPath;
    float  originalDurationMs;
    long   originalAudioBytes;
    int    originalRotationValue;
    int    originalWidth;
    int    originalHeight;
    int    originalVideoBitrate;

    long   startTime;
    long   endTime;
    int    rotationValue;

    int    resultWidth;
    int    resultHeight;
    int    resultVideoBitrate;

    int    estimatedBytes;
  }

  private static VideoEditedInfo getVideoEditInfoFromFile(String videoPath) {
    // load information for the given video
    VideoEditedInfo vei = new VideoEditedInfo();
    vei.originalPath = videoPath;

    try {
      IsoFile isoFile = new IsoFile(videoPath);
      List<Box> boxes = Path.getPaths(isoFile, "/moov/trak/");
      TrackHeaderBox trackHeaderBox = null;

      // if we find a video-track, we're just optimistic that the following recoding also works -
      // if it fails, we know this then.
      // older versions check before for paths as "/moov/trak/mdia/minf/stbl/stsd/mp4a/"
      // (using Path.getPath()), however this is not sufficient, other paths as "moov/mvhd"
      // are also valid and it is hard to maintain a list here.

      for (Box box : boxes) {
        TrackBox trackBox = (TrackBox) box;
        long sampleSizes = 0;
        long trackBitrate = 0;
        try {
          MediaBox mediaBox = trackBox.getMediaBox();
          MediaHeaderBox mediaHeaderBox = mediaBox.getMediaHeaderBox();
          SampleSizeBox sampleSizeBox = mediaBox.getMediaInformationBox().getSampleTableBox().getSampleSizeBox();
          for (long size : sampleSizeBox.getSampleSizes()) {
            sampleSizes += size;
          }
          float originalVideoSeconds = (float) mediaHeaderBox.getDuration() / (float) mediaHeaderBox.getTimescale();
          trackBitrate = (int) (sampleSizes * 8 / originalVideoSeconds);
          vei.originalDurationMs = originalVideoSeconds * 1000;
        } catch (Exception e) {
          Log.w(TAG, "Get video info: Calculating sample sizes failed unexpectedly", e);
        }
        TrackHeaderBox headerBox = trackBox.getTrackHeaderBox();
        if (headerBox.getWidth() != 0 && headerBox.getHeight() != 0) {
          trackHeaderBox = headerBox;
          vei.originalVideoBitrate = (int) (trackBitrate / 100000 * 100000);
        } else {
          vei.originalAudioBytes += sampleSizes;
        }
      }
      if (trackHeaderBox == null) {
        Log.w(TAG, "Get video info: No trackHeaderBox");
        return null;
      }

      Matrix matrix = trackHeaderBox.getMatrix();
      if (matrix.equals(Matrix.ROTATE_90)) {
        vei.originalRotationValue = 90;
      } else if (matrix.equals(Matrix.ROTATE_180)) {
        vei.originalRotationValue = 180;
      } else if (matrix.equals(Matrix.ROTATE_270)) {
        vei.originalRotationValue = 270;
      }
      vei.originalWidth = (int) trackHeaderBox.getWidth();
      vei.originalHeight = (int) trackHeaderBox.getHeight();

    } catch (Exception e) {
      Log.w(TAG, "Get video info: Reading message info failed unexpectedly", e);
      return null;
    }

    return vei;
  }

  private static int calculateEstimatedSize(float timeDelta, int resultBitrate, float originalDurationMs, long originalAudioBytes) {
    long videoFramesSize = (long) (resultBitrate / 8 * (originalDurationMs /1000));
    int size = (int) ((originalAudioBytes + videoFramesSize) * timeDelta);
    return size;
  }

  private static void alert(Context context, String str)
  {
    Log.e(TAG, str);
    Util.runOnMain(() -> new AlertDialog.Builder(context)
      .setCancelable(false)
      .setMessage(str)
      .setPositiveButton(android.R.string.ok, null)
      .show());
  }

  // prepareVideo() assumes the msg object is set up properly to being sent;
  // the function fills out missing information and also recodes the video as needed.
  // return: true=video might be prepared, can be sent, false=error
  public static boolean prepareVideo(Context context, int chatId, DcMsg msg) {
    final long MAX_BYTES = DcHelper.getInt(context, "sys.msgsize_max_recommended");
    final String TOO_BIG_FILE = "Video cannot be compressed to a reasonable size. Try a shorter video or a lower quality.";
    try {
      String inPath = msg.getFile();
      Log.i(TAG, "Preparing video: " + inPath);

      // try to get information from video file
      VideoEditedInfo vei = getVideoEditInfoFromFile(inPath);
      if (vei == null) {
        Log.w(TAG, String.format("Recoding failed for %s: cannot get info", inPath));
        if (msg.getFilebytes() > MAX_BYTES+MAX_BYTES/4) {
          alert(context, TOO_BIG_FILE);
          return false;
        }
        return true; // if the file is small, send it without recoding
      }

      vei.rotationValue = vei.originalRotationValue;
      vei.startTime = 0;
      vei.endTime = -1;

      // set these information to the message object (not yet in database);
      // if we can recdode, this will be overwritten below
      if (vei.originalRotationValue == 90 || vei.originalRotationValue == 270) {
        msg.setDimension(vei.originalHeight, vei.originalWidth);
      } else {
        msg.setDimension(vei.originalWidth, vei.originalHeight);
      }
      msg.setDuration((int)vei.originalDurationMs);

      // check if video bitrate is already reasonable
      final int  MAX_KBPS = 1500000;
      long inBytes = new File(inPath).length();
      if (inBytes > 0 && inBytes <= MAX_BYTES && vei.originalVideoBitrate <= MAX_KBPS*2 /*be tolerant as long the file size matches*/) {
        Log.i(TAG, String.format("recoding for %s is not needed, %d bytes and %d kbps are ok", inPath, inBytes, vei.originalVideoBitrate));
        return true;
      }

      // calculate new video bitrate, sth. between 200 kbps and 1500 kbps
      long resultDurationMs = (long) vei.originalDurationMs;
      long maxVideoBytes = MAX_BYTES - vei.originalAudioBytes - resultDurationMs /*10 kbps codec overhead*/;
      vei.resultVideoBitrate = (int) (maxVideoBytes / Math.max(1, resultDurationMs / 1000) * 8);

      if (vei.resultVideoBitrate < 200000) {
        vei.resultVideoBitrate = 200000;
      } else if (vei.resultVideoBitrate > 500000) {
        boolean hardCompression = Prefs.isHardCompressionEnabled(context);
        if (resultDurationMs < 30 * 1000 && !hardCompression) {
          vei.resultVideoBitrate = MAX_KBPS; // ~ 12 MB/minute, plus Audio
        } else if (resultDurationMs < 60 * 1000 && !hardCompression) {
          vei.resultVideoBitrate = 1000000; // ~ 8 MB/minute, plus Audio
        } else {
          vei.resultVideoBitrate = 500000; // ~ 3.7 MB/minute, plus Audio
        }
      }

      // calculate video dimensions
      int maxSide = vei.resultVideoBitrate > 400000 ? 640 : 480;
      vei.resultWidth = vei.originalWidth;
      vei.resultHeight = vei.originalHeight;
      if (vei.resultWidth > maxSide || vei.resultHeight > maxSide) {
        float scale = vei.resultWidth > vei.resultHeight ? (float) maxSide / vei.resultWidth : (float) maxSide / vei.resultHeight;
        vei.resultWidth *= scale;
        vei.resultHeight *= scale;
      }

      // we know the most important things now, prepare the message to get a responsive ui
      if (vei.originalRotationValue == 90 || vei.originalRotationValue == 270) {
        msg.setDimension(vei.resultHeight, vei.resultWidth);
      } else {
        msg.setDimension(vei.resultWidth, vei.resultHeight);
      }
      msg.setDuration((int) resultDurationMs);

      // calculate bytes
      vei.estimatedBytes = VideoRecoder.calculateEstimatedSize((float) resultDurationMs / vei.originalDurationMs,
          vei.resultVideoBitrate, vei.originalDurationMs, vei.originalAudioBytes);

      if (vei.estimatedBytes > MAX_BYTES+MAX_BYTES/4) {
        alert(context, TOO_BIG_FILE);
        return false;
      }

      // recode
      String tempPath = DcHelper.getBlobdirFile(DcHelper.getContext(context), inPath);
      VideoRecoder videoRecoder = new VideoRecoder();
      if (!videoRecoder.convertVideo(vei, tempPath)) {
        alert(context, String.format("Recoding failed for %s: cannot convert to temporary file %s", inPath, tempPath));
        return false;
      }

      msg.setFileAndDeduplicate(tempPath, msg.getFilename(), msg.getFilemime());

      Log.i(TAG, String.format("recoding for %s done", inPath));
    }
    catch(Exception e) {
      e.printStackTrace();
    }

    return true;
  }
}
