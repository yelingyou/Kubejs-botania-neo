# Botania 1.21.1 配方 / 泰拉凝聚板内部机制规格（kubejs_botania_neo 权威参考）

> 本文件是 `kubejs_botania_neo`（MC 1.21.1 / NeoForge 21.1.248，modid = `kubejs_botania_neo`）
> 移植 Botania 配方 schema 与实现“泰拉凝聚板扩展”的唯一权威规格。
> 所有类名、字段名、方法描述符、默认值、JSON 示例均以 **用户实际运行的
> `deps/botania-neoforge-1.21.1-455.jar`（455-SNAPSHOT）** 为准，
> 并用只读源码 `refs/botania-1.21.1/Botania-1.21.1-porting`（build 457）交叉核对。
> 两者有差异时以 455 jar 为准，差异清单见 §0.3。

---

## 0. 文档信息与验证方法

### 0.1 权威数据源

| 数据源 | 路径 | 用途 |
|---|---|---|
| **运行时 jar（权威）** | `deps/botania-neoforge-1.21.1-455.jar` | 类/方法/字段/描述符；`data/botania/recipe/**` 真实 JSON（857 个） |
| 源码（参考） | `refs/botania-1.21.1/Botania-1.21.1-porting/Xplat/...` | codec 语义、调用链、注释（build 457，可能领先于 455） |
| 依赖 jar | `deps/kubejs-neoforge-2101.7.2-build.377.jar`、`deps/Patchouli`（工程 `libs/`） | KubeJS/Patchouli 行为 |
| MC / NeoForge | `01-我的世界模组开发/AnvilCraft-TerminalPlugins/build/moddev/artifacts/neoforge-21.1.248-merged.jar` | 1.21.1 mojmap 的 `RecipeManager` / `BlockPos` 字节码 |

### 0.2 可复现的验证命令

```powershell
$jar = "<workspace>/deps\botania-neoforge-1.21.1-455.jar"
$javap = "<JDK21>\bin\javap.exe"

# 类成员
& $javap -p -classpath $jar vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity
# 精确描述符
& $javap -s -p -classpath $jar vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity
# 字节码（确认调用顺序 / 目标描述符）
& $javap -c -p -classpath $jar vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity
# 解包真实配方 JSON
cd $env:TEMP; & "<JDK21>\bin\jar.exe" xf $jar "data/botania/recipe"
```

1.21.1 datapack 目录名是 **`recipe`（单数）**，不是 1.20.1 的 `recipes`（本 jar 内即 `data/botania/recipe/`）。

### 0.3 455 jar 与 457 源码的已确认差异（写代码/写文档必须知道）

| 项 | 455 jar（实际运行） | 457 源码 | 影响 |
|---|---|---|---|
| spark API 类名 | `vazkii.botania.api.mana.spark.SparkAttachable`、`SparkHelper` | `ManaSparkAttachable`、`ManaSparkHelper` | 引用 spark API 时必须用 455 名。泰拉板 BE 在 455 实现的是 `SparkAttachable` |
| `XplatAbstractions` 入口 | 既有 `public static final XplatAbstractions INSTANCE`，又有 `static XplatAbstractions instance()` | 只有 `instance()` | 自定义配方里写 `XplatAbstractions.INSTANCE.requiresCustomTesting(...)`（455 可用） |
| 配方 JSON 数量/内容 | 857 个 | 878 个（61 个仅源码有，12 个仅 jar 有） | 有 34 个同名文件内容不同（marimorphosis 的 bonus tag 名、pure_daisy 的 success_function、部分 crafting 修改）。**所有类型字段名/结构完全一致**（逐文件对比 455 vs 457 的每个 type 的键集合：无差异）。示例以 455 为准 |

> 结论：**schema 字段以本文为准不需要因版本改动**；只有引用 Botania 类名时要按 455 jar（见 §9 的 API 清单）。

---

## 1. 配方系统总览

### 1.1 注册机制（1.21.1）

- Botania 的 `RecipeType` 注册在 **`BuiltInRegistries.RECIPE_TYPE`**（NeoForge `RegisterEvent`，见
  `NeoForge/src/main/java/vazkii/botania/neoforge/NeoForgeCommonInitializer.java:209`）。
- 所有 `RecipeSerializer` 注册在 **`BuiltInRegistries.RECIPE_SERIALIZER`**（同文件 :210）。
- 配方 JSON 通过 `Recipe.CODEC` 按 JSON 的 **`"type"` 字段 dispatch 到 serializer**；
  之后 `RecipeManager.replaceRecipes` 里按 **`recipe.getType()` 建 `byType` 索引**（已用 1.21.1 字节码确认）。
  → JSON 的 `"type"`（serializer id）与 `recipe.getType()`（RecipeType）**是两个独立概念**，
  这对 `runic_altar_head` / `elven_trade_lexicon` 以及我们的泰拉板扩展都至关重要。

### 1.2 全部配方类型 / serializer 总表（type id → serializer 类）

**Botania 自有 RecipeType（10 个）**

| # | JSON `type` | RecipeType（`BotaniaRecipeTypes` 字段） | 对应 recipe class | serializer 类（全限定名） |
|---|---|---|---|---|
| 1 | `botania:mana_infusion` | `MANA_INFUSION_TYPE` | `api.recipe.ManaInfusionRecipe` | `vazkii.botania.common.crafting.ManaInfusionRecipe$Serializer` |
| 2 | `botania:pure_daisy` | `PURE_DAISY_TYPE` | `api.recipe.PureDaisyRecipe` | `vazkii.botania.common.crafting.PureDaisyRecipe$Serializer` |
| 3 | `botania:elven_trade` | `ELVEN_TRADE_TYPE` | `api.recipe.ElvenTradeRecipe` | `vazkii.botania.common.crafting.ElvenTradeRecipe$Serializer` |
| 4 | `botania:elven_trade_lexicon` | `ELVEN_TRADE_TYPE`（复用） | `common.crafting.LexiconElvenTradeRecipe` | `vazkii.botania.common.crafting.LexiconElvenTradeRecipe$Serializer` |
| 5 | `botania:runic_altar` | `RUNIC_ALTAR_TYPE` | `api.recipe.RunicAltarRecipe` | `vazkii.botania.common.crafting.RunicAltarRecipe$Serializer` |
| 6 | `botania:runic_altar_head` | `RUNIC_ALTAR_TYPE`（复用） | `common.crafting.recipe.HeadRecipe` | `vazkii.botania.common.crafting.recipe.HeadRecipe$Serializer` |
| 7 | `botania:brew` | `BREW_TYPE` | `api.recipe.BotanicalBreweryRecipe` | `vazkii.botania.common.crafting.BotanicalBreweryRecipe$Serializer` |
| 8 | `botania:petal_apothecary` | `PETAL_APOTHECARY_TYPE` | `api.recipe.PetalApothecaryRecipe` | `vazkii.botania.common.crafting.PetalApothecaryRecipe$Serializer` |
| 9 | `botania:terrestrial_agglomeration_plate` | `TERRA_PLATE_TYPE` | `api.recipe.TerrestrialAgglomerationRecipe` | `vazkii.botania.common.crafting.TerrestrialAgglomerationRecipe$Serializer` |
| 10 | `botania:orechid` | `ORECHID_TYPE` | `api.recipe.OrechidRecipe` | `vazkii.botania.common.crafting.OrechidRecipe$Serializer` |
| 11 | `botania:orechid_ignem` | `ORECHID_IGNEM_TYPE` | `api.recipe.OrechidRecipe` | `vazkii.botania.common.crafting.OrechidIgnemRecipe$Serializer` |
| 12 | `botania:marimorphosis` | `MARIMORPHOSIS_TYPE` | `api.recipe.OrechidRecipe` | `vazkii.botania.common.crafting.MarimorphosisRecipe$Serializer` |

> 注意：`botania:terra_plate` **不是** 1.21.1 的 type id（1.20.1 的旧名，1.21.1 改名
> `botania:terrestrial_agglomeration_plate`）。KubeJS 侧可以注册 `terra_plate` 作为脚本别名，
> 但写进 JSON 的 `"type"` 必须是真实 id。

**Botania 的 crafting 变体 serializer（RecipeType 均为 `minecraft:crafting`）**

| JSON `type` | serializer 类 |
|---|---|
| `botania:crafting_special_ancient_will_attach` | `AncientWillRecipe.SERIALIZER` |
| `botania:crafting_shaped_armor_upgrade` | `ArmorUpgradeRecipe.SERIALIZER` |
| `botania:crafting_special_black_hole_talisman_extract` | `BlackHoleTalismanExtractRecipe.SERIALIZER` |
| `botania:crafting_special_composite_lens` | `CompositeLensRecipe.SERIALIZER` |
| `botania:crafting_special_cosmetic_attach` | `CosmeticAttachRecipe.SERIALIZER` |
| `botania:crafting_special_cosmetic_remove` | `CosmeticRemoveRecipe.SERIALIZER` |
| `botania:crafting_special_shard_of_laputa_upgrade` | `LaputaShardUpgradeRecipe.SERIALIZER` |
| `botania:crafting_special_lens_dye` | `LensDyeingRecipe.SERIALIZER` |
| `botania:crafting_shapeless_lexicon_return` | `LexiconReturningShapelessRecipe.SERIALIZER` |
| `botania:crafting_special_mana_blaster_add_clip` | `ManaBlasterClipRecipe.SERIALIZER` |
| `botania:crafting_special_mana_blaster_add_lens` | `ManaBlasterLensRecipe.SERIALIZER` |
| `botania:crafting_special_mana_blaster_remove_lens` | `ManaBlasterRemoveLensRecipe.SERIALIZER` |
| `botania:crafting_shaped_mana_upgrade` | `ManaUpgradeRecipe.SERIALIZER` |
| `botania:crafting_special_merge_vial` | `MergeVialRecipe.SERIALIZER` |
| `botania:crafting_special_phantom_ink_apply` | `PhantomInkRecipe.SERIALIZER` |
| `botania:crafting_special_resolute_ivy` | `ResoluteIvyRecipe.SERIALIZER` |
| `botania:crafting_shapeless_mana_upgrade` | `ShapelessManaUpgradeRecipe.SERIALIZER` |
| `botania:crafting_shapeless_uncover_spreader` | `ShapelessUncoverSpreaderRecipe.SERIALIZER` |
| `botania:crafting_special_spell_cloth_apply` | `SpellbindingClothRecipe.SERIALIZER` |
| `botania:crafting_special_split_lens` | `SplitLensRecipe.SERIALIZER` |
| `botania:crafting_special_terra_shatterer_tipping` | `TerraShattererTippingRecipe.SERIALIZER` |
| `botania:crafting_special_tiara_wings` | `TiaraWingsRecipe.SERIALIZER` |
| `botania:crafting_shaped_wand` | `WandOfTheForestRecipe.SERIALIZER` |
| `botania:crafting_shaped_water_bottle_matching` | `WaterBottleMatchingRecipe.SERIALIZER` |

（映射来源：`BotaniaRecipeTypes.submitRecipeSerializers`，源码 `Xplat/src/main/java/vazkii/botania/common/crafting/BotaniaRecipeTypes.java:69-108`；
这些 id 在 455 jar 的 `data/botania/recipe/dynamic/*.json`、`fluegel_tiara_*.json` 等文件中均实际出现。）

### 1.3 通用 JSON 语法（写 schema 时复用）

#### Ingredient（物品/物品标签/多选一）
`Ingredient.CODEC_NONEMPTY`（1.21.1 格式）：

```json
"minecraft:iron_ingot"                      // ① 纯字符串 item id（等价于 {"item": ...}）
{ "item": "minecraft:iron_ingot" }          // ② 单个物品
{ "tag": "c:ingots/iron" }                  // ③ 物品标签（注意：物品标签**不带** '#' 前缀）
[ { "item": "minecraft:name_tag" },         // ④ 多选一（数组 = OR），如 runic_altar_head 的命名牌/成书
  { "item": "minecraft:written_book" } ]
```
`{"count": n}` 在 Ingredient 里不支持（Ingredient 只匹配物品类型；数量由配方匹配逻辑处理）。
NeoForge 自定义 `ICustomIngredient`（`requiresCustomTesting()` 为 true）**不适用于**泰拉板输入物品缓存（见 §7.5），
Botania 对 elven_trade / runic_altar 甚至会直接抛异常拒绝（`ElvenTradeRecipe.validateIngredients`）。

#### ItemStack / 输出（`ItemStack.STRICT_CODEC` 或 `ItemStack.STRICT_SINGLE_ITEM_CODEC`）
```json
{ "id": "botania:manasteel_ingot" }                                  // count 省略 = 1
{ "count": 2, "id": "botania:rune_of_air" }
{ "id": "minecraft:player_head",
  "components": { "minecraft:profile": { "name": "Vazkii" } } }      // 1.21 数据组件
```
- `count` 可省略，默认 1；`components` 可省略。
- elven_trade 的 `output` 是**列表**，且用 compact codec：count==1 时可只写 `{"id": "..."}`，count!=1 时写完整 ItemStack。

