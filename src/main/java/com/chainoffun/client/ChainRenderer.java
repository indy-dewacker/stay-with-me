package com.chainoffun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a sagging chain between two players using vanilla's chain texture, as two crossed strips
 * (like the chain block). It droops when slack, goes straight when taut, and tints red under strain.
 */
public final class ChainRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/chain.png");
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final float HALF_WIDTH = 0.09f;

    private ChainRenderer() {}

    /** PoseStack origin is {@code from}'s render position. */
    public static void render(Player from, Player to, float length, PoseStack poseStack, MultiBufferSource buffers, float partialTick) {
        Vec3 origin = from.getPosition(partialTick);
        Vec3 a = new Vec3(0, from.getBbHeight() * 0.5, 0);
        Vec3 b = to.getPosition(partialTick).add(0, to.getBbHeight() * 0.5, 0).subtract(origin);

        double dist = a.distanceTo(b);
        if (dist < 0.05) return;
        double sag = dist < length ? Math.min(Math.sqrt(length * length - dist * dist) * 0.5, length * 0.5) : 0;
        float strain = (float) Mth.clamp((dist - length) / 1.5, 0, 1);

        int red = 255;
        int greenBlue = (int) (255 * (1 - 0.6f * strain));

        int segments = Mth.clamp((int) Math.ceil((dist + sag) / 0.3), 6, 96);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Level level = from.level();

        Vec3 prev = point(a, b, sag, 0);
        double v = 0;
        for (int i = 1; i <= segments; i++) {
            Vec3 cur = point(a, b, sag, i / (double) segments);
            Vec3 seg = cur.subtract(prev);
            double segLen = seg.length();
            if (segLen < 1.0E-5) {
                prev = cur;
                continue;
            }
            Vec3 dir = seg.scale(1 / segLen);
            Vec3 side = dir.cross(UP);
            if (side.lengthSqr() < 1.0E-4) side = new Vec3(1, 0, 0);
            side = side.normalize().scale(HALF_WIDTH);
            Vec3 side2 = dir.cross(side.normalize()).normalize().scale(HALF_WIDTH);

            Vec3 mid = origin.add(prev.add(cur).scale(0.5));
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(mid));

            float v0 = (float) v;
            float v1 = (float) (v + segLen);
            quad(vc, pose, prev, cur, side, 0f, 3f / 16f, v0, v1, red, greenBlue, light);
            quad(vc, pose, prev, cur, side2, 3f / 16f, 6f / 16f, v0, v1, red, greenBlue, light);

            v = (v + segLen) % 1.0;
            prev = cur;
        }
    }

    private static Vec3 point(Vec3 a, Vec3 b, double sag, double t) {
        return a.lerp(b, t).add(0, -4 * sag * t * (1 - t), 0);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 off,
                             float u0, float u1, float v0, float v1, int r, int gb, int light) {
        vertex(vc, pose, p0.subtract(off), u0, v0, r, gb, light);
        vertex(vc, pose, p0.add(off), u1, v0, r, gb, light);
        vertex(vc, pose, p1.add(off), u1, v1, r, gb, light);
        vertex(vc, pose, p1.subtract(off), u0, v1, r, gb, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, int r, int gb, int light) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(r, gb, gb, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }
}
