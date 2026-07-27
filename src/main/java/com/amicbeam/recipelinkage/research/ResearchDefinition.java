package com.amicbeam.recipelinkage.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record ResearchDefinition(
        ResourceLocation id,
        Component title,
        String targetStage,
        String targetNode,
        int minDistanceToTarget,
        int generationAttempts,
        boolean randomInitialNodes,
        boolean activateAllInitialNodes,
        List<String> initialNodes,
        List<Node> nodes,
        List<Edge> edges
) {
    private static final int FALLBACK_ATTEMPTS = 64;

    public ResearchDefinition {
        generationAttempts = Math.max(1, generationAttempts <= 0 ? FALLBACK_ATTEMPTS : generationAttempts);
        initialNodes = List.copyOf(initialNodes);
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public Optional<Node> node(String nodeId) {
        return nodes.stream().filter(node -> node.id().equals(nodeId)).findFirst();
    }

    public int targetIndex() {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id().equals(targetNode)) {
                return i;
            }
        }
        return -1;
    }

    public Map<String, Integer> nodeIndex() {
        return nodes.stream().collect(Collectors.toUnmodifiableMap(Node::id, nodes::indexOf));
    }

    public record Node(String id, ResearchMaterial material, int x, int y, boolean fixedPosition) {
    }

    public record Edge(String from, String to, double chance) {
    }
}
