package com.chainoffun.network;

import com.chainoffun.ChainOfFun;
import com.chainoffun.client.ClientTetherState;
import com.chainoffun.tether.PhysicsParams;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server -> client: every tether on the server plus the rope physics settings. */
public record TetherSyncPayload(List<Entry> links, PhysicsParams params) implements CustomPacketPayload {
    public record Entry(UUID a, UUID b, float length) {}

    public static final Type<TetherSyncPayload> TYPE = new Type<>(ChainOfFun.id("sync"));
    public static final StreamCodec<FriendlyByteBuf, TetherSyncPayload> CODEC =
            StreamCodec.ofMember(TetherSyncPayload::write, TetherSyncPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(links.size());
        for (Entry e : links) {
            buf.writeUUID(e.a());
            buf.writeUUID(e.b());
            buf.writeFloat(e.length());
        }
        params.write(buf);
    }

    private static TetherSyncPayload read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readUUID(), buf.readUUID(), buf.readFloat()));
        }
        return new TetherSyncPayload(list, PhysicsParams.read(buf));
    }

    public static void handle(TetherSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientTetherState.apply(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
