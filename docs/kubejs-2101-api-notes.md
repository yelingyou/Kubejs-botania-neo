# KubeJS 2101 / Botania 1.21.1 配方 API 对照笔记（P2）

本文档记录把 **KubeJS-Botania 1.20.1** 的配方 schema 移植到 **KubeJS 2101（MC 1.21.1 / NeoForge）**
时核实过的真实 API。所有签名都以以下**实际文件**为准（不是猜测）：

- KubeJS 源码 `refs/kubejs-2101/kubejs-2101`（2101.7.2），并用 `javap` 对照
  `deps/kubejs-neoforge-2101.7.2-build.377.jar` 复核。
- Botania 源码 `refs/botania-1.21.1/Botania-1.21.1-porting`，并用
  `deps/botania-neoforge-1.21.1-455.jar` 复核：
  - 配方 JSON 直接从 jar 内 `data/botania/recipe/**` 解包统计（共 **1387** 个 recipe JSON）；
  - `javap` 复核 `StateIngredients` / `StateIngredient` / `Brew` / `BotaniaAPI`。
- 旧版 `refs/kubejs-botania-1.20.1/.../com/prunoideae/**` 仅作字段/命名参考。

> 结论先行：**Botania 1.21.1 的配方 type id 与 1.20.1 不同**（尤其 Terra Plate 改名），
> 且 KubeJS 2101 的 `RecipeComponent` / `RecipeKey` API 与 2001 不兼容，旧代码不能直接搬。

---

## 1. KubeJS 2101 插件入口

### 1.1 插件发现（不同于 2001 / 不是 ServiceLoader）

`KubeJSPlugins.loadMod()` 读取 **mod jar 内** 的 `kubejs.plugins.txt`，内容为插件类全限定名
（每行一个，支持 `#注释` 和 `client modid` 后缀）。本 mod 的 `src/main/resources/kubejs.plugins.txt`：

```
dev.kubejsbotania.neo.kubejs.BotaniaKubeJSPlugin
```

插件类必须是 `public`、有 public 无参构造器、实现 `dev.latvian.mods.kubejs.plugin.KubeJSPlugin`。

### 1.2 相关 hook（`KubeJSPlugin` 接口，2101）

```java
default void registerRecipeFactories(RecipeFactoryRegistry registry);          // 自定义 KubeRecipe 子类工厂
default void registerRecipeMappings(RecipeMappingRegistry registry);           // 旧配方名 -> 新 type 映射
default void registerRecipeComponents(RecipeComponentTypeRegistry registry);   // 注册自定义 RecipeComponentType
default void registerRecipeSchemas(RecipeSchemaRegistry registry);             // 注册命名空间/配方 schema
default void registerRecipeSchemaFunctionTypes(RecipeSchemaFunctionRegistry registry);
default void registerBindings(BindingRegistry bindings);                       // 脚本全局绑定
```

`BindingRegistry`（2101）：

```java
public record BindingRegistry(KubeJSContext context, Scriptable scope) {
    public ScriptType type();                 // STARTUP / CLIENT / SERVER
    public void add(String name, Object value);
}
```

### 1.3 `RecipeSchemaRegistry` / `RecipeNamespace`

```java
public class RecipeSchemaRegistry {
    public RecipeNamespace namespace(String namespace);          // 取/建命名空间
    public void register(ResourceLocation id, RecipeSchema schema);
}

public class RecipeNamespace extends LinkedHashMap<String, RecipeSchemaType> {
    public RecipeNamespace register(String id, RecipeSchema schema);   // 返回 this，可链式
    public RecipeNamespace registerBasic(String id, RecipeKey<?>... keys);
    public RecipeNamespace withExistingParent(String id, ResourceLocation parent);
}
```

`RecipeSchemaType` 的关键点：**schema 的注册 id 会作为 recipe serializer id 去查注册表**

```java
serializerKey = ResourceKey.create(Registries.RECIPE_SERIALIZER, schema.typeOverride == null ? id : schema.typeOverride);
// RecipeTypeFunction#createRecipe 中 schemaType.getSerializer() 找不到 serializer 时直接抛异常
```

因此“脚本里的 schema 名字”必须和真实 serializer id 一致，或者用 `RecipeSchema.typeOverride(...)` 指向真实 id。
最终写出的配方 JSON 的 `"type"` 用的是 `serializerType`（= typeOverride 或注册 id），见 `KubeRecipe#serializeChanges`：

```java
if (newRecipe) json.addProperty("type", getSerializationTypeFunction().schemaType.serializerType);
```

### 1.4 `RecipeSchema`（2101）

```java
public class RecipeSchema {
    public KubeRecipeFactory recipeFactory;
    public ResourceLocation typeOverride;
    public final List<RecipeKey<?>> keys;
    public final List<RecipeKey<?>> includedKeys;
    public final Map<RecipeKey<?>, RecipeOptional<?>> keyOverrides;
    public final Map<String, RecipeFunctionInstance> functions;

    public RecipeSchema(Map<RecipeKey<?>, RecipeOptional<?>> keyOverrides, List<RecipeKey<?>> keys);
    public RecipeSchema(RecipeKey<?>... keys);

    public RecipeSchema factory(KubeRecipeFactory factory);
    public RecipeSchema typeOverride(ResourceLocation id);
    public RecipeSchema constructor(RecipeConstructor/RecipeKey<?>... keys);
    public RecipeSchema uniqueId(RecipeKey<?> key);
    public RecipeSchema uniqueIds(SequencedCollection<RecipeKey<?>> keys);
    public RecipeSchema function(RecipeFunctionInstance function);
    public <T> RecipeSchema setOpFunction(String name, RecipeKey<T> key, T value);
    public <T> RecipeSchema addToListOpFunction(String name, RecipeKey<List<T>> key);
    public RecipeSchema postProcessor(RecipePostProcessor processor);
}
```

要点：

