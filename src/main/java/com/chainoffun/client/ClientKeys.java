package com.chainoffun.client;

import com.chainoffun.ChainOfFun;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ChainOfFun.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientKeys {
    private static final String CATEGORY = "key.categories.chainoffun";

    /** Hold this and scroll to reel the rope in/out. */
    public static final KeyMapping REEL = new KeyMapping("key.chainoffun.reel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    /** Jerk the rope toward you. */
    public static final KeyMapping YANK = new KeyMapping("key.chainoffun.yank", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private ClientKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(REEL);
        event.register(YANK);
    }
}
