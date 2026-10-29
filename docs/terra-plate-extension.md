# 泰拉凝聚板扩展（Terra Plate Extension）实现说明

本文档说明 `kubejs_botania_neo:terra_plate` 配方类型的设计、匹配算法、Mixin 注入方式与测试清单。

## 1. 目标

在 Botania 1.21.1 的泰拉凝聚板基础上，允许 KubeJS 脚本：

- 自定义板正下方一层的方块/流体图案（不再固定 Botania 默认的 Livingrock/Shimmerrock + 青金石块平台）；
- 图案自动 4 向旋转匹配；
- 合成成功后对下方指定格子做方块/流体转化；
- 自定义魔力消耗；
- 保留 Botania 原版配方的所有行为（火花/魔力接收、进度条、音效、粒子、DONE/IDLE 状态机）。

## 2. 关键设计

### 2.1 复用 Botania 的配方类型（而不是另起一套）

`TerraPlateExtensionRecipe` 直接实现 Botania 的 `vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe` 接口，**不覆盖** `getType()`：

- 该接口的 `getType()` 默认返回 `botania:terrestrial_agglomeration_plate`；
- Minecraft 1.21.1 的 `RecipeManager` 在构建 `byType` 索引时使用的是 `recipe.getType()`（不是 JSON `type`/serializer），因此扩展配方会被自动索引进 Botania 的 `BotaniaRecipeTypes.TERRA_PLATE_TYPE`；
- 由此获得：
  - Botania 的 `RecipeManager.CachedCheck` 能直接查到扩展配方（不需要改 `serverCollectingTick`/`tryStartProcessing`）；
  - Botania 的 `BotaniaRecipeIngredientsCache` 会把扩展配方的材料自动加入“可放上凝聚板”的物品缓存（不需要 mixin 该缓存）；
  - Botania 自带的 JEI/EMI/REI 分类也能显示扩展配方（仅输入/魔力/产物）。

JSON 的 `"type"` 仍然是本模组注册的 `kubejs_botania_neo:terra_plate`（`ModRecipes` 注册的 `RecipeSerializer`），Botania 的 codec 不会被用到；两条路径互不冲突：JSON type → 我们的 serializer；索引 type → Botania 的 recipe type。

### 2.2 平台校验（`hasValidPlatform`）与线程内上下文

Botania 的调用顺序（455 字节码核对）：

1. `TerrestrialAgglomerationPlateBlock.tick` → `BlockEntity.tryStartProcessing()`
2. `tryStartProcessing()`：`hasValidPlatform()` → 取物品实体 → `cachedCheck.getRecipeFor(...)` → 命中则进入 `COLLECTING` 并记录 `manaToGet`
3. `serverCollectingTick()`：`hasValidPlatform()` → 取物品实体 → `getRecipeFor` → 收魔力 → 进度/完成

`hasValidPlatform()` 在“取物品”之前执行，且是 private 实例方法，因此：在它的 `HEAD` 注入里同时做“图案匹配 + 物品匹配”，命中时：

- 把 `(recipe, rotation, platePos)` 写入 `TerraPlateExtensionEngine` 的 `ThreadLocal` 上下文；
- `cir.setReturnValue(true)` 让 Botania 认为平台合法；
- 随后的 `getRecipeFor` 会调用 `recipe.matches(input, level)`，`matches()` 只在 `ThreadLocal` 中正是本配方时才接受（没有 pattern 的配方不受此限制），从而避免扩展配方在原版平台上顶替原版配方。

未命中时清除上下文、走 Botania 原逻辑（默认平台）。

> 说明：`findMatch` 会跳过“没有 `below` 图案”的扩展配方。这类配方不接管平台校验，而是让 Botania 原版 `hasValidPlatform()` 正常判定默认平台；随后原版物品查找仍会命中它们（它们的 `matches()` 不做上下文限制），从而在标准平台上正常合成。

`RecipeManager.CachedCheck` 没有“输入相同就跳过 matches”的缓存，因此每 tick 读取 `ThreadLocal` 是安全的（已核对 455 字节码）。

