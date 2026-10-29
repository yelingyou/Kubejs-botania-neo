// KubeJS Botania Neo - 泰拉凝聚板扩展配方示例
// 放置位置：<整合包>/kubejs/server_scripts/terra_plate_extension.js
// 配方类型：event.recipes.kubejs_botania_neo.terra_plate({...})
//
// 工作方块：泰拉凝聚板（Terrestrial Agglomeration Plate）。
// 在 EMI 中指向/右键泰拉凝聚板选择“查看配方”，或在 JEI 中悬停泰拉凝聚板按 U，
// 即可打开「泰拉凝聚板（扩展）」分类（0.4.0 起凝聚板已注册为该分类的 workstation/催化剂）。
//
// 坐标说明：以泰拉凝聚板所在方块为原点 (0,0)，y 固定为板正下方一层。
// x 向右（东）为正，z 向南为正。图案会自动尝试 4 个水平旋转方向，命中哪个方向，
// result_blocks 就以同一旋转方向应用。position 里 block 可以是方块 id 或 #方块标签，
// 也可以把 block 换成 fluid（流体 id）。

ServerEvents.recipes(event => {
    // ===== 示例 1：完整写法（与原 terra-plate-extension 1.20.1 页面示例一致） =====
    // 工作方块：泰拉凝聚板
    event.recipes.kubejs_botania_neo.terra_plate({
        // 输入物品：钻石 x2 + 绿宝石 x1（物品总数必须等于 ingredients 数量）
        ingredients: ['minecraft:diamond', 'minecraft:diamond', 'minecraft:emerald'],
        // 需要的魔力
        mana: 50000,
        // 产物（支持 { item: 'id', count: n } 或直接 'id'）
        output: { item: 'minecraft:netherite_ingot' },
        // 合成前：下方 3x3 方块图案（最多可写任意 x/z 偏移，通常 -1..1）
        below: {
            positions: [
                { x: -1, z: -1, block: 'minecraft:stone' },
                { x: 0, z: -1, block: 'minecraft:cobblestone' },
                { x: 1, z: -1, block: 'minecraft:stone_bricks' },
                { x: -1, z: 0, block: 'minecraft:obsidian' },
                { x: 0, z: 0, block: 'minecraft:gold_block' },
                { x: 1, z: 0, block: 'minecraft:iron_block' },
                { x: -1, z: 1, block: 'minecraft:diamond_block' },
                { x: 0, z: 1, block: 'minecraft:emerald_block' },
                { x: 1, z: 1, block: 'minecraft:lapis_block' }
            ]
        },
        // 合成后：逐格替换下方方块
        result_blocks: [
            { x: -1, z: -1, block: 'minecraft:mossy_stone_bricks' },
            { x: 0, z: -1, block: 'minecraft:cracked_stone_bricks' },
            { x: 1, z: -1, block: 'minecraft:chiseled_stone_bricks' },
            { x: -1, z: 0, block: 'minecraft:crying_obsidian' },
            { x: 0, z: 0, block: 'minecraft:netherite_block' },
            { x: 1, z: 0, block: 'minecraft:copper_block' },
            { x: -1, z: 1, block: 'minecraft:redstone_block' },
            { x: 0, z: 1, block: 'minecraft:coal_block' },
            { x: 1, z: 1, block: 'minecraft:quartz_block' }
        ]
    })

    // ===== 示例 2：下方图案支持流体 / 方块标签，below 也可直接写数组 =====
    // 工作方块：泰拉凝聚板
    event.recipes.kubejs_botania_neo.terra_plate({
        ingredients: ['minecraft:coal', 'minecraft:coal'],
        mana: 20000,
        output: 'minecraft:diamond',
        below: [
            { x: 0, z: 0, fluid: 'minecraft:water' },
            { x: -1, z: 0, block: '#minecraft:stone_bricks' },
            { x: 1, z: 0, block: '#minecraft:stone_bricks' }
        ],
        result_blocks: [
            { x: 0, z: 0, block: 'minecraft:ice' }
        ]
    })

    // ===== 示例 3：不写 below 时使用 Botania 默认 3x3 平台（Livingrock/Shimmerrock + 青金石块），
    //       但产物/魔力/图案转化仍按扩展配方处理 =====
    // 工作方块：泰拉凝聚板
    event.recipes.kubejs_botania_neo.terra_plate({
        ingredients: ['minecraft:gold_ingot', 'minecraft:gold_ingot'],
        mana: 100000,
        output: { item: 'minecraft:diamond_block', count: 2 }
    })

    // ===== 示例 4：单方块图案（只要求板正下方是钻石块） =====
    // 工作方块：泰拉凝聚板
    event.recipes.kubejs_botania_neo.terra_plate({
        ingredients: ['minecraft:redstone', 'minecraft:redstone'],
        mana: 12000,
        output: 'minecraft:redstone_block',
        below: [{ x: 0, z: 0, block: 'minecraft:diamond_block' }],
        result_blocks: [{ x: 0, z: 0, block: 'minecraft:gold_block' }]
    })
})