#### StateIngredient（方块状态/标签/组合；pure_daisy、orechid*、marimorphosis 用）
```json
{ "type": "botania:block",  "block": "minecraft:stone" }                       // BlockTypeIngredient
{ "type": "botania:state",  "state": { "Name": "minecraft:calcite" } }          // BlockStateIngredient（BlockState.CODEC）
{ "type": "botania:tag",    "tag": "#minecraft:logs" }                          // BlockTagIngredient（注意 '#' 前缀！）
{ "type": "botania:any_of", "ingredients": [ <StateIngredient>, ... ] }          // AnyOfStateIngredient
{ "type": "botania:all_of_excluding",
  "ingredients": [ <StateIngredient>, ... ],
  "excluding": [ <StateIngredient>, ... ] }                                      // AllOfExcludingStateIngredient（excluding 可省略，默认 []）
{ "type": "botania:none" }                                                       // NONE（所有状态都匹配）
```
- dispatch 字段是 **`"type"`**（`StateIngredients.TYPED_CODEC`：registry = `botania:state_ingredient_type`）。
- 注册的 type id：`botania:none` / `botania:block` / `botania:state` / `botania:tag` / `botania:any_of` / `botania:all_of_excluding`
  （`StateIngredients.submitRegistrations`）。
- **实测：455 jar 里所有 9 个 StateIngredient tag 条目全部写成带 `#` 的字符串**（`#botania:marimorphosis_convertable`、`#minecraft:logs`）。
  不要写成无 `#` 的普通 id。
- 字段名（逐 codec，见 §2.10、§2.11）：`block`、`state`、`tag`、`ingredients`、`excluding`。

#### CacheableFunction（`pre_update_function` / `success_function`）
字符串函数 id，如 `"botania:ender_air_release"`（`CacheableFunction.CODEC`）。可选字段。

#### `neoforge:conditions` / `fabric:load_conditions`
配方 JSON 顶层的加载条件，由加载器在解析前处理，**不属于** Botania codec；KubeJS 写配方时一般不需要。

#### group / category
- `group`：`Codec.STRING`，可选，默认 `""`（目前只有 `mana_infusion` 有）。
- `category`：vanilla crafting 专用，Botania 自有类型没有。

---

## 2. 每种配方 type 的完整 schema + 真实 JSON 示例

> 下列每个字段都取自 455 jar 中对应 `Serializer` 的 `CODEC` 字节码（`fieldOf` / `optionalFieldOf` / `listOf` 的实参），
> 与 457 源码一致。示例全部来自 455 jar 的 `data/botania/recipe/`。

### 2.1 `botania:mana_infusion`

- serializer：`vazkii.botania.common.crafting.ManaInfusionRecipe$Serializer`
- codec：`ManaInfusionRecipe.Serializer.CODEC`；stream：`STREAM_CODEC`
- RecipeType：`BotaniaRecipeTypes.MANA_INFUSION_TYPE`；recipe 接口：`vazkii.botania.api.recipe.ManaInfusionRecipe`
- 用途：魔力池 + 输入物品 → 输出（可选炼金催化剂方块/状态，取决于池子下方方块）。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `output` | ItemStack | **必填** | - | `ItemStack.STRICT_CODEC.fieldOf("output")`；count 省略=1 |
| `input` | Ingredient | **必填** | - | `Ingredient.CODEC_NONEMPTY.fieldOf("input")`；支持 item/tag |
| `mana` | int | **必填** | - | `ExtraCodecs.intRange(1, ManaPoolBlock.MAX_MANA + 1)`；`MAX_MANA = 1000000`，即 1..1000001 |
| `group` | string | 可选 | `""` | `Codec.STRING.optionalFieldOf("group", "")` |
| `catalyst` | StateIngredient | 可选 | `botania:none`（无限制） | `StateIngredients.TYPED_CODEC.optionalFieldOf("catalyst", StateIngredients.NONE)` |

真实示例（无 catalyst，455 jar `mana_infusion/manasteel_ingot.json`）：
```json
{
  "type": "botania:mana_infusion",
  "input": {
    "tag": "c:ingots/iron"
  },
  "mana": 3000,
  "output": {
    "count": 1,
    "id": "botania:manasteel_ingot"
  }
}
```

真实示例（带 catalyst + group，`mana_infusion/alchemy/agricarnation_petite.json`）：
```json
{
  "type": "botania:mana_infusion",
  "catalyst": {
    "type": "botania:block",
    "block": "botania:alchemy_catalyst"
  },
  "group": "botania:flower_shrinking",
  "input": {
    "item": "botania:agricarnation"
  },
  "mana": 2500,
  "output": {
    "count": 1,
    "id": "botania:agricarnation_petite"
  }
}
```

### 2.2 `botania:pure_daisy`

- serializer：`vazkii.botania.common.crafting.PureDaisyRecipe$Serializer`
- codec：`PureDaisyRecipe.Serializer.CODEC`（`BlockStateRecipe` 基底，输入/输出是 StateIngredient）
- RecipeType：`BotaniaRecipeTypes.PURE_DAISY_TYPE`；接口：`api.recipe.PureDaisyRecipe`
- 用途：纯净雏菊把目标方块状态转换为输出状态；`time` = 转化耗时 tick。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `input` | StateIngredient | **必填** | - | `StateIngredients.TYPED_CODEC.fieldOf("input")` |
| `output` | StateIngredient | **必填** | - | `StateIngredients.TYPED_CODEC.fieldOf("output")` |
| `time` | int | 可选 | **0** | `ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 0)`；实际数据均写 150（`PureDaisyRecipe.DEFAULT_TIME=150` 只是常量，**不是** codec 默认值） |
| `copy_properties` | bool | 可选 | `false` | `Codec.BOOL.optionalFieldOf("copy_properties", false)`；true 时输出方块继承输入方块属性 |
| `pre_update_function` | CacheableFunction | 可选 | 无 | `CacheableFunction.CODEC.optionalFieldOf("pre_update_function")` |
| `success_function` | CacheableFunction | 可选 | 无 | `CacheableFunction.CODEC.optionalFieldOf("success_function")` |

真实示例（`pure_daisy/calcite.json`）：
```json
{
  "type": "botania:pure_daisy",
  "input": {
    "type": "botania:block",
    "block": "minecraft:dripstone_block"
  },
  "output": {
    "type": "botania:state",
    "state": {
      "Name": "minecraft:calcite"
    }
  },
  "time": 150
}
```

真实示例（带 `copy_properties` + `all_of_excluding` + `#tag`，`pure_daisy/livingwood_log.json`）：
```json
{
  "type": "botania:pure_daisy",
  "copy_properties": true,
  "input": {
    "type": "botania:all_of_excluding",
    "excluding": [
      {
        "type": "botania:any_of",
        "ingredients": [
          {
            "type": "botania:block",
            "block": "botania:livingwood_log"
          }
        ]
      }
    ],
    "ingredients": [
      {
        "type": "botania:tag",
        "tag": "#minecraft:logs"
      }
    ]
  },
  "output": {
    "type": "botania:block",
    "block": "botania:livingwood_log"
  },
  "time": 150
}
```

### 2.3 `botania:elven_trade`

- serializer：`vazkii.botania.common.crafting.ElvenTradeRecipe$Serializer`
- codec：`ElvenTradeRecipe.Serializer.CODEC`
- RecipeType：`BotaniaRecipeTypes.ELVEN_TRADE_TYPE`；接口：`api.recipe.ElvenTradeRecipe`
- 用途：精灵传送门投掷输入 → 输出（可多输入、多输出）。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `output` | ItemStack 数组 | **必填** | - | `ExtraCodecs.nonEmptyList(ITEM_STACK_COMPACT_CODEC.listOf()).fieldOf("output")`；元素可写完整 ItemStack，count==1 时可只写 `{"id": ...}` |
| `ingredients` | Ingredient 数组 | **必填** | - | `ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf()).fieldOf("ingredients")`；非空 |

真实示例（`elven_trade/alfglass.json`）：
```json
{
  "type": "botania:elven_trade",
  "ingredients": [
    {
      "item": "botania:managlass"
    }
  ],
  "output": [
    {
      "id": "botania:alfglass"
    }
  ]
}
```

### 2.4 `botania:elven_trade_lexicon`（特殊 serializer，无字段）

- serializer：`vazkii.botania.common.crafting.LexiconElvenTradeRecipe$Serializer`
- codec：`MapCodec.unit(() -> LexiconElvenTradeRecipe.INSTANCE)`；stream：`StreamCodec.unit(INSTANCE)`
- RecipeType：`ELVEN_TRADE_TYPE`（recipe class 继承接口默认 `getType()`）
- 语义：把一本 Botania 词典丢进精灵门 → 升级为精灵词典（设置 `botania:elven_unlock` 组件与 UNCOMMON 稀有度）。
  配方对象是单例 `LexiconElvenTradeRecipe.INSTANCE`，JSON **只有 `type` 一个字段**。

真实示例（`elven_trade/lexica_botania_upgrade.json`）：
```json
{
  "type": "botania:elven_trade_lexicon"
}
```

### 2.5 `botania:runic_altar`

- serializer：`vazkii.botania.common.crafting.RunicAltarRecipe$Serializer`
- codec：`RunicAltarRecipe.Serializer.CODEC`（`RAW_CODEC.validate(...)`）
- RecipeType：`BotaniaRecipeTypes.RUNIC_ALTAR_TYPE`；接口：`api.recipe.RunicAltarRecipe`
- 用途：符文祭坛，1 个 reagent 物品 + ingredients + catalysts（催化剂不消耗），注入魔力。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `ingredients` | Ingredient 数组 | **必填** | - | `Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")`；可以是空数组（与 catalysts 合计 1..16） |
| `catalysts` | Ingredient 数组 | **必填**（可为 `[]`） | - | `Ingredient.CODEC_NONEMPTY.listOf().fieldOf("catalysts")`；字段本身必填 |
| `reagent` | Ingredient | **必填** | - | `Ingredient.CODEC_NONEMPTY.fieldOf("reagent")`；祭坛中央的“配方催化剂”物品 |
| `mana` | int | **必填** | - | `ExtraCodecs.POSITIVE_INT.fieldOf("mana")`（≥1） |
| `output` | ItemStack | **必填** | - | `ItemStack.STRICT_CODEC.fieldOf("output")` |

- 额外校验（`CODEC.validate`）：`ingredients.size() + catalysts.size()` 必须 **1..16**；否则加载报错
  “Must have at least one ingredient or catalyst” / “Cannot have more than 16 ingredients and catalysts in total”。
- 构造函数还会校验：ingredients / catalysts / reagent 的物品集合两两不相交、Reagent 定义不得与其它重复；
  含 NeoForge 自定义测试 Ingredient 会直接抛异常。

真实示例（`runic_altar/rune_of_air.json`）：
```json
{
  "type": "botania:runic_altar",
  "catalysts": [],
  "ingredients": [
    {
      "tag": "c:dusts/mana"
    },
    {
      "tag": "c:ingots/manasteel"
    },
    {
      "tag": "minecraft:wool_carpets"
    },
    {
      "item": "minecraft:feather"
    },
    {
      "item": "minecraft:string"
    }
  ],
  "mana": 5200,
  "output": {
    "count": 2,
    "id": "botania:rune_of_air"
  },
  "reagent": {
    "item": "botania:livingrock"
  }
}
```

### 2.6 `botania:runic_altar_head`（特殊 serializer）

- serializer：`vazkii.botania.common.crafting.recipe.HeadRecipe$Serializer`
- codec/stream：直接 `xmap` 复用 `RunicAltarRecipe.Serializer.CODEC/STREAM_CODEC` → **字段与 `botania:runic_altar` 完全相同**。
- RecipeType：`RUNIC_ALTAR_TYPE`（`HeadRecipe extends RunicAltarRecipe`，不覆盖 `getType()`）
- 语义：把骷髅头/玩家头 + 命名牌或成书（书中文本含皮肤 URL/hash/base64）在符文祭坛合成出带 `minecraft:profile` 的玩家头颅。
  `matches` 额外要求：恰好一个带自定义名称的命名牌，或一本前两页含合法皮肤信息的成书；`assemble` 写入 `DataComponents.PROFILE`。

真实示例（`runic_altar/player_head.json`，注意 ingredients 里第 4 项是**多选一数组**）：
```json
{
  "type": "botania:runic_altar_head",
  "catalysts": [],
  "ingredients": [
    {
      "item": "minecraft:skeleton_skull"
    },
    {
      "tag": "c:dusts/pixie"
    },
    {
      "tag": "c:gems/prismarine"
    },
    [
      {
        "item": "minecraft:name_tag"
      },
      {
        "item": "minecraft:written_book"
      }
    ],
    {
      "item": "minecraft:golden_apple"
    }
  ],
  "mana": 22500,
  "output": {
    "count": 1,
    "id": "minecraft:player_head"
  },
  "reagent": {
    "item": "botania:livingrock"
  }
}
```

### 2.7 `botania:brew`

- serializer：`vazkii.botania.common.crafting.BotanicalBreweryRecipe$Serializer`
- codec：`BotanicalBreweryRecipe.Serializer.CODEC`
- RecipeType：`BotaniaRecipeTypes.BREW_TYPE`；接口：`api.recipe.BotanicalBreweryRecipe`
- 用途：植物酿造台：1 个容器（`BrewContainer` 物品，由 `RecipeUtils.getBrewContainerIngredient()` 动态生成）
  + ingredients → 得到带 brew 的药水/血之 pendant/熏香。**魔力消耗不在配方里**，来自 Brew 对象本身（`brew.getManaCost()`）。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `brew` | Brew 注册名 | **必填** | - | `BotaniaAPI.instance().getBrewRegistry().byNameCodec().fieldOf("brew")`；例 `botania:absolution` |
