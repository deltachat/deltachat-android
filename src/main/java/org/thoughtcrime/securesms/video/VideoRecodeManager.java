package org.thoughtcrime.securesms.video;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.transformer.ExportException;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcMsg;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.PartAuthority;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.service.VideoRecodeService;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil;
import org.thoughtcrime.securesms.util.Util;

/**
 * One send action contains a set of videos to be sent to one or more chats of an account. Only one
 * operation runs at a time globally.
 *
 * <p>Policy: recode when the file exceeds sys.msgsize_max_recommended, or when the bitrate exceeds
 * ~3 Mbps, or when the video codec is VP8/VP9 (WebM), or always when "hard compression" is enabled.
 * Recoding runs a medium pass first (~6 MB/min) and a low pass (~1.5 MB/min) only if the result
 * does not fit size limit.
 */
@SuppressLint("StaticFieldLeak") // holds only application context
@OptIn(markerClass = UnstableApi.class)
public class VideoRecodeManager {

  private static final String TAG = "VideoRecodeManager";

  static final int MEDIUM_BPS = 800_000; // ~6 MB/min
  static final int LOW_BPS = 200_000; // ~1.5 MB/min
  static final int AUDIO_BPS = 96_000;
  static final int MEDIUM_MAX_SIDE = 640;
  static final int LOW_MAX_SIDE = 480;
  private static final long BITRATE_RECODE_THRESHOLD = 3_000_000L;
  private static final long FALLBACK_MAX_BYTES = 25L * 1024 * 1024; // 25 min

  public static final String TOO_BIG_MSG =
      "Video cannot be compressed to a reasonable size. Try a shorter video or a lower quality.";
  public static final String SENT_ORIGINAL_MSG =
      "Could not recode video; sending it at its original size.";
  public static final String READ_FAILED_MSG = "Video file could not be read and was not sent.";

  public enum Result {
    SENT,
    SENT_ORIGINAL,
    FAILED_TOO_BIG,
    FAILED_ERROR,
    FAILED_READ,
    CANCELLED
  }

  public interface Listener {
    default void onStateChanged(RecodeState state) {}

    default boolean onFinished(int accId, int chatId, boolean fromComposer, Result result) {
      return false;
    }
  }

  private static VideoRecodeManager instance;

  public static synchronized VideoRecodeManager getInstance(Context context) {
    if (instance == null) {
      instance = new VideoRecodeManager(context.getApplicationContext());
    }
    return instance;
  }

