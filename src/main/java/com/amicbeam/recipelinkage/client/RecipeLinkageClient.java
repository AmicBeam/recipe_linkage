package com.amicbeam.recipelinkage.client;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.client.screen.ResearchTableScreen;
import com.amicbeam.recipelinkage.registry.ModItems;
import com.amicbeam.recipelinkage.registry.ModMenus;
import com.amicbeam.recipelinkage.research.ResearchSampleData;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = RecipeLinkage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RecipeLinkageClient {
    private RecipeLinkageClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new);
            ItemProperties.register(
                    ModItems.RESEARCH_SAMPLE.get(),
                    new ResourceLocation(RecipeLinkage.MOD_ID, "completed"),
                    (stack, level, entity, seed) -> ResearchSampleData.isCompleted(stack) ? 1.0F : 0.0F);
        });
    }
}
