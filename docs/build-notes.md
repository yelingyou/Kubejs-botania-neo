# 构建说明（P0 环境 / build-notes）

> 本文件由 build-env 维护，记录 **kubejs-botania-neo** 工程的环境事实、可复现构建命令、libs 清单、
> 以及当前阻塞项（`createMinecraftArtifacts` 的 NFRT `recompile` 节点）与已尝试方案。
> 最后更新：2026-10-29 12:32（最终构建 BUILD SUCCESSFUL 后定稿）。

---

## 1. 工程路径与环境事实

| 项 | 值 |
|---|---|
| workspace | `<workspace>` |
| 工程根目录 | `<workspace>\kubejs-botania-neo` |
| modid | `kubejs_botania_neo` |
| 包名 | `dev.kubejsbotania.neo` |
| JDK | `<JDK21>`（JAVA_HOME 环境变量未设置，需在命令行设置） |
| Gradle Wrapper 版本 | **9.5.1**（见下方说明，非 8.14.5） |
| NeoForge | `21.1.248`（缓存固定，勿升级） |
| ModDevGradle | `2.0.142`（缓存固定，勿升级） |
| Parchment | `1.21.1` / `2024.11.17` |
| NeoFormRuntime (NFRT) | `2.0.22`（由 ModDevGradle 2.0.142 引入，离线缓存内） |
| 原始离线 Gradle home | `<workspace>/dev-tools\构建缓存-gradle-home`（**本会话沙箱下只读，不要直接当 GRADLE_USER_HOME**） |
| 实际使用的 GRADLE_USER_HOME | `<workspace>\.gradle-home`（原始 home 的完整可写镜像，1.33 GB） |
| Gradle 分发包 | `<user-home>/.gradle\wrapper\dists\gradle-9.5.1-bin\iq79hdu3mqx29lgffhp8bfmx\gradle-9.5.1`（已预置进 `.gradle-home/wrapper/dists`，包装器可离线启动） |

### 1.1 为什么 GRADLE_USER_HOME 必须是 `.gradle-home`（重要）

- 原始目录 `01-我的世界模组开发\构建缓存-gradle-home` 的目录完整性级别（integrity label）是 **Medium**，
  而本会话（DSH workspace-write 沙箱）内的进程 token 是 **Low**。Windows 强制完整性策略（No-Write-Up）
  导致对它的写入一律 `拒绝访问`，实测 `Set-Content` / Gradle 的 `*.lock`、`journal` 写入全部失败。
  因此**直接** `GRADLE_USER_HOME=...\构建缓存-gradle-home` 的构建无法启动。
- 解决方式：把原始 home **只读拷贝**到 workspace 下可写路径 `.gradle-home`（robocopy，1.33 GB，
  已在拷贝后核对大小与文件数），未修改原始目录任何内容。该路径也正是历史上 `开发脚本/build_loop.ps1`
  使用的 `<workspace>\.gradle-home`，因此内部绝对路径引用能对上。
- 拷贝日志：`docs/gradle-home-copy.log`（robocopy `/MT` 的 `0%` 进度行不代表失败；robocopy exit code 1 = 有文件成功复制）。

### 1.2 为什么 wrapper 是 Gradle 9.5.1 而不是 8.14.5

- 原始 home 中 `wrapper/dists/gradle-8.14.5-bin/690y85m0j9nfaub7xoiayko8a` 是**空目录**，8.14.5 分发包实际未缓存；
  `<user-home>/.gradle` 下只有一个 `gradle-8.14.5-bin.zip.part`；`services.gradle.org` 下载会卡在 0 字节。
- `<user-home>/.gradle\wrapper\dists\gradle-9.5.1-bin\...\gradle-9.5.1` 是完整解压版（历史构建 `build_loop.ps1`
  就是用 9.5.1 二进制构建成功 1.7.3 版本的）。
- 因此：`gradle/wrapper/gradle-wrapper.properties` 已改为
  `distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip`，
  并把解压目录 + `.ok` 标记预置到 `.gradle-home/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/`，
  `gradlew.bat --version` 可完全离线运行（Launcher JVM 21.0.11）。

### 1.3 网络事实（离线构建原因）

不可达：`maven.neoforged.net`、`plugins.gradle.org`、`maven.fabricmc.net`（以及 `services.gradle.org` 下载）。
可达（历史上已验证）：Maven Central、`maven.blamejared.com`、`maven.theillusivec4.top`、`maven.shedaniel.me`。
当前 P0 构建**必须加 `--offline`**，全部依赖由上述缓存提供。

