package org.thoughtcrime.securesms.deltax.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.thoughtcrime.securesms.deltax.LuaTableUtil;

/**
 * Manages per-plugin configuration stored as JSON (MinecraftX used YAML; DeltaX uses JSON). Config
 * files live at {@code <baseDir>/config/<pluginName>/config.json} and resource directories are
 * copied from a plugin's {@code resources/} folder on load.
 */
public class ConfigManager {

  private final File configRoot;
  private final ObjectMapper mapper = new ObjectMapper();

  public ConfigManager(File baseDir) {
    this.configRoot = new File(baseDir, "config");
  }

  public File getConfigDir(String pluginName) {
    return new File(configRoot, pluginName);
  }

  public File getConfigFile(String pluginName) {
    return new File(getConfigDir(pluginName), "config.json");
  }

  public void copyResources(PluginInfo plugin) {
    File resourcesDir = plugin.getResourcesDir();
    File[] files = resourcesDir.listFiles();
    if (files == null || files.length == 0) return;
    File targetDir = getConfigDir(plugin.manifest.name);
    targetDir.mkdirs();
    copyDirectory(resourcesDir, targetDir);
  }

  private void copyDirectory(File source, File target) {
    if (!source.exists()) return;
    File[] files = source.listFiles();
    if (files == null) return;
    for (File file : files) {
      File targetFile = new File(target, file.getName());
      if (file.isDirectory()) {
        targetFile.mkdirs();
        copyDirectory(file, targetFile);
      } else if (!targetFile.exists()) {
        try {
          copyFile(file, targetFile);
        } catch (IOException ignored) {
        }
      }
    }
  }

  private void copyFile(File source, File target) throws IOException {
    try (InputStream in = new FileInputStream(source)) {
      java.nio.file.Files.copy(
          in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public LuaValue loadPluginConfig(String pluginName) {
    File file = getConfigFile(pluginName);
    if (!file.exists()) return new LuaTable();
    try (InputStream in = new FileInputStream(file)) {
      com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(in);
      return LuaTableUtil.jsonToLua(node);
    } catch (Exception e) {
      return new LuaTable();
    }
  }

  public void savePluginConfig(String pluginName, LuaValue config) {
    File dir = getConfigDir(pluginName);
    dir.mkdirs();
    try {
      com.fasterxml.jackson.databind.JsonNode node = LuaTableUtil.luaToJson(config);
      mapper.writerWithDefaultPrettyPrinter().writeValue(getConfigFile(pluginName), node);
    } catch (Exception ignored) {
    }
  }

  public LuaValue reloadPluginConfig(String pluginName) {
    return loadPluginConfig(pluginName);
  }

  public boolean deletePluginConfig(String pluginName) {
    return getConfigFile(pluginName).delete();
  }
}
