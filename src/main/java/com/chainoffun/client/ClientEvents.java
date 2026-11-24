package com.chainoffun.client;

import com.chainoffun.ChainOfFun;
import com.chainoffun.network.ReelPayload;
import com.chainoffun.network.YankPayload;
import com.chainoffun.tether.TetherPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

@EventBusSubscriber(modid = ChainOfFun.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean wasTaut = false;

    private ClientEvents() {}

    @Nullable
    private static Player partnerOf(Player player) {
        UUID id = ClientTetherState.partnerOf(player.getUUID());
        return id == null ? null : player.level().getPlayerByUUID(id);
    }

    /** Rope physics for our own player, applied right before vanilla moves us this tick. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;
        LocalPlayer self = mc.player;
        if (self.isSpectator()) return;

        Player partner = partnerOf(self);
        if (partner == null || partner.isSpectator()) return;

        Entity mover = self;
        if (self.isPassenger()) {
            Entity vehicle = self.getVehicle();
            if (vehicle == null || vehicle.getControllingPassenger() != self || !vehicle.isControlledByLocalInstance()) return;
            mover = vehicle;
        }

        Vec3 v = TetherPhysics.constrain(self, partner, mover.getDeltaMovement(),
                ClientTetherState.lengthOf(self.getUUID()), ClientTetherState.params());
        if (v != null) mover.setDeltaMovement(v);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer self = mc.player;
        if (self == null || mc.level == null) {
            wasTaut = false;
            return;
        }

        Player partner = partnerOf(self);
        while (ClientKeys.YANK.consumeClick()) {
            if (partner != null) PacketDistributor.sendToServer(YankPayload.INSTANCE);
        }

        // Chain rattle when the rope snaps taut.
        if (partner == null) {
            wasTaut = false;
            return;
        }
        double stretch = TetherPhysics.stretch(self, partner, ClientTetherState.lengthOf(self.getUUID()));
        if (!wasTaut && stretch > 0.2) {
            mc.level.playLocalSound(self.getX(), self.getY(), self.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS,
                    0.6f, 0.8f + self.getRandom().nextFloat() * 0.3f, false);
        }
        wasTaut = stretch > 0.05;
    }

    /** Hold the reel key + scroll: up reels in, down lets rope out. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !ClientKeys.REEL.isDown()) return;
        if (ClientTetherState.partnerOf(mc.player.getUUID()) == null) return;
        double dy = event.getScrollDeltaY();
        if (dy == 0) return;
        PacketDistributor.sendToServer(new ReelPayload(dy > 0 ? -1 : 1));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player from = event.getEntity();
        Player to = partnerOf(from);
        if (to == null || mc.player == null) return;

        // Draw each chain once: from our own body in third person, from the partner in first person,
        // and from the lower UUID for other people's chains.
        boolean draw;
        if (from == mc.player) {
            draw = true;
        } else if (to == mc.player) {
            draw = mc.options.getCameraType().isFirstPerson();
        } else {
            draw = from.getUUID().compareTo(to.getUUID()) < 0;
        }
        if (!draw) return;

        ChainRenderer.render(from, to, ClientTetherState.lengthOf(from.getUUID()),
                event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTetherState.clear();
        wasTaut = false;
    }
}
