![Recipe Linkage](media/recipe-linkage-header-16x10.png)

# Recipe Linkage

**Recipe Linkage** is a data-driven research table mod for Minecraft Forge 1.20.1.

It is made for modpacks that want recipes, machines, or progression steps to feel like discoveries instead of simple quest rewards. Pack authors define research samples through datapacks, then players solve material-linkage puzzles on a dedicated research table to unlock configured progression stages.

## What Does It Add?

- A compact one-block **Recipe Research Table**
- Reusable **Research Samples** with a paper-like style
- Icon-based research graphs built from item relationships
- Material submission puzzles with persistent progress
- AStages progression rewards for completed research
- Optional JEI and Sophisticated Backpacks quality-of-life support

## How Research Works

Players insert a bound research sample into the table. The sample generates a fixed research graph and keeps that graph forever.

The target is visible, but the route must be opened by submitting related materials. Available nodes show item icons and required counts, so the puzzle is about choosing an efficient path through the graph rather than guessing a written riddle.

The final target node does not consume an item. Research is complete once the reached path connects to the target.

Completed samples can be right-clicked to claim their configured stage. Server configs can control whether stages are awarded automatically on completion and whether claimed samples are consumed.

## Designed For Modpacks

Recipe Linkage does not ship built-in research content. Every research is loaded from datapacks, which means pack authors decide exactly what each sample represents.

Research nodes can use:

- A specific item
- An item tag
- A required count
- Optional NBT matching
- Optional fixed coordinates, or automatic graph layout
- Weighted edges for randomized route generation
- Localized research titles through Minecraft text components

This makes it useful for gated recipes, themed technology chains, magic discoveries, chapter milestones, or any progression where materials should hint at their own relationships.

## Optional Integrations

- **JEI**: hover a research node and use JEI lookup for the required material.
- **AStages**: grant a stage when research is completed.
- **Sophisticated Backpacks**: material submissions can pull matching items from carried backpacks when enabled.

These integrations are optional. The table and sample system still work as a standalone research mechanic.

## Important Notes

- Research samples are not consumed by the table.
- Removing a sample does not reset its graph.
- Submitted materials are not refunded.
- Generated routes do not reroll after the sample is created.
- The base mod provides no default research samples; packs must provide them through datapacks, quests, loot, commands, KubeJS, or another delivery system.

## For Pack Authors

Research JSON files are loaded from:

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

A bound sample can be given with NBT:

```mcfunction
/give @p recipe_linkage:research_sample{RecipeLinkage:{Research:"modpack:resonant_core"}}
```

See the included datapack format documentation and vanilla example datapack for a full reference.

---

# 简体中文介绍

**Recipe Linkage** 是一个面向 Minecraft Forge 1.20.1 整合包的配方研究台模组。

它提供一格空间的研究台方块和可复用的研究样本。整合包作者通过数据包定义“材料关联”研究图，玩家在研究台中提交相关材料，打通路线并解锁对应阶段。

## 核心特点

- 模组本体不内置研究内容，全部研究由数据包载入
- 线索以物品图标呈现，不依赖文字谜面
- 样本生成研究图后会永久保存进度
- 玩家需要选择更省材料的路线，而不是盲目试物品
- 研究完成后可联动 AStages 授予阶段
- 可选支持 JEI 查询和 Sophisticated Backpacks 背包取材

## 玩法流程

玩家把已绑定的研究样本放入研究台后，样本会生成固定路线图。

当前可提交节点会显示半透明物品图标和数量。玩家提交材料后，该节点变为已到达节点，并显露相邻节点。当已到达路线连接到终点时，研究完成。终点本身不需要提交物品。

研究完成后的阶段发放、右键领取是否消耗样本，都可以通过配置文件调整。

## 给整合包作者

研究文件路径：

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

样本可通过命令、任务、战利品、KubeJS 或其他方式发放：

```mcfunction
/give @p recipe_linkage:research_sample{RecipeLinkage:{Research:"modpack:resonant_core"}}
```

节点支持具体物品、物品标签、数量、NBT 条件、手动坐标或自动布局。边可以配置生成概率，从而让同一研究在不同样本中拥有不同路线。
