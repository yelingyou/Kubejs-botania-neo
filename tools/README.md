# tools/

本目录用于**离线开发环境**的编译兼容：

- injection-stubs/src/：10 个空的 AnvilCraft 接口 stub（dev.dubhe.anvilcraft.api.injection.*）。本机预置的 NeoForm 产物被 AnvilCraft 的 interface injection 修改过，直接编译会引用到这些接口；正常联网环境不需要它们。
- 生成好的 nvilcraft-interface-stubs.jar 与 injection-stubs/classes/ **不随仓库提交**（见 .gitignore）。离线使用时可按 docs/build-notes.md 重新生成 stub jar。

在干净环境中构建时，可删除 uild.gradle 中 compileOnly files('tools/anvilcraft-interface-stubs.jar') 一行。