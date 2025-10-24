package org.thoughtcrime.securesms.preferences;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.service.GenericForegroundService;
import org.thoughtcrime.securesms.service.NotificationController;
import org.thoughtcrime.securesms.util.ScreenLockUtil;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class ListSummaryPreferenceFragment extends CorrectedPreferenceFragment implements DcEventCenter.DcEventDelegate {
  protected static final int REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP = ScreenLockUtil.REQUEST_CODE_CONFIRM_CREDENTIALS + 1;
  protected static final int REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS = REQUEST_CODE_CONFIRM_CREDENTIALS_BACKUP + 1;
  protected static final int REQUEST_CODE_CONFIRM_CREDENTIALS_ACCOUNT = REQUEST_CODE_CONFIRM_CREDENTIALS_KEYS + 1;
  protected DcContext dcContext;
  private NotificationController notificationController;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    dcContext = DcHelper.getContext(getContext());
    DcHelper.getEventCenter(getContext()).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this);
  }

  @Override
  public void onDestroy() {
    DcHelper.getEventCenter(getContext()).removeObservers(this);

    NotificationController notifController = notificationController;
    if (notifController != null) {
      // cancel backup when settings-activity is destroyed.
      //
      // where possible, we avoid the settings-activity from being destroyed,
      // however, i did not find a simple way to cancel ConversationListActivity.onNewIntent() -
      // which one is cleaning up "back stack" due to the singleTask flag.
      // using a dummy activity and several workarounds all result even in worse side-effects
      // than cancel-backup when the user relaunches the app.
      // maybe we could bear the singleTask flag or could decouple
      // backup completely from ui-flows -
      // however, all this is some work and probably not maybe the effort just now.
      //
      // anyway, normally, the backup is fast enough and the users will just wait.
      // btw, import does not have this issue (no singleTask in play there)
      // and also for export, switching to other apps and tapping the notification will work.
      // so, the current state is not that bad :)
      notifController.close();
      notificationController = null;
      stopOngoingProcess();
      Toast.makeText(getActivity(), R.string.export_aborted, Toast.LENGTH_LONG).show();
    }

    super.onDestroy();
  }

  protected class ListSummaryListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object value) {
      updateListSummary(preference, value);
      return true;
    }
  }

  protected String getSelectedSummary(Preference preference, Object value) {
    ListPreference listPref = (ListPreference) preference;
    int entryIndex = Arrays.asList(listPref.getEntryValues()).indexOf(value);
    return entryIndex >= 0 && entryIndex < listPref.getEntries().length
            ? listPref.getEntries()[entryIndex].toString()
            : getString(R.string.unknown);
  }

  protected void updateListSummary(Preference preference, Object value) {
    updateListSummary(preference, value, null);
  }

  protected void updateListSummary(Preference preference, Object value, String hint) {
    ListPreference listPref = (ListPreference) preference;
    String summary = getSelectedSummary(preference, value);
    if (hint != null) {
      summary += "\n\n" + hint;
    }
    listPref.setSummary(summary);
  }

  protected void initializeListSummary(ListPreference pref) {
    pref.setSummary(pref.getEntry());
  }

  private Map<Integer, Integer> imexProgress;
  protected int[] imexAccounts;
  protected int accountsDone;

  protected void startImexAll(int what) {
    imexAccounts = DcHelper.getAccounts(getActivity()).getAll();
    imexProgress = new HashMap<>();
    accountsDone = 0;
    showProgressDialog();
    String path = DcHelper.getImexDir().getAbsolutePath();
    for (int i = 0; i < imexAccounts.length; i++) {
      startImexInner(imexAccounts[i], what, path, path);
    }
  }

  protected void startImexOne(int what)
  {
    String path = DcHelper.getImexDir().getAbsolutePath();
    startImexOne(what, path, path);
  }

  protected void startImexOne(int what, String imexPath, String pathAsDisplayedToUser) {
    imexAccounts = new int[]{ dcContext.getAccountId() };
    imexProgress = new HashMap<>();
    accountsDone = 0;
    showProgressDialog();
    startImexInner(imexAccounts[0], what, imexPath, pathAsDisplayedToUser);
  }

  protected ProgressDialog progressDialog = null;
  protected int            progressWhat = 0;
  protected String         pathAsDisplayedToUser = "";
  protected void startImexInner(int accountId, int what, String imexPath, String pathAsDisplayedToUser)
  {
    DcContext dcContext = DcHelper.getAccounts(getActivity()).getAccount(accountId);
    this.pathAsDisplayedToUser = pathAsDisplayedToUser;
    progressWhat = what;
    dcContext.imex(progressWhat, imexPath);
  }

  private void stopOngoingProcess() {
    for (int accId : imexAccounts) {
      DcHelper.getAccounts(requireActivity()).getAccount(accId).stopOngoingProcess();
    }
  }

  private void showProgressDialog() {
    notificationController = GenericForegroundService.startForegroundTask(getContext(), getString(R.string.export_backup_desktop));
    if( progressDialog!=null ) {
      progressDialog.dismiss();
      progressDialog = null;
    }
    progressDialog = new ProgressDialog(getActivity());
    progressDialog.setMessage(getActivity().getString(R.string.one_moment));
    progressDialog.setCanceledOnTouchOutside(false);
    progressDialog.setCancelable(false);
    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getActivity().getString(android.R.string.cancel), (dialog, which) -> {
      notificationController.close();
      notificationController = null;
      stopOngoingProcess();
    });
    progressDialog.show();
  }

  private int getTotalProgress() {
    int progress = 0;
    for (Integer accProgress : imexProgress.values()) {
      progress += accProgress;
    }
    return progress;
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    if (event.getId()== DcContext.DC_EVENT_IMEX_PROGRESS) {
      NotificationController notifController = notificationController;
      if (notifController == null) return;

      long progress = event.getData1Int();
      Context context = getActivity();
      if (progress==0/*error/aborted*/) {
        notifController.close();
        notificationController = null;
        stopOngoingProcess();
        progressDialog.dismiss();
        progressDialog = null;
        DcContext dcContext = DcHelper.getAccounts(context).getAccount(event.getAccountId());
        new AlertDialog.Builder(context)
          .setMessage(dcContext.getLastError())
          .setPositiveButton(android.R.string.ok, null)
          .show();
      }
      else if (progress<1000/*progress in permille*/) {
        imexProgress.put(event.getAccountId(), (int) progress);
        int totalProgress = getTotalProgress();
        int percent = totalProgress / (10 * imexAccounts.length);
        String formattedPercent = percent > 0 ? String.format(" %d%%", percent) : "";
        progressDialog.setMessage(getResources().getString(R.string.one_moment) + formattedPercent);
        notifController.setProgress(1000L * imexAccounts.length, totalProgress, formattedPercent);
      }
      else if (progress==1000/*done*/) {
        accountsDone++;
        if (accountsDone == imexAccounts.length) {
          notifController.close();
          notificationController = null;
          progressDialog.dismiss();
          progressDialog = null;
          new AlertDialog.Builder(context)
            .setMessage(context.getString(R.string.pref_backup_written_to_x, pathAsDisplayedToUser))
            .setPositiveButton(android.R.string.ok, null)
            .show();
        }
      }
    }
  }

}
