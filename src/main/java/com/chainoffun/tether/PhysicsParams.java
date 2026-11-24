package com.chainoffun.tether;

import com.chainoffun.Config;
import net.minecraft.network.FriendlyByteBuf;

/** Rope physics settings, owned by the server and sent to clients so both sides agree. */
public record PhysicsParams(float stiffness, float damping, float maxPull, float braceAnchor, float groundAnchor) {
    public static final PhysicsParams DEFAULT = new PhysicsParams(0.25f, 0.9f, 0.9f, 20f, 2f);

    public static PhysicsParams fromConfig() {
        try {
            return new PhysicsParams(
                    Config.STIFFNESS.get().floatValue(),
                    Config.DAMPING.get().floatValue(),
                    Config.MAX_PULL.get().floatValue(),
                    Config.BRACE_ANCHOR.get().floatValue(),
                    Config.GROUND_ANCHOR.get().floatValue());
        } catch (IllegalStateException notLoaded) {
            return DEFAULT;
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(stiffness);
        buf.writeFloat(damping);
        buf.writeFloat(maxPull);
        buf.writeFloat(braceAnchor);
        buf.writeFloat(groundAnchor);
    }

    public static PhysicsParams read(FriendlyByteBuf buf) {
        return new PhysicsParams(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
