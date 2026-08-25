package org.thoughtcrime.securesms.deltax;

import android.content.Context;
import android.util.Log;
import com.b44t.messenger.DcContext;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.luaj.vm2.LuaValue;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.deltax.module.ConfigManager;
import org.thoughtcrime.securesms.deltax.module.PluginInfo;
import org.thoughtcrime.securesms.deltax.module.PluginLoader;
import org.thoughtcrime.securesms.deltax.module.PluginPackager;

public class DeltaX {

  private static final String TAG = "DeltaX";
  public static final String ENGINE_NAME = "DeltaX";
  public static final String ENGINE_VERSION = "1.0";

  private final Context context;
  private final int accountId;
  private final File extensionDir;
  private final File pluginsDir;

  private final LuaEngine luaEngine;
  private final ConfigManager configManager;
  private final PluginLoader evaluator;
  private final PluginPackager pluginPackager;

  private List<PluginInfo> loadedPlugins = new ArrayList<>();
  private boolean initialised = false;

  private static final Map<Integer, DeltaX> instances = new HashMap<>();

  public static synchronized DeltaX getInstance(Context context) {
    return getInstance(context, getSelectedAccountId(context));
  }

  public static synchronized DeltaX getInstance(Context context, int accountId) {
    DeltaX inst = instances.get(accountId);
    if (inst == null) {
      inst = new DeltaX(context, accountId);
      instances.put(accountId, inst);
    }
    return inst;
  }

  private static int getSelectedAccountId(Context context) {
    try {
      DcContext dc = DcHelper.getContext(context);
      if (dc != null) return dc.getAccountId();
    } catch (Exception ignored) {
    }
    return -1;
  }

  public DeltaX(Context context, int accountId) {
    this.context = context.getApplicationContext();
    this.accountId = accountId;
    File accountDir = resolveAccountDir(context);
    this.extensionDir = new File(accountDir, "extension");
    this.pluginsDir = new File(extensionDir, "plugin");

    this.configManager = new ConfigManager(extensionDir);
    this.luaEngine = new LuaEngine(this.context, this);
    this.evaluator = new PluginLoader(this, pluginsDir, configManager, luaEngine);
    this.pluginPackager = new PluginPackager(extensionDir);
  }

  private static File resolveAccountDir(Context context) {
    try {
      DcContext dc = DcHelper.getContext(context);
      if (dc != null) {
        String blobdir = dc.getBlobdir();
        if (blobdir != null && !blobdir.isEmpty()) {
          File dir = new File(blobdir).getParentFile();
          if (dir != null && dir.isDirectory()) return dir;
        }
      }
    } catch (Exception ignored) {
    }
    return new File(context.getFilesDir(), "DeltaX");
  }

  public boolean isInitialised() {
    return initialised;
  }

  public void init() {
    if (initialised) return;
    extensionDir.mkdirs();
    pluginsDir.mkdirs();
    loadedPlugins = evaluator.loadPlugins();
    initialised = true;
    Log.i(
        TAG,
        "DeltaX initialised with " + loadedPlugins.size() + " plugin(s) for account " + accountId);
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

  public int getAccountId() {
    return accountId;
  }

  public File getExtensionDir() {
    return extensionDir;
  }

  public File getBaseDir() {
    return extensionDir;
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

  public boolean isBackupPackage(File zip) {
    return PluginPackager.isBackupZip(zip);
  }

  /** Restores a backup package into this account's extension directory. */
  public boolean restoreBackupFromZip(File zip) {
    boolean ok = pluginPackager.restoreBackup(zip);
    pluginPackager.reload();
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

  /** Returns true when the plugin (by package name) registered an interactive page via onOpen. */
  public boolean hasInteractivePage(String packageName) {
    PluginInfo plugin = evaluator.getPlugin(packageName);
    if (plugin == null || plugin.globals == null) return false;
    LuaValue onOpen = plugin.globals.get("onOpen");
    return onOpen.isfunction();
  }
}
