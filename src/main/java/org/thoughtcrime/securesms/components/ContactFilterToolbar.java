package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.graphics.Rect;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.ViewUtil;

public class ContactFilterToolbar extends Toolbar {
  private   OnFilterChangedListener listener;

  private final EditText        searchText;
  private final AnimatingToggle toggle;
  private final ImageView       clearToggle;
  private final LinearLayout    toggleContainer;
  private boolean         useClearButton;

  public ContactFilterToolbar(Context context) {
    this(context, null);
  }

  public ContactFilterToolbar(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.toolbarStyle);
    useClearButton = true;
  }

  public ContactFilterToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inflate(context, R.layout.contact_filter_toolbar, this);

    this.searchText      = ViewUtil.findById(this, R.id.search_view);
    this.toggle          = ViewUtil.findById(this, R.id.button_toggle);
    this.clearToggle     = ViewUtil.findById(this, R.id.search_clear);
    this.toggleContainer = ViewUtil.findById(this, R.id.toggle_container);

    searchText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

    this.clearToggle.setOnClickListener(v -> {
      searchText.setText("");
      displayTogglingView(null);
    });

    this.searchText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        if (!SearchUtil.isEmpty(searchText)) {
          if(useClearButton) {
            displayTogglingView(clearToggle);
          }
        }
        else {
          displayTogglingView(null);
        }
        notifyListener();
      }
    });

    setLogo(null);
    setContentInsetStartWithNavigation(0);

    // avoid flickering by setting button_toggle to INVISIBLE in contact_filter_toolbar.xml
    // and set it to VISIBLE _after_ choosing to display nothing
    // (AnimatingToggle displays the first what will flash shortly otherwise)
    toggle.displayQuick(null);
    toggle.setVisibility(View.VISIBLE);
  }

  public void clear() {
    searchText.setText("");
    notifyListener();
  }

  public void setUseClearButton(boolean useClearButton) {
    this.useClearButton = useClearButton;
  }

  public void setOnFilterChangedListener(OnFilterChangedListener listener) {
    this.listener = listener;
  }

  private void notifyListener() {
    if (listener != null) listener.onFilterChanged(searchText.getText().toString());
  }

  private void displayTogglingView(View view) {
    toggle.display(view);
    if (view!=null) {
      expandTapArea(toggleContainer, view);
    }
  }

  private void expandTapArea(final View container, final View child) {
    final int padding = getResources().getDimensionPixelSize(R.dimen.contact_selection_actions_tap_area);

    container.post(() -> {
      Rect rect = new Rect();
      child.getHitRect(rect);

      rect.top -= padding;
      rect.left -= padding;
      rect.right += padding;
      rect.bottom += padding;

      container.setTouchDelegate(new TouchDelegate(rect, child));
    });
  }

  private static class SearchUtil {
    public static boolean isEmpty(EditText editText) {
      return editText.getText().length() <= 0;
    }
  }

  public interface OnFilterChangedListener {
    void onFilterChanged(String filter);
  }
}
