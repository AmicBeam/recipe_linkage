package com.amicbeam.recipelinkage.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("recipe_linkage").versioned(PROTOCOL);
        registrar.playToServer(UnlockNodePacket.TYPE, UnlockNodePacket.STREAM_CODEC, UnlockNodePacket::handle);
    }
}
