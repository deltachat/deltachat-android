package org.thoughtcrime.securesms.webxdc;

import android.content.Context;
import android.util.Log;
import android.webkit.WebStorage;
import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.thoughtcrime.securesms.connect.DcHelper;

public class WebxdcGarbageCollectionWorker extends ListenableWorker {
  private static final String TAG = WebxdcGarbageCollectionWorker.class.getSimpleName();
  private Context context;

  public WebxdcGarbageCollectionWorker(Context context, WorkerParameters params) {
    super(context, params);
    this.context = context;
  }

  @Override
  public @NonNull ListenableFuture<Result> startWork() {
    Log.i(TAG, "Running Webxdc storage garbage collection...");

    final Pattern WEBXDC_URL_PATTERN =
      Pattern.compile("^https?://acc(\\d+)-msg(\\d+)\\.localhost/?");

    return CallbackToFutureAdapter.getFuture(completer -> {
      WebStorage webStorage = WebStorage.getInstance();

      webStorage.getOrigins((origins) -> {
        if (origins == null || origins.isEmpty()) {
          Log.i(TAG, "Done, no WebView origins found.");
          completer.set(Result.success());
          return;
        }

        Rpc rpc = DcHelper.getRpc(context);
        if (rpc == null) {
            Log.e(TAG, "Failed to get access to RPC, Webxdc storage garbage collection aborted.");
            completer.set(Result.failure());
            return;
        }

        for (Object key : origins.keySet()) {
          String url = (String)key;
          Matcher m = WEBXDC_URL_PATTERN.matcher(url);
          if (m.matches()) {
            int accId = Integer.parseInt(m.group(1));
            int msgId = Integer.parseInt(m.group(2));
            try {
              if (rpc.getExistingMsgIds(accId, Collections.singletonList(msgId)).isEmpty()) {
                webStorage.deleteOrigin(url);
                Log.i(TAG, String.format("Deleted webxdc origin: %s", url));
              } else {
                Log.i(TAG, String.format("Existing webxdc origin: %s", url));
              }
            } catch (RpcException e) {
              Log.e(TAG, "error calling rpc.getExistingMsgIds()", e);
              completer.set(Result.failure());
              return;
            }
          } else { // old webxdc URL schemes, etc
            webStorage.deleteOrigin(url);
            Log.i(TAG, String.format("Deleted unknown origin: %s", url));
          }
        }

        Log.i(TAG, "Done running Webxdc storage garbage collection.");
        completer.set(Result.success());
      });

      return "Webxdc Garbage Collector";
    });
  }
}
