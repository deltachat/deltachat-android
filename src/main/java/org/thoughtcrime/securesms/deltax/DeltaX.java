package org.thoughtcrime.securesms.deltax;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.thoughtcrime.securesms.deltax.module.ConfigManager;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;
import org.thoughtcrime.securesms.deltax.module.PluginLoader;
import org.thoughtcrime.securesms.deltax.module.PluginPackager;

public class DeltaX {

  private static final String TAG = "DeltaX";
  public static final String ENGINE_NAME = "DeltaX";
  public static final String ENGINE_VERSION = "1.0";

  private final Context context;
  private final File baseDir;
  private final File pluginsDir;

  private final LuaEngine luaEngine;
  private final ConfigManager configManager;
  private final PluginLoader evaluator;
  private final PluginPackager pluginPackager;

  private List<PluginInfo> loadedPlugins = new ArrayList<>();
  private boolean initialised = false;

  private static DeltaX instance;

  public static synchronized DeltaX getInstance(Context context) {
    if (instance == null) {
      instance = new DeltaX(context);
    }
    return instance;
  }

  public DeltaX(Context context) {
    this.context = context.getApplicationContext();
    this.baseDir = new File(this.context.getFilesDir(), "DeltaX");
    this.pluginsDir = new File(baseDir, "plugins");

    this.configManager = new ConfigManager(baseDir);
    this.luaEngine = new LuaEngine(this.context, this);
    this.evaluator = new PluginLoader(this, pluginsDir, configManager, luaEngine);
    this.pluginPackager = new PluginPackager(pluginsDir);
  }

  public boolean isInitialised() {
    return initialised;
  }

  public void init() {
    if (initialised) return;
    baseDir.mkdirs();
    pluginsDir.mkdirs();
    loadedPlugins = evaluator.loadPlugins();
    initialised = true;
    Log.i(TAG, "DeltaX initialised with " + loadedPlugins.size() + " plugin(s)");
  }

  public void shutdown() {
    evaluator.shutdown();
  }

  public void reloadPlugins() {
    evaluator.shutdown();
    loadedPlugins = evaluator.loadPlugins();
  }

  public List<String> getLoadedPlugins() {
    List<String> names = new ArrayList<>();
    for (PluginInfo plugin : loadedPlugins) {
      names.add(plugin.getPackageName());
    }
    return names;
  }

  public List<String> getPluginList() {
    return evaluator.getPluginList();
  }

  public PluginInfo getPlugin(String nameOrPkg) {
    return evaluator.getPlugin(nameOrPkg);
  }

  public LuaEngine getLuaEngine() {
    return luaEngine;
  }

  public PluginLoader getPluginLoader() {
    return evaluator;
  }

  public PluginPackager getPluginPackager() {
    return pluginPackager;
  }

  public Context getContext() {
    return context;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public File getPluginsDir() {
    return pluginsDir;
  }

  public List<PluginInfo> getInstalledPlugins() {
    return pluginPackager.getInstalledPlugins();
  }

  public int installPluginFromZip(File zip) {
    int n = pluginPackager.installFromZip(zip);
    reloadPlugins();
    return n;
  }

  public boolean uninstallPlugin(String packageName) {
    boolean ok = pluginPackager.uninstall(packageName);
    reloadPlugins();
    return ok;
  }

  public void setPluginEnabled(String packageName, boolean enabled) {
    if (enabled) {
      evaluator.enablePlugin(packageName);
    } else {
      evaluator.disablePlugin(packageName);
    }
    reloadPlugins();
  }

  public boolean isPluginDisabled(String packageName) {
    return evaluator.isDisabled(packageName);
  }
}
