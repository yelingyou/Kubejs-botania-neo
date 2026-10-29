// KubeJS Botania Neo - Botania 1.21.1 原生配方类型示例
// 放置位置：<整合包>/kubejs/server_scripts/botania_recipes.js
// 修改后游戏内执行 /reload 即可生效（配方改动需要重新加载数据包）。
//
// 每条配方注释里的“工作方块”是 JEI/EMI 中用于查询该配方的催化剂物品：
// JEI 中悬停工作方块按 U；EMI 中指向/右键工作方块选择“查看配方”即可打开对应分类。

ServerEvents.recipes(event => {
    // 魔力灌注：output, input, mana, [catalyst], [group]
    // 工作方块：魔力池（Mana Pool）/ 稀释魔力池 / 神话魔力池
    event.recipes.botania.mana_infusion('minecraft:gold_ingot', 'minecraft:iron_ingot', 5000)

    // 纯净雏菊：output, input, [time]（time 默认 150）
    // 输入/输出可以是方块 id、方块状态字符串（minecraft:oak_log[axis=y]）或 #方块标签
    // 工作方块：白雏菊（Pure Daisy）/ 浮空白雏菊
    event.recipes.botania.pure_daisy('minecraft:deepslate', 'minecraft:stone', 150)

    // 精灵交易：输出列表, 输入列表（可多输入多输出）
    // 工作方块：精灵传送门核心（Elven Gateway Core，实际合成还需要精灵门结构）
    event.recipes.botania.elven_trade(
        ['minecraft:diamond', 'minecraft:lapis_lazuli'],
        ['minecraft:emerald']
    )

    // 符文祭坛：output, ingredients, mana, reagent, [catalysts]
    // 工作方块：符文祭坛（Runic Altar）
    event.recipes.botania.runic_altar(
        'botania:rune_of_spring',
        ['minecraft:feather', 'minecraft:string', '#c:dusts/mana'],
        5200,
        'botania:livingrock'
    )

    // 植物酿造：brew registry id, ingredients
    // 工作方块：植物酿造台（Botanical Brewery）
    event.recipes.botania.brew(
        'botania:absolution',
        ['minecraft:nether_wart', 'minecraft:quartz', 'minecraft:emerald']
    )

    // 花瓣药台：output, ingredients, reagent
    // 工作方块：花药台（Petal Apothecary，含 #botania:petal_apothecaries 的所有变种）
    event.recipes.botania.petal_apothecary(
        'botania:pure_daisy',
        ['#botania:petals/white', '#botania:petals/white', '#botania:petals/white', '#botania:petals/white'],
        '#botania:seed_apothecary_reagent'
    )

    // 兰花：output, input, mana, [weight], [cooldown], ...
    // output / input 为 Botania StateIngredient（方块 / 方块状态 / 方块标签）
    // 工作方块：凝矿兰（Orechid）/ 浮空凝矿兰
    event.recipes.botania.orechid('minecraft:diamond_ore', 'minecraft:stone', 17500, 30)

    // 烈焰兰花：同上
    // 工作方块：炎矿兰（Orechid Ignem）/ 浮空炎矿兰
    event.recipes.botania.orechid_ignem('minecraft:nether_quartz_ore', 'minecraft:netherrack', 20000, 50)

    // 变色兰花：output, input, mana, [weight], [cooldown], [biome_bonus_weight], [biome_bonus_tag]
    // 工作方块：异构花（Marimorphosis）/ 小型异构花（含浮空版本）
    event.recipes.botania.marimorphosis('botania:cataclasite', 'minecraft:stone', 12, 10)

    // 泰拉凝聚板（标准 Botania 配方，使用 Botania 默认的 3x3 平台）：
    // result, ingredients, mana
    // 工作方块：泰拉凝聚板（Terrestrial Agglomeration Plate）
    event.recipes.botania.terrestrial_agglomeration_plate(
        'botania:terrasteel_ingot',
        ['#c:ingots/manasteel', '#c:gems/mana_pearl', '#c:gems/mana_diamond'],
        500000
    )

    // 1.20.1 里常用的 terra_plate 别名依然可用，写出的 JSON 仍是
    // botania:terrestrial_agglomeration_plate（1.21.1 的真实类型 id）。
    // 工作方块：泰拉凝聚板
    // event.recipes.botania.terra_plate('botania:terrasteel_ingot', [...], 500000)

    // ===== 对象写法（KubeJS 2101 单对象参数）同样可用，值支持脚本字符串 =====
    // input/output 可写 'minecraft:xxx' 或 '#标签'；
    // StateIngredient 类型（pure_daisy/orechid/marimorphosis）可写 'minecraft:stone' / '#minecraft:base_stone_overworld'。
    // 工作方块：魔力池
    event.recipes.botania.mana_infusion({
        output: 'minecraft:emerald',
        input: 'minecraft:diamond',
        mana: 9000
    })

    // 工作方块：白雏菊
    event.recipes.botania.pure_daisy({
        output: 'minecraft:cobbled_deepslate',
        input: 'minecraft:deepslate',
        time: 150
    })

    // 工作方块：凝矿兰
    event.recipes.botania.orechid({
        output: 'minecraft:coal_ore',
        input: 'minecraft:tuff',
        mana: 9000,
        weight: 20
    })
})
