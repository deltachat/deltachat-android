package org.thoughtcrime.securesms.deltax.module;

import android.util.Log;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.thoughtcrime.securesms.deltax.DeltaX;
import org.thoughtcrime.securesms.deltax.LuaEngine;

public class PluginLoader {

  private static final String TAG = "DeltaX";

  private final DeltaX deltax;
  private final LuaEngine luaEngine;
  private final ConfigManager configManager;
  private final File pluginsDir;
  private final ObjectMapper mapper = new ObjectMapper();

  private final Map<String, PluginInfo> registry = new HashMap<>();
  private final Map<String, List<PluginInfo>> nameIndex = new HashMap<>();
  private final Map<String, PluginInfo> providesMap = new HashMap<>();
  private final File disabledFile;

  private boolean hasFatalError = false;

  public PluginLoader(
      DeltaX deltax, File pluginsDir, ConfigManager configManager, LuaEngine luaEngine) {
    this.deltax = deltax;
    this.pluginsDir = pluginsDir;
    this.configManager = configManager;
    this.luaEngine = luaEngine;
    this.disabledFile = new File(pluginsDir, "disabled.txt");
  }

  public List<PluginInfo> loadPlugins() {
    List<PluginInfo> scanned = scanPlugins();
    if (scanned.isEmpty()) return new ArrayList<>();

    Set<String> disabled = loadDisabledList();
    for (PluginInfo plugin : scanned) {
      if (disabled.contains(plugin.getPackageName())) {
        plugin.enabled = false;
      }
    }

    checkDuplicates(scanned);
    if (hasFatalError) return new ArrayList<>();

    List<PluginInfo> duplicateRemediation = resolveDuplicates(scanned);
    List<PluginInfo> validated = new ArrayList<>();
    for (PluginInfo plugin : duplicateRemediation) {
      if (plugin.enabled) validated.add(plugin);
    }

    List<String> cycle = detectCircularDependency(validated);
    if (cycle != null) {
      Log.e(TAG, "Detected dependency cycle, aborting plugin load");
      Log.e(TAG, String.join(" -> ", cycle));
      return new ArrayList<>();
    }

    Map<String, PluginInfo> resolvedProviders = new HashMap<>();
    for (PluginInfo plugin : validated) {
      if (plugin.manifest.provides != null) {
        for (String pkg : plugin.manifest.provides) {
          resolvedProviders.put(pkg, plugin);
        }
      }
    }

    List<PluginInfo> result = new ArrayList<>();
    for (PluginInfo plugin : validated) {
      if (!validatePlugin(plugin, validated, resolvedProviders)) {
        plugin.enabled = false;
        continue;
      }
      result.add(plugin);
    }

    List<PluginInfo> finalPlugins = new ArrayList<>();
    for (PluginInfo plugin : result) {
      if (plugin.enabled) finalPlugins.add(plugin);
    }

    for (PluginInfo plugin : finalPlugins) {
      try {
        loadPluginScript(plugin);
      } catch (Exception e) {
        Log.w(TAG, "Plugin " + plugin.getPackageName() + " failed to load: " + e.getMessage());
        plugin.enabled = false;
      }
    }

    List<PluginInfo> loaded = new ArrayList<>();
    for (PluginInfo plugin : finalPlugins) {
      if (plugin.loaded) loaded.add(plugin);
    }

    for (PluginInfo plugin : loaded) {
      try {
        callOnEnable(plugin);
      } catch (Exception e) {
        Log.w(TAG, "Plugin " + plugin.getPackageName() + " onEnable failed: " + e.getMessage());
      }
    }

    return loaded;
  }

  public void shutdown() {
    for (PluginInfo plugin : registry.values()) {
      if (plugin.loaded) {
        try {
          callOnDisable(plugin);
        } catch (Exception e) {
          Log.w(TAG, "Plugin " + plugin.getPackageName() + " onDisable failed: " + e.getMessage());
        }
      }
    }
  }

