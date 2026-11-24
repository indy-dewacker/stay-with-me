package com.chainoffun.network;

import com.chainoffun.ChainOfFun;
import com.chainoffun.tether.TetherManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: reel the rope in (-1) or out (+1) by one step. */
public record ReelPayload(int direction) implements CustomPacketPayload {
    public static final Type<ReelPayload> TYPE = new Type<>(ChainOfFun.id("reel"));
    public static final StreamCodec<FriendlyByteBuf, ReelPayload> CODEC =
            StreamCodec.ofMember(ReelPayload::write, ReelPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeByte(direction);
    }

    private static ReelPayload read(FriendlyByteBuf buf) {
        return new ReelPayload(buf.readByte());
    }

    public static void handle(ReelPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                TetherManager.reel(sp, Integer.signum(payload.direction()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
