package com.chainoffun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Right-click a friend to chain yourselves together. The actual linking happens server-side in TetherManager. */
public class ChainOfFunItem extends Item {
    public ChainOfFunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // Only reached on the client (the server handles it earlier); returning success swings the arm.
        return target instanceof Player ? InteractionResult.sidedSuccess(player.level().isClientSide()) : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.chainoffun.chain_of_fun.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.chainoffun.chain_of_fun.desc2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
