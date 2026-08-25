package org.thoughtcrime.securesms.deltax;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;
import org.thoughtcrime.securesms.deltax.ui.DeltaXPage;

public class DeltaXPluginActivity extends BaseActionBarActivity {

  public static final String EXTRA_PACKAGE = "deltax_plugin_package";

  private DeltaX deltaX;
  private DeltaXPage page;
  private LinearLayout container;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    deltaX = DeltaX.getInstance(this);
    if (!deltaX.isInitialised()) deltaX.init();

    String pkg = getIntent().getStringExtra(EXTRA_PACKAGE);
    PluginInfo plugin = pkg != null ? deltaX.getPluginLoader().getPlugin(pkg) : null;

    setContentView(R.layout.activity_deltax_plugin);
    container = findViewById(R.id.deltax_page_container);

    ActionBar ab = getSupportActionBar();
    if (ab != null) {
      ab.setDisplayHomeAsUpEnabled(true);
      ab.setTitle(plugin != null ? plugin.manifest.name : getString(R.string.deltax_title));
    }

    if (plugin == null || plugin.globals == null) {
      addText(getString(R.string.deltax_no_page));
    } else {
      LuaValue onOpen = plugin.globals.get("onOpen");
      if (onOpen.isfunction()) {
        page = new DeltaXPage(this, plugin, deltaX, plugin.globals, this);
        onOpen.call(CoerceJavaToLua.coerce(page));
        for (DeltaXPage.Widget w : page.getWidgets()) {
          buildWidget(w);
        }
        addSaveButton();
      } else {
        addText(getString(R.string.deltax_no_page));
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  // ---------------------------------------------------------------- rendering

  private LinearLayout.LayoutParams fullWidth() {
    return new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  private void addText(String text) {
    TextView t = new TextView(this);
    t.setText(text);
    t.setPadding(0, 8, 0, 8);
    container.addView(t);
  }

  private void buildWidget(DeltaXPage.Widget w) {
    switch (w.type) {
      case TITLE:
        {
          TextView t = new TextView(this);
          t.setText(w.label);
          t.setTextSize(20);
          t.setPadding(0, 8, 0, 8);
          container.addView(t);
          break;
        }
      case TEXT:
        {
          TextView t = new TextView(this);
          t.setText(w.label);
          t.setPadding(0, 8, 0, 8);
          container.addView(t);
          break;
        }
      case SECTION:
        {
          TextView t = new TextView(this);
          t.setText(w.label);
          t.setTextSize(16);
          t.setTextColor(getResources().getColor(R.color.delta_accent));
          t.setPadding(0, 16, 0, 4);
          container.addView(t);
          break;
        }
      case INPUT:
      case PASSWORD:
        {
          TextInputLayout til = new TextInputLayout(this);
          til.setLayoutParams(fullWidth());
          TextInputEditText et = new TextInputEditText(this);
          et.setHint(w.hint != null && !w.hint.isEmpty() ? w.hint : w.label);
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
          til.addView(et);
          container.addView(til);
          break;
        }
      case SWITCH:
        {
          SwitchCompat sw = new SwitchCompat(this);
          sw.setLayoutParams(fullWidth());
          sw.setPadding(0, 16, 0, 16);
          sw.setText(w.label);
          sw.setChecked(getBool(w));
          final String key = w.key;
          sw.setOnCheckedChangeListener((v, checked) -> page.set(key, LuaValue.valueOf(checked)));
          container.addView(sw);
          break;
        }
      case SLIDER:
        {
          TextView label = new TextView(this);
          label.setText(w.label);
          label.setPadding(0, 16, 0, 4);
          container.addView(label);
          SeekBar sb = new SeekBar(this);
          sb.setLayoutParams(fullWidth());
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
          container.addView(sb);
          container.addView(val);
          break;
        }
      case SELECT:
        {
          TextView label = new TextView(this);
          label.setText(w.label);
          label.setPadding(0, 16, 0, 4);
          container.addView(label);
          Spinner sp = new Spinner(this);
          sp.setLayoutParams(fullWidth());
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
                    android.widget.AdapterView<?> parent, View view, int position, long id) {
                  page.set(key, LuaValue.valueOf(w.options[position]));
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
              });
          container.addView(sp);
          break;
        }
      case BUTTON:
        {
          MaterialButton b = new MaterialButton(this);
          b.setText(w.label);
          LinearLayout.LayoutParams bp =
              new LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
          bp.gravity = Gravity.END;
          bp.setMargins(0, 8, 0, 8);
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
          container.addView(b);
          break;
        }
    }
  }

  private void addSaveButton() {
    MaterialButton save = new MaterialButton(this);
    save.setText(R.string.deltax_save);
    LinearLayout.LayoutParams bp =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    bp.setMargins(0, 16, 0, 0);
    save.setLayoutParams(bp);
    save.setOnClickListener(v -> page.save());
    container.addView(save);
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
