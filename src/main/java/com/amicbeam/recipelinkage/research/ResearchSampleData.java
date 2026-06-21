package com.amicbeam.recipelinkage.research;

import com.amicbeam.recipelinkage.config.RecipeLinkageConfig;
import com.amicbeam.recipelinkage.data.ResearchManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public final class ResearchSampleData {
    private static final String ROOT = "RecipeLinkage";
    private static final String TAG_RESEARCH = "Research";
    private static final String TAG_GRAPH = "Graph";
    private static final String TAG_SEED = "Seed";

    private ResearchSampleData() {
    }

    public static Optional<ResourceLocation> researchId(ItemStack stack) {
        CompoundTag root = root(stack);
        if (!root.contains(TAG_RESEARCH, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(root.getString(TAG_RESEARCH)));
    }

    public static ItemStack createBoundSample(Item item, ResourceLocation researchId) {
        ItemStack stack = new ItemStack(item);
        bind(stack, researchId);
        return stack;
    }

    public static void bind(ItemStack stack, ResourceLocation researchId) {
        CompoundTag itemTag = stack.getOrCreateTag();
        CompoundTag root = itemTag.getCompound(ROOT);
        root.putString(TAG_RESEARCH, researchId.toString());
        itemTag.put(ROOT, root);
        stack.setTag(itemTag);
    }

    public static boolean hasGraph(ItemStack stack) {
        return root(stack).contains(TAG_GRAPH, Tag.TAG_COMPOUND);
    }

    public static boolean ensureGraph(ItemStack stack, ServerLevel level) {
        CompoundTag itemTag = stack.getOrCreateTag();
        CompoundTag root = itemTag.getCompound(ROOT);
        if (root.contains(TAG_GRAPH, Tag.TAG_COMPOUND)) {
            return true;
        }
        Optional<ResourceLocation> id = researchId(stack);
        if (id.isEmpty()) {
            return false;
        }
        Optional<ResearchDefinition> definition = ResearchManager.INSTANCE.get(id.get());
        if (definition.isEmpty()) {
            return false;
        }
        long seed = root.contains(TAG_SEED, Tag.TAG_LONG) ? root.getLong(TAG_SEED) : RandomSource.create(level.getGameTime() ^ stack.hashCode()).nextLong();
        ResearchGraph graph = ResearchGraphGenerator.generate(definition.get(), seed);
        if (graph == null) {
            return false;
        }
        root.putLong(TAG_SEED, seed);
        root.put(TAG_GRAPH, graph.toTag());
        itemTag.put(ROOT, root);
        stack.setTag(itemTag);
        return true;
    }

    public static boolean isCompleted(ItemStack stack) {
        return graph(stack).map(ResearchGraph::completed).orElse(false);
    }

    public static String stage(ItemStack stack) {
        return graph(stack).map(ResearchGraph::stage).orElse("");
    }

    public static Optional<ResearchGraph> graph(ItemStack stack) {
        CompoundTag root = root(stack);
        if (!root.contains(TAG_GRAPH, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return Optional.of(ResearchGraph.fromTag(root.getCompound(TAG_GRAPH)));
    }

    public static UnlockResult tryUnlock(ItemStack stack, int nodeIndex, ServerPlayer player) {
        Optional<ResearchGraph> optionalGraph = graph(stack);
        if (optionalGraph.isEmpty()) {
            return UnlockResult.fail(Optional.empty());
        }
        ResearchGraph graph = optionalGraph.get();
        if (graph.completed()) {
            return UnlockResult.success(false, graph.stage());
        }
        if (nodeIndex < 0 || nodeIndex >= graph.nodes().size() || !graph.isAvailable(nodeIndex)) {
            return UnlockResult.fail(Optional.empty());
        }
        ItemStack cost = graph.stackFor(nodeIndex);
        if (cost.isEmpty() || !hasItems(player, cost.getItem(), cost.getCount())) {
            return UnlockResult.fail(Optional.of(Component.translatable(
                    "message.recipe_linkage.research.need_items",
                    cost.getCount(),
                    cost.getHoverName())));
        }
        consumeItems(player, cost.getItem(), cost.getCount());
        graph.unlock(nodeIndex);
        boolean completed = graph.targetConnected();
        graph.setCompleted(completed);
        writeGraph(stack, graph);
        if (completed) {
            player.displayClientMessage(Component.translatable("message.recipe_linkage.research.completed", graph.title()), true);
        }
        return UnlockResult.success(completed, graph.stage());
    }

    private static boolean hasItems(ServerPlayer player, Item item, int count) {
        return countItems(player, item, count) >= count;
    }

    private static void consumeItems(ServerPlayer player, Item item, int count) {
        int remaining = consumeInventoryItems(player.getInventory(), item, count);
        if (remaining > 0 && RecipeLinkageConfig.ENABLE_SOPHISTICATED_BACKPACK_MATERIALS.get()) {
            consumeBackpackItems(player.getInventory(), item, remaining);
        }
    }

    private static int countItems(ServerPlayer player, Item item, int limit) {
        int found = countInventoryItems(player.getInventory(), item, limit);
        if (found >= limit || !RecipeLinkageConfig.ENABLE_SOPHISTICATED_BACKPACK_MATERIALS.get()) {
            return found;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && found < limit; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isSophisticatedBackpack(stack)) {
                found += countBackpackItems(stack, item, limit - found);
            }
        }
        return found;
    }

    private static int countInventoryItems(Inventory inventory, Item item, int limit) {
        int found = 0;
        for (int slot = 0; slot < inventory.getContainerSize() && found < limit; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                found += Math.min(stack.getCount(), limit - found);
            }
        }
        return found;
    }

    private static int consumeInventoryItems(Inventory inventory, Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
        inventory.setChanged();
        return remaining;
    }

    private static int countBackpackItems(ItemStack backpack, Item item, int limit) {
        return backpack.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(handler -> countHandlerItems(handler, item, limit))
                .orElse(0);
    }

    private static int countHandlerItems(IItemHandler handler, Item item, int limit) {
        int found = 0;
        for (int slot = 0; slot < handler.getSlots() && found < limit; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                found += handler.extractItem(slot, limit - found, true).getCount();
            }
        }
        return found;
    }

    private static void consumeBackpackItems(Inventory inventory, Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isSophisticatedBackpack(stack)) {
                remaining = consumeBackpackItems(stack, item, remaining);
            }
        }
    }

    private static int consumeBackpackItems(ItemStack backpack, Item item, int count) {
        return backpack.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(handler -> consumeHandlerItems(handler, item, count))
                .orElse(count);
    }

    private static int consumeHandlerItems(IItemHandler handler, Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                remaining -= handler.extractItem(slot, remaining, false).getCount();
            }
        }
        return remaining;
    }

    private static boolean isSophisticatedBackpack(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "sophisticatedbackpacks".equals(id.getNamespace());
    }

    private static void writeGraph(ItemStack stack, ResearchGraph graph) {
        CompoundTag itemTag = stack.getOrCreateTag();
        CompoundTag root = itemTag.getCompound(ROOT);
        root.put(TAG_GRAPH, graph.toTag());
        itemTag.put(ROOT, root);
        stack.setTag(itemTag);
    }

    private static CompoundTag root(ItemStack stack) {
        CompoundTag itemTag = stack.getTag();
        if (itemTag == null) {
            return new CompoundTag();
        }
        return itemTag.getCompound(ROOT);
    }

    @SuppressWarnings("unused")
    private static Item item(ResourceLocation id) {
        return ForgeRegistries.ITEMS.getValue(id);
    }

    public record UnlockResult(boolean success, boolean completed, String stage, Optional<Component> message) {
        public static UnlockResult success(boolean completed, String stage) {
            return new UnlockResult(true, completed, stage, Optional.empty());
        }

        public static UnlockResult fail(Optional<Component> message) {
            return new UnlockResult(false, false, "", message);
        }
    }
}
