package org.thoughtcrime.securesms.deltax;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    installBundledSampleIfEmpty();
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

  private void installBundledSampleIfEmpty() {
    File[] existing = pluginsDir.listFiles();
    if (existing != null && existing.length > 0) return;
    try {
      String[] entries = context.getAssets().list("deltax");
      if (entries == null || entries.length == 0) return;
      for (String entry : entries) {
        copyAssets("deltax/" + entry, new File(pluginsDir, entry));
      }
    } catch (IOException e) {
      Log.w(TAG, "Failed to install bundled sample plugin: " + e.getMessage());
    }
  }

  private void copyAssets(String assetPath, File outFile) throws IOException {
    String[] children = context.getAssets().list(assetPath);
    if (children != null && children.length > 0) {
      outFile.mkdirs();
      for (String child : children) {
        copyAssets(assetPath + "/" + child, new File(outFile, child));
      }
    } else {
      outFile.getParentFile().mkdirs();
      try (InputStream in = context.getAssets().open(assetPath)) {
        java.nio.file.Files.copy(
            in, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }
}
