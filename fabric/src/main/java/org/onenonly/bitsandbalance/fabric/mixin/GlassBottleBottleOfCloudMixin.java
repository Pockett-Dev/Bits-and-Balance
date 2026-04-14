package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudItemTransforms;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.item.BottleItem.class)
public abstract class GlassBottleBottleOfCloudMixin {
    private static final double MIN_Y = 192.0D;

    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$bottleCloudAboveSky(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!BottleOfCloudRuntime.enabled) return;
        if (player.getY() < MIN_Y) return;

        if (cir.getReturnValue() != InteractionResult.PASS) return;

        ItemStack bottle = player.getItemInHand(hand);
        ItemStack transformedStack = BottleOfCloudItemTransforms.fillCloudBottle(bottle, player);
        if (!level.isClientSide()) {
            player.setItemInHand(hand, transformedStack);
        }
        cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(transformedStack));

        if (level.isClientSide()) return;

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9F, 1.0F);
    }
}
