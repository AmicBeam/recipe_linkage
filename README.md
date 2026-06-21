# 配方研究台 / Recipe Linkage

配方研究台是一个面向 Minecraft 1.20.1 Forge 整合包的研究与阶段解锁模组。它提供一个一格空间的研究台方块和纸质风格的研究样本物品，让整合包作者用数据包定义“材料关联”谜题。

玩家将研究样本放入研究台后，样本会生成并保存一张固定的路线图。玩家沿着已显露的节点提交对应材料，逐步连接到可见的终点。节点材料可写为具体物品、物品标签，或带 NBT 的物品栈条件。终点本身不需要提交物品；当已提交节点与终点连通时，研究完成，并可解锁对应的 AStages 阶段。

## 玩法特点

- 数据驱动：模组本体不内置任何研究样本，全部研究由数据包提供。
- 材料关联：线索以物品图标呈现，不依赖文字谜面。
- 固定样本：研究图生成后写入样本，取出样本不会重置路线、返还材料或重新随机。
- 路线策略：玩家看到终点和当前可提交节点，需要用尽量少的材料打通路径，而不是暴力枚举物品。
- 阶段联动：完成研究后通过 AStages 授予整合包阶段。
- 易于整合：研究样本可通过任务、战利品、KubeJS、函数或其他系统发放。

## 研究流程

1. 整合包通过数据包定义研究图，并给玩家发放已绑定的研究样本。
2. 玩家把研究样本放入配方研究台。
3. 研究台生成并固定本样本的路线图。
4. 可提交节点会显示半透明物品图标和所需数量。
5. 玩家提交材料后，该节点变为已到达节点，并显露相邻可提交节点。
6. 当已到达节点连接到终点时，研究完成。
7. 已完成样本可手持右键，用于领取对应阶段。

## 数据包入口

研究定义文件放在：

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

研究样本通过 NBT 绑定到某个研究定义，例如：

```mcfunction
/give @p recipe_linkage:research_sample{RecipeLinkage:{Research:"modpack:resonant_core"}}
```

完整字段说明见 `docs/DATAPACK_FORMAT.md`。仓库中也包含一个仅使用原版物品的示例数据包：`examples/vanilla_linkage_datapack`。

研究标题使用 Minecraft 文本组件格式，整合包可写 `{ "translate": "...", "fallback": "..." }`。对应语言文件属于客户端资源，需放在资源包或 KubeJS assets 中，例如 `assets/<namespace>/lang/zh_cn.json`。

## 可选联动

- JEI：鼠标悬停在待提交节点上时，可用 JEI 查询对应材料来源。
- AStages：研究完成时授予配置的阶段。
- Sophisticated Backpacks：提交材料时可从玩家携带的 Sophisticated Backpacks 背包内取材。

---

Recipe Linkage is a research and progression mod for Minecraft 1.20.1 Forge modpacks. It adds a one-block research table and paper-like research samples, allowing pack authors to define material-linkage puzzles through datapacks.

When a player inserts a research sample into the table, the sample generates and stores a fixed route graph. The player submits matching materials along revealed nodes until their reached path connects to the visible final target. Node materials can be concrete items, item tags, or NBT-bearing stack requirements. The target node itself does not consume an item. Completing the graph can unlock an AStages stage.

## Features

- Data-driven: the base mod ships no built-in research samples; all research is provided by datapacks.
- Material linkage: clues are represented by item icons rather than written riddles.
- Persistent samples: once a graph is generated, removing the sample does not reset progress, refund items, or reroll the route.
- Route strategy: players see the target and available nodes, then decide which material path is worth paying for.
- Stage integration: completed research grants modpack progression through AStages.
- Pack-friendly delivery: bound samples can be given through quests, loot, KubeJS, functions, or other progression systems.

## Research Flow

1. A modpack defines research graphs through datapacks and gives players bound research samples.
2. The player inserts a sample into the Recipe Research Table.
3. The table generates and fixes a route graph onto that sample.
4. Submit-ready nodes show translucent item icons and required counts.
5. Submitting materials marks a node as reached and reveals adjacent submit-ready nodes.
6. The research completes when reached nodes connect to the target.
7. A completed sample can be right-clicked in hand to claim its stage.

## Datapack Entry

Research definition files are loaded from:

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

A research sample is bound to a definition with NBT, for example:

```mcfunction
/give @p recipe_linkage:research_sample{RecipeLinkage:{Research:"modpack:resonant_core"}}
```

See `docs/DATAPACK_FORMAT.md` for the full format. A vanilla-only example datapack is included at `examples/vanilla_linkage_datapack`.

Research titles use Minecraft text components. Packs can define `{ "translate": "...", "fallback": "..." }`; the matching language files are client assets and should live in a resource pack or KubeJS assets such as `assets/<namespace>/lang/en_us.json`.

## Optional Integrations

- JEI: hover a submit-ready node and use JEI lookup for the required material.
- AStages: grant the configured stage when a research graph is completed.
- Sophisticated Backpacks: material submissions can consume matching items from carried Sophisticated Backpacks.
