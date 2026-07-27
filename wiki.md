# Recipe Linkage Wiki

[README](README.md) | [中文 Wiki](wiki_zh.md)

This page is the pack-author reference for Recipe Linkage. It covers datapack paths, sample delivery, research JSON fields, config options, and examples.

## Research Flow

1. A pack gives the player a bound research sample.
2. The player inserts it into the Recipe Research Table.
3. The sample generates one fixed graph and stores it on the item.
4. The player submits materials to unlock visible nodes.
5. The research completes when unlocked nodes connect to the visible target node.
6. Recipe Linkage grants or lets the player claim the configured AStages stage, depending on config.

The target node never consumes an item. Removing a sample does not reset progress, refund materials, or reroll the graph.

## Datapack Path

Research JSON files are loaded from:

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

Loaded research definitions also appear as ready-to-use bound samples in the Recipe Linkage creative tab.

## Giving Bound Samples

NeoForge 1.21.1 uses `minecraft:custom_data`:

```mcfunction
/give @p recipe_linkage:research_sample[minecraft:custom_data={RecipeLinkage:{Research:"modpack:basic_optics"}}]
```

Forge 1.20.1 uses legacy item NBT:

```mcfunction
/give @p recipe_linkage:research_sample{RecipeLinkage:{Research:"modpack:basic_optics"}}
```

Packs can also distribute samples through quests, loot tables, KubeJS, or other progression systems.

## Config

Pack and server owners can tune behavior in `config/recipe_linkage-common.toml`:

- `autoAwardStageOnCompletion`: default `true`, grants the configured stage when research is completed.
- `consumeCompletedSampleOnClaim`: default `false`, controls whether right-click claiming consumes a completed sample.
- `revealCompletedGraph`: default `false`, controls whether completed research reveals the full generated graph.
- `enableSophisticatedBackpackMaterials`: default `true`, allows material submissions to pull from carried Sophisticated Backpacks.

## Research JSON Fields

- `title`: optional Minecraft text component used as the research display name. Use `{ "translate": "research.<namespace>.<path>", "fallback": "Readable Name" }` for localization.
- `target_stage`: AStages stage string passed to `/astages add <player> <stage> false false false`.
- `target`: node id of the visible final node.
- `initial_nodes`: optional array of candidate node ids that can become available to submit at the start. They are not pre-submitted. If multiple nodes are listed, one reachable node from the list is chosen randomly for each generated sample graph by default.
- `activate_all_initial_nodes`: optional boolean, defaults to `false`. When `true`, every node listed in `initial_nodes` is available at the start, matching the old multi-start behavior.
- `min_distance_to_target`: preferred minimum number of material submissions needed to finish from the initial available node set. The target node itself is not counted.
- `generation_attempts`: number of weighted graph candidates to generate before taking the best-scored one.
- `nodes[].ingredient`: preferred material format. It accepts recipe-style item ingredients, such as `{ "item": "minecraft:glass" }`, `{ "tag": "minecraft:sand" }`, arrays like `[ { "item": "minecraft:coal" }, { "item": "minecraft:charcoal" } ]`, and registered custom ingredient types.
- Legacy `nodes[].item`, `nodes[].tag`, and `nodes[].nbt`: still supported for existing packs. A single node must use either `ingredient` or the legacy flat fields, but different nodes in the same research JSON may mix both styles.
- `nodes[].count`: material count consumed when unlocking that node. Defaults to `1`. Use `0` for a possession check: the player must have a matching item in inventory or supported backpack, but the item is not consumed.
- Legacy `nodes[].nbt`: optional SNBT string matched against the submitted stack's item data. For new `ingredient` nodes, prefer the loader's own custom ingredient format instead.
- `nodes[].x` and `nodes[].y`: optional percentage coordinates used as fixed layout anchors. Provide both fields to pin a node. If either field is omitted, the node is placed by automatic layout.
- `edges[].from` and `edges[].to`: node ids connected by this edge.
- `edges[].chance`: probability that this edge appears in an individual generated sample graph. Defaults to `1.0` when omitted.

Automatic layout runs after the route graph, initial nodes, and target are known. It places the start side on the left, the target on the right, groups nodes by graph distance, and uses a compact football-shaped spread.

The table progress bar uses the shortest remaining number of material submissions, not raw edge distance.

## AStages + KubeJS Recipe Gate Example

This example binds the redstone comparator recipe to the same `target_stage: "basic_optics"` used by the research JSON. Recipe Linkage grants that AStages stage when the research is completed; the AStages KubeJS API makes the recipe require that stage.

Place this script in `kubejs/server_scripts/recipe_linkage_stages.js`:

```js
// Requires: AStages, KubeJS
AStages.addRestrictionForRecipe(
  'recipe_linkage:basic_optics_comparator',
  'basic_optics',
  'minecraft:crafting',
  'minecraft:comparator'
)
```

If you use the full example below with `target_stage: "redstone_lens"`, replace `basic_optics` with `redstone_lens`.

## Minimal Auto-Layout Example

This example omits all `x` and `y` fields. Recipe Linkage will automatically place the graph with the start side on the left and the target on the right.

Save as `data/modpack/recipe_linkage/researches/basic_optics.json`:

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

If `initial_nodes` is omitted, the mod chooses a valid initial node when the sample graph is generated. If multiple `initial_nodes` are listed, generated samples randomly activate one of the reachable listed nodes unless `activate_all_initial_nodes` is set to `true`. If `chance` is omitted on an edge, it defaults to `1.0`.

## Fully Customized Example

This example uses a localized title, fixed coordinates, multiple initial nodes, recipe-style `ingredient` inputs, an OR ingredient, and weighted edges. The minimal `item` / `tag` node fields remain supported, and different nodes in the same research JSON may mix the old flat style and the `ingredient` style.

Save as `data/modpack/recipe_linkage/researches/redstone_lens.json`:

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
  "activate_all_initial_nodes": true,
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

Add the title translation in a resource pack or KubeJS assets, for example `assets/modpack/lang/en_us.json`:

```json
{
  "research.modpack.redstone_lens": "Redstone Lens"
}
```
