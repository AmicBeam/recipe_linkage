# Vanilla Linkage Example Datapack

This datapack defines one Recipe Linkage research using only vanilla Minecraft items as graph nodes.

Install it into a world's `datapacks` folder, then run:

```mcfunction
/reload
/function vanilla_linkage:give_redstone_optics_sample
```

The function gives a bound `recipe_linkage:research_sample`. Insert it into the Recipe Research Table to generate a fixed graph on that sample.

This example sets `initial_nodes` to `["sand"]`, so sand is the first submit-ready node. Remove that field to let each generated sample pick a random initial node.

The research target stage is:

```text
redstone_optics
```

When AStages is installed, completing the graph runs the equivalent of:

```mcfunction
/astages add <player> redstone_optics true true
```
