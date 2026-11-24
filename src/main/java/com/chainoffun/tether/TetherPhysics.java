package com.chainoffun.tether;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The rope math, shared by client and server.
 *
 * Each game only moves its own player. When the rope is stretched past its length, a player gets
 * a share of the correction depending on how well each end is anchored: a sneaking player on solid
 * ground is a strong anchor, a grounded player a weak one, an airborne player none at all. Both
 * clients compute the same shares, so the two halves add up to one consistent rope.
 */
public final class TetherPhysics {
    private static final double MAX_SPEED = 3.0;

    private TetherPhysics() {}

    /** Where the rope attaches: roughly the player's waist. */
    public static Vec3 point(Entity e, float partialTick) {
        return e.getPosition(partialTick).add(0, e.getBbHeight() * 0.5, 0);
    }

    /** Standing on something solid (works for remote players too, whose onGround flag can lag). */
    public static boolean isGrounded(Entity e) {
        Entity body = e.isPassenger() && e.getVehicle() != null ? e.getRootVehicle() : e;
        if (body.onGround()) return true;
        AABB box = body.getBoundingBox();
        AABB below = new AABB(box.minX + 0.01, box.minY - 0.1, box.minZ + 0.01, box.maxX - 0.01, box.minY, box.maxZ - 0.01);
        return !body.level().noCollision(body, below);
    }

    /** Sneaking with both feet on solid ground. */
    public static boolean isBraced(Entity e) {
        return e instanceof Player p && !p.isPassenger() && p.isShiftKeyDown() && isGrounded(p);
    }

    public static double anchor(Entity e, PhysicsParams params) {
        if (isBraced(e)) return params.braceAnchor();
        return isGrounded(e) ? params.groundAnchor() : 1.0;
    }

    /** How far past its length the rope is stretched (0 when slack). */
    public static double stretch(Entity self, Entity partner, float length) {
        return Math.max(0, point(self, 1f).distanceTo(point(partner, 1f)) - length);
    }

    /**
     * New velocity for {@code self} (or the vehicle it steers), or null when the rope is slack.
     */
    @Nullable
    public static Vec3 constrain(Entity self, Entity partner, Vec3 velocity, float length, PhysicsParams params) {
        Vec3 a = point(self, 1f);
        Vec3 b = point(partner, 1f);
        Vec3 d = b.subtract(a);
        double dist = d.length();
        if (dist <= length || dist < 1.0E-4) return null;

        Vec3 n = d.scale(1.0 / dist);
        double stretch = dist - length;

        double mine = anchor(self, params);
        double theirs = anchor(partner, params);
        double share = theirs / (mine + theirs);

        double add = Math.min(stretch * params.stiffness(), params.maxPull());
        double radial = velocity.dot(n);
        if (radial < 0) add += -radial * params.damping();

        Vec3 result = velocity.add(n.scale(add * share));
        double speed = result.length();
        if (speed > MAX_SPEED) result = result.scale(MAX_SPEED / speed);
        return result;
    }
}
