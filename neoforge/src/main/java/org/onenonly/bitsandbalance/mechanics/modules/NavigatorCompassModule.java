package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;
import org.onenonly.bitsandbalance.network.NavigatorCompassOpenGUIPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Navigator Compass Module - Allows players to set custom coordinates for compass navigation.
 * Stores target coordinates in mod-owned CustomData (does not use LODESTONE_TRACKER).
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class NavigatorCompassModule implements FeatureModule {

    @Override
    public String getFeatureName() {
        return "Navigator Compass";
    }

    @Override
    public boolean isEnabled() {
        return Config.enableNavigatorCompass;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.enableNavigatorCompass) return;

        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) return;

        Level level = event.getLevel();
        if (level.isClientSide()) return;

        ItemStack item = event.getItemStack();
        if (!item.is(Items.COMPASS)) return;

        BlockPos clickedPos = event.getPos();
        if (!level.getBlockState(clickedPos).is(Blocks.LODESTONE)) return;

        try {
            // Convert: clear Navigator target but preserve mode flag, then set lodestone tracker.
            NavigatorCompassItemData.clearNavigatorTargetOnly(item);
            item.set(
                DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), clickedPos)), true)
            );

            // Reset visual customization so it behaves/looks like a normal Lodestone Compass.
            item.remove(DataComponents.CUSTOM_NAME);
            item.remove(DataComponents.LORE);
            item.remove(DataComponents.ENCHANTMENTS);
            item.remove(DataComponents.STORED_ENCHANTMENTS);

            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!Config.enableNavigatorCompass) return;
        
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        ItemStack item = event.getItemStack();
        if (!item.is(Items.COMPASS)) return;

        // Migrate any legacy targets (older versions used LODESTONE_TRACKER)
        NavigatorCompassItemData.migrateLegacyLodestoneTargetIfPresent(item);

        // Shift + right-click toggles display mode (persistent) when both options are enabled.
        if (serverPlayer.isShiftKeyDown()
                && Config.navigatorCompassShowCoordinates
                && Config.navigatorCompassShowDistance) {
            NavigatorCompassItemData.toggleMode(item);
            event.setCanceled(true);
            return;
        }
        
        // Get current target or player position as default
        BlockPos currentTarget = getCompassTarget(item);
        BlockPos playerPos = serverPlayer.blockPosition();
        
        // Use current target if available, otherwise use player position
        int defaultX = currentTarget != null ? currentTarget.getX() : playerPos.getX();
        int defaultY = currentTarget != null ? currentTarget.getY() : playerPos.getY();
        int defaultZ = currentTarget != null ? currentTarget.getZ() : playerPos.getZ();
        
        // Open GUI
        openNavigatorCompassGUI(serverPlayer, defaultX, defaultY, defaultZ);
        
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (!Config.enableNavigatorCompass) return;
        
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // Check if player is holding a compass
        ItemStack mainHand = serverPlayer.getMainHandItem();
        ItemStack offHand = serverPlayer.getOffhandItem();
        
        ItemStack compass = null;
        if (mainHand.is(Items.COMPASS)) {
            compass = mainHand;
        } else if (offHand.is(Items.COMPASS)) {
            compass = offHand;
        }
        
        if (compass == null) return;

        NavigatorCompassItemData.migrateLegacyLodestoneTargetIfPresent(compass);
        
        // Set compass target to player's current position
        BlockPos playerPos = serverPlayer.blockPosition();
        setCompassTarget(compass, (ServerLevel) serverPlayer.level(), playerPos);
        
        serverPlayer.sendSystemMessage(Component.literal("§aCompass target set to current position: §f" +
            playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ()));
        
        // Play success sound
        serverPlayer.level().playSound(null, serverPlayer.blockPosition(), 
            net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 
            net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    /**
     * Set compass target in mod-owned CustomData.
     */
    public static void setCompassTarget(ItemStack compass, ServerLevel level, BlockPos targetPos) {
        if (!compass.is(Items.COMPASS)) return;

        String dimId;
        try {
            dimId = level.dimension().identifier().toString();
        } catch (Throwable t) {
            dimId = "";
        }

        NavigatorCompassItemData.setTarget(compass, dimId, targetPos);
        
        // Remove any enchantments that might have been added
        compass.remove(DataComponents.ENCHANTMENTS);
        compass.remove(DataComponents.STORED_ENCHANTMENTS);
        
        // Update compass name and lore
        updateCompassDisplay(compass, targetPos);
    }

    /**
     * Clear compass target so it points to world spawn again.
     */
    public static void clearCompassTarget(ItemStack compass) {
        if (!compass.is(Items.COMPASS)) return;

        NavigatorCompassItemData.clearTarget(compass);
        
        // Update lore to show no target
        updateCompassDisplayCleared(compass);
    }

    /**
     * Get the current compass target position.
     */
    public static BlockPos getCompassTarget(ItemStack compass) {
        if (!compass.is(Items.COMPASS)) return null;
        return NavigatorCompassItemData.getTargetPos(compass);
    }

    /**
     * Check if compass has a custom target set.
     */
    public static boolean hasCustomTarget(ItemStack compass) {
        return getCompassTarget(compass) != null;
    }

    /**
     * Calculate distance to target coordinates.
     */
    public static double getDistanceToTarget(ServerPlayer player, BlockPos target) {
        BlockPos playerPos = player.blockPosition();
        double deltaX = target.getX() - playerPos.getX();
        double deltaY = target.getY() - playerPos.getY();
        double deltaZ = target.getZ() - playerPos.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /**
     * Generate compass tooltip text based on configuration.
     */
    public static Component getCompassTooltip(ItemStack compass, ServerPlayer player) {
        BlockPos target = getCompassTarget(compass);
        if (target == null) {
            return Component.literal("§7Points to world spawn");
        }

        StringBuilder tooltip = new StringBuilder();
        tooltip.append("§aNavigator Compass\n");
        
        if (Config.navigatorCompassShowCoordinates) {
            tooltip.append("§fTarget: ").append(target.getX()).append(", ")
                   .append(target.getY()).append(", ").append(target.getZ()).append("\n");
        }
        
        if (Config.navigatorCompassShowDistance) {
            double distance = getDistanceToTarget(player, target);
            tooltip.append("§eDistance: ").append(String.format("%.1f", distance)).append(" blocks");
        }
        
        return Component.literal(tooltip.toString());
    }

    /**
     * Open Navigator Compass GUI on client.
     */
    private static void openNavigatorCompassGUI(ServerPlayer player, int defaultX, int defaultY, int defaultZ) {
        NavigatorCompassOpenGUIPayload payload = new NavigatorCompassOpenGUIPayload(defaultX, defaultY, defaultZ);
        player.connection.send(payload);
    }

    /**
     * Update compass display with custom name and lore.
     */
    private static void updateCompassDisplay(ItemStack compass, BlockPos targetPos) {
        // Set custom name (white, not gold, not italic)
        compass.set(DataComponents.CUSTOM_NAME, Component.literal("Navigator Compass").withStyle(style -> style.withItalic(false)));
        
        // Create lore with coordinates
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal(targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()).withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("Right-click to change target").withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        
        compass.set(DataComponents.LORE, new ItemLore(lore));
    }

    /**
     * Update compass display when cleared (keep name, show no target).
     */
    private static void updateCompassDisplayCleared(ItemStack compass) {
        // Keep the Navigator Compass name (white, not italic)
        compass.set(DataComponents.CUSTOM_NAME, Component.literal("Navigator Compass").withStyle(style -> style.withItalic(false)));
        
        // Create lore showing no target
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("No target set").withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("Right-click to set target").withStyle(style -> style.withColor(0xAAAAAA).withItalic(false)));
        
        compass.set(DataComponents.LORE, new ItemLore(lore));
    }

    /**
     * Reset compass display to default.
     */
    private static void resetCompassDisplay(ItemStack compass) {
        // Remove custom name and lore
        compass.remove(DataComponents.CUSTOM_NAME);
        compass.remove(DataComponents.LORE);
    }
}