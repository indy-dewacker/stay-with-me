package com.chainoffun;

import com.chainoffun.network.Network;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(ChainOfFun.MODID)
public class ChainOfFun {
    public static final String MODID = "chainoffun";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<ChainOfFunItem> CHAIN_OF_FUN =
            ITEMS.register("chain_of_fun", () -> new ChainOfFunItem(new Item.Properties().stacksTo(16)));

    public ChainOfFun(IEventBus modBus, ModContainer container) {
        ITEMS.register(modBus);
        modBus.addListener(Network::register);
        modBus.addListener(ChainOfFun::addCreative);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CHAIN_OF_FUN.get());
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
