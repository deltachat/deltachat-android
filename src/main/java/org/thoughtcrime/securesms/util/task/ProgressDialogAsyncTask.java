package org.thoughtcrime.securesms.util.task;

import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.util.views.ProgressDialog;

import java.lang.ref.WeakReference;

public abstract class ProgressDialogAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

  private final WeakReference<Context> contextReference;
  private       ProgressDialog         progress;
  private final String                 title;
  private final String                 message;
  private       boolean                cancelable;
  private       OnCancelListener       onCancelListener;

  public ProgressDialogAsyncTask(Context context, String title, String message) {
    super();
    this.contextReference = new WeakReference<>(context);
    this.title            = title;
    this.message          = message;
  }

  public void setCancelable(@Nullable OnCancelListener onCancelListener) {
    this.cancelable = true;
    this.onCancelListener = onCancelListener;
  }

  @Override
  protected void onPreExecute() {
    final Context context = contextReference.get();
    if (context != null) {
      progress = ProgressDialog.show(context, title, message, true, cancelable, onCancelListener);
    }
  }

  @Override
  protected void onPostExecute(Result result) {
    try {
      if (progress != null) progress.dismiss();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  protected Context getContext() {
    return contextReference.get();
  }
}

