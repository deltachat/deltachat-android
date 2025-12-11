package org.thoughtcrime.securesms.qr;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcLot;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.relay.RelayListActivity;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;

import chat.delta.rpc.types.SecurejoinSource;
import chat.delta.rpc.types.SecurejoinUiPath;

public class QrCodeHandler {
    private static final String TAG = QrCodeHandler.class.getSimpleName();

    public static int SECUREJOIN_SOURCE_EXTERNAL_LINK = 1;
    public static int SECUREJOIN_SOURCE_INTERNAL_LINK = 2;
    public static int SECUREJOIN_SOURCE_CLIPBOARD = 3;
    public static int SECUREJOIN_SOURCE_IMAGE_LOADED = 4;
    public static int SECUREJOIN_SOURCE_SCAN = 5;

    public static int SECUREJOIN_UIPATH_QR_ICON = 1;
    public static int SECUREJOIN_UIPATH_NEW_CONTACT = 2;


    private final Activity activity;
    private final DcContext dcContext;
    private final Rpc rpc;
    private final int accId;

    public QrCodeHandler(Activity activity) {
        this.activity = activity;
        dcContext = DcHelper.getContext(activity);
        rpc = DcHelper.getRpc(activity);
        accId = dcContext.getAccountId();
    }

    public void onScanPerformed(IntentResult scanResult, SecurejoinUiPath uipath) {
        if (scanResult == null || scanResult.getFormatName() == null) {
            return; // aborted
        }

        handleQrData(scanResult.getContents(), SecurejoinSource.Scan, uipath);
    }

