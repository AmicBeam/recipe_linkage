package com.amicbeam.recipelinkage.block.entity;

import com.amicbeam.recipelinkage.config.RecipeLinkageConfig;
import com.amicbeam.recipelinkage.item.ResearchSampleItem;
import com.amicbeam.recipelinkage.menu.ResearchTableMenu;
import com.amicbeam.recipelinkage.registry.ModBlockEntities;
import com.amicbeam.recipelinkage.research.ResearchSampleData;
import com.amicbeam.recipelinkage.stage.StageAwarder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_SAMPLE = 0;
    public static final int SLOT_COUNT = 1;
    private static final String TAG_INVENTORY = "Inventory";

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_SAMPLE && stack.getItem() instanceof ResearchSampleItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_SAMPLE && level instanceof ServerLevel serverLevel) {
                ItemStack sample = getStackInSlot(SLOT_SAMPLE);
                if (sample.getItem() instanceof ResearchSampleItem) {
                    ResearchSampleData.ensureGraph(sample, serverLevel);
                }
            }
            setChanged();
        }
    };

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.recipe_linkage.research_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ensureSampleGraph(serverPlayer);
        }
        return new ResearchTableMenu(containerId, playerInventory, this);
    }

    public IItemHandler inventory() {
        return inventory;
    }

    public ItemStack sampleStack() {
        return inventory.getStackInSlot(SLOT_SAMPLE);
    }

    public void ensureSampleGraph(ServerPlayer player) {
        ItemStack sample = sampleStack();
        if (sample.getItem() instanceof ResearchSampleItem && ResearchSampleData.ensureGraph(sample, player.serverLevel())) {
            inventory.setStackInSlot(SLOT_SAMPLE, sample);
            setChanged();
        }
    }

    public boolean unlockNode(ServerPlayer player, int nodeIndex) {
        ItemStack sample = sampleStack();
        if (!(sample.getItem() instanceof ResearchSampleItem)) {
            return false;
        }
        if (!ResearchSampleData.ensureGraph(sample, player.serverLevel())) {
            player.displayClientMessage(Component.translatable("message.recipe_linkage.research.missing"), true);
            return false;
        }
        ResearchSampleData.UnlockResult result = ResearchSampleData.tryUnlock(sample, nodeIndex, player);
        if (!result.success()) {
            result.message().ifPresent(message -> player.displayClientMessage(message, true));
            return false;
        }
        inventory.setStackInSlot(SLOT_SAMPLE, sample);
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.7F, 1.2F);
            if (result.completed()) {
                level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.45F);
            }
        }
        if (result.completed() && RecipeLinkageConfig.AUTO_AWARD_STAGE_ON_COMPLETION.get()) {
            StageAwarder.award(player, result.stage());
        }
        return true;
    }

    public void dropContents(Level level, BlockPos pos) {
        ItemStack stack = inventory.getStackInSlot(SLOT_SAMPLE);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_INVENTORY, inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound(TAG_INVENTORY));
    }
}
