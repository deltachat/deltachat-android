package org.thoughtcrime.securesms.connect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.ApplicationContext;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.LocalHelpActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.ShareActivity;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.qr.QrActivity;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.FileUtils;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.ShareUtil;
import org.thoughtcrime.securesms.util.Util;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;

public class DcHelper {

    private static final String TAG = DcHelper.class.getSimpleName();

    public static final String CONFIG_CONFIGURED_ADDRESS = "configured_addr";
    public static final String CONFIG_DISPLAY_NAME = "displayname";
    public static final String CONFIG_SELF_STATUS = "selfstatus";
    public static final String CONFIG_SELF_AVATAR = "selfavatar";
    public static final String CONFIG_MVBOX_MOVE = "mvbox_move";
    public static final String CONFIG_ONLY_FETCH_MVBOX = "only_fetch_mvbox";
    public static final String CONFIG_BCC_SELF = "bcc_self";
    public static final String CONFIG_SHOW_EMAILS = "show_emails";
    public static final String CONFIG_MEDIA_QUALITY = "media_quality";
    public static final String CONFIG_PROXY_ENABLED = "proxy_enabled";
    public static final String CONFIG_PROXY_URL = "proxy_url";
    public static final String CONFIG_WEBXDC_REALTIME_ENABLED = "webxdc_realtime_enabled";
    public static final String CONFIG_PRIVATE_TAG = "private_tag";
    public static final String CONFIG_STATS_SENDING = "stats_sending";
    public static final String CONFIG_STATS_ID = "stats_id";

    public static DcContext getContext(@NonNull Context context) {
        return ApplicationContext.getInstance(context).getDcContext();
    }

    public static Rpc getRpc(@NonNull Context context) {
        return ApplicationContext.getInstance(context).getRpc();
    }

    public static DcAccounts getAccounts(@NonNull Context context) {
        return ApplicationContext.getInstance(context).getDcAccounts();
    }

    public static DcEventCenter getEventCenter(@NonNull Context context) {
        return ApplicationContext.getInstance(context).eventCenter;
    }

    public static NotificationCenter getNotificationCenter(@NonNull Context context) {
        return ApplicationContext.getInstance(context).notificationCenter;
    }

    public static boolean isConfigured(Context context) {
        DcContext dcContext = getContext(context);
        return dcContext.isConfigured() == 1;
    }

