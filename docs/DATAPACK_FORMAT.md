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
  "title": "Resonant Core",
  "target_stage": "resonant_core",
  "target": "amethyst",
  "min_distance_to_target": 3,
  "generation_attempts": 64,
  "initial_nodes": ["copper"],
  "nodes": [
    { "id": "copper", "item": "minecraft:copper_ingot", "count": 2, "x": 5, "y": 55 },
    { "id": "redstone", "item": "minecraft:redstone", "count": 4, "x": 30, "y": 40 },
    { "id": "quartz", "item": "minecraft:quartz", "count": 2, "x": 55, "y": 45 },
    { "id": "glass", "item": "minecraft:glass", "count": 3, "x": 32, "y": 72 },
    { "id": "amethyst", "item": "minecraft:amethyst_shard", "count": 1, "x": 88, "y": 52 }
  ],
  "edges": [
    { "from": "copper", "to": "redstone", "chance": 1.0 },
    { "from": "redstone", "to": "quartz", "chance": 0.8 },
    { "from": "quartz", "to": "amethyst", "chance": 0.9 },
    { "from": "copper", "to": "glass", "chance": 0.75 },
    { "from": "glass", "to": "amethyst", "chance": 0.45 }
  ]
}
```

## Field Notes

- `target_stage`: AStages stage string passed to `/astages add <player> <stage> true true`.
- `target`: node id of the visible final node.
- `initial_nodes`: optional array of node ids that are available to submit at the start. They are not pre-submitted. Multiple initial nodes are allowed. If the field is omitted or empty, one initial node is chosen randomly with `min_distance_to_target` applied when possible.
- `min_distance_to_target`: preferred minimum graph distance between the randomly generated start node and target. If no candidate satisfies it, generation falls back to the farthest valid node. Configured `initial_nodes` are trusted as pack-authored starts.
- `generation_attempts`: number of weighted graph candidates to generate before taking the best-scored one.
- `nodes[].x` and `nodes[].y`: percentage coordinates used as the initial layout. Generated samples may nudge participating nodes apart to prevent icon overlap.
- `nodes[].count`: item count consumed when unlocking that node.
- `edges[].chance`: probability that this edge appears in an individual generated sample graph.

The table progress bar uses the shortest remaining number of item submissions, not raw edge distance. The final target node does not require item submission.

## Mod Config

The common config file includes:

- `client_behavior.revealCompletedGraph`: defaults to `false`. When `true`, completed samples reveal every generated node and edge in the research graph.
