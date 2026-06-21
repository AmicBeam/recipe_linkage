package com.amicbeam.recipelinkage.registry;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.data.ResearchManager;
import com.amicbeam.recipelinkage.research.ResearchSampleData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RecipeLinkage.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.recipe_linkage.main"))
            .icon(() -> ModItems.RESEARCH_TABLE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.RESEARCH_TABLE.get());
                output.accept(ModItems.RESEARCH_SAMPLE.get());
                ResearchManager.INSTANCE.all().keySet().stream()
                        .sorted(Comparator.comparing(ResourceLocation::toString))
                        .map(id -> ResearchSampleData.createBoundSample(ModItems.RESEARCH_SAMPLE.get(), id))
                        .forEach(output::accept);
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