| `ingredients` | Ingredient 数组 | **必填** | - | `ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf()).fieldOf("ingredients")` |

- 匹配规则（`matches`）：输入数量必须 = `ingredients.size() + 1`，且第 0 个输入必须是 `BrewContainer`；
  其余按无序匹配。
- **brew 是注册表条目**（`botania:brews`），不是本配方 JSON 定义的（见 §9.3）。

真实示例（`brew/absolution.json`）：
```json
{
  "type": "botania:brew",
  "brew": "botania:absolution",
  "ingredients": [
    {
      "item": "minecraft:nether_wart"
    },
    {
      "item": "minecraft:quartz"
    },
    {
      "item": "minecraft:emerald"
    },
    {
      "item": "minecraft:melon_slice"
    }
  ]
}
```

### 2.8 `botania:petal_apothecary`

- serializer：`vazkii.botania.common.crafting.PetalApothecaryRecipe$Serializer`
- codec：`PetalApothecaryRecipe.Serializer.CODEC`
- RecipeType：`BotaniaRecipeTypes.PETAL_APOTHECARY_TYPE`；接口：`api.recipe.PetalApothecaryRecipe`
- 用途：花瓣药台：水 + 花瓣等 ingredients + 1 个 reagent（种子类物品）→ 产物（花/种子等）。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `output` | ItemStack | **必填** | - | `ItemStack.STRICT_CODEC.fieldOf("output")`；可带 `components`（如 `minecraft:profile`） |
| `reagent` | Ingredient | **必填** | - | `Ingredient.CODEC_NONEMPTY.fieldOf("reagent")`；**1.21.1 是必填**（不是可选） |
| `ingredients` | Ingredient 数组 | **必填** | - | `Ingredient.CODEC_NONEMPTY.listOf(1, 16).fieldOf("ingredients")`；**1..16 个** |

真实示例（`petal_apothecary/agricarnation.json`）：
```json
{
  "type": "botania:petal_apothecary",
  "ingredients": [
    {
      "tag": "botania:petals/lime"
    },
    {
      "tag": "botania:petals/lime"
    },
    {
      "tag": "botania:petals/green"
    },
    {
      "tag": "botania:petals/yellow"
    },
    {
      "item": "botania:rune_of_spring"
    },
    {
      "item": "botania:redstone_root"
    }
  ],
  "output": {
    "count": 1,
    "id": "botania:agricarnation"
  },
  "reagent": {
    "tag": "botania:seed_apothecary_reagent"
  }
}
```

真实示例（输出带数据组件，`petal_apothecary/vazkii_head.json`，节选 output）：
```json
  "output": {
    "components": {
      "minecraft:profile": {
        "name": "Vazkii"
      }
    },
    "count": 1,
    "id": "minecraft:player_head"
  }
```

### 2.9 `botania:terrestrial_agglomeration_plate`（泰拉凝聚板，旧名 terra_plate）

- serializer：`vazkii.botania.common.crafting.TerrestrialAgglomerationRecipe$Serializer`
- codec：`TerrestrialAgglomerationRecipe.Serializer.CODEC`
- RecipeType：`BotaniaRecipeTypes.TERRA_PLATE_TYPE`；接口：`vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe`
- 用途：3x3 平台上的泰拉凝聚板，投入物品 + 火花/魔力，充满后产出。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `ingredients` | Ingredient 数组 | **必填** | - | `ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf()).fieldOf("ingredients")`；非空 |
| `mana` | int | **必填** | - | `ExtraCodecs.POSITIVE_INT.fieldOf("mana")`（≥1） |
| `result` | ItemStack | **必填** | - | `ItemStack.STRICT_CODEC.fieldOf("result")`（注意字段名是 **`result`**，不是 output） |

- 匹配规则（`matches`）：输入物品总数必须 == `ingredients.size()`（堆叠会展开成单个），无序匹配；
  物品来自泰拉板上方 1x1x1 体积内的 `ItemEntity`（见 §5.1）。
- 产物在 `finishCrafting` 里在板中心生成（`x+0.5, y+0.2, z+0.5`）。

真实示例（`terrestrial_agglomeration_plate/terrasteel_ingot.json`，455 jar 里唯一一条）：
```json
{
  "type": "botania:terrestrial_agglomeration_plate",
  "ingredients": [
    {
      "tag": "c:ingots/manasteel"
    },
    {
      "tag": "c:gems/mana_pearl"
    },
    {
      "tag": "c:gems/mana_diamond"
    }
  ],
  "mana": 500000,
  "result": {
    "count": 1,
    "id": "botania:terrasteel_ingot"
  }
}
```

### 2.10 `botania:orechid`

- serializer：`vazkii.botania.common.crafting.OrechidRecipe$Serializer`
- codec：`OrechidRecipe.Serializer.CODEC`（`RAW_CODEC.validate(...)`）
- RecipeType：`BotaniaRecipeTypes.ORECHID_TYPE`；接口：`api.recipe.OrechidRecipe`
- 用途：矿化花（Orechid）把 input 状态方块随机变成 output 状态方块，按 weight 加权；`cooldown` tick 间隔，`mana` 每次消耗。

| 字段 | 类型 | 必填 | 默认值 | codec 约束 / 说明 |
|---|---|---|---|---|
| `input` | StateIngredient | **必填** | - | `StateIngredients.TYPED_CODEC.fieldOf("input")` |
| `output` | StateIngredient | **必填** | - | `StateIngredients.TYPED_CODEC.fieldOf("output")` |
| `cooldown` | int | 可选 | `0` | `ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cooldown", 0)` |
| `mana` | int | **必填** | - | `ExtraCodecs.POSITIVE_INT.fieldOf("mana")`（≥1） |
| `weight` | int | 可选 | `0` | `ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("weight", 0)` |
| `biome_bonus_weight` | int | 可选 | `0` | `Codec.INT.optionalFieldOf("biome_bonus_weight", 0)`（可为负） |
| `biome_bonus_tag` | biome tag | 可选 | 无 | `TagKey.codec(Registries.BIOME).optionalFieldOf("biome_bonus_tag")` |
| `pre_update_function` | CacheableFunction | 可选 | 无 | `CacheableFunction.CODEC.optionalFieldOf(...)` |
| `success_function` | CacheableFunction | 可选 | 无 | `CacheableFunction.CODEC.optionalFieldOf(...)` |

- `CODEC.validate`：`weight == 0 && biome_bonus_weight == 0` → 报错 “Weight and bonus weight cannot both be 0”；
  `weight + biome_bonus_weight < 0` → 报错 “Weight combined with bonus cannot be less than 0”。
- `getWeight(Level,pos)`：若方块位置所在生物群系属于 `biome_bonus_tag`，权重 = `weight + biome_bonus_weight`，否则 `weight`。

真实示例（`orechid/copper_ore.json`；含可选加载条件，可省略）：
```json
{
  "fabric:load_conditions": [
    {
      "condition": "fabric:not",
      "value": {
        "condition": "fabric:all_mods_loaded",
        "values": [
          "gardenofglass"
        ]
      }
    }
  ],
  "neoforge:conditions": [
    {
      "type": "neoforge:not",
      "value": {
        "type": "neoforge:mod_loaded",
        "modid": "gardenofglass"
      }
    }
  ],
  "type": "botania:orechid",
  "biome_bonus_tag": "botania:orechid_stone_copper_bonus",
  "biome_bonus_weight": 2500,
  "cooldown": 100,
  "input": {
    "type": "botania:block",
    "block": "minecraft:stone"
  },
  "mana": 17500,
  "output": {
    "type": "botania:block",
    "block": "minecraft:copper_ore"
  },
  "weight": 2724
}
```

### 2.11 `botania:orechid_ignem`

- serializer：`vazkii.botania.common.crafting.OrechidIgnemRecipe$Serializer`
- codec：`OrechidRecipe.SERIALIZER.codec().xmap(OrechidIgnemRecipe::new, Function.identity())`
  → **字段与 `botania:orechid` 完全相同**（见 §2.10 表）。
- RecipeType：`ORECHID_IGNEM_TYPE`；`getType()` 返回 ignem 类型。
- 语义：炽热矿化花（在 Nether 工作），`cooldown` 与 `mana` 有效；同样支持 biome bonus。

真实示例（`orechid_ignem/nether_gold_ore.json`）：
```json
{
  "type": "botania:orechid_ignem",
  "cooldown": 100,
  "input": {
    "type": "botania:block",
    "block": "minecraft:netherrack"
  },
  "mana": 20000,
  "output": {
    "type": "botania:block",
    "block": "minecraft:nether_gold_ore"
  },
  "weight": 2613
}
```

### 2.12 `botania:marimorphosis`

- serializer：`vazkii.botania.common.crafting.MarimorphosisRecipe$Serializer`
- codec：同样 `xmap` 复用 `OrechidRecipe.Serializer.codec()` → **字段与 `botania:orechid` 完全相同**。
- RecipeType：`MARIMORPHOSIS_TYPE`。
- 差异：`MarimorphosisRecipe` 构造函数把 `cooldown` 固定为 **0**（JSON 里写 cooldown 也会被丢弃/忽略）；
  `mana`、`weight`、`biome_bonus_weight`、`biome_bonus_tag` 有效。
- 语义：大理石花（Marimorphosis）把 `#botania:marimorphosis_convertable` 方块在特定生物群系加成下变成多种变种石。

真实示例（`marimorphosis/cataclasite.json`，455 jar 版本）：
```json
{
  "type": "botania:marimorphosis",
  "biome_bonus_tag": "botania:marimorphosis_swamp_bonus",
  "biome_bonus_weight": 11,
  "input": {
    "type": "botania:tag",
    "tag": "#botania:marimorphosis_convertable"
  },
  "mana": 12,
  "output": {
    "type": "botania:block",
    "block": "botania:cataclasite"
  },
  "weight": 1
}
```

### 2.13 crafting 特殊 serializer 的真实 JSON 形态（了解即可）

- 大多数 special crafting JSON 只有 `{"type": "...", "category": "..."}`，例如
  `data/botania/recipe/dynamic/ancient_will_attach.json`：
```json
{
  "type": "botania:crafting_special_ancient_will_attach",
  "category": "misc"
}
```
- `botania:crafting_special_tiara_wings` 有 `variant`（int，0..8）与 `material`（Ingredient，variant 0 时为 `[]`）：
```json
{
  "type": "botania:crafting_special_tiara_wings",
  "category": "equipment",
  "material": {
    "item": "minecraft:quartz"
  },
  "variant": 1
}
```
- shaped 变体（`crafting_shaped_armor_upgrade` / `crafting_shaped_mana_upgrade` / `crafting_shaped_wand` /
  `crafting_shaped_water_bottle_matching`）与 vanilla shaped 结构相同：`pattern`、`key`、`result`、`category`；
  `crafting_shapeless_lexicon_return` / `crafting_shapeless_mana_upgrade` 与 vanilla shapeless 相同：`ingredients`、`result`、`category`。

---

## 3. 配方匹配的通用规则（KubeJS schema 语义参考）

| 配方 | 输入匹配 | 输入数量 | 备注 |
|---|---|---|---|
| `mana_infusion` | `input.test(stack)`（只匹配 1 个物品） | 1 | 催化剂是池子下方方块的状态匹配 |
| `pure_daisy` | StateIngredient.test(BlockState) | - | 方块状态配方 |
| `elven_trade` | `StackedContents.canCraft`（无序） | ≥1 | 可多输出 |
| `runic_altar` | `input.size() == ingredients.size()+catalysts.size()` 且 `wrapAllIngredients` canCraft | 1..16 | 催化剂不消耗，`getRemainingItems` 返回催化剂原物 |
| `brew` | 第 0 个是 BrewContainer 且总量 = ingredients+1 | ≥2 | 魔力来自 Brew |
| `petal_apothecary` | `RecipeUtils.matches`：数量相等 + `StackedContents.canCraft`（无序） | 1..16 | reagent 不进输入，由方块逻辑处理 |
| `terrestrial_agglomeration_plate` | 数量相等 + `StackedContents.canCraft`（无序） | = ingredients.size() | 见 §5 |
| `orechid`/`orechid_ignem`/`marimorphosis` | StateIngredient.test | - | 随机加权 |

- `RecipeUtils.matches(recipe, input, specialMatching)`（public static，455 存在）：
  要求 `input.size() == recipe.getIngredients().size()`；只有 1 个 ingredient 时直接 `test`；
  否则 `input.getStackedContents().canCraft(recipe, null)`（**无序/形状无关**）。
- 物品输入 `ProcessingRecipeInput` 由 `RecipeUtils.getInputFromEntities(List<ItemEntity>)` 展开：
  每个 `ItemEntity` 的 `ItemStack.getCount()` 会展开成对应数量的单物品槽（顺序 = 实体列表顺序）。
- 泰拉板/extended 配方若使用 `requiresCustomTesting()` 的 NeoForge 自定义 Ingredient，会走
  `XplatAbstractions.matchesWithCustomTestIngredients`，但**物品识别缓存不支持**（见 §7.5），不建议。

---

## 4. 泰拉凝聚板：类、字段、方法（455 jar 精确签名）

### 4.1 `TerrestrialAgglomerationPlateBlockEntity`

