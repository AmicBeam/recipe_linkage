package com.amicbeam.recipelinkage.client.screen;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.client.JeiBridge;
import com.amicbeam.recipelinkage.menu.ResearchTableMenu;
import com.amicbeam.recipelinkage.network.ModNetwork;
import com.amicbeam.recipelinkage.network.UnlockNodePacket;
import com.amicbeam.recipelinkage.research.ResearchGraph;
import com.amicbeam.recipelinkage.research.ResearchSampleData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(RecipeLinkage.MOD_ID, "textures/gui/research_table.png");
    private static final int TEXT = 0xFF3A2A1C;
    private static final int MUTED = 0xFF72573B;
    private static final int LINE_UNLOCKED = 0xFFB8873A;
    private static final int LINE_AVAILABLE = 0xCCB8873A;
    private static final int LINE_COMPLETED = 0xFF5DA65D;
    private static final int COMPLETED_WASH = 0x335DA65D;
    private static final int PANEL_X = 38;
    private static final int PANEL_Y = 18;
    private static final int PANEL_W = 202;
    private static final int PANEL_H = 108;
    private static final int PROGRESS_X = 9;
    private static final int PROGRESS_Y = PANEL_Y;
    private static final int PROGRESS_W = 26;
    private static final int PROGRESS_H = 70;
    private static final int NODE_SIZE = 24;
    private static final double MIN_ZOOM = 0.55D;
    private static final double MAX_ZOOM = 2.5D;
    private static final double ZOOM_STEP = 1.15D;
    private static final double DRAG_THRESHOLD = 2.0D;
    private HoveredNode hoveredNode;
    private String viewKey = "";
    private double viewScale = 1.0D;
    private double viewOffsetX;
    private double viewOffsetY;
    private boolean draggingGraph;
    private boolean graphDragMoved;
    private double graphDragStartX;
    private double graphDragStartY;
    private double graphDragStartOffsetX;
    private double graphDragStartOffsetY;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 248;
        imageHeight = 224;
        titleLabelX = 8;
        titleLabelY = 9;
        inventoryLabelX = 45;
        inventoryLabelY = 131;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        hoveredNode = null;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderGraphTooltip(guiGraphics, mouseX, mouseY);
        renderProgressTooltip(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, leftPos, topPos, imageWidth, imageHeight, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
        renderProgressPanel(guiGraphics);
        renderGraph(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        Optional<ResearchGraph> graph = ResearchSampleData.graph(menu.sampleStack());
        graph.ifPresent(value -> drawRightTitle(guiGraphics, value.title()));
        guiGraphics.drawString(font, Component.translatable("gui.recipe_linkage.sample_slot"), 8, 91, MUTED, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);

        if (graph.isEmpty()) {
            Component status = menu.sampleStack().isEmpty()
                    ? Component.translatable("gui.recipe_linkage.no_sample")
                    : Component.translatable("gui.recipe_linkage.no_graph");
            int x = PANEL_X + (PANEL_W - font.width(status)) / 2;
            int y = PANEL_Y + PANEL_H / 2 - 4;
            guiGraphics.drawString(font, status, Math.max(PANEL_X + 6, x), y, MUTED, false);
        }
    }

    private void drawRightTitle(GuiGraphics guiGraphics, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        int right = imageWidth - 8;
        int left = titleLabelX + font.width(title) + 12;
        int maxWidth = Math.max(0, right - left);
        String displayed = text;
        if (font.width(displayed) > maxWidth) {
            String ellipsis = "...";
            displayed = font.plainSubstrByWidth(displayed, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
        }
        guiGraphics.drawString(font, displayed, right - font.width(displayed), titleLabelY, MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInsidePanel(mouseX, mouseY) && ResearchSampleData.graph(menu.sampleStack()).isPresent()) {
            draggingGraph = true;
            graphDragMoved = false;
            graphDragStartX = mouseX;
            graphDragStartY = mouseY;
            graphDragStartOffsetX = viewOffsetX;
            graphDragStartOffsetY = viewOffsetY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingGraph) {
            double totalX = mouseX - graphDragStartX;
            double totalY = mouseY - graphDragStartY;
            if (Math.abs(totalX) > DRAG_THRESHOLD || Math.abs(totalY) > DRAG_THRESHOLD) {
                graphDragMoved = true;
            }
            viewOffsetX = graphDragStartOffsetX + totalX;
            viewOffsetY = graphDragStartOffsetY + totalY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingGraph) {
            draggingGraph = false;
            if (!graphDragMoved) {
                Optional<HoveredNode> clicked = hoveredNodeAt(mouseX, mouseY);
                if (clicked.isPresent() && clicked.get().available()) {
                    ModNetwork.CHANNEL.sendToServer(new UnlockNodePacket(menu.blockPos(), clicked.get().index()));
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Optional<ResearchGraph> graph = ResearchSampleData.graph(menu.sampleStack());
        if (isInsidePanel(mouseX, mouseY) && graph.isPresent()) {
            ensureView(graph.get());
            double panelLeft = leftPos + PANEL_X;
            double panelTop = topPos + PANEL_Y;
            double worldX = (mouseX - panelLeft - viewOffsetX) / viewScale;
            double worldY = (mouseY - panelTop - viewOffsetY) / viewScale;
            double factor = delta > 0.0D ? ZOOM_STEP : 1.0D / ZOOM_STEP;
            viewScale = Mth.clamp(viewScale * factor, MIN_ZOOM, MAX_ZOOM);
            viewOffsetX = mouseX - panelLeft - worldX * viewScale;
            viewOffsetY = mouseY - panelTop - worldY * viewScale;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hoveredNode != null && !hoveredNode.stack().isEmpty() && JeiBridge.tryShowRecipes(hoveredNode.stack(), keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderGraph(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Optional<ResearchGraph> optionalGraph = ResearchSampleData.graph(menu.sampleStack());
        if (optionalGraph.isEmpty()) {
            viewKey = "";
            return;
        }
        ResearchGraph graph = optionalGraph.get();
        ensureView(graph);
        if (graph.completed()) {
            renderCompletedPanel(guiGraphics);
        }
        guiGraphics.enableScissor(leftPos + PANEL_X + 1, topPos + PANEL_Y + 1, leftPos + PANEL_X + PANEL_W - 1, topPos + PANEL_Y + PANEL_H - 1);
        for (ResearchGraph.Edge edge : graph.edges()) {
            if (!shouldDrawEdge(graph, edge)) {
                continue;
            }
            double ax = nodeCenterX(graph.nodes().get(edge.a()));
            double ay = nodeCenterY(graph.nodes().get(edge.a()));
            double bx = nodeCenterX(graph.nodes().get(edge.b()));
            double by = nodeCenterY(graph.nodes().get(edge.b()));
            boolean unlocked = graph.nodes().get(edge.a()).unlocked() && graph.nodes().get(edge.b()).unlocked();
            int color = graph.completed() ? LINE_COMPLETED : unlocked ? LINE_UNLOCKED : LINE_AVAILABLE;
            drawLine(guiGraphics, ax, ay, bx, by, color);
        }

        for (int i = 0; i < graph.nodes().size(); i++) {
            if (!graph.isVisible(i)) {
                continue;
            }
            ResearchGraph.Node node = graph.nodes().get(i);
            ItemStack stack = graph.stackFor(i);
            boolean target = i == graph.targetIndex();
            boolean available = graph.isAvailable(i);
            boolean hovered = isInsidePanel(mouseX, mouseY) && isInsideNode(node, mouseX, mouseY);
            if (hovered) {
                hoveredNode = new HoveredNode(i, stack, available);
            }
            renderNode(guiGraphics, node, stack, node.unlocked(), available, target, hovered);
        }
        guiGraphics.disableScissor();
    }

    private void renderNode(GuiGraphics guiGraphics, ResearchGraph.Node node, ItemStack stack, boolean unlocked, boolean available, boolean target, boolean hovered) {
        double scaledSize = NODE_SIZE * viewScale;
        int x = Mth.floor(nodeCenterX(node) - scaledSize / 2.0D);
        int y = Mth.floor(nodeCenterY(node) - scaledSize / 2.0D);
        int frame = target ? 0xFFFFD45C : unlocked ? 0xFFB8873A : available ? 0xFFC8A86B : 0xFF8C7B63;
        int fill = target ? 0xFFF5E2A8 : unlocked ? 0xFFF2D28B : available ? 0xCCF4E3BF : 0x99E7D3AD;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale((float) viewScale, (float) viewScale, 1.0F);
        guiGraphics.fill(0, 0, NODE_SIZE, NODE_SIZE, 0xAA3B2819);
        guiGraphics.fill(1, 1, NODE_SIZE - 1, NODE_SIZE - 1, frame);
        guiGraphics.fill(2, 2, NODE_SIZE - 2, NODE_SIZE - 2, fill);
        if (target) {
            renderTargetMarker(guiGraphics);
        }
        if (hovered) {
            guiGraphics.fill(0, 0, NODE_SIZE, 2, 0xFFFFFFFF);
            guiGraphics.fill(0, NODE_SIZE - 2, NODE_SIZE, NODE_SIZE, 0xFFFFFFFF);
            guiGraphics.fill(0, 0, 2, NODE_SIZE, 0xFFFFFFFF);
            guiGraphics.fill(NODE_SIZE - 2, 0, NODE_SIZE, NODE_SIZE, 0xFFFFFFFF);
        }
        guiGraphics.renderItem(stack, 4, 4);
        if (available && !unlocked && !target) {
            guiGraphics.fill(3, 3, NODE_SIZE - 3, NODE_SIZE - 3, 0x55F6E7C8);
        }
        if (!target) {
            guiGraphics.renderItemDecorations(font, stack, 4, 4);
        }
        guiGraphics.pose().popPose();
    }

    private static void renderTargetMarker(GuiGraphics guiGraphics) {
        int dark = 0xFF6D4B1F;
        int bright = 0xFFFFF0A0;
        guiGraphics.fill(3, -3, NODE_SIZE - 3, 0, dark);
        guiGraphics.fill(5, -2, NODE_SIZE - 5, -1, bright);
        guiGraphics.fill(-3, 3, 0, NODE_SIZE - 3, dark);
        guiGraphics.fill(NODE_SIZE, 3, NODE_SIZE + 3, NODE_SIZE - 3, dark);
        guiGraphics.fill(5, 5, 9, 7, dark);
        guiGraphics.fill(15, 5, 19, 7, dark);
        guiGraphics.fill(5, 17, 9, 19, dark);
        guiGraphics.fill(15, 17, 19, 19, dark);
    }

    private boolean shouldDrawEdge(ResearchGraph graph, ResearchGraph.Edge edge) {
        if (graph.completed()) {
            return graph.isVisible(edge.a()) && graph.isVisible(edge.b());
        }
        boolean aUnlocked = graph.nodes().get(edge.a()).unlocked();
        boolean bUnlocked = graph.nodes().get(edge.b()).unlocked();
        boolean aAvailable = graph.isAvailable(edge.a());
        boolean bAvailable = graph.isAvailable(edge.b());
        boolean aTarget = edge.a() == graph.targetIndex();
        boolean bTarget = edge.b() == graph.targetIndex();
        return (aUnlocked && bUnlocked)
                || (aUnlocked && (bAvailable || bTarget))
                || (bUnlocked && (aAvailable || aTarget));
    }

    private void renderCompletedPanel(GuiGraphics guiGraphics) {
        int x0 = leftPos + PANEL_X + 3;
        int y0 = topPos + PANEL_Y + 3;
        int x1 = leftPos + PANEL_X + PANEL_W - 3;
        int y1 = topPos + PANEL_Y + PANEL_H - 3;
        guiGraphics.fill(x0, y0, x1, y1, COMPLETED_WASH);
        guiGraphics.fill(x0, y0, x1, y0 + 2, LINE_COMPLETED);
        guiGraphics.fill(x0, y1 - 2, x1, y1, LINE_COMPLETED);
        guiGraphics.fill(x0, y0, x0 + 2, y1, LINE_COMPLETED);
        guiGraphics.fill(x1 - 2, y0, x1, y1, LINE_COMPLETED);
        Component completed = Component.translatable("gui.recipe_linkage.completed");
        int labelX = x1 - font.width(completed) - 8;
        guiGraphics.drawString(font, completed, labelX, y0 + 6, LINE_COMPLETED, false);
    }

    private void renderProgressPanel(GuiGraphics guiGraphics) {
        Optional<ResearchGraph> optionalGraph = ResearchSampleData.graph(menu.sampleStack());
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        int barX = x + 8;
        int barY = y + 8;
        int barW = PROGRESS_W - 16;
        int barH = PROGRESS_H - 16;
        guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xAA3E2A18);
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xAA8D662E);
        if (optionalGraph.isEmpty()) {
            return;
        }
        ResearchGraph graph = optionalGraph.get();
        double progress = graph.progressToTarget();
        int fillHeight = Mth.floor(barH * progress);
        if (progress > 0.0D && fillHeight <= 0) {
            fillHeight = 1;
        }
        int fillColor = graph.completed() || graph.remainingSubmissionsToTarget() == 0 ? LINE_COMPLETED : LINE_UNLOCKED;
        guiGraphics.fill(barX, barY + barH - fillHeight, barX + barW, barY + barH, fillColor);
        if (fillHeight > 2) {
            guiGraphics.fill(barX + 1, barY + barH - fillHeight + 1, barX + barW - 1, barY + barH - fillHeight + 2, 0x88FFF0A0);
        }
    }

    private void renderGraphTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredNode == null || hoveredNode.stack().isEmpty()) {
            return;
        }
        guiGraphics.renderTooltip(font, hoveredNode.stack(), mouseX, mouseY);
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isInsideProgressPanel(mouseX, mouseY)) {
            return;
        }
        Optional<ResearchGraph> graph = ResearchSampleData.graph(menu.sampleStack());
        if (graph.isEmpty()) {
            return;
        }
        int remaining = graph.get().remainingSubmissionsToTarget();
        Component tooltip = remaining < 0
                ? Component.translatable("gui.recipe_linkage.progress.unknown")
                : Component.translatable("gui.recipe_linkage.progress.remaining", remaining);
        guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private void ensureView(ResearchGraph graph) {
        String key = viewKey(graph);
        if (key.equals(viewKey)) {
            return;
        }
        viewKey = key;
        fitGraph(graph);
    }

    private String viewKey(ResearchGraph graph) {
        StringBuilder builder = new StringBuilder();
        builder.append(graph.title()).append('|').append(graph.stage()).append('|')
                .append(graph.startIndex()).append('|').append(graph.targetIndex()).append('|');
        for (ResearchGraph.Node node : graph.nodes()) {
            builder.append(node.id()).append('@').append(node.x()).append(',').append(node.y()).append(';');
        }
        for (ResearchGraph.Edge edge : graph.edges()) {
            builder.append(edge.a()).append('-').append(edge.b()).append(';');
        }
        return builder.toString();
    }

    private void fitGraph(ResearchGraph graph) {
        Bounds bounds = graphBounds(graph);
        if (bounds == null) {
            viewScale = 1.0D;
            viewOffsetX = 0.0D;
            viewOffsetY = 0.0D;
            return;
        }
        double padding = 12.0D;
        double width = Math.max(1.0D, bounds.maxX() - bounds.minX());
        double height = Math.max(1.0D, bounds.maxY() - bounds.minY());
        double scale = Math.min(1.0D, Math.min((PANEL_W - padding * 2.0D) / width, (PANEL_H - padding * 2.0D) / height));
        viewScale = Mth.clamp(scale, MIN_ZOOM, MAX_ZOOM);
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5D;
        double centerY = (bounds.minY() + bounds.maxY()) * 0.5D;
        viewOffsetX = PANEL_W * 0.5D - centerX * viewScale;
        viewOffsetY = PANEL_H * 0.5D - centerY * viewScale;
    }

    private Bounds graphBounds(ResearchGraph graph) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < graph.nodes().size(); i++) {
            if (!graph.isPresent(i)) {
                continue;
            }
            ResearchGraph.Node node = graph.nodes().get(i);
            double x = nodeWorldX(node);
            double y = nodeWorldY(node);
            minX = Math.min(minX, x - NODE_SIZE * 0.5D);
            minY = Math.min(minY, y - NODE_SIZE * 0.5D);
            maxX = Math.max(maxX, x + NODE_SIZE * 0.5D);
            maxY = Math.max(maxY, y + NODE_SIZE * 0.5D);
        }
        if (!Double.isFinite(minX)) {
            return null;
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private Optional<HoveredNode> hoveredNodeAt(double mouseX, double mouseY) {
        if (!isInsidePanel(mouseX, mouseY)) {
            return Optional.empty();
        }
        Optional<ResearchGraph> optionalGraph = ResearchSampleData.graph(menu.sampleStack());
        if (optionalGraph.isEmpty()) {
            return Optional.empty();
        }
        ResearchGraph graph = optionalGraph.get();
        ensureView(graph);
        for (int i = graph.nodes().size() - 1; i >= 0; i--) {
            if (!graph.isVisible(i)) {
                continue;
            }
            ResearchGraph.Node node = graph.nodes().get(i);
            if (isInsideNode(node, mouseX, mouseY)) {
                return Optional.of(new HoveredNode(i, graph.stackFor(i), graph.isAvailable(i)));
            }
        }
        return Optional.empty();
    }

    private double nodeCenterX(ResearchGraph.Node node) {
        return leftPos + PANEL_X + viewOffsetX + nodeWorldX(node) * viewScale;
    }

    private double nodeCenterY(ResearchGraph.Node node) {
        return topPos + PANEL_Y + viewOffsetY + nodeWorldY(node) * viewScale;
    }

    private double nodeWorldX(ResearchGraph.Node node) {
        return node.x() * PANEL_W / 100.0D;
    }

    private double nodeWorldY(ResearchGraph.Node node) {
        return node.y() * PANEL_H / 100.0D;
    }

    private boolean isInsidePanel(double mouseX, double mouseY) {
        int x = leftPos + PANEL_X;
        int y = topPos + PANEL_Y;
        return mouseX >= x && mouseX < x + PANEL_W && mouseY >= y && mouseY < y + PANEL_H;
    }

    private boolean isInsideProgressPanel(double mouseX, double mouseY) {
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        return mouseX >= x && mouseX < x + PROGRESS_W && mouseY >= y && mouseY < y + PROGRESS_H;
    }

    private boolean isInsideNode(ResearchGraph.Node node, double mouseX, double mouseY) {
        double size = NODE_SIZE * viewScale;
        double x = nodeCenterX(node) - size / 2.0D;
        double y = nodeCenterY(node) - size / 2.0D;
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    private void drawLine(GuiGraphics guiGraphics, double startX, double startY, double endX, double endY, int color) {
        int x0 = Mth.floor(startX);
        int y0 = Mth.floor(startY);
        int x1 = Mth.floor(endX);
        int y1 = Mth.floor(endY);
        int thickness = Math.max(1, Mth.floor(3.0D * viewScale));
        int radius = Math.max(0, thickness / 2);
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            guiGraphics.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private record Bounds(double minX, double minY, double maxX, double maxY) {
    }

    private record HoveredNode(int index, ItemStack stack, boolean available) {
    }
}
