package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Armed Armor Stands - Off-Hand Support
 * Allows players to place items in armor stand off-hand by shift-right-clicking.
 */
@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$handleOffHandInteraction(Player player, net.minecraft.world.phys.Vec3 vec3, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FabricTweaksConfig.enableArmedArmorStands) return;

        ArmorStand armorStand = (ArmorStand) (Object) this;
        ItemStack itemStack = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && !itemStack.isEmpty()) {
            if (!armorStand.showArms()) {
                armorStand.setShowArms(true);
            }

            ItemStack offHandItem = armorStand.getOffhandItem();
            if (offHandItem.isEmpty()) {
                armorStand.setItemSlot(EquipmentSlot.OFFHAND, itemStack.copy());

                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }

                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}
