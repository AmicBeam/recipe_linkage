package com.amicbeam.recipelinkage.research;

import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchGraphGenerator {
    private static final double PANEL_WIDTH = 202.0D;
    private static final double PANEL_HEIGHT = 108.0D;
    private static final double AUTO_MARGIN_X = 18.0D;
    private static final double AUTO_MARGIN_Y = 18.0D;
    private static final double NODE_SPACING_X = 32.0D;
    private static final double NODE_SPACING_Y = 32.0D;
    private static final double RELAX_STRENGTH = 0.58D;
    private static final double ANCHOR_STRENGTH = 0.015D;
    private static final int LAYOUT_ITERATIONS = 96;
    private static final int LAYOUT_FINAL_ITERATIONS = 48;
    private static final int ORDERING_SWEEPS = 4;

    private ResearchGraphGenerator() {
    }

    public static ResearchGraph generate(ResearchDefinition definition, long seed) {
        RandomSource random = RandomSource.create(seed);
        Candidate best = null;
        for (int attempt = 0; attempt < definition.generationAttempts(); attempt++) {
            Candidate candidate = attempt(definition, random);
            if (candidate != null && isBetter(candidate, best)) {
                best = candidate;
            }
        }
        if (best == null || !best.meetsMinDistance()) {
            Candidate fallback = fallback(definition);
            if (fallback != null && isBetter(fallback, best)) {
                best = fallback;
            }
        }
        if (best == null) {
            return null;
        }
        return best.toGraph(definition);
    }

    private static boolean isBetter(Candidate candidate, Candidate best) {
        if (best == null) {
            return true;
        }
        if (candidate.meetsMinDistance() != best.meetsMinDistance()) {
            return candidate.meetsMinDistance();
        }
        if (!candidate.meetsMinDistance() && candidate.minSubmissionsToTarget() != best.minSubmissionsToTarget()) {
            return candidate.minSubmissionsToTarget() > best.minSubmissionsToTarget();
        }
        return candidate.score() > best.score();
    }

    private static Candidate attempt(ResearchDefinition definition, RandomSource random) {
        Map<String, Integer> index = definition.nodeIndex();
        List<ResearchGraph.Edge> selected = new ArrayList<>();
        for (ResearchDefinition.Edge edge : definition.edges()) {
            if (edge.chance() >= 1.0D || random.nextDouble() < edge.chance()) {
                selected.add(new ResearchGraph.Edge(index.get(edge.from()), index.get(edge.to())));
            }
        }
        return score(definition, selected, random);
    }

    private static Candidate fallback(ResearchDefinition definition) {
        Map<String, Integer> index = definition.nodeIndex();
        List<ResearchGraph.Edge> allEdges = definition.edges().stream()
                .map(edge -> new ResearchGraph.Edge(index.get(edge.from()), index.get(edge.to())))
                .toList();
        return score(definition, allEdges, RandomSource.create(0L));
    }

    private static Candidate score(ResearchDefinition definition, List<ResearchGraph.Edge> edges, RandomSource random) {
        int target = definition.targetIndex();
        if (target < 0 || edges.isEmpty()) {
            return null;
        }
        List<List<Integer>> adjacency = adjacency(definition.nodes().size(), edges);
        int[] distance = distances(adjacency, target);
        StartSelection startSelection = selectStarts(definition, distance, random);
        if (startSelection == null) {
            return null;
        }
        int start = startSelection.startIndex();
        List<Integer> initialNodes = startSelection.initialNodes();
        int reachable = 0;
        int deadEnds = 0;
        int usefulBranches = 0;
        int initialDistance = 0;
        int minSubmissionsToTarget = Integer.MAX_VALUE;
        for (int initialNode : initialNodes) {
            initialDistance += Math.max(0, distance[initialNode]);
            minSubmissionsToTarget = Math.min(minSubmissionsToTarget, distance[initialNode]);
        }
        if (!initialNodes.isEmpty()) {
            initialDistance /= initialNodes.size();
        } else {
            initialDistance = Math.max(0, distance[start]);
            minSubmissionsToTarget = distance[start];
        }
        if (minSubmissionsToTarget == Integer.MAX_VALUE) {
            minSubmissionsToTarget = -1;
        }
        for (int i = 0; i < distance.length; i++) {
            if (distance[i] < 0) {
                continue;
            }
            reachable++;
            int degree = adjacency.get(i).size();
            if (!initialNodes.contains(i) && i != start && i != target && degree <= 1) {
                deadEnds++;
            }
            if (degree >= 3) {
                usefulBranches++;
            }
        }
        int cycles = Math.max(0, edges.size() - reachable + 1);
        int overlapPenalty = layoutOverlapPenalty(definition, edges, adjacency, start, target, presentNodes(edges, initialNodes, start, target));
        int score = initialDistance * 12 + reachable * 4 + usefulBranches * 6 + cycles * 10 + edges.size() * 2 - deadEnds * 14 - overlapPenalty;
        return new Candidate(start, target, initialNodes, edges, score, minSubmissionsToTarget, minSubmissionsToTarget >= definition.minDistanceToTarget());
    }

    private static StartSelection selectStarts(ResearchDefinition definition, int[] distance, RandomSource random) {
        Map<String, Integer> index = definition.nodeIndex();
        if (!definition.randomInitialNodes()) {
            List<Integer> initialNodes = definition.initialNodes().stream().map(index::get).toList();
            if (!initialNodes.isEmpty()) {
                for (Integer initialNode : initialNodes) {
                    if (initialNode == null || initialNode < 0 || initialNode >= distance.length || distance[initialNode] < 0) {
                        return null;
                    }
                }
                return new StartSelection(initialNodes.get(0), initialNodes);
            }
        }

        List<Integer> starts = new ArrayList<>();
        int target = definition.targetIndex();
        for (int i = 0; i < distance.length; i++) {
            if (i != target && distance[i] >= definition.minDistanceToTarget()) {
                starts.add(i);
            }
        }
        if (starts.isEmpty()) {
            for (int i = 0; i < distance.length; i++) {
                if (i != target && distance[i] > 0) {
                    starts.add(i);
                }
            }
        }
        if (starts.isEmpty()) {
            return null;
        }
        starts.sort(Comparator.<Integer>comparingInt(node -> distance[node]).reversed());
        int start = starts.get(Math.min(starts.size() - 1, random.nextInt(Math.min(3, starts.size()))));
        return new StartSelection(start, List.of(start));
    }

    private static List<List<Integer>> adjacency(int nodeCount, List<ResearchGraph.Edge> edges) {
        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (ResearchGraph.Edge edge : edges) {
            if (edge.a() < 0 || edge.b() < 0 || edge.a() >= nodeCount || edge.b() >= nodeCount) {
                continue;
            }
            adjacency.get(edge.a()).add(edge.b());
            adjacency.get(edge.b()).add(edge.a());
        }
        return adjacency;
    }

    private static int[] distances(List<List<Integer>> adjacency, int target) {
        int[] distance = new int[adjacency.size()];
        java.util.Arrays.fill(distance, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        distance[target] = 0;
        queue.add(target);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (int other : adjacency.get(current)) {
                if (distance[other] < 0) {
                    distance[other] = distance[current] + 1;
                    queue.add(other);
                }
            }
        }
        return distance;
    }

    private static Set<Integer> presentNodes(List<ResearchGraph.Edge> edges, List<Integer> initialNodes, int start, int target) {
        Set<Integer> present = new HashSet<>();
        present.add(start);
        present.add(target);
        present.addAll(initialNodes);
        for (ResearchGraph.Edge edge : edges) {
            present.add(edge.a());
            present.add(edge.b());
        }
        return present;
    }

    private static int layoutOverlapPenalty(ResearchDefinition definition, List<ResearchGraph.Edge> edges, List<List<Integer>> adjacency, int start, int target, Set<Integer> present) {
        int penalty = 0;
        List<Integer> nodes = new ArrayList<>(present);
        Map<Integer, Point> anchors = layoutAnchors(definition, edges, adjacency, start, target, present);
        for (int i = 0; i < nodes.size(); i++) {
            Point a = anchors.getOrDefault(nodes.get(i), point(definition.nodes().get(nodes.get(i))));
            for (int j = i + 1; j < nodes.size(); j++) {
                Point b = anchors.getOrDefault(nodes.get(j), point(definition.nodes().get(nodes.get(j))));
                double overlapX = NODE_SPACING_X - Math.abs(a.x() - b.x());
                double overlapY = NODE_SPACING_Y - Math.abs(a.y() - b.y());
                if (overlapX > 0.0D && overlapY > 0.0D) {
                    penalty += (int) Math.ceil(overlapX + overlapY);
                }
            }
        }
        return penalty;
    }

    private static Map<Integer, Point> relaxedLayout(ResearchDefinition definition, List<ResearchGraph.Edge> edges, int start, int target, Set<Integer> present) {
        List<List<Integer>> adjacency = adjacency(definition.nodes().size(), edges);
        Map<Integer, MutablePoint> points = new HashMap<>();
        Map<Integer, Point> anchors = layoutAnchors(definition, edges, adjacency, start, target, present);
        for (int index : present) {
            Point point = anchors.getOrDefault(index, point(definition.nodes().get(index)));
            points.put(index, new MutablePoint(point.x(), point.y()));
        }

        List<Integer> ids = new ArrayList<>(present);
        ids.sort(Integer::compareTo);
        for (int iteration = 0; iteration < LAYOUT_ITERATIONS; iteration++) {
            boolean moved = pushOverlaps(points, ids);

            for (int id : ids) {
                MutablePoint point = points.get(id);
                Point anchor = anchors.get(id);
                point.x += (anchor.x() - point.x) * ANCHOR_STRENGTH;
                point.y += (anchor.y() - point.y) * ANCHOR_STRENGTH;
            }
            if (!moved) {
                break;
            }
        }
        for (int iteration = 0; iteration < LAYOUT_FINAL_ITERATIONS; iteration++) {
            if (!pushOverlaps(points, ids)) {
                break;
            }
        }

        Map<Integer, Point> result = new HashMap<>();
        for (Map.Entry<Integer, MutablePoint> entry : points.entrySet()) {
            result.put(entry.getKey(), new Point(entry.getValue().x, entry.getValue().y));
        }
        return result;
    }

    private static Map<Integer, Point> layoutAnchors(ResearchDefinition definition, List<ResearchGraph.Edge> edges, List<List<Integer>> adjacency, int start, int target, Set<Integer> present) {
        if (!hasAutoPosition(definition, present)) {
            Map<Integer, Point> explicit = new HashMap<>();
            for (int index : present) {
                explicit.put(index, point(definition.nodes().get(index)));
            }
            return explicit;
        }

        int[] fromStart = distances(adjacency, start);
        int[] toTarget = distances(adjacency, target);
        int pathLength = fromStart[target] > 0 ? fromStart[target] : Math.max(1, farthestReachable(toTarget));
        Map<Integer, Integer> layers = autoLayers(present, fromStart, toTarget, pathLength);
        Map<Integer, List<Integer>> layerGroups = new HashMap<>();
        for (int index : present) {
            ResearchDefinition.Node node = definition.nodes().get(index);
            if (node.fixedPosition()) {
                continue;
            }
            layerGroups.computeIfAbsent(layers.get(index), ignored -> new ArrayList<>()).add(index);
        }
        orderLayers(layerGroups, adjacency);

        Map<Integer, Point> anchors = new HashMap<>();
        for (int index : present) {
            ResearchDefinition.Node node = definition.nodes().get(index);
            if (node.fixedPosition()) {
                anchors.put(index, point(node));
            }
        }

        double usableWidth = PANEL_WIDTH - AUTO_MARGIN_X * 2.0D;
        double centerY = PANEL_HEIGHT * 0.5D;
        double usableHalfHeight = Math.max(1.0D, PANEL_HEIGHT * 0.5D - AUTO_MARGIN_Y);
        for (Map.Entry<Integer, List<Integer>> entry : layerGroups.entrySet()) {
            int layer = entry.getKey();
            List<Integer> nodes = entry.getValue();
            double progress = pathLength <= 0 ? 0.5D : layer / (double) pathLength;
            progress = clamp(progress, 0.0D, 1.0D);
            double x = AUTO_MARGIN_X + usableWidth * progress;
            double football = Math.sin(Math.PI * progress);
            double maxSpread = Math.max(4.0D, usableHalfHeight * (0.18D + 0.82D * football));
            double spacing = nodes.size() <= 1 ? 0.0D : Math.min(NODE_SPACING_Y, (maxSpread * 2.0D) / (nodes.size() - 1));
            for (int i = 0; i < nodes.size(); i++) {
                double lane = i - (nodes.size() - 1) * 0.5D;
                double y = centerY + lane * spacing;
                anchors.put(nodes.get(i), new Point(x, clamp(y, AUTO_MARGIN_Y, PANEL_HEIGHT - AUTO_MARGIN_Y)));
            }
        }
        return anchors;
    }

    private static boolean hasAutoPosition(ResearchDefinition definition, Set<Integer> present) {
        for (int index : present) {
            if (!definition.nodes().get(index).fixedPosition()) {
                return true;
            }
        }
        return false;
    }

    private static int farthestReachable(int[] distance) {
        int farthest = 1;
        for (int value : distance) {
            if (value > farthest) {
                farthest = value;
            }
        }
        return farthest;
    }

    private static Map<Integer, Integer> autoLayers(Set<Integer> present, int[] fromStart, int[] toTarget, int pathLength) {
        Map<Integer, Integer> layers = new HashMap<>();
        for (int index : present) {
            int layer;
            int startDistance = fromStart[index];
            int targetDistance = toTarget[index];
            if (startDistance >= 0 && targetDistance >= 0) {
                double progress = startDistance / (double) Math.max(1, startDistance + targetDistance);
                layer = (int) Math.round(progress * pathLength);
            } else if (startDistance >= 0) {
                layer = Math.min(pathLength, startDistance);
            } else if (targetDistance >= 0) {
                layer = Math.max(0, pathLength - targetDistance);
            } else {
                layer = pathLength / 2;
            }
            layers.put(index, Math.max(0, Math.min(pathLength, layer)));
        }
        return layers;
    }

    private static void orderLayers(Map<Integer, List<Integer>> layerGroups, List<List<Integer>> adjacency) {
        Map<Integer, Double> lanes = new HashMap<>();
        List<Integer> layerIds = new ArrayList<>(layerGroups.keySet());
        layerIds.sort(Integer::compareTo);
        for (int layer : layerIds) {
            List<Integer> nodes = layerGroups.get(layer);
            nodes.sort(Integer::compareTo);
            updateLanes(lanes, nodes);
        }
        for (int sweep = 0; sweep < ORDERING_SWEEPS; sweep++) {
            for (int layer : layerIds) {
                sortLayer(layerGroups.get(layer), lanes, adjacency);
                updateLanes(lanes, layerGroups.get(layer));
            }
            for (int i = layerIds.size() - 1; i >= 0; i--) {
                int layer = layerIds.get(i);
                sortLayer(layerGroups.get(layer), lanes, adjacency);
                updateLanes(lanes, layerGroups.get(layer));
            }
        }
    }

    private static void sortLayer(List<Integer> nodes, Map<Integer, Double> lanes, List<List<Integer>> adjacency) {
        nodes.sort(Comparator.<Integer>comparingDouble(node -> neighborLane(node, lanes, adjacency)).thenComparingInt(Integer::intValue));
    }

    private static double neighborLane(int node, Map<Integer, Double> lanes, List<List<Integer>> adjacency) {
        double total = 0.0D;
        int count = 0;
        for (int other : adjacency.get(node)) {
            Double lane = lanes.get(other);
            if (lane != null) {
                total += lane;
                count++;
            }
        }
        return count == 0 ? lanes.getOrDefault(node, 0.0D) : total / count;
    }

    private static void updateLanes(Map<Integer, Double> lanes, List<Integer> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            lanes.put(nodes.get(i), i - (nodes.size() - 1) * 0.5D);
        }
    }

    private static boolean pushOverlaps(Map<Integer, MutablePoint> points, List<Integer> ids) {
        boolean moved = false;
        for (int i = 0; i < ids.size(); i++) {
            MutablePoint a = points.get(ids.get(i));
            for (int j = i + 1; j < ids.size(); j++) {
                MutablePoint b = points.get(ids.get(j));
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                if (Math.abs(dx) < 0.01D && Math.abs(dy) < 0.01D) {
                    dx = ((ids.get(j) * 37 + ids.get(i) * 11) % 2 == 0) ? 1.0D : -1.0D;
                    dy = ((ids.get(j) * 17 + ids.get(i) * 23) % 2 == 0) ? 1.0D : -1.0D;
                }
                double overlapX = NODE_SPACING_X - Math.abs(dx);
                double overlapY = NODE_SPACING_Y - Math.abs(dy);
                if (overlapX <= 0.0D || overlapY <= 0.0D) {
                    continue;
                }
                moved = true;
                if (overlapX < overlapY) {
                    double push = overlapX * 0.5D * RELAX_STRENGTH;
                    double direction = dx < 0.0D ? -1.0D : 1.0D;
                    a.x -= direction * push;
                    b.x += direction * push;
                } else {
                    double push = overlapY * 0.5D * RELAX_STRENGTH;
                    double direction = dy < 0.0D ? -1.0D : 1.0D;
                    a.y -= direction * push;
                    b.y += direction * push;
                }
            }
        }
        return moved;
    }

    private static Point point(ResearchDefinition.Node node) {
        return new Point(node.x() * PANEL_WIDTH / 100.0D, node.y() * PANEL_HEIGHT / 100.0D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int percentX(Point point) {
        return (int) Math.round(point.x() * 100.0D / PANEL_WIDTH);
    }

    private static int percentY(Point point) {
        return (int) Math.round(point.y() * 100.0D / PANEL_HEIGHT);
    }

    private record StartSelection(int startIndex, List<Integer> initialNodes) {
    }

    private record Point(double x, double y) {
    }

    private static final class MutablePoint {
        private double x;
        private double y;

        private MutablePoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private record Candidate(int start, int target, List<Integer> initialNodes, List<ResearchGraph.Edge> edges, int score, int minSubmissionsToTarget, boolean meetsMinDistance) {
        ResearchGraph toGraph(ResearchDefinition definition) {
            Set<Integer> present = presentNodes(edges, initialNodes, start, target);
            Map<Integer, Point> layout = relaxedLayout(definition, edges, start, target, present);
            List<ResearchGraph.Node> nodes = new ArrayList<>();
            for (int i = 0; i < definition.nodes().size(); i++) {
                ResearchDefinition.Node node = definition.nodes().get(i);
                Point point = layout.getOrDefault(i, point(node));
                // Keep isolated nodes in the save data for layout stability, but they remain invisible.
                nodes.add(new ResearchGraph.Node(node.id(), node.material(), percentX(point), percentY(point), false));
            }
            return new ResearchGraph(definition.title(), definition.targetStage(), start, target, initialNodes, false, nodes, edges);
        }
    }
}
