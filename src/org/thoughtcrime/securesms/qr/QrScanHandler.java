package org.thoughtcrime.securesms.qr;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.widget.Toast;

import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEventCenter;
import com.b44t.messenger.DcLot;
import com.google.zxing.integration.android.IntentResult;

import org.thoughtcrime.securesms.ConversationActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.IntentUtils;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.views.ProgressDialog;

public class QrScanHandler implements DcEventCenter.DcEventDelegate {

    private Activity activity;
    ProgressDialog progressDialog;

    private ApplicationDcContext dcContext;

    public QrScanHandler(Activity activity) {
        this.activity = activity;
        dcContext = DcHelper.getContext(activity);
    }

    public void onScanPerformed(IntentResult scanResult) {
        if (scanResult == null || scanResult.getFormatName() == null) {
            return; // Should not happen!
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final String qrRawString = scanResult.getContents();
        final DcLot qrParsed = dcContext.checkQr(qrRawString);
        String nameAndAddress = dcContext.getContact(qrParsed.getId()).getNameNAddr();
        switch (qrParsed.getState()) {
            case DcContext.DC_QR_ASK_VERIFYCONTACT:
            case DcContext.DC_QR_ASK_VERIFYGROUP:
                showVerifyContactOrGroup(activity, builder, qrRawString, qrParsed, nameAndAddress);
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

            default:
                handleDefault(builder, qrRawString, qrParsed);
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
                msg = qrParsed.getText1() + "<br><br><c#808080>" + String.format(activity.getString(R.string.qrscan_contains_text), scannedText) + "</c>";
                break;
            case DcContext.DC_QR_TEXT:
                scannedText = qrParsed.getText1();
                msg = String.format(activity.getString(R.string.qrscan_contains_text), scannedText);
                break;
            default:
                scannedText = qrRawString;
                msg = String.format(activity.getString(R.string.qrscan_contains_text), scannedText);
                break;
        }
        builder.setMessage(Html.fromHtml(msg));
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
        @StringRes int resId = qrParsed.getState() == DcContext.DC_QR_ADDR ? R.string.ask_start_chat_with : R.string.qrscan_ask_join_group;
        builder.setMessage(Html.fromHtml(String.format(activity.getString(resId, nameAndAddress))));
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            int chatId = dcContext.createChatByContactId(qrParsed.getId());
            Intent intent = new Intent(activity, ConversationActivity.class);
            intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId);
            activity.startActivity(intent);
        });
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    private void showFingerPrintError(AlertDialog.Builder builder, String nameAndAddress) {
        builder.setMessage(Html.fromHtml(String.format(activity.getString(R.string.qrscan_fingerprint_mismatch), nameAndAddress)));
        builder.setPositiveButton(android.R.string.ok, null);
    }

    private void showVerifyFingerprintWithoutAddress(AlertDialog.Builder builder, DcLot qrParsed) {
        builder.setMessage(Html.fromHtml(activity.getString(R.string.qrscan_no_addr_found) + "<br><br><c#808080>" + activity.getString(R.string.qrscan_fingerprint_label) + ":<br>" + qrParsed.getText1() + "</c>"));
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
                msg = String.format(activity.getString(R.string.qrscan_ask_join_group), qrParsed.getText1());
                break;
            default:
                msg = String.format(activity.getString(R.string.ask_start_chat_with), nameNAddr);
                break;
        }
        builder.setMessage(Html.fromHtml(msg));
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
    public void handleEvent(int eventId, Object data1, Object data2) {
        if (eventId == DcContext.DC_EVENT_SECUREJOIN_JOINER_PROGRESS) {
            long contact_id = (Long)data1;
            long progress = (Long)data2;
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
