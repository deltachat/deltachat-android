package org.thoughtcrime.securesms.qr;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.widget.Toast;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcLot;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

public class QrCodeHandler implements DcEventCenter.DcEventDelegate {

    private Activity activity;
    ProgressDialog progressDialog;

    private ApplicationDcContext dcContext;

    public QrCodeHandler(Activity activity) {
        this.activity = activity;
        dcContext = DcHelper.getContext(activity);
    }

    public void onScanPerformed(IntentResult scanResult) {
        if (scanResult == null || scanResult.getFormatName() == null) {
            return; // aborted
        }

        handleOpenPgp4Fpr(scanResult.getContents());
    }

    public void handleOpenPgp4Fpr(String rawString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final DcLot qrParsed = dcContext.checkQr(rawString);
        String nameAndAddress = dcContext.getContact(qrParsed.getId()).getNameNAddr();
        switch (qrParsed.getState()) {
            case DcContext.DC_QR_ASK_VERIFYCONTACT:
            case DcContext.DC_QR_ASK_VERIFYGROUP:
                showVerifyContactOrGroup(activity, builder, rawString, qrParsed, nameAndAddress);
                break;

            case DcContext.DC_QR_FPR_WITHOUT_ADDR:
                showVerifyFingerprintWithoutAddress(builder, qrParsed);
                break;

            case DcContext.DC_QR_FPR_MISMATCH:
                showFingerPrintError(builder, nameAndAddress);
                break;

            case DcContext.DC_QR_FPR_OK:
            case DcContext.DC_QR_ADDR:
                showFingerprintOrQrSuccess(builder, qrParsed, nameAndAddress);
                break;

            case DcContext.DC_QR_URL:
                showQrUrl(builder, qrParsed);
                break;

            case DcContext.DC_QR_ACCOUNT:
                String domain = qrParsed.getText1();
                builder.setMessage(activity.getString(R.string.qraccount_ask_create_and_login_another, domain));
                builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                    AccountManager.getInstance().addAccountFromQr(activity, rawString);
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.setCancelable(false);
                break;

            case DcContext.DC_QR_WEBRTC:
                builder.setMessage(activity.getString(R.string.videochat_instance_from_qr, qrParsed.getText1()));
                builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                    dcContext.setConfigFromQr(rawString);
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.setCancelable(false);
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
        builder.setPositiveButton(R.string.open, (dialog, which) -> IntentUtils.showBrowserIntent(activity, url));
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setNeutralButton(R.string.menu_copy_to_clipboard, (dialog, which) -> {
            Util.writeTextToClipboard(activity, url);
            showDoneToast(activity);
        });
    }

    private void showDoneToast(Activity activity) {
        Toast.makeText(activity, activity.getString(R.string.done), Toast.LENGTH_SHORT).show();
    }

    private void showFingerprintOrQrSuccess(AlertDialog.Builder builder, DcLot qrParsed, String nameAndAddress) {
        @StringRes int resId = qrParsed.getState() == DcContext.DC_QR_ADDR ? R.string.ask_start_chat_with : R.string.qrshow_x_verified;
        builder.setMessage(activity.getString(resId, nameAndAddress));
        builder.setPositiveButton(R.string.start_chat, (dialogInterface, i) -> {
            int chatId = dcContext.createChatByContactId(qrParsed.getId());
            Intent intent = new Intent(activity, ConversationActivity.class);
            intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
            activity.startActivity(intent);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    private void showFingerPrintError(AlertDialog.Builder builder, String nameAndAddress) {
        builder.setMessage(activity.getString(R.string.qrscan_fingerprint_mismatch, nameAndAddress));
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

    private void showVerifyContactOrGroup(Activity activity, AlertDialog.Builder builder, String qrRawString, DcLot qrParsed, String nameNAddr) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        String msg;
        switch (qrParsed.getState()) {
            case DcContext.DC_QR_ASK_VERIFYGROUP:
                msg = activity.getString(R.string.qrscan_ask_join_group, qrParsed.getText1());
                break;
            default:
                msg = activity.getString(R.string.ask_start_chat_with, nameNAddr);
                break;
        }
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage(activity.getString(R.string.one_moment));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(android.R.string.cancel), (dialog, which) -> dcContext.stopOngoingProcess());
            progressDialog.show();

            dcContext.captureNextError();

            new Thread(() -> {

                    dcContext.eventCenter.addObserver(this, DcContext.DC_EVENT_SECUREJOIN_JOINER_PROGRESS);
                    int newChatId = dcContext.joinSecurejoin(qrRawString); // joinSecurejoin() runs until all needed messages are sent+received!
                    dcContext.eventCenter.removeObservers(this);

                    Util.runOnMain(() -> {
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                            progressDialog = null;
                        }

                        String errorString = dcContext.getCapturedError();
                        dcContext.endCaptureNextError();
                        if (newChatId != 0) {
                            Intent intent = new Intent(activity, ConversationActivity.class);
                            intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, newChatId);
                            activity.startActivity(intent);
                        } else if (!errorString.isEmpty()) {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                            builder1.setMessage(errorString);
                            builder1.setPositiveButton(android.R.string.ok, null);
                            builder1.create().show();
                        }
                    });
                }
            ).start();


        });
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    @Override
    public void handleEvent(DcEvent event) {
        if (event.getId() == DcContext.DC_EVENT_SECUREJOIN_JOINER_PROGRESS) {
            long contact_id = event.getData1Int();
            long progress = event.getData2Int();
            String msg = null;
            if( progress == 400) {
                msg = activity.getString(R.string.qrscan_x_verified_introduce_myself, dcContext.getContact((int)contact_id).getNameNAddr());
            }

            if( progressDialog != null && msg != null ) {
                progressDialog.setMessage(msg);
            }

        }
    }
}
