package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Fabric networking for Item Sharing. */
public final class FabricItemShareNetworking {
    private FabricItemShareNetworking() {
    }

    private static final Map<UUID, Long> LAST_ITEM_SHARE_AT_MS = new HashMap<>();

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

        HoverEvent hover = new HoverEvent.ShowItem(stack);

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
}
