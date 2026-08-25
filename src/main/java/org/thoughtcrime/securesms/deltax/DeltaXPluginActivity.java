package org.thoughtcrime.securesms.deltax;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;
import org.thoughtcrime.securesms.deltax.ui.DeltaXPage;

public class DeltaXPluginActivity extends AppCompatActivity {

  public static final String EXTRA_PACKAGE = "deltax_plugin_package";

  private DeltaX deltaX;
  private DeltaXPage page;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    deltaX = DeltaX.getInstance(this);
    if (!deltaX.isInitialised()) deltaX.init();

    String pkg = getIntent().getStringExtra(EXTRA_PACKAGE);
    PluginInfo plugin = pkg != null ? deltaX.getPluginLoader().getPlugin(pkg) : null;

    Toolbar toolbar = new Toolbar(this);
    toolbar.setId(View.generateViewId());
    toolbar.setBackgroundColor(getColorCompat(R.attr.fab_color));
    setSupportActionBar(toolbar);
    ActionBar ab = getSupportActionBar();
    if (ab != null) {
      ab.setDisplayHomeAsUpEnabled(true);
      ab.setTitle(plugin != null ? plugin.manifest.name : getString(R.string.deltax_title));
    }

    ScrollView scroll = new ScrollView(this);
    scroll.setLayoutParams(
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(8, 8, 8, 8);
    container.setId(View.generateViewId());
    scroll.addView(container);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    root.addView(toolbar);
    root.addView(scroll);

    if (plugin == null || plugin.globals == null) {
      addText(container, getString(R.string.deltax_no_page));
    } else {
      LuaValue onOpen = plugin.globals.get("onOpen");
      if (onOpen.isfunction()) {
        page = new DeltaXPage(this, plugin, deltaX, plugin.globals, this);
        onOpen.call(CoerceJavaToLua.coerce(page));
        for (DeltaXPage.Widget w : page.getWidgets()) {
          buildWidget(container, w);
        }
        addSaveButton(container);
      } else {
        addText(container, getString(R.string.deltax_no_page));
      }
    }

    setContentView(root);
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }

  private int getColorCompat(int attr) {
    android.util.TypedValue tv = new android.util.TypedValue();
    getTheme().resolveAttribute(attr, tv, true);
    return tv.data;
  }

  // ---------------------------------------------------------------- rendering