- **构造器参数顺序即 schema key 声明顺序**。自动生成的 JS 构造器是 `includedKeys` 的从前缀：
  `for (a = minRequiredArguments; a <= includedKeys.size(); a++) constructor(includedKeys.subList(0, a))`。
- **required key 必须全部排在 optional key 之前**，否则构造 `RecipeSchema` 直接
  `IllegalArgumentException("Required key '...' must be ahead of optional keys!")`。
- `key.exclude()` 的 optional key 不参与自动构造器。
- `alwaysWrite() + defaultOptional()` 是非法组合（构造时抛错）；但
  `optional(值).alwaysWrite()` 合法（适合“codec 必填但我们想给默认并总是写出”的字段，见 runic_altar catalysts）。
- `RecipeSchema.typeOverride` 用于 kubejs:shaped→minecraft:crafting_shaped 这类“脚本名 ≠ serializer id”场景。

### 1.5 `RecipeKey`（2101）：**没有 `RecipeKey.key(...)` 静态工厂**

2001 的 `ItemComponents.OUTPUT.key("result")`、`RecipeKey.key(component, name)` 都不存在了。
2101 通过组件的默认方法创建 key：

```java
public interface RecipeComponent<T> {
    default RecipeKey<T> key(String name, ComponentRole role);
    default RecipeKey<T> inputKey(String name);
    default RecipeKey<T> outputKey(String name);
    default RecipeKey<T> otherKey(String name);
}
// RecipeComponentType 也提供同样的 inputKey/outputKey/otherKey（内部调 instance()）
```

`ComponentRole` 是显式枚举：`INPUT("input") / OUTPUT("output") / OTHER("other")`；
`RecipeSchema` 用它统计 input/output 数量（配方替换、JEI 展示等）。旧 API 里“组件自带 role”的写法已删除，
**每个 key 必须显式选择 role**：产物 `outputKey`、原料 `inputKey`、其余（mana/time/weight/group…）`otherKey`。

```java
public final class RecipeKey<T> {
    public final RecipeComponent<T> component;
    public final String name;
    public final ComponentRole role;
    public final SequencedSet<String> names;
    public RecipeOptional<T> optional;
    public boolean excluded;
    public List<String> functionNames;
    public boolean alwaysWrite;

    public RecipeKey(RecipeComponent<T> component, String name, ComponentRole role);
    public RecipeKey<T> optional(T value);                    // 默认值（不可变对象！）
    public RecipeKey<T> optional(RecipeOptional<T> value);
    public RecipeKey<T> defaultOptional();                    // 默认 null
    public RecipeKey<T> alt(String... names);                 // JSON 别名
    public RecipeKey<T> exclude();
    public RecipeKey<T> noFunctions();
    public RecipeKey<T> functionNames(String... names);
    public RecipeKey<T> alwaysWrite();
}
```

### 1.6 `RecipeComponent`（2101）：codec 驱动

```java
public interface RecipeComponent<T> {
    RecipeComponentType<?> type();
    Codec<T> codec();
    TypeInfo typeInfo();
    default T wrap(RecipeScriptContext cx, Object from);      // 脚本值 -> Java 值（替代旧 read()）
    default void writeToJson(KubeRecipe recipe, RecipeComponentValue<T> cv, JsonObject json);
    default void readFromJson(KubeRecipe recipe, RecipeComponentValue<T> cv, JsonObject json);
    default boolean hasPriority(RecipeMatchContext cx, Object from);
    default boolean matches(RecipeMatchContext cx, T value, ReplacementMatchInfo match);
    default T replace(RecipeScriptContext cx, T original, ReplacementMatchInfo match, Object with);
    default boolean allowEmpty();
    default void validate(RecipeValidationContext ctx, T value);
    default boolean isEmpty(T value);
    default void buildUniqueId(UniqueIdBuilder builder, T value);
    default ListRecipeComponent<T> asList();                  // 非空 list（IntBounds.DEFAULT = min 1！）
    default ListRecipeComponent<T> asListOrSelf();
    default RecipeComponent<T> withCodec(Codec<T> codec);
    default <O> EitherRecipeComponent<T, O> or(RecipeComponent<O> other);
}
```

- **读写默认实现完全基于 `codec()`**：`writeToJson` 用 `cv.key.codec.encodeStart(recipe.type.event.ops.json(), value)`；
  `readFromJson` 用 `key.codec.parse(ops.json(), json)`。`ops.json()` 是 **`RegistryOps<JsonElement>`**，
  所以需要 registry 的 codec（如 Botania 的 StateIngredient/Brew）可以直接工作。
- 旧 2001 的 `Class<?> componentClass()`、`JsonElement write(RecipeJS, T)`、`T read(RecipeJS, Object)`、
  `TypeDescJS constructorDescription(DescriptionContext)`、`ComponentRole role()` **全部已删除**，不要照抄。
- 自定义组件通常只需实现 `type()`、`codec()`、`typeInfo()`，需要更友好的脚本入参时再覆盖 `wrap()`。
- `typeInfo()` 用于脚本类型提示（ProbeJS/d.ts）与默认 `wrap`；可用 `TypeInfo.STRING.or(TypeInfo.of(Xxx.class))`
  表示“字符串或 Java 对象”。

### 1.7 `RecipeComponentType`（2101）：组件必须注册

```java
public abstract class RecipeComponentType<T> {
    public static <T> Unit<T> unit(ResourceLocation id, Function<RecipeComponentType<T>, RecipeComponent<T>> instanceGetter);
    public static <T> Unit<T> unit(ResourceLocation id, RecipeComponent<T> instance);
    public static <CT extends RecipeComponent<?>> RecipeComponentType<?> dynamic(ResourceLocation id, MapCodec<CT> mapCodec);

    public ResourceLocation id();
    public boolean isUnit();
    public RecipeComponent<T> instance();      // 仅 unit 类型可用
    public RecipeKey<T> inputKey(String name); // 等价于 instance().inputKey(name)
    public RecipeKey<T> outputKey(String name);
    public RecipeKey<T> otherKey(String name);
}
```

