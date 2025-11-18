package org.thoughtcrime.securesms.qr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.ScaleStableImageView;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Util;

public class QrShowFragment extends Fragment implements DcEventCenter.DcEventDelegate {

    private final static String TAG = QrShowFragment.class.getSimpleName();
    public final static int WHITE = 0xFFFFFFFF;
    private final static int BLACK = 0xFF000000;
    private final static int WIDTH = 400;
    private final static int HEIGHT = 400;
    private final static String CHAT_ID = "chat_id";

    private int chatId = 0;

    private int numJoiners;

    private DcEventCenter dcEventCenter;

    private DcContext dcContext;

    private View.OnClickListener scanClicklistener;

    public QrShowFragment() {
        this(null);
    }

    public QrShowFragment(View.OnClickListener scanClicklistener) {
        super();
        this.scanClicklistener = scanClicklistener;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeping the screen on also avoids falling back from IDLE to POLL
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.qr_show_fragment, container, false);

        dcContext = DcHelper.getContext(getActivity());
        dcEventCenter = DcHelper.getEventCenter(getActivity());

        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            chatId = extras.getInt(CHAT_ID);
        }

        dcEventCenter.addObserver(DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS, this);

        numJoiners = 0;

        ScaleStableImageView backgroundView = view.findViewById(R.id.background);
        Drawable drawable;
        if(DynamicTheme.isDarkTheme(getActivity())) {
            drawable = getActivity().getResources().getDrawable(R.drawable.background_hd_dark);
        } else {
            drawable = getActivity().getResources().getDrawable(R.drawable.background_hd);
        }
        backgroundView.setImageDrawable(drawable);

        SVGImageView imageView = view.findViewById(R.id.qrImage);
        try {
            SVG svg = SVG.getFromString(fixSVG(dcContext.getSecurejoinQrSvg(chatId)));
            imageView.setSVG(svg);
        } catch (SVGParseException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Activity activity = getActivity();
            if (activity != null) {
              activity.finish();
            }
        }

        view.findViewById(R.id.share_link_button).setOnClickListener((v) -> showInviteLinkDialog());
        Button scanBtn = view.findViewById(R.id.scan_qr_button);
        if (scanClicklistener != null) {
            scanBtn.setVisibility(View.VISIBLE);
            scanBtn.setOnClickListener(scanClicklistener);
        } else {
            scanBtn.setVisibility(View.GONE);
        }

        return view;
    }

    public static String fixSVG(String svg) {
      // HACK: move avatar-letter down, baseline alignment not working,
      // see https://github.com/deltachat/deltachat-core-rust/pull/2815#issuecomment-978067378 ,
      // suggestions welcome :)
      return svg.replace("y=\"281.136\"", "y=\"296\"");
    }

    public void shareInviteURL() {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            String inviteURL = dcContext.getSecurejoinQr(chatId);
            intent.putExtra(Intent.EXTRA_TEXT, inviteURL);
            startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)));
        } catch (Exception e) {
            Log.e(TAG, "failed to share invite URL", e);
        }
    }

    public void copyQrData() {
        String inviteURL = dcContext.getSecurejoinQr(chatId);
        Util.writeTextToClipboard(getActivity(), inviteURL);
        Toast.makeText(getActivity(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
    }

    public void withdrawQr() {
        Activity activity = getActivity();
        String message;
        if (chatId == 0) {
            message = activity.getString(R.string.withdraw_verifycontact_explain);
        } else {
            DcChat chat = dcContext.getChat(chatId);
            if (chat.getType() == DcChat.DC_CHAT_TYPE_GROUP) {
                message = activity.getString(R.string.withdraw_verifygroup_explain, chat.getName());
            } else {
                message = activity.getString(R.string.withdraw_joinbroadcast_explain, chat.getName());
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.withdraw_qr_code);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.reset, (dialog, which) -> {
                DcContext dcContext = DcHelper.getContext(activity);
                dcContext.setConfigFromQr(dcContext.getSecurejoinQr(chatId));
                activity.finish();
        });
        builder.setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.show();
        Util.redPositiveButton(dialog);
    }

    public void showInviteLinkDialog() {
      View view = View.inflate(getActivity(), R.layout.dialog_share_invite_link, null);
      String inviteURL = dcContext.getSecurejoinQr(chatId);
      ((TextView)view.findViewById(R.id.invite_link)).setText(inviteURL);
      new AlertDialog.Builder(getActivity())
        .setView(view)
        .setNegativeButton(R.string.cancel, null)
        .setNeutralButton(R.string.menu_copy_to_clipboard, (d, b) -> copyQrData())
        .setPositiveButton(R.string.menu_share, (d, b) -> shareInviteURL())
        .create()
        .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!DcHelper.isNetworkConnected(getContext())) {
            Toast.makeText(getActivity(), R.string.qrshow_join_contact_no_connection_toast, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dcEventCenter.removeObservers(this);
    }

    @Override
    public void handleEvent(@NonNull DcEvent event) {
        if (event.getId() == DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS) {
            DcContext dcContext = DcHelper.getContext(getActivity());
            int contact_id = event.getData1Int();
            long progress = event.getData2Int();
            String msg = null;
            if (progress == 300) {
                msg = String.format(getString(R.string.qrshow_x_joining), dcContext.getContact(contact_id).getDisplayName());
                numJoiners++;
            } else if (progress == 600) {
                msg = String.format(getString(R.string.qrshow_x_verified), dcContext.getContact(contact_id).getDisplayName());
            } else if (progress == 800) {
                msg = String.format(getString(R.string.qrshow_x_has_joined_group), dcContext.getContact(contact_id).getDisplayName());
            }

            if (msg != null) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }

            if (progress == 1000) {
                numJoiners--;
                if (numJoiners <= 0) {
                    if (getActivity() != null) getActivity().finish();
                }
            }
        }

    }


}