全限定名：`vazkii.botania.common.block.block_entity.TerrestrialAgglomerationPlateBlockEntity`
（源码 `Xplat/src/main/java/vazkii/botania/common/block/block_entity/TerrestrialAgglomerationPlateBlockEntity.java`）

类声明（455）：
```java
public class TerrestrialAgglomerationPlateBlockEntity extends BlockEntity
        implements vazkii.botania.api.mana.spark.SparkAttachable, vazkii.botania.api.mana.ManaReceiver
```
> ⚠ 455 的接口名是 `SparkAttachable`（457 改名为 `ManaSparkAttachable`）。

**私有字段（准确类型）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `MULTIBLOCK` | `public static final Supplier<IMultiblock>` | Patchouli 多方块模板（memoized） |
| `BLOCK_EVENT_PROGRESS_UPDATE` | `public static final int = 0` | 进度条 blockEvent |
| `BLOCK_EVENT_CRAFTING_EFFECT` | `public static final int = 1` | 完成特效 blockEvent |
| `BLOCK_EVENT_CRAFTING_ABORTED` | `public static final int = 2` | 中断特效 blockEvent |
| `TAG_MANA` | `private static final String = "mana"` | NBT key |
| `TAG_MANA_TO_GET` | `private static final String = "mana_to_get"` | NBT key |
| `mana` | `private int` | 当前已收集魔力 |
| `manaToGet` | `private int` | 目标魔力（= recipe.getMana()） |
| `currentProgress` | `private int = -1` | 进度 0..100，-1 表示无 |
| `lastProgressTick` | `private long` | 上一次进度 blockEvent 的 gameTime |
| `cachedCheck` | `private final RecipeManager.CachedCheck<ProcessingRecipeInput, TerrestrialAgglomerationRecipe>` | 由 `RecipeManager.createCheck(BotaniaRecipeTypes.TERRA_PLATE_TYPE)` 创建 |

**方法（mojmap 名 + 精确描述符）**

| 方法 | 描述符 | 可见性/static | 说明 |
|---|---|---|---|
| `serverCollectingTick` | `(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lvazkii/botania/common/block/block_entity/TerrestrialAgglomerationPlateBlockEntity;)V` | public static | 服务端 COLLECTING ticker |
| `clientCollectingTick` | 同上 | public static | 客户端 COLLECTING ticker（粒子） |
| `serverDissipatingTick` | 同上 | public static | 服务端 DISSIPATING ticker |
| `finishCrafting` | `(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/List;Lvazkii/botania/api/recipe/ProcessingRecipeInput;Lnet/minecraft/world/item/crafting/RecipeHolder;)V` | **private** | 合成成功 |
| `stopCollecting` | `()V` | **private** | 中断/重置 |
| `tryStartProcessing` | `()V` | public | 由 Block 的 scheduled tick 调用 |
| `getCraftingPlayer` | `(Ljava/util/List;)Lnet/minecraft/world/entity/player/Player;` | private static | 取最早 owner |
| `getItemEntities` | `()Ljava/util/List;` | **private** | 板内物品实体 |
| `hasValidPlatform` | `()Z` | **private** | 多方块校验 |
| `triggerEvent` | `(II)Z` | public | blockEvent 处理 |
| `saveAdditional` | `(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V` | protected | 存 mana / mana_to_get |
| `loadAdditional` | 同上 | protected | 读 |
| `receiveMana` | `(I)V` | public | `mana = max(0, mana+delta)` + 比较器更新 |
| `getCurrentMana` | `()I` | public | |
| `isFull` | `()Z` | public | `mana >= manaToGet` |
| `canReceiveManaFromBursts` | `()Z` | public | `manaToGet > 0` |
| `canAttachSpark` | `(Lnet/minecraft/world/item/ItemStack;)Z` | public | 恒 true |
| `areIncomingTransfersDone` | `()Z` | public | `mana >= manaToGet` |
| `getAvailableSpaceForMana` | `()I` | public | `max(0, manaToGet-mana)` |
| `getCompletion` | `()F` | public | `currentProgress/100f` |
| `getComparatorLevel` | `()I` | public | 1..15 或 1/0 |

### 4.2 `TerrestrialAgglomerationPlateBlock`

全限定名：`vazkii.botania.common.block.mana.TerrestrialAgglomerationPlateBlock`
（源码 `.../common/block/mana/TerrestrialAgglomerationPlateBlock.java`）

- 继承 `vazkii.botania.common.block.BotaniaWaterloggedBlock`（带 `waterlogged` 属性），实现 `EntityBlock`。
- 方块属性（`BotaniaBlocks.TERRESTRIAL_AGGLOMERATION_PLATE`）：`Properties.of().mapColor(MapColor.LAPIS).strength(3,10).sound(SoundType.METAL).requiresCorrectToolForDrops()`。
- 方块状态属性：`waterlogged` + `BotaniaStateProperties.TERRA_PLATE_STATE`；默认 `TerraPlateState.IDLE`。
- `getShape = box(0,0,0,16,3,16)`。
- 关键方法：
  - `useItemOn`：若 `BotaniaRecipeIngredientsCache.isTerraPlateInputItem(level, stack.getItem())`，
    在**服务端**把物品 `split(1)` 生成 `ItemEntity` 于 `(x+0.5, y+0.5, z+0.5)`、`setThrower(player)`、
    `setPickUpDelay(40)`、`deltaMovement=ZERO`，返回 `sidedSuccess`；否则 PASS。
  - `entityInside`（服务端也执行）：当 entity 是 `ItemEntity`、当前状态 `isLookingForIngredients()`、该位置没有已调度的
    block tick、`ItemSource.HOLDER.getFor(item) != ItemSources.TERRA_PLATE`（避免产物循环触发）、
    且 `BotaniaRecipeIngredientsCache.isTerraPlateInputItem(level, item.getItem().getItem())` 时，
    `level.scheduleTick(pos, this, 1)`。
  - `tick(BlockState, ServerLevel, BlockPos, RandomSource)`：若 `state.isLookingForIngredients()` →
    `level.getBlockEntity(pos, BotaniaBlockEntities.TERRESTRIAL_AGGLOMERATION_PLATE).ifPresent(BE::tryStartProcessing)`；
    若 `state == DONE` → 设回 `IDLE`（`Block.UPDATE_ALL`）。
  - `getTicker`：按 `TERRA_PLATE_STATE`：
    - `COLLECTING` → 客户端 `clientCollectingTick` / 服务端 `serverCollectingTick`
    - `DISSIPATING` → 客户端 null / 服务端 `serverDissipatingTick`
    - `IDLE`、`DONE` → null（**IDLE 时没有每 tick 的 BE ticker！**）
  - `hasAnalogOutputSignal=true`；`getAnalogOutputSignal`：IDLE→0，DONE→15，其余→`getComparatorLevel()`。
  - `animateTick`：DISSIPATING 状态时生成蓝色 Wisp 粒子。

### 4.3 `TerraPlateState` 状态机

枚举 `vazkii.botania.api.state.enums.TerraPlateState`（`StringRepresentable`）：

| 常量 | 序列化名 | `isLookingForIngredients()` |
|---|---|---|
| `IDLE` | `idle` | `true` |
| `COLLECTING` | `collecting` | `false` |
| `DISSIPATING` | `dissipating` | `true` |
| `DONE` | `done` | `false` |

状态属性：`vazkii.botania.api.state.BotaniaStateProperties.TERRA_PLATE_STATE =
EnumProperty.create("state", TerraPlateState.class)`。

```
IDLE --(tryStartProcessing: 平台+配方命中)--> COLLECTING --(mana 满)--> DONE --(Block.tick 4t)--> IDLE
  ^                                              |
  |                                              +--(平台/配方/物品失效 in serverCollectingTick)--> stopCollecting:
  |                                                                            mana>0 -> DISSIPATING, 否则 IDLE
  +------------- DISSIPATING --(receiveMana(-1000) 直到 mana==0)--> IDLE
```

- `tryStartProcessing`：`hasValidPlatform()` 为 false 直接 return；否则取物品 → `cachedCheck.getRecipeFor`；
  有配方才 `manaToGet = recipe.getMana()` 并 `setBlock(..., COLLECTING, UPDATE_ALL)`。
  **它不重置 mana/currentProgress**。
- `stopCollecting`：`manaToGet = 0`；`setBlock(state = mana > 0 ? DISSIPATING : IDLE, UPDATE_ALL)`；
  `blockEvent(BLOCK_EVENT_CRAFTING_ABORTED, -1)`（客户端会执行 10 次 `animateTick` 粒子）。
- `serverDissipatingTick`：每 tick `receiveMana(-1000)`；`mana == 0` 时设回 IDLE。

### 4.4 完整调用链（服务端）

```
玩家右键 / 物品实体进入方块空间
  └─ Block.useItemOn → 生成 ItemEntity（板内）           （或外部投掷的物品经过 entityInside）
  └─ Block.entityInside → 条件满足时 level.scheduleTick(pos, block, 1)
        └─ Block.tick (延迟 1 tick)
              └─ BE.tryStartProcessing()
                    ├─ hasValidPlatform()   // 私有：MULTIBLOCK.validate(level, pos.below()) != null
                    ├─ getItemEntities()    // AABB(worldPosition) + EntitySelector.ENTITY_STILL_ALIVE
                    ├─ RecipeUtils.getInputFromEntities(items)
                    ├─ cachedCheck.getRecipeFor(input, level)
                    └─ 命中 → manaToGet=recipe.getMana(); state=COLLECTING
  ...
  └─ 每 tick BlockEntityTicker (state==COLLECTING, server) → serverCollectingTick(level,pos,state,self)
        ├─ if (!hasValidPlatform()) { stopCollecting(); return; }
        ├─ items = getItemEntities(); input = RecipeUtils.getInputFromEntities(items)
        ├─ recipeHolder = cachedCheck.getRecipeFor(input, level)
        ├─ if empty { stopCollecting(); return; }
        ├─ if (manaToGet != recipe.getMana()) { manaToGet = recipe.getMana(); setChanged(); }
        ├─ 注册火花魔力转移：
        │    ManaSpark spark = SparkAttachable.getAttachedSpark(level, pos);            // 455
        │    SparkHelper.registerTransferFromSparksAround(spark, level, pos);           // 455
        ├─ if (mana > 0) { newProgress = 100*mana/manaToGet;
        │     if (newProgress != currentProgress || gameTime-lastProgressTick > 10)
        │        { currentProgress = newProgress; level.blockEvent(pos, block, 0, newProgress); } }
        ├─ if (mana < manaToGet) return;
        └─ finishCrafting(level, pos, state, itemEntities, recipeInput, recipeHolder)
              ├─ player = getCraftingPlayer(items)
              ├─ result = recipeHolder.value().assemble(recipeInput, level.registryAccess())
              ├─ 玩家触发 triggerRecipeCrafted / onCraftedBy（无玩家则 onCraftedBySystem）
              ├─ 所有输入 ItemEntity discard()
              ├─ new ItemEntity(level, x+0.5, y+0.2, z+0.5, result, 0,0,0)
              ├─ ItemSource.HOLDER.setFor(item, ItemSources.TERRA_PLATE)
              ├─ addFreshEntity(item)
              ├─ playSound(null, pos, BotaniaSounds.TERRA_PLATE_CRAFT, SoundSource.BLOCKS, 1F, 1F)
              ├─ mana = 0; manaToGet = 0
              ├─ level.blockEvent(pos, block, 1, 0)                 // CRAFTING_EFFECT
              ├─ level.setBlock(pos, state.setValue(TERRA_PLATE_STATE, DONE), UPDATE_ALL)
              └─ level.scheduleTick(pos, block, 4)                  // 4 tick 后 Block.tick 回 IDLE
```

### 4.5 多方块结构（Patchouli `MULTIBLOCK`，逐格世界坐标）

定义（455 字节码与源码一致）：
```java
public static final Supplier<IMultiblock> MULTIBLOCK = Suppliers.memoize(() -> PatchouliAPI.get().makeMultiblock(
    new String[][] {
        { "___", "_P_", "___" },   // 第 0 层（写在最上面）
        { "RLR", "L0L", "RLR" }    // 第 1 层
    },
    'P', BotaniaBlocks.TERRESTRIAL_AGGLOMERATION_PLATE,
    'R', PatchouliAPI.get().tagMatcher(BotaniaTags.Blocks.TERRA_PLATE_BASE),
    '0', PatchouliAPI.get().tagMatcher(BotaniaTags.Blocks.TERRA_PLATE_BASE),
    'L', PatchouliAPI.get().tagMatcher(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lapis")))
));
```
`hasValidPlatform()`：
```java
return MULTIBLOCK.get().validate(level, getBlockPos().below()) != null;
```

**Patchouli 语义（已用 Patchouli 1.21.1-93 源码 `DenseMultiblock.java` / `AbstractMultiblock.java` 核实）**
- `pattern` 第 1 维是层（Y），书写顺序 **从上到下**；第 2 维是 X（行），字符串是 Z 轴字符。
- `'0'` 是特殊“中心”字符：该格正好落在传给 `validate` 的 `pos` 上 → `validate(level, pos.below())`
  即锚点 = **板正下方那一格**。
- `validate(level,pos)` 依次尝试 4 个水平 `Rotation`（`NONE/CLOCKWISE_90/CLOCKWISE_180/COUNTERCLOCKWISE_90`），
  返回首个匹配的 `Rotation`，全部失败返回 `null`；`hasValidPlatform` 只判断非 null（不保留 rotation）。
