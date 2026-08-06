package org.thoughtcrime.securesms.updater;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import java.io.File;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcHelper;

public class AppUpdateDialogFragment extends DialogFragment {
  private static final String TAG = "AppUpdateDialog";

  private static final String ARG_URL = "url";
  private static final String ARG_VERSION = "version";
  private static final String KEY_STATE = "state";
  private static final String KEY_DOWNLOAD_ID = "download_id";
  private static final String KEY_ERROR_RES = "error_res";

  private enum State {
    CHECK_PERMISSION,
    PERMISSION_NEEDED,
    DOWNLOADING,
    ERROR
  }

  private static final int POLL_INTERVAL_MS = 500;
  private static final String DEST_FILENAME = "deltachat-update.apk";

  private final Handler handler = new Handler(Looper.getMainLooper());

  private DownloadManager downloadManager;
  private String versionString;

  private State state = State.CHECK_PERMISSION;
  private long downloadId = -1;
  private @StringRes int errorRes;

  private TextView messageText;
  private View progressContainer;
  private ProgressBar progressBar;
  private TextView progressText;

  private Runnable pollRunnable;

  public static void show(FragmentActivity activity, AppUpdate.LatestVersion latest) {
    AppUpdateDialogFragment f = new AppUpdateDialogFragment();
    Bundle args = new Bundle();
    args.putString(ARG_URL, latest.downloadUrl);
    args.putString(ARG_VERSION, latest.versionString);
    f.setArguments(args);
    f.show(activity.getSupportFragmentManager(), "app_update_dialog");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    downloadManager =
        (DownloadManager)
            requireContext().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
    versionString = requireArguments().getString(ARG_VERSION, "");

    if (savedInstanceState != null) {
      state = stateFromName(savedInstanceState.getString(KEY_STATE));
      downloadId = savedInstanceState.getLong(KEY_DOWNLOAD_ID, -1);
      errorRes = savedInstanceState.getInt(KEY_ERROR_RES, 0);
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = requireActivity().getLayoutInflater().inflate(R.layout.app_update_dialog, null);
    messageText = view.findViewById(R.id.update_message_text);
    progressContainer = view.findViewById(R.id.update_progress_container);
    progressBar = view.findViewById(R.id.update_progress_bar);
    progressText = view.findViewById(R.id.update_progress_text);

    return new AlertDialog.Builder(requireContext())
        .setView(view)
        .setPositiveButton(R.string.ok, null)
        .setNegativeButton(R.string.cancel, null)
        .create();
  }

  @Override
  public void onStart() {
    super.onStart();
    render();
  }

  @Override
  public void onResume() {
    super.onResume();
    advanceState();
    render();
    startPolling();
  }

  @Override
  public void onPause() {
    super.onPause();
    stopPolling();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(KEY_STATE, state.name());
    outState.putLong(KEY_DOWNLOAD_ID, downloadId);
    outState.putInt(KEY_ERROR_RES, errorRes);
  }

  @Override
  public void onDestroy() {
    stopPolling();
    super.onDestroy();
  }

  private void advanceState() {
    if (state != State.CHECK_PERMISSION && state != State.PERMISSION_NEEDED) return;

    Activity activity = getActivity();
    if (activity == null) return;

    if (!DcHelper.canInstallApks(activity)) {
      state = State.PERMISSION_NEEDED;
      return;
    }
    state = State.DOWNLOADING;
    if (downloadId == -1) {
      startDownload();
    }
  }

  private void startDownload() {
    Context appContext = requireContext().getApplicationContext();
    File dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    if (dir == null || downloadManager == null) {
      showError(R.string.download_failed);
      return;
    }

    File stale = new File(dir, DEST_FILENAME);
    if (stale.exists() && !stale.delete()) {
      Log.w(TAG, "could not delete file " + stale);
    }

    try {
      DownloadManager.Request request =
          new DownloadManager.Request(Uri.parse(requireArguments().getString(ARG_URL)));
      request.setTitle(getString(R.string.update_downloading, versionString));
      request.setDestinationInExternalFilesDir(
          appContext, Environment.DIRECTORY_DOWNLOADS, DEST_FILENAME);
      request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
      downloadId = downloadManager.enqueue(request);
    } catch (Exception e) {
      Log.e(TAG, "failed to start download", e);
      showError(R.string.download_failed);
    }
  }

  private void render() {
    AlertDialog dialog = (AlertDialog) getDialog();
    if (dialog == null) return;
    Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
    if (positive == null || negative == null) return;

    setCancelable(state == State.PERMISSION_NEEDED || state == State.ERROR);

    switch (state) {
      case PERMISSION_NEEDED:
        messageText.setVisibility(View.VISIBLE);
        messageText.setText(R.string.update_install_permission_explain);
        progressContainer.setVisibility(View.GONE);
        positive.setVisibility(View.VISIBLE);
        positive.setText(R.string.open_settings);
        positive.setOnClickListener(
            v -> {
              Activity activity = getActivity();
              if (activity != null) {
                DcHelper.requestInstallApksPermission(activity);
              }
            });
        negative.setVisibility(View.VISIBLE);
        negative.setText(R.string.cancel);
        negative.setOnClickListener(v -> dismissAllowingStateLoss());
        break;

      case DOWNLOADING:
        messageText.setVisibility(View.VISIBLE);
        messageText.setText(getString(R.string.update_downloading, versionString));
        progressContainer.setVisibility(View.VISIBLE);
        positive.setVisibility(View.GONE);
        negative.setVisibility(View.VISIBLE);
        negative.setText(R.string.cancel);
        negative.setOnClickListener(
            v -> {
              cancelDownload();
              dismissAllowingStateLoss();
            });
        break;

      case ERROR:
        messageText.setVisibility(View.VISIBLE);
        messageText.setText(errorRes != 0 ? errorRes : R.string.error);
        progressContainer.setVisibility(View.GONE);
        positive.setVisibility(View.VISIBLE);
        positive.setText(R.string.ok);
        positive.setOnClickListener(v -> dismissAllowingStateLoss());
        negative.setVisibility(View.GONE);
        break;

      default:
        messageText.setVisibility(View.GONE);
        progressContainer.setVisibility(View.GONE);
        positive.setVisibility(View.GONE);
        negative.setVisibility(View.GONE);
        break;
    }
  }

  private void showError(@StringRes int messageRes) {
    stopPolling();
    state = State.ERROR;
    errorRes = messageRes;
    render();
  }

  private void startPolling() {
    stopPolling();
    if (state != State.DOWNLOADING || downloadId == -1) return;
    pollRunnable = this::poll;
    handler.post(pollRunnable);
  }

  private void stopPolling() {
    if (pollRunnable != null) {
      handler.removeCallbacks(pollRunnable);
      pollRunnable = null;
    }
  }

  private void poll() {
    if (state != State.DOWNLOADING || downloadId == -1) return;
    Cursor cursor = null;
    try {
      cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
      if (cursor != null && cursor.moveToFirst()) {
        int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
          String localUri =
              cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
          String localPath = localUri != null ? Uri.parse(localUri).getPath() : null;
          if (localPath == null) {
            showError(R.string.download_failed);
          } else {
            onDownloadSucceeded(localPath);
          }
          return;
        } else if (status == DownloadManager.STATUS_FAILED) {
          showError(R.string.download_failed);
          return;
        }
        updateProgress(
            cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
      }
    } catch (Exception e) {
      Log.e(TAG, "poll failed", e);
    } finally {
      if (cursor != null) cursor.close();
    }

    if (pollRunnable != null) {
      handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }
  }

  private void updateProgress(long downloaded, long total) {
    if (progressBar == null) return;
    if (total > 0) {
      int percent = (int) (downloaded * 100 / total);
      progressBar.setIndeterminate(false);
      progressBar.setMax(100);
      progressBar.setProgress(percent);
      progressText.setText(getString(R.string.progress_percent, percent));
    } else {
      progressBar.setIndeterminate(true);
      progressText.setText("");
    }
  }

  private void onDownloadSucceeded(String localPath) {
    stopPolling();

    Context context = getContext();
    Activity activity = getActivity();
    if (context == null || activity == null) return;

    PackageInfo info = context.getPackageManager().getPackageArchiveInfo(localPath, 0);
    if (info == null || !context.getPackageName().equals(info.packageName)) {
      new File(localPath).delete();
      showError(R.string.update_apk_mismatch);
      return;
    }

    Uri apkUri = downloadManager.getUriForDownloadedFile(downloadId);
    if (apkUri == null) {
      showError(R.string.download_failed);
      return;
    }
    DcHelper.installApk(activity, apkUri);
    dismissAllowingStateLoss();
  }

  private void cancelDownload() {
    stopPolling();
    if (downloadId != -1) {
      downloadManager.remove(downloadId);
      downloadId = -1;
    }
  }

  private static State stateFromName(String name) {
    if (name != null) {
      try {
        return State.valueOf(name);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return State.CHECK_PERMISSION;
  }
}
