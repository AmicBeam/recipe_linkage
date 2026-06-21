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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RecipeLinkage.MOD_ID)
public class RecipeLinkage {
    public static final String MOD_ID = "recipe_linkage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RecipeLinkage() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
        ModNetwork.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RecipeLinkageConfig.COMMON_SPEC);

        MinecraftForge.EVENT_BUS.addListener(this::addReloadListeners);
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ResearchManager.INSTANCE);
    }
}
