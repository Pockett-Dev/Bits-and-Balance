package org.onenonly.bitsandbalance.mechanics.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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

        if (QuickHarvestingLogic.tryHandleInteraction(player, level, event.getHand(), clickedPos, clickedState, Config.enableQuickHarvestingHoes, Config.enableQuickHarvestingAxes, Config.enableQuickHarvestingHomeDropsToUser)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
