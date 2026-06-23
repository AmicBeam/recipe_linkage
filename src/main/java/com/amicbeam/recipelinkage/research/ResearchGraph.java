package com.amicbeam.recipelinkage.research;

import com.amicbeam.recipelinkage.config.RecipeLinkageConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResearchGraph {
    private final Component title;
    private final String stage;
    private final int startIndex;
    private final int targetIndex;
    private final List<Integer> initialIndices;
    private boolean completed;
    private final List<Node> nodes;
    private final List<Edge> edges;

    public ResearchGraph(Component title, String stage, int startIndex, int targetIndex, List<Integer> initialIndices, boolean completed, List<Node> nodes, List<Edge> edges) {
        this.title = title;
        this.stage = stage;
        this.startIndex = startIndex;
        this.targetIndex = targetIndex;
        this.initialIndices = new ArrayList<>(initialIndices);
        this.completed = completed;
        this.nodes = new ArrayList<>(nodes);
        this.edges = new ArrayList<>(edges);
    }

    public Component title() {
        return title;
    }

    public String stage() {
        return stage;
    }

    public int startIndex() {
        return startIndex;
    }

    public int targetIndex() {
        return targetIndex;
    }

    public List<Integer> initialIndices() {
        return initialIndices;
    }

    public boolean completed() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<Node> nodes() {
        return nodes;
    }

    public List<Edge> edges() {
        return edges;
    }

    public boolean isAvailable(int nodeIndex) {
        if (nodeIndex < 0 || nodeIndex >= nodes.size() || completed || nodeIndex == targetIndex || nodes.get(nodeIndex).unlocked()) {
            return false;
        }
        if (initialIndices.contains(nodeIndex)) {
            return true;
        }
        for (Edge edge : edges) {
            int other = edge.other(nodeIndex);
            if (other >= 0 && nodes.get(other).unlocked()) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisible(int nodeIndex) {
        return isPresent(nodeIndex) && (shouldRevealCompletedGraph() || nodeIndex == targetIndex || nodes.get(nodeIndex).unlocked() || isAvailable(nodeIndex));
    }

    public boolean isPresent(int nodeIndex) {
        if (nodeIndex < 0 || nodeIndex >= nodes.size()) {
            return false;
        }
        if (nodeIndex == startIndex || nodeIndex == targetIndex || initialIndices.contains(nodeIndex)) {
            return true;
        }
        for (Edge edge : edges) {
            if (edge.a() == nodeIndex || edge.b() == nodeIndex) {
                return true;
            }
        }
        return false;
    }

    public void unlock(int nodeIndex) {
        Node node = nodes.get(nodeIndex);
        nodes.set(nodeIndex, new Node(node.id(), node.material(), node.x(), node.y(), true));
    }

    public boolean targetConnected() {
        if (!hasUnlockedNodes()) {
            return false;
        }
        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (i != targetIndex && nodes.get(i).unlocked()) {
                visited.add(i);
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (Edge edge : edges) {
                int other = edge.other(current);
                if (other == targetIndex) {
                    return true;
                }
                if (other >= 0 && nodes.get(other).unlocked() && visited.add(other)) {
                    queue.add(other);
                }
            }
        }
        return false;
    }

    public int remainingSubmissionsToTarget() {
        if (completed || targetConnected()) {
            return 0;
        }
        int[] distances = distancesFromTarget();
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < nodes.size(); i++) {
            if (i == targetIndex || !nodes.get(i).unlocked()) {
                continue;
            }
            if (distances[i] >= 0) {
                best = Math.min(best, Math.max(0, distances[i] - 1));
            }
        }
        for (int initialIndex : initialIndices) {
            if (initialIndex >= 0 && initialIndex < distances.length && !nodes.get(initialIndex).unlocked() && distances[initialIndex] >= 0) {
                best = Math.min(best, distances[initialIndex]);
            }
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    public int totalSubmissionsToTarget() {
        int[] distances = distancesFromTarget();
        int best = Integer.MAX_VALUE;
        for (int initialIndex : initialIndices) {
            if (initialIndex >= 0 && initialIndex < distances.length && distances[initialIndex] >= 0) {
                best = Math.min(best, distances[initialIndex]);
            }
        }
        if (best == Integer.MAX_VALUE && startIndex >= 0 && startIndex < distances.length && distances[startIndex] >= 0) {
            best = distances[startIndex];
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    public double progressToTarget() {
        if (completed || targetConnected()) {
            return 1.0D;
        }
        int total = totalSubmissionsToTarget();
        int remaining = remainingSubmissionsToTarget();
        if (total <= 0 || remaining < 0) {
            return 0.0D;
        }
        double progress = (total - remaining) / (double) total;
        return Math.max(0.0D, Math.min(1.0D, progress));
    }

    private boolean shouldRevealCompletedGraph() {
        return completed && RecipeLinkageConfig.REVEAL_COMPLETED_GRAPH.get();
    }

    private int[] distancesFromTarget() {
        int[] distances = new int[nodes.size()];
        java.util.Arrays.fill(distances, -1);
        if (targetIndex < 0 || targetIndex >= nodes.size()) {
            return distances;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        distances[targetIndex] = 0;
        queue.add(targetIndex);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (Edge edge : edges) {
                int other = edge.other(current);
                if (other >= 0 && distances[other] < 0) {
                    distances[other] = distances[current] + 1;
                    queue.add(other);
                }
            }
        }
        return distances;
    }

    private boolean hasUnlockedNodes() {
        for (Node node : nodes) {
            if (node.unlocked()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack stackFor(int nodeIndex) {
        return nodes.get(nodeIndex).material().displayStack();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Title", titleToJson(title));
        tag.putString("Stage", stage);
        tag.putInt("Start", startIndex);
        tag.putInt("Target", targetIndex);
        tag.putBoolean("Completed", completed);

        ListTag initialList = new ListTag();
        for (int initialIndex : initialIndices) {
            CompoundTag initialTag = new CompoundTag();
            initialTag.putInt("Index", initialIndex);
            initialList.add(initialTag);
        }
        tag.put("Initials", initialList);

        ListTag nodeList = new ListTag();
        for (Node node : nodes) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putString("Id", node.id());
            nodeTag.put("Material", node.material().toTag());
            nodeTag.putInt("X", node.x());
            nodeTag.putInt("Y", node.y());
            nodeTag.putBoolean("Unlocked", node.unlocked());
            nodeList.add(nodeTag);
        }
        tag.put("Nodes", nodeList);

        ListTag edgeList = new ListTag();
        for (Edge edge : edges) {
            CompoundTag edgeTag = new CompoundTag();
            edgeTag.putInt("A", edge.a());
            edgeTag.putInt("B", edge.b());
            edgeList.add(edgeTag);
        }
        tag.put("Edges", edgeList);
        return tag;
    }

    public static ResearchGraph fromTag(CompoundTag tag) {
        List<Node> nodes = new ArrayList<>();
        ListTag nodeList = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodeList.size(); i++) {
            CompoundTag nodeTag = nodeList.getCompound(i);
            ResearchMaterial material = nodeTag.contains("Material", Tag.TAG_COMPOUND)
                    ? ResearchMaterial.fromTag(nodeTag.getCompound("Material"))
                    : ResearchMaterial.fallback();
            nodes.add(new Node(
                    nodeTag.getString("Id"),
                    material,
                    nodeTag.getInt("X"),
                    nodeTag.getInt("Y"),
                    nodeTag.getBoolean("Unlocked")));
        }

        List<Edge> edges = new ArrayList<>();
        ListTag edgeList = tag.getList("Edges", Tag.TAG_COMPOUND);
        for (int i = 0; i < edgeList.size(); i++) {
            CompoundTag edgeTag = edgeList.getCompound(i);
            edges.add(new Edge(edgeTag.getInt("A"), edgeTag.getInt("B")));
        }
        List<Integer> initialIndices = new ArrayList<>();
        if (tag.contains("Initials", Tag.TAG_LIST)) {
            ListTag initialList = tag.getList("Initials", Tag.TAG_COMPOUND);
            for (int i = 0; i < initialList.size(); i++) {
                initialIndices.add(initialList.getCompound(i).getInt("Index"));
            }
        }
        if (initialIndices.isEmpty()) {
            initialIndices.add(tag.getInt("Start"));
        }
        return new ResearchGraph(
                titleFromTag(tag),
                tag.getString("Stage"),
                tag.getInt("Start"),
                tag.getInt("Target"),
                initialIndices,
                tag.getBoolean("Completed"),
                nodes,
                edges);
    }

    private static Component titleFromTag(CompoundTag tag) {
        String title = tag.getString("Title");
        if (title.isBlank()) {
            return Component.empty();
        }
        try {
            return ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(title))
                    .getOrThrow(JsonParseException::new);
        } catch (JsonParseException ex) {
            return Component.literal(title);
        }
    }

    private static String titleToJson(Component title) {
        JsonElement json = ComponentSerialization.CODEC
                .encodeStart(JsonOps.INSTANCE, title)
                .getOrThrow(JsonParseException::new);
        return json.toString();
    }

    public record Node(String id, ResearchMaterial material, int x, int y, boolean unlocked) {
    }

    public record Edge(int a, int b) {
        public int other(int node) {
            if (a == node) {
                return b;
            }
            if (b == node) {
                return a;
            }
            return -1;
        }
    }
}
