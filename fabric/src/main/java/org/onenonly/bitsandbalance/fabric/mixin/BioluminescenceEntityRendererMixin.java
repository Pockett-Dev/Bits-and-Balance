package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class BioluminescenceEntityRendererMixin<T extends Entity> {
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$applyBioluminescenceEntityLight(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BioluminescenceLightManager.observeEntity(entity);
        int vanilla = cir.getReturnValueI();
        int entityLuminance = BioluminescenceLightManager.getEntityLuminance(entity);
        int nearbyDynamic = (int) BioluminescenceLightManager.getDynamicLightLevel(pos);
        cir.setReturnValue(Math.max(vanilla, Math.max(entityLuminance, nearbyDynamic)));
    }
}