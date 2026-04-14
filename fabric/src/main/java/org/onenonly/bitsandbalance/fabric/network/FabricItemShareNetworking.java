package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Fabric networking for Item Sharing. */
public final class FabricItemShareNetworking {
    private FabricItemShareNetworking() {
    }

    private static final Map<UUID, Long> LAST_ITEM_SHARE_AT_MS = new HashMap<>();
    private static volatile Constructor<?> bitsandbalance$showItemConstructor;
    private static volatile Method bitsandbalance$itemStackTemplateFromNonEmptyStack;

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(ItemSharePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            server.execute(() -> handle(server, player, payload));
        });
    }

    private static void handle(MinecraftServer server, ServerPlayer player, ItemSharePayload payload) {
        if (!FabricTweaksConfig.enableItemSharing) return;

        double cooldownSeconds = Math.max(0.0D, FabricTweaksConfig.itemShareCooldownSeconds);
        long cooldownMs = (long) (cooldownSeconds * 1000.0D);
        if (cooldownMs > 0L) {
            long now = System.currentTimeMillis();
            UUID id = player.getUUID();
            Long last = LAST_ITEM_SHARE_AT_MS.get(id);
            if (last != null) {
                long elapsed = now - last;
                if (elapsed >= 0L && elapsed < cooldownMs) {
                    return;
                }
            }
            LAST_ITEM_SHARE_AT_MS.put(id, now);
        }

        var stack = payload.stack();
        if (stack == null || stack.isEmpty()) return;

        // Build a vanilla-like player chat prefix for Chat Heads compatibility
        Component prefix = Component.literal("<" + player.getName().getString() + "> ");

        HoverEvent hover = bitsandbalance$createShowItemHover(stack.copy());

        // ItemShareClient renders the icon when it sees a space marker and the *next* character has SHOW_ITEM.
        // We use THREE spaces so the third space becomes a visible gap between the icon and the item text.
        Component spaces = Component.literal("   ").withStyle(s -> s.withHoverEvent(hover));

        Component baseName = stack.getHoverName().copy();
        Component itemLink = ComponentUtils.wrapInSquareBrackets(baseName)
                .withStyle(style -> style.withHoverEvent(hover));

        var msg = Component.empty().copy().append(prefix).append(spaces).append(itemLink);

        try {
            server.getPlayerList().broadcastSystemMessage(msg, false);
        } catch (Throwable ignored) {
            try {
                player.sendSystemMessage(msg);
            } catch (Throwable ignored2) {
                // ignore
            }
        }
    }

    private static HoverEvent bitsandbalance$createShowItemHover(net.minecraft.world.item.ItemStack stack) {
        try {
            Constructor<?> constructor = bitsandbalance$showItemConstructor;
            if (constructor == null) {
                Class<?> showItemClass = Class.forName("net.minecraft.network.chat.HoverEvent$ShowItem");
                for (Constructor<?> candidate : showItemClass.getDeclaredConstructors()) {
                    if (candidate.getParameterCount() != 1) continue;
                    String parameterName = candidate.getParameterTypes()[0].getName();
                    if (!parameterName.equals("net.minecraft.world.item.ItemStack")
                            && !parameterName.equals("net.minecraft.world.item.ItemStackTemplate")) {
                        continue;
                    }
                    candidate.setAccessible(true);
                    bitsandbalance$showItemConstructor = candidate;
                    constructor = candidate;
                    break;
                }
            }

            if (constructor != null) {
                Class<?> parameterType = constructor.getParameterTypes()[0];
                Object constructorArg = stack;
                if (!parameterType.isInstance(constructorArg)) {
                    if (parameterType.getName().equals("net.minecraft.world.item.ItemStackTemplate")) {
                        Method method = bitsandbalance$itemStackTemplateFromNonEmptyStack;
                        if (method == null) {
                            Class<?> itemStackTemplateClass = Class.forName("net.minecraft.world.item.ItemStackTemplate");
                            method = itemStackTemplateClass.getMethod("fromNonEmptyStack", net.minecraft.world.item.ItemStack.class);
                            bitsandbalance$itemStackTemplateFromNonEmptyStack = method;
                        }
                        constructorArg = method.invoke(null, stack);
                    } else {
                        return new HoverEvent.ShowItem(stack);
                    }
                }

                return (HoverEvent) constructor.newInstance(constructorArg);
            }
        } catch (Throwable ignored) {
        }

        return new HoverEvent.ShowItem(stack);
    }
}