---

## 2. libs/ 清单（共 9 个 jar，由 Lead 维护，**不要删除/重写**）

构建以 `compileOnly fileTree(dir: 'libs', include: ['*.jar'])` 引入，全部**不打包进 mod jar**。

| 文件 | modId | 版本 |
|---|---|---|
| `botania-neoforge-1.21.1-455.jar` | botania | `455-SNAPSHOT` |
| `kubejs-neoforge-2101.7.2-build.377.jar` | kubejs | `2101.7.2-build.377` |
| `rhino-2101.2.7-build.85.jar` | rhino | `2101.2.7-build.85` |
| `better-advanced-tooltips-2101.1.0-build.5.jar` | betteradvancedtooltips | `2101.1.0-build.5` |
| `jei-1.21.1-neoforge-19.64.0.455.jar` | jei | `19.64.0.455`（完整 jar） |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | patchouli | `1.21.1-93`（jar 内版本为 `${file.jarVersion}`，由 manifest 提供） |
| `curios-neoforge-9.5.1+1.21.1.jar` | curios | `9.5.1+1.21.1` |
| `architectury-13.0.11-neoforge.jar` | architectury | `13.0.11` |
| `emi-1.1.24+1.21.1+neoforge.jar` | emi | `1.1.24+1.21.1+neoforge` |

> 来源：用户/Lead 提供的本地依赖（`<workspace>/deps\` 及 Lead 直接补齐），
> 本次 P0 **没有联网下载**，也没有生成/运行任何 libs 同步脚本。libs 内容以 Lead 维护的 9 个 jar 为准。

---

## 3. 可复现构建命令（PowerShell）

```powershell
$env:JAVA_HOME     = '<JDK21>'
$env:GRADLE_USER_HOME = '<workspace>\.gradle-home'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location '<workspace>\kubejs-botania-neo'

# 只编译 Java（自检用）
.\gradlew.bat compileJava --offline --console=plain

# 产出 mod jar（会先跑 createMinecraftArtifacts 与编译）
.\gradlew.bat build --offline --console=plain
```

- 产物：`build/libs/kubejs_botania_neo-0.2.0.jar`（`base.archivesName = mod_id`，version = 0.1.0）。
- 首次构建会由 ModDevGradle 生成 NeoForge 编译产物到 `build/moddev/artifacts/`，并可能耗时数分钟。
- **同一时间只允许一个 Gradle 构建**：多个 agent/进程共享同一个 `.gradle-home` 时会互相抢缓存锁与 daemon，
  表现为卡在 `Starting a Gradle Daemon` 或耗时异常；排查时先确认没有其它 java/gradle 进程。

---

## 4. 已知阻塞问题：`createMinecraftArtifacts` 的 NFRT `recompile` 节点失败（已通过预置 artifacts 方案绕过，最终构建 BUILD SUCCESSFUL，见 6.5）

### 4.1 现象

```
> Task :createMinecraftArtifacts FAILED
*** Started working on recompile
 110 items on the compile classpath
 0 items on the sourcepath
*** Started working on sourcesWithNeoForge
 REUSE Used cache of sourcesWithNeoForge in 0.00s
 Compiling 5364 source files
 ERROR Line: 1, ..net.minecraft.world.ticks.package-info...: null in /net/minecraft/world/ticks/package-info.java
Total runtime: 38.76s

net.neoforged.neoform.runtime.graph.NodeExecutionException: Node action for recompile failed
	at net.neoforged.neoform.runtime.engine.NeoFormEngine.runNode(NeoFormEngine.java:613)
	...
Caused by: java.io.IOException: Compilation failed
	at net.neoforged.neoform.runtime.actions.RecompileSourcesActionWithJDK.run(RecompileSourcesActionWithJDK.java:77)
	... 4 more
