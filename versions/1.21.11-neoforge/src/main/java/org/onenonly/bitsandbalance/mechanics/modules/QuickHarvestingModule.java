package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.QuickHarvestingLogic;
import org.onenonly.bitsandbalance.mechanics.FeatureModule;
import org.onenonly.bitsandbalance.mechanics.MechanicsUtils;

/**
 * Quick Harvesting Module
 * 
 * Provides enhanced crop harvesting mechanics for different tools and bare hands.
 * 
 * Features:
 * - Hand harvesting: Harvest mature crops with bare hands (single crop)
 * - Hoe harvesting: Harvest mature crops in a 3x3 area with hoes
 * - Axe harvesting: Harvest pumpkins, melons, and mature cocoa with axes
 * - Automatic replanting: Consumes one drop to replant crops when possible
 * - Tool durability: Applies durability damage to tools used
 * - Sound effects: Plays crop break sounds for feedback
 * - Configurable: Each tool type can be enabled/disabled independently
 * 
 * How it works:
 * - Right-click on mature crops with appropriate tools
 * - Harvests the crop and collects drops
 * - Automatically replants if seeds are available in drops
 * - Damages the tool used (except for hand harvesting)
 * - Plays harvest sound for feedback
 * 
 * Tool-specific behavior:
 * - Hands: Single crop only, no tool damage
 * - Hoes: 3x3 area harvesting, tool damage applied
 * - Axes: Pumpkins, melons, and cocoa, tool damage applied
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class QuickHarvestingModule implements FeatureModule {
    
    @Override
    public String getFeatureName() {
        return "Quick Harvesting";
    }
    
    @Override
    public boolean isEnabled() {
        return Config.enableQuickHarvesting;
    }
    
    @Override
    public int getInitializationPriority() {
        return 800; // Low priority - tool enhancement feature
    }
    
    /**
     * Main quick harvesting logic - triggered when player right-clicks blocks
     */
    @SubscribeEvent
    public static void onQuickHarvesting(PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer player = MechanicsUtils.validateServerPlayer(Config.enableQuickHarvesting, event.getEntity());
        if (player == null) return;

        Level level = event.getLevel();
        BlockPos clickedPos = event.getPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        try {
            boolean handled = QuickHarvestingLogic.tryHandleInteraction(
                    player,
                    level,
                    event.getHand(),
                    clickedPos,
                    clickedState,
                    Config.enableQuickHarvestingHoes,
                    Config.enableQuickHarvestingAxes,
                    Config.enableQuickHarvestingHomeDropsToUser
            );

            if (handled) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
}
