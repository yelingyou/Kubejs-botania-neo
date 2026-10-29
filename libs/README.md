# libs/ 说明（不随仓库提交第三方 jar）

本模组编译需要以下第三方 jar。由于它们版权归各自作者所有，**不放入本仓库**，请自行从对应整合包或官方发布页获取后放入本目录：

| 文件名（开发时所用版本） | 用途 |
|---|---|
| otania-neoforge-1.21.1-455.jar | Botania 1.21.1 NeoForge（必需） |
| kubejs-neoforge-2101.7.2-build.377.jar | KubeJS 2101（必需） |
| hino-2101.2.7-build.85.jar | KubeJS 的 Rhino 运行时（编译期需要） |
| etter-advanced-tooltips-2101.1.0-build.5.jar | KubeJS 依赖（编译期需要） |
| jei-1.21.1-neoforge-19.64.0.455.jar | JEI，可选（客户端配方页） |
| emi-1.1.24+1.21.1+neoforge.jar | EMI，可选（客户端原生配方页） |
| Patchouli-1.21.1-93-NEOFORGE.jar | Botania 前置 |
| curios-neoforge-9.5.1+1.21.1.jar | Botania 前置 |
| rchitectury-13.0.11-neoforge.jar | Botania/前置编译依赖 |

uild.gradle 通过 compileOnly fileTree('libs') 读取这些 jar；联网环境也可以改为 Maven 依赖（Botania 455 等并非都发布在公共 Maven，离线时用本地 jar 最方便）。

离线构建还需要 NeoForm 产物 uild/moddev/artifacts/*.jar 与 	ools/anvilcraft-interface-stubs.jar，详见 docs/build-notes.md。