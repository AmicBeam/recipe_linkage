package com.amicbeam.recipelinkage.stage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class StageAwarder {
    private StageAwarder() {
    }

    public static boolean award(ServerPlayer player, String stage) {
        if (stage == null || stage.isBlank()) {
            return false;
        }
        if (!ModList.get().isLoaded("astages")) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.recipe_linkage.stage.astages_missing"), true);
            return false;
        }
        CommandSourceStack source = player.server.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        String playerName = player.getGameProfile().getName();
        String command = "astages add " + playerName + " " + stage + " true true";
        player.server.getCommands().performPrefixedCommand(source, command);
        return true;
    }
}

