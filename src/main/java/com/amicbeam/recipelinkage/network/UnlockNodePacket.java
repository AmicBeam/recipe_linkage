package com.amicbeam.recipelinkage.network;

import com.amicbeam.recipelinkage.block.entity.ResearchTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UnlockNodePacket(BlockPos pos, int nodeIndex) {
    public static void encode(UnlockNodePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.nodeIndex);
    }

    public static UnlockNodePacket decode(FriendlyByteBuf buf) {
        return new UnlockNodePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(UnlockNodePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(packet.pos.getX() + 0.5D, packet.pos.getY() + 0.5D, packet.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(packet.pos) instanceof ResearchTableBlockEntity table) {
                table.unlockNode(player, packet.nodeIndex);
            }
        });
        context.setPacketHandled(true);
    }
}