- 内置单例可直接用类型取 key：`ItemStackComponent.ITEM_STACK.outputKey("result")`、
  `NumberComponent.POSITIVE_INT.otherKey("mana")`。
- 自定义组件（本 mod 的 state_ingredient / brew / cacheable_function / terra_plate_positions）需在
  `registerRecipeComponents(RecipeComponentTypeRegistry)` 注册，否则 KubeJS 生成 schema JSON / Web API
  序列化时会报 `Missing stored recipe component type for '...'`。**注意注册发生在 schema 注册之前**
  （`RecipeSchemaStorage#fireEvents` 先加载组件类型，再 `registerRecipeSchemas`），所以顺序天然安全。

### 1.8 可直接复用的内置组件（本移植用到的）

| 用途 | 2101 写法 | 说明 |
|---|---|---|
| ItemStack 产物 | `ItemStackComponent.ITEM_STACK.outputKey("result")` | codec=`ItemStack.STRICT_CODEC`（非空） |
| ItemStack 可空 | `ItemStackComponent.OPTIONAL_ITEM_STACK` | `ItemStack.OPTIONAL_CODEC` |
| Ingredient | `IngredientComponent.INGREDIENT.inputKey("input")` | codec=`Ingredient.CODEC_NONEMPTY` |
| Ingredient 可空 | `IngredientComponent.OPTIONAL_INGREDIENT` | `Ingredient.CODEC` |
| Ingredient 列表 | `IngredientComponent.INGREDIENT.instance().asList()` | 默认 **min=1**（非空校验） |
| 列表自定义边界 | `...asList().withBounds(IntBounds.of(0, 16))` | `IntBounds.DEFAULT=(1,MAX)`、`OPTIONAL=(0,MAX)` |
| ItemStack 列表 | `ItemStackComponent.ITEM_STACK.instance().asList()` | 同上 |
| 整数 | `NumberComponent.INT / LONG / FLOAT / DOUBLE`（组件实例） | `Codec.INT` 等 |
| 正整数 | `NumberComponent.POSITIVE_INT.instance()`（类型） | `>0` |
| 非负整数 | `NumberComponent.NON_NEGATIVE_INT.instance()` | `>=0` |
| 自定义范围整数 | `NumberComponent.intRange(min, max)` | `Codec.intRange` |
| 字符串 | `StringComponent.STRING / OPTIONAL_STRING / ID` | `OPTIONAL_STRING` 允许 `""` |
| 布尔 | `BooleanComponent.BOOLEAN` | |
| Block | `BlockComponent.BLOCK` / `OPTIONAL_BLOCK` | |
| BlockState | `BlockStateComponent.BLOCK`（对象形式）/ `BLOCK_STRING` | |
| TagKey | `TagKeyComponent.BLOCK / ITEM / BIOME / FLUID / ENTITY_TYPE`（`HASHED_*` 写 `#` 前缀） | |
| 时间 | `TimeComponent.TICKS` 等 | **值类型是 `TickDuration`，不是 long**；`TickDuration.CODEC=Codec.LONG`，`isEmpty()` 对 0 返回 true（0 会被校验成空！） |
| Either | `component.or(other)` | |
| 嵌套对象 | `RecipeComponent.builder(CustomObjectRecipeComponent.Key...)` | |
| 只包 codec 的简单组件 | `new SimpleRecipeComponent<>(type, codec, typeInfo)` | 本 mod 的 Lead 扩展用到了 |

> ⚠️ `TimeComponent` 的 `isEmpty(TickDuration)` 是 `ticks <= 0`，`allowEmpty()==false`。
> 如果某个可省略的时间字段默认值可能是 0（如 `pure_daisy.time`），**不要用 `TimeComponent.TICKS`**，
> 否则新配方默认值 0 会在 `afterLoaded` 校验时抛 `EmptyRecipeComponentException`。本移植改用
> `NumberComponent.intRange(0, Integer.MAX_VALUE)`。

### 1.9 默认值 / 序列化语义（容易踩坑）

- 新配方：`KubeRecipeFactory.create(...)` → `KubeRecipe#initValues(save=true)`：
  - 所有 optional key 填入 `RecipeKey.optional` 的默认值；
  - `alwaysWrite` 的 key 置 write 标记；
  - **默认值只有在 `alwaysWrite` 或之后被脚本 set 过时才会写进 JSON**。
- 读已有 JSON：`deserialize(false)` 只读 JSON 里存在的字段；缺失的 optional 保持 null，
  之后 `getValue` 才回落到默认值（`KubeRecipe#getValue`）。
- 校验：`afterLoaded` 对每个 value 调 `component.validate` → 默认实现
  `if (!allowEmpty() && isEmpty(value)) throw EmptyRecipeComponentException`；
  `null` 且非 optional 才报缺失。**默认值也会被校验**，所以给 optional 的默认值本身必须能通过校验。
- 编码：`serialize()` 只处理 `shouldWrite()` 的 holder。所以“未设置的 optional 字段”不会出现在 JSON 里，
  Botania 侧就用自己的 codec 默认值 —— 这正是本次 schema 默认值必须对齐 codec 的原因。

### 1.10 2001 → 2101 差异清单（移植必读）

