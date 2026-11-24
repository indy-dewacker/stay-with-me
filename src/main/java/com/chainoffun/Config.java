package com.chainoffun;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-world server config (world/serverconfig/chainoffun-server.toml). Physics values are sent to clients. */
public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue DEFAULT_LENGTH;
    public static final ModConfigSpec.DoubleValue MIN_LENGTH;
    public static final ModConfigSpec.DoubleValue MAX_LENGTH;
    public static final ModConfigSpec.DoubleValue REEL_STEP;

    public static final ModConfigSpec.DoubleValue STIFFNESS;
    public static final ModConfigSpec.DoubleValue DAMPING;
    public static final ModConfigSpec.DoubleValue MAX_PULL;
    public static final ModConfigSpec.DoubleValue BRACE_ANCHOR;
    public static final ModConfigSpec.DoubleValue GROUND_ANCHOR;

    public static final ModConfigSpec.DoubleValue YANK_STRENGTH;
    public static final ModConfigSpec.IntValue YANK_COOLDOWN_TICKS;

    public static final ModConfigSpec.IntValue UNLINK_WINDOW_SECONDS;
    public static final ModConfigSpec.DoubleValue PORTAL_RADIUS;
    public static final ModConfigSpec.BooleanValue PORTAL_BLOCKED_WHEN_PARTNER_OFFLINE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("rope");
        DEFAULT_LENGTH = b.comment("Rope length (blocks) when two players get chained").defineInRange("defaultLength", 6.0, 1.0, 64.0);
        MIN_LENGTH = b.comment("Shortest the rope can be reeled in to").defineInRange("minLength", 2.0, 1.0, 64.0);
        MAX_LENGTH = b.comment("Longest the rope can be reeled out to").defineInRange("maxLength", 16.0, 1.0, 64.0);
        REEL_STEP = b.comment("Blocks reeled per scroll notch").defineInRange("reelStep", 0.5, 0.1, 4.0);
        b.pop();

        b.push("physics");
        STIFFNESS = b.comment("Spring strength once the rope is taut. Higher = less stretchy").defineInRange("stiffness", 0.25, 0.01, 2.0);
        DAMPING = b.comment("How much outward speed the rope absorbs each tick (0-1)").defineInRange("damping", 0.9, 0.0, 1.0);
        MAX_PULL = b.comment("Max speed (blocks/tick) the spring can add per tick").defineInRange("maxPull", 0.9, 0.05, 3.0);
        BRACE_ANCHOR = b.comment("Anchor weight of a sneaking player on solid ground").defineInRange("braceAnchor", 20.0, 1.0, 1000.0);
        GROUND_ANCHOR = b.comment("Anchor weight of a player standing on the ground (airborne = 1)").defineInRange("groundAnchor", 2.0, 1.0, 1000.0);
        b.pop();

        b.push("yank");
        YANK_STRENGTH = b.comment("How hard a yank launches your partner").defineInRange("strength", 1.4, 0.1, 5.0);
        YANK_COOLDOWN_TICKS = b.comment("Ticks between yanks (20 = 1 second)").defineInRange("cooldownTicks", 60, 0, 1200);
        b.pop();

        b.push("rules");
        UNLINK_WINDOW_SECONDS = b.comment("Seconds both players have to sneak + right-click each other to unchain").defineInRange("unlinkWindowSeconds", 5, 1, 60);
        PORTAL_RADIUS = b.comment("Partner must be in a portal within this many blocks to travel together").defineInRange("portalRadius", 4.0, 1.0, 32.0);
        PORTAL_BLOCKED_WHEN_PARTNER_OFFLINE = b.comment("Block portals while your partner is offline").define("portalBlockedWhenPartnerOffline", true);
        b.pop();

        SPEC = b.build();
    }

    private Config() {}
}
