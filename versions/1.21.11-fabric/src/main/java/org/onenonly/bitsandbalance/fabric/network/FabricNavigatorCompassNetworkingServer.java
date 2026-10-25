package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;

import java.util.ArrayList;
import java.util.List;

/** Server-only networking for Navigator Compass. */
public final class FabricNavigatorCompassNetworkingServer {
    private FabricNavigatorCompassNetworkingServer() {
    }

    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(NavigatorCompassSetPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();
                ItemStack compass = main.is(Items.COMPASS) ? main : (off.is(Items.COMPASS) ? off : ItemStack.EMPTY);
                if (compass.isEmpty()) {
                    return;
                }

                boolean clear = payload.x() == Integer.MAX_VALUE
                        && payload.y() == Integer.MAX_VALUE
                        && payload.z() == Integer.MAX_VALUE;

                if (clear) {
                    clearCompassTarget(compass);
                    return;
                }

                if (!(player.level() instanceof ServerLevel level)) {
                    return;
                }

                BlockPos target = new BlockPos(payload.x(), payload.y(), payload.z());
                setCompassTarget(compass, level, target);
            });
        });
    }

    public static void sendOpenGui(ServerPlayer player, int x, int y, int z) {
        ServerPlayNetworking.send(player, new NavigatorCompassOpenGUIPayload(x, y, z));
    }

    public static void setCompassTarget(ItemStack compass, ServerLevel level, BlockPos targetPos) {
        if (!compass.is(Items.COMPASS)) {
            return;
        }

        String dimId;
        try {
            dimId = level.dimension().identifier().toString();
        } catch (Throwable t) {
            dimId = "";
        }
        NavigatorCompassItemData.setTarget(compass, dimId, targetPos);

        // If a datapack/mod enchanted the compass, keep behavior consistent with common-side.
        try {
            compass.remove(DataComponents.ENCHANTMENTS);
            compass.remove(DataComponents.STORED_ENCHANTMENTS);
        } catch (Throwable ignored) {
        }

        updateCompassDisplay(compass, targetPos);
    }

    public static void clearCompassTarget(ItemStack compass) {
        if (!compass.is(Items.COMPASS)) {
            return;
        }

        NavigatorCompassItemData.clearTarget(compass);

        updateCompassDisplayCleared(compass);
    }

    private static void updateCompassDisplay(ItemStack compass, BlockPos targetPos) {
        compass.set(DataComponents.CUSTOM_NAME, Component.literal("Navigator Compass").withStyle(style -> style.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ())
                .withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("Right-click to change target").withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        compass.set(DataComponents.LORE, new ItemLore(lore));
    }

    private static void updateCompassDisplayCleared(ItemStack compass) {
        compass.set(DataComponents.CUSTOM_NAME, Component.literal("Navigator Compass").withStyle(style -> style.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("No target set").withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("Right-click to set target").withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        compass.set(DataComponents.LORE, new ItemLore(lore));
    }
}