| 2001（KubeJS-Botania 旧代码） | 2101 现状 |
|---|---|
| `RegisterRecipeSchemasEvent event; event.namespace("botania").register(...)` | `KubeJSPlugin#registerRecipeSchemas(RecipeSchemaRegistry registry)`；`registry.namespace("botania").register(...)` |
| `BindingsEvent event; event.add(...)` | `KubeJSPlugin#registerBindings(BindingRegistry bindings)`；`bindings.add(...)` |
| `RecipeJS` | `KubeRecipe` |
| `RecipeKey.key(component, name)` / `ItemComponents.OUTPUT.key(...)` | `component.inputKey/outputKey/otherKey(name)` 或 `new RecipeKey<>(component, name, role)`；`ItemComponents` 已删除 |
| `ItemComponents.OUTPUT / INPUT / INPUT_ARRAY / OUTPUT_ARRAY` | `ItemStackComponent` / `IngredientComponent` / `.asList()`（+ `IntBounds`） |
| `InputItem` / `OutputItem` | 纯 `Ingredient` / `ItemStack` |
| `RecipeComponent.componentClass()`、`read()`、`write()`、`role()`、`constructorDescription()` | `codec()`、`wrap()`、`type()`、`typeInfo()`；role 移到 `RecipeKey` |
| 组件自带 role（`BlockComponent.INPUT` 等） | role 由 key 的 `inputKey/outputKey/otherKey` 决定 |
| `TypeDescJS` / `DescriptionContext` | `dev.latvian.mods.rhino.type.TypeInfo` |
| 插件经 `KubeJSPlugin` ServiceLoader?（旧版也是显式注册） | **`kubejs.plugins.txt` 资源文件** |
| `RecipeSchema` 只有 varargs 构造 | 仍支持 varargs，但另有 `(Map<RecipeKey,RecipeOptional> keyOverrides, List<RecipeKey>)` |
| `new RecipeSchema(...).constructor(RecipeConstructor...)` | 同名保留；`constructor(RecipeKey...)` 改为自动生成 |
| `RecipeKey.optional(long)` | `optional(T)`（自动装箱） |
| `UtilsJS.parseBlockState` | `BlockWrapper.parseBlockState(RegistryAccessContainer, String)` / `BlockWrapper.wrapBlockState(...)` |
| 内置 `BotaniaSchema.BREW` 组件（2001 手写） | Botania 1.21.1 有 brew 注册表 + `byNameCodec()`，用 codec 组件 |
| 内置 `BotaniaSchema.BLOCK_OR_TAG`（2001 手写 block/tag Either） | **不需要了**：1.21.1 的 StateIngredient 自带 typed codec（block/tag/state/any_of/all_of_excluding） |

---

## 2. Botania 1.21.1 配方规格（以源码 codec + jar 内真实 JSON 为准）

从 `botania-neoforge-1.21.1-455.jar` 解包 `data/botania/recipe/**` 统计（1387 个 JSON）：

| type | JSON 数量 | 用到的字段集合 |
|---|---:|---|
| `botania:mana_infusion` | 152 | `input,mana,output` + 可选 `catalyst`(135)、`group`(104) |
| `botania:pure_daisy` | 10 | `input,output,time` + 可选 `copy_properties`(1)、`success_function`(1) |
| `botania:elven_trade` | 15 | `ingredients,output` |
| `botania:runic_altar` | 16 | `catalysts,ingredients,mana,output,reagent` |
| `botania:brew` | 20 | `brew,ingredients` |
| `botania:petal_apothecary` | 45 | `ingredients,output,reagent` |
| `botania:orechid` | 16 | `cooldown,input,mana,output,weight` + 可选 `biome_bonus_tag`/`biome_bonus_weight`(5) |
| `botania:orechid_ignem` | 3 | `cooldown,input,mana,output,weight` |
| `botania:marimorphosis` | 8 | `biome_bonus_tag,biome_bonus_weight,input,mana,output,weight` |
| `botania:terrestrial_agglomeration_plate` | 1 | `ingredients,mana,result` |
| `botania:runic_altar_head` | 1 | `catalysts,ingredients,mana,output,reagent` |
| `botania:elven_trade_lexicon` | 1 | （空，特殊 serializer） |

> `runic_altar_head` / `elven_trade_lexicon` 不在本次 10 个 schema 范围内；KubeJS 会继续用
> `UnknownRecipeSchemaType`，不会破坏配方加载。`terra_plate` **不是** 1.21.1 的 type id（改名了，见 2.10）。

### 2.1 `botania:mana_infusion` — `ManaInfusionRecipe.Serializer.CODEC`

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `output` | `ItemStack.STRICT_CODEC` | 是 | — |
| `input` | `Ingredient.CODEC_NONEMPTY` | 是 | — |
| `mana` | `ExtraCodecs.intRange(1, ManaPoolBlock.MAX_MANA + 1)`（MAX_MANA=1000000） | 是 | — |
| `group` | `Codec.STRING.optionalFieldOf("group", "")` | 否 | `""` |
| `catalyst` | `StateIngredients.TYPED_CODEC.optionalFieldOf("catalyst", StateIngredients.NONE)` | 否 | `botania:none` |

真实 JSON：

```json
{
  "type": "botania:mana_infusion",
  "catalyst": {"type": "botania:block", "block": "botania:alchemy_catalyst"},
  "group": "botania:flower_shrinking",
  "input": {"item": "botania:agricarnation"},
  "mana": 2500,
  "output": {"count": 1, "id": "botania:agricarnation_petite"}
}
```

### 2.2 `botania:pure_daisy` — `PureDaisyRecipe.Serializer.CODEC`

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `input` | `StateIngredients.TYPED_CODEC` | 是 | — |
| `output` | `StateIngredients.TYPED_CODEC`（**1.21.1 起输出也是 StateIngredient，不再只是 BlockState**） | 是 | — |
| `time` | `ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 0)` | 否 | **0**（见下） |
| `copy_properties` | `Codec.BOOL.optionalFieldOf("copy_properties", false)` | 否 | `false` |
| `pre_update_function` | `CacheableFunction.CODEC` optional | 否 | 无（null） |
| `success_function` | `CacheableFunction.CODEC` optional | 否 | 无（null） |

⚠️ **`time` 的 codec 默认值确实是 0**（`PureDaisyBlockEntity` 收到 0 会下一 tick 立刻转换），
但 Botania 自己的 datagen/所有自带 JSON 都写 `150`（`PureDaisyRecipe.DEFAULT_TIME = 150`）。
本移植按 codec 对齐（默认 0，不写则不写）；**脚本想要正常 150 tick 必须显式传 150**。
如需改成“默认 150 且总是写出”，只需把 `PureDaisySchema.TIME` 改成
`.optional(150).alwaysWrite()`。

