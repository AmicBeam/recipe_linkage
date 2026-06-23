package com.amicbeam.recipelinkage.registry;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.item.ResearchSampleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RecipeLinkage.MOD_ID);

    public static final DeferredHolder<Item, BlockItem> RESEARCH_TABLE = ITEMS.register(
            "research_table",
            () -> new BlockItem(ModBlocks.RESEARCH_TABLE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, ResearchSampleItem> RESEARCH_SAMPLE = ITEMS.register(
            "research_sample",
            () -> new ResearchSampleItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