    public static int getInt(Context context, String key) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfigInt(key);
    }

    public static String get(Context context, String key) {
        DcContext dcContext = getContext(context);
        return dcContext.getConfig(key);
    }

    @Deprecated public static int getInt(Context context, String key, int defaultValue) {
        return getInt(context, key);
    }

    @Deprecated public static String get(Context context, String key, String defaultValue) {
        return get(context, key);
    }

    public static void set(Context context, String key, String value) {
        DcContext dcContext = getContext(context);
        dcContext.setConfig(key, value);
    }

  public static void setStockTranslations(Context context) {
    DcContext dcContext = getContext(context);
    // the integers are defined in the core and used only here, an enum or sth. like that won't have a big benefit
    dcContext.setStockTranslation(1, context.getString(R.string.chat_no_messages));
    dcContext.setStockTranslation(2, context.getString(R.string.self));
    dcContext.setStockTranslation(3, context.getString(R.string.draft));
    dcContext.setStockTranslation(7, context.getString(R.string.voice_message));
    dcContext.setStockTranslation(9, context.getString(R.string.image));
    dcContext.setStockTranslation(10, context.getString(R.string.video));
    dcContext.setStockTranslation(11, context.getString(R.string.audio));
    dcContext.setStockTranslation(12, context.getString(R.string.file));
    dcContext.setStockTranslation(23, context.getString(R.string.gif));
    dcContext.setStockTranslation(35, context.getString(R.string.contact_verified));
    dcContext.setStockTranslation(40, context.getString(R.string.chat_archived_label));
    dcContext.setStockTranslation(60, context.getString(R.string.login_error_cannot_login));
    dcContext.setStockTranslation(66, context.getString(R.string.location));
    dcContext.setStockTranslation(67, context.getString(R.string.sticker));
    dcContext.setStockTranslation(68, context.getString(R.string.device_talk));
    dcContext.setStockTranslation(69, context.getString(R.string.saved_messages));
    dcContext.setStockTranslation(70, context.getString(R.string.device_talk_explain));
    dcContext.setStockTranslation(71, context.getString(R.string.device_talk_welcome_message2));
    dcContext.setStockTranslation(73, context.getString(R.string.systemmsg_subject_for_new_contact));
    dcContext.setStockTranslation(74, context.getString(R.string.systemmsg_failed_sending_to));
    dcContext.setStockTranslation(84, context.getString(R.string.configuration_failed_with_error));
    dcContext.setStockTranslation(85, context.getString(R.string.devicemsg_bad_time));
    dcContext.setStockTranslation(86, context.getString(R.string.devicemsg_update_reminder));
    dcContext.setStockTranslation(90, context.getString(R.string.reply_noun));
    dcContext.setStockTranslation(91, context.getString(R.string.devicemsg_self_deleted));
    dcContext.setStockTranslation(97, context.getString(R.string.forwarded));
    dcContext.setStockTranslation(98, context.getString(R.string.devicemsg_storage_exceeding));
    dcContext.setStockTranslation(99, context.getString(R.string.n_bytes_message));
    dcContext.setStockTranslation(100, context.getString(R.string.download_max_available_until));
    dcContext.setStockTranslation(103, context.getString(R.string.incoming_messages));
    dcContext.setStockTranslation(104, context.getString(R.string.outgoing_messages));
    dcContext.setStockTranslation(105, context.getString(R.string.storage_on_domain));
    dcContext.setStockTranslation(107, context.getString(R.string.connectivity_connected));
    dcContext.setStockTranslation(108, context.getString(R.string.connectivity_connecting));
    dcContext.setStockTranslation(109, context.getString(R.string.connectivity_updating));
    dcContext.setStockTranslation(110, context.getString(R.string.sending));
    dcContext.setStockTranslation(111, context.getString(R.string.last_msg_sent_successfully));
    dcContext.setStockTranslation(112, context.getString(R.string.error_x));
    dcContext.setStockTranslation(113, context.getString(R.string.not_supported_by_provider));
    dcContext.setStockTranslation(114, context.getString(R.string.messages));
    dcContext.setStockTranslation(116, context.getString(R.string.part_of_total_used));
    dcContext.setStockTranslation(117, context.getString(R.string.secure_join_started));
    dcContext.setStockTranslation(118, context.getString(R.string.secure_join_replies));
    dcContext.setStockTranslation(119, context.getString(R.string.qrshow_join_contact_hint));

    // HACK: svg does not handle entities correctly and shows `&quot;` as the text `quot;`.
    // until that is fixed, we fix the most obvious errors (core uses encode_minimal, so this does not affect so many characters)
    // cmp. https://github.com/deltachat/deltachat-android/issues/2187
    dcContext.setStockTranslation(120, context.getString(R.string.qrshow_join_group_hint).replace("\"", ""));
    dcContext.setStockTranslation(121, context.getString(R.string.connectivity_not_connected));
    dcContext.setStockTranslation(124, context.getString(R.string.group_name_changed_by_you));
    dcContext.setStockTranslation(125, context.getString(R.string.group_name_changed_by_other));
    dcContext.setStockTranslation(126, context.getString(R.string.group_image_changed_by_you));
    dcContext.setStockTranslation(127, context.getString(R.string.group_image_changed_by_other));
    dcContext.setStockTranslation(128, context.getString(R.string.add_member_by_you));
    dcContext.setStockTranslation(129, context.getString(R.string.add_member_by_other));
    dcContext.setStockTranslation(130, context.getString(R.string.remove_member_by_you));
    dcContext.setStockTranslation(131, context.getString(R.string.remove_member_by_other));
    dcContext.setStockTranslation(132, context.getString(R.string.group_left_by_you));
    dcContext.setStockTranslation(133, context.getString(R.string.group_left_by_other));
    dcContext.setStockTranslation(134, context.getString(R.string.group_image_deleted_by_you));
    dcContext.setStockTranslation(135, context.getString(R.string.group_image_deleted_by_other));
    dcContext.setStockTranslation(136, context.getString(R.string.location_enabled_by_you));
    dcContext.setStockTranslation(137, context.getString(R.string.location_enabled_by_other));
    dcContext.setStockTranslation(138, context.getString(R.string.ephemeral_timer_disabled_by_you));
    dcContext.setStockTranslation(139, context.getString(R.string.ephemeral_timer_disabled_by_other));
    dcContext.setStockTranslation(140, context.getString(R.string.ephemeral_timer_seconds_by_you));
    dcContext.setStockTranslation(141, context.getString(R.string.ephemeral_timer_seconds_by_other));
    dcContext.setStockTranslation(144, context.getString(R.string.ephemeral_timer_1_hour_by_you));
    dcContext.setStockTranslation(145, context.getString(R.string.ephemeral_timer_1_hour_by_other));
    dcContext.setStockTranslation(146, context.getString(R.string.ephemeral_timer_1_day_by_you));
    dcContext.setStockTranslation(147, context.getString(R.string.ephemeral_timer_1_day_by_other));
    dcContext.setStockTranslation(148, context.getString(R.string.ephemeral_timer_1_week_by_you));
    dcContext.setStockTranslation(149, context.getString(R.string.ephemeral_timer_1_week_by_other));
    dcContext.setStockTranslation(150, context.getString(R.string.ephemeral_timer_minutes_by_you));
    dcContext.setStockTranslation(151, context.getString(R.string.ephemeral_timer_minutes_by_other));
    dcContext.setStockTranslation(152, context.getString(R.string.ephemeral_timer_hours_by_you));
    dcContext.setStockTranslation(153, context.getString(R.string.ephemeral_timer_hours_by_other));
    dcContext.setStockTranslation(154, context.getString(R.string.ephemeral_timer_days_by_you));
    dcContext.setStockTranslation(155, context.getString(R.string.ephemeral_timer_days_by_other));
    dcContext.setStockTranslation(156, context.getString(R.string.ephemeral_timer_weeks_by_you));
    dcContext.setStockTranslation(157, context.getString(R.string.ephemeral_timer_weeks_by_other));
    dcContext.setStockTranslation(158, context.getString(R.string.ephemeral_timer_1_year_by_you));
    dcContext.setStockTranslation(159, context.getString(R.string.ephemeral_timer_1_year_by_other));
    dcContext.setStockTranslation(162, context.getString(R.string.multidevice_qr_subtitle));
    dcContext.setStockTranslation(163, context.getString(R.string.multidevice_transfer_done_devicemsg));
    dcContext.setStockTranslation(170, context.getString(R.string.chat_protection_enabled_tap_to_learn_more));
    dcContext.setStockTranslation(172, context.getString(R.string.chat_new_group_hint));
    dcContext.setStockTranslation(173, context.getString(R.string.member_x_added));
    dcContext.setStockTranslation(174, context.getString(R.string.invalid_unencrypted_tap_to_learn_more));
    dcContext.setStockTranslation(176, context.getString(R.string.reaction_by_you));
    dcContext.setStockTranslation(177, context.getString(R.string.reaction_by_other));
    dcContext.setStockTranslation(178, context.getString(R.string.member_x_removed));
    dcContext.setStockTranslation(190, context.getString(R.string.secure_join_wait));
    dcContext.setStockTranslation(193, context.getString(R.string.donate_device_msg));
    dcContext.setStockTranslation(194, context.getString(R.string.outgoing_call));
    dcContext.setStockTranslation(195, context.getString(R.string.incoming_call));
    dcContext.setStockTranslation(196, context.getString(R.string.declined_call));
    dcContext.setStockTranslation(197, context.getString(R.string.canceled_call));
    dcContext.setStockTranslation(198, context.getString(R.string.missed_call));
    dcContext.setStockTranslation(200, context.getString(R.string.channel_left_by_you));
    dcContext.setStockTranslation(201, context.getString(R.string.qrshow_join_channel_hint));
    dcContext.setStockTranslation(202, context.getString(R.string.you_joined_the_channel));
    dcContext.setStockTranslation(203, context.getString(R.string.secure_join_channel_started));
    dcContext.setStockTranslation(210, context.getString(R.string.stats_msg_body));
    dcContext.setStockTranslation(220, context.getString(R.string.proxy_enabled));
    dcContext.setStockTranslation(221, context.getString(R.string.proxy_enabled_hint));
    dcContext.setStockTranslation(230, context.getString(R.string.chat_unencrypted_explanation));
  }

  public static File getImexDir() {
    // DIRECTORY_DOCUMENTS could be used but DIRECTORY_DOWNLOADS seems to be easier accessible by the user,
    // eg. "Download Managers" are nearly always installed.
    // CAVE: do not use DownloadManager to add the file as it is deleted on uninstall then ...
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
  }

  // When the user shares a file to another app or opens a file in another app, it is added here.
  // `HashMap<file, mimetype>` where `file` is the name of the file in the blobdir (not the user-visible filename).
  public static final HashMap<String, String> sharedFiles = new HashMap<>();

  public static void openForViewOrShare(Context activity, int msg_id, String cmd) {
    DcContext dcContext = getContext(activity);

    if(!(activity instanceof Activity)) {
      // would be nicer to accepting only Activity objects,
      // however, typically in Android just Context objects are passed around (as this normally does not make a difference).
      // Accepting only Activity here would force callers to cast, which would easily result in even more ugliness.
      Toast.makeText(activity, "openForViewOrShare() expects an Activity object", Toast.LENGTH_LONG).show();
      return;
    }

    DcMsg msg = dcContext.getMsg(msg_id);
    String path = msg.getFile();
    String filename = msg.getFilename();
    String mimeType = msg.getFilemime();
    try {
      File file = new File(path);
      if (!file.exists()) {
        Toast.makeText(activity, activity.getString(R.string.file_not_found, path), Toast.LENGTH_LONG).show();
        return;
      }

      Uri uri;
      mimeType = checkMime(filename, mimeType);
      if (path.startsWith(dcContext.getBlobdir())) {
        // Build a Uri that will later be passed to AttachmentsContentProvider.openFile().
        // The last part needs to be `filename`, i.e. the original, user-visible name of the file,
        // so that the external apps show the name of the file correctly.
        uri = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".attachments/" + Uri.encode(file.getName()) + "/" + Uri.encode(filename));

        // As different Android version handle uris in putExtra differently,
        // we also check on our own that the file was actually shared.
        // The check happens in AttachmentsContentProvider.openFile().
        sharedFiles.put(file.getName(), mimeType);
      } else {
        if (Build.VERSION.SDK_INT >= 24) {
          uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file);
        } else {
          uri = Uri.fromFile(file);
        }
      }

      if (cmd.equals(Intent.ACTION_VIEW)) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity((Activity) activity, intent);
      } else {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, msg.getText());
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.chat_share_with_title)));
      }
    } catch (RuntimeException e) {
      Toast.makeText(activity, String.format("%s (%s)", activity.getString(R.string.no_app_to_handle_data), mimeType), Toast.LENGTH_LONG).show();
      Log.i(TAG, "opening of external activity failed.", e);
    }
  }

  public static void sendToChat(Context activity, byte[] data, String mimeType, String fileName, String text) {
      Intent intent = new Intent(activity, ShareActivity.class);
      intent.setAction(Intent.ACTION_SEND);
      ShareUtil.setIsFromWebxdc(intent, true);

      if (data != null) {
          Uri uri = PersistentBlobProvider.getInstance().create(activity, data, mimeType, fileName);
          intent.setType(mimeType);
          intent.putExtra(Intent.EXTRA_STREAM, uri);
          ShareUtil.setSharedTitle(intent, activity.getString(R.string.send_file_to, fileName));
      }

      if (text != null) {
          intent.putExtra(Intent.EXTRA_TEXT, text);
          if (data == null) {
              ShareUtil.setSharedTitle(intent, activity.getString(R.string.send_message_to));
          }
      }

      activity.startActivity(intent);
  }

  private static void startActivity(Activity activity, Intent intent) {
    // request for permission to install apks on API 26+ if intent mimetype is an apk
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
      "application/vnd.android.package-archive".equals(intent.getType()) &&
      !activity.getPackageManager().canRequestPackageInstalls()) {
      activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", activity.getPackageName()))));
      return;
    }
    activity.startActivity(intent);
  }

  public static String checkMime(String path, String mimeType) {
    if(mimeType == null || mimeType.equals("application/octet-stream")) {
      path = path.replaceAll(" ", "");
      String extension = MediaUtil.getFileExtensionFromUrl(path);
      String newType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
      if(newType != null) return newType;
    }
    return mimeType;
  }

  /**
   * Return the path of a not-yet-existing file in the blobdir with roughly the given filename
   * and the given extension.
   * In many cases, since we're using setFileAndDeduplicate now, this wouldn't be necessary anymore
   * and we could just create a file with a random filename,
   * but there are a few usages that still need the current behavior (like `openMaps()`).
   */
  public static String getBlobdirFile(DcContext dcContext, String filename, String ext) {
    filename = FileUtils.sanitizeFilename(filename);
    ext = FileUtils.sanitizeFilename(ext);
    String outPath = null;
    for (int i = 0; i < 1000; i++) {
      String test = dcContext.getBlobdir() + "/" + filename + (i == 0 ? "" : i < 100 ? "-" + i : "-" + (new Date().getTime() + i)) + ext;
      if (!new File(test).exists()) {
        outPath = test;
        break;
      }
    }
    if(outPath==null) {
      // should not happen
      outPath = dcContext.getBlobdir() + "/" + Math.random();
    }
    return outPath;
  }

  public static String getBlobdirFile(DcContext dcContext, String path) {
    String filename = path.substring(path.lastIndexOf('/')+1); // is the whole path if '/' is not found (lastIndexOf() returns -1 then)
    String ext = "";
    int point = filename.indexOf('.');
    if(point!=-1) {
      ext = filename.substring(point);
      filename = filename.substring(0, point);
    }
    return getBlobdirFile(dcContext, filename, ext);

  }

  @NonNull
  public static ThreadRecord getThreadRecord(Context context, DcLot summary, DcChat chat) { // adapted from ThreadDatabase.getCurrent()
    int chatId = chat.getId();

    String body = summary.getText1();
    if (!body.isEmpty()) {
      body += ": ";
    }
    body += summary.getText2();

    Recipient recipient = new Recipient(context, chat);
    long date = summary.getTimestamp();
    int unreadCount = getContext(context).getFreshMsgCount(chatId);

    return new ThreadRecord(body, recipient, date,
      unreadCount, chatId,
      chat.getVisibility(), chat.isSendingLocations(), chat.isMuted(), chat.isContactRequest(), summary);
  }

  public static boolean isNetworkConnected(Context context) {
    try {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netInfo = cm.getActiveNetworkInfo();
      if (netInfo != null && netInfo.isConnected()) {
        return true;
      }

    } catch (Exception ignored) {
    }
    return false;
  }

  /**
   * Gets a string you can show to the user with basic information about connectivity.
   * @param context
   * @param connectedString Usually "Connected", but when using this as the title in
   *                        ConversationListActivity, we want to write "Delta Chat"
   *                        or the user's display name there instead.
   * @return
   */
  public static String getConnectivitySummary(Context context, String connectedString) {
      int connectivity = getContext(context).getConnectivity();
      if (connectivity >= DcContext.DC_CONNECTIVITY_CONNECTED) {
          return connectedString;
      } else if (connectivity >= DcContext.DC_CONNECTIVITY_WORKING) {
          return context.getString(R.string.connectivity_updating);
      } else if (connectivity >= DcContext.DC_CONNECTIVITY_CONNECTING) {
          return context.getString(R.string.connectivity_connecting);
      } else {
          return context.getString(R.string.connectivity_not_connected);
      }
  }

  public static void showProtectionEnabledDialog(Context context) {
    new AlertDialog.Builder(context)
      .setMessage(context.getString(R.string.chat_protection_enabled_explanation))
      .setNeutralButton(R.string.learn_more, (d, w) -> openHelp(context, "#e2ee"))
      .setPositiveButton(R.string.ok, null)
      .setCancelable(true)
      .show();
  }

  public static void showInvalidUnencryptedDialog(Context context) {
    new AlertDialog.Builder(context)
      .setMessage(context.getString(R.string.invalid_unencrypted_explanation))
      .setNeutralButton(R.string.learn_more, (d, w) -> openHelp(context, "#howtoe2ee"))
      .setNegativeButton(R.string.qrscan_title, (d, w) -> context.startActivity(new Intent(context, QrActivity.class)))
      .setPositiveButton(R.string.ok, null)
      .setCancelable(true)
      .show();
  }

  public static void openHelp(Context context, String section) {
    Intent intent = new Intent(context, LocalHelpActivity.class);
    if (section != null) { intent.putExtra(LocalHelpActivity.SECTION_EXTRA, section); }
    context.startActivity(intent);
  }

    /**
     * For the PGP-Contacts migration, things can go wrong.
     * The migration happens when the account is setup, at which point no events can be sent yet.
     * So, instead, if something goes wrong, it's returned by getLastError().
     * This function shows the error message to the user.
     * <p>
     * A few releases after the PGP-contacts migration (which happened in 2025-05),
     * we can remove this function again.
     */
    public static void maybeShowMigrationError(Context context) {
        try {
            String lastError = DcHelper.getRpc(context).getMigrationError(DcHelper.getContext(context).getAccountId());

            if (lastError != null && !lastError.isEmpty()) {
                Log.w(TAG, "Opening account failed, trying to share error: " + lastError);

                String subject = "Delta Chat failed to update";
                String email = "delta@merlinux.eu";

                new AlertDialog.Builder(context)
                        .setMessage(context.getString(R.string.error_x, lastError))
                        .setNeutralButton(R.string.global_menu_edit_copy_desktop, (d, which) -> {
                            Util.writeTextToClipboard(context, lastError);
                        })
                        .setPositiveButton(R.string.menu_send, (d, which) -> {
                            Intent sharingIntent = new Intent(
                                    Intent.ACTION_SENDTO, Uri.fromParts(
                                    "mailto", email, null
                            )
                            );
                            sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, lastError);

                            if (sharingIntent.resolveActivity(context.getPackageManager()) == null) {
                                Log.w(TAG, "No email client found to send crash report");
                                sharingIntent = new Intent(Intent.ACTION_SEND);
                                sharingIntent.setType("text/plain");
                                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                                sharingIntent.putExtra(Intent.EXTRA_TEXT, lastError);
                                sharingIntent.putExtra(Intent.EXTRA_EMAIL, email);
                            }

                            Intent chooser =
                                    Intent.createChooser(sharingIntent, "Send using...");
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            chooser.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

                            context.startActivity(chooser);
                        })
                        .setCancelable(false)
                        .show();
            }
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }
}