### 2.3 合成后转化（`finishCrafting` TAIL）

`finishCrafting(...)` 在以下动作全部完成后执行：

- 产物已生成、输入物品已消失、`mana=0`、`manaToGet=0`；
- 已广播 `CRAFTING_EFFECT` 事件、方块状态设为 `DONE`、并 `scheduleTick(4)`。

在 TAIL 注入里：若 `recipeHolder.value()` 是本模组的扩展配方：

- 有自定义图案：仅当它正是本 tick 平台匹配的配方（与上下文中的 recipe 实例相同）时，按命中的 rotation 应用 `result_blocks`，避免把转化误应用到原版配方；
- 无自定义图案（走默认平台）：按 rotation 0 应用 `result_blocks`。

然后清除上下文。

### 2.4 图案匹配算法

`TerraPlateExtensionEngine`：

```text
rotation r ∈ {0,1,2,3}：(x,z) → (x,z) → 旋转 90° 依次为 (-z,x) → (-x,-z) → (z,-x)
目标坐标 = platePos.offset(rotatedX, -1, rotatedZ)
- block 以 '#' 开头 → 方块标签匹配（BlockState#is(TagKey)）
- 其他 block → 方块匹配
- fluid → level.getFluidState(pos).getType() == 指定流体
同一 rotation 下所有 positions 全部匹配才算图案命中
取第一个命中的 rotation；result_blocks 用同一 rotation 变换
```

## 3. 配方 JSON 格式（由 KubeJS 生成）

```json
{
  "type": "kubejs_botania_neo:terra_plate",
  "ingredients": [ { "item": "minecraft:diamond" }, { "item": "minecraft:diamond" }, { "item": "minecraft:emerald" } ],
  "mana": 50000,
  "output": { "id": "minecraft:netherite_ingot", "count": 1 },
  "below": [ { "x": -1, "z": -1, "block": "minecraft:stone" } ],
  "result_blocks": [ { "x": 0, "z": 0, "block": "minecraft:gold_block" } ]
}
```

- `ingredients` / `output` 必填；`mana` 默认 500000；`below` / `result_blocks` 可省略；
- `below` 同时接受 `{"positions":[...]}` 对象写法（兼容 1.20.1 页面写法）与直接数组写法；
- `result_blocks` 中的 `block` 不支持 `#标签`（会被忽略），`fluid` 以源方块形式放置。

## 4. 测试清单（游戏内）

1. 安装 Botania + KubeJS + 本模组 + JEI，进入世界 `/reload`；
2. 把 `examples/kubejs/server_scripts/terra_plate_extension.js` 复制到 `kubejs/server_scripts/`，`/reload`；
3. 检查日志无 KubeJS 配方错误；`/kubejs recipes`（如可用）或 JEI 中应出现“泰拉凝聚板（扩展）”分类；
4. 按示例 1 摆放 3×3 图案，投入钻石×2+绿宝石，用魔力散发器/火花提供魔力 → 应产出下界合金锭，且下方 9 格按 `result_blocks` 转化；
5. 旋转整个图案 90°/180°/270° 重复，应同样成功；
6. 示例 2：验证流体格子（水）与方块标签格子匹配；
7. 示例 3：不写 `below` 时用默认平台（Livingrock/Shimmerrock + 青金石块）成功；
8. 示例 4：单方块图案；
9. 反例：图案不匹配时应不消耗物品、不合成；
10. 原版 Botania 泰拉钢配方仍正常（默认平台）。

## 5. 已知问题与后续可改进

- 扩展配方与 Botania 自带配方材料完全相同时，原版配方可能优先命中（按 recipe id 排序）；文档建议避免材料完全重复；
- 只支持 4 向旋转，不支持镜像；
- `result_blocks` 直接覆盖方块，不检查目标位置原有方块；
- 无 `below` 的扩展配方使用 Botania 默认平台，其 `result_blocks` 按 rotation 0 应用；
- 后续可选：EMI/REI 展示、`below` 支持多层、图案镜像匹配、在 JEI 中隐藏 Botania 自带分类里的扩展配方条目。
