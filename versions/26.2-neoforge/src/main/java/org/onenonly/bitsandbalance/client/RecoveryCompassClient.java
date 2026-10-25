package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class RecoveryCompassClient {
    
    private static boolean showingCoords = false;
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.enableImprovedRecoveryCompass) return;
        if (!Config.recoveryCompassShowDistance && !Config.recoveryCompassShowCoordsOnRightClick) return;
        
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.level() == null) return;
        
        // Check if player is holding a recovery compass
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdingCompass = mainHand.getItem() == Items.RECOVERY_COMPASS || offHand.getItem() == Items.RECOVERY_COMPASS;
        
        if (holdingCompass) {
            // Update HUD overlay state
            RecoveryCompassHudOverlay.isActive = true;
            RecoveryCompassHudOverlay.showingCoords = showingCoords;
        } else {
            // Deactivate HUD overlay
            RecoveryCompassHudOverlay.isActive = false;
            showingCoords = false;
        }
    }
    
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!Config.enableImprovedRecoveryCompass) return;
        
        ItemStack item = event.getItemStack();
        if (item.getItem() != Items.RECOVERY_COMPASS) return;
        
        var player = event.getEntity();
        if (!(player instanceof LocalPlayer localPlayer)) return;
        
        // Check if shift is held down
        boolean isShiftHeld = localPlayer.isShiftKeyDown();
        
        if (!isShiftHeld && Config.recoveryCompassShowCoordsOnRightClick) {
            // Toggle between showing coordinates and distance
            showingCoords = !showingCoords;
            // Update HUD overlay state
            RecoveryCompassHudOverlay.showingCoords = showingCoords;

            // Prevent vanilla from also handling the click
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
