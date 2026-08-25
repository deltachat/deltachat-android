# DeltaX 插件系统 API 文档

> 本文档基于 `deltachat-android` 仓库中 `org.thoughtcrime.securesms.deltax` 包的实际实现编写，
> 覆盖 **Java 引擎层**、**插件生命周期**、**manifest 规范**、**打包格式**，以及插件在 Lua 中可用的
> **全部全局函数（Bridge API）**。
>
> 引擎名称：`DeltaX`，版本：`1.0`（见 `DeltaX.ENGINE_NAME` / `DeltaX.ENGINE_VERSION`）。
> 本系统由 `deltachatx-android` 的 MinecraftX 引擎移植而来，去除了游戏服务器相关部分
> （事件、广播、标题等），保留纯 Java↔Lua 互操作能力。

---

## 目录

1. [架构总览](#1-架构总览)
2. [Java 引擎层 API](#2-java-引擎层-api)
   - [DeltaX](#21-deltax)
   - [LuaEngine](#22-luaengine)
   - [PluginLoader](#23-pluginloader)
   - [PluginPackager](#24-pluginpackager)
   - [Manifest](#25-manifest)
   - [PluginInfo](#26-plugininfo)
   - [ConfigManager](#27-configmanager)
   - [LuaTableUtil](#28-luatableutil)
   - [Bridge 类](#29-bridge-类)
3. [插件生命周期](#3-插件生命周期)
4. [manifest.json 规范](#4-manifestjson-规范)
5. [插件打包格式](#5-插件打包格式)
6. [Lua 全局 API（插件侧）](#6-lua-全局-api插件侧)
   - [引擎内置全局变量](#61-引擎内置全局变量)
   - [模块辅助函数](#62-模块辅助函数)
   - [LuaProxyBridge](#63-luaproxybridge)
   - [LambdaBridge](#64-lambdabridge)
   - [AnnotationBridge](#65-annotationbridge)
   - [AsyncBridge（并发与异步）](#66-asyncbridge并发与异步)
   - [ReflectionBridge（反射）](#67-reflectionbridge反射)
   - [CollectionBridge（集合/流/转换）](#68-collectionbridge集合流转换)
7. [插件示例](#7-插件示例)
8. [与 deltachat 主程序的集成](#8-与-deltachat-主程序的集成)
9. [注意事项与限制](#9-注意事项与限制)

---

## 1. 架构总览

```
DeltaX (单例引擎)
 ├─ LuaEngine         每插件创建独立 Lua Globals
 ├─ PluginLoader      扫描/校验/加载/启用/停用 插件
 ├─ PluginPackager    安装/卸载/打包/解压 插件
 ├─ ConfigManager     读写 <baseDir>/config/<name>/config.json
 └─ baseDir = <app files>/DeltaX
       ├─ plugins/                已安装插件目录（每个插件一个子目录）
       │    ├─ <pluginDir>/
       │    │    ├─ manifest.json
       │    │    ├─ scripts/       Lua 入口与脚本
       │    │    └─ resources/     资源（加载时拷贝到 config 目录）
       │    └─ disabled.txt        被停用的插件包名列表
       └─ config/<pluginName>/    插件配置（JSON）与资源副本
```

入口类：`DeltaXActivity`（图形化插件管理器，见第 8 节）。
数据目录：`Context.getFilesDir()/DeltaX`，其中 `plugins/` 为用户可见的插件安装位置。

---

## 2. Java 引擎层 API

### 2.1 DeltaX

引擎核心单例（`org.thoughtcrime.securesms.deltax.DeltaX`）。

| 成员 | 类型 | 说明 |
|------|------|------|
| `ENGINE_NAME` | `String` | 常量 `"DeltaX"` |
| `ENGINE_VERSION` | `String` | 常量 `"1.0"` |
| `getInstance(Context)` | `static synchronized DeltaX` | 获取（并惰性创建）单例，传入 `Context` |
| `DeltaX(Context, int)` | 构造 | 第二个参数为账号 id；内部使用 `context.getApplicationContext()`；根据当前账号目录解析 `extensionDir = <账号目录>/extension`，并初始化 `pluginsDir = <extensionDir>/plugin`、`ConfigManager(<extensionDir>)`、`LuaEngine`、`PluginLoader`、`PluginPackager` |
| `getInstance(Context)` | `DeltaX` | 自动取当前选中账号 id，返回该账号对应的 `DeltaX` 实例（按账号缓存） |
| `getInstance(Context, int)` | `DeltaX` | 取指定账号对应的 `DeltaX` 实例（按账号缓存，互不干扰） |
| `isInitialised()` | `boolean` | 是否已 `init()` |
| `init()` | `void` | 创建目录并 `loadPlugins()`；重复调用安全（幂等） |
| `shutdown()` | `void` | 调用 `PluginLoader.shutdown()`，触发各插件 `onDisable` |
| `reloadPlugins()` | `void` | 卸载全部 → 重新扫描加载 |
| `getLoadedPlugins()` | `List<String>` | 已加载插件的包名列表 |
| `getPluginList()` | `List<String>` | 文本化的插件清单（含 `[x]/[ ]` 状态，供调试） |
| `getPlugin(String)` | `PluginInfo` | 按名字或包名查找 |
| `getLuaEngine()` | `LuaEngine` | 底层引擎 |
| `getPluginLoader()` | `PluginLoader` | 加载器 |
| `getPluginPackager()` | `PluginPackager` | 打包器 |
| `getContext()` | `Context` | 应用上下文 |
| `getAccountId()` | `int` | 当前实例所属的账号 id |
| `getExtensionDir()` | `File` | `<账号目录>/extension`（本账号的插件空间根目录） |
| `getBaseDir()` | `File` | 同 `getExtensionDir()`，兼容旧名 |
| `getPluginsDir()` | `File` | `<账号目录>/extension/plugin` |
| `getInstalledPlugins()` | `List<PluginInfo>` | 所有**已安装**（不论是否启用）的插件 |
| `installPluginFromZip(File)` | `int` | 从 zip 包安装（仅作用于当前账号空间），返回安装的插件数量，并 `reloadPlugins()` |
| `uninstallPlugin(String)` | `boolean` | 按包名卸载（仅当前账号空间），并 `reloadPlugins()` |
| `setPluginEnabled(String, boolean)` | `void` | `true`→`enablePlugin`，`false`→`disablePlugin`，随后 `reloadPlugins()` |
| `isPluginDisabled(String)` | `boolean` | 是否在被停用列表中 |

> 注意：启用/停用并不会立即热加载，而是写入 `disabled.txt` 后重新加载（`reloadPlugins`）。

## 账号隔离（per-account）

每个 DeltaChat 账号拥有**独立**的插件空间，互不共享：

```
<账号目录>/extension/
├── plugin/      # 已安装插件（每个插件一个目录，含 manifest.json）
└── config/     # 各插件的配置（<插件名>/config.json 及 resources 拷贝）
```

- 账号目录取自当前选中账号 `DcContext.getBlobdir()` 的父目录；无账号时回退到 `filesDir/DeltaX`。
- `DeltaX` 按账号 id 缓存实例，`getInstance(context)` 自动绑定当前账号，因此插件列表、安装、卸载、配置都只影响当前账号。
- 切换账号后，插件页面对应账号的 `extension/` 空间，彼此不互通。
- 旧版全局目录 `filesDir/DeltaX/{plugins,config}` 不会被自动迁移；如需把已有插件带入某账号，请在该账号下重新安装。


### 2.2 LuaEngine

为**每个插件**构造独立的 Lua 环境（`org.thoughtcrime.securesms.deltax.LuaEngine`）。

| 方法 | 说明 |
|------|------|
| `createGlobals()` | 返回 `JsePlatform.standardGlobals()`，并注册：<br>• `luajava.bindClass(className)` —— 按名加载类（使用应用 `ClassLoader`）<br>• `context` —— 应用 `Context`（Java 对象）<br>• `deltax` —— `DeltaX` 单例（Java 对象）<br>• `log(level, message)` —— 日志桥<br>• `JavaBridge.registerAll(...)` 注册的全部 Lua 全局函数（见第 6 节） |
| `runScript(File, Globals)` | 以 `loadfile` 执行脚本，设置 `SCRIPT_NAME` 后 `chunk.call()` |

`log(level, message)`：级别不区分大小写，`ERROR/SEVERE`→`Log.e`、 `WARN/WARNING`→`Log.w`、 `DEBUG`→`Log.d`、 `VERBOSE`→`Log.v`、其它→`Log.i`，均带 `TAG = "DeltaX"`。

### 2.3 PluginLoader

负责扫描、校验、依赖解析、加载、启用/停用（`module.PluginLoader`）。

**加载流程（`loadPlugins()`）：**
1. `scanPlugins()`：遍历 `pluginsDir` 子目录，解析 `manifest.json`。
2. 读取 `disabled.txt`，标记停用。
3. `checkDuplicates()`：若同一 `author@name` 存在多个版本 → 致命错误，中止全部加载。
4. `resolveDuplicates()`：同名插件只启用第一个，其余标记停用。
5. 依赖校验（`validatePlugin`）：校验 `depends.required`、`conflicts.break`。
6. `detectCircularDependency()`：检测依赖环，有环则中止。
7. 解析 `provides` 提供表。
8. 逐个 `loadPluginScript()`（执行 `main` 脚本，捕获 `onEnable`/`onDisable`）。
9. 已加载插件依次调用 `onEnable`。

**公开方法：**

| 方法 | 说明 |
|------|------|
| `loadPlugins()` | 返回成功加载并启用的 `List<PluginInfo>` |
| `shutdown()` | 对所有已加载插件调用 `onDisable` |
| `getPluginList()` / `getPluginListFilterStatus(bool)` / `getPluginListFilterAuthor(String)` | 文本化列表（调试用途） |
| `getPluginInfo(String)` | 返回该插件信息的字符串列表 |
| `disablePlugin(String)` / `enablePlugin(String)` | 写入/移除 `disabled.txt` |
| `isDisabled(String)` | 是否在停用列表 |
| `getPluginNames()` / `getPluginAuthors()` | 集合 |
| `getPlugin(String)` | 按名或包名查找 |
| `registerPlugin` / `unregisterPlugin` | 注册表维护 |
| `loadPlugin` / `unloadPlugin` | 单插件热加载/卸载 |
| `parseManifest(File)` | 解析 `manifest.json`（Jackson），字段缺失则返回 `null` |
| `static parseDependencyEntry(String)` | 解析依赖条目 `author@name:version[:description]` |

**依赖条目格式**：`<author>@<name>:<version>`（可选追加 `:<description>`）。例如 `"Andy@demo:1.0"`。

### 2.4 PluginPackager

安装/卸载/归档/打包/解压（`module.PluginPackager`）。

| 方法 | 说明 |
|------|------|
| `getPluginInfo(String)` | 取已安装插件信息 |
| `parseManifest(File)` | 解析 `manifest.json` |
| `install(File moduleDir)` | 将目录复制到 `plugins/<name>`；若目标已存在则返回 `false` |
| `remove(String)` / `uninstall(String)` | 删除 `plugins/<package>` 目录 |
| `archive(String)` | 打包为 `plugins/<name>.zip` |
| `pack(String, File)` | 打包为 Jar |
| `getInstalledPlugins()` | 返回 `List<PluginInfo>` |
| `installFromZip(File)` | **核心安装入口**：解压 zip（含 zip 穿越防护），支持一个 zip 内含多个插件目录（各含 `manifest.json`），逐个 `install()`，返回安装数量 |
| `unzip(File, File)` | 内部：带 canonical-path 校验的解压 |

### 2.5 Manifest

`module.Manifest`（Jackson，忽略未知字段）。

| 字段 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `name` | `String` | 是 | 插件显示名 |
| `version` | `String` | 是 | 版本 |
| `main` | `String` | 是 | 入口脚本名（位于 `scripts/` 下，如 `main.lua`） |
| `author` | `String` | 是 | 作者 |
| `description` | `String` | 否 | 描述 |
| `expose` | `boolean` | 否 | 是否允许被其它插件 `import` |
| `depends` | `DependsSpec` | 否 | `{ "required": [...], "optional": [...] }` |
| `conflicts` | `ConflictsSpec` | 否 | `{ "break": [...], "incompatible": [...] }` |
| `provides` | `List<String>` | 否 | 提供的包名（供依赖解析） |

`getPackageName()` 返回 **`author + "@" + name + ":" + version`**（唯一标识）。

### 2.6 PluginInfo

`module.PluginInfo`（插件运行时状态载体）。

| 字段/方法 | 说明 |
|----------|------|
| `manifest` | `Manifest` |
| `pluginDir` | 插件目录 `File` |
| `enabled` / `loaded` | 运行时状态 |
| `globals` / `exportedFunctions` / `onEnableFunc` / `onDisableFunc` | Lua 绑定 |
| `getPackageName()` | 委托 `manifest.getPackageName()` |
| `getScriptsDir()` | `pluginDir/scripts` |
| `getResourcesDir()` | `pluginDir/resources` |

### 2.7 ConfigManager

按插件名管理 JSON 配置（`module.ConfigManager`）。

- 配置根：`baseDir/config/<pluginName>/config.json`
- 资源：插件 `resources/` 在加载时拷贝到 `config/<name>/`（仅当目标不存在时拷贝）
- `loadPluginConfig(name)` → `LuaTable`（文件不存在返回空表）
- `savePluginConfig(name, value)` → 美化写入 JSON
- `reloadPluginConfig(name)` / `deletePluginConfig(name)`

### 2.8 LuaTableUtil

`org.thoughtcrime.securesms.deltax.LuaTableUtil`：Jackson `JsonNode` ↔ Lua table 互转。

- `jsonToLua(JsonNode)`：对象→表、数组→表、文本/布尔/数值→对应 Lua 类型。
- `luaToJson(LuaValue)`：根据是否“数组形”（连续整数键）决定生成 `ArrayNode` 或 `ObjectNode`。
- `luaTableToStringList(LuaValue)`：把数组形表转为 `List<String>`。

### 2.9 Bridge 类

`bridge` 包下的一组全局函数注册器，由 `JavaBridge.registerAll(globals)` 统一安装：

| 类 | 职责 |
|----|------|
| `JavaBridge` | 聚合注册下列全部 Bridge |
| `LuaProxyBridge` | 用 Lua 表实现 Java 接口（`createProxy` 等） |
| `LambdaBridge` | 用 Lua 函数实现函数式接口（SAM） |
| `AnnotationBridge` | 读取 Java 注解 |
| `AsyncBridge` | `CompletableFuture`、线程池、原子类、锁 |
| `ReflectionBridge` | 字段/方法/构造器/枚举/数组/类的反射 |
| `CollectionBridge` | List/Map/Set/Optional/Stream 与 Lua 表的互转 |

---

## 3. 插件生命周期

1. **安装**：用户通过 `DeltaXActivity` 选择本地 `.zip` → `DeltaX.installPluginFromZip()` → `PluginPackager.installFromZip()` 解压并 `install()`。
2. **加载**：`init()` / `reloadPlugins()` → `PluginLoader.loadPlugins()`：
   - 扫描 `plugins/*/manifest.json`
   - 依赖/冲突/环校验
   - 为每个插件 `createGlobals()`（独立环境）
   - 注册模块辅助函数（`export`/`import`/`loadPluginConfig`/`savePluginConfig`）
   - `loadfile(scripts/<main>)` 并执行；捕获全局 `onEnable` / `onDisable` 函数
   - 调用 `onEnable()`
3. **运行**：插件通过桥接函数访问 Java/Android API，可 `export` 函数供其它插件 `import`。
4. **停用/启用**：`setPluginEnabled()` 改写 `disabled.txt` 后重新加载。
5. **卸载**：`uninstallPlugin()` 删除目录并重新加载。
6. **关闭**：`shutdown()` → 各插件 `onDisable()`。

**Lua 侧入口函数（可选）：**
```lua
function onEnable()
  log("INFO", "plugin enabled")
end

function onDisable()
  log("INFO", "plugin disabled")
end
```

---

## 4. manifest.json 规范

最小示例：
```json
{
  "name": "demo",
  "version": "1.0",
  "main": "main.lua",
  "author": "Andy",
  "description": "A demo plugin",
  "expose": false,
  "depends": {
    "required": ["SomeAuthor@other:1.0"],
    "optional": ["Foo@bar:2.0"]
  },
  "conflicts": {
    "break": ["Baz@qux:1.0"],
    "incompatible": ["X@y:0.1"]
  },
  "provides": ["Andy@demo:1.0"]
}
```

- 缺 `name`/`version`/`main`/`author` 任一 → 该插件被跳过（`parseManifest` 返回 `null`）。
- 入口脚本路径 = `scripts/<main>`（例如 `scripts/main.lua`）。
- `depends.required` 任一缺失 → 该插件**不加载**；`optional` 缺失仅告警。
- `conflicts.break` 命中已启用插件 → 本插件不加载，且冲突方被停用；`incompatible` 命中仅告警。
- 同名多版本 → 全部中止加载（致命）。

---

## 5. 插件打包格式

采用 **MinecraftX / DeltaX 模块包**格式：一个 `.zip` 内可包含**多个**插件目录，每个插件目录各自带 `manifest.json`。

```
package.zip
 ├─ plugin_a/
 │   ├─ manifest.json
 │   ├─ scripts/main.lua
 │   └─ resources/...
 └─ plugin_b/
     ├─ manifest.json
     └─ scripts/main.lua
```

也可直接是单插件根目录（`package.zip/manifest.json` 存在即视为一个插件）。
安装时 `installFromZip` 会遍历候选目录逐个 `install()`，返回安装的插件数量。

---

## 6. Lua 全局 API（插件侧）

> 所有 Java 对象在 Lua 中以 *userdata* 形式存在；`nil` 对应 Java `null`。
> 参数/返回值自动在 Lua 与 Java 间 coerce（`CoerceJavaToLua` / `CoerceLuaToJava`）。

### 6.1 引擎内置全局变量

| 全局 | 类型 | 说明 |
|------|------|------|
| `context` | Java `Context` | 应用上下文，可借此访问 deltachat 内部（需反射，见 9） |
| `deltax` | Java `DeltaX` | 引擎单例，可调用 2.1 所列方法 |
| `luajava` | table | 标准 luaj 库；已扩展 `bindClass(className)` |
| `log(level, msg)` | function | 日志（见 2.2） |
| `SCRIPT_NAME` | string | 当前脚本文件名（不含扩展名） |

示例：
```lua
local ctx = context
local pkg = deltax:getPackageName and "n/a" or nil
log("INFO", "hello from " .. SCRIPT_NAME)
```

### 6.2 模块辅助函数

由 `PluginLoader` 注入（每个插件作用域内）：

| 函数 | 签名 | 说明 |
|------|------|------|
| `export` | `export(name, value)` | 仅当 `manifest.expose=true` 时有效；把 `value` 注册到本插件的导出表，供其它插件 `import` |
| `import` | `import("author@name:version")` | 返回目标插件导出的函数表（`__exports`）；目标必须 `expose=true` 且已加载，否则抛 Lua 错误 |
| `loadPluginConfig` | `loadPluginConfig()` | 读取本插件配置，返回 `LuaTable`（不存在返回空表） |
| `savePluginConfig` | `savePluginConfig(table)` | 保存本插件配置到 JSON |

示例（插件 A 暴露，插件 B 使用）：
```lua
-- 插件 A (expose=true)
function greet(name) return "hi " .. name end
export("greet", greet)

-- 插件 B
local a = import("Andy@A:1.0")
log("INFO", a.greet("world"))
```

### 6.3 LuaProxyBridge

| 函数 | 说明 |
|------|------|
| `createProxy(className, methodsTable)` | 为接口（或其实现的接口）生成代理；`methodsTable` 的 key 为方法名、value 为 Lua 函数 |
| `createProxyFor(tableOfInterfaces, methodsTable)` | 同上，但接口列表由 table 显式给出 |
| `isProxy(obj)` | 是否为 Lua 代理 |
| `getProxyHandler(obj)` | 取回代理背后的 Lua 方法表（`nil` 若非代理） |

```lua
local runnable = createProxy("java.lang.Runnable", {
  run = function() log("INFO", "running") end
})
```

### 6.4 LambdaBridge

| 函数 | 说明 |
|------|------|
| `createSAM(className, func)` | 为单抽象方法接口生成代理 |
| `wrapLambda(func, interfaceClass)` | 同 `createSAM`，参数为 (函数, 接口类全名) |
| `isFunctionalInterface(className)` | 是否为函数式接口 |
| `createRunnable(func)` | `java.lang.Runnable` |
| `createSupplier(func)` | `java.util.function.Supplier` |
| `createConsumer(func)` | `java.util.function.Consumer` |
| `createFunction(func)` | `java.util.function.Function` |
| `createPredicate(func)` | `java.util.function.Predicate` |

```lua
local r = createRunnable(function() log("INFO", "tick") end)
```

### 6.5 AnnotationBridge

| 函数 | 说明 |
|------|------|
| `getAnnotations(obj)` | 取对象/类/方法/字段的全部注解 → table |
| `getAnnotation(obj, annClass)` | 取指定类型注解（无则 `nil`） |
| `hasAnnotation(obj, annClass)` | 是否存在该注解 |
| `getAnnotationsByType(obj, annClass)` | 重复注解 → table |
| `getDeclaredAnnotations(obj)` | 仅直接声明注解 |
| `getMethodAnnotations(methodObj)` | 方法的注解 |
| `getFieldAnnotations(fieldObj)` | 字段的注解 |
| `getParameterAnnotations(methodObj, idx)` | 方法第 `idx` 个参数的注解 |

注解以 table 表示，含字段 `annotationType`（类名）及注解各属性。

### 6.6 AsyncBridge（并发与异步）

底层线程池为 daemon 线程（`DeltaX-Async-*`）。

**CompletableFuture 链：**
| 函数 | 说明 |
|------|------|
| `async(func)` | 提交执行，返回 `CompletableFuture` |
| `asyncWithDelay(delayMs, func)` | 延迟执行 |
| `thenApply(future, func)` | 映射结果 |
| `thenAccept(future, func)` | 消费结果（无返回） |
| `thenRun(future, func)` | 完成后运行 |
| `exceptionHandler(future, func)` | 异常处理 |
| `allOf(...)` / `anyOf(...)` | 组合多个 future |
| `await(future[, timeoutMs])` | **阻塞**等待结果（可超时） |
| `isDone(future)` / `isCancelled(future)` / `cancelFuture(future)` | 状态查询/取消 |

**线程池：**
| 函数 | 说明 |
|------|------|
| `newExecutor(name)` | 缓存线程池 |
| `newFixedPool(name, threads)` | 固定大小线程池 |
| `submitToExecutor(exec, func)` | 提交到指定池，返回 future |

**同步原语：**
| 函数 | 说明 |
|------|------|
| `sleep(millis)` | `Thread.sleep` |
| `newAtomicInt(n)` / `newAtomicLong(n)` / `newAtomicBool(b)` | 原子变量 |
| `newCountDownLatch(n)` / `newCyclicBarrier(n)` | 闭锁/栅栏 |
| `semaphore(permits)` | 信号量 |
| `lock()` | `ReentrantLock` |

```lua
local f = async(function() return 42 end)
local r = await(thenApply(f, function(x) return x + 1 end))
log("INFO", tostring(r))   -- 43
```

### 6.7 ReflectionBridge（反射）

**字段：**
| 函数 | 说明 |
|------|------|
| `getField(obj, name)` / `setField(obj, name, value)` | 实例字段（含父类，setAccessible） |
| `getStaticField(class, name)` / `setStaticField(class, name, value)` | 静态字段 |
| `getDeclaredFields(class)` / `getFields(class)` | 字段信息表 |

**方法：**
| 函数 | 说明 |
|------|------|
| `getMethods(class)` / `getDeclaredMethods(class)` | 方法对象表 |
| `invokeMethod(obj, name, ...)` | 实例方法调用（按参数个数匹配） |
| `invokeStatic(class, name, ...)` | 静态方法调用 |
| `methodInfo(methodObj)` | 方法元信息（name/returnType/modifiers/parameterTypes…） |

**构造器/枚举/数组/类：**
| 函数 | 说明 |
|------|------|
| `getConstructors(class)` / `getDeclaredConstructors(class)` | 构造器表 |
| `newInstance(class, ...)` | 构造实例 |
| `enumValues(class)` / `enumValueOf(class, name)` / `enumName(e)` / `enumOrdinal(e)` | 枚举操作 |
| `arrayLength(arr)` / `arrayGet(arr, i)` / `arraySet(arr, i, v)` / `newArray(className, len)` / `newPrimitiveArray(type, len)` / `arrayToList(arr)` | 数组操作（索引 **1-based**） |
| `classOf(obj)` / `className(c)` / `classSimpleName(c)` / `superClass(c)` / `interfaces(c)` / `isAssignableFrom(a,b)` / `isInstance(c,obj)` / `cast(c,obj)` | 类信息 |
| `isPublic/isPrivate/isStatic/isFinal/isAbstract(mods)` | 修饰符判断 |

```lua
local clazz = luajava.bindClass("java.lang.Math")
local pi = invokeStatic(clazz, "abs", -3)   -- 3
```

### 6.8 CollectionBridge（集合/流/转换）

**List：**
`newArrayList(...)`、`newLinkedList()`、`listSize`、`listGet`(1-based)、`listSet`、`listAdd`、`listRemove`、`listRemoveAt`、`listClear`、`listContains`、`listIsEmpty`、`listSort([comparator])`、`listToTable`。

**Map：**
`newHashMap()`、`newLinkedHashMap()`、`newTreeMap()`、`mapPut`、`mapGet`、`mapRemove`、`mapContainsKey`、`mapContainsValue`、`mapKeys`、`mapValues`、`mapSize`、`mapIsEmpty`、`mapClear`、`mapToTable`。

**Set：**
`newHashSet(...)`、`setAdd`、`setRemove`、`setContains`、`setSize`、`setToTable`、`setClear`。

**Optional / Stream：**
`optionalOf`、`optionalEmpty`、`optionalIsPresent`、`optionalGet`、`optionalOrElse`；
`streamToList/ToSet/Map/Filter/ForEach/Count/Collect/Distinct/Sorted/Limit/Skip/FindFirst/AnyMatch/AllMatch/NoneMatch`，以及 `collectorsToList/ToSet/Joining/ToMap/GroupingBy`。

**Lua ↔ Java 转换：**
| 函数 | 说明 |
|------|------|
| `asList(table)` / `asMap(table)` / `asSet(table)` | Lua table → Java 集合 |
| `toTable(javaCollection)` | Java 集合/数组/Map → Lua table |
| `toArray(table, componentType)` | Lua 数组表 → Java 数组 |
| `iteratorToTable(iter)` / `enumerationToTable(enum)` | 迭代器/枚举 → Lua table |

```lua
local list = newArrayList(1, 2, 3)
listAdd(list, 4)
log("INFO", tostring(listSize(list)))   -- 4
local t = listToTable(list)
```

---

## 7. 插件示例

目录结构（在 zip 内）：
```
myplugin/
 ├─ manifest.json
 └─ scripts/main.lua
```

`manifest.json`：
```json
{
  "name": "myplugin",
  "version": "1.0",
  "main": "main.lua",
  "author": "Andy",
  "description": "Example",
  "expose": false
}
```

`scripts/main.lua`：
```lua
function onEnable()
  log("INFO", "myplugin enabled, context=" .. tostring(context ~= nil))
  local cfg = loadPluginConfig()
  if cfg.greeting == nil then
    cfg.greeting = "hello"
    savePluginConfig(cfg)
  end
  log("INFO", "greeting=" .. cfg.greeting)
end

function onDisable()
  log("INFO", "myplugin disabled")
end
```

---

## 8. 与 deltachat 主程序的集成

- **入口 Activity**：`org.thoughtcrime.securesms.deltax.DeltaXActivity`
  - 卡片列表展示 `DeltaX.getInstalledPlugins()`（名称/版本/作者/描述）。
  - 右下角圆形 `+` 按钮（与主界面一致的 `PulsingFloatingActionButton`）通过
    `Intent.ACTION_GET_CONTENT`（`application/zip`）选择本地包 → `installPluginFromZip()`。
  - 每张卡片含“启用/停用”开关（`setPluginEnabled`）与“卸载”按钮（`uninstallPlugin`，带确认框）。
  - 空状态由 `deltax_empty` 字符串提示。
- **设置入口**：`preferences_advanced.xml` 中的 `pref_plugins`，于
  `AdvancedPreferenceFragment` 中启动 `DeltaXActivity`。
- **Manifest 声明**：`DeltaXActivity` 已在 `AndroidManifest.xml` 注册。
- **相关字符串**（英文 / 简体 / 繁体）：`plugins`、`deltax_title`（Plugins / 插件 / 外掛）、
  `deltax_install`、`deltax_install_success`、`deltax_install_failed`、
  `deltax_uninstall`、`deltax_confirm_uninstall`、`deltax_empty`。

---

## 9. 注意事项与限制

1. **Java 互操作是“全开”的**：插件可通过 `luajava.bindClass` + 反射桥接访问**任意**
   deltachat 类（如 `org.thoughtcrime.securesms.*`）。这属于**未受支持**的能力，
   跨版本极易失效，且存在安全风险——请勿依赖具体内部实现。
2. **启用/停用非热切换**：写入 `disabled.txt` 后**重新整体加载**插件。
3. **依赖解析基于包名**：`author@name:version` 必须精确匹配；`provides` 可别名提供。
4. **同名多版本是致命错误**：会中止全部插件加载，需手动删除其一。
5. **`export`/`import` 要求 `expose=true`**，且目标必须先于引用方加载（受加载顺序与依赖图影响）。
6. **配置为 JSON**：`LuaTableUtil` 按“连续整数键”判定数组形；混合键表会被当作对象（丢弃非字符串键）。
7. **线程安全**：异步桥在独立 daemon 线程执行 Lua；访问 deltachat UI/Android 主线程 API 时需自行切回主线程（可用 `context` 拿到 `Handler`/`runOnUiThread`）。
8. **包名唯一性**：安装目标目录为 `plugins/<name>`，同名已存在则 `install()` 返回 `false`。
9. **Lua 错误隔离**：单个插件 `onEnable`/`onDisable`/加载异常仅记录日志并标记该插件未加载，不影响其它插件。

---

## 10. 插件交互页面 API（DeltaXPage）

为了让 Lua 插件能高效地自建交互界面，引擎提供一套 **声明式 UI 构建器**。在插件管理列表
里点击某个插件卡片上的 **▶（打开）** 按钮，即会进入该插件的专属页面
（`DeltaXPluginActivity`），引擎调用插件的全局函数 **`onOpen(page)`**（若存在），由它用链式
API 描述界面；引擎把这些描述渲染为本机 Android 控件，并把输入实时绑定到该插件的
JSON 配置（`config/<name>/config.json`）。

### 10.1 生命周期与入口

- 点击 ▶ → `DeltaXActivity` 启动 `DeltaXPluginActivity`（携带包名 extra）。
- 页面打开时，若插件 `globals` 中存在 **`onOpen`** 函数，则 `onOpen(page)` 被调用，`page` 为
  `DeltaXPage` 的 Java 实例（已通过 luaj 转为 userdata，可直接 `page:method(...)` 调用）。
- 若未定义 `onOpen`，页面显示 `deltax_no_page` 提示。
- 页面底部固定一个 **保存** 按钮，调用 `page:save()`（写配置 + 可选 `onSave` 回调）。
- 配置读取自 `loadPluginConfig(name)`；所有 `input/switch/select/slider` 的改动先写入内存中的
  Lua table，点击 **保存** 时才落盘。

### 10.2 DeltaXPage 方法

| 方法 | 说明 |
|------|------|
| `page:title(text)` | 大标题 |
| `page:text(text)` | 段落说明文字 |
| `page:section(title)` | 分组小标题（分割） |
| `page:input(key, default, hint)` | 文本输入，绑定配置 `key` |
| `page:password(key, default, hint)` | 密码输入（掩码） |
| `page:toggle(key, defaultBool)` | 开关，绑定配置 `key` |
| `page:slider(key, min, max, step, default)` | 滑块，绑定配置 `key` |
| `page:select(key, optionsTable, default)` | 下拉选择，绑定配置 `key` |
| `page:button(label, callbackFn)` | 按钮，点击在**主线程**调用 `callbackFn()` |
| `page:get(key)` | 读取当前配置值（LuaValue） |
| `page:set(key, value)` | 写入当前配置值（内存） |
| `page:toast(text)` | 弹 Toast |
| `page:save()` | 持久化配置并触发 `onSave`（若有） |
| `page:close()` | 关闭页面（finish） |

> 所有方法均返回 `page` 自身，可链式书写：`page:title("A"):text("B"):input(...)`.

### 10.3 示例

```lua
function onOpen(page)
  page:title("我的插件")
  page:section("常规")
  page:text("下面可以修改配置，修改后点保存。")
  page:input("name", "", "你的昵称")
  page:toggle("enabled", true)
  page:select("mode", {"安静", "普通", "喧闹"}, "普通")
  page:slider("volume", 0, 100, 1, 50)
  page:button("测试", function()
    page:toast("name = " .. tostring(page:get("name")))
  end)
  page:button("保存", function() page:save() end)
end

function onSave()
  log("INFO", "config saved")
end
```

### 10.4 实现要点

- `DeltaXPage`（`org.thoughtcrime.securesms.deltax.ui`）持有插件 `PluginInfo`、配置 `LuaTable`
  与 `ConfigManager`，收集 `Widget` 描述列表。
- `DeltaXPluginActivity` 把每个 `Widget` 渲染为 `CardView` 包裹的本机控件
  （`EditText`/`Switch`/`SeekBar`/`Spinner`/`Button`），并把用户改动写回 `page` 的内存配置；
  按钮点击在主线程调用插件 Lua 回调。
- 依赖 `PluginLoader.getConfigManager()` 读写配置，配置落盘路径同第 2.7 节。
- 线程安全：页面内所有 Lua 执行均发生在主线程；若插件同时使用 `async` 桥，需注意不要与页面
  回调并发访问同一 Lua 状态。
