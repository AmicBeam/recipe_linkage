# Recipe Linkage Wiki

[README](README_ZH.md) | [English Wiki](wiki.md)

这是 Recipe Linkage 面向整合包作者的详细用法文档，包含数据包路径、样本发放、研究 JSON 字段、配置项和示例。

## 研究流程

1. 整合包向玩家发放已绑定研究样本。
2. 玩家把样本放入 Recipe Research Table。
3. 样本生成一张固定研究图，并保存到物品自身。
4. 玩家提交材料，解锁当前可见节点。
5. 当已解锁节点连接到可见终点时，研究完成。
6. Recipe Linkage 根据配置自动授予阶段，或允许玩家右键已完成样本领取阶段。

终点节点永远不消耗物品。取出样本不会重置进度、退还材料或重新随机路线。

## 数据包路径

研究 JSON 文件放在：

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

数据包载入的研究会直接以已绑定样本的形式出现在 Recipe Linkage 创造模式物品栏中。

## 发放已绑定样本

NeoForge 1.21.1 使用 `minecraft:custom_data`：

```mcfunction
/give @p recipe_linkage:research_sample[minecraft:custom_data={RecipeLinkage:{Research:"modpack:basic_optics"}}]
```

Forge 1.20.1 使用旧版物品 NBT：

```mcfunction
/give @p recipe_linkage:research_sample{RecipeLinkage:{Research:"modpack:basic_optics"}}
```

整合包也可以通过任务、战利品、KubeJS 或其他进度系统发放样本。

## 配置说明

整合包和服务器可以在 `config/recipe_linkage-common.toml` 中调整行为：

- `autoAwardStageOnCompletion`：默认 `true`，研究完成后自动授予配置的阶段。
- `consumeCompletedSampleOnClaim`：默认 `false`，控制右键领取已完成样本时是否消耗样本。
- `revealCompletedGraph`：默认 `false`，控制研究完成后是否显示完整生成路线图。
- `enableSophisticatedBackpackMaterials`：默认 `true`，允许提交材料时从玩家携带的 Sophisticated Backpacks 中扣除匹配物品。

## 研究 JSON 字段

- `title`：可选的 Minecraft 文本组件，用作研究显示名。整合包想做本地化时可以写 `{ "translate": "research.<namespace>.<path>", "fallback": "Readable Name" }`。
- `target_stage`：AStages 阶段字符串，会传给 `/astages add <player> <stage> false false false`。
- `target`：可见终点节点的 id。
- `initial_nodes`：可选数组，表示初始可提交节点。它们不是已提交状态。允许多个。
- `min_distance_to_target`：从初始可提交节点集合到完成研究，期望至少需要提交的材料次数。终点不需要提交，因此不计入次数。
- `generation_attempts`：生成带权路线图时尝试的候选数量，最后取评分较好的图。
- `nodes[].ingredient`：推荐的材料格式。它接受配方风格的 ingredient，例如 `{ "item": "minecraft:glass" }`、`{ "tag": "minecraft:sand" }`、`[ { "item": "minecraft:coal" }, { "item": "minecraft:charcoal" } ]`，以及已注册的自定义 ingredient 类型。
- 旧版 `nodes[].item`、`nodes[].tag`、`nodes[].nbt`：仍然兼容已有整合包。单个节点必须在 `ingredient` 和旧平铺字段之间二选一，但同一份研究 JSON 中不同节点可以混用两种写法。
- `nodes[].count`：解锁该节点时消耗的材料数量，默认 `1`。写 `0` 表示只检查玩家是否持有匹配物品，不消耗。
- 旧版 `nodes[].nbt`：可选 SNBT 字符串，用于匹配提交物品的数据。新写法推荐使用加载器自己的自定义 ingredient 格式。
- `nodes[].x` 和 `nodes[].y`：可选百分比坐标。两个字段都提供时，该节点使用固定位置。缺少任意一个时，该节点交给自动布局。
- `edges[].from` 和 `edges[].to`：这条边连接的两个节点 id。
- `edges[].chance`：该边在单个样本生成图中出现的概率，不写时默认为 `1.0`。

自动布局会在路线图、初始节点和终点确定后执行。它会尽量把起点侧放在左边、终点放在右边，按图距离分组，并使用紧凑的橄榄球型展开方式。

研究台进度条使用“最少还需要提交几次材料”计算，不是单纯的边距离。

## AStages + KubeJS 配方绑定示例

下面示例把红石比较器配方绑定到研究 JSON 里的 `target_stage: "basic_optics"`。研究完成后，Recipe Linkage 会授予这个 AStages 阶段；AStages 的 KubeJS API 会让没有该阶段的玩家无法使用对应配方。

