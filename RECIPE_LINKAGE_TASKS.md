# Recipe Linkage Implementation Plan

## Product decisions

- Mod display name: 配方研究台
- English name: Recipe Linkage
- Mod id: `recipe_linkage`
- Author: AmicBeam
- Minecraft / loader: Forge 1.20.1
- Progression bridge: optional AStages, granted through the server command surface when installed.
- JEI bridge: optional client support for the JEI show-recipe key while hovering graph nodes.

## Puzzle loop

- Research samples are data-driven and modpack-provided. The base mod does not provide any research sample loot, recipe, or built-in research definition.
- A sample stores its generated graph in item NBT. Removing it from the table never rolls back progress or refreshes routes.
- The graph target is always visible. Initial submit-ready nodes can be configured by datapack with `initial_nodes`; if omitted, one initial node is chosen randomly and kept away from the target when possible.
- Generated graph layouts nudge participating nodes apart to reduce icon overlap. The research panel supports mouse-wheel zoom and left-drag panning.
- Neighboring locked nodes appear as translucent material icons with required counts. Players can hover them for full tooltips and query JEI recipes.
- Clicking an available node consumes the displayed material count from the player's inventory and unlocks that node. Materials are not refunded.
- When the unlocked component reaches the target, the sample is marked completed and the configured AStages stage is granted.
- A completed sample is not consumed. Holding it and right-clicking grants the stage again to the holder.

## Engineering checklist

- [x] Create independent Forge project structure.
- [x] Define Gradle, mods.toml, pack metadata, and translations.
- [x] Register research table block, block entity, menu, creative tab, and research sample item.
- [x] Load research definitions from datapacks.
- [x] Generate scored random graph instances from weighted candidate edges.
- [x] Persist graph state on the research sample item.
- [x] Build the research table screen with paper graph styling, tooltips, node unlocking, and JEI recipe lookup.
- [x] Add 3D block model, Minecraft-style textures, item icon, GUI background, recipe, and loot table.
- [x] Build and verify the jar.

## Build result

- Verified with `./gradlew build`.
- Verified with `./gradlew --offline build`.
- Jar: `build/libs/recipe_linkage-0.0.1+1.20.1.jar`.

## Datapack sketch

Research definitions live under:

```text
data/<namespace>/recipe_linkage/researches/<path>.json
```

Samples can be issued by loot tables, commands, KubeJS, quests, or any other pack system by setting:

```text
{RecipeLinkage:{Research:"namespace:path"}}
```
