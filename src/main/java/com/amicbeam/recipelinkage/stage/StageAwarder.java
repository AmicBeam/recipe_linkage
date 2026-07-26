package com.amicbeam.recipelinkage.stage;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

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
        String command = "astages add " + playerName + " " + stage + " false false false";
        Commands commands = player.server.getCommands();
        ParseResults<CommandSourceStack> parseResults = commands.getDispatcher().parse(command, source);
        try {
            Commands.validateParseResults(parseResults);
        } catch (CommandSyntaxException exception) {
            return false;
        }
        commands.performCommand(parseResults, command);
        return true;
    }
}