脚本放在 `kubejs/server_scripts/recipe_linkage_stages.js`：

```js
// Requires: AStages, KubeJS
AStages.addRestrictionForRecipe(
  'recipe_linkage:basic_optics_comparator',
  'basic_optics',
  'minecraft:crafting',
  'minecraft:comparator'
)
```

如果使用下面复杂示例里的 `target_stage: "redstone_lens"`，就把脚本中的 `basic_optics` 改成 `redstone_lens`。

## 最简自动布局示例

这个示例没有写任何 `x` 和 `y` 字段。Recipe Linkage 会自动布局，让整体尽量呈现左侧起点、右侧终点的路线图。

保存为 `data/modpack/recipe_linkage/researches/basic_optics.json`：

```json
{
  "target_stage": "basic_optics",
  "target": "comparator",
  "min_distance_to_target": 3,
  "nodes": [
    { "id": "sand", "tag": "minecraft:sand", "count": 8 },
    { "id": "glass", "item": "minecraft:glass", "count": 3 },
    { "id": "redstone", "item": "minecraft:redstone", "count": 4 },
    { "id": "quartz", "item": "minecraft:quartz", "count": 2 },
    { "id": "comparator", "item": "minecraft:comparator" }
  ],
  "edges": [
    { "from": "sand", "to": "glass" },
    { "from": "glass", "to": "redstone" },
    { "from": "redstone", "to": "quartz" },
    { "from": "quartz", "to": "comparator" }
  ]
}
```

不写 `initial_nodes` 时，样本生成研究图时会自动选择一个合适的初始节点。不写 `chance` 时，该边的生成概率默认为 `1.0`。

## 全部自定义的复杂示例

这个示例展示了本地化标题、固定坐标、多个初始节点、配方风格的 `ingredient` 输入、多选一输入和带概率的边。最简 `item` / `tag` 节点字段仍然可用，并且同一份研究 JSON 中，不同节点可以混用旧的平铺写法和新的 `ingredient` 写法。

保存为 `data/modpack/recipe_linkage/researches/redstone_lens.json`：

```json
{
  "title": {
    "translate": "research.modpack.redstone_lens",
    "fallback": "Redstone Lens"
  },
  "target_stage": "redstone_lens",
  "target": "comparator",
  "min_distance_to_target": 4,
  "generation_attempts": 96,
  "initial_nodes": ["sand", "copper"],
  "nodes": [
    { "id": "sand", "ingredient": { "tag": "minecraft:sand" }, "count": 8, "x": 5, "y": 58 },
    { "id": "copper", "ingredient": { "item": "minecraft:copper_ingot" }, "count": 3, "x": 8, "y": 78 },
    { "id": "glass", "ingredient": { "item": "minecraft:glass" }, "count": 3, "x": 25, "y": 52 },
    { "id": "redstone", "ingredient": { "item": "minecraft:redstone" }, "count": 4, "x": 42, "y": 30 },
    { "id": "quartz", "ingredient": { "item": "minecraft:quartz" }, "count": 2, "x": 55, "y": 42 },
    { "id": "lens_material", "ingredient": [ { "item": "minecraft:quartz" }, { "item": "minecraft:amethyst_shard" } ], "count": 2, "x": 32, "y": 80 },
    { "id": "spyglass", "ingredient": { "item": "minecraft:spyglass" }, "count": 1, "x": 60, "y": 72 },
    { "id": "daylight_detector", "ingredient": { "item": "minecraft:daylight_detector" }, "count": 1, "x": 76, "y": 56 },
    { "id": "comparator", "ingredient": { "item": "minecraft:comparator" }, "count": 1, "x": 94, "y": 50 }
  ],
  "edges": [
    { "from": "sand", "to": "glass", "chance": 1.0 },
    { "from": "glass", "to": "redstone", "chance": 0.85 },
    { "from": "redstone", "to": "quartz", "chance": 0.9 },
    { "from": "quartz", "to": "comparator", "chance": 0.8 },
    { "from": "glass", "to": "lens_material", "chance": 0.65 },
    { "from": "lens_material", "to": "spyglass", "chance": 0.75 },
    { "from": "copper", "to": "spyglass", "chance": 0.9 },
    { "from": "spyglass", "to": "daylight_detector", "chance": 0.8 },
    { "from": "daylight_detector", "to": "comparator", "chance": 0.7 },
    { "from": "redstone", "to": "daylight_detector", "chance": 0.55 }
  ]
}
```

标题文本可以放在资源包或 KubeJS assets 中，例如 `assets/modpack/lang/zh_cn.json`：

```json
{
  "research.modpack.redstone_lens": "红石透镜研究"
}
```
