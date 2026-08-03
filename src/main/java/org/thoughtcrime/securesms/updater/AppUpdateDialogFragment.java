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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
  private static final String STATE_DOWNLOAD_ID = "download_id";

  private static final int POLL_INTERVAL_MS = 500;
  private static final String DEST_FILENAME = "deltachat-update.apk";

  private final Handler handler = new Handler(Looper.getMainLooper());

  private DownloadManager downloadManager;
  private long downloadId = -1;
  private File destFile;
  private String versionString;

  private ProgressBar progressBar;
  private TextView progressText;

  private Runnable pollRunnable;
  private boolean finished;

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
    setCancelable(false);

    Context appContext = requireContext().getApplicationContext();
    downloadManager = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
    versionString = requireArguments().getString(ARG_VERSION, "");

    File dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    if (dir == null || downloadManager == null) {
      Toast.makeText(appContext, R.string.error, Toast.LENGTH_LONG).show();
      dismissAllowingStateLoss();
      return;
    }
    destFile = new File(dir, DEST_FILENAME);

    if (savedInstanceState != null) {
      downloadId = savedInstanceState.getLong(STATE_DOWNLOAD_ID, -1);
    }
    if (downloadId == -1) {
      startDownload(appContext);
    }
  }

  private void startDownload(Context appContext) {
    if (destFile.exists()) {
      destFile.delete();
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
      Log.e(TAG, "Failed to start download", e);
      Toast.makeText(appContext, R.string.error, Toast.LENGTH_LONG).show();
      dismissAllowingStateLoss();
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = requireActivity().getLayoutInflater().inflate(R.layout.app_update_dialog, null);
    progressBar = view.findViewById(R.id.update_progress_bar);
    progressText = view.findViewById(R.id.update_progress_text);

    return new AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.update_downloading, versionString))
        .setView(view)
        .setNegativeButton(R.string.cancel, (d, w) -> cancelDownload())
        .setCancelable(false)
        .create();
  }

  @Override
  public void onResume() {
    super.onResume();
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
    outState.putLong(STATE_DOWNLOAD_ID, downloadId);
  }

  private void startPolling() {
    stopPolling();
    if (finished || downloadId == -1) return;
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
    if (finished || downloadId == -1) return;
    Cursor cursor = null;
    try {
      cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
      if (cursor == null || !cursor.moveToFirst()) {
        return;
      }
      int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
      if (status == DownloadManager.STATUS_SUCCESSFUL) {
        String localUri =
            cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
        String localPath = localUri != null ? Uri.parse(localUri).getPath() : null;
        if (localPath == null) {
          onDownloadFailed();
        } else {
          onDownloadSucceeded(localPath);
        }
        return;
      }
      long downloaded =
          cursor.getLong(
              cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
      long total =
          cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
      updateProgress(downloaded, total);
    } catch (Exception e) {
      Log.e(TAG, "poll failed", e);
    } finally {
      if (cursor != null) cursor.close();
    }
    handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
  }

  private void updateProgress(long downloaded, long total) {
    if (progressBar == null) return;
    if (total > 0) {
      int percent = (int) (downloaded * 100 / total);
      progressBar.setIndeterminate(false);
      progressBar.setMax(100);
      progressBar.setProgress(percent);
      if (progressText != null) {
        progressText.setText(getString(R.string.progress_percent, percent));
      }
    } else {
      progressBar.setIndeterminate(true);
      if (progressText != null) {
        progressText.setText("");
      }
    }
  }

  private void onDownloadSucceeded(String localPath) {
    if (finished) return;
    finished = true;
    stopPolling();

    Context context = getContext();
    if (context == null) return;

    PackageInfo info = context.getPackageManager().getPackageArchiveInfo(localPath, 0);
    if (info == null || !context.getPackageName().equals(info.packageName)) {
      Toast.makeText(context, R.string.update_apk_mismatch, Toast.LENGTH_LONG).show();
      new File(localPath).delete();
      dismissAllowingStateLoss();
      return;
    }

    Uri apkUri = downloadManager.getUriForDownloadedFile(downloadId);
    Activity activity = getActivity();
    if (activity != null && apkUri != null) {
      DcHelper.installApk(activity, apkUri);
    }
    dismissAllowingStateLoss();
  }

  private void onDownloadFailed() {
    if (finished) return;
    finished = true;
    stopPolling();
    Context context = getContext();
    if (context != null) {
      Toast.makeText(context, R.string.download_failed, Toast.LENGTH_LONG).show();
    }
    dismissAllowingStateLoss();
  }

  private void cancelDownload() {
    finished = true;
    stopPolling();
    if (downloadId != -1) {
      downloadManager.remove(downloadId);
      downloadId = -1;
    }
  }

  @Override
  public void onDestroy() {
    stopPolling();
    super.onDestroy();
  }
}