真实 JSON：

```json
{
  "type": "botania:pure_daisy",
  "input":  {"type": "botania:block", "block": "minecraft:netherrack"},
  "output": {"type": "botania:state", "state": {"Name": "minecraft:cobblestone"}},
  "time": 150
}
```

复杂输入示例（all_of_excluding + tag，脚本可用 `event.custom(json)` 或传 object）：

```json
{
  "type": "botania:pure_daisy",
  "copy_properties": true,
  "input": {
    "type": "botania:all_of_excluding",
    "excluding": [{"type": "botania:any_of", "ingredients": [{"type": "botania:block", "block": "botania:livingwood_log"}]}],
    "ingredients": [{"type": "botania:tag", "tag": "#minecraft:logs"}]
  },
  "output": {"type": "botania:block", "block": "botania:livingwood_log"},
  "time": 150
}
```

### 2.3 `botania:elven_trade` — `ElvenTradeRecipe.Serializer.CODEC`

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `output` | `ExtraCodecs.nonEmptyList(ITEM_STACK_COMPACT_CODEC.listOf())` — **ItemStack 列表** | 是 | — |
| `ingredients` | `ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf())` | 是 | — |

```json
{
  "type": "botania:elven_trade",
  "ingredients": [{"item": "botania:livingwood"}],
  "output": [{"id": "botania:dreamwood"}]
}
```

（KubeJS 用 `ItemStack.STRICT_CODEC` 编码，会写出 `{"id":...,"count":1}`；Botania 的
`ITEM_STACK_COMPACT_CODEC` 解码端是 `STRICT_CODEC`，两种形式都接受。）

### 2.4 `botania:runic_altar` — `RunicAltarRecipe.Serializer.CODEC`

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `ingredients` | `Ingredient.CODEC_NONEMPTY.listOf()`（字段必填，列表可空） | 是 | — |
| `catalysts` | `Ingredient.CODEC_NONEMPTY.listOf()`（字段必填，列表可空；自带 JSON 全为 `[]`） | 是 | — |
| `reagent` | `Ingredient.CODEC_NONEMPTY`（1.21.1 无默认值；自带配方都是 `botania:livingrock`） | 是 | — |
| `mana` | `ExtraCodecs.POSITIVE_INT` | 是 | — |
| `output` | `ItemStack.STRICT_CODEC` | 是 | — |

Botania 会在加载时额外校验：`ingredients+catalysts` 数量 1..16、三类输入不能有相同物品/相同定义。
KubeJS 不重复这些校验；本 schema 给两个列表加了 `IntBounds.of(0,16)`。

```json
{
  "type": "botania:runic_altar",
  "catalysts": [],
  "ingredients": [{"tag": "c:dusts/mana"}, {"item": "minecraft:stone"}],
  "mana": 5200,
  "output": {"count": 2, "id": "botania:rune_of_earth"},
  "reagent": {"item": "botania:livingrock"}
}
```

### 2.5 `botania:brew` — `BotanicalBreweryRecipe.Serializer.CODEC`

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `brew` | `BotaniaAPI.instance().getBrewRegistry().byNameCodec()`（brew 注册表 id） | 是 | — |
| `ingredients` | `ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf())` | 是 | — |

**没有 `mana` 字段**：1.21.1 的 `getManaUsage()` 直接取自 `brew.getManaCost()`。
brew 本身是 datapack registry 条目（`botania:brews`），脚本只能引用已注册的 brew。

```json
{
  "type": "botania:brew",
  "brew": "botania:absolution",
  "ingredients": [{"item": "minecraft:nether_wart"}, {"item": "minecraft:quartz"}]
}
```

### 2.6 `botania:petal_apothecary` — `PetalApothecaryRecipe.Serializer.CODEC`

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `output` | `ItemStack.STRICT_CODEC` | 是 | — |
| `reagent` | `Ingredient.CODEC_NONEMPTY` | 是 | —（旧 2001 KubeJS schema 给过默认值，1.21.1 没有） |
| `ingredients` | `Ingredient.CODEC_NONEMPTY.listOf(1, 16)` | 是 | — |

```json
{
  "type": "botania:petal_apothecary",
  "ingredients": [{"tag": "botania:petals/light_gray"}, {"item": "botania:rune_of_earth"}],
  "output": {"count": 1, "id": "botania:clayconia"},
  "reagent": {"tag": "botania:seed_apothecary_reagent"}
}
```

### 2.7 `botania:orechid` / `botania:orechid_ignem` / `botania:marimorphosis`

三者**共用同一个 codec**（`OrechidRecipe.Serializer.RAW_CODEC`；`OrechidIgnemRecipe` 与
`MarimorphosisRecipe` 只是 `xmap` 包装，字段完全相同）：

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `input` | `StateIngredients.TYPED_CODEC` | 是 | — |
| `output` | `StateIngredients.TYPED_CODEC` | 是 | — |
| `mana` | `ExtraCodecs.POSITIVE_INT` | 是 | — |
| `cooldown` | `ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cooldown", 0)` | 否 | 0 |
| `weight` | `ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("weight", 0)` | 否 | 0 |
| `biome_bonus_weight` | `Codec.INT.optionalFieldOf("biome_bonus_weight", 0)`（普通 INT，可负） | 否 | 0 |
| `biome_bonus_tag` | `TagKey.codec(Registries.BIOME)` optional | 否 | 无 |
| `pre_update_function` | `CacheableFunction.CODEC` optional | 否 | 无 |
| `success_function` | `CacheableFunction.CODEC` optional | 否 | 无 |

Botania codec 的额外校验（KubeJS 不重复）：`weight + biome_bonus_weight` 不能同时为 0、总和不能为负。
`marimorphosis` 的 constructor 强制 `cooldown=0`（自带 JSON 不写 cooldown）；共用 codec 仍接受该字段。
注意 1.21.1 起 `mana` 是**必填**（旧 KubeJS-Botania schema 没有 mana，迁移配方时必须补）。

