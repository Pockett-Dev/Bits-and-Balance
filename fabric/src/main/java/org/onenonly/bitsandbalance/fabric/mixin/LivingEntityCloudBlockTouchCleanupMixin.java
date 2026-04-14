package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCloudBlockTouchCleanupMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$cleanupCloudJumpState(CallbackInfo ci) {
        CloudBlock.cleanupEntityStateIfNotTouchingCloud((LivingEntity) (Object) this);
    }
}
