package com.amicbeam.recipelinkage.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RecipeLinkageConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue REVEAL_COMPLETED_GRAPH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SOPHISTICATED_BACKPACK_MATERIALS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("client_behavior");
        REVEAL_COMPLETED_GRAPH = builder
                .comment("When true, completed research samples reveal every generated graph node and edge.")
                .define("revealCompletedGraph", false);
        builder.pop();
        builder.push("integrations");
        ENABLE_SOPHISTICATED_BACKPACK_MATERIALS = builder
                .comment("When true, research table material submissions can consume matching items from Sophisticated Backpacks carried by the player.")
                .define("enableSophisticatedBackpackMaterials", true);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    private RecipeLinkageConfig() {
    }
}
