package org.thoughtcrime.securesms.deltax.ui;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.deltax.DeltaX;
import org.thoughtcrime.securesms.deltax.module.ConfigManager;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;

/**
 * Fluent, Lua-callable builder that lets a plugin describe an interactive configuration page. The
 * engine renders the collected {@link Widget} descriptors as native Android views in {@link
 * org.thoughtcrime.securesms.deltax.DeltaXPluginActivity}. Lua example:
 *
 * <pre>
 * function onOpen(page)
 *   page:title("My settings")
 *   page:text("Configure me below.")
 *   page:input("name", "", "Your name")
 *   page:switch("enabled", true)
 *   page:select("mode", {"a", "b"}, "a")
 *   page:slider("count", 0, 100, 1, 10)
 *   page:button("Save", function() page:save() end)
 * end
 * </pre>
 */
public class DeltaXPage {

  public enum Type {
    TITLE,
    TEXT,
    SECTION,
    INPUT,
    PASSWORD,
    SWITCH,
    SLIDER,
    SELECT,
    BUTTON
  }

  /** A single rendered component described by the plugin. */
  public static class Widget {
    public Type type;
    public String key;
    public String label;
    public String hint;
    public String[] options;
    public double min;
    public double max;
    public double step;
    public double numDefault;
    public boolean boolDefault;
    public String strDefault;
    public LuaFunction fn;

    Widget(Type type) {
      this.type = type;
    }
  }

  private final Context context;
  private final PluginInfo plugin;
  private final ConfigManager configManager;
  private final LuaTable config;
  private final LuaValue globals;
  private final Activity activity;
  private final java.util.List<Widget> widgets = new java.util.ArrayList<>();

  public DeltaXPage(
      Context context, PluginInfo plugin, DeltaX deltaX, LuaValue globals, Activity activity) {
    this.context = context.getApplicationContext();
    this.plugin = plugin;
    this.configManager = deltaX.getPluginLoader().getConfigManager();
    this.globals = globals;
    this.activity = activity;
    LuaValue loaded = configManager.loadPluginConfig(plugin.manifest.name);
    this.config = loaded.istable() ? (LuaTable) loaded : new LuaTable();
  }

  public java.util.List<Widget> getWidgets() {
    return widgets;
  }

  public LuaTable getConfig() {
    return config;
  }

  public PluginInfo getPlugin() {
    return plugin;
  }

  // ---------------------------------------------------------------- builder API

  public DeltaXPage title(String text) {
    Widget w = new Widget(Type.TITLE);
    w.label = text;
    widgets.add(w);
    return this;
  }

  public DeltaXPage text(String text) {
    Widget w = new Widget(Type.TEXT);
    w.label = text;
    widgets.add(w);
    return this;
  }

  public DeltaXPage section(String title) {
    Widget w = new Widget(Type.SECTION);
    w.label = title;
    widgets.add(w);
    return this;
  }

  public DeltaXPage input(String key, String def, String hint) {
    Widget w = new Widget(Type.INPUT);
    w.key = key;
    w.label = hint != null && !hint.isEmpty() ? hint : key;
    w.hint = hint;
    w.strDefault = def != null ? def : "";
    widgets.add(w);
    return this;
  }

  public DeltaXPage password(String key, String def, String hint) {
    Widget w = new Widget(Type.PASSWORD);
    w.key = key;
    w.label = hint != null && !hint.isEmpty() ? hint : key;
    w.hint = hint;
    w.strDefault = def != null ? def : "";
    widgets.add(w);
    return this;
  }

  public DeltaXPage toggle(String key, boolean def) {
    Widget w = new Widget(Type.SWITCH);
    w.key = key;
    w.label = key;
    w.boolDefault = def;
    widgets.add(w);
    return this;
  }

  public DeltaXPage slider(String key, double min, double max, double step, double def) {
    Widget w = new Widget(Type.SLIDER);
    w.key = key;
    w.label = key;
    w.min = min;
    w.max = max;
    w.step = step <= 0 ? 1 : step;
    w.numDefault = def;
    widgets.add(w);
    return this;
  }

  public DeltaXPage select(String key, LuaTable options, String def) {
    Widget w = new Widget(Type.SELECT);
    w.key = key;
    w.label = key;
    java.util.List<String> list = new java.util.ArrayList<>();
    int i = 1;
    while (!options.get(i).isnil()) {
      list.add(options.get(i).tojstring());
      i++;
    }
    w.options = list.toArray(new String[0]);
    w.strDefault = def != null ? def : (w.options.length > 0 ? w.options[0] : "");
    widgets.add(w);
    return this;
  }

  public DeltaXPage button(String label, LuaFunction fn) {
    Widget w = new Widget(Type.BUTTON);
    w.label = label;
    w.fn = fn;
    widgets.add(w);
    return this;
  }

  // ---------------------------------------------------------------- runtime API

  public LuaValue get(String key) {
    return config.get(key);
  }

  public void set(String key, LuaValue value) {
    config.set(key, value);
  }

  public void toast(String message) {
    if (context != null) Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }

  public void save() {
    try {
      configManager.savePluginConfig(plugin.manifest.name, config);
    } catch (Exception ignored) {
    }
    LuaValue onSave = globals.get("onSave");
    if (onSave.isfunction()) {
      try {
        onSave.call();
      } catch (Exception ignored) {
      }
    }
    if (context != null)
      Toast.makeText(context, R.string.deltax_saved, Toast.LENGTH_SHORT).show();
  }

  public void close() {
    if (activity != null) activity.finish();
  }
}