    public void handleQrData(String rawString, SecurejoinSource source, SecurejoinUiPath uiPath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final DcLot qrParsed = dcContext.checkQr(rawString);
        String name = dcContext.getContact(qrParsed.getId()).getDisplayName();
        switch (qrParsed.getState()) {
            case DcContext.DC_QR_ASK_VERIFYCONTACT:
            case DcContext.DC_QR_ASK_VERIFYGROUP:
            case DcContext.DC_QR_ASK_JOIN_BROADCAST:
                showVerifyContactOrGroup(activity, builder, rawString, qrParsed, name, source, uiPath);
                break;

            case DcContext.DC_QR_FPR_WITHOUT_ADDR:
                showVerifyFingerprintWithoutAddress(builder, qrParsed);
                break;

            case DcContext.DC_QR_FPR_MISMATCH:
                showFingerPrintError(builder, name);
                break;

            case DcContext.DC_QR_FPR_OK:
            case DcContext.DC_QR_ADDR:
                showFingerprintOrQrSuccess(builder, qrParsed, name);
                break;

            case DcContext.DC_QR_URL:
                showQrUrl(builder, qrParsed);
                break;

            case DcContext.DC_QR_ACCOUNT:
            case DcContext.DC_QR_LOGIN:
                final String scope = qrParsed.getText1();
                setAddTransportDialog(activity, builder, rawString, scope);
                builder.setNegativeButton(R.string.cancel, null);
                builder.setCancelable(false);
                break;

            case DcContext.DC_QR_BACKUP2:
                builder.setTitle(R.string.multidevice_receiver_title);
                builder.setMessage(activity.getString(R.string.multidevice_receiver_scanning_ask) + "\n\n" + activity.getString(R.string.multidevice_same_network_hint));
                builder.setPositiveButton(R.string.perm_continue, (dialog, which) -> {
                  AccountManager.getInstance().addAccountFromSecondDevice(activity, rawString);
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.setCancelable(false);

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                BackupTransferActivity.appendSSID(activity, alertDialog.findViewById(android.R.id.message));
                return;

            case DcContext.DC_QR_BACKUP_TOO_NEW:
                builder.setTitle(R.string.multidevice_receiver_title);
                builder.setMessage(activity.getString(R.string.multidevice_receiver_needs_update));
                builder.setNegativeButton(R.string.ok, null);
                break;

            case DcContext.DC_QR_PROXY:
                builder.setTitle(R.string.proxy_use_proxy);
                builder.setMessage(activity.getString(R.string.proxy_use_proxy_confirm, qrParsed.getText1()));
                builder.setPositiveButton(R.string.proxy_use_proxy, (dlg, btn) -> {
                    dcContext.setConfigFromQr(rawString);
                    dcContext.restartIo();
                    showDoneToast(activity);
                });
                if (rawString.toLowerCase().startsWith("http")) {
                    builder.setNeutralButton(R.string.open, (d, b) -> IntentUtils.showInBrowser(activity, rawString));
                }
                builder.setNegativeButton(R.string.cancel, null);
                builder.setCancelable(false);
                break;

            case DcContext.DC_QR_WITHDRAW_VERIFYCONTACT:
            case DcContext.DC_QR_WITHDRAW_VERIFYGROUP:
            case DcContext.DC_QR_WITHDRAW_JOINBROADCAST:
                String message = qrParsed.getState() == DcContext.DC_QR_WITHDRAW_VERIFYCONTACT ? activity.getString(R.string.withdraw_verifycontact_explain)
                               : qrParsed.getState() == DcContext.DC_QR_WITHDRAW_VERIFYCONTACT ? activity.getString(R.string.withdraw_verifygroup_explain, qrParsed.getText1())
                               : activity.getString(R.string.withdraw_joinbroadcast_explain, qrParsed.getText1());
                builder.setTitle(R.string.qrshow_title);
                builder.setMessage(message);
                builder.setNeutralButton(R.string.reset, (dialog, which) -> {
                    dcContext.setConfigFromQr(rawString);
                });
                builder.setPositiveButton(R.string.ok, null);
                AlertDialog withdrawDialog = builder.show();
                Util.redButton(withdrawDialog, AlertDialog.BUTTON_NEUTRAL);
                return;

            case DcContext.DC_QR_REVIVE_VERIFYCONTACT:
            case DcContext.DC_QR_REVIVE_VERIFYGROUP:
            case DcContext.DC_QR_REVIVE_JOINBROADCAST:
                builder.setTitle(R.string.qrshow_title);
                builder.setMessage(activity.getString(R.string.revive_verifycontact_explain));
                builder.setNeutralButton(R.string.revive_qr_code, (dialog, which) -> {
                    dcContext.setConfigFromQr(rawString);
                });
                builder.setPositiveButton(R.string.ok, null);
                break;

            default:
                handleDefault(builder, rawString, qrParsed);
                break;
        }
        builder.create().show();
    }

    private void handleDefault(AlertDialog.Builder builder, String qrRawString, DcLot qrParsed) {
        String msg;
        final String scannedText;
        switch (qrParsed.getState()) {
            case DcContext.DC_QR_ERROR:
                scannedText = qrRawString;
                msg = qrParsed.getText1() + "\n\n" + activity.getString(R.string.qrscan_contains_text, scannedText);
                break;
            case DcContext.DC_QR_TEXT:
                scannedText = qrParsed.getText1();
                msg = activity.getString(R.string.qrscan_contains_text, scannedText);
                break;
            default:
                scannedText = qrRawString;
                msg = activity.getString(R.string.qrscan_contains_text, scannedText);
                break;
        }
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(R.string.menu_copy_to_clipboard, (dialog, which) -> {
            Util.writeTextToClipboard(activity, scannedText);
            showDoneToast(activity);
        });
    }

    private void showQrUrl(AlertDialog.Builder builder, DcLot qrParsed) {
        final String url = qrParsed.getText1();
        String msg = String.format(activity.getString(R.string.qrscan_contains_url), url);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.open, (dialog, which) -> IntentUtils.showInBrowser(activity, url));
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setNeutralButton(R.string.menu_copy_to_clipboard, (dialog, which) -> {
            Util.writeTextToClipboard(activity, url);
            showDoneToast(activity);
        });
    }

    private void showDoneToast(Activity activity) {
        Toast.makeText(activity, activity.getString(R.string.done), Toast.LENGTH_SHORT).show();
    }

    private void showFingerprintOrQrSuccess(AlertDialog.Builder builder, DcLot qrParsed, String name) {
        @StringRes int resId = qrParsed.getState() == DcContext.DC_QR_ADDR ? R.string.ask_start_chat_with : R.string.qrshow_x_verified;
        builder.setMessage(activity.getString(resId, name));
        builder.setPositiveButton(R.string.start_chat, (dialogInterface, i) -> {
            int chatId = dcContext.createChatByContactId(qrParsed.getId());
            Intent intent = new Intent(activity, ConversationActivity.class);
            intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
            if (qrParsed.getText1Meaning() == DcLot.DC_TEXT1_DRAFT) {
                intent.putExtra(ConversationActivity.TEXT_EXTRA, qrParsed.getText1());
            }
            activity.startActivity(intent);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    private void showFingerPrintError(AlertDialog.Builder builder, String name) {
        builder.setMessage(activity.getString(R.string.qrscan_fingerprint_mismatch, name));
        builder.setPositiveButton(android.R.string.ok, null);
    }

    private void showVerifyFingerprintWithoutAddress(AlertDialog.Builder builder, DcLot qrParsed) {
        builder.setMessage(activity.getString(R.string.qrscan_no_addr_found) + "\n\n" + activity.getString(R.string.qrscan_fingerprint_label) + ":\n" + qrParsed.getText1());
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(R.string.menu_copy_to_clipboard, (dialog, which) -> {
            Util.writeTextToClipboard(activity, qrParsed.getText1());
            showDoneToast(activity);
        });
    }

  private void showVerifyContactOrGroup(Activity activity,
                                        AlertDialog.Builder builder,
                                        String qrRawString,
                                        DcLot qrParsed,
                                        String name,
                                        SecurejoinSource source,
                                        SecurejoinUiPath uipath) {
        String msg;
        switch (qrParsed.getState()) {
            case DcContext.DC_QR_ASK_VERIFYGROUP:
                msg = activity.getString(R.string.qrscan_ask_join_group, qrParsed.getText1());
                break;
            case DcContext.DC_QR_ASK_JOIN_BROADCAST:
                msg = activity.getString(R.string.qrscan_ask_join_channel, qrParsed.getText1());
                break;
            default:
                msg = activity.getString(R.string.ask_start_chat_with, name);
                break;
        }
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            try {
                int newChatId = DcHelper.getRpc(activity).secureJoinWithUxInfo(dcContext.getAccountId(), qrRawString, source, uipath);
                if (newChatId == 0) throw new Exception("Securejoin failed to create a chat");

                Intent intent = new Intent(activity, ConversationActivity.class);
                intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, newChatId);
                activity.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                builder1.setMessage(e.getMessage());
                builder1.setPositiveButton(android.R.string.ok, null);
                builder1.create().show();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    private void setAddTransportDialog(Activity activity, AlertDialog.Builder builder, String qrData, String transportName) {
        builder.setTitle(R.string.confirm_add_transport);
        builder.setMessage(transportName);
        builder.setPositiveButton(R.string.ok, (d, w) -> {
            ProgressDialog progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage(activity.getResources().getString(R.string.one_moment));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            String cancel = activity.getResources().getString(android.R.string.cancel);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, cancel, (d2, w2) -> {
                dcContext.stopOngoingProcess();
            });
            progressDialog.show();

            Util.runOnAnyBackgroundThread(() -> {
                String error = null;
                try {
                    rpc.addTransportFromQr(accId, qrData);
                } catch (RpcException e) {
                    Log.w(TAG, e);
                    error = e.getMessage();
                }
                final String finalError = error;
                Util.runOnMain(() -> {
                    if (!progressDialog.isShowing()) return; // canceled dialog, nothing to do
                    if (finalError != null) {
                        new AlertDialog.Builder(activity)
                           .setTitle(R.string.error)
                           .setMessage(finalError)
                           .setPositiveButton(R.string.ok, null)
                           .show();
                    } else {
                        showDoneToast(activity);
                        if (!(activity instanceof RelayListActivity)) {
                            activity.startActivity(new Intent(activity, RelayListActivity.class));
                        }
                    }
                    try {
                        progressDialog.dismiss();
                    } catch (IllegalArgumentException e) {
                        // see https://stackoverflow.com/a/5102572/4557005
                        Log.w(TAG, e);
                    }
                });
            });
        });
    }
}
