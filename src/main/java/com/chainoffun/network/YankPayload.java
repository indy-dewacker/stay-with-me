package com.chainoffun.network;

import com.chainoffun.ChainOfFun;
import com.chainoffun.tether.TetherManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: yank the rope. */
public record YankPayload() implements CustomPacketPayload {
    public static final YankPayload INSTANCE = new YankPayload();
    public static final Type<YankPayload> TYPE = new Type<>(ChainOfFun.id("yank"));
    public static final StreamCodec<FriendlyByteBuf, YankPayload> CODEC = StreamCodec.unit(INSTANCE);

    public static void handle(YankPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                TetherManager.yank(sp);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
