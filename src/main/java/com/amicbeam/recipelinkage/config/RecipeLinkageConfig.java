package com.amicbeam.recipelinkage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RecipeLinkageConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec.BooleanValue REVEAL_COMPLETED_GRAPH;
    public static final ModConfigSpec.BooleanValue AUTO_AWARD_STAGE_ON_COMPLETION;
    public static final ModConfigSpec.BooleanValue CONSUME_COMPLETED_SAMPLE_ON_CLAIM;
    public static final ModConfigSpec.BooleanValue ENABLE_SOPHISTICATED_BACKPACK_MATERIALS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("progression");
        AUTO_AWARD_STAGE_ON_COMPLETION = builder
                .comment("When true, completing a research graph immediately grants the configured AStages stage.")
                .define("autoAwardStageOnCompletion", true);
        CONSUME_COMPLETED_SAMPLE_ON_CLAIM = builder
                .comment("When true, right-clicking a completed research sample to claim its stage consumes the sample.")
                .define("consumeCompletedSampleOnClaim", false);
        builder.pop();
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
