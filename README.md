# KubeJS Botania Neo

适用于 **Minecraft 1.21.1 / NeoForge** 的 KubeJS 附属模组：把 Botania（植物魔法）的配方体系接入 KubeJS 脚本，并额外提供**可完全自定义下方图案的泰拉凝聚板扩展配方**与 JEI 展示。

> 本模组是 [Prunoideae/KubeJS-Botania](https://github.com/Prunoideae/KubeJS-Botania)（1.20.1，GPL-3.0）的 1.21.1 NeoForge 移植，并参考 MC百科 `terra-plate-extension` 的泰拉凝聚板扩展思路重新实现。协议见 [LICENSE](LICENSE)（GPL-3.0）。

> **仓库**：[yelingyou/Kubejs-botania-neo](https://github.com/yelingyou/Kubejs-botania-neo)  
> **下载**：[Releases](https://github.com/yelingyou/Kubejs-botania-neo/releases)（最新 `v0.4.0`）  
> **问题反馈**：[Issues](https://github.com/yelingyou/Kubejs-botania-neo/issues)

---

## 1. 版本要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.x（编译 21.1.248，Botania 要求 ≥21.1.229） | |
| KubeJS | 2101.7.x（开发/验证：`2101.7.2-build.377`） | 必需 |
| Botania | 1.21.1 NeoForge 构建（开发/验证：`455-SNAPSHOT`） | 必需 |
| Patchouli / Curios | 1.21.1-93+ / 9.5.1+ | Botania 必需的前置 |
| JEI | 1.21.1 NeoForge 19.64.0.455+ | 可选，仅客户端配方展示 |

模组 jar 本身不打包以上任何依赖；把它们和本模组一起放进整合包 `mods/` 即可。

## 2. 功能

### 2.1 Botania 配方 KubeJS schema（`event.recipes.botania.*`）

| 脚本方法 | Botania 1.21.1 配方类型 | 参数（按顺序，`?` 为可选） |
|---|---|---|
| `mana_infusion` | `botania:mana_infusion` | output, input, mana, ?catalyst, ?group |
| `pure_daisy` | `botania:pure_daisy` | output, input, ?time（默认 150） |
| `elven_trade` | `botania:elven_trade` | output（列表）, ingredients（列表） |
| `runic_altar` | `botania:runic_altar` | output, ingredients, mana, reagent, ?catalysts |
| `brew` | `botania:brew` | brew id, ingredients |
| `petal_apothecary` | `botania:petal_apothecary` | output, ingredients, reagent |
| `orechid` | `botania:orechid` | output, input, mana, ?weight, ?cooldown, ?biome_bonus_weight, ?biome_bonus_tag |
| `orechid_ignem` | `botania:orechid_ignem` | 同上 |
| `marimorphosis` | `botania:marimorphosis` | 同上 |
| `terrestrial_agglomeration_plate` | `botania:terrestrial_agglomeration_plate` | result, ingredients, mana |
| `terra_plate`（别名） | 同上 | 同上；1.20.1 习惯写法，JSON 仍写真实类型 id |

`output`/`input` 里与方块相关的参数使用 Botania 的 `StateIngredient`：脚本里可以直接写方块 id（`'minecraft:stone'`）、方块状态（`'minecraft:oak_log[axis=y]'`）、方块标签（`'#minecraft:logs'`），或完整 JSON 对象（`{ type: 'botania:block', block: 'minecraft:stone' }`）。

**位置写法与对象写法都支持**。对象写法（KubeJS 2101 单对象参数）里的值也可以直接用脚本字符串，例如：

```js
event.recipes.botania.mana_infusion({ output: 'minecraft:diamond', input: 'minecraft:coal', mana: 10000 })
event.recipes.botania.pure_daisy({ output: 'minecraft:deepslate', input: 'minecraft:stone', time: 150 })
```

完整示例见 [examples/kubejs/server_scripts/botania_recipes.js](examples/kubejs/server_scripts/botania_recipes.js)。

### 2.2 泰拉凝聚板扩展（`event.recipes.kubejs_botania_neo.terra_plate`）

在 Botania 泰拉凝聚板（Terrestrial Agglomeration Plate）配方的基础上增加：

- **自定义下方方块/流体图案**：`below` 指定板正下方一层的若干坐标（通常 3×3，x/z 取 -1..1），支持方块 id、`#方块标签`、流体 id；
- **自动旋转匹配**：图案会依次尝试 4 个水平旋转方向，合成后 `result_blocks` 以同一旋转方向应用；
- **合成后逐格转化**：`result_blocks` 指定合成成功后下方哪些格子变成什么方块/流体；
- **自定义魔力消耗**：`mana`（默认 500000）；
- **单物品/多物品输入**：`ingredients` 数量必须与玩家放上板的物品个数完全一致（与 Botania 原版机制相同）；
- **不写 `below` 时**回退到 Botania 默认 3×3 平台（Livingrock/Shimmerrock + 青金石块），可作为普通泰拉板配方的 KubeJS 写法；
- **JEI / EMI 展示**：
  - JEI：独立分类「泰拉凝聚板（扩展）」显示输入/产物、下方图案与合成后的方块变化；魔力使用 **Botania 自己的魔力条**（直接调用 Botania 公开的 `HUDHandler.renderManaBar`）。扩展配方会从 Botania 自带的「泰拉凝聚板」JEI 页隐藏，仅在本模组分类显示。
  - EMI（0.4.0+）：**原生 EMI 分类**「泰拉凝聚板（扩展）」，输入/产物、`below` 图案、`result_blocks` 合成后变化均为原生 Widget；魔力条使用 Botania 自己的 EMI `ManaWidget`（同样走 `HUDHandler.renderManaBar`）。**泰拉凝聚板注册为该分类的 workstation**：在 EMI 中对着凝聚板按配方键即可查看扩展配方，配方页内也有工作方块槽；扩展配方同样从 Botania 的 EMI「Terrestrial Agglomeration」页移除。
  - 同时安装 EMI 时 JEI 端不再注册本模组分类（避免 EMI 通过 JEMI 显示重复页面）；仅安装 JEI 时行为不变。
- **物品识别**：扩展配方的材料会被 Botania 自动识别为“可放上凝聚板”的物品（无需额外配置）。

脚本写法：

```js
ServerEvents.recipes(event => {
    event.recipes.kubejs_botania_neo.terra_plate({
        ingredients: ['minecraft:diamond', 'minecraft:diamond', 'minecraft:emerald'],
        mana: 50000,
        output: { item: 'minecraft:netherite_ingot' },
        below: { positions: [
            { x: -1, z: -1, block: 'minecraft:stone' },
            { x: 0,  z: -1, block: 'minecraft:cobblestone' },
            // ... 其余格
            { x: 0,  z: 0,  fluid: 'minecraft:water' }
        ] },
        result_blocks: [
            { x: 0, z: 0, block: 'minecraft:ice' }
            // ...
        ]
    })
})
```

坐标：以凝聚板所在方块为原点，`x` 向东为正、`z` 向南为正，y 固定为板正下方一层。`below` 也可以直接写成数组（`below: [ ... ]`）。

完整示例见 [examples/kubejs/server_scripts/terra_plate_extension.js](examples/kubejs/server_scripts/terra_plate_extension.js)。

### 2.3 配方工作方块（JEI / EMI 催化剂）

每个 Botania 配方类型在 JEI/EMI 中都对应一个"工作方块"（workstation / catalyst）：在 JEI 里悬停该物品按 `U`，或在 EMI 里指向/右键该物品选择"查看配方"，即可打开对应分类。本模组的所有示例配方对应的中文分类与工作方块如下：

| 脚本配方 | JEI/EMI 分类（中文） | 工作方块 / 触发物 |
|---|---|---|
| `mana_infusion` | 魔力灌注 | 魔力池、稀释魔力池、神话魔力池 |
| `pure_daisy` | 白雏菊 | 白雏菊、浮空白雏菊 |
| `elven_trade` | 精灵交易 | 精灵传送门核心（实际合成还需要精灵门结构） |
| `runic_altar` | 符文祭坛 | 符文祭坛 |
| `brew` | 植物酿造 | 植物酿造台 |
| `petal_apothecary` | 花药台（花瓣炼制） | 花药台（`#botania:petal_apothecaries` 标签内的全部变种） |
| `orechid` | 凝矿兰 | 凝矿兰、浮空凝矿兰 |
| `orechid_ignem` | 炎矿兰 | 炎矿兰、浮空炎矿兰 |
| `marimorphosis` | 异构花 | 异构花 / 小型异构花（含浮空版本） |
| `terrestrial_agglomeration_plate` | 泰拉凝聚板 | 泰拉凝聚板（Terrestrial Agglomeration Plate） |
| `kubejs_botania_neo:terra_plate`（扩展，0.4.0+） | 泰拉凝聚板（扩展） | **泰拉凝聚板**（已注册为 EMI workstation；JEI 中为催化剂，配方页内也显示「工作方块」槽） |

说明：

- "工作方块"只是配方查看器里的**查询催化剂**；实际合成还需要对应的 Botania 机器/花/结构，例如泰拉凝聚板下方需要有平台（活石/微光石 + 青金石块，或扩展配方的 `below` 图案），精灵交易需要搭建精灵门。
- 本模组页面的界面文本都有中文翻译：工作方块槽标签为「工作方块」，扩展分类名称为「泰拉凝聚板（扩展）」；Botania 各分类的中文名称来自 Botania 自带语言文件。
- 示例脚本 [botania_recipes.js](examples/kubejs/server_scripts/botania_recipes.js) 与 [terra_plate_extension.js](examples/kubejs/server_scripts/terra_plate_extension.js) 的注释里也标出了每个配方的"工作方块"。

### 2.4 脚本绑定

- `ManaHandler`：`vazkii.botania.api.mana.ManaItemHandler` 实例
- `CorporeaHelper`：`vazkii.botania.api.corporea.CorporeaHelper` 实例

## 3. 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.x 整合包/服务端；
2. 放入前置：KubeJS（+ Rhino、Better Advanced Tooltips）、Botania（+ Patchouli、Curios）；
3. 放入本模组 `kubejs_botania_neo-0.4.0.jar`（从 [Releases](https://github.com/yelingyou/Kubejs-botania-neo/releases) 下载，或按第 5 节自行构建）；
4. 可选：客户端放入 JEI 以获得配方展示；
5. 把示例脚本放到 `kubejs/server_scripts/` 后进入游戏 `/reload` 或重启。

## 4. 已知行为 / 限制

### 4.1 启动/元数据问题（重要）

- NeoForge 1.21.1 的模组元数据文件是 `META-INF/neoforge.mods.toml`（旧版 Forge 的 `mods.toml` 已不再使用）。
- 0.1.x 最初打包的一版 `neoforge.mods.toml` 意外带上了 UTF-8 BOM（`EF BB BF`），会导致 NeoForge 的 TOML 解析器报错、模组加载失败。**现已修复为 UTF-8 无 BOM**；请使用 `dist/kubejs_botania_neo-0.4.0.jar`（2026-10-29 12:xx 之后打包的版本）。若在游戏里看到 `neoforge.mods.toml`/`mods.toml` 解析或编码相关报错，先确认 jar 是最新版本。

### 4.2 行为限制

1. **原版配方优先**：如果某扩展配方的 `ingredients` 与 Botania 自带泰拉板配方（如泰拉钢锭）完全相同，Botania 可能优先命中自带配方；请避免与自带配方材料完全重复。
2. **JEI/EMI 只显示一次**：扩展配方通过 JEI 的 `hideRecipes` 与 EMI 的 `removeRecipes` 从 Botania 自带“泰拉凝聚板”分类中隐藏，只在「泰拉凝聚板（扩展）」分类显示（JEI 隐藏集合按 RecipeType 区分、EMI 过滤按分类 id + 配方命名空间，均不影响本模组分类）；同时安装 EMI 时使用 EMI 原生分类、JEI 端不注册本模组分类以避免 JEMI 重复页面；REI 未做隐藏处理。
3. **图案层数**：只支持板正下方一层（y = -1）。`below`/`result_blocks` 的坐标可以超出 -1..1，但必须在这一层。
4. **旋转**：只做 4 个水平旋转，不做镜像匹配；`result_blocks` 跟随命中的旋转。
5. **方块转化时机**：在合成完成时直接 `setBlock`；如果目标位置有其他方块会被覆盖。
6. **`result_blocks` 不支持标签**（标签无法确定具体方块），流体则放置为源方块。
7. **扩展配方的图案判定每 tick 都会执行**（仅遍历本模组扩展配方并读取图案方块，开销很小）。

## 5. 构建

```bash
# 需要 JDK 21
./gradlew build
```

产物：`build/libs/kubejs_botania_neo-0.4.0.jar`。

本仓库的开发环境是离线环境，`build/moddev/artifacts/` 中预置了与 `gradle.properties` 中 NeoForge/neoform/parchment 版本一致的 NeoForm 产物；`build.gradle` 检测到它们存在时会跳过 `createMinecraftArtifacts`（原因见 [docs/build-notes.md](docs/build-notes.md)）。在正常联网环境中，删除 `build/moddev/artifacts/` 后让 Gradle 自行生成即可，同时可删除 `tools/anvilcraft-interface-stubs.jar` 相关的那行 `compileOnly`（仅为兼容本机缓存产物中的 AnvilCraft interface injection 而存在）。

## 6. 开发文档

- [CHANGELOG.md](CHANGELOG.md)：版本变更记录
- [docs/build-notes.md](docs/build-notes.md)：构建环境、已知问题与可复现命令
- [docs/kubejs-2101-api-notes.md](docs/kubejs-2101-api-notes.md)：KubeJS 2101 配方 API 调研与 1.20.1→2101 差异
- [docs/botania-1.21.1-recipe-spec.md](docs/botania-1.21.1-recipe-spec.md)：Botania 1.21.1 配方/泰拉板内部机制规格
- [docs/terra-plate-extension.md](docs/terra-plate-extension.md)：泰拉凝聚板扩展实现说明

## 7. 验证状态

- [x] `gradlew build` 编译通过、jar 产出（0.4.0）
- [x] KubeJS 插件在用户整合包中成功加载（0.1.0 实测日志：`dev.kubejsbotania.neo.kubejs.BotaniaKubeJSPlugin` 出现在插件列表）
- [x] 标准 Botania 配方（位置写法）在游戏内成功创建（0.1.0 实测 9/10；剩余问题已定位并修复：示例 reagent 标签、对象写法 codec）
- [x] 扩展配方平台校验逻辑修正（无 `below` 配方不再绕过原版平台）
- [x] 示例脚本 JavaScript 语法检查通过
- [ ] **0.4.0 客户端实测**：10 种配方 + 对象写法、扩展配方（图案/旋转/流体/result_blocks）、JEI 分类（按约定由用户测试）
- [ ] 日志检查：`[kubejs_botania_neo] JEI: N terra plate recipes total, M extended recipes` 中 M > 0，以及 `JEI: hid N extended terra plate recipes from Botania's agglomeration page`

建议测试清单见 [docs/terra-plate-extension.md](docs/terra-plate-extension.md) 第 4 节；版本变更见 [CHANGELOG.md](CHANGELOG.md)。