```json
{
  "type": "botania:orechid",
  "biome_bonus_tag": "botania:orechid_stone_copper_bonus",
  "biome_bonus_weight": 2500,
  "cooldown": 100,
  "input": {"type": "botania:block", "block": "minecraft:stone"},
  "mana": 17500,
  "output": {"type": "botania:block", "block": "minecraft:copper_ore"},
  "weight": 2724
}
```

### 2.8 `botania:terrestrial_agglomeration_plate`（旧名 `botania:terra_plate`）

`TerrestrialAgglomerationRecipe.Serializer.CODEC`：

| JSON 字段 | 类型 / codec | 必填 | 默认 |
|---|---|---|---|
| `ingredients` | `ExtraCodecs.nonEmptyList(Ingredient.CODEC_NONEMPTY.listOf())` | 是 | — |
| `mana` | `ExtraCodecs.POSITIVE_INT` | 是 | — |
| `result` | `ItemStack.STRICT_CODEC` — **字段名是 `result`，不是 `output`** | 是 | — |

```json
{
  "type": "botania:terrestrial_agglomeration_plate",
  "ingredients": [{"tag": "c:ingots/manasteel"}, {"tag": "c:gems/mana_pearl"}, {"tag": "c:gems/mana_diamond"}],
  "mana": 500000,
  "result": {"count": 1, "id": "botania:terrasteel_ingot"}
}
```

### 2.9 `StateIngredient` 的 JSON 形式（Botania 源码 codec 实测）

`StateIngredients.TYPED_CODEC = 注册表.byNameCodec().dispatch("type", StateIngredient::getType, StateIngredientType::codec)`。
jar 内出现的 6 种 type：

| type | 字段 | 示例 |
|---|---|---|
| `botania:block` | `block`（`BuiltInRegistries.BLOCK.byNameCodec()`） | `{"type":"botania:block","block":"minecraft:stone"}` |
| `botania:state` | `state`（`BlockState.CODEC`） | `{"type":"botania:state","state":{"Name":"minecraft:cobblestone"}}` |
| `botania:tag` | `tag`（`TagKey.hashedCodec(Registries.BLOCK)`，**带 `#`**） | `{"type":"botania:tag","tag":"#minecraft:logs"}` |
| `botania:any_of` | `ingredients`（StateIngredient 列表） | `{"type":"botania:any_of","ingredients":[...]}` |
| `botania:all_of_excluding` | `ingredients` + `excluding` | 见 2.2 示例 |
| `botania:none` | （无字段，unit） | `{"type":"botania:none"}` |

KubeJS 组件直接复用这个 codec，因此脚本可以传：`StateIngredient` 实例、`Block`、`BlockState`、
`TagKey<Block>`、`"#tag"`、`"namespace:block"`、`"namespace:block[prop=val]"`，或 JSON object。

### 2.10 Terra Plate 改名结论（重要）

- 1.20.1：type id = `botania:terra_plate`（旧 KubeJS-Botania 注册的就是这个名字）。
- **1.21.1：type id = `botania:terrestrial_agglomeration_plate`**
  （`vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe.TYPE_ID = botaniaRL(LibBlockNames.TERRESTRIAL_AGGLOMERATION_PLATE)`,
  `LibBlockNames.TERRESTRIAL_AGGLOMERATION_PLATE = "terrestrial_agglomeration_plate"`）。
  jar 内唯一配方 JSON 也是这个 type。
  （JEI 分类里残留的 `RecipeType.create(MODID, "terra_plate", ...)` 只是客户端展示用旧字符串，不是配方 type。）
- 因此真实注册必须是：
  - `botania.register("terrestrial_agglomeration_plate", TerraPlateSchema.SCHEMA)`（**datapack 解析必须用真实 id**）
  - 另外注册别名 `botania.register("terra_plate", TerraPlateSchema.ALIAS_SCHEMA)`，其
    `typeOverride = botania:terrestrial_agglomeration_plate`，脚本 `event.recipes.botania.terra_plate(...)`
    写出的 JSON type 仍是真实 id（KubeJS 用 `serializerType` 覆写新配方的 type）。
- 脚本里真实名字也照常可用：`event.recipes.botania.terrestrial_agglomeration_plate(...)`。

### 2.11 Brew 注册表 / BotaniaAPI 变化

- 1.20.1 旧 KubeJS-Botania 用 `BotaniaAPI.instance().getBrewRegistry()` + `RegistryInfo<Brew>`；
  1.21.1 仍是 `BotaniaAPI#getBrewRegistry()`（`DefaultedRegistry<Brew>`，registry key
  `BotaniaRegistries.BREWS = botania:brews`），brew recipe codec 用
  `BotaniaAPI.instance().getBrewRegistry().byNameCodec()`。
- 1.21.1 KubeJS 2101 不再需要 `RegistryInfo` 注册自定义注册表；NeoForge registry 直接可用。
- `ManaItemHandler.instance()`、`CorporeaHelper.instance()` 在 1.21.1 仍存在（API 包未变），
  可继续作为脚本 binding 暴露。

---

## 3. 本 mod 的实现（P2 交付）

包：`dev.kubejsbotania.neo.kubejs`

```
BotaniaKubeJSPlugin.java            // 唯一 KubeJSPlugin 入口（kubejs.plugins.txt 指向它）
BotaniaRecipeSchemas.java           // register(RecipeSchemaRegistry)：10 个 botania: schema + terra_plate 别名
BotaniaBindings.java                // register(BindingRegistry)：ManaHandler / CorporeaHelper
components/
  BotaniaComponents.java            // 3 个自定义 RecipeComponentType 的持有与注册
  StateIngredientComponent.java     // StateIngredient <-> Botania TYPED_CODEC
  BrewComponent.java                // Brew <-> botania:brews 注册表 byNameCodec
  CacheableFunctionComponent.java   // .mcfunction id <-> CacheableFunction.CODEC
schemas/
  TerraPlateSchema.java             // result, ingredients, mana （+ 别名 schema）
  ManaInfusionSchema.java           // output, input, mana, catalyst?, group?
  PureDaisySchema.java              // output, input, time?, copy_properties?, pre_update_function?, success_function?
  ElvenTradeSchema.java             // output(list), ingredients(list)
  RunicAltarSchema.java             // output, ingredients, mana, reagent, catalysts?(alwaysWrite)
  BrewSchema.java                   // brew, ingredients(list)
  PetalApothecarySchema.java        // output, ingredients(list), reagent
  OrechidSchema.java                // output, input, mana, weight?, cooldown?, biome_bonus_weight?, biome_bonus_tag?, functions?
  OrechidIgnemSchema.java           // 复用 OrechidSchema.create()
  MarimorphosisSchema.java          // 复用 OrechidSchema.create()
```

