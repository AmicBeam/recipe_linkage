package com.amicbeam.recipelinkage.data;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.research.ResearchDefinition;
import com.amicbeam.recipelinkage.research.ResearchMaterial;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ResearchManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final ResearchManager INSTANCE = new ResearchManager();
    private Map<ResourceLocation, ResearchDefinition> researches = Map.of();
    private int version;

    private ResearchManager() {
        super(GSON, "recipe_linkage/researches");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ResearchDefinition> loaded = new LinkedHashMap<>();
        files.forEach((id, element) -> {
            try {
                ResearchDefinition definition = parse(id, GsonHelper.convertToJsonObject(element, "research definition"));
                loaded.put(id, definition);
            } catch (Exception ex) {
                RecipeLinkage.LOGGER.error("Failed to load research definition {}: {}", id, ex.getMessage(), ex);
            }
        });
        researches = Collections.unmodifiableMap(loaded);
        version++;
        RecipeLinkage.LOGGER.info("Loaded {} recipe linkage researches", researches.size());
    }

    public Optional<ResearchDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(researches.get(id));
    }

    public Map<ResourceLocation, ResearchDefinition> all() {
        return researches;
    }

    public int version() {
        return version;
    }

    private static ResearchDefinition parse(ResourceLocation id, JsonObject object) {
        Component title = parseTitle(id, object);
        String targetStage = GsonHelper.getAsString(object, "target_stage");
        String targetNode = GsonHelper.getAsString(object, "target");
        int minDistance = Math.max(1, GsonHelper.getAsInt(object, "min_distance_to_target", 2));
        int attempts = Math.max(1, GsonHelper.getAsInt(object, "generation_attempts", 64));
        List<ResearchDefinition.Node> nodes = parseNodes(object);
        List<ResearchDefinition.Edge> edges = parseEdges(object);
        boolean randomInitialNodes = !object.has("initial_nodes");
        List<String> initialNodes = randomInitialNodes ? List.of() : parseInitialNodes(object);
        validate(id, targetNode, initialNodes, nodes, edges);
        return new ResearchDefinition(id, title, targetStage, targetNode, minDistance, attempts, randomInitialNodes, initialNodes, nodes, edges);
    }

    private static Component parseTitle(ResourceLocation id, JsonObject object) {
        if (!object.has("title")) {
            return Component.literal(id.toString());
        }
        try {
            return ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, object.get("title"))
                    .getOrThrow(JsonParseException::new);
        } catch (JsonParseException ex) {
            throw new JsonSyntaxException("Invalid title component: " + ex.getMessage(), ex);
        }
    }

    private static List<ResearchDefinition.Node> parseNodes(JsonObject object) {
        JsonArray array = GsonHelper.getAsJsonArray(object, "nodes");
        List<ResearchDefinition.Node> nodes = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject nodeObject = GsonHelper.convertToJsonObject(array.get(i), "nodes[" + i + "]");
            String nodeId = GsonHelper.getAsString(nodeObject, "id");
            ResearchMaterial material = parseMaterial(nodeObject, i);
            boolean fixedPosition = nodeObject.has("x") && nodeObject.has("y");
            int x = fixedPosition ? clamp(GsonHelper.getAsInt(nodeObject, "x"), 0, 100) : 50;
            int y = fixedPosition ? clamp(GsonHelper.getAsInt(nodeObject, "y"), 0, 100) : 50;
            nodes.add(new ResearchDefinition.Node(nodeId, material, x, y, fixedPosition));
        }
        return List.copyOf(nodes);
    }

    private static ResearchMaterial parseMaterial(JsonObject nodeObject, int index) {
        int count = Math.max(0, GsonHelper.getAsInt(nodeObject, "count", 1));
        boolean hasIngredient = nodeObject.has("ingredient");
        boolean hasItem = nodeObject.has("item");
        boolean hasTag = nodeObject.has("tag");
        boolean hasLegacyNbt = nodeObject.has("nbt");
        if (hasIngredient) {
            if (hasItem || hasTag || hasLegacyNbt) {
                throw new IllegalArgumentException("nodes[" + index + "] must use either ingredient or legacy item/tag/nbt fields, not both");
            }
            return ResearchMaterial.ingredient(parseIngredient(nodeObject.get("ingredient"), "nodes[" + index + "].ingredient"), count);
        }
        if (hasItem == hasTag) {
            throw new IllegalArgumentException("nodes[" + index + "] must contain exactly one of item or tag");
        }
        CompoundTag nbt = parseNbt(nodeObject, "nodes[" + index + "].nbt");
        if (hasTag) {
            return ResearchMaterial.tag(parseId(GsonHelper.getAsString(nodeObject, "tag"), "nodes[" + index + "].tag"), count, nbt);
        }
        return ResearchMaterial.item(parseId(GsonHelper.getAsString(nodeObject, "item"), "nodes[" + index + "].item"), count, nbt);
    }

    private static Ingredient parseIngredient(JsonElement element, String field) {
        try {
            return Ingredient.CODEC_NONEMPTY
                    .parse(JsonOps.INSTANCE, element)
                    .getOrThrow(message -> new JsonSyntaxException("Invalid " + field + ": " + message));
        } catch (JsonSyntaxException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new JsonSyntaxException("Invalid " + field + ": " + ex.getMessage(), ex);
        }
    }

    private static CompoundTag parseNbt(JsonObject object, String field) {
        if (!object.has("nbt")) {
            return null;
        }
        try {
            return TagParser.parseTag(GsonHelper.convertToString(object.get("nbt"), field));
        } catch (CommandSyntaxException ex) {
            throw new JsonSyntaxException("Invalid " + field + ": " + ex.getMessage());
        }
    }

    private static List<ResearchDefinition.Edge> parseEdges(JsonObject object) {
        JsonArray array = GsonHelper.getAsJsonArray(object, "edges");
        List<ResearchDefinition.Edge> edges = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonObject edgeObject = GsonHelper.convertToJsonObject(array.get(i), "edges[" + i + "]");
            String from = GsonHelper.getAsString(edgeObject, "from");
            String to = GsonHelper.getAsString(edgeObject, "to");
            double chance = GsonHelper.getAsDouble(edgeObject, "chance", 1.0D);
            if (chance < 0.0D || chance > 1.0D) {
                throw new IllegalArgumentException("edges[" + i + "].chance must be from 0.0 to 1.0");
            }
            edges.add(new ResearchDefinition.Edge(from, to, chance));
        }
        return List.copyOf(edges);
    }

    private static List<String> parseInitialNodes(JsonObject object) {
        JsonArray array = GsonHelper.getAsJsonArray(object, "initial_nodes");
        List<String> initialNodes = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            initialNodes.add(GsonHelper.convertToString(array.get(i), "initial_nodes[" + i + "]"));
        }
        return List.copyOf(initialNodes);
    }

    private static void validate(ResourceLocation id, String targetNode, List<String> initialNodes, List<ResearchDefinition.Node> nodes, List<ResearchDefinition.Edge> edges) {
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("research " + id + " requires at least two nodes");
        }
        Map<String, Integer> nodeIds = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            ResearchDefinition.Node node = nodes.get(i);
            if (node.id().isBlank()) {
                throw new IllegalArgumentException("nodes[" + i + "].id is blank");
            }
            if (nodeIds.put(node.id(), i) != null) {
                throw new IllegalArgumentException("duplicate node id " + node.id());
            }
            if (node.material().legacyItem() && !BuiltInRegistries.ITEM.containsKey(node.material().id())) {
                throw new IllegalArgumentException("node " + node.id() + " references missing item " + node.material().id());
            }
        }
        if (!nodeIds.containsKey(targetNode)) {
            throw new IllegalArgumentException("target node " + targetNode + " is not present");
        }
        Set<String> seenInitialNodes = new HashSet<>();
        for (String initialNode : initialNodes) {
            if (!nodeIds.containsKey(initialNode)) {
                throw new IllegalArgumentException("initial node " + initialNode + " is not present");
            }
            if (initialNode.equals(targetNode)) {
                throw new IllegalArgumentException("target node " + targetNode + " cannot be an initial node");
            }
            if (!seenInitialNodes.add(initialNode)) {
                throw new IllegalArgumentException("duplicate initial node " + initialNode);
            }
        }
        for (int i = 0; i < edges.size(); i++) {
            ResearchDefinition.Edge edge = edges.get(i);
            if (!nodeIds.containsKey(edge.from())) {
                throw new IllegalArgumentException("edges[" + i + "].from references missing node " + edge.from());
            }
            if (!nodeIds.containsKey(edge.to())) {
                throw new IllegalArgumentException("edges[" + i + "].to references missing node " + edge.to());
            }
            if (edge.from().equals(edge.to())) {
                throw new IllegalArgumentException("edges[" + i + "] links a node to itself");
            }
        }
    }

    private static ResourceLocation parseId(String id, String field) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            throw new IllegalArgumentException(field + " is not a valid resource location: " + id);
        }
        return location;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