- `'_'` 未在映射中指定时默认 `StateMatcher.ANY`（**任意方块，不是空气**）；
  `'R'`/`'0'` = tag `botania:terra_plate_base`；`'L'` = tag `c:storage_blocks/lapis`。
- tag 值（455 jar `tags/block/terra_plate_base.json`）：`["botania:livingrock", "botania:shimmerrock"]`。
- tag `c:storage_blocks/lapis` 由 NeoForge 提供（NeoForge 21.1.248 merged jar 内有
  `data/c/tags/block/storage_blocks/lapis.json`，值 = `minecraft:lapis_block`）。

**世界坐标逐格含义**（板位置 `P = (px, py, pz)`，锚点 `A = P.below() = (px, py-1, pz)`，rotation = NONE）：

| 世界位置（锚点 `A = platePos.below()`） | 局部 (x,z) | 要求方块 |
|---|---|---|
| `A + (0,0,0)` | (0,0) | `#botania:terra_plate_base`（中心 `'0'`） |
| `A + (±1,0,±1)`（4 个角） | (±1,±1) | `#botania:terra_plate_base`（`'R'`） |
| `A + (±1,0,0)`、`A + (0,0,±1)`（4 边中点） | (±1,0),(0,±1) | `#c:storage_blocks/lapis`（`'L'`） |
| `P + (0,0,0)` | (0,0) 上层 | `botania:terrestrial_agglomeration_plate`（`'P'`） |
| `P + (dx,0,dz)` 其它 8 格 | 其它 | `'_'` = ANY（任意方块/空气均可） |

> 即：板下一层是 3x3 基座，**中心和四角**用活石/微光石（tag），**四边中点**用青金石块（tag）；
> 板放在中心正上方。基座层其它没有提到的格子不存在（3x3 就是全部）。
> 与原版 `R`/`L` 的混淆：`R` 在**角落**，`L`（lapis）在**边中点**。

### 4.6 blockEvent / 声音 / 粒子 / NBT

- blockEvent id：`0` = 进度更新（param=0..100）；`1` = 合成完成特效；`2` = 中断特效（param=-1）。
  `Block.triggerEvent` → `BotaniaBlock.triggerEvent` 会转发到 BE 的 `triggerEvent(int,int)`。
- BE `triggerEvent`：
  - `1`：`currentProgress=-1`；客户端生成 15 个绿色 Wisp 粒子。
  - `2`：`currentProgress=-1`；客户端调用 10 次 `animateTick`（DISSIPATING 状态才有粒子）。
  - `0`：`currentProgress=param; lastProgressTick=gameTime`。
- 声音：`BotaniaSounds.TERRA_PLATE_CRAFT`，id = **`botania:terra_plate_craft`**（`SoundEvent.createVariableRangeEvent`），
  在 `finishCrafting` 里 `playSound(null, pos, ..., SoundSource.BLOCKS, 1F, 1F)`。
- 粒子：`clientCollectingTick` 生成 3 个环绕的 Wisp（颜色随进度 r=0,g=p,b=1-p）、`animateTick` DISSIPATING 蓝粒子。
- NBT（`saveAdditional`/`loadAdditional`）：仅 `mana`（int）与 `mana_to_get`（int）；`currentProgress`/`lastProgressTick` 不持久化。

### 4.7 客户端渲染

`vazkii.botania.client.render.block_entity.TerrestrialAgglomerationPlateBlockEntityRenderer`
（`EntityRenderers` 注册到 `BotaniaBlockEntities.TERRESTRIAL_AGGLOMERATION_PLATE`）— 与 mixin 无关，无需处理。

---

## 5. 泰拉凝聚板扩展（kubejs_botania_neo）——最终采用方案

> 本节结论 = 已在代码中实现并采纳的最终方案（Lead 确认）。实现位于：
> `src/main/java/dev/kubejsbotania/neo/recipe/TerraPlateExtensionRecipe.java`、
> `.../TerraPlateExtensionEngine.java`、`.../TerraPlatePosition.java`、
> `src/main/java/dev/kubejsbotania/neo/mixin/TerrestrialAgglomerationPlateBlockEntityMixin.java`、
> `.../kubejs/TerraPlateExtensionSchema.java`、`.../ModRecipes.java`。

### 5.1 需求

在原版泰拉板上额外支持：

1. **自定义下方 3x3 方块/流体图案**（`below`）：板下一层（`plate.below()`）的 `(x,z)` 位置要求指定方块/方块标签/流体；
2. **旋转自动匹配**：图案自动尝试 4 个水平旋转（0/90/180/270），命中首个可用 rotation；
3. **合成后逐格方块转化**（`result_blocks`）：成功合成后按**同一个 rotation** 把指定格替换为方块/流体；
4. **自定义魔力**：`mana` 字段（默认 500000）。

### 5.2 为什么可以“零侵入”复用 Botania 流程（字节码证据）

1. **索引按 `recipe.getType()`，不按 serializer/JSON type**：
   1.21.1 `RecipeManager.replaceRecipes` 字节码（merged jar）：
   `holder.value().getType()` → `byType.put(type, holder)`。
   `RecipeManager.fromJson` 只用 `Recipe.CODEC`（按 JSON `"type"` 找 `RecipeSerializer`）反序列化，
   **不校验** `recipe.getType()` 与 serializer id 是否一致。
2. 因此：自定义 recipe `implements TerrestrialAgglomerationRecipe` 且**不覆盖 `getType()`** 时，
   接口默认实现返回 `BuiltInRegistries.RECIPE_TYPE.get(botania:terrestrial_agglomeration_plate)`
   （Botania 在 `NeoForgeCommonInitializer:209` 注册过该 RecipeType），
   该 recipe 会被索引到 **`BotaniaRecipeTypes.TERRA_PLATE_TYPE`** 下。
3. 于是以下全部自动生效，**不需要 mixin**：
   - Botania 的 `cachedCheck.getRecipeFor(...)`（按 TERRA_PLATE_TYPE 查，会调用我们的 `matches`）；
   - `BotaniaRecipeIngredientsCache.isTerraPlateInputItem(...)`（按 TERRA_PLATE_TYPE 的 ingredients 建缓存，
     我们的输入物品自动变成“可放上板”的物品）；
   - Botania 的配方查看器（JEI/REI/EMI）按 TERRA_PLATE_TYPE/RecipeHolder 读取。
4. `RecipeManager.CachedCheck`（1.21.1）实现类 `RecipeManager$1` 只有 `lastRecipe: ResourceLocation`
   一个缓存字段，每次 `getRecipeFor` 都会调用 `Recipe.matches(input,level)`
   （4 参重载先 `byKeyTyped` 快速试上次配方，不中再遍历 `byType` filter），**不存在“输入相同跳过 matches”的缓存**。
   → 用 ThreadLocal 在 `hasValidPlatform` 与 `matches` 之间传递上下文是可靠且每 tick 生效的。

**唯一前提（已在工程实现）**：JSON 的 `"type"` 必须是我们注册的自定义 serializer id
**`kubejs_botania_neo:terra_plate`**，不能写 `botania:terrestrial_agglomeration_plate`
（否则 Botania 自己的 codec 会丢掉 `below`/`result_blocks` 等扩展字段）。
`ModRecipes` 用 `DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID).register("terra_plate", ...)`
注册该 serializer。

### 5.3 扩展配方 JSON / KubeJS schema

JSON type：`kubejs_botania_neo:terra_plate`
serializer：`dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe.SERIALIZER`
recipe class：`dev.kubejsbotania.neo.recipe.TerraPlateExtensionRecipe`
RecipeType：继承接口默认 → **`botania:terrestrial_agglomeration_plate`**（这就是关键）

| 字段 | 类型 | 必填 | 默认值 | codec |
|---|---|---|---|---|
| `ingredients` | Ingredient 数组（非空） | **必填** | - | `Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")` |
| `mana` | int | 可选 | **500000** | `ExtraCodecs.POSITIVE_INT.optionalFieldOf("mana", 500000)` |
| `output` | ItemStack | **必填** | - | `ItemStack.STRICT_CODEC.fieldOf("output")` |
| `below` | TerraPlatePosition 列表 | 可选 | `[]` | `TerraPlatePosition.LIST_CODEC.optionalFieldOf("below", List.of())` |
| `result_blocks` | TerraPlatePosition 列表 | 可选 | `[]` | 同上 |

`TerraPlatePosition` 单项：
```json
{ "x": -1, "z": -1, "block": "minecraft:stone" }
{ "x": 0,  "z": 0,  "fluid": "minecraft:water" }
{ "x": 1,  "z": 0,  "block": "#c:storage_blocks/lapis" }
```
- `x`、`z`：**必填 int**，相对板位置的水平偏移（板下一层，y 固定 -1）；通常 -1..1，但不强制范围。
- `block`：可选 string；普通方块 id，或 `#namespace:tag` 表示方块标签。
- `fluid`：可选 string；流体 id。
- **`block` 与 `fluid` 恰好一个**（`CODEC.validate` 校验，违反 = 数据加载错误）。
- `below`/`result_blocks` 同时接受两种 JSON 形态：纯数组 `[ ... ]`，或对象 `{"positions": [ ... ]}`
  （向后兼容旧 terra-plate-extension 写法；序列化时总是输出纯数组）。
- `result_blocks` 里 **标签形式会被忽略**（不能放置 tag），流体用 `defaultFluidState().createLegacyBlock()` 放置。

KubeJS 脚本（schema 已实现，脚本命名空间 = `kubejs_botania_neo`，路径 = `terra_plate`）：
```js
ServerEvents.recipes(event => {
    event.recipes.kubejs_botania_neo.terra_plate({
        ingredients: ['minecraft:diamond', 'minecraft:diamond', 'minecraft:emerald'],
        mana: 50000,
        output: { item: 'minecraft:netherite_ingot', count: 1 },
        below: { positions: [
            { x: -1, z: -1, block: 'minecraft:stone' },
            { x:  0, z:  0, fluid: 'minecraft:water' }
        ] },
        result_blocks: [
            { x: 0, z: 0, block: 'minecraft:gold_block' }
        ]
    });
});
```
- `output` 兼容两种写法：`{ item: 'id', count: n }`（KubeJS 页面风格）与标准 ItemStack（`{ id: 'id', count: n }`）。
- 不写 `below` = 无自定义图案 → 该配方只能走**原版**多方块平台（见 §5.8 风险 R1）。

### 5.4 匹配/合成流程（最终实现）

`TerraPlateExtensionEngine`（不依赖 Mixin，便于单测/复用）：

| 成员 | 签名 | 作用 |
|---|---|---|
| `Match` | `record Match(TerraPlateExtensionRecipe recipe, int rotation)` | findMatch 结果 |
| `ActiveMatch` | `record ActiveMatch(TerraPlateExtensionRecipe recipe, int rotation, BlockPos platePos)` | ThreadLocal 上下文 |
| `ACTIVE` | `private static final ThreadLocal<ActiveMatch>` | 当前线程正在校验的板上下文 |
| `getInputItemEntities` | `static List<ItemEntity> getInputItemEntities(Level, BlockPos platePos)` | `level.getEntitiesOfClass(ItemEntity.class, new AABB(platePos), EntitySelector.ENTITY_STILL_ALIVE)`（与 Botania 私有 `getItemEntities` 完全一致） |
| `findMatch` | `static @Nullable Match findMatch(Level, BlockPos platePos)` | 物品非空 → `RecipeUtils.getInputFromEntities` → 遍历 `getAllRecipesFor(TERRA_PLATE_TYPE)` 中 `instanceof TerraPlateExtensionRecipe` 的配方；`hasCustomPattern()` 时 `matchRotation`（4 向），否则 rotation=0；再 `recipe.matchesInput(input)`。⚠ 当前实现对 `hasCustomPattern()==false` 的配方也返回 Match（rotation=0，不校验平台）→ 见 §5.8 R1 的建议修复 |
| `matchRotation` | `static int matchRotation(Level, BlockPos, List<TerraPlatePosition>)` | 0..3 依次试，返回首个命中或 -1 |
| `matchesPattern` | `static boolean matchesPattern(Level, BlockPos, List<TerraPlatePosition>, int rotation)` | 逐格 `TerraPlatePosition.matches` |
| `applyResultBlocks` | `static void applyResultBlocks(Level, BlockPos, TerraPlateExtensionRecipe, int rotation)` | 逐格 `TerraPlatePosition.apply` |
| `setActive` / `clearActive` / `getActive` / `isActive` | 静态 | 维护/查询 ThreadLocal |

`TerraPlateExtensionRecipe`：
- `matchesInput(ProcessingRecipeInput)`：纯物品匹配（`RecipeUtils.matches(this, input, requireSpecialMatching)`），
  不依赖上下文，供平台校验使用（避免自锁）。
- `matches(ProcessingRecipeInput, Level)`：若 `hasCustomPattern()` 且 `!TerraPlateExtensionEngine.isActive(this)` → **false**；
  否则 `matchesInput`。→ 无上下文时（客户端/JEI/其它 mod 直接调用）扩展配方不会误匹配；普通平台上的物品可能匹配
  但不会选中自定义配方（除非上下文激活）。
- `assemble`：`output.copy()`；`getResultItem`：`output`；`getIngredients`：ingredients；`getSerializer`：`SERIALIZER`；
  **不覆盖 `getType()`**（关键）。
