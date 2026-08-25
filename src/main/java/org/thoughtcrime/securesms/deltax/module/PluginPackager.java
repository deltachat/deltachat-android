package org.thoughtcrime.securesms.deltax.module;

import android.util.Log;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Installs / removes / archives / packs DeltaX plugins from directories. Ported from MinecraftX's
 * ModulePackager; manifest parsing reads {@code manifest.json} (Jackson) instead of YAML.
 */
public class PluginPackager {

  private static final String TAG = "DeltaX";
  private final File pluginsDir;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, PluginInfo> modules = new HashMap<>();

  public PluginPackager(File pluginsDir) {
    this.pluginsDir = pluginsDir;
    loadAllPlugins();
  }

  private void loadAllPlugins() {
    File[] files = pluginsDir.listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
    if (files == null) return;
    for (File moduleDir : files) {
      File manifestFile = new File(moduleDir, "manifest.json");
      if (!manifestFile.exists()) continue;
      Manifest manifest = parseManifest(manifestFile);
      if (manifest == null) continue;
      PluginInfo info = new PluginInfo(manifest, moduleDir);
      modules.put(info.getPackageName(), info);
    }
  }

  public PluginInfo getPluginInfo(String packageName) {
    return modules.get(packageName);
  }

  public Manifest parseManifest(File file) {
    try (FileInputStream in = new FileInputStream(file)) {
      JsonNode root = mapper.readTree(in);
      if (root == null || !root.isObject()) return null;
      String name = nodeText(root, "name");
      String version = nodeText(root, "version");
      String main = nodeText(root, "main");
      String author = nodeText(root, "author");
      if (name == null || version == null || main == null || author == null) return null;
      Manifest manifest = new Manifest();
      manifest.name = name;
      manifest.version = version;
      manifest.main = main;
      manifest.author = author;
      manifest.description = nodeText(root, "description");
      manifest.expose = root.has("expose") && root.get("expose").asBoolean();
      return manifest;
    } catch (IOException e) {
      Log.w(
          TAG, "Failed to parse manifest.json: " + file.getAbsolutePath() + " - " + e.getMessage());
      return null;
    }
  }

  private String nodeText(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value != null && !value.isNull() ? value.asText() : null;
  }

  private void copyDir(File src, File dest) throws IOException {
    if (src.isDirectory()) {
      dest.mkdirs();
      File[] children = src.listFiles();
      if (children == null) return;
      for (File child : children) {
        copyDir(child, new File(dest, child.getName()));
      }
    } else {
      dest.getParentFile().mkdirs();
      try (FileInputStream in = new FileInputStream(src)) {
        java.nio.file.Files.copy(
            in, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  public boolean install(File moduleDir) {
    if (!moduleDir.isDirectory()) return false;
    File manifestFile = new File(moduleDir, "manifest.json");
    if (!manifestFile.exists()) return false;
    Manifest manifest = parseManifest(manifestFile);
    if (manifest == null) return false;
    File destination = new File(pluginsDir, manifest.name);
    if (destination.exists()) return false;
    try {
      copyDir(moduleDir, destination);
    } catch (IOException e) {
      Log.w(TAG, "Failed to install plugin: " + e.getMessage());
      return false;
    }
    PluginInfo info = new PluginInfo(manifest, destination);
    modules.put(info.getPackageName(), info);
    return true;
  }

  public boolean remove(String packageName) {
    PluginInfo info = modules.get(packageName);
    if (info == null) return false;
    deleteRecursive(info.pluginDir);
    modules.remove(packageName);
    return true;
  }

  public boolean uninstall(String packageName) {
    return remove(packageName);
  }

  public boolean archive(String packageName) {
    PluginInfo info = modules.get(packageName);
    if (info == null) return false;
    File zipFile = new File(pluginsDir, info.manifest.name + ".zip");
    if (zipFile.exists()) return false;
    try (ZipOutputStream zos = new ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
      addDirToZip(info.pluginDir, "", zos);
    } catch (IOException e) {
      Log.w(TAG, "Failed to archive plugin: " + e.getMessage());
      return false;
    }
    return true;
  }

  public boolean pack(String packageName, File outputJar) {
    PluginInfo info = modules.get(packageName);
    if (info == null) return false;
    if (outputJar.exists()) return false;
    try (JarOutputStream jos = new JarOutputStream(new java.io.FileOutputStream(outputJar))) {
      addDirToJar(info.pluginDir, "", jos);
    } catch (IOException e) {
      Log.w(TAG, "Failed to pack plugin: " + e.getMessage());
      return false;
    }
    return true;
  }

  private void addDirToZip(File dir, String base, ZipOutputStream zos) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File f : files) {
      String entryName = base.isEmpty() ? f.getName() : base + "/" + f.getName();
      if (f.isDirectory()) {
        addDirToZip(f, entryName, zos);
      } else {
        zos.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream in = new FileInputStream(f)) {
          byte[] buffer = new byte[8192];
          int len;
          while ((len = in.read(buffer)) != -1) zos.write(buffer, 0, len);
        }
        zos.closeEntry();
      }
    }
  }

  private void addDirToJar(File dir, String base, JarOutputStream jos) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File f : files) {
      String entryName = base.isEmpty() ? f.getName() : base + "/" + f.getName();
      if (f.isDirectory()) {
        addDirToJar(f, entryName, jos);
      } else {
        jos.putNextEntry(new JarEntry(entryName));
        try (FileInputStream in = new FileInputStream(f)) {
          byte[] buffer = new byte[8192];
          int len;
          while ((len = in.read(buffer)) != -1) jos.write(buffer, 0, len);
        }
        jos.closeEntry();
      }
    }
  }

  private void deleteRecursive(File dir) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) deleteRecursive(f);
        else f.delete();
      }
    }
    dir.delete();
  }
}