脚本参数顺序（自动构造器 = key 声明顺序，`[...]` 为可选参数）：

| schema | 脚本调用 |
|---|---|
| `botania:terra_plate`（别名） | `terra_plate(result, ingredients, mana)` → JSON type 仍为 `botania:terrestrial_agglomeration_plate` |
| `botania:terrestrial_agglomeration_plate` | 同上 |
| `botania:mana_infusion` | `mana_infusion(output, input, mana[, catalyst[, group]])` |
| `botania:pure_daisy` | `pure_daisy(output, input[, time[, copy_properties[, pre_update_function[, success_function]]]])` — 建议显式传 `150` |
| `botania:elven_trade` | `elven_trade([outputs], [ingredients])` |
| `botania:runic_altar` | `runic_altar(output, [ingredients], mana, reagent[, [catalysts]])`（catalysts 省略时自动写 `[]`） |
| `botania:brew` | `brew(brewIdOrBrew, [ingredients])` |
| `botania:petal_apothecary` | `petal_apothecary(output, [ingredients], reagent)` |
| `botania:orechid` / `_ignem` / `marimorphosis` | `orechid(output, input, mana[, weight[, cooldown[, biomeBonusWeight[, biomeBonusTag[, preFn[, successFn]]]]]])` |

脚本示例（ServerEvents.recipes）：

```js
ServerEvents.recipes(event => {
  // Mana Infusion: 输出、输入、mana，可选 catalyst / group
  event.recipes.botania.mana_infusion('minecraft:diamond', 'minecraft:coal', 5000)
  event.recipes.botania.mana_infusion('minecraft:gold_ingot', '#c:ingots/iron', 2000,
      { type: 'botania:block', block: 'botania:alchemy_catalyst' }, 'mypack:gold')

  // Pure Daisy: 输出/输入都是 StateIngredient；150 建议显式给
  event.recipes.botania.pure_daisy('#c:stone', 'minecraft:basalt', 150)
  event.recipes.botania.pure_daisy(
      { type: 'botania:state', state: { Name: 'minecraft:oak_wood' } },
      'minecraft:oak_log', 150, true)

  // Runic Altar: reagents 必填，catalysts 可省略（自动写 []）
  event.recipes.botania.runic_altar('botania:rune_of_spring', ['botania:manasteel_ingot'],
      5000, { item: 'botania:livingrock' })

  // Terra Plate（别名，等价于 terrestrial_agglomeration_plate）
  event.recipes.botania.terra_plate('minecraft:netherite_ingot',
      ['#c:ingots/manasteel', '#c:gems/mana_diamond'], 100000)

  // Brew：brew 为注册表 id（字符串）
  event.recipes.botania.brew('botania:absorption',
      ['minecraft:nether_wart', 'minecraft:gold_ingot'])

  // Petal Apothecary
  event.recipes.botania.petal_apothecary('minecraft:poppy',
      ['#botania:petals/red', 'minecraft:wheat_seeds'],
      '#botania:seed_apothecary_reagent')

  // Orechid：mana 是必填项；权重/群系加成可选
  event.recipes.botania.orechid('minecraft:emerald_ore', 'minecraft:stone', 17500, 100, 100)
  event.recipes.botania.marimorphosis('botania:cataclasite', '#botania:marimorphosis_convertable',
      12, 1, 11, 'botania:marimorphosis_swamp_bonus')
})
```

### 3.1 设计决定 / 已知取舍

1. **真实 id 优先**：10 个 schema 全部用 1.21.1 真实 serializer id 注册（datapack 解析、KubeJS
   修改已有 Botania 配方都依赖这一点）。额外提供 `terra_plate` 别名以兼容 1.20.1 脚本习惯。
2. **`pure_daisy.time` 默认 0**（对齐 codec，见 2.2 的警告）。
3. **`runic_altar.catalysts`**：codec 必填但可为 `[]`。schema 里写成
   `optional(List.of()).alwaysWrite()`，既给出 4 参数短构造器，又保证新配方 JSON 一定含
   `"catalysts": []`（与 Botania 自带 JSON 一致）。
4. **`petal_apothecary.reagent`** 改为 required（1.21.1 codec 无默认值）。旧脚本需要补参数。
5. **`orechid` 系 `mana`** 改为 required（1.21.1 新增必填）。
6. **自定义组件只做 3 个**：StateIngredient / Brew / CacheableFunction；block/tag Either 不需要了
   （StateIngredient 已覆盖，且 block+tag 的 `any_of` 组合也在 codec 内）。
7. `BotaniaKubeJSPlugin` 还按 Lead 要求注册了 Lead 的泰拉板扩展：
   `TerraPlatePositionComponent.TYPE`（components）与 `TerraPlateExtensionSchema.register(registry)`（schemas）。

---

## 4. 待编译 / 运行验证点（交 Lead）

