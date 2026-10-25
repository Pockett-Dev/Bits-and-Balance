package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.onenonly.bitsandbalance.Config;

/**
 * Tweaks: Armed Armor Stands - Off-Hand Support
 * Allows players to place items in armor stand off-hand by shift-right-clicking.
 */
@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void rebalance$handleOffHandInteraction(Player player, net.minecraft.world.phys.Vec3 vec3, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!Config.enableArmedArmorStands) return;
        
        ArmorStand armorStand = (ArmorStand) (Object) this;
        ItemStack itemStack = player.getItemInHand(hand);
        
        // Check if player is shift-right-clicking (trying to place in off-hand)
        if (player.isShiftKeyDown() && !itemStack.isEmpty()) {
            // Check if armor stand has arms
            if (!armorStand.showArms()) {
                armorStand.setShowArms(true);
            }
            
            // Check if off-hand slot is empty
            ItemStack offHandItem = armorStand.getOffhandItem();
            if (offHandItem.isEmpty()) {
                // Place item in off-hand
                armorStand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, itemStack.copy());
                
                // Consume item from player's hand if not in creative
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }
                
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
        }
        
        // For all other cases (regular right-click, empty hand, etc.), let vanilla handle it
        // Don't call cir.setReturnValue() or cir.cancel() - let the method continue normally
    }
}