- 构造时 `requireSpecialMatching = ingredients 中任一 requiresCustomTesting()`（455 用 `XplatAbstractions.INSTANCE`）。

### 5.5 Mixin 注入设计（精确描述符 + handler 签名）

Mixin 类：`dev.kubejsbotania.neo.mixin.TerrestrialAgglomerationPlateBlockEntityMixin`
（`@Mixin(TerrestrialAgglomerationPlateBlockEntity.class)`；`mixins.json` 的 `package` 必须与之一致，见 §5.8 R2）

当前实现只有 **2 个 `@Inject`**，无需 `@Redirect` / `@Accessor` / `@Invoker` / `@Shadow`。

#### 5.5.1 核心时序问题：`hasValidPlatform` 拿不到物品，但配方选择需要“图案 + 物品”

`hasValidPlatform()` 的签名是 `()Z`，只能看世界；而 `getRecipeFor(input)` 只能看物品。Botania 的调用顺序是
`hasValidPlatform → getItemEntities → getRecipeFor`（`tryStartProcessing` 与 `serverCollectingTick` 两处均如此，
455 字节码逐条确认）。因此：

- 若 HEAD **只校验图案**：图案命中就 `cancel → true`，随后物品可能是原版配方材料
  → 原版配方会在自定义图案上合成（越权泄漏）；
- 若 HEAD **不 cancel**：自定义图案上原版平台校验必然失败，扩展配方永远无法启动。

**最终解法**：HEAD 内同时完成“图案（4 向旋转）+ 物品实体”的完整匹配（`TerraPlateExtensionEngine.findMatch`），
命中才 `setReturnValue(true)`，并把「命中的 recipe + rotation + platePos」写入 ThreadLocal；
随后 `getRecipeFor` 触发的扩展 `recipe.matches()` 读取该上下文做二次确认。
这样既堵住泄漏，又给配方选择提供了 pos/rotation 上下文。

#### 注入 A：`hasValidPlatform()Z` HEAD（可取消）

```java
@Inject(method = "hasValidPlatform()Z", at = @At("HEAD"), cancellable = true)
private void kubejs_botania_neo$hasValidPlatform(CallbackInfoReturnable<Boolean> cir) {
    Level level = getLevel();
    if (level == null || level.isClientSide()) return;
    BlockPos pos = getBlockPos();
    TerraPlateExtensionEngine.Match match = TerraPlateExtensionEngine.findMatch(level, pos);
    if (match != null) {
        TerraPlateExtensionEngine.setActive(match.recipe(), match.rotation(), pos);
        cir.setReturnValue(true);          // 覆盖原版平台校验结果
    } else {
        TerraPlateExtensionEngine.clearActive();   // 让原版逻辑照常执行
    }
}
```
- 目标方法：`private boolean hasValidPlatform()`，描述符 **`()Z`**（455 `javap -s` 已确认）。
- handler 是**实例方法**（目标方法是实例方法）；通过继承的 `getLevel()`/`getBlockPos()` 访问上下文，
  不需要 `@Shadow`/`@Accessor`。
- 时序保证：`tryStartProcessing` 与 `serverCollectingTick` 的字节码顺序都是
  `hasValidPlatform()` → `getItemEntities()` → `cachedCheck.getRecipeFor(...)`，
  因此 HEAD 里写入的 ThreadLocal 一定早于 `matches()` 被调用。
- 「hasValidPlatform 与物品无关」问题的最终解法：**在 HEAD 里同时校验图案与物品实体**
  （`findMatch` 内部先 `RecipeUtils.getInputFromEntities` 再逐个 recipe `matchesInput`），
  命中才返回 true。这样：
  a) 为随后的 `getRecipeFor` 提供「已命中的配方 + rotation」上下文；
  b) 阻止“自定义平台上用原版 terrasteel 材料合成原版配方”的越权路径（图案命中但配方不命中 → 不 cancel → 原版校验失败）；
  c) 图案命中且物品命中多个扩展配方时，`findMatch` 取 `getAllRecipesFor` 迭代序第一个
     （配方管理器顺序；如需优先级可后续扩展）。

#### 注入 B：`finishCrafting` TAIL

```java
@Inject(
    method = "finishCrafting(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
           + "Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/List;"
           + "Lvazkii/botania/api/recipe/ProcessingRecipeInput;"
           + "Lnet/minecraft/world/item/crafting/RecipeHolder;)V",
    at = @At("TAIL")
)
private void kubejs_botania_neo$finishCrafting(Level level, BlockPos pos, BlockState state,
        List<ItemEntity> itemEntities, ProcessingRecipeInput recipeInput,
        RecipeHolder<TerrestrialAgglomerationRecipe> recipeHolder, CallbackInfo ci) {
    TerraPlateExtensionEngine.ActiveMatch active = TerraPlateExtensionEngine.getActive();
    if (active != null && recipeHolder.value() == active.recipe()) {
        TerraPlateExtensionEngine.applyResultBlocks(level, pos, active.recipe(), active.rotation());
    }
    TerraPlateExtensionEngine.clearActive();
}
```
- 目标方法：`private void finishCrafting(Level, BlockPos, BlockState, List, ProcessingRecipeInput, RecipeHolder)`，
  描述符：
  `(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/List;Lvazkii/botania/api/recipe/ProcessingRecipeInput;Lnet/minecraft/world/item/crafting/RecipeHolder;)V`
- handler 参数 = 目标方法形参（原样接收）+ `CallbackInfo`；`recipeHolder` 是**形参**，
  不需要 `@LocalCapture`/`Local`；handler 为实例方法。
- TAIL 时点（已确认）：结果物品已生成、输入 ItemEntity 已 discard、音效已播、`mana=0`/`manaToGet=0`、
  `blockEvent(1)` 已发、blockstate 已设为 `DONE`、`scheduleTick(4)` 已调度。**方块转化发生在产物生成之后**，
  不会影响产物生成/玩家触发；`DONE → IDLE` 的 4 tick 延迟也在转化之后，不会覆盖转化结果。
- 只对“本次激活的同一个扩展配方”执行转化（`== active.recipe()`，同一 RecipeHolder.value 实例）；
  原版配方/其它扩展配方不触发。

#### 5.5.2 备用注入点（当前不需要，未来若需要可用）

| 需求 | 目标成员 | 描述符 |
|---|---|---|
| 取板内物品实体 | `private List<ItemEntity> getItemEntities()` | `()Ljava/util/List;` |
| 重置收集状态 | `private void stopCollecting()` | `()V` |
| 读取/修改魔力 | `private int mana` / `private int manaToGet` / `private int currentProgress` / `private long lastProgressTick` | `I` / `I` / `I` / `J`（`@Shadow`/`@Accessor`） |
| 取/换 recipe 缓存 | `private final RecipeManager.CachedCheck<...> cachedCheck` | `Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;` |
| 完全接管 tick | `public static void serverCollectingTick(Level,BlockPos,BlockState,BE)` | `(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lvazkii/botania/common/block/block_entity/TerrestrialAgglomerationPlateBlockEntity;)V` |
| 强制指定配方 | `RecipeManager$CachedCheck#getRecipeFor` 两处调用点 | `@Redirect` 目标 `Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;` |

#### 5.5.3 备选注入方案评估（为什么不用另外两种）

| 方案 | 做法 | 优点 | 缺点 / 风险 | 结论 |
|---|---|---|---|---|
| **A. `@Redirect CachedCheck#getRecipeFor`（2 处调用点）+ `hasValidPlatform` HEAD + `finishCrafting` TAIL** | Redirect 目标：`Lnet/minecraft/world/item/crafting/RecipeManager$CachedCheck;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;`；handler 里自己查物品+图案选配方 | 不依赖 `matches()` 上的上下文 | ① Redirect handler 只有 receiver + 实参，**拿不到 `pos`**，仍要额外 ThreadLocal/注入传 pos；② 要在 handler 里重新实现“物品→配方”选择（重复 Botania 逻辑）；③ `invokeinterface` redirect 与其它 mod 同时修改该调用点时冲突面更大 | 不采用 |
| **B. `@Inject HEAD serverCollectingTick` 完全接管** | cancel 原方法，自己写收集/进度/完成 | 完全可控 | 需重复实现约 30 行原版逻辑：Spark 魔力转移注册、进度 blockEvent 节流、火花检查、产物生成、玩家 trigger、产物 ItemSource 标记、音效/状态切换；还要 `@Invoker finishCrafting/stopCollecting`；Botania 更新即碎 | 不采用 |
| **C. 最终采用：`hasValidPlatform` HEAD + `finishCrafting` TAIL + 扩展 `matches()` ThreadLocal 门控（配合自定义 serializer 让 recipe 进入 TERRA_PLATE_TYPE）** | 见 §5.5.1 与注入 A/B | 只有 2 个 `@Inject`；零 `@Shadow/@Accessor/@Invoker`；完全复用 Botania 的火花/进度/产物/音效流程；Engine 与 Mixin 解耦、可单测 | 依赖“1.21.1 CachedCheck 每次调用都会调用 `matches`”这一语义（已用 `RecipeManager$1` 字节码确认）；扩展配方在无上下文时 `matches=false`，JEI 需自建分类（工程已实现；注意与 Botania 自带分类重复，见 §5.8 R11） | **最终采用** |

### 5.6 坐标系与旋转语义（用户 / KubeJS 脚本约定）

- 原点：**板方块位置 `platePos`**；图案所有 `(x,z)` 都映射到 **板下一层**：
  `worldPos = platePos.offset(rotatedX, -1, rotatedZ)`。
- 旋转（顺时针 90° 递增，与 `net.minecraft.core.BlockPos#rotate(Rotation)` 一致，已用 1.21.1 字节码确认）：

| `rotation` | `Rotation` | `(x,z) → (rx,rz)` | 世界位置 |
|---|---|---|---|
| 0 | `NONE` | `(x, z)` | `platePos.offset(x, -1, z)` |
| 1 | `CLOCKWISE_90` | `(-z, x)` | `platePos.offset(-z, -1, x)` |
| 2 | `CLOCKWISE_180` | `(-x, -z)` | `platePos.offset(-x, -1, -z)` |
| 3 | `COUNTERCLOCKWISE_90` | `(z, -x)` | `platePos.offset(z, -1, -x)` |

- 与 §4.5 的原版 MULTIBLOCK 完全对齐：Patchouli 锚点 = `platePos.below()`，基座层在该锚点所在 y；
  `TerraPlatePosition.rotatedPos` 里的 `-1` 就是把“相对板的偏移”换算到“锚点层”。
  即：`below` 的 `x/z` 就是**基座层以锚点为中心的局部坐标**，`(0,0)` = `platePos.below()` = 原版 `'0'` 所在格。
- 自动旋转匹配：`matchRotation` 从 0 到 3 依次试，取**第一个完全匹配**的 rotation；
  `result_blocks` 使用**同一个 rotation** 计算物理位置。因此脚本作者按照 rotation=0 书写图案，
  玩家建造时把整个图案旋转 90/180/270 度也能匹配（产物/转化位置随之旋转）。
- 方块/流体判定：
  - `block` = 普通 id → `level.getBlockState(pos).is(block)`；
  - `block` = `#tag` → `state.is(TagKey.create(Registries.BLOCK, id))`；
  - `fluid` → `level.getFluidState(pos).getType() == BuiltInRegistries.FLUID.get(id)`。
- `result_blocks` 应用：方块 → `level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL)`；
  流体 → `level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL)`；
  `#tag` 条目直接跳过（无法放置 tag）。

### 5.7 性能与耗时结论（用户问的 b 点）

- **`hasValidPlatform` 每 tick 都会被调用吗？** 分状态：
  - `IDLE`/`DONE`：没有 BE ticker，`hasValidPlatform` 只在 `entityInside` 触发一次 `scheduleTick` 后的
    `Block.tick → tryStartProcessing` 里调用（每次投物品/调度一次，不是每 tick）。
  - `COLLECTING`：`serverCollectingTick` **每 tick（20 次/秒）** 调用一次 `hasValidPlatform`。
  - 另外 `tryStartProcessing`（每次 scheduled tick）也调用一次。
- 每次调用 `findMatch` 的开销：`getEntitiesOfClass`（AABB 内物品实体，通常 0..16 个）+
  `getAllRecipesFor(TERRA_PLATE_TYPE)` 遍历（Botania 自身 1 条 terrasteel + 我们的扩展配方 N 条；N 很小）+
  每条扩展配方最多 4×|below| 次 `getBlockState`/`getFluidState` 读取（|below| ≤ 9）。
  → 单板 COLLECTING 时每秒约 `20 × (4×9) = 720` 次方块状态读取的量级，可忽略；
  多个并行板线性增长。**结论：无需缓存/优化**，且不应为此引入额外数据结构（配方改动后缓存失效难处理）。
- 配方查找：Botania 的 `cachedCheck` 会先尝试 `lastRecipe`（`byKeyTyped` + `matches`），命中即返回；
  我们 `matches` 只做 ThreadLocal 引用比较 + `RecipeUtils.matches`（`StackedContents.canCraft`），
  与 Botania 原版 terrasteel 匹配成本同量级。

### 5.8 风险 / 边界 / 必测清单

