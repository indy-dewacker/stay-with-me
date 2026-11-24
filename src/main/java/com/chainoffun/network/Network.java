package com.chainoffun.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(TetherSyncPayload.TYPE, TetherSyncPayload.CODEC, TetherSyncPayload::handle);
        registrar.playToServer(ReelPayload.TYPE, ReelPayload.CODEC, ReelPayload::handle);
        registrar.playToServer(YankPayload.TYPE, YankPayload.CODEC, YankPayload::handle);
    }
}
