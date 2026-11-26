package org.thoughtcrime.securesms.qr;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import com.b44t.messenger.DcChat;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class QrShowView extends FrameLayout implements DcEventCenter.DcEventDelegate {

  private static final String TAG = "QrShowView";

  public interface CloseListener {
    void onClose();
  }

  private int chatId;
  private int numJoiners;
  private boolean active;

  private DcContext dcContext;
  private DcEventCenter dcEventCenter;
  private CloseListener closeListener;
  private String inviteUrl;

  private SVGImageView qrImageView;
  private TextView instructionView;

  public QrShowView(@NonNull Context context) {
    this(context, null);
  }

  public QrShowView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public QrShowView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    LayoutInflater.from(getContext()).inflate(R.layout.qr_show_view, this, true);

    qrImageView = findViewById(R.id.qrImage);
    instructionView = findViewById(R.id.qr_show_instruction);

    findViewById(R.id.share_link_button).setOnClickListener(v -> showInviteLinkDialog());

    MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable();
    shapeDrawable.setShapeAppearanceModel(
        shapeDrawable.getShapeAppearanceModel().toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(18))
            .setTopRightCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(18))
            .build());

    TypedValue typedValue = new TypedValue();
    if (getContext()
        .getTheme()
        .resolveAttribute(R.attr.dialog_background_color, typedValue, true)) {
      shapeDrawable.setFillColor(ColorStateList.valueOf(typedValue.data));
    } else {
      shapeDrawable.setFillColor(
          ColorStateList.valueOf(getResources().getColor(R.color.background_material_light)));
    }

    setBackground(shapeDrawable);
    setClipToOutline(true);
    setElevation(ViewUtil.dpToPx(4));
  }

  public void bind(int chatId, @NonNull CloseListener closeListener) {
    this.chatId = chatId;
    this.closeListener = closeListener;
    if (active) {
      loadQr();
    }
  }

  public void activate() {
    if (active) return;

    active = true;
    numJoiners = 0;
    dcContext = DcHelper.getContext(getContext());
    dcEventCenter = DcHelper.getEventCenter(getContext());
    dcEventCenter.addObserver(DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS, this);
    setKeepScreenOn(true);

    loadQr();
    if (!active) return;

    if (!DcHelper.isNetworkConnected(getContext())) {
      Toast.makeText(
              getContext(), R.string.qrshow_join_contact_no_connection_toast, Toast.LENGTH_LONG)
          .show();
    }
  }

  public void deactivate() {
    if (!active) return;

    active = false;
    setKeepScreenOn(false);
    if (dcEventCenter != null) {
      dcEventCenter.removeObservers(this);
    }
  }

  public boolean isActive() {
    return active;
  }

  @Override
  protected void onDetachedFromWindow() {
    deactivate();
    super.onDetachedFromWindow();
  }

  private void loadQr() {
    try {
      bindIdentity();

      inviteUrl = dcContext.getSecurejoinQr(chatId);
      if (TextUtils.isEmpty(inviteUrl)) {
        throw new Exception("Empty secure-join QR payload");
      }

      String svgData = dcContext.createQrSvg(inviteUrl);
      if (TextUtils.isEmpty(svgData)) {
        throw new Exception("Empty QR SVG");
      }

      qrImageView.setSVG(SVG.getFromString(svgData));
    } catch (Exception e) {
      Log.e(TAG, "Cannot create secure-join QR", e);
      requestClose();
    }
  }

  private void bindIdentity() {
    String name;
    int instructionRes;

    if (chatId == 0) {
      name = DcHelper.get(getContext(), DcHelper.CONFIG_DISPLAY_NAME);
      if (name.isEmpty()) {
        name = getResources().getString(R.string.unnamed);
      }
      instructionRes = R.string.qrshow_join_contact_hint;
    } else {
      DcChat chat = dcContext.getChat(chatId);
      name = chat.getName();
      instructionRes =
          chat.getType() == DcChat.DC_CHAT_TYPE_GROUP
              ? R.string.qrshow_join_group_hint
              : R.string.qrshow_join_channel_hint;
    }

    instructionView.setText(getResources().getString(instructionRes, name));
  }

  public void showInviteLinkDialog() {
    if (inviteUrl == null) return;

    View view = View.inflate(getContext(), R.layout.dialog_share_invite_link, null);
    ((TextView) view.findViewById(R.id.invite_link)).setText(inviteUrl);

    new AlertDialog.Builder(getContext())
        .setView(view)
        .setNegativeButton(R.string.cancel, null)
        .setNeutralButton(R.string.menu_copy_to_clipboard, (d, b) -> copyQrData())
        .setPositiveButton(R.string.menu_share, (d, b) -> shareInviteURL())
        .create()
        .show();
  }

  public void setHiddenHeight(int hiddenHeight) {
    NestedScrollView scrollView = findViewById(R.id.qr_show_scroll);
    View content = findViewById(R.id.qr_show_content);

    int navigationBottom = 0;
    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(this);
    if (ViewUtil.isEdgeToEdgeSupported() && insets != null) {
      navigationBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
    }

    int bottom = hiddenHeight + navigationBottom;
    if (scrollView.getPaddingBottom() != bottom) {
      scrollView.setPadding(0, 0, 0, bottom);
    }

    if (content.getHeight() == 0) return;

    int viewportHeight = scrollView.getHeight() - bottom;
    int availableWidth = content.getWidth() - content.getPaddingLeft() - content.getPaddingRight();
    int nonQrHeight = content.getHeight() - qrImageView.getHeight();
    int size =
        Math.max(ViewUtil.dpToPx(240), Math.min(availableWidth, viewportHeight - nonQrHeight));

    ViewGroup.LayoutParams params = qrImageView.getLayoutParams();
    if (params.height != size) {
      params.height = size;
      qrImageView.setLayoutParams(params);
    }
  }

  private void shareInviteURL() {
    try {
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_TEXT, inviteUrl);
      getContext()
          .startActivity(
              Intent.createChooser(
                  intent, getResources().getString(R.string.chat_share_with_title)));
    } catch (Exception e) {
      Log.e(TAG, "Failed to share invite URL", e);
    }
  }

  private void copyQrData() {
    Util.writeTextToClipboard(getContext(), inviteUrl);
    Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
  }

  public void withdrawQr() {
    if (active) {
      showWithdrawDialog(getContext(), chatId, this::requestClose);
    }
  }

  public static void showWithdrawDialog(Context context, int chatId, Runnable onReset) {
    DcContext dcContext = DcHelper.getContext(context);
    String message;

    if (chatId == 0) {
      message = context.getString(R.string.withdraw_verifycontact_explain);
    } else {
      DcChat chat = dcContext.getChat(chatId);
      if (chat.getType() == DcChat.DC_CHAT_TYPE_GROUP) {
        message = context.getString(R.string.withdraw_verifygroup_explain, chat.getName());
      } else {
        message = context.getString(R.string.withdraw_joinbroadcast_explain, chat.getName());
      }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.withdraw_qr_code);
    builder.setMessage(message);
    builder.setPositiveButton(
        R.string.reset,
        (dialog, which) -> {
          dcContext.setConfigFromQr(dcContext.getSecurejoinQr(chatId));
          if (onReset != null) {
            onReset.run();
          }
        });
    builder.setNegativeButton(R.string.cancel, null);

    AlertDialog dialog = builder.show();
    Util.redPositiveButton(dialog);
  }

  private void requestClose() {
    deactivate();
    if (closeListener != null) {
      closeListener.onClose();
    }
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    if (!active || event.getId() != DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS) {
      return;
    }

    int contactId = event.getData1Int();
    long progress = event.getData2Int();
    String msg = null;

    if (progress == 300) {
      msg =
          String.format(
              getResources().getString(R.string.qrshow_x_joining),
              dcContext.getContact(contactId).getDisplayName());
      numJoiners++;
    } else if (progress == 600) {
      msg =
          String.format(
              getResources().getString(R.string.qrshow_x_verified),
              dcContext.getContact(contactId).getDisplayName());
    } else if (progress == 800) {
      msg =
          String.format(
              getResources().getString(R.string.qrshow_x_has_joined_group),
              dcContext.getContact(contactId).getDisplayName());
    }

    if (msg != null) {
      Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    if (progress == 1000) {
      numJoiners--;
      if (numJoiners <= 0) {
        requestClose();
      }
    }
  }
}
