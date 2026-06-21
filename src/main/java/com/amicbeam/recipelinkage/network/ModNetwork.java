package com.amicbeam.recipelinkage.network;

import com.amicbeam.recipelinkage.RecipeLinkage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RecipeLinkage.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, UnlockNodePacket.class, UnlockNodePacket::encode, UnlockNodePacket::decode, UnlockNodePacket::handle);
    }
}

