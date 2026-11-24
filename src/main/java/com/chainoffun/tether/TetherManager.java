package com.chainoffun.tether;

import com.chainoffun.ChainOfFun;
import com.chainoffun.Config;
import com.chainoffun.network.TetherSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side rules: linking, unlinking, reeling, yanking, catching falls, respawns and portals. */
public final class TetherManager {
    private record UnlinkRequest(UUID target, long time) {}

    private static final Map<UUID, UnlinkRequest> UNLINK_REQUESTS = new HashMap<>();
    private static final Map<UUID, Long> YANK_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> LAST_CAUGHT = new HashMap<>();
    private static final Map<UUID, Long> LAST_PORTAL_MSG = new HashMap<>();

    private TetherManager() {}

    // ---------------------------------------------------------------- sync

    public static TetherSyncPayload snapshot(MinecraftServer server) {
        List<TetherSyncPayload.Entry> entries = new ArrayList<>();
        for (TetherData.Link l : TetherData.get(server).links()) {
            entries.add(new TetherSyncPayload.Entry(l.a, l.b, l.length));
        }
        return new TetherSyncPayload(entries, PhysicsParams.fromConfig());
    }

    public static void syncAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(snapshot(server));
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot(player.server));
    }

    @Nullable
    public static ServerPlayer partnerOf(ServerPlayer player) {
        TetherData.Link l = TetherData.get(player.server).get(player.getUUID());
        return l == null ? null : player.server.getPlayerList().getPlayer(l.other(player.getUUID()));
    }

    // ---------------------------------------------------------------- linking / unlinking

    /** Right-click on another player. Returns true if we handled it. */
    public static boolean handleInteract(ServerPlayer self, ServerPlayer target) {
        TetherData data = TetherData.get(self.server);
        TetherData.Link mine = data.get(self.getUUID());
        ItemStack held = self.getMainHandItem();
        boolean holdingChain = held.is(ChainOfFun.CHAIN_OF_FUN.get());

        if (mine != null && mine.other(self.getUUID()).equals(target.getUUID())) {
            if (self.isShiftKeyDown()) {
                requestUnlink(self, target, data);
                return true;
            }
            if (holdingChain) {
                chat(self, Component.translatable("msg.chainoffun.already_linked_pair", target.getDisplayName()));
                return true;
            }
            return false;
        }

        if (!holdingChain) return false;

        if (mine != null) {
            chat(self, Component.translatable("msg.chainoffun.already_linked_self"));
            return true;
        }
        if (data.get(target.getUUID()) != null) {
            chat(self, Component.translatable("msg.chainoffun.already_linked_target", target.getDisplayName()));
            return true;
        }

        float length = Config.DEFAULT_LENGTH.get().floatValue();
        data.link(self.getUUID(), target.getUUID(), length);
        if (!self.getAbilities().instabuild) held.shrink(1);

        sound(self, SoundEvents.CHAIN_PLACE, 1.0f, 0.8f);
        sound(target, SoundEvents.CHAIN_PLACE, 1.0f, 0.8f);
        chat(self, Component.translatable("msg.chainoffun.linked", target.getDisplayName()).withStyle(ChatFormatting.GOLD));
        chat(target, Component.translatable("msg.chainoffun.linked", self.getDisplayName()).withStyle(ChatFormatting.GOLD));
        syncAll(self.server);
        return true;
    }

    private static void requestUnlink(ServerPlayer self, ServerPlayer target, TetherData data) {
        long now = self.server.overworld().getGameTime();
        long window = Config.UNLINK_WINDOW_SECONDS.get() * 20L;
        UnlinkRequest theirs = UNLINK_REQUESTS.get(target.getUUID());

        if (theirs != null && theirs.target().equals(self.getUUID()) && now - theirs.time() <= window) {
            UNLINK_REQUESTS.remove(self.getUUID());
            UNLINK_REQUESTS.remove(target.getUUID());
            data.unlink(self.getUUID());
            give(self, new ItemStack(ChainOfFun.CHAIN_OF_FUN.get()));
            sound(self, SoundEvents.CHAIN_BREAK, 1.0f, 1.0f);
            sound(target, SoundEvents.CHAIN_BREAK, 1.0f, 1.0f);
            chat(self, Component.translatable("msg.chainoffun.unlinked", target.getDisplayName()));
            chat(target, Component.translatable("msg.chainoffun.unlinked", self.getDisplayName()));
            syncAll(self.server);
            return;
        }

        UNLINK_REQUESTS.put(self.getUUID(), new UnlinkRequest(target.getUUID(), now));
        chat(self, Component.translatable("msg.chainoffun.unlink_sent", target.getDisplayName()));
        chat(target, Component.translatable("msg.chainoffun.unlink_request", self.getDisplayName(), Config.UNLINK_WINDOW_SECONDS.get())
                .withStyle(ChatFormatting.YELLOW));
    }

    /** Admin command link. */
    public static void forceLink(ServerPlayer a, ServerPlayer b, float length) {
        TetherData.get(a.server).link(a.getUUID(), b.getUUID(), length);
        syncAll(a.server);
    }

    /** Admin command unlink. */
    public static boolean forceUnlink(MinecraftServer server, UUID player) {
        boolean removed = TetherData.get(server).unlink(player) != null;
        if (removed) syncAll(server);
        return removed;
    }

    public static boolean setLength(MinecraftServer server, UUID player, float length) {
        TetherData data = TetherData.get(server);
        TetherData.Link l = data.get(player);
        if (l == null) return false;
        l.length = length;
        data.setDirty();
        syncAll(server);
        return true;
    }

    // ---------------------------------------------------------------- reel & yank

    public static void reel(ServerPlayer player, int direction) {
        if (direction == 0) return;
        TetherData data = TetherData.get(player.server);
        TetherData.Link l = data.get(player.getUUID());
        if (l == null) return;

        if (!TetherPhysics.isBraced(player)) {
            actionBar(player, Component.translatable("msg.chainoffun.reel_denied").withStyle(ChatFormatting.RED));
            return;
        }

        float min = Config.MIN_LENGTH.get().floatValue();
        float max = Math.max(min, Config.MAX_LENGTH.get().floatValue());
        float next = Mth.clamp(l.length + direction * Config.REEL_STEP.get().floatValue(), min, max);
        if (next == l.length) return;

        l.length = next;
        data.setDirty();
        syncAll(player.server);
        sound(player, SoundEvents.CHAIN_STEP, 0.6f, direction < 0 ? 1.3f : 0.9f);
        actionBar(player, Component.translatable("msg.chainoffun.rope_length", String.format("%.1f", next)));
    }

    public static void yank(ServerPlayer player) {
        ServerPlayer partner = partnerOf(player);
        if (partner == null || partner.level() != player.level() || partner.isSpectator()) {
            actionBar(player, Component.translatable("msg.chainoffun.yank_no_partner"));
            return;
        }

        long now = player.server.overworld().getGameTime();
        Long last = YANK_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < Config.YANK_COOLDOWN_TICKS.get()) {
            actionBar(player, Component.translatable("msg.chainoffun.yank_cooldown"));
            return;
        }
        YANK_COOLDOWN.put(player.getUUID(), now);

        Vec3 dir = TetherPhysics.point(player, 1f).subtract(TetherPhysics.point(partner, 1f));
        if (dir.lengthSqr() < 0.01) return;
        dir = dir.normalize();

        double strength = Config.YANK_STRENGTH.get();
        if (TetherPhysics.isBraced(partner)) strength *= 0.25;

        partner.push(dir.x * strength, dir.y * strength + 0.3, dir.z * strength);
        partner.hurtMarked = true; // sends the new motion to the partner's client

        sound(player, SoundEvents.CHAIN_HIT, 1.0f, 0.7f);
        sound(partner, SoundEvents.CHAIN_HIT, 1.0f, 0.7f);
    }

    // ---------------------------------------------------------------- per tick

    /** Catches falls and keeps the anti-flight kick away while someone hangs on the rope. */
    public static void tick(ServerPlayer player) {
        if (player.isSpectator()) return;
        TetherData.Link l = TetherData.get(player.server).get(player.getUUID());
        if (l == null) return;
        ServerPlayer partner = player.server.getPlayerList().getPlayer(l.other(player.getUUID()));
        if (partner == null || partner.level() != player.level()) return;

        Vec3 a = TetherPhysics.point(player, 1f);
        Vec3 b = TetherPhysics.point(partner, 1f);
        boolean taut = a.distanceTo(b) >= l.length - 0.3;
        boolean below = a.y < b.y - 0.25;

        if (taut && below) {
            player.resetFallDistance();
            LAST_CAUGHT.put(player.getUUID(), player.server.overworld().getGameTime());
            player.connection.aboveGroundTickCount = 0;
            player.connection.aboveGroundVehicleTickCount = 0;
        }
    }

    public static boolean recentlyCaught(ServerPlayer player) {
        Long t = LAST_CAUGHT.get(player.getUUID());
        return t != null && player.server.overworld().getGameTime() - t <= 10;
    }

    // ---------------------------------------------------------------- death & portals

    /** The survivor gets pulled to wherever their partner respawned. */
    public static void onRespawn(ServerPlayer respawned) {
        syncTo(respawned);
        ServerPlayer partner = partnerOf(respawned);
        if (partner == null || partner == respawned || !partner.isAlive()) return;

        partner.teleportTo(respawned.serverLevel(), respawned.getX(), respawned.getY(), respawned.getZ(),
                partner.getYRot(), partner.getXRot());
        partner.resetFallDistance();
        sound(partner, SoundEvents.CHAIN_PLACE, 1.0f, 0.6f);
        chat(partner, Component.translatable("msg.chainoffun.pulled_to_partner", respawned.getDisplayName()));
    }

    /** Portals only work if your partner is in one with you (or already on the other side). */
    public static boolean canUsePortal(ServerPlayer player, ResourceKey<Level> destination) {
        if (!isInPortal(player)) return true; // commands, respawns, other mods' teleports
        TetherData.Link l = TetherData.get(player.server).get(player.getUUID());
        if (l == null) return true;

        ServerPlayer partner = player.server.getPlayerList().getPlayer(l.other(player.getUUID()));
        boolean ok;
        Component why;
        if (partner == null) {
            ok = !Config.PORTAL_BLOCKED_WHEN_PARTNER_OFFLINE.get();
            why = Component.translatable("msg.chainoffun.portal_blocked_offline");
        } else if (partner.level().dimension().equals(destination)) {
            ok = true;
            why = null;
        } else {
            ok = partner.level() == player.level()
                    && partner.distanceTo(player) <= Config.PORTAL_RADIUS.get()
                    && isInPortal(partner);
            why = Component.translatable("msg.chainoffun.portal_blocked", partner.getDisplayName());
        }

        if (!ok && why != null) {
            long now = player.server.overworld().getGameTime();
            Long last = LAST_PORTAL_MSG.get(player.getUUID());
            if (last == null || now - last > 40) {
                LAST_PORTAL_MSG.put(player.getUUID(), now);
                actionBar(player, why.copy().withStyle(ChatFormatting.RED));
            }
        }
        return ok;
    }

    private static boolean isInPortal(Entity e) {
        return BlockPos.betweenClosedStream(e.getBoundingBox().deflate(0.001)).anyMatch(pos -> {
            BlockState s = e.level().getBlockState(pos);
            return s.is(Blocks.NETHER_PORTAL) || s.is(Blocks.END_PORTAL) || s.is(Blocks.END_GATEWAY);
        });
    }

    // ---------------------------------------------------------------- helpers

    private static void chat(ServerPlayer p, Component c) {
        p.displayClientMessage(c, false);
    }

    private static void actionBar(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }

    private static void sound(ServerPlayer p, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }
}