  public List<String> getPluginList() {
    List<String> lines = new ArrayList<>();
    lines.add("=== DeltaX plugins ===");
    List<PluginInfo> sorted = new ArrayList<>(registry.values());
    sorted.sort(java.util.Comparator.comparing(a -> a.manifest.name));
    for (PluginInfo plugin : sorted) {
      boolean isDup =
          nameIndex.get(plugin.manifest.name) != null
              && nameIndex.get(plugin.manifest.name).size() > 1;
      String display = isDup ? plugin.getPackageName() : plugin.manifest.name;
      lines.add((plugin.enabled && plugin.loaded ? "[x] " : "[ ] ") + display);
    }
    return lines;
  }

  public List<String> getPluginListFilterStatus(boolean enabled) {
    List<String> lines = new ArrayList<>();
    lines.add("=== " + (enabled ? "enabled" : "disabled") + " plugins ===");
    List<PluginInfo> sorted = new ArrayList<>(registry.values());
    sorted.sort(java.util.Comparator.comparing(a -> a.manifest.name));
    for (PluginInfo plugin : sorted) {
      if ((plugin.enabled && plugin.loaded) == enabled) {
        lines.add(plugin.manifest.name);
      }
    }
    return lines;
  }

  public List<String> getPluginListFilterAuthor(String author) {
    List<String> lines = new ArrayList<>();
    lines.add("=== plugins by " + author + " ===");
    List<PluginInfo> sorted = new ArrayList<>(registry.values());
    sorted.sort(java.util.Comparator.comparing(a -> a.manifest.name));
    for (PluginInfo plugin : sorted) {
      if (plugin.manifest.author.equalsIgnoreCase(author)) {
        lines.add(plugin.manifest.name);
      }
    }
    return lines;
  }

  public List<String> getPluginInfo(String nameOrPkg) {
    PluginInfo plugin = findPlugin(nameOrPkg);
    if (plugin == null) return null;
    List<String> lines = new ArrayList<>();
    lines.add(plugin.getPackageName());
    lines.add("  name: " + plugin.manifest.name);
    lines.add("  version: " + plugin.manifest.version);
    lines.add("  main: " + plugin.manifest.main);
    if (plugin.manifest.description != null) {
      lines.add("  description: " + plugin.manifest.description);
    }
    lines.add("  author: " + plugin.manifest.author);
    return lines;
  }

  public boolean disablePlugin(String nameOrPkg) {
    PluginInfo plugin = findPlugin(nameOrPkg);
    if (plugin == null) return false;
    Set<String> disabled = loadDisabledList();
    disabled.add(plugin.getPackageName());
    saveDisabledList(disabled);
    Log.i(TAG, "Plugin " + plugin.getPackageName() + " disabled, restart to apply");
    return true;
  }

  public boolean enablePlugin(String nameOrPkg) {
    PluginInfo plugin = findPlugin(nameOrPkg);
    if (plugin == null) return false;
    Set<String> disabled = loadDisabledList();
    if (disabled.remove(plugin.getPackageName())) {
      saveDisabledList(disabled);
      Log.i(TAG, "Plugin " + plugin.getPackageName() + " enabled, restart to apply");
    }
    return true;
  }

  public boolean isDisabled(String nameOrPkg) {
    PluginInfo plugin = findPlugin(nameOrPkg);
    if (plugin == null) return false;
    return loadDisabledList().contains(plugin.getPackageName());
  }

  private PluginInfo findPlugin(String nameOrPkg) {
    if (registry.containsKey(nameOrPkg)) return registry.get(nameOrPkg);
    List<PluginInfo> byName = nameIndex.get(nameOrPkg);
    if (byName != null) {
      return byName.size() == 1 ? byName.get(0) : null;
    }
    return null;
  }

  public Set<String> getPluginNames() {
    Set<String> result = new HashSet<>();
    for (PluginInfo plugin : registry.values()) {
      result.add(plugin.manifest.name);
      result.add(plugin.getPackageName());
    }
    return result;
  }

  public Set<String> getPluginAuthors() {
    Set<String> result = new HashSet<>();
    for (PluginInfo plugin : registry.values()) {
      result.add(plugin.manifest.author);
    }
    return result;
  }

