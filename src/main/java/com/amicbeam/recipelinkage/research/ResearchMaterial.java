package com.amicbeam.recipelinkage.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public record ResearchMaterial(@Nullable ResourceLocation id, boolean tag, int count, @Nullable CompoundTag nbt, @Nullable Ingredient ingredient) {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final ResourceLocation FALLBACK_ITEM = ResourceLocation.fromNamespaceAndPath("minecraft", "barrier");
    private static final String KIND_ITEM = "item";
    private static final String KIND_TAG = "tag";
    private static final String KIND_INGREDIENT = "ingredient";

    public ResearchMaterial {
        count = Math.max(0, count);
        if (nbt != null) {
            nbt = nbt.copy();
        }
    }

    public static ResearchMaterial item(ResourceLocation item, int count, @Nullable CompoundTag nbt) {
        return new ResearchMaterial(item, false, count, nbt, null);
    }

    public static ResearchMaterial tag(ResourceLocation tag, int count, @Nullable CompoundTag nbt) {
        return new ResearchMaterial(tag, true, count, nbt, null);
    }

    public static ResearchMaterial ingredient(Ingredient ingredient, int count) {
        return new ResearchMaterial(null, false, count, null, ingredient);
    }

    public static ResearchMaterial fallback() {
        return item(FALLBACK_ITEM, 1, null);
    }

    public boolean legacy() {
        return ingredient == null;
    }

    public boolean legacyItem() {
        return legacy() && !tag;
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (ingredient != null) {
            return ingredient.test(stack);
        }
        if (tag) {
            if (!stack.is(ItemTags.create(id))) {
                return false;
            }
        } else {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null || !stack.is(item)) {
                return false;
            }
        }
        if (nbt == null) {
            return true;
        }
        CompoundTag stackTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return NbtUtils.compareNbt(nbt, stackTag, true);
    }

    public ItemStack displayStack() {
        if (ingredient != null) {
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length > 0 && !stacks[0].isEmpty()) {
                return stacks[0].copyWithCount(Math.max(1, count));
            }
            return new ItemStack(Items.BARRIER, Math.max(1, count));
        }
        ItemStack stack = new ItemStack(displayItem(), Math.max(1, count));
        if (nbt != null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
        return stack;
    }

    private Item displayItem() {
        if (tag) {
            return BuiltInRegistries.ITEM.getTag(ItemTags.create(id))
                    .flatMap(tag -> tag.stream().findFirst())
                    .map(Holder::value)
                    .orElse(Items.BARRIER);
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? Items.BARRIER : item;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Count", count);
        if (ingredient != null) {
            tag.putString("Kind", KIND_INGREDIENT);
            JsonElement json = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient)
                    .getOrThrow(IllegalStateException::new);
            tag.putString("Ingredient", GSON.toJson(json));
            return tag;
        }
        tag.putString("Kind", this.tag ? KIND_TAG : KIND_ITEM);
        tag.putString("Id", id.toString());
        if (nbt != null) {
            tag.put("Nbt", nbt.copy());
        }
        return tag;
    }

    public static ResearchMaterial fromTag(CompoundTag tag) {
        if (KIND_INGREDIENT.equals(tag.getString("Kind"))) {
            try {
                JsonElement json = JsonParser.parseString(tag.getString("Ingredient"));
                Ingredient ingredient = Ingredient.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(IllegalStateException::new);
                return ingredient(ingredient, tag.getInt("Count"));
            } catch (RuntimeException ex) {
                return fallback();
            }
        }
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
