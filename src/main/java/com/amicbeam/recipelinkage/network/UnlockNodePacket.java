package com.amicbeam.recipelinkage.network;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.amicbeam.recipelinkage.block.entity.ResearchTableBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UnlockNodePacket(BlockPos pos, int nodeIndex) implements CustomPacketPayload {
    public static final Type<UnlockNodePacket> TYPE = new Type<>(RecipeLinkage.id("unlock_node"));
    public static final StreamCodec<ByteBuf, UnlockNodePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            UnlockNodePacket::pos,
            ByteBufCodecs.VAR_INT,
            UnlockNodePacket::nodeIndex,
            UnlockNodePacket::new);

    public static void handle(UnlockNodePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player == null || player.distanceToSqr(packet.pos.getX() + 0.5D, packet.pos.getY() + 0.5D, packet.pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(packet.pos) instanceof ResearchTableBlockEntity table) {
                table.unlockNode(player, packet.nodeIndex);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
