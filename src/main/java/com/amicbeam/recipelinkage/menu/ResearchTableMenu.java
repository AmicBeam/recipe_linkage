package com.amicbeam.recipelinkage.menu;

import com.amicbeam.recipelinkage.block.entity.ResearchTableBlockEntity;
import com.amicbeam.recipelinkage.item.ResearchSampleItem;
import com.amicbeam.recipelinkage.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class ResearchTableMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 1;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final ResearchTableBlockEntity table;
    private final ContainerLevelAccess access;

    public ResearchTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, blockEntity(playerInventory, data.readBlockPos()));
    }

    public ResearchTableMenu(int containerId, Inventory playerInventory, ResearchTableBlockEntity table) {
        this(containerId, playerInventory, table, ContainerLevelAccess.create(table.getLevel(), table.getBlockPos()));
    }

    private ResearchTableMenu(int containerId, Inventory playerInventory, ResearchTableBlockEntity table, ContainerLevelAccess access) {
        super(ModMenus.RESEARCH_TABLE.get(), containerId);
        this.table = table;
        this.access = access;

        addSlot(new SlotItemHandler(table.inventory(), ResearchTableBlockEntity.SLOT_SAMPLE, 14, 105));
        addPlayerInventory(playerInventory);
    }

    private static ResearchTableBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ResearchTableBlockEntity table) {
            return table;
        }
        throw new IllegalStateException("Expected Recipe Linkage research table block entity");
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 45 + col * 18, 143 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 45 + col * 18, 201));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) == table
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (source.getItem() instanceof ResearchSampleItem) {
            if (!moveItemStackTo(source, ResearchTableBlockEntity.SLOT_SAMPLE, ResearchTableBlockEntity.SLOT_SAMPLE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    public ItemStack sampleStack() {
        return table.sampleStack();
    }

    public BlockPos blockPos() {
        return table.getBlockPos();
    }
}
