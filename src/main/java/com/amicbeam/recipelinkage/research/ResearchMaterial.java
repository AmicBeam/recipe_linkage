package com.amicbeam.recipelinkage.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import org.jetbrains.annotations.Nullable;

public record ResearchMaterial(ResourceLocation id, boolean tag, int count, @Nullable CompoundTag nbt) {
    private static final ResourceLocation FALLBACK_ITEM = new ResourceLocation("minecraft", "barrier");
    private static final String KIND_ITEM = "item";
    private static final String KIND_TAG = "tag";

    public ResearchMaterial {
        count = Math.max(1, count);
        if (nbt != null) {
            nbt = nbt.copy();
        }
    }

    public static ResearchMaterial item(ResourceLocation item, int count, @Nullable CompoundTag nbt) {
        return new ResearchMaterial(item, false, count, nbt);
    }

    public static ResearchMaterial tag(ResourceLocation tag, int count, @Nullable CompoundTag nbt) {
        return new ResearchMaterial(tag, true, count, nbt);
    }

    public static ResearchMaterial fallback() {
        return item(FALLBACK_ITEM, 1, null);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (tag) {
            if (!stack.is(ItemTags.create(id))) {
                return false;
            }
        } else {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || !stack.is(item)) {
                return false;
            }
        }
        return nbt == null || NbtUtils.compareNbt(nbt, stack.getTag(), true);
    }

    public ItemStack displayStack() {
        ItemStack stack = new ItemStack(displayItem(), count);
        if (nbt != null) {
            stack.setTag(nbt.copy());
        }
        return stack;
    }

    private Item displayItem() {
        if (tag) {
            ITagManager<Item> tags = ForgeRegistries.ITEMS.tags();
            if (tags != null) {
                return tags.getTag(ItemTags.create(id)).stream().findFirst().orElse(Items.BARRIER);
            }
            return Items.BARRIER;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? Items.BARRIER : item;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", this.tag ? KIND_TAG : KIND_ITEM);
        tag.putString("Id", id.toString());
        tag.putInt("Count", count);
        if (nbt != null) {
            tag.put("Nbt", nbt.copy());
        }
        return tag;
    }

    public static ResearchMaterial fromTag(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
        if (id == null) {
            id = FALLBACK_ITEM;
        }
        CompoundTag nbt = tag.contains("Nbt", Tag.TAG_COMPOUND) ? tag.getCompound("Nbt") : null;
        if (KIND_TAG.equals(tag.getString("Kind"))) {
            return tag(id, tag.getInt("Count"), nbt);
        }
        return item(id, tag.getInt("Count"), nbt);
    }
}
