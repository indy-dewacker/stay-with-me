package com.chainoffun;

import com.chainoffun.tether.TetherData;
import com.chainoffun.tether.TetherManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/** /chainoffun link|unlink|length|list — admin tools (permission level 2). */
public final class ChainCommand {
    private ChainCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chainoffun")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("link")
                        .then(Commands.argument("a", EntityArgument.player())
                                .then(Commands.argument("b", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer a = EntityArgument.getPlayer(ctx, "a");
                                            ServerPlayer b = EntityArgument.getPlayer(ctx, "b");
                                            if (a == b) {
                                                ctx.getSource().sendFailure(Component.literal("Can't chain a player to themselves."));
                                                return 0;
                                            }
                                            TetherManager.forceLink(a, b, Config.DEFAULT_LENGTH.get().floatValue());
                                            ctx.getSource().sendSuccess(() -> Component.literal("Chained " + a.getScoreboardName() + " and " + b.getScoreboardName() + "."), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("unlink")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    if (!TetherManager.forceUnlink(ctx.getSource().getServer(), p.getUUID())) {
                                        ctx.getSource().sendFailure(Component.literal(p.getScoreboardName() + " isn't chained."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("Unchained " + p.getScoreboardName() + "."), true);
                                    return 1;
                                })))
                .then(Commands.literal("length")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("blocks", FloatArgumentType.floatArg(1f, 64f))
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            float len = FloatArgumentType.getFloat(ctx, "blocks");
                                            if (!TetherManager.setLength(ctx.getSource().getServer(), p.getUUID(), len)) {
                                                ctx.getSource().sendFailure(Component.literal(p.getScoreboardName() + " isn't chained."));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Rope length set to " + len + "."), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();
                            List<TetherData.Link> links = TetherData.get(server).links();
                            if (links.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("Nobody is chained."), false);
                                return 0;
                            }
                            for (TetherData.Link l : links) {
                                String line = name(server, l.a) + " <-> " + name(server, l.b) + " (" + String.format("%.1f", l.length) + " blocks)";
                                ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                            }
                            return links.size();
                        })));
    }

    private static String name(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p != null ? p.getScoreboardName() : id.toString().substring(0, 8) + " (offline)";
    }
}
