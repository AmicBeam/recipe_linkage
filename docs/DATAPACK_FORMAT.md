# Recipe Linkage Datapack Format

Research JSON files are loaded from:

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

The base mod intentionally ships no research definitions and no way to obtain bound research samples. Packs should issue samples through quests, loot, commands, KubeJS, or other progression systems.

## Sample Item NBT

```text
/give @p recipe_linkage:research_sample{RecipeLinkage:{Research:"modpack:resonant_core"}}
```

The first time the sample is inserted into a research table, the generated graph is written into the same item stack. Removing the sample does not reset progress, refund items, or reroll the route graph.

## Research Definition

```json
{
  "title": {
    "translate": "research.modpack.resonant_core",
    "fallback": "Resonant Core"
  },
  "target_stage": "resonant_core",
  "target": "amethyst",
  "min_distance_to_target": 3,
  "generation_attempts": 64,
  "initial_nodes": ["copper"],
  "nodes": [
    { "id": "copper", "item": "minecraft:copper_ingot", "count": 2, "x": 5, "y": 55 },
    { "id": "redstone", "item": "minecraft:redstone", "count": 4, "x": 30, "y": 40 },
    { "id": "quartz", "item": "minecraft:quartz", "count": 2, "x": 55, "y": 45 },
    { "id": "glass", "tag": "minecraft:smelts_to_glass", "count": 3, "x": 32, "y": 72 },
    { "id": "amethyst", "item": "minecraft:amethyst_shard", "count": 1, "x": 88, "y": 52 }
  ],
  "edges": [
    { "from": "copper", "to": "redstone" },
    { "from": "redstone", "to": "quartz", "chance": 0.8 },
    { "from": "quartz", "to": "amethyst", "chance": 0.9 },
    { "from": "copper", "to": "glass", "chance": 0.75 },
    { "from": "glass", "to": "amethyst", "chance": 0.45 }
  ]
}
```

## Field Notes

- `title`: Minecraft text component used as the research display name. Use `{ "translate": "research.<namespace>.<path>", "fallback": "Readable Name" }` when pack authors want localization.
- Localized title strings are client assets, so put them in a resource pack or KubeJS assets, for example `assets/<namespace>/lang/en_us.json` and `assets/<namespace>/lang/zh_cn.json`.
- `target_stage`: AStages stage string passed to `/astages add <player> <stage> true true`.
- `target`: node id of the visible final node.
- `initial_nodes`: optional array of node ids that are available to submit at the start. They are not pre-submitted. Multiple initial nodes are allowed. If the field is omitted or empty, one initial node is chosen randomly with `min_distance_to_target` applied when possible.
- `min_distance_to_target`: preferred minimum number of material submissions needed to finish the research from the initial available node set. The target node itself is not counted because it does not require submission. Candidates that satisfy this value are preferred; if none can satisfy it, generation falls back to the farthest valid candidate.
- `generation_attempts`: number of weighted graph candidates to generate before taking the best-scored one.
- `nodes[]`: each node must contain exactly one material key, either `item` or `tag`, following the same style as recipe ingredient objects. Use `item` for one concrete item id, or `tag` for an item tag id.
- `nodes[].count`: material count consumed when unlocking that node. Defaults to `1`.
- `nodes[].nbt`: optional SNBT string matched against submitted stacks, for example `{Damage:0}` or `{"Potion":"minecraft:water"}`. The specified NBT must be present; unrelated extra NBT on the submitted stack is allowed.
- Tag nodes accept any item in the tag. Their graph icon is the first registered item in that tag.
- `nodes[].x` and `nodes[].y`: optional percentage coordinates used as fixed layout anchors. Provide both fields to pin a node. If either field is omitted, the node is placed by the automatic layout.
- Automatic layout: generated after the route graph, initial nodes, and target are known. It places the start side on the left, the target on the right, groups nodes by graph distance, and uses a compact football-shaped vertical spread. A few cheap ordering and overlap passes are applied to reduce crossing lines and icon overlap.
- `edges[].chance`: probability that this edge appears in an individual generated sample graph. Defaults to `1.0` when omitted.

The table progress bar uses the shortest remaining number of material submissions, not raw edge distance. The final target node does not require material submission.

Example language file:

```json
{
  "research.modpack.resonant_core": "Resonant Core"
}
```

## Mod Config

The common config file includes:

- `progression.autoAwardStageOnCompletion`: defaults to `true`. When `true`, completing a research graph immediately grants the configured AStages stage.
- `progression.consumeCompletedSampleOnClaim`: defaults to `false`. When `true`, right-clicking a completed research sample to claim its stage consumes the sample.
- `client_behavior.revealCompletedGraph`: defaults to `false`. When `true`, completed samples reveal every generated node and edge in the research graph.
- `integrations.enableSophisticatedBackpackMaterials`: defaults to `true`. When `true`, material submissions can consume matching items from Sophisticated Backpacks carried by the player.
