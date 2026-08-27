package org.thoughtcrime.securesms.deltax;

import android.animation.Animator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.animation.AnimationCompleteListener;

/**
 * Search overlay for the DeltaX plugin manager, mirroring the expanding {@code SearchToolbar} used
 * on the main conversation list. A three-dot button next to the input lets the user pick which
 * plugin field to match against: name, author, version, or the full package id ({@code
 * author@name:version}).
 */
public class DeltaXSearchToolbar extends android.widget.LinearLayout {

  public enum SearchField {
    NAME,
    AUTHOR,
    VERSION,
    PACKAGE
  }

  public interface SearchListener {
    void onSearchTextChange(String text);

    void onSearchClosed();
  }

  public interface FieldListener {
    void onFieldChanged(SearchField field);
  }

  private float x, y;
  private EditText searchInput;
  private ImageView fieldButton;
  private SearchListener listener;
  private FieldListener fieldListener;
  private SearchField currentField = SearchField.NAME;

  public DeltaXSearchToolbar(Context context) {
    super(context);
    initialize();
  }

  public DeltaXSearchToolbar(Context context, @Nullable android.util.AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public DeltaXSearchToolbar(
      Context context, @Nullable android.util.AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  private void initialize() {
    inflate(getContext(), R.layout.deltax_search_toolbar, this);
    setOrientation(VERTICAL);

    ImageView back = findViewById(R.id.search_back);
    searchInput = findViewById(R.id.search_input);
    fieldButton = findViewById(R.id.search_field);

    Drawable drawable = getContext().getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
    drawable.mutate();
    drawable.setColorFilter(
        getContext().getResources().getColor(R.color.grey_700), PorterDuff.Mode.SRC_IN);
    back.setImageDrawable(drawable);

    back.setOnClickListener(v -> hide());

    searchInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
    searchInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            if (listener != null) listener.onSearchTextChange(s.toString());
          }
        });

    fieldButton.setOnClickListener(this::showFieldMenu);
    applyFieldHint();
  }

  private void showFieldMenu(View anchor) {
    PopupMenu popup = new PopupMenu(getContext(), anchor);
    popup.getMenuInflater().inflate(R.menu.deltax_search_field, popup.getMenu());
    popup.setOnMenuItemClickListener(
        item -> {
          int id = item.getItemId();
          if (id == R.id.search_field_name) currentField = SearchField.NAME;
          else if (id == R.id.search_field_author) currentField = SearchField.AUTHOR;
          else if (id == R.id.search_field_version) currentField = SearchField.VERSION;
          else if (id == R.id.search_field_package) currentField = SearchField.PACKAGE;
          else return false;
          applyFieldHint();
          if (fieldListener != null) fieldListener.onFieldChanged(currentField);
          return true;
        });
    popup.show();
  }

  private void applyFieldHint() {
    int res;
    switch (currentField) {
      case AUTHOR:
        res = R.string.deltax_search_by_author;
        break;
      case VERSION:
        res = R.string.deltax_search_by_version;
        break;
      case PACKAGE:
        res = R.string.deltax_search_by_package;
        break;
      default:
        res = R.string.deltax_search_by_name;
        break;
    }
    searchInput.setHint(res);
  }

  @MainThread
  public void display(float x, float y) {
    if (getVisibility() != View.VISIBLE) {
      this.x = x;
      this.y = y;

      Animator animator =
          ViewAnimationUtils.createCircularReveal(this, (int) x, (int) y, 0, getWidth());
      animator.setDuration(400);

      setVisibility(View.VISIBLE);
      animator.start();

      searchInput.requestFocus();
      InputMethodManager imm =
          (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }
  }

  public void collapse() {
    hide();
  }

  @MainThread
  private void hide() {
    if (getVisibility() == View.VISIBLE) {
      if (listener != null) listener.onSearchClosed();

      Animator animator =
          ViewAnimationUtils.createCircularReveal(this, (int) x, (int) y, getWidth(), 0);
      animator.setDuration(400);
      animator.addListener(
          new AnimationCompleteListener() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
              setVisibility(View.INVISIBLE);
            }
          });
      animator.start();
    }
  }

  public boolean isVisible() {
    return getVisibility() == View.VISIBLE;
  }

  public void setListener(SearchListener listener) {
    this.listener = listener;
  }

  public void setFieldListener(FieldListener listener) {
    this.fieldListener = listener;
  }

  public SearchField getSearchField() {
    return currentField;
  }

  public void clearQuery() {
    if (searchInput != null) searchInput.setText("");
  }
}