  public static void showBusyDialog(Context activityContext) {
    new AlertDialog.Builder(activityContext)
        .setMessage(R.string.video_recode_busy)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  private static final class Operation {
    final DcContext dcContext;
    final int accId;
    final ImmutableSet<Integer> chatIds;
    final int ingestTotal;
    volatile boolean cancelled;
    volatile boolean ingestRunning;
    final ArrayList<Uri> persistedUris = new ArrayList<>();
    final CountDownLatch ingestDone = new CountDownLatch(1);
    @Nullable Future<?> ingestFuture;

    RecodeState.Phase phase = RecodeState.Phase.INGEST;
    int ingestFinished;
    final ArrayList<RecodeJob> jobs = new ArrayList<>();
    int jobsFinished;

    Operation(DcContext dcContext, ImmutableSet<Integer> chatIds, int ingestTotal) {
      this.dcContext = dcContext;
      this.accId = dcContext.getAccountId();
      this.chatIds = chatIds;
      this.ingestTotal = ingestTotal;
    }
  }

  private static final class Decision {
    static final Decision SEND_AS_IS = new Decision(null);
    static final Decision TOO_BIG = new Decision(null);

    @Nullable final RecodeJob job;

    Decision(@Nullable RecodeJob job) {
      this.job = job;
    }
  }

  private interface IngestTask {
    void run(ArrayList<RecodeJob> jobs);
  }

  private final Context context;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
  private final ExecutorService ingestExecutor =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "VideoIngest"));

  @Nullable private Operation current;
  @Nullable private RecodeJob job;
  @Nullable private VideoTranscoder transcoder;
  private RecodeState state = RecodeState.IDLE;

  private VideoRecodeManager(Context context) {
    this.context = context;
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @MainThread
  public RecodeState getState() {
    return state;
  }

  @MainThread
  public boolean isBusy() {
    return current != null;
  }

  /**
   * Call from the confirm handler while the app is in the foreground.
   *
   * @return false if another operation is running
   */
  @MainThread
  public boolean startOperation(
      DcContext dcContext, Long[] chatIds, List<Uri> uris, @Nullable String text) {
    if (current != null) {
      return false;
    }
    Long[] chatIdsCopy = chatIds.clone();
    List<Uri> uriList = new ArrayList<>(uris);
    ImmutableSet.Builder<Integer> ids = ImmutableSet.builder();
    for (Long id : chatIdsCopy) {
      ids.add(id.intValue());
    }
    Operation op = new Operation(dcContext, ids.build(), chatIdsCopy.length * uriList.size());
    takeReadPermissions(op, uriList);
    start(op, jobs -> ingestUris(op, chatIdsCopy, uriList, text, jobs));
    return true;
  }

  @MainThread
  public boolean startComposerOperation(
      DcContext dcContext, int chatId, Attachment attachment, String body, @Nullable DcMsg quote) {
    if (current != null) {
      return false;
    }
    Operation op = new Operation(dcContext, ImmutableSet.of(chatId), 1);
    Uri dataUri = attachment.getDataUri();
    if (dataUri != null) {
      takeReadPermissions(op, Collections.singletonList(dataUri));
    }
    start(op, jobs -> ingestComposer(op, chatId, attachment, body, quote, jobs));
    return true;
  }

  @MainThread
  public void cancelOperation() {
    Operation op = current;
    if (op == null || op.cancelled) {
      return;
    }
    op.cancelled = true;
    Future<?> ingest = op.ingestFuture;
    if (ingest != null) {
      ingest.cancel(true);
    }
    VideoTranscoder t = transcoder;
    if (t != null) {
      t.cancel();
    } else {
      finishOperation();
    }
  }

  @MainThread
  public void cancelForAccount(int accountId) {
    Operation op = current;
    if (op == null || op.accId != accountId) {
      return;
    }
    cancelOperation();
    if (op.ingestRunning) {
      try {
        //noinspection ResultOfMethodCallIgnored
        op.ingestDone.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @MainThread
  public void cancelIfPending() {
    if (current != null) {
      emitState();
    }
  }

  private void start(Operation op, IngestTask task) {
    current = op;
    emitState();
    op.ingestFuture = ingestExecutor.submit(() -> runIngest(op, task));
  }

  @WorkerThread
  private void runIngest(Operation op, IngestTask task) {
    op.ingestRunning = true;
    ArrayList<RecodeJob> jobs = new ArrayList<>();
    try {
      task.run(jobs);
    } catch (CancellationException e) {
      Log.i(TAG, "Ingest cancelled");
      return;
    } catch (Exception e) {
      Log.w(TAG, "Ingest failed", e);
    } finally {
      releaseReadPermissions(op);
      op.ingestRunning = false;
      op.ingestDone.countDown();
    }
    mainHandler.post(() -> onIngestFinished(op, jobs));
  }

  @WorkerThread
  private void ingestUris(
      Operation op,
      Long[] chatIds,
      List<Uri> uris,
      @Nullable String text,
      ArrayList<RecodeJob> jobs) {
    for (Long longChatId : chatIds) {
      int chatId = longChatId.intValue();
      if (uris.size() > 1 && text != null) {
        checkCancelled(op);
        sendDirect(
            op,
            chatId,
            SendRelayedMessageUtil.createMessage(context, op.dcContext, null, text),
            false);
      }
      for (Uri uri : uris) {
        checkCancelled(op);
        DcMsg msg =
            SendRelayedMessageUtil.createMessage(
                context, op.dcContext, uri, uris.size() == 1 ? text : null);
        checkCancelled(op);
        if (!msg.hasFile()) {
          Log.w(TAG, "Cannot read " + uri);
          postFinished(op, chatId, false, Result.FAILED_READ);
          postIngestProgress(op);
          continue;
        }
        handleIngested(op, chatId, msg, false, jobs);
        postIngestProgress(op);
      }
    }
  }

  @WorkerThread
  private void ingestComposer(
      Operation op,
      int chatId,
      Attachment attachment,
      String body,
      @Nullable DcMsg quote,
      ArrayList<RecodeJob> jobs) {
    Uri uri = attachment.getDataUri();
    String path = null;
    boolean handedOver = false;
    try {
      DcMsg msg;
      try {
        op.dcContext.setDraft(chatId, null);
        String name = displayName(attachment);
        path = copyAttachment(op, uri, name);
        checkCancelled(op);
        msg = new DcMsg(op.dcContext, DcMsg.DC_MSG_VIDEO);
        msg.setFileAndDeduplicate(path, name, null);
        handedOver = true;
        msg.setText(body);
        if (quote != null) {
          msg.setQuote(quote);
        }
      } catch (CancellationException e) {
        if (!handedOver) {
          deleteQuietly(path);
        }
        throw e;
      } catch (Exception e) {
        if (!handedOver) {
          deleteQuietly(path);
        }
        checkCancelled(op);
        Log.w(TAG, "cannot prepare video from composer", e);
        postFinished(op, chatId, true, Result.FAILED_ERROR);
        return;
      }
      checkCancelled(op);
      handleIngested(op, chatId, msg, true, jobs);
      postIngestProgress(op);
    } finally {
      if (uri != null && PersistentBlobProvider.isAuthority(context, uri)) {
        PersistentBlobProvider.getInstance().delete(context, uri);
      }
    }
  }

  private static String displayName(Attachment attachment) {
    String name = attachment.getFileName();
    if (!TextUtils.isEmpty(name)) {
      return name;
    }
    String ext = MediaUtil.getExtensionFromMimeType(attachment.getContentType());
    return "video." + (ext != null ? ext : "mp4");
  }

  @WorkerThread
  private String copyAttachment(Operation op, @Nullable Uri uri, String name) throws IOException {
    if (uri == null) {
      throw new IOException("attachment without uri");
    }
    String base = name;
    String ext = "";
    int dot = name.lastIndexOf('.');
    if (dot >= 0) {
      ext = name.substring(dot);
      base = name.substring(0, dot);
    }
    String path = DcHelper.getBlobdirFile(op.dcContext, base, ext);
    InputStream in = PartAuthority.getAttachmentStream(context, uri);
    if (in == null) {
      throw new IOException("cannot open " + uri);
    }
    OutputStream out;
    try {
      out = new FileOutputStream(path);
    } catch (IOException e) {
      in.close();
      throw e;
    }
    Util.copyCancellable(in, out, path);
    return path;
  }

  private static void deleteQuietly(@Nullable String path) {
    if (path != null) {
      //noinspection ResultOfMethodCallIgnored
      new File(path).delete();
    }
  }

  private static void checkCancelled(Operation op) {
    boolean interrupted = Thread.interrupted();
    if (interrupted || op.cancelled) {
      throw new CancellationException();
    }
  }

  @MainThread
  private void takeReadPermissions(Operation op, List<Uri> uris) {
    ContentResolver resolver = context.getContentResolver();
    for (Uri uri : uris) {
      if (!"content".equals(uri.getScheme()) || hasPersistedReadPermission(resolver, uri)) {
        continue;
      }
      try {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        op.persistedUris.add(uri);
      } catch (SecurityException ignored) {
      }
    }
  }

  private static boolean hasPersistedReadPermission(ContentResolver resolver, Uri uri) {
    for (UriPermission permission : resolver.getPersistedUriPermissions()) {
      if (permission.getUri().equals(uri) && permission.isReadPermission()) {
        return true;
      }
    }
    return false;
  }

  @WorkerThread
  private void releaseReadPermissions(Operation op) {
    for (Uri uri : op.persistedUris) {
      try {
        context
            .getContentResolver()
            .releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } catch (SecurityException e) {
        Log.w(TAG, "Cannot release read permission for " + uri, e);
      }
    }
  }

  @WorkerThread
  private void handleIngested(
      Operation op, int chatId, DcMsg msg, boolean fromComposer, ArrayList<RecodeJob> jobs) {
    if (msg.getType() != DcMsg.DC_MSG_VIDEO) {
      sendDirect(op, chatId, msg, fromComposer);
      return;
    }
    Decision decision = classify(op, chatId, msg, fromComposer);
    if (decision.job != null) {
      jobs.add(decision.job);
    } else if (decision == Decision.TOO_BIG) {
      postFinished(op, chatId, fromComposer, Result.FAILED_TOO_BIG);
    } else {
      sendDirect(op, chatId, msg, fromComposer);
    }
  }

  @WorkerThread
  private void sendDirect(Operation op, int chatId, DcMsg msg, boolean fromComposer) {
    int msgId = op.dcContext.sendMsg(chatId, msg);
    if (msgId == 0) {
      Log.w(TAG, "sendMsg failed: " + op.dcContext.getLastError());
    }
    postFinished(op, chatId, fromComposer, msgId != 0 ? Result.SENT : Result.FAILED_ERROR);
  }

  @WorkerThread
  private Decision classify(Operation op, int chatId, DcMsg msg, boolean fromComposer) {
    String inPath = msg.getFile();
    if (inPath == null || inPath.isEmpty() || !new File(inPath).canRead()) {
      return Decision.SEND_AS_IS;
    }
    long maxBytes = maxBytesFor(op.dcContext);
    long inBytes = new File(inPath).length();
    boolean tooLarge = inBytes > maxBytes;

    VideoMeta meta = probe(inPath);
    if (meta == null || meta.durationMs <= 0) {
      return tooLarge ? Decision.TOO_BIG : Decision.SEND_AS_IS;
    }

    msg.setDimension(meta.dispW, meta.dispH);
    msg.setDuration((int) meta.durationMs);

    boolean forceCompat = isVp8OrVp9(meta.videoMime);
    long bitrate =
        meta.overallBitrate > 0
            ? meta.overallBitrate
            : (inBytes * 8 / Math.max(1, meta.durationMs / 1000));
    boolean hard = Prefs.isHardCompressionEnabled(context);
    boolean overSize = tooLarge || bitrate > BITRATE_RECODE_THRESHOLD;
    if (!hard && !forceCompat && !overSize) {
      return Decision.SEND_AS_IS;
    }

    long lowEstimate = (long) LOW_BPS / 8 * (meta.durationMs / 1000);
    if (lowEstimate > maxBytes + maxBytes / 4) {
      return Decision.TOO_BIG;
    }

    RecodeJob job = new RecodeJob();
    job.chatId = chatId;
    job.msg = msg;
    job.inputPath = inPath;
    job.outputPath = DcHelper.getBlobdirFile(op.dcContext, inPath);
    job.maxBytes = maxBytes;
    job.originalBytes = inBytes;
    job.durationMs = meta.durationMs;
    job.hasAudio = meta.hasAudio;
    job.dispW = meta.dispW;
    job.dispH = meta.dispH;
    job.fromComposer = fromComposer;
    job.compatOnly = forceCompat && !hard && !overSize;
    return new Decision(job);
  }

  private void postIngestProgress(Operation op) {
    mainHandler.post(
        () -> {
          if (op == current && !op.cancelled) {
            op.ingestFinished++;
            emitState();
          }
        });
  }

  private void postFinished(Operation op, int chatId, boolean fromComposer, Result result) {
    mainHandler.post(() -> dispatchFinished(op.accId, chatId, fromComposer, result));
  }

  private void onIngestFinished(Operation op, ArrayList<RecodeJob> jobs) {
    if (op != current || op.cancelled) {
      return;
    }
    if (jobs.isEmpty()) {
      finishOperation();
      return;
    }
    op.jobs.addAll(jobs);
    op.phase = RecodeState.Phase.RECODING;
    startNextJob();
  }

  private void startNextJob() {
    Operation op = current;
    if (op == null) {
      return;
    }
    if (op.cancelled || op.jobsFinished >= op.jobs.size()) {
      finishOperation();
      return;
    }
    RecodeJob next = op.jobs.get(op.jobsFinished);
    job = next;
    startPass(op, next, false);
    emitState();
  }

  private void startPass(Operation op, RecodeJob job, boolean low) {
    int[] wh = job.sizeForPass(low);
    PassCallback callback = new PassCallback(op, job, low, wh[0], wh[1]);
    VideoTranscoder t = new VideoTranscoder(context, callback);
    transcoder = t;
    try {
      t.start(
          job.inputPath, job.outputPath, wh[0], wh[1], job.videoBitrateForPass(low), job.hasAudio);
    } catch (Exception e) {
      Log.w(TAG, "could not start export", e);
      callback.onFailed(null);
    }
  }

  private final class PassCallback implements VideoTranscoder.Callback {
    private final Operation op;
    private final RecodeJob job;
    private final boolean low;
    private final int width;
    private final int height;

    PassCallback(Operation op, RecodeJob job, boolean low, int width, int height) {
      this.op = op;
      this.job = job;
      this.low = low;
      this.width = width;
      this.height = height;
    }

    private boolean isCurrent() {
      return op == current && job == VideoRecodeManager.this.job;
    }

    @Override
    public void onProgress(int progress) {
      if (!isCurrent()) {
        return;
      }
      // pass 1 0-60%, pass 2 (if needed) 60-100%
      job.lastProgress = low ? 60 + progress * 2 / 5 : progress * 3 / 5;
      emitState();
    }

    @Override
    public void onCompleted() {
      if (!isCurrent()) {
        return;
      }
      File out = new File(job.outputPath);
      long size = out.length();
      if (size <= 0) {
        handleFailed(op, job, null);
      } else if (size > job.maxBytes) {
        //noinspection ResultOfMethodCallIgnored
        out.delete();
        if (!low) {
          startPass(op, job, true);
        } else {
          finishJob(op, job, Result.FAILED_TOO_BIG);
        }
      } else {
        finishJob(op, job, sendRecoded(op, job, width, height) ? Result.SENT : Result.FAILED_ERROR);
      }
    }

    @Override
    public void onFailed(@Nullable ExportException error) {
      if (isCurrent()) {
        handleFailed(op, job, error);
      }
    }

    @Override
    public void onCancelled() {
      //noinspection ResultOfMethodCallIgnored
      new File(job.outputPath).delete();
      if (isCurrent()) {
        finishJob(op, job, Result.CANCELLED);
      }
    }
  }

  private void handleFailed(Operation op, RecodeJob job, @Nullable Exception error) {
    Log.w(TAG, "recode failed", error);
    //noinspection ResultOfMethodCallIgnored
    new File(job.outputPath).delete();
    if (job.originalBytes <= job.maxBytes) {
      job.msg.setDimension(job.dispW, job.dispH);
      job.msg.setDuration((int) job.durationMs);
      if (op.dcContext.sendMsg(job.chatId, job.msg) != 0) {
        finishJob(op, job, Result.SENT_ORIGINAL);
        return;
      }
      finishJob(op, job, Result.FAILED_ERROR);
      return;
    }
    finishJob(op, job, Result.FAILED_TOO_BIG);
  }

  private boolean sendRecoded(Operation op, RecodeJob job, int width, int height) {
    DcContext dc = op.dcContext;
    // output is always MP4/H.264 now, fix file name and mime type.
    // Also fixes https://github.com/deltachat/deltachat-android/issues/3928#issuecomment-4422383476
    String name = job.msg.getFilename();
    String base = TextUtils.isEmpty(name) ? "video" : name;
    int dot = base.lastIndexOf('.');
    if (dot > 0) {
      base = base.substring(0, dot);
    }
    job.msg.setFileAndDeduplicate(job.outputPath, base + ".mp4", "video/mp4");
    job.msg.setDimension(width, height);
    job.msg.setDuration((int) job.durationMs);
    int msgId = dc.sendMsg(job.chatId, job.msg);
    if (msgId == 0) {
      Log.w(TAG, "sendMsg failed: " + dc.getLastError());
      //noinspection ResultOfMethodCallIgnored
      new File(job.outputPath).delete();
      return false;
    }
    return true;
  }

  private void finishJob(Operation op, RecodeJob done, Result result) {
    job = null;
    transcoder = null;
    op.jobsFinished++;
    dispatchFinished(op.accId, done.chatId, done.fromComposer, result);
    startNextJob();
  }

  private void finishOperation() {
    current = null;
    job = null;
    transcoder = null;
    emitState();
  }

  private void dispatchFinished(int accountId, int chatId, boolean fromComposer, Result result) {
    boolean handled = false;
    for (Listener l : listeners) {
      handled |= l.onFinished(accountId, chatId, fromComposer, result);
    }
    if (!handled) {
      String text = null;
      if (result == Result.FAILED_TOO_BIG) {
        text = TOO_BIG_MSG;
      } else if (result == Result.SENT_ORIGINAL) {
        text = SENT_ORIGINAL_MSG;
      } else if (result == Result.FAILED_ERROR) {
        text = context.getString(R.string.error);
      } else if (result == Result.FAILED_READ) {
        text = READ_FAILED_MSG;
      }
      if (text != null) {
        VideoRecodeService.postOnce(context, text);
      }
    }
  }

  private void emitState() {
    state = buildState();
    for (Listener l : listeners) {
      l.onStateChanged(state);
    }
    VideoRecodeService.onStateChanged(context, state);
  }

  private RecodeState buildState() {
    Operation op = current;
    if (op == null) {
      return RecodeState.IDLE;
    }
    if (op.phase == RecodeState.Phase.INGEST) {
      int progress = op.ingestTotal > 0 ? op.ingestFinished * 100 / op.ingestTotal : 0;
      return new RecodeState(
          RecodeState.Phase.INGEST,
          op.accId,
          op.chatIds,
          op.ingestFinished,
          op.ingestTotal,
          progress);
    }
    RecodeJob active = job;
    return new RecodeState(
        RecodeState.Phase.RECODING,
        op.accId,
        op.chatIds,
        op.jobsFinished,
        op.jobs.size(),
        active != null ? active.lastProgress : 0);
  }

  private long maxBytesFor(DcContext dc) {
    try {
      String v = dc.getConfig("sys.msgsize_max_recommended");
      if (v != null && !v.isEmpty()) {
        return Long.parseLong(v);
      }
    } catch (Exception e) {
      Log.w(TAG, "sys.msgsize_max_recommended unreadable", e);
    }
    return FALLBACK_MAX_BYTES;
  }

  private static boolean isVp8OrVp9(@Nullable String mime) {
    return "video/x-vnd.on2.vp8".equals(mime) || "video/x-vnd.on2.vp9".equals(mime);
  }

  private static class VideoMeta {
    int width;
    int height;
    int rotation;
    int dispW;
    int dispH;
    long durationMs;
    long overallBitrate;
    boolean hasAudio;
    String videoMime;
  }

  @SuppressLint("InlinedApi")
  @Nullable
  private static VideoMeta probe(String path) {
    VideoMeta meta = new VideoMeta();
    boolean foundVideo = false;
    MediaExtractor extractor = new MediaExtractor();
    try {
      extractor.setDataSource(path);
      for (int i = 0; i < extractor.getTrackCount(); i++) {
        MediaFormat format = extractor.getTrackFormat(i);
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime == null) {
          continue;
        }
        if (mime.startsWith("video/")) {
          foundVideo = true;
          meta.videoMime = mime;
          meta.width = format.getInteger(MediaFormat.KEY_WIDTH);
          meta.height = format.getInteger(MediaFormat.KEY_HEIGHT);
          if (format.containsKey(MediaFormat.KEY_DURATION)) {
            meta.durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000;
          }
          if (format.containsKey(MediaFormat.KEY_ROTATION)) {
            meta.rotation = ((format.getInteger(MediaFormat.KEY_ROTATION) % 360) + 360) % 360;
          }
        } else if (mime.startsWith("audio/")) {
          meta.hasAudio = true;
          if (meta.durationMs == 0 && format.containsKey(MediaFormat.KEY_DURATION)) {
            meta.durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000;
          }
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "probe: cannot read " + path, e);
      return null;
    } finally {
      extractor.release();
    }
    if (!foundVideo || meta.width <= 0 || meta.height <= 0) {
      return null;
    }

    // noinspection resource
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      retriever.setDataSource(path);
      String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
      if (bitrate != null) {
        meta.overallBitrate = Long.parseLong(bitrate);
      }
      if (meta.durationMs == 0) {
        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (duration != null) {
          meta.durationMs = Long.parseLong(duration);
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "probe: retriever failed for " + path, e);
    } finally {
      try {
        retriever.release();
      } catch (Exception e) {
        Log.w(TAG, "probe: retriever release failed", e);
      }
    }

    if (meta.rotation == 90 || meta.rotation == 270) {
      meta.dispW = meta.height;
      meta.dispH = meta.width;
    } else {
      meta.dispW = meta.width;
      meta.dispH = meta.height;
    }
    return meta;
  }
}
