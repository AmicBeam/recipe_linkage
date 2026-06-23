package com.amicbeam.recipelinkage.client;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.client.screen.ResearchTableScreen;
import com.amicbeam.recipelinkage.registry.ModItems;
import com.amicbeam.recipelinkage.registry.ModMenus;
import com.amicbeam.recipelinkage.research.ResearchSampleData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.renderer.item.ItemProperties;

@EventBusSubscriber(modid = RecipeLinkage.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RecipeLinkageClient {
    private RecipeLinkageClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModItems.RESEARCH_SAMPLE.get(),
                    RecipeLinkage.id("completed"),
                    (stack, level, entity, seed) -> ResearchSampleData.isCompleted(stack) ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new);
    }
}
