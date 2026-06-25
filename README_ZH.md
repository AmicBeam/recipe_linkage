![Recipe Linkage](docs/media/recipe-linkage-header-16x10.png)

# Recipe Linkage

[English README](README.md) | [完整 Wiki](wiki_zh.md)

**Recipe Linkage** 是一个面向 Minecraft NeoForge 1.21.1 和 Forge 1.20.1 整合包的配方研究台模组。

它新增一个研究台方块和可复用的研究样本。整合包作者通过数据包定义“材料关联”研究图，玩家在研究台中提交相关材料，打通路线并解锁对应 AStages 阶段。

## 核心特点

- 新增 **Recipe Research Table** 方块
- 可复用的 **Research Sample**，研究进度会保存在样本上
- 以物品图标呈现线索，不依赖文字谜面
- 终点可见，玩家需要选择更省材料的路线
- 研究完成后可授予 AStages 阶段
- 可选支持 JEI 查询和 Sophisticated Backpacks 背包取材

## 玩法概览

玩家把已绑定的研究样本放入研究台后，样本会生成固定路线图，并永久保存这张图。

当前可提交节点会显示物品图标和数量。玩家提交材料后，该节点变为已到达节点，并显露相邻节点。终点始终可见且不需要提交物品；当已到达路线连接到终点时，研究完成。

已完成的样本可以右键领取配置的阶段。服务器配置可以控制研究完成后是否自动发放阶段，以及右键领取阶段是否消耗样本。

## 给整合包作者

Recipe Linkage 不内置任何研究样本。研究 JSON 文件放在：

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

研究节点支持配方风格的 `ingredient` 对象、提交数量、可选固定坐标、带概率的边、本地化标题，并兼容旧整合包的平铺 `item` / `tag` / `nbt` 字段。

数据包载入的研究会直接以已绑定样本的形式出现在 Recipe Linkage 创造模式物品栏中。你也可以通过任务、战利品、命令、KubeJS 或其他系统发放样本。

完整 JSON 示例、字段说明、命令、配置项和 AStages + KubeJS 配方绑定示例请看 [完整 Wiki](wiki_zh.md)。

## 联动

- **AStages**：作为研究完成后的阶段产物。
- **JEI**：鼠标悬停研究节点时，可以使用 JEI 查询所需材料。
- **Sophisticated Backpacks**：启用后，提交材料时可以从玩家携带的背包中扣除匹配物品。

## 注意事项

- 研究台不会消耗研究样本。
- 取出样本不会重置研究图。
- 已提交材料不会退还。
- 样本生成路线后不会重新随机。
- 模组本体不提供默认研究样本；整合包需要自行通过数据包和发放方式提供内容。
