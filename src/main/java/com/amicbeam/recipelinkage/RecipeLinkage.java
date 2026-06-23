package com.amicbeam.recipelinkage;

import com.amicbeam.recipelinkage.config.RecipeLinkageConfig;
import com.amicbeam.recipelinkage.data.ResearchManager;
import com.amicbeam.recipelinkage.network.ModNetwork;
import com.amicbeam.recipelinkage.registry.ModBlockEntities;
import com.amicbeam.recipelinkage.registry.ModBlocks;
import com.amicbeam.recipelinkage.registry.ModCreativeTabs;
import com.amicbeam.recipelinkage.registry.ModItems;
import com.amicbeam.recipelinkage.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(RecipeLinkage.MOD_ID)
public class RecipeLinkage {
    public static final String MOD_ID = "recipe_linkage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RecipeLinkage(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(this::registerCapabilities);

        container.registerConfig(ModConfig.Type.COMMON, RecipeLinkageConfig.COMMON_SPEC);

        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ResearchManager.INSTANCE);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.RESEARCH_TABLE.get(),
                (table, side) -> table.inventory());
    }
}