  public PluginInfo getPlugin(String nameOrPkg) {
    return findPlugin(nameOrPkg);
  }

  public ConfigManager getConfigManager() {
    return configManager;
  }

  public void registerPlugin(PluginInfo plugin) {
    registry.put(plugin.getPackageName(), plugin);
    nameIndex.computeIfAbsent(plugin.manifest.name, k -> new ArrayList<>()).add(plugin);
  }

  public void unregisterPlugin(PluginInfo plugin) {
    registry.remove(plugin.getPackageName());
    List<PluginInfo> list = nameIndex.get(plugin.manifest.name);
    if (list != null) {
      list.remove(plugin);
      if (list.isEmpty()) nameIndex.remove(plugin.manifest.name);
    }
  }

  public boolean loadPlugin(PluginInfo plugin) {
    configManager.copyResources(plugin);
    loadPluginScript(plugin);
    if (plugin.loaded) {
      callOnEnable(plugin);
      return true;
    }
    return false;
  }

  public void unloadPlugin(PluginInfo plugin) {
    if (plugin.loaded) {
      callOnDisable(plugin);
      plugin.loaded = false;
      plugin.globals = null;
      plugin.exportedFunctions = null;
      plugin.onEnableFunc = null;
      plugin.onDisableFunc = null;
    }
  }

