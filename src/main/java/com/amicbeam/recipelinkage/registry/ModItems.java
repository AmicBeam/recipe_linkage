package com.amicbeam.recipelinkage.registry;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.item.ResearchSampleItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RecipeLinkage.MOD_ID);

    public static final RegistryObject<Item> RESEARCH_TABLE = ITEMS.register(
            "research_table",
            () -> new BlockItem(ModBlocks.RESEARCH_TABLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RESEARCH_SAMPLE = ITEMS.register(
            "research_sample",
            () -> new ResearchSampleItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

