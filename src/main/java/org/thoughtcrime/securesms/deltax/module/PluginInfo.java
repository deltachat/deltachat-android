package org.thoughtcrime.securesms.deltax.module;

import java.io.File;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class PluginInfo {
  public final Manifest manifest;
  public final File pluginDir;
  public boolean enabled = true;
  public boolean loaded = false;
  public LuaValue globals = null;
  public LuaTable exportedFunctions = null;
  public LuaValue onEnableFunc = null;
  public LuaValue onDisableFunc = null;

  public PluginInfo(Manifest manifest, File pluginDir) {
    this.manifest = manifest;
    this.pluginDir = pluginDir;
  }

  public String getPackageName() {
    return manifest.getPackageName();
  }

  public File getScriptsDir() {
    return new File(pluginDir, "scripts");
  }

  public File getResourcesDir() {
    return new File(pluginDir, "resources");
  }

  @Override
  public String toString() {
    return getPackageName();
  }
}