BUILD FAILED
```
日志显示 `Process 'command '<JDK21>\bin\java.exe'' finished with non-zero exit value 1`。

### 4.2 已确认的事实

- **不是插件解析/网络问题**：NeoForge 21.1.248、ModDevGradle 2.0.142、parchment、NFRT 2.0.22 以及
  `mergetool` / `AutoRenamingTool` / `vineflower` / `jst-cli-bundle` / `installertools` 全部从缓存成功解析并执行
  （`downloadClient/downloadServer/mergeMappings/strip*/merge/rename/decompile/inject/patch/applyNeoforgePatches` 等节点均 DONE/REUSE）。
  唯一失败节点是 `recompile`。
- **javac 侧全部输出就只有这一条 ERROR diagnostic**（NFRT 的 DiagnosticListener 只打印 ERROR 级）：
  `ERROR Line: 1, ..net.minecraft.world.ticks.package-info...: null in /net/minecraft/world/ticks/package-info.java`
  —— message 为 `null`；被编译的 `package-info.java` 内容本身是合法且与原始缓存内的一致（已核对 zip 内容）。
- 用 javap 反编译 NFRT 2.0.22 确认：`RecompileSourcesActionWithJDK.java:77` = `compilationTask.call()` 返回 false 后
  `throw new IOException("Compilation failed")`；javac 参数固定为
  `--release 21 -proc:none -nowarn -g -XDuseUnsharedTable=true -implicit:none`。
  证据文件：`docs/nfrt-recompile-javap.txt`、`docs/nfrt-inner-javap.txt`。

### 4.3 根因分析（关键）

1. 原始 home 的 `caches/neoformruntime/intermediate_results` 里本来就有 9 月份 AnvilCraft-TerminalPlugins 成功构建留下的
   35 个中间产物（含 `recompile_2cd60345..._output.jar`，19.5 MB）。
2. **新工程第一次构建时，NFRT 自身的缓存清理逻辑把这些条目当"过期"删除了**：
   `15 expired keys found / Freed up 211 MiB by deleting 35 expired entries`。
   于是 `recompile` 没有缓存可复用，只能真跑 javac → 在 JDK 21.0.11 + NFRT 2.0.22 下触发上述 null-message 错误。
3. 进一步对比缓存条目 `.txt` 里的 cache key 组件发现：原始缓存的 **transformSources key = `0bb2a47b0335ac12…`**，
   而我们工程生成的 **transformSources key = `44ee389c2604013a…`**；两者组件**只差一项**：
   原始多一个 `"injected interfaces[0]"`（= AnvilCraft 的
   `anvilcraft-neoforge-1.21.1-1.6.0+snapshot.2270-interfaceinjection.json`，
   内容 sha1 `f6d67d68…`，缓存内有，1422 B）。
   → 原始 NFRT 缓存是用**带 AnvilCraft interface injection** 的配置构建的；本工程没有注册 interface injection，
   cache key 全不一致，所以必须真编译 5364 个源文件，从而暴露第 2 步的问题。
   （AnvilCraft-TerminalPlugins 的 `dependencies.gradle` 第 7 行：`interfaceInjectionData(libs.anvilcraft) { transitive = false }`。）

### 4.4 已尝试方案与结果

| # | 方案 | 结果 |
|---|---|---|
| 1 | 原始 home → 可写镜像 `.gradle-home`，`gradlew compileJava --offline` | 170.7 s，NFRT 全流程真跑，`recompile` 失败（同 4.1 错误） |
| 2 | 从参考工程 `build/moddev/artifacts/` 拷入 4 个已构建 NeoForge jar + 拷 manifest 后重跑 | 72.4 s，绝大多数节点 REUSE，但 `recompile` 仍无缓存 → 仍失败（同错误） |
| 3 | 从原始只读 home **重新完整拷回** `neoformruntime` 缓存（35 个 intermediates）+ 刷新 mtime 防过期清理 | 第 3 次运行在 `Starting a Gradle Daemon` 处长时间无输出（怀疑与并发的其它 Gradle 进程抢锁），被中止；Lead 随后实测仍失败（77 s，同错误） |
| 4 | 待验证：build.gradle 加 `interfaceInjectionData(...)` 对齐 cache key（见 4.5-A） | 未执行（Lead 接手构建；本次未改动构建文件） |
| 5 | 待验证：用参考工程已构建 artifacts + `-x createMinecraftArtifacts`（见 4.5-B） | 未执行 |
| 6 | Lead 最终方案：预置 artifacts + build.gradle 自动跳过 createMinecraftArtifacts + `tools/anvilcraft-interface-stubs.jar`（10 个空接口）compileOnly | **BUILD SUCCESSFUL**（`docs/lead-build-final.log`，jar 见 6.5） |

### 4.5 诊断期备选方案（最终采用"预置 artifacts + stub"方案，见 6.5；以下保留供参考）

**A. 对齐 NFRT cache key（推荐，可完全跳过失败的真编译）**

在 `build.gradle` 中：
```groovy
repositories {
    // ...
    maven { name = 'Cjsah'; url = 'https://server.cjsah.net:1002/maven/' } // AnvilCraft/Anvillib 来源
}
dependencies {
    // 让 NFRT 的 "injected interfaces" 输入与 9 月成功构建的缓存一致 → 命中 transformSources/recompile 缓存
    interfaceInjectionData('dev.dubhe:anvilcraft-neoforge-1.21.1:1.6.0+snapshot.2270') { transitive = false }
    // 注入接口挂在 Block/BlockState/Entity/ItemEntity 等核心类上，编译我们的源码时需能解析这些接口：
    compileOnly 'dev.anvilcraft.lib:anvillib-neoforge-1.21.1:2.0.0+snapshot.521'
    compileOnly 'dev.dubhe:anvilcraft-neoforge-1.21.1:1.6.0+snapshot.2270'
}
```
预期 `createMinecraftArtifacts` 日志出现 `REUSE Used cache of transformSources` 与 `REUSE Used cache of recompile`，
不在 javac 阶段失败。离线缓存中已有上述模块的 `.module/.pom/.jar` 与 interfaceinjection 分类器（`dev.dubhe`、
`dev.anvilcraft.lib` 两组，位于 `.gradle-home/caches/modules-2/files-2.1` 下），`--offline` 仍可解析。
若离线仓库元数据解析失败，可退化为 `compileOnly files('…cache…jar')` 直接指向缓存文件。

**B. 绕过任务（保底）**

把参考工程 `<workspace>/dev-tools\AnvilCraft-TerminalPlugins\build\moddev\artifacts\`
下的 4 个 jar（`neoforge-21.1.248.jar` / `-merged.jar` / `-sources.jar` / `-client-extra-aka-minecraft-resources.jar`，09/11 构建，仍在）
拷到本工程 `build/moddev/artifacts/`，然后：
```powershell
.\gradlew.bat build --offline --console=plain -x createMinecraftArtifacts
```
注意：ref 产物含 AnvilCraft 注入接口，同样需要 4.5-A 里的 anvilcraft/anvillib `compileOnly`；
且 `clean` 后需重新拷贝，交付命令不干净，仅作保底。

**C. 换 JDK（不推荐先试）**：本机另有 `jdk-17`、`jdk-26.0.2`。JDK 21.0.11 的 javac 与 NFRT 2.0.22 组合
疑似存在兼容问题，但 Gradle 9.5.1 对 JDK 26 支持未知，风险高。

---

## 5. Botania jar 放入 `libs/` 后如何重新编译

`build.gradle` 已写死为：

```groovy
compileOnly fileTree(dir: 'libs', include: ['*.jar'])
```

因此**无需改构建脚本**：

1. 把 Botania jar（命名建议 `botania*` 开头，如 `botania-neoforge-1.21.1-455.jar`）放进 `kubejs-botania-neo/libs/`；
   同时确认 KubeJS/Rhino/BAT/patchouli/curios/architectury 等依赖 jar 也都在 `libs/`。
2. 运行第 3 节的 `gradlew.bat build --offline --console=plain`。
3. 检查产物 `build/libs/kubejs_botania_neo-0.2.0.jar`：`compileOnly` 的 libs jar 不会被打进去
   （可用 `jar tf build\libs\kubejs_botania_neo-0.2.0.jar | Select-String 'vazkii|dev/latvian|me/zzinger'` 验证应为空）。

---

## 6. 配置文件核对（当前状态）

| 文件 | 状态 | 备注 |
|---|---|---|
| `settings.gradle` | OK | `pluginManagement` 仓库 mavenLocal/gradlePluginPortal/neoforged；`rootProject.name = kubejs-botania-neo` |
| `gradle.properties` | OK | `neo_version=21.1.248`、`parchment_mappings_version=2024.11.17`、Java toolchain 自动下载已禁用并指向本机 JDK21 |
| `build.gradle` | OK | java-library + `net.neoforged.moddev` 2.0.142；`compileOnly fileTree('libs')`；`compileOnly files('tools/anvilcraft-interface-stubs.jar')`（本机离线垫片，见 4.5/6.5）；`base.archivesName = mod_id`；`jar` 清单属性；**自动跳过 `createMinecraftArtifacts`**（当 `build/moddev/artifacts/neoforge-${neo_version}.jar` 存在时）；`processResources { inputs.property 'version' }` 目前是空操作（neoforge.mods.toml 的 version 写死 0.1.0），可删可保留 |
| `src/main/resources/META-INF/neoforge.mods.toml` | OK（已修） | `dependencies` 中 kubejs/botania/patchouli/curios 均为 required；**botania `versionRange` 已改为 `[455-SNAPSHOT,)`**（匹配 jar 内 455-SNAPSHOT）；license 为 `GPL-3.0-or-later`；`[[mixins]] config="kubejs_botania_neo.mixins.json"` 已配置 |
| `src/main/resources/pack.mcmeta` | OK | 位于 jar 根目录（NeoForge 正确位置），`pack_format = 34`（1.21.1）；不需要 META-INF 副本 |
| `src/main/resources/kubejs_botania_neo.mixins.json` | OK | `package=dev.kubejsbotania.neo.mixin`、`compatibilityLevel=JAVA_21`、`required=true`，已登记 `TerrestrialAgglomerationPlateBlockEntityMixin`（源文件 package 已纠正为 mixin） |
| mixin 编译依赖 | OK | NeoForge 传递依赖已提供：compile classpath 含 `net.fabricmc:sponge-mixin:0.15.2+mixin.0.8.7` 与 `mixinextras-neoforge:0.5.3`，**无需**额外声明 mixin 依赖；也不需要 mixin 注解处理器 |
| `tools/anvilcraft-interface-stubs.jar` | OK（本机环境专用） | 10 个空接口的编译期垫片，仅用于解析带 AnvilCraft 注入接口的预置 NeoForge artifacts；不打包、运行时无 AnvilCraft 依赖 |
| `src/main/resources/kubejs.plugins.txt` | OK | `dev.kubejsbotania.neo.kubejs.BotaniaKubeJSPlugin` |
| `src/main/resources/kubejs.classfilter.txt` | OK | `+vazkii.botania.api.**` / `+vazkii.botania.common.**` / `+dev.kubejsbotania.neo.**` |
| `.gitignore` | OK | 忽略 `.gradle/ build/ run/ out/`，保留 `gradle-wrapper.jar` |
| 最终构建验证 | **BUILD SUCCESSFUL** | `docs/lead-build-final.log`（31s）；产物 `build/libs/kubejs_botania_neo-0.2.0.jar`（58,222 B，52 entry，第三方包 0 命中） |

> 说明：本表由 build-env 核对后与最终状态同步；`build.gradle`/源码的最终修改由 Lead 实施（build-env 未改动 Lead 负责的源文件）。

---

## 6.5 最终采用的构建方案与验证结果（2026-10-29 12:31 定稿）

**结论：`BUILD SUCCESSFUL`，jar 已产出。**

方案（由 Lead 实施）：

1. 把参考工程 `AnvilCraft-TerminalPlugins/build/moddev/artifacts/` 下的 4 个 jar 恢复到
   `kubejs-botania-neo/build/moddev/artifacts/`：
   `neoforge-21.1.248.jar`、`neoforge-21.1.248-merged.jar`、`neoforge-21.1.248-sources.jar`、
   `neoforge-21.1.248-client-extra-aka-minecraft-resources.jar`。
2. `build.gradle` 保留自动跳过逻辑：当 `build/moddev/artifacts/neoforge-${neo_version}.jar` 存在时
   `createMinecraftArtifacts.enabled = false`（在能正常重新生成 artifacts 的机器上任务照常执行）。
3. 这批预置 artifacts 来自注册过 AnvilCraft interface injection 的参考工程（Minecraft 核心类上带
   AnvilCraft 注入接口），javac 解析 `Block`/`BlockState`/`Entity` 等类时需要这些接口类型存在。
   为此生成 `tools/anvilcraft-interface-stubs.jar`（10 个空接口，覆盖注入接口），仅以
   `compileOnly files('tools/anvilcraft-interface-stubs.jar')` 参与编译；**不打包进 mod jar**。
4. 已由 Lead 验证编译产物**无任何 `dev/dubhe`、`anvilcraft` 引用**（30 个 class 扫描 0 命中），
   因此运行时**不依赖 AnvilCraft**（stub 只是本机离线编译期的垫片，正常环境重新生成干净 artifacts 后可删除）。

最终构建命令（离线，PowerShell）：

```powershell
$env:JAVA_HOME        = '<JDK21>'
$env:GRADLE_USER_HOME = '<workspace>\.gradle-home'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location '<workspace>\kubejs-botania-neo'
.\gradlew.bat build --offline --console=plain --no-daemon
```

- 结果：`BUILD SUCCESSFUL in 31s`（完整日志 `docs/lead-build-final.log`；上一轮 `docs/lead-build3.log` 为 38s 成功）。
- 关键任务行：`> Task :createMinecraftArtifacts SKIPPED` → `:compileJava` → `:processResources` → `:jar` → `:build`。
- 产物：`build/libs/kubejs_botania_neo-0.2.0.jar`（58,222 B；52 个 entry；含 `META-INF/neoforge.mods.toml`、
  `pack.mcmeta`、`kubejs_botania_neo.mixins.json`、`kubejs.plugins.txt`、`kubejs.classfilter.txt`、
  `assets/kubejs_botania_neo/lang/{en_us,zh_cn}.json` 与 30 个 class；第三方包 0 命中）。

**已知限制 / 注意**：

1. 该方案依赖 `build/moddev/artifacts/` 下 4 个预置 NeoForge jar（约 74 MB，不在版本控制内）。
   `gradlew clean` 会删除 `build/` → 需从参考工程重新拷贝这 4 个 jar 才能再次构建；
   **不要 clean 后直接 build**（会重新触发 NFRT recompile → 失败）。
2. `tools/anvilcraft-interface-stubs.jar` 是本机离线环境的编译期垫片；在能联网重新生成干净 artifacts 的机器上，
   可删除 `build.gradle` 中对应的 `compileOnly files(...)` 与自动跳过逻辑。
3. NFRT `recompile` 在 JDK 21.0.11 + NFRT 2.0.22 下的根因（`message=null` 诊断）未彻底修复，只是绕过；
   换 JDK 26 的尝试也失败：错误进程仍是 jdk-21.0.11 的 java（NFRT 由 ModDevGradle 用 Java toolchain 启动），
   见 `docs/lead-nfrt-jdk26.log`。
4. Gradle 9.5.1 wrapper 与 `.gradle-home` 镜像均为本机离线环境特有；换机需按第 1 节重建。

最终构建中已生效的配置修复：

- `neoforge.mods.toml`：`license` 改为 `GPL-3.0-or-later`；botania 依赖 `versionRange` 改为 `[455-SNAPSHOT,)`（与实际 jar 版本匹配）。
- mixin 源文件 `package` 声明纠正为 `dev.kubejsbotania.neo.mixin`（此前误写为 `recipe`）。
- 构建日志中唯一的警告为 JEI `getBackground()` 已过时（`[removal]`），不影响产物。

## 7. 证据文件与过程记录

- `docs/lead-build-final.log`：**最终成功构建日志**（`BUILD SUCCESSFUL in 31s`，12:31）。
- `docs/lead-build3.log`：上一轮成功构建日志（38s）。
- `docs/lead-build.log` / `docs/lead-build2.log`：诊断过程日志（预置 artifacts 前后、compileJava 2 个 anvilcraft 接口错误的现场）。
- `docs/lead-nfrt-jdk26.log`：换 JDK 26 尝试失败的日志（NFRT 子进程仍是 jdk-21.0.11）。
- `docs/nfrt-recompile-javap.txt`：NFRT 2.0.22 `RecompileSourcesActionWithJDK` 反编译（含 line 77 失败点、javac 固定参数、cache key 组成）。
- `docs/nfrt-inner-javap.txt`：NFRT 诊断监听器反编译（证明只打印 ERROR 级、`getMessage(null)` 为 null）。
- `docs/gradle-home-copy.log`：`.gradle-home` 拷贝日志（robocopy `/MT`）。
- `docs/kubejs-2101-api-notes.md`：另一位队友整理的 KubeJS 2101 API 笔记（非本文件维护）。

## 8. 历史参考（同环境曾经的可用构建方式）

`<workspace>/dev-tools\开发脚本\build_loop.ps1` 记录了 9 月/10 月构建
AnvilCraft-TerminalPlugins 1.7.3 时的方式：
`JAVA_HOME=jdk-21.0.11`、`GRADLE_USER_HOME=<workspace>\.gradle-home`、
Gradle 二进制 `<user-home>/.gradle\wrapper\dists\gradle-9.5.1-bin\...\gradle-9.5.1\bin\gradle.bat`、
参数含 `--max-workers=1` 与 HTTP 代理指向 `127.0.0.1:8899`（代理当前未运行，本 P0 全离线，不需要）。
本工程的 `.gradle-home` 与 wrapper 9.5.1 即按此历史方式复刻。
