package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.block.Blocks;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.network.FabricNavigatorCompassNetworking;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;

import java.util.Optional;

/**
 * Fabric port: Navigator Compass (server-side interaction)
 * Right-click a compass to open the coordinate GUI.
 */
public final class FabricNavigatorCompass {
    private FabricNavigatorCompass() {
    }

    public static void initServer() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!FabricMechanicsConfig.enableNavigatorCompass) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.COMPASS)) return InteractionResult.PASS;

            BlockPos clickedPos = hitResult.getBlockPos();
            if (!level.getBlockState(clickedPos).is(Blocks.LODESTONE)) return InteractionResult.PASS;

            try {
                NavigatorCompassItemData.clearNavigatorTargetOnly(stack);
                stack.set(
                    DataComponents.LODESTONE_TRACKER,
                    new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), clickedPos)), true)
                );

                stack.remove(DataComponents.CUSTOM_NAME);
                stack.remove(DataComponents.LORE);
                stack.remove(DataComponents.ENCHANTMENTS);
                stack.remove(DataComponents.STORED_ENCHANTMENTS);

                return InteractionResult.SUCCESS;
            } catch (Throwable ignored) {
                return InteractionResult.PASS;
            }
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!FabricMechanicsConfig.enableNavigatorCompass) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.COMPASS)) return InteractionResult.PASS;

            // Migrate any legacy targets (older versions used LODESTONE_TRACKER)
            NavigatorCompassItemData.migrateLegacyLodestoneTargetIfPresent(stack);

            // Shift + right-click toggles display mode (persistent) when both are enabled.
            if (serverPlayer.isShiftKeyDown()
                    && FabricMechanicsConfig.navigatorCompassShowCoordinates
                    && FabricMechanicsConfig.navigatorCompassShowDistance) {
                NavigatorCompassItemData.toggleMode(stack);
                return InteractionResult.CONSUME;
            }

            BlockPos defaultPos = getCompassTarget(stack);
            if (defaultPos == null) {
                defaultPos = serverPlayer.blockPosition();
            }

            FabricNavigatorCompassNetworking.sendOpenGui(serverPlayer, defaultPos.getX(), defaultPos.getY(), defaultPos.getZ());
            return InteractionResult.CONSUME;
        });
    }

    private static BlockPos getCompassTarget(ItemStack compass) {
        return NavigatorCompassItemData.getTargetPos(compass);
    }
}