  private void addText(LinearLayout parent, String text) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setPadding(8, 8, 8, 8);
    parent.addView(t);
  }

  private CardView newCard() {
    CardView card = new CardView(this);
    LinearLayout.LayoutParams cp =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cp.setMargins(6, 6, 6, 6);
    card.setLayoutParams(cp);
    card.setRadius(8);
    card.setCardElevation(2);
    return card;
  }

  private LinearLayout newCardInner(CardView card) {
    LinearLayout inner = new LinearLayout(this);
    inner.setOrientation(LinearLayout.VERTICAL);
    inner.setPadding(12, 12, 12, 12);
    card.addView(inner);
    return inner;
  }

  private void buildWidget(LinearLayout parent, DeltaXPage.Widget w) {
    switch (w.type) {
      case TITLE:
        {
          TextView t = new TextView(this);
          t.setText(w.label);
          t.setTextSize(20);
          t.setPadding(8, 12, 8, 8);
          parent.addView(t);
          break;
        }
      case TEXT:
        {
          TextView t = new TextView(this);
          t.setText(w.label);
          t.setPadding(8, 8, 8, 8);
          parent.addView(t);
          break;
        }
      case SECTION:
        {
          TextView t = new TextView(this);
          t.setText(w.label);
          t.setTextSize(12);
          t.setPadding(8, 16, 8, 4);
          t.setAllCaps(true);
          t.setTextColor(getColorCompat(android.R.attr.textColorSecondary));
          parent.addView(t);
          break;
        }
      case INPUT:
      case PASSWORD:
        {
          CardView card = newCard();
          LinearLayout inner = newCardInner(card);
          TextView label = new TextView(this);
          label.setText(w.label);
          inner.addView(label);
          EditText et = new EditText(this);
          et.setHint(w.hint);
          et.setText(getStr(w));
          if (w.type == DeltaXPage.Type.PASSWORD) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
          }
          final String key = w.key;
          et.addTextChangedListener(
              new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int c) {}

                @Override
                public void afterTextChanged(Editable s) {
                  page.set(key, LuaValue.valueOf(s.toString()));
                }
              });
          inner.addView(et);
          parent.addView(card);
          break;
        }
      case SWITCH:
        {
          CardView card = newCard();
          LinearLayout inner = newCardInner(card);
          Switch sw = new Switch(this);
          sw.setText(w.label);
          sw.setChecked(getBool(w));
          final String key = w.key;
          sw.setOnCheckedChangeListener(
              (v, checked) -> page.set(key, LuaValue.valueOf(checked)));
          inner.addView(sw);
          parent.addView(card);
          break;
        }
      case SLIDER:
        {
          CardView card = newCard();
          LinearLayout inner = newCardInner(card);
          TextView label = new TextView(this);
          label.setText(w.label);
          inner.addView(label);
          SeekBar sb = new SeekBar(this);
          int steps = (int) Math.round((w.max - w.min) / w.step);
          sb.setMax(Math.max(steps, 1));
          sb.setProgress((int) Math.round((getNum(w) - w.min) / w.step));
          TextView val = new TextView(this);
          val.setGravity(Gravity.END);
          val.setText(String.valueOf(getNum(w)));
          final String key = w.key;
          sb.setOnSeekBarChangeListener(
              new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                  double v = w.min + progress * w.step;
                  page.set(key, LuaValue.valueOf(v));
                  val.setText(String.valueOf(v));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
              });
          inner.addView(sb);
          inner.addView(val);
          parent.addView(card);
          break;
        }
      case SELECT:
        {
          CardView card = newCard();
          LinearLayout inner = newCardInner(card);
          TextView label = new TextView(this);
          label.setText(w.label);
          inner.addView(label);
          Spinner sp = new Spinner(this);
          ArrayAdapter<String> adapter =
              new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, w.options);
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
          sp.setAdapter(adapter);
          String cur = getStr(w);
          int sel = 0;
          for (int i = 0; i < w.options.length; i++) {
            if (w.options[i].equals(cur)) {
              sel = i;
              break;
            }
          }
          sp.setSelection(sel);
          final String key = w.key;
          sp.setOnItemSelectedListener(
              new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(
                    android.widget.AdapterView<?> parent2, View view, int position, long id) {
                  page.set(key, LuaValue.valueOf(w.options[position]));
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent2) {}
              });
          inner.addView(sp);
          parent.addView(card);
          break;
        }
      case BUTTON:
        {
          Button b = new Button(this);
          b.setText(w.label);
          LinearLayout.LayoutParams bp =
              new LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
          bp.gravity = Gravity.END;
          bp.setMargins(6, 6, 6, 6);
          b.setLayoutParams(bp);
          if (w.fn != null) {
            b.setOnClickListener(
                v -> {
                  try {
                    w.fn.call();
                  } catch (LuaError e) {
                    if (page != null) page.toast("Lua error: " + e.getMessage());
                  }
                });
          }
          parent.addView(b);
          break;
        }
    }
  }

  private void addSaveButton(LinearLayout parent) {
    Button save = new Button(this, null, android.R.attr.buttonStyle);
    save.setText(R.string.deltax_save);
    LinearLayout.LayoutParams bp =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    bp.setMargins(6, 12, 6, 6);
    save.setLayoutParams(bp);
    save.setOnClickListener(v -> page.save());
    parent.addView(save);
  }

  // ---------------------------------------------------------------- value read

  private String getStr(DeltaXPage.Widget w) {
    LuaValue v = page.getConfig().get(w.key);
    return v.isnil() ? w.strDefault : v.tojstring();
  }

  private boolean getBool(DeltaXPage.Widget w) {
    LuaValue v = page.getConfig().get(w.key);
    return v.isnil() ? w.boolDefault : v.toboolean();
  }

  private double getNum(DeltaXPage.Widget w) {
    LuaValue v = page.getConfig().get(w.key);
    return v.isnil() ? w.numDefault : v.todouble();
  }
}
