package org.thoughtcrime.securesms.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.thoughtcrime.securesms.util.Prefs;

public class AudioCodec {

  private static final String TAG = AudioCodec.class.getSimpleName();

  private static final int SAMPLE_RATE = 44100;
  private static final int CHANNELS = 1;
  private static final int BIT_RATE_BALANCED = 32000;
  private static final int BIT_RATE_WORSE = 24000;

  private final int bufferSize;
  private final MediaCodec mediaCodec;
  private final AudioRecord audioRecord;
  private final String outputPath;

  private volatile boolean running = true;
  private volatile boolean finished = false;
  private long startTime = 0;

  public AudioCodec(Context context, String outputPath) throws IOException {
    this.outputPath = outputPath;
    this.bufferSize =
        AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    this.audioRecord = createAudioRecord(this.bufferSize);
    this.mediaCodec = createMediaCodec(context, this.bufferSize);

    this.mediaCodec.start();

    try {
      audioRecord.startRecording();
    } catch (Exception e) {
      Log.w(TAG, "Failed to start recording", e);
      mediaCodec.release();
      throw new IOException(e);
    }
  }

  public synchronized void stop() {
    running = false;
    while (!finished) {
      try {
        wait(5000);
        if (!finished) {
          Log.w(TAG, "Timeout waiting for recording to finish");
          break;
        }
      } catch (InterruptedException ie) {
        Log.w(TAG, "Interrupted while waiting for recording to finish", ie);
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  public void start() {
    new Thread(
            () -> {
              this.startTime = System.nanoTime();

              MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
              byte[] audioRecordData = new byte[bufferSize];
              MediaMuxer muxer = null;
              int audioTrackIndex = -1;
              boolean muxerStarted = false;

              try {
                muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                while (true) {
                  boolean running = isRunning();

                  handleCodecInput(audioRecord, audioRecordData, mediaCodec, running);
                  int codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 2000);

                  while (codecOutputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (codecOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                      // Get the format to add track to muxer
                      if (muxerStarted) {
                        Log.w(TAG, "Output format changed after muxer started, ignoring");
                        codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                        continue;
                      }
                      MediaFormat newFormat = mediaCodec.getOutputFormat();
                      Log.d(TAG, "Output format changed: " + newFormat);
                      audioTrackIndex = muxer.addTrack(newFormat);
                      muxer.start();
                      muxerStarted = true;
                      Log.d(TAG, "Muxer started");
                    } else if (codecOutputBufferIndex >= 0) {
                      ByteBuffer encodedData = mediaCodec.getOutputBuffer(codecOutputBufferIndex);

                      if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // Codec config info, not actual data, skip it
                        Log.d(TAG, "Ignoring CODEC_CONFIG buffer");
                        bufferInfo.size = 0;
                      }

                      if (bufferInfo.size != 0 && encodedData != null) {
                        if (!muxerStarted) {
                          Log.w(TAG, "Muxer not started, dropping encoded data");
                        } else {
                          // Adjust ByteBuffer to match BufferInfo
                          encodedData.position(bufferInfo.offset);
                          encodedData.limit(bufferInfo.offset + bufferInfo.size);

                          // Write sample data to muxer
                          muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo);
                        }
                      }

                      mediaCodec.releaseOutputBuffer(codecOutputBufferIndex, false);

                      // Check for end of stream
                      if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "End of stream reached");
                        break;
                      }
                    }

                    codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                  }

                  if (!running && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                  }
                }
              } catch (Exception e) {
                Log.w(TAG, "Error during encoding", e);
              } finally {
                try {
                  if (muxerStarted && muxer != null) {
                    try {
                      muxer.stop();
                      Log.d(TAG, "Muxer stopped");
                    } catch (IllegalStateException e) {
                      Log.w(TAG, "Muxer already stopped", e);
                    }
                  }
                } catch (Exception e) {
                  Log.w(TAG, "Error stopping muxer", e);
                }

                if (muxer != null) {
                  try {
                    muxer.release();
                    Log.d(TAG, "Muxer released");
                  } catch (Exception e) {
                    Log.w(TAG, "Error releasing muxer", e);
                  }
                }

                try {
                  mediaCodec.stop();
                } catch (Exception e) {
                  Log.w(TAG, "Error stopping codec", e);
                }

                try {
                  audioRecord.stop();
                } catch (Exception e) {
                  Log.w(TAG, "Error stopping audio record", e);
                }

                try {
                  mediaCodec.release();
                } catch (Exception e) {
                  Log.w(TAG, "Error releasing codec", e);
                }

                try {
                  audioRecord.release();
                } catch (Exception e) {
                  Log.w(TAG, "Error releasing audio record", e);
                }

                setFinished();
              }
            },
            AudioCodec.class.getSimpleName())
        .start();
  }

  private synchronized boolean isRunning() {
    return running;
  }

  private synchronized void setFinished() {
    finished = true;
    notifyAll();
  }

  private void handleCodecInput(
      AudioRecord audioRecord, byte[] audioRecordData, MediaCodec mediaCodec, boolean running) {
    int length = audioRecord.read(audioRecordData, 0, audioRecordData.length);

    if (length < 0) {
      Log.w(TAG, "Error reading from AudioRecord: " + length);
      return;
    }

    int codecInputBufferIndex = mediaCodec.dequeueInputBuffer(10 * 1000);

    if (codecInputBufferIndex >= 0) {
      ByteBuffer codecBuffer = mediaCodec.getInputBuffer(codecInputBufferIndex);

      if (codecBuffer != null) {
        codecBuffer.clear();
        codecBuffer.put(audioRecordData, 0, length);
        long presentationTimeUs = getPresentationTimeUs();
        mediaCodec.queueInputBuffer(
            codecInputBufferIndex,
            0,
            length,
            presentationTimeUs,
            running ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
      }
    }
  }

  private long getPresentationTimeUs() {
    return (System.nanoTime() - startTime) / 1000;
  }

  private AudioRecord createAudioRecord(int bufferSize) {
    return new AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize * 10);
  }

  private MediaCodec createMediaCodec(Context context, int bufferSize) throws IOException {
    MediaCodec mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");

    MediaFormat mediaFormat =
        MediaFormat.createAudioFormat("audio/mp4a-latm", SAMPLE_RATE, CHANNELS);
    mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
    mediaFormat.setInteger(
        MediaFormat.KEY_BIT_RATE,
        Prefs.isHardCompressionEnabled(context) ? BIT_RATE_WORSE : BIT_RATE_BALANCED);
    mediaFormat.setInteger(
        MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

    try {
      mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    } catch (Exception e) {
      Log.w(TAG, e);
      mediaCodec.release();
      throw new IOException(e);
    }

    return mediaCodec;
  }
}