| # | 风险/边界 | 说明与建议 |
|---|---|---|
| R1 | **无 `below` 的扩展配方当前会绕过原版平台校验** | 当前 `findMatch` 对 `hasCustomPattern()==false` 也返回 Match（rotation=0，只查物品），`hasValidPlatform` 因此直接返回 true → 没有基座也能合成。**建议**：`findMatch` 只对 `recipe.hasCustomPattern()` 返回 Match；无图案的扩展配方交给原版 `hasValidPlatform`（真平台）+ 其 item-only `matches` 处理。修改后再测“无图案配方只能原版平台” |
| R2 | **mixins.json 的 `package` 必须与 mixin 类包名一致** | 已对齐：配置 `"package": "dev.kubejsbotania.neo.mixin"`，类 `dev.kubejsbotania.neo.mixin.TerrestrialAgglomerationPlateBlockEntityMixin`。以后移动/重命名 mixin 类时必须同步该配置与 `mixins` 列表；`injectors.defaultRequire=1` 保证注入失败会硬报错 |
| R3 | 多个扩展配方图案/物品都匹配 | `findMatch` 返回 `getAllRecipesFor` 迭代序第一条；`RecipeManager` 的 `byType` 顺序来自 JSON 文件解析顺序（HashMap 迭代，非确定）。**建议**扩展配方使用互不重叠的 ingredients；或后续在 Engine 里定义显式优先级 |
| R4 | 原版配方与扩展配方输入物品相同 | 首次查找（lastRecipe 为空）遍历 `byType`，可能先命中原版配方。产物/行为以先命中者为准；同样建议 ingredients 唯一化 |
| R5 | 图案在 COLLECTING 中被破坏/物品被拿走 | 下一 tick `hasValidPlatform` 返回 false → `stopCollecting` → `mana>0` 时进入 `DISSIPATING`，每 tick 扣 1000 魔力直到 0；与 Botania 原版行为一致，无需处理 |
| R6 | 图案破坏者同时也是合成者 | 与原版一致：mana 全额消耗后才 finishCrafting；中途失效会 dissipate |
| R7 | ThreadLocal 泄漏/串板 | 只在服务端 tick 线程使用；`hasValidPlatform` 未命中时 `clearActive()`；`finishCrafting` TAIL 末尾 `clearActive()`。多板并发在同一线程顺序执行，每次 hasValidPlatform 都会重设上下文。仍需注意：其它 mod 若在 `matches()` 调用栈内再触发别的板逻辑属极端情况，可忽略 |
| R8 | `result_blocks` 改掉板下方图案 | 允许；转化发生在 DONE 之后，4 tick 后回 IDLE。下次使用时按新图案重新匹配（若配方要求被改掉的方块，则可能不再匹配） |
| R9 | `result_blocks` 的 `fluid` 放置 | 用 `createLegacyBlock()`（水源/流动方块按流体默认状态），不做 waterlogged 兼容处理 |
| R10 | 扩展配方 JSON 的 `type` 写错 | 写成 `botania:terrestrial_agglomeration_plate` 会**静默丢失** `below`/`result_blocks`（Botania codec 不读这些字段），配方退化成原版配方。KubeJS schema 已固定写 `kubejs_botania_neo:terra_plate` |
| R11 | 无上下文时 `matches` 返回 false；配方查看器重复显示 | 扩展配方在 JEI/EMI 的“可合成”判定、其它 mod 直接 `RecipeManager.getRecipeFor(TERRA_PLATE_TYPE, ...)` 时不会命中（图案无法在无 pos 条件下判定）→ 这是设计使然。**但 Botania 自带的 JEI/REI/EMI 分类按 `getAllRecipesFor(TERRA_PLATE_TYPE)` 遍历**（如 `JEIBotaniaPlugin.java:139`），所以扩展配方会**自动出现在 Botania 自己的泰拉板分类里**；工程自建的 `TerraPlateExtensionJeiPlugin` 可能与它重复，建议二选一 |
| R12 | 输入物品识别缓存 | 见 §5.9；扩展配方 `getIngredients()` 必须返回完整 ingredient 列表，且不要用 NeoForge 自定义 `ICustomIngredient` |
| R13 | `hasValidPlatform` 注入方法名是否需带描述符 | 当前写 `"hasValidPlatform"` 可解析（唯一名字）；**推荐**写成 `"hasValidPlatform()Z"` 以防 Botania 未来重载。`injectors.defaultRequire=1` 保证注入失败会报错而不是静默 |
| R14 | 强制校验注入生效 | 编译/启动后打日志或断点确认 `hasValidPlatform` HEAD 被执行；Mixin 配置见 §5.8 R2 |

**必测清单**
1. 原版 terrasteel 配方在原生平台仍正常（无 mixin 副作用）；
2. 扩展配方：4 个 rotation 各建一次图案都能开始收魔力；
3. 扩展配方合成成功：产物、音效、blockstate DONE→IDLE、`result_blocks` 按 rotation 正确替换；
4. 收集中破坏一格图案 → 进入 DISSIPATING 并最终清空魔力；
5. 在扩展图案上放原版 terrasteel 材料 → 不应合成（R1 修复后应无法启动）；
6. 无 `below` 的扩展配方 → 只在原版平台可合成（R1 修复后）；
7. KubeJS `/reload` 后新增/修改扩展配方→ `BotaniaRecipeIngredientsCache` 重新懒构建、新物品可放上板；
8. 客户端：进度粒子/完成特效正常（不依赖我们的注入）。

### 5.9 `BotaniaRecipeIngredientsCache` 关键实现（决定是否需要 mixin）

源码 `Xplat/src/main/java/vazkii/botania/common/handler/BotaniaRecipeIngredientsCache.java`（455 字节码一致）：

```java
public static boolean isTerraPlateInputItem(Level level, Item item) {
    return getInstance(level).isTerraPlateInputItemInternal(level, item);
}

private boolean isTerraPlateInputItemInternal(Level level, Item item) {
    if (terraPlateInputItemIds.isEmpty()) {
        // TODO: terra plate allows special ingredients, for which this caching approach is incorrect
        level.getRecipeManager().getAllRecipesFor(BotaniaRecipeTypes.TERRA_PLATE_TYPE).stream()
            .flatMap(holder -> holder.value().getIngredients().stream())
            .flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
            .map(ItemStack::getItem)
            .mapToInt(BuiltInRegistries.ITEM::getId)
            .forEach(terraPlateInputItemIds::add);
    }
    return terraPlateInputItemIds.contains(BuiltInRegistries.ITEM.getId(item));
}
```

- 懒构建：第一次调用时从 **`getAllRecipesFor(TERRA_PLATE_TYPE)` 的所有 ingredients** 收集 item registry id；
  之后只查 `IntSet`。
- 刷新：实现 `ResourceManagerReloadListener`，`onResourceManagerReload` → `clearCache()`（清空，不立即重建）；
  `registerListener()` 由 `NeoForgeCommonInitializer:171` 在 mod 构造期注册到 `PackType.SERVER_DATA`。
- **对我们的结论**：扩展配方被索引到 `TERRA_PLATE_TYPE` 后自动进入该缓存 → **不需要 mixin 这个类**。
  右键 `useItemOn` 与 `entityInside` 调度 tick 都会放行扩展配方的 ingredient 物品。
- 注意事项：
  - 缓存构建依赖 `getIngredients()` 完整（非空、与 matches 的 ingredient 列表一致）；
  - `Ingredient#getItems()` 对 NeoForge 自定义 ICustomIngredient 可能为空 → 这类物品不会被识别（源码 TODO 也承认），
    所以扩展配方请用普通 item/tag ingredient；
  - KubeJS 在 `RecipeManager.apply` HEAD 阶段往 JSON map 里增删配方，随后才解析；
    缓存是懒构建（玩家交互时），因此 `/reload` 后新增的扩展配方物品能被识别；
  - 该缓存是 `SERVER_INSTANCE`/`CLIENT_INSTANCE` 两个单例（`Suppliers.memoize`），客户端侧由
    `clearClientCache()` 清空（由加载器在客户端资源重载时调用）。

---

## 6. 泰拉板扩展与原版流程的契约总结（给实现者）

1. 扩展配方**必须** `implements vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe`，且**不覆盖 `getType()`**；
   `getSerializer()` 返回 `kubejs_botania_neo:terra_plate` 对应的 serializer。
2. `getIngredients()` 返回完整 ingredient 列表（用于物品缓存 + `RecipeUtils.matches`）。
3. `matches()` 必须：无激活上下文时返回 false（若 `hasCustomPattern()`）；激活时用 `matchesInput`。
4. `getMana()` 返回自定义魔力；`assemble`/`getResultItem` 返回 output 副本。
5. 平台校验/方块转化只通过 2 个 `@Inject`（§5.5）；不要 `@Redirect` cachedCheck，也不要 mixin
   `BotaniaRecipeIngredientsCache`。
6. `below`/`result_blocks` 的 `(x,z)` 语义 = 板下一层、以 `platePos.below()` 为中心的局部坐标（§5.6）。
7. 服务端/客户端 serializer 都要注册（`DeferredRegister<RecipeSerializer<?>>` + `streamCodec`），否则客户端同步会崩。

---

## 7. Botania 公共 API 清单（KubeJS bindings / brew schema 用）

> 以下均为 **455 jar** 的实际类与方法（`javap` 验证），与 457 源码有重命名差异时以 455 为准。

### 7.1 `vazkii.botania.api.BotaniaAPI`（interface，单例服务加载）

- 常量：`MODID = "botania"`、`GOG_MODID`、`LOGGER`、`INSTANCE`（`public static final BotaniaAPI`）、
  `DUMMY_ARMOR_MATERIAL`、`DUMMY_ITEM_TIER`。
- 静态：`static BotaniaAPI instance()`、`static ResourceLocation botaniaRL(String)`、`gogRL(String)`。
- 默认方法（节选，均 455 存在）：
  - `int apiVersion()`
  - **`DefaultedRegistry<Brew> getBrewRegistry()`**
  - `DefaultedRegistry<IslandType> getIslandTypeRegistry()`
  - `Map<ResourceLocation, Function<DyeColor, Block>> getPaintableBlocks()` / `registerPaintableBlock(...)`
  - `Holder<ArmorMaterial> getManasteelArmorMaterial()` / `getElementiumArmorMaterial()` / `getManaweaveArmorMaterial()` / `getTerrasteelArmorMaterial()`
  - `Tier getManasteelItemTier()` / `getElementiumItemTier()` / `getTerrasteelItemTier()`
  - `ManaNetwork getManaNetworkInstance()`
  - `Container getAccessoriesInventory(Player)`
  - `void breakOnAllCursors(Player, ItemStack, BlockPos, Direction)`
  - `boolean hasSolegnoliaAround(Entity)`
  - `void sparkleFX(Level, double x,double y,double z, float r,float g,float b,float size,int age)`
  - `void registerCorporeaNodeDetector(CorporeaNodeDetector)`
  - `ConfigDataManager getConfigData()` / `setConfigData(...)`
  - `findBlockApi(BlockApiNoContext, Level, BlockPos, BlockState, BlockEntity)` / `findBlockApi(BlockApiWithContext, ..., C)`
  - `findItemApi(ItemApiNoContext, ItemStack)` / `findItemApi(..., C)` / `findEntityApi(...)`
  - `getEntityData(DataIdBase<T>, Entity)` / `setEntityData(...)` / `removeEntityData(...)`
- 实现类：`vazkii.botania.common.impl.BotaniaAPIImpl`（`META-INF/services/vazkii.botania.api.BotaniaAPI`）。
- KubeJS bindings 建议暴露：`BotaniaAPI.instance()` 本身（方法均可调用）+ `botaniaRL`。

### 7.2 `vazkii.botania.api.BotaniaRegistries`（注册表 key）

```java
public class BotaniaRegistries {
    public static final ResourceKey<Registry<Brew>> BREWS = ResourceKey.createRegistryKey(botaniaRL("brews"));
    public static final ResourceKey<Registry<IslandType>> ISLAND_TYPES = ...("island_types");
    public static final ResourceKey<Registry<StateIngredientType<?>>> STATE_INGREDIENT_TYPE = ...("state_ingredient_type");
    public static final ResourceKey<Registry<ItemSource>> ITEM_SOURCE = ...("item_source");
    public static final ResourceKey<CreativeModeTab> BOTANIA_TAB_KEY = ...;
}
```
- 1.21.1 NeoForge 用 `NewRegistryEvent` + `RegistryBuilder` 创建：`BREWS`/`ISLAND_TYPES`/`STATE_INGREDIENT_TYPE`
  带 `.defaultKey(...)`、`.sync(true)`（`NeoForgeRegistryHandler:27-32`）。
- 取值：`RegistryHelper.getRegistry(BotaniaRegistries.BREWS)`（内部）或
  `BotaniaAPI.instance().getBrewRegistry()`（公开），也可 `server.registryAccess().registryOrThrow(BotaniaRegistries.BREWS)`。

### 7.3 Brew 系统（`brew` 配方 & schema 需要）

- `vazkii.botania.api.brew.Brew`（class，非 record）：
  - `static final ResourceLocation DEFAULT_ID = botania:fallback`
  - 构造：`Brew(int color, int cost, MobEffectInstance... effects)`、
    `Brew(int cost, Supplier<List<MobEffectInstance>> effects)`
  - `Brew setNotBloodPendantInfusable()` / `setNotIncenseInfusable()`（链式）
  - `boolean canInfuseBloodPendant()` / `canInfuseIncense()`
  - `String getTranslationKey()` / `getTranslationKey(ItemStack)`
  - `int getColor(ItemStack)` / `int getManaCost()` / `int getManaCost(ItemStack)`
  - `List<MobEffectInstance> getPotionEffects(ItemStack)`