> **2026-10-29 本地手工编译验证：已通过。**
> 用 ModDevGradle 参考工程产物
> `AnvilCraft-TerminalPlugins/build/moddev/artifacts/neoforge-21.1.248.jar`
> + `kubejs-botania-neo/libs/*.jar` + Gradle 缓存全部依赖，对 `src/main/java` 下所有源文件
> （含 Lead 的 `recipe/`、`mixin/`、`TerraPlateExtension*`）手工执行了一次 `javac`：
> **0 error**、27 个 class 产出（其中本 P2 包 18 个类）。因此下面 1~11 项涉及的 KubeJS/Botania
> API 签名均已由编译器确认；仍需 `gradlew build` 作为最终构建验证。
> 同时修复了 Lead 的 `TerraPlateExtensionSchema` 的一个前向引用编译错误
> （`OUTPUT_CODEC` 在 `PAGE_STYLE_ITEM_STACK` 声明之前引用它，已把 `PAGE_STYLE_ITEM_STACK`
> 声明移到前面；行为不变）。

**编译相关：**

1. [ ] `BotaniaComponents.STATE_INGREDIENT/BREW/CACHEABLE_FUNCTION` 的
   `RecipeComponentType.unit(id, XxxComponent::new)` 方法引用泛型推断（期望 `Function<RecipeComponentType<T>, RecipeComponent<T>>`）。
2. [ ] `NumberComponent.intRange(...)` 返回的 `IntRange` 上调 `.otherKey(...)`（`RecipeComponent` 默认方法）。
3. [ ] `IngredientComponent.INGREDIENT.instance().asList().withBounds(IntBounds.of(0,16)).inputKey(...)`
   的链式泛型；`alwaysWrite()` + `optional(List.of())` 组合。
4. [ ] `TagKeyComponent.BIOME.instance().otherKey("biome_bonus_tag")` 的 `TagKey<Biome>` 泛型。
5. [ ] `StateIngredientComponent` 中 `TagKey#registry()`、`BlockWrapper.parseBlockState(RegistryAccessContainer, String)`、
   `JsonUtils.of(Context, Object)`、`StateIngredients.TYPED_CODEC.parse(RegistryOps, JsonElement)`。
6. [ ] `BrewComponent` 中 `BotaniaAPI#getBrewRegistry().byNameCodec()`、`DefaultedRegistry#get/getKey`
   与 `Codec.lazyInitialized`。
7. [ ] `CacheableFunctionComponent` 中 `CacheableFunction.CODEC` 的公开性与类型（应为 `Codec<CacheableFunction>`）。
8. [ ] `ManaPoolBlock.MAX_MANA` 可见性（`public static final int`，已 javap 源码确认；若 Lead 只想依赖 api 包可改硬编码 1000000）。
9. [ ] 本包引用了 Lead 的 `TerraPlateExtensionSchema` / `TerraPlatePositionComponent`
   （Lead 已在同一工程写好；若彼时尚未就绪，可临时注释 `BotaniaKubeJSPlugin` 里那两行）。
10. [ ] `kubejs.plugins.txt` 已由 Lead 写入，内容必须为
    `dev.kubejsbotania.neo.kubejs.BotaniaKubeJSPlugin`（全限定名一致）。
11. [ ] 依赖：KubeJS 2101 编译还需要 Rhino（`dev.latvian.mods.rhino.type.TypeInfo`），
    `libs/rhino-2101.2.7-build.85.jar` 已由 build-env/Lead 补齐。

**运行验证点（编译通过后）：**

1. [ ] 启动日志无 `Failed to load plugin ...`；`/kubejs recipes` 或导出 schema 后能看到
   `botania:mana_infusion` 等 10+1 个 schema（而不是 `UnknownRecipeSchema`）。
2. [ ] 脚本创建各类型配方后，`kubejs/exported/recipes/...`（或 DataExport）JSON 的
   `"type"` 分别正确（特别是 `terra_plate` 别名必须写出 `botania:terrestrial_agglomeration_plate`）。
3. [ ] Botania 自带 152/10/15/16/20/45/16/3/8/1 个配方能被 KubeJS 读取且不被标记为 unknown
   （日志 `Skipping recipe ..., unknown type` 不应出现这些 type）。
4. [ ] 用手写脚本创建 `pure_daisy`（不传 time）→ JSON 无 `time`，运行时按 0 处理（预期行为，记录）；
   若决定改默认 150 需同步改 schema。
5. [ ] `runic_altar` 4 参数调用 → JSON 含 `"catalysts": []`。
6. [ ] StateIngredient 各种输入形式（block/state/tag/JSON object）→ `event.custom`/导出 JSON 与
   Botania 格式一致。
7. [ ] `brew` schema 用 `'botania:absorption'` 字符串 → JSON `"brew": "botania:absorption"`；
   非法 id 抛 `Unknown botania brew: ...`。
8. [ ] `orechid` 系输出 JSON 的 `input/output` 是 StateIngredient 对象（不是 item）。
9. [ ] `ManaHandler` / `CorporeaHelper` 脚本绑定可访问（startup/client/server 三种脚本类型）。

---

## 5. 权威资料索引

| 内容 | 位置 |
|---|---|
| KubeJS 2101 源码 | `refs/kubejs-2101/kubejs-2101/src/main/java/dev/latvian/mods/kubejs/` |
| 插件加载 | `plugin/KubeJSPlugins.java` |
| schema/组件核心 | `recipe/schema/*.java`、`recipe/component/*.java`、`recipe/RecipeKey.java` |
| schema JSON 数据驱动示例 | `src/main/resources/data/minecraft/kubejs/recipe_schema/*.json`（KubeJS 自己也支持 addon 用 JSON 注册 schema，路径 `data/<ns>/kubejs/recipe_schema/<path>.json`；本 mod 选择 Java 注册） |
| Botania 配方 codec | `refs/botania-1.21.1/.../Xplat/src/main/java/vazkii/botania/common/crafting/*.java` |
| Botania 配方 API id | `Xplat/src/main/java/vazkii/botania/api/recipe/*.java` |
| 真实配方 JSON | `deps/botania-neoforge-1.21.1-455.jar` → `data/botania/recipe/**`（1387 个） |
| 旧版参考 | `refs/kubejs-botania-1.20.1/.../common/src/main/java/com/prunoideae/{schema,custom}/` |