  private List<PluginInfo> scanPlugins() {
    List<PluginInfo> result = new ArrayList<>();
    if (!pluginsDir.exists()) return result;
    File[] subDirs = pluginsDir.listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
    if (subDirs == null) return result;
    List<File> sorted = new ArrayList<>(java.util.Arrays.asList(subDirs));
    sorted.sort(java.util.Comparator.comparing(File::getName));
    for (File dir : sorted) {
      File manifestFile = new File(dir, "manifest.json");
      if (!manifestFile.exists()) continue;
      Manifest manifest = parseManifest(manifestFile);
      if (manifest == null) {
        Log.w(TAG, "Plugin " + dir.getName() + " manifest.json parse failed, skipped");
        continue;
      }
      result.add(new PluginInfo(manifest, dir));
    }
    return result;
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
      manifest.depends = parseDepends(root.get("depends"));
      manifest.conflicts = parseConflicts(root.get("conflicts"));
      JsonNode provides = root.get("provides");
      if (provides != null && provides.isArray()) {
        List<String> list = new ArrayList<>();
        for (JsonNode n : provides) list.add(n.asText().trim());
        manifest.provides = list;
      }
      return manifest;
    } catch (Exception e) {
      Log.w(
          TAG, "Failed to parse manifest.json: " + file.getAbsolutePath() + " - " + e.getMessage());
      return null;
    }
  }

  private Manifest.DependsSpec parseDepends(JsonNode node) {
    if (node == null || !node.isObject()) return null;
    Manifest.DependsSpec spec = new Manifest.DependsSpec();
    JsonNode required = node.get("required");
    if (required != null && required.isArray()) {
      List<String> list = new ArrayList<>();
      for (JsonNode n : required) list.add(n.asText().trim());
      spec.required = list;
    }
    JsonNode optional = node.get("optional");
    if (optional != null && optional.isArray()) {
      List<String> list = new ArrayList<>();
      for (JsonNode n : optional) list.add(n.asText().trim());
      spec.optional = list;
    }
    return spec;
  }

  private Manifest.ConflictsSpec parseConflicts(JsonNode node) {
    if (node == null || !node.isObject()) return null;
    Manifest.ConflictsSpec spec = new Manifest.ConflictsSpec();
    JsonNode breakNode = node.get("break");
    if (breakNode != null && breakNode.isArray()) {
      List<String> list = new ArrayList<>();
      for (JsonNode n : breakNode) list.add(n.asText().trim());
      spec.breakList = list;
    }
    JsonNode incompatible = node.get("incompatible");
    if (incompatible != null && incompatible.isArray()) {
      List<String> list = new ArrayList<>();
      for (JsonNode n : incompatible) list.add(n.asText().trim());
      spec.incompatible = list;
    }
    return spec;
  }

  private String nodeText(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value != null && !value.isNull() ? value.asText() : null;
  }

  private Set<String> loadDisabledList() {
    if (!disabledFile.exists()) return new HashSet<>();
    try {
      Set<String> result = new HashSet<>();
      for (String line : java.nio.file.Files.readAllLines(disabledFile.toPath())) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) result.add(trimmed);
      }
      return result;
    } catch (IOException e) {
      Log.w(TAG, "Failed to read disabled.txt: " + e.getMessage());
      return new HashSet<>();
    }
  }

  private void saveDisabledList(Set<String> list) {
    try {
      List<String> sorted = new ArrayList<>(list);
      java.util.Collections.sort(sorted);
      java.nio.file.Files.write(disabledFile.toPath(), sorted);
    } catch (IOException e) {
      Log.w(TAG, "Failed to write disabled.txt: " + e.getMessage());
    }
  }

  private void checkDuplicates(List<PluginInfo> plugins) {
    for (PluginInfo plugin : plugins) {
      registry.put(plugin.getPackageName(), plugin);
      nameIndex.computeIfAbsent(plugin.manifest.name, k -> new ArrayList<>()).add(plugin);
    }

    Map<String, List<PluginInfo>> dupMap = new LinkedHashMap<>();
    for (PluginInfo plugin : plugins) {
      String key = plugin.manifest.author + "@" + plugin.manifest.name;
      dupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(plugin);
    }

    List<List<PluginInfo>> fatalDuplicates = new ArrayList<>();
    for (List<PluginInfo> dups : dupMap.values()) {
      if (dups.size() > 1) fatalDuplicates.add(dups);
    }
    if (!fatalDuplicates.isEmpty()) {
      hasFatalError = true;
      Log.e(
          TAG,
          "Same plugin with different versions detected, aborting. Remove one version and restart.");
      for (List<PluginInfo> dups : fatalDuplicates) {
        for (PluginInfo d : dups) {
          Log.e(TAG, "  " + d.getPackageName());
        }
      }
    }
  }

  private List<PluginInfo> resolveDuplicates(List<PluginInfo> plugins) {
    Map<String, PluginInfo> byName = new HashMap<>();
    List<PluginInfo> result = new ArrayList<>();
    for (PluginInfo plugin : plugins) {
      PluginInfo existing = byName.get(plugin.manifest.name);
      if (existing != null) {
        if (existing.enabled) plugin.enabled = false;
      } else {
        byName.put(plugin.manifest.name, plugin);
      }
      result.add(plugin);
    }
    return result;
  }

  private boolean validatePlugin(
      PluginInfo plugin, List<PluginInfo> allPlugins, Map<String, PluginInfo> providers) {
    Manifest.DependsSpec deps = plugin.manifest.depends;
    if (deps == null) return true;
    boolean valid = true;

    if (deps.required != null) {
      for (String entry : deps.required) {
        DependencyRef ref = parseDependencyEntry(entry);
        PluginInfo resolved = resolvePackage(ref.packageName, allPlugins, providers);
        if (resolved == null) {
          valid = false;
          Log.e(TAG, plugin.getPackageName() + " missing required dependency: " + ref.packageName);
        }
      }
    }

    if (deps.optional != null) {
      for (String entry : deps.optional) {
        DependencyRef ref = parseDependencyEntry(entry);
        PluginInfo resolved = resolvePackage(ref.packageName, allPlugins, providers);
        if (resolved == null) {
          Log.w(TAG, plugin.getPackageName() + " missing optional dependency: " + ref.packageName);
        }
      }
    }

    Manifest.ConflictsSpec conflicts = plugin.manifest.conflicts;
    if (conflicts != null) {
      if (conflicts.breakList != null) {
        for (String entry : conflicts.breakList) {
          DependencyRef ref = parseDependencyEntry(entry);
          PluginInfo conflicted = resolvePackage(ref.packageName, allPlugins, providers);
          if (conflicted != null && conflicted.enabled) {
            valid = false;
            conflicted.enabled = false;
            Log.e(
                TAG,
                plugin.getPackageName() + " conflicts with " + ref.packageName + ", not loading");
          }
        }
      }
      if (conflicts.incompatible != null) {
        for (String entry : conflicts.incompatible) {
          DependencyRef ref = parseDependencyEntry(entry);
          PluginInfo conflicted = resolvePackage(ref.packageName, allPlugins, providers);
          if (conflicted != null && conflicted.enabled) {
            Log.w(TAG, plugin.getPackageName() + " is incompatible with " + ref.packageName);
          }
        }
      }
    }

    return valid;
  }

  private PluginInfo resolvePackage(
      String target, List<PluginInfo> allPlugins, Map<String, PluginInfo> providers) {
    for (PluginInfo plugin : allPlugins) {
      if (plugin.getPackageName().equals(target) && plugin.enabled) return plugin;
    }
    PluginInfo provided = providers.get(target);
    if (provided != null && provided.enabled) return provided;
    return null;
  }

  private List<String> detectCircularDependency(List<PluginInfo> plugins) {
    Map<String, List<String>> graph = new HashMap<>();
    Map<String, PluginInfo> pkgToPlugin = new HashMap<>();
    for (PluginInfo plugin : plugins) pkgToPlugin.put(plugin.getPackageName(), plugin);

    for (PluginInfo plugin : plugins) {
      if (plugin.manifest.depends == null || plugin.manifest.depends.required == null) continue;
      for (String entry : plugin.manifest.depends.required) {
        DependencyRef ref = parseDependencyEntry(entry);
        if (pkgToPlugin.containsKey(ref.packageName)) {
          graph
              .computeIfAbsent(plugin.getPackageName(), k -> new ArrayList<>())
              .add(ref.packageName);
        }
      }
    }

    Set<String> visited = new HashSet<>();
    List<String> recursionStack = new ArrayList<>();
    DfsResult result = new DfsResult();

    for (String pkg : graph.keySet()) {
      if (result.cycle != null) break;
      dfs(pkg, graph, visited, recursionStack, pkgToPlugin, result);
    }
    return result.cycle;
  }

  private static class DfsResult {
    List<String> cycle;
  }

  private void dfs(
      String pkg,
      Map<String, List<String>> graph,
      Set<String> visited,
      List<String> recursionStack,
      Map<String, PluginInfo> pkgToPlugin,
      DfsResult result) {
    if (result.cycle != null) return;
    if (recursionStack.contains(pkg)) {
      List<String> cycle =
          recursionStack.subList(recursionStack.indexOf(pkg), recursionStack.size());
      cycle.add(pkg);
      result.cycle = cycle;
      return;
    }
    if (visited.contains(pkg)) return;
    visited.add(pkg);
    recursionStack.add(pkg);
    List<String> deps = graph.get(pkg);
    if (deps != null) {
      for (String dep : deps) {
        dfs(dep, graph, visited, recursionStack, pkgToPlugin, result);
        if (result.cycle != null) return;
      }
    }
    recursionStack.remove(recursionStack.size() - 1);
  }

  private void loadPluginScript(PluginInfo plugin) {
    if (plugin.enabled) {
      configManager.copyResources(plugin);
    }

    Globals globals = luaEngine.createGlobals();
    plugin.globals = globals;

    registerModuleHelpers(globals, plugin);
    registerModuleConfigHelpers(globals, plugin);
    registerExportImport(globals, plugin);

    File mainScript = new File(plugin.getScriptsDir(), plugin.manifest.main);
    if (!mainScript.exists()) {
      Log.w(
          TAG,
          "Plugin "
              + plugin.getPackageName()
              + " main script "
              + plugin.manifest.main
              + " not found");
      return;
    }

    try {
      globals.set("SCRIPT_NAME", LuaValue.valueOf(plugin.manifest.name));
      LuaValue chunk = globals.loadfile(mainScript.getAbsolutePath());
      chunk.call();

      LuaValue onEnable = globals.get("onEnable");
      plugin.onEnableFunc = onEnable.isfunction() ? onEnable : null;
      LuaValue onDisable = globals.get("onDisable");
      plugin.onDisableFunc = onDisable.isfunction() ? onDisable : null;

      plugin.loaded = true;
      Log.i(TAG, "Plugin " + plugin.getPackageName() + " loaded");
    } catch (LuaError e) {
      Log.w(TAG, "Plugin " + plugin.getPackageName() + " Lua error: " + e.getMessage());
      plugin.loaded = false;
    } catch (Exception e) {
      Log.w(TAG, "Plugin " + plugin.getPackageName() + " failed to load: " + e.getMessage());
      plugin.loaded = false;
    }
  }

  private void registerModuleHelpers(Globals globals, PluginInfo plugin) {
    globals.set(
        "export",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue name, LuaValue value) {
            if (!plugin.manifest.expose) {
              Log.w(TAG, "Plugin " + plugin.getPackageName() + " used export() without expose");
              return LuaValue.NIL;
            }
            plugin.exportedFunctions.set(name, value);
            return LuaValue.NIL;
          }
        });

    globals.set(
        "import",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue packageName) {
            String target = packageName.tojstring();
            PluginInfo targetPlugin = registry.get(target);
            if (targetPlugin == null) throw new LuaError("plugin not found: " + target);
            if (!targetPlugin.manifest.expose)
              throw new LuaError("plugin " + target + " does not expose");
            if (!targetPlugin.loaded) throw new LuaError("plugin " + target + " is not loaded");
            return targetPlugin.exportedFunctions != null
                ? targetPlugin.exportedFunctions
                : new LuaTable();
          }
        });
  }

  private void registerModuleConfigHelpers(Globals globals, PluginInfo plugin) {
    globals.set(
        "loadPluginConfig",
        new org.luaj.vm2.lib.ZeroArgFunction() {
          @Override
          public LuaValue call() {
            return configManager.loadPluginConfig(plugin.manifest.name);
          }
        });

    globals.set(
        "savePluginConfig",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue configValue) {
            configManager.savePluginConfig(plugin.manifest.name, configValue);
            return LuaValue.NIL;
          }
        });
  }

  private void registerExportImport(Globals globals, PluginInfo plugin) {
    LuaTable exportTable = new LuaTable();
    plugin.exportedFunctions = exportTable;
    globals.set("__exports", exportTable);
  }

  private void callOnEnable(PluginInfo plugin) {
    LuaValue func = plugin.onEnableFunc;
    if (func == null) return;
    try {
      func.call();
    } catch (LuaError e) {
      Log.w(TAG, "Plugin " + plugin.getPackageName() + " onEnable error: " + e.getMessage());
    }
  }

  private void callOnDisable(PluginInfo plugin) {
    LuaValue func = plugin.onDisableFunc;
    if (func == null) return;
    try {
      func.call();
    } catch (LuaError e) {
      Log.w(TAG, "Plugin " + plugin.getPackageName() + " onDisable error: " + e.getMessage());
    }
  }

  public static DependencyRef parseDependencyEntry(String entry) {
    String s = entry.trim();
    int atIdx = s.indexOf('@');
    if (atIdx == -1)
      throw new IllegalArgumentException("Invalid dependency format, missing '@': " + entry);
    String afterAt = s.substring(atIdx + 1);
    int firstColon = afterAt.indexOf(':');
    if (firstColon == -1)
      throw new IllegalArgumentException("Invalid dependency format, missing ':': " + entry);
    String name = afterAt.substring(0, firstColon);
    String rest = afterAt.substring(firstColon + 1);
    int secondColon = rest.indexOf(':');
    String version;
    String description;
    if (secondColon == -1) {
      version = rest.trim();
      description = null;
    } else {
      version = rest.substring(0, secondColon).trim();
      description = rest.substring(secondColon + 1).trim();
    }
    String author = s.substring(0, atIdx);
    String pkg = author + "@" + name + ":" + version;
    return new DependencyRef(pkg, description);
  }
}