- `vazkii.botania.api.brew.BrewContainer`（interface，物品实现它才可装 brew）：
  - `ItemStack getItemForBrew(Brew, ItemStack)`
  - `int getManaCost(Brew, ItemStack)`
- `vazkii.botania.api.brew.BrewItem`：`Brew getBrew(ItemStack)`。
- **brew 注册表 = 数据包注册表 `botania:brews`**，但 **不是 JSON 数据包**：455 jar 的 `data/botania/` 下没有
  `brews/` 目录（已核对所有顶层目录：advancement、banner_pattern、config、curios、damage_type、function、
  gametest、jukebox_song、loot_modifiers、loot_table、neoforge、patchouli_books、recipe、scripts、structure、
  tags、worldgen）。Brew 没有 `MapCodec`，**只能由 Java 通过 NeoForge `RegisterEvent`（注册表 key `BotaniaRegistries.BREWS`）
  注册**，如 Botania 自己在 `NeoForgeCommonInitializer:219`
  `bind(event, BotaniaRegistries.BREWS, BotaniaBrews::submitRegistrations)`。
- Botania 自带 21 个 brew：`botania:fallback` + `absolution, adrenaline, cloaking, crimson_shade, crossed_souls,
  feather_feet, fleetfeet, fortitude, gills, magmaskin, marine_allure, mending, overload, owlsight, restoration,
  revitalization, shielding, upsurging, vanitys_emptiness, vigor`（`BotaniaBrews` 字段）。
- **对 KubeJS brew schema 的结论**：`brew` 字段只能引用**已注册**的 brew id（如 `botania:absolution`）；
  KubeJS 无法通过数据包新增 brew（无 codec/JSON 加载路径）。若将来要支持脚本自定义 brew，只能由本 mod
  在 Java 侧暴露注册 API（例如 `event.registerBrew(...)` 之类的自定义事件），不属于本次移植范围。

### 7.4 `vazkii.botania.api.mana.ManaItemHandler`

- interface，`public static final ManaItemHandler INSTANCE` + `static ManaItemHandler instance()`（服务加载）。
- 默认方法：
  - `List<ItemStack> getManaItems(Player)`
  - `List<ItemStack> getManaAccesories(Player)`（注意拼写 Accesories）
  - `int requestMana(ItemStack, Player, int mana, boolean remove)`
  - `boolean requestManaExact(ItemStack, Player, int mana, boolean remove)`
  - `int dispatchMana(ItemStack, Player, int mana, boolean remove)`
  - `boolean dispatchManaExact(ItemStack, Player, int mana, boolean remove)`
  - `int requestManaForTool(ItemStack, Player, int, boolean)`
  - `boolean requestManaExactForTool(ItemStack, Player, int, boolean)`
  - `int getInvocationCountForTool(ItemStack, Player, int)`
  - `float getFullDiscountForTools(Player, ItemStack)`
  - `boolean hasProficiency(Player, ItemStack)`
- 实现：`vazkii.botania.common.impl.mana.ManaItemHandlerImpl`（`META-INF/services/vazkii.botania.api.mana.ManaItemHandler`）。

### 7.5 `vazkii.botania.api.corporea.CorporeaHelper`

- interface，`public static final CorporeaHelper INSTANCE` + `static CorporeaHelper instance()`（服务加载）。
- 默认方法：
  - `Set<CorporeaNode> getNodesOnNetwork(CorporeaSpark)`
  - `CorporeaRequestMatcher createMatcher(ItemStack, boolean)`
  - `CorporeaRequestMatcher createMatcher(String)`
  - `CorporeaResult requestItem(CorporeaRequestMatcher, int, CorporeaSpark, LivingEntity, boolean)`
  - `CorporeaSpark getSparkForBlock(Level, BlockPos)`
  - `boolean doesBlockHaveSpark(Level, BlockPos)`
  - `int signalStrengthForRequestSize(int)`
  - `<T extends CorporeaRequestMatcher> void registerRequestMatcher(ResourceLocation, Class<T>, BiFunction<CompoundTag, HolderLookup.Provider, T>)`
- 实现：`vazkii.botania.common.impl.corporea.CorporeaHelperImpl`（`META-INF/services/vazkii.botania.api.corporea.CorporeaHelper`）。

### 7.6 其他对配方/泰拉板扩展有用的类

| 类 | 用途 |
|---|---|
| `vazkii.botania.common.crafting.BotaniaRecipeTypes` | `TERRA_PLATE_TYPE` 等 10 个 RecipeType 的 public static 字段 + `submitRecipeTypes/submitRecipeSerializers` |
| `vazkii.botania.common.crafting.recipe.RecipeUtils` | `matches(Recipe, ProcessingRecipeInput, boolean)`、`getInputFromEntities(List<ItemEntity>)`、`getInputFromListWithoutUnstacking`、`getInputFromContainer`、`wrapAllIngredients`、`wrapCatalysts`、`getBrewContainerIngredient` |
| `vazkii.botania.api.recipe.ProcessingRecipeInput` | 泰拉板/祭坛等“处理配方”的输入（`getItems()`、`getStackedContents()`、`getSubset`） |
| `vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe` | 扩展配方必须实现的接口；默认 `getType()`、`getRemainingItems`（恒返回空列表=消耗所有输入）、`getToastSymbol` |
| `vazkii.botania.api.state.BotaniaStateProperties` | `TERRA_PLATE_STATE = EnumProperty.create("state", TerraPlateState.class)` |
| `vazkii.botania.api.state.enums.TerraPlateState` | IDLE/COLLECTING/DISSIPATING/DONE |
| `vazkii.botania.common.lib.BotaniaTags` | `Blocks.TERRA_PLATE_BASE = tag("terra_plate_base")` 等 |
| `vazkii.botania.api.internal.ItemSource` + `common.internal_caps.ItemSources` | `ItemSources.TERRA_PLATE`：产物物品来源标记（防止产物立即再次触发合成） |
| `vazkii.botania.common.handler.BotaniaSounds` | `TERRA_PLATE_CRAFT`（id `botania:terra_plate_craft`） |
| `vazkii.botania.common.handler.BotaniaRecipeIngredientsCache` | `isTerraPlateInputItem(Level, Item)` 等（§5.9） |
| `vazkii.botania.xplat.XplatAbstractions` | 455：`static final INSTANCE`；`requiresCustomTesting(Ingredient)` 等 |

### 7.7 KubeJS 2101 对接要点（与 task-2 文档 `docs/kubejs-2101-api-notes.md` 对齐）

- KubeJS 侧注册入口：`KubeJSPlugin#registerRecipeComponents` / `registerRecipeSchemas(RecipeSchemaRegistry)` /
  `registerBindings(BindingRegistry)`；插件清单文件 `kubejs.plugins.txt`（KubeJS 2101 不用 ServiceLoader）。
- Botania 原版类型的脚本命名空间必须是 `botania`、路径必须是真实 serializer id
  （`terrestrial_agglomeration_plate`；`terra_plate` 只是 `typeOverride` 别名）。
- 本 mod 扩展配方：命名空间 `kubejs_botania_neo`、路径 `terra_plate`，JSON type 固定
  `kubejs_botania_neo:terra_plate`（与 serializer 注册 id 一致）。
- KubeJS 在 `RecipeManagerMixin` 的 `apply(Map<ResourceLocation,JsonElement>,...)` HEAD 阶段修改原始 JSON map，
  → 写入的 JSON 会走 vanilla `Recipe.CODEC` dispatch → 我们的 serializer 被使用；
  之后 `RecipeManager` 按 `recipe.getType()` 建索引 → 落到 Botania `TERRA_PLATE_TYPE`。
- KubeJS 脚本里 `below`/`result_blocks` 用 `{x,z,block|fluid}` 组件（`TerraPlatePositionComponent`，
  component type = `kubejs_botania_neo:terra_plate_positions` 等，见工程代码）。

---

## 8. 完整配方 type 速查表（最终版）

| JSON `type` | RecipeType | 字段（必填 *） | 输出字段 | 额外 |
|---|---|---|---|---|
| `botania:mana_infusion` | MANA_INFUSION | `input*`, `mana*`, `output*`, `group?`, `catalyst?` | `output` | mana 1..1000001 |
| `botania:pure_daisy` | PURE_DAISY | `input*`, `output*`, `time?=0`, `copy_properties?=false`, `pre_update_function?`, `success_function?` | `output` | time 实际写 150 |
| `botania:elven_trade` | ELVEN_TRADE | `ingredients*`(非空), `output*`(非空数组) | `output` 数组 | |
| `botania:elven_trade_lexicon` | ELVEN_TRADE | （无字段） | 固化：精灵词典 | 单例 |
| `botania:runic_altar` | RUNIC_ALTAR | `ingredients*`, `catalysts*`(可 `[]`), `reagent*`, `mana*`, `output*` | `output` | ingredients+catalysts 1..16 |
| `botania:runic_altar_head` | RUNIC_ALTAR | 同 `runic_altar` | `output`（会写 profile 组件） | 需命名牌/成书 |
| `botania:brew` | BREW | `brew*`, `ingredients*`(非空) | 由 brew + 容器决定 | mana 来自 Brew |
| `botania:petal_apothecary` | PETAL_APOTHECARY | `output*`, `reagent*`, `ingredients*`(1..16) | `output` | |
| `botania:terrestrial_agglomeration_plate` | TERRA_PLATE | `ingredients*`(非空), `mana*`, `result*` | `result` | 见 §9 泰拉板 |
| `botania:orechid` | ORECHID | `input*`, `output*`, `mana*`, `cooldown?=0`, `weight?=0`, `biome_bonus_weight?=0`, `biome_bonus_tag?`, `pre_update_function?`, `success_function?` | `output` (StateIngredient) | weight+bonus 不能都为 0 |
| `botania:orechid_ignem` | ORECHID_IGNEM | 同 orechid | 同上 | |
| `botania:marimorphosis` | MARIMORPHOSIS | 同 orechid（`cooldown` 被忽略） | 同上 | |
| `kubejs_botania_neo:terra_plate`（本 mod） | **TERRA_PLATE**（继承） | `ingredients*`(非空), `output*`, `mana?=500000`, `below?=[]`, `result_blocks?=[]` | `output` | 见 §5 |

---

## 9. 附：泰拉板扩展涉及的文件/行号索引（源码，供 review）

| 内容 | 文件:行 |
|---|---|
| BE 类声明/接口 | `TerrestrialAgglomerationPlateBlockEntity.java:61` |
| MULTIBLOCK 定义 | 同上 `:62-79` |
| blockEvent 常量 | 同上 `:80-82` |
| 字段 mana/manaToGet/currentProgress/lastProgressTick/cachedCheck | 同上 `:84-91` |
| serverCollectingTick | 同上 `:97-130` |
| finishCrafting | 同上 `:132-156` |
| stopCollecting | 同上 `:158-165` |
| serverDissipatingTick | 同上 `:167-175` |
| clientCollectingTick | 同上 `:177-215` |
| triggerEvent | 同上 `:217-256` |
| tryStartProcessing | 同上 `:258-271` |
| getItemEntities / hasValidPlatform | 同上 `:286-292` |
| save/loadAdditional | 同上 `:294-304` |
| ManaReceiver/Spark 实现 | 同上 `:306-361` |
| Block.useItemOn | `TerrestrialAgglomerationPlateBlock.java:67-84` |
| Block.entityInside | 同上 `:91-101` |
| Block.tick | 同上 `:103-113` |
| Block.getTicker | 同上 `:125-138` |
| Block 模拟输出/粒子 | 同上 `:140-171` |
| 配方类型注册 | `NeoForgeCommonInitializer.java:209-210` |
| 物品缓存 listener 注册 | `NeoForgeCommonInitializer.java:171` |
| Brew 注册表创建 | `NeoForgeRegistryHandler.java:27-32` |
| Brew 内容注册 | `NeoForgeCommonInitializer.java:219` |
| 原版 terrasteel 配方 JSON | `data/botania/recipe/terrestrial_agglomeration_plate/terrasteel_ingot.json` |
| Patchouli DenseMultiblock 源码（验证用） | `vazkii.patchouli:common.multiblock.DenseMultiblock`（1.21.1-93-NEOFORGE sources） |
| 本 mod 实现 | `src/main/java/dev/kubejsbotania/neo/{recipe,mixin,kubejs}/...`（见 §5） |

---

## 10. 参考链接

- Patchouli 多方块文档：<https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/multiblocks/>
- Patchouli 1.21.1-93-NEOFORGE sources（DenseMultiblock/AbstractMultiblock 语义核实）：
  `https://maven.blamejared.com/vazkii/patchouli/Patchouli/1.21.1-93-NEOFORGE/`
- Botania 源码（只读参考）：`refs/botania-1.21.1/Botania-1.21.1-porting`
- Botania 455 运行 jar：`deps/botania-neoforge-1.21.1-455.jar`
- KubeJS 2101 源码：`refs/kubejs-2101/kubejs-2101`
- KubeJS 2101 API 笔记：`kubejs-botania-neo/docs/kubejs-2101-api-notes.md`
