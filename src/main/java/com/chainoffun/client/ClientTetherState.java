package com.chainoffun.client;

import com.chainoffun.network.TetherSyncPayload;
import com.chainoffun.tether.PhysicsParams;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The client's copy of who is chained to whom. Plain data, safe to load on a server. */
public final class ClientTetherState {
    private static final Map<UUID, UUID> PARTNERS = new HashMap<>();
    private static final Map<UUID, Float> LENGTHS = new HashMap<>();
    private static PhysicsParams params = PhysicsParams.DEFAULT;

    private ClientTetherState() {}

    public static void apply(TetherSyncPayload payload) {
        PARTNERS.clear();
        LENGTHS.clear();
        for (TetherSyncPayload.Entry e : payload.links()) {
            PARTNERS.put(e.a(), e.b());
            PARTNERS.put(e.b(), e.a());
            LENGTHS.put(e.a(), e.length());
            LENGTHS.put(e.b(), e.length());
        }
        params = payload.params();
    }

    public static void clear() {
        PARTNERS.clear();
        LENGTHS.clear();
        params = PhysicsParams.DEFAULT;
    }

    @Nullable
    public static UUID partnerOf(UUID player) {
        return PARTNERS.get(player);
    }

    public static float lengthOf(UUID player) {
        return LENGTHS.getOrDefault(player, 6f);
    }

    public static PhysicsParams params() {
        return params;
    }
}
