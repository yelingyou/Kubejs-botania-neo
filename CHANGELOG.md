## 0.4.0

### 新增：原生 EMI 支持
- 新增原生 EMI 配方分类 kubejs_botania_neo:terra_plate（不再依赖 EMI 的 JEMI 桥接导入 JEI 页面）：
  - 输入/产物、下方图案（elow）、合成后变化（esult_blocks）均以原生 EMI Widget 展示；
  - 魔力条使用 **Botania 自己的 EMI ManaWidget**（内部调用 HUDHandler.renderManaBar），与 Botania 官方 EMI 页一致；
  - **泰拉凝聚板（Terrestrial Agglomeration Plate）** 注册为该分类的 **workstation**，在 EMI 中对着凝聚板按配方查看键即可看到扩展配方；每个配方页内也有凝聚板工作方块槽。
- 扩展配方仍会从 Botania 自己的 EMI「Terrestrial Agglomeration」分类中移除，只在本模组分类显示。
- 同时安装 EMI 时，JEI 端不再注册本模组分类（避免 EMI 通过 JEMI 显示重复页面）；仅安装 JEI 时行为与 0.3.1 相同。

### 说明
- EMI 为可选依赖（仅客户端，mods.toml 声明 optional）；未安装 EMI 时该类不会被加载。
- 0.3.1 的 JEI 防御性修复保留：Botania 的 JEI 分类不存在时只告警、不崩溃。
## 0.3.1

### 修复
- 修复 0.3.0 的崩溃：当 Botania 自己的 JEI 插件未加载（JEI 中没有 `botania:terra_plate` 分类）时，hideRecipes 会抛 IllegalStateException: There is no recipe category registered for ...；旧代码在客户端 tick 监听器里反复重试导致崩溃。现在：
  - 第一次尝试后即记录该 RecipeManager 已处理（失败也不会每 tick 重试）；
  - 捕获 IllegalStateException 并输出 WARN 日志后跳过（那种情况下 JEI 本来也没有 Botania 页面可隐藏）。
- 说明：用户整合包实际以 **EMI** 作为配方查看器（logs 显示 Botania 的 JEI 插件未注册、EMI 的 Botania 插件正常）；kubejs_botania_neo-0.3.0 的崩溃由此触发。

### 新增
- **EMI 集成**（可选，仅安装 EMI 时加载）：通过 @EmiEntrypoint 插件把扩展配方从 Botania 的 EMI「Terrestrial Agglomeration」分类中移除（按 `botania:terrestrial_agglomeration` 分类 id + 配方 id 命名空间 kubejs_botania_neo 过滤），不影响本模组自己的 JEI 分类（以及 EMI 通过 JEMI 引入的该分类）。
- EMI 页面的 Botania 魔力条由 Botania 自己的 EMI 插件/ManaWidget 渲染，无需额外处理。

## 0.3.0

### 新增 / 变更（JEI）
- 泰拉凝聚板扩展 JEI 页改用 **Botania 自己的魔力条**：直接调用 Botania 公开的 HUDHandler.renderManaBar(...)，纹理、颜色、透明度与填充比例（mana / 100000）与 Botania 自带泰拉凝聚板 JEI 页完全一致。
- 扩展配方**不再出现在 Botania 自带的「泰拉凝聚板」JEI 页**：通过 JEI 官方 IRecipeManager#hideRecipes 按 Botania 的 JEI RecipeType 隐藏（JEI 的隐藏集合按 RecipeType 分开存储，因此本模组自己的「泰拉凝聚板（扩展）」页仍正常显示）。
- JEI 日志新增隐藏结果：JEI: hid N extended terra plate recipes from Botania's agglomeration page。

### 说明
- 仅影响客户端 JEI 展示，不影响游戏内配方与合成逻辑；未安装 JEI 时无影响。
- 如果通过其它配方查看器（EMI/REI）查看，仍可能在 Botania 分类中看到扩展配方（本版只处理 JEI）。
## 0.2.1

### 修复
- 修复位置写法（positional）下 StateIngredient 参数无法接受脚本字符串的问题：schema 重新使用带 JS 包装的 StateIngredientComponent（支持 'minecraft:stone'、'#方块标签'、带属性的方块状态字符串与 JSON 对象）。
- 修复对象写法（object）下 'minecraft:xxx' 等脚本字符串被原样写进配方 JSON、导致 Botania/本模组 serializer 解析失败的问题：KubeJS 2101 的对象写法默认只解码校验、不重新编码；物品/配料/StateIngredient 组件现在会在读取对象后标记重写，写出规范 JSON。
- 以上覆盖全部 10 种 Botania 配方 schema 与泰拉凝聚板扩展配方，包含列表字段（如 elven_trade 输出列表、符文祭坛材料/触媒列表）与 terra_plate 的 ingredients 列表。

### 说明
- 0.2.0 用户日志确认插件加载正常，但上述两种写法分别出现 Cannot convert ... to StateIngredient 与 Not a JSON object: "minecraft:xxx" 报错；本版修复后位置写法与对象写法应全部通过。
# Changelog

## 0.2.0

首个对外发布的正式版本，包含以下内容。

### 新增
- **独立 JEI 分类「泰拉凝聚板（扩展）」**：显示输入/产物/魔力，以及 `below` 下方 3×3 图案与 `result_blocks` 合成后 3×3 转化示意（支持方块、`#标签`、流体）。
- 泰拉凝聚板扩展配方（`kubejs_botania_neo:terra_plate`）：
  - 自定义下方方块/流体图案，支持 4 向旋转自动匹配；
  - 合成后逐格方块/流体转化；
  - 自定义魔力消耗；
  - 不写 `below` 时回退 Botania 默认 3×3 平台；
  - 复用 Botania 真实配方类型，材料自动被识别为可放上凝聚板。
- `ManaHandler` / `CorporeaHelper` 脚本绑定。
- 全部 10 种 Botania 1.21.1 配方类型的 KubeJS schema。

### 修复
- 修复 `neoforge.mods.toml` 意外带 UTF-8 BOM 导致模组加载失败的问题（0.1.1）。
- 修复扩展配方对象写法无法解析脚本字符串 `'minecraft:xxx' / '#标签'` 的问题：新增脚本友好的 ItemStack / Ingredient / StateIngredient codec，并对全部 Botania schema 生效（0.1.2）。
- 修复 `petal_apothecary` 示例把物品标签 `botania:seed_apothecary_reagent` 当作物品 id 的问题（应为 `#botania:seed_apothecary_reagent`）。
- 修复扩展配方 `findMatch` 对无 `below` 配方也接管平台校验、导致可绕过原版平台的问题；无 `below` 的配方现在正常走 Botania 默认平台校验（0.1.2）。
- 修复 `finishCrafting` TAIL 对无 `below` 配方不应用 `result_blocks` 的问题（0.1.2）。
- 修复 JEI 分类槽位间距重叠（16px→18px）并调整布局（0.2.0）。

### 变更
- 版本号改由 `gradle.properties` 的 `mod_version` 统一控制，并自动注入 `neoforge.mods.toml` 与 jar 文件名，避免版本不一致。
- 新增 JEI 诊断日志：`[kubejs_botania_neo] JEI: N terra plate recipes total, M extended recipes`。

## 0.1.0

- 首个开发构建（存在 BOM 与运行时配方解析问题，不要使用）。
