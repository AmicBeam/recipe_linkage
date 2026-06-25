![Recipe Linkage](docs/media/recipe-linkage-header-16x10.png)

# Recipe Linkage

[中文说明](README_ZH.md) | [Full Wiki](wiki.md)

**Recipe Linkage** is a data-driven research table mod for Minecraft NeoForge 1.21.1 and Forge 1.20.1 modpacks.

It adds a research table block and reusable research samples. Pack authors define material-linkage research graphs through datapacks, and players solve icon-based material puzzles to unlock configured AStages progression.

## What It Adds

- A **Recipe Research Table** block
- Reusable **Research Samples** with persistent graph progress
- Icon-based research graphs built from item relationships
- Material submission puzzles with visible targets
- AStages rewards for completed research
- Optional JEI and Sophisticated Backpacks quality-of-life support

## Research Loop

Players insert a bound research sample into the table. The sample generates a fixed graph and keeps it forever.

Available nodes show item icons and required counts. Submitting a material unlocks that node and reveals connected options. The final target node is visible and does not consume an item; research completes once the reached path connects to it.

Completed samples can be right-clicked to claim their configured stage. Server config controls whether stages are awarded automatically and whether claimed samples are consumed.

## For Pack Authors

Recipe Linkage ships no built-in research content. Research JSON files are loaded from:

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

Research nodes support recipe-style `ingredient` objects, counts, optional fixed coordinates, weighted edges, localized titles, and legacy flat `item` / `tag` / `nbt` fields for existing packs.

Loaded research definitions also appear as bound samples in the Recipe Linkage creative tab. Samples can also be issued through quests, loot, commands, KubeJS, or other progression systems.

See the [full wiki](wiki.md) for JSON examples, field notes, commands, config options, and the AStages + KubeJS recipe gate example.

## Integrations

- **AStages**: expected progression output for completed research.
- **JEI**: hover a research node and use JEI lookup for the required material.
- **Sophisticated Backpacks**: material submissions can pull matching items from carried backpacks when enabled.

## Important Notes

- Research samples are not consumed by the table.
- Removing a sample does not reset its graph.
- Submitted materials are not refunded.
- Generated routes do not reroll after the sample is created.
- The base mod provides no default research samples; packs must provide them through datapacks and their own delivery flow.
