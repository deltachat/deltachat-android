package org.thoughtcrime.securesms.qr;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ViewUtil;

public class QrShowBottomSheetFragment extends BottomSheetDialogFragment {

  private static final String ARG_CHAT_ID = "chat_id";

  private QrShowView qrShowView;

  public static QrShowBottomSheetFragment newInstance(int chatId) {
    QrShowBottomSheetFragment fragment = new QrShowBottomSheetFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_CHAT_ID, chatId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public int getTheme() {
    return R.style.QrShowBottomSheetDialog;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.qr_show_bottom_sheet, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    int chatId = getArguments() != null ? getArguments().getInt(ARG_CHAT_ID, 0) : 0;
    qrShowView = view.findViewById(R.id.qr_show_view);
    qrShowView.setBackground(null);
    qrShowView.setClipToOutline(false);
    qrShowView.setElevation(0);
    qrShowView.bind(chatId, this::dismiss);
    qrShowView.activate();
  }

  @Override
  public void onStart() {
    super.onStart();

    BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
    if (dialog == null) return;

    View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet == null) return;

    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
    behavior.setShouldRemoveExpandedCorners(false);
    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

    MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable();
    shapeDrawable.setShapeAppearanceModel(
        shapeDrawable.getShapeAppearanceModel().toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(18))
            .setTopRightCorner(CornerFamily.ROUNDED, ViewUtil.dpToPx(18))
            .build());

    TypedValue typedValue = new TypedValue();
    if (requireContext()
        .getTheme()
        .resolveAttribute(R.attr.dialog_background_color, typedValue, true)) {
      shapeDrawable.setFillColor(ColorStateList.valueOf(typedValue.data));
    } else {
      shapeDrawable.setFillColor(
          ColorStateList.valueOf(getResources().getColor(R.color.background_material_light)));
    }

    bottomSheet.setBackground(shapeDrawable);
    bottomSheet.setClipToOutline(true);
  }

  @Override
  public void onDestroyView() {
    if (qrShowView != null) {
      qrShowView.deactivate();
      qrShowView = null;
    }
    super.onDestroyView();
  }
}
