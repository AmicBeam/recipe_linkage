package com.amicbeam.recipelinkage.item;

import com.amicbeam.recipelinkage.config.RecipeLinkageConfig;
import com.amicbeam.recipelinkage.data.ResearchManager;
import com.amicbeam.recipelinkage.research.ResearchSampleData;
import com.amicbeam.recipelinkage.stage.StageAwarder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ResearchSampleItem extends Item {
    public ResearchSampleItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<ResourceLocation> researchId = ResearchSampleData.researchId(stack);
        if (researchId.isPresent()) {
            Component title = ResearchManager.INSTANCE.get(researchId.get())
                    .map(definition -> definition.title())
                    .orElse(Component.literal(researchId.get().toString()));
            return Component.translatable("item.recipe_linkage.research_sample.bound", title);
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && ResearchSampleData.isCompleted(stack)) {
            String stage = ResearchSampleData.stage(stack);
            if (StageAwarder.award(serverPlayer, stage)) {
                serverPlayer.displayClientMessage(Component.translatable("message.recipe_linkage.research.claimed", stage), true);
                if (RecipeLinkageConfig.CONSUME_COMPLETED_SAMPLE_ON_CLAIM.get() && !serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Optional<ResourceLocation> researchId = ResearchSampleData.researchId(stack);
        if (researchId.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.recipe_linkage.sample.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.recipe_linkage.sample.research", researchId.get().toString()).withStyle(ChatFormatting.GRAY));
        if (ResearchSampleData.hasGraph(stack)) {
            tooltip.add(Component.translatable(ResearchSampleData.isCompleted(stack)
                    ? "tooltip.recipe_linkage.sample.completed"
                    : "tooltip.recipe_linkage.sample.incomplete").withStyle(ResearchSampleData.isCompleted(stack) ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }
        if (ResearchSampleData.isCompleted(stack)) {
            String key = RecipeLinkageConfig.CONSUME_COMPLETED_SAMPLE_ON_CLAIM.get()
                    ? "tooltip.recipe_linkage.sample.use_completed_consume"
                    : "tooltip.recipe_linkage.sample.use_completed";
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
