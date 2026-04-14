package org.onenonly.bitsandbalance.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Utility class for calculating HUD positioning adjustments based on player equipment
 * and item name display state. This helps ensure HUD elements like auto-walking and 
 * compass text are properly positioned when the player is wearing armor and when
 * item names are being displayed.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class HudPositioningHelper {
    
    /**
     * The vertical offset applied to HUD elements when armor is equipped.
     * This matches the distance that item names shift upward when armor slots are visible.
     */
    private static final int ARMOR_OFFSET_Y = -8;
    
    /**
     * The vertical offset applied to HUD elements when item names are being displayed.
     * We shift our HUD up so it never overlaps the vanilla selected-item name.
     */
    private static final int ITEM_NAME_OFFSET_Y = -22;
    
    /**
     * Tracks whether item names are currently being displayed above the hotbar.
     * This is updated by monitoring item changes and tooltip events.
     */
    private static boolean itemNameDisplayed = false;
    
    /**
     * Timer to track how long item names have been displayed.
     * Used to implement fade-out behavior.
     */
    private static int itemNameDisplayTicks = 0;
    
    /**
     * Maximum number of ticks to keep item name offset active after item name disappears.
     * This provides a smooth transition when item names fade away.
     */
    private static final int ITEM_NAME_FADE_TICKS = 40; // 2 seconds at 20 TPS
    
    /**
     * Tracks the last held item to detect item changes.
     */
    private static ItemStack lastHeldItem = ItemStack.EMPTY;
    
    /**
     * Timer for item name display after item changes.
     */
    private static int itemChangeTicks = 0;
    
    /**
     * Maximum ticks to show item name after item change.
     */
    private static final int ITEM_CHANGE_DISPLAY_TICKS = 40; // 2 seconds at 20 TPS
    
    /**
     * Checks if the player is wearing any armor pieces.
     * 
     * @param player The local player to check
     * @return true if the player has any armor equipped, false otherwise
     */
    public static boolean isWearingArmor(LocalPlayer player) {
        if (player == null) return false;
        
        // Check each armor slot for non-empty items
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        
        return !helmet.isEmpty() || !chestplate.isEmpty() || 
               !leggings.isEmpty() || !boots.isEmpty();
    }
    
    /**
     * Calculates the Y offset adjustment for HUD elements based on whether the player is wearing armor.
     * 
     * @param player The local player to check
     * @return The Y offset to apply (negative values move elements up)
     */
    public static int getArmorOffsetY(LocalPlayer player) {
        return isWearingArmor(player) ? ARMOR_OFFSET_Y : 0;
    }
    
    /**
     * Calculates the adjusted Y position for HUD elements, taking armor into account.
     * 
     * @param player The local player to check
     * @param baseY The base Y position (typically screenHeight - 56)
     * @return The adjusted Y position
     */
    public static int getAdjustedYPosition(LocalPlayer player, int baseY) {
        return baseY + getArmorOffsetY(player);
    }
    
    /**
     * Calculates the dynamic Y position for HUD elements, taking both armor and item name display into account.
     * This provides the smooth transition behavior requested.
     * 
     * @param player The local player to check
     * @param baseY The base Y position (typically screenHeight - 54)
     * @return The adjusted Y position with dynamic item name awareness
     */
    public static int getDynamicYPosition(LocalPlayer player, int baseY) {
        // Make item-change detection robust even on the very first render frame.
        // Sometimes RenderGui happens before our ClientTick runs, causing a one-frame overlap.
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                ItemStack currentItem = mc.player.getMainHandItem();
                if (!ItemStack.isSameItem(currentItem, lastHeldItem)) {
                    if (!currentItem.isEmpty()) {
                        markItemNameDisplayed();
                        itemChangeTicks = ITEM_CHANGE_DISPLAY_TICKS;
                    }
                    lastHeldItem = currentItem.copy();
                }
            }
        } catch (Throwable ignored) {
        }

        boolean hasArmor = isWearingArmor(player);
        boolean hasItemNames = itemNameDisplayed || itemNameDisplayTicks > 0 || itemChangeTicks > 0;

        int y = baseY;
        if (hasArmor) {
            y += getArmorOffsetY(player);
        }
        if (hasItemNames) {
            y += ITEM_NAME_OFFSET_Y;
        }
        return y;
    }
    
    
    /**
     * Updates the item name display state. Should be called every client tick.
     */
    public static void tick() {
        // Decrease timers
        if (itemNameDisplayTicks > 0) {
            itemNameDisplayTicks--;
        }
        if (itemChangeTicks > 0) {
            itemChangeTicks--;
        }
        
        // Check for item changes to trigger item name display
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            ItemStack currentItem = mc.player.getMainHandItem();
            if (!ItemStack.isSameItem(currentItem, lastHeldItem)) {
                // Item changed, trigger item name display
                if (!currentItem.isEmpty()) {
                    markItemNameDisplayed();
                    itemChangeTicks = ITEM_CHANGE_DISPLAY_TICKS;
                }
                lastHeldItem = currentItem.copy();
            }
        }
        
        // Update item name display state based on timers
        itemNameDisplayed = itemNameDisplayTicks > 0 || itemChangeTicks > 0;
    }
    
    /**
     * Marks that item names are currently being displayed.
     * This should be called when tooltips are rendered.
     */
    public static void markItemNameDisplayed() {
        itemNameDisplayed = true;
        itemNameDisplayTicks = ITEM_NAME_FADE_TICKS;
    }
    
    /**
     * Marks that item names are no longer being displayed.
     * This should be called when tooltips are no longer rendered.
     */
    public static void markItemNameHidden() {
        itemNameDisplayed = false;
        // Don't reset itemNameDisplayTicks immediately to allow for fade-out
    }
    
    /**
     * Client tick event handler to update item name display state.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tick();
    }
    
    /**
     * Event handler for tooltip rendering to detect when item names are displayed.
     */
    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        // Only trigger for tooltips that are likely item names above the hotbar
        // Skip inventory tooltips by checking if the game is in a screen (inventory, etc.)
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.screen == null) {
            // Only trigger when not in any screen (inventory, crafting, etc.)
            // This prevents false positives from inventory tooltips
            itemNameDisplayed = true;
            itemNameDisplayTicks = 10; // 0.5 seconds
        }
    }
}
