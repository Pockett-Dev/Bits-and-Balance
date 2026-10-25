package org.onenonly.bitsandbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Seeds a stable Bioluminescence self-light value before LambDynamicLights
 * layers its entity, held-item, and nearby dynamic-light result on top.
 */
@Mixin(value = EntityRenderer.class, priority = 1100)
public class LambDynamicLightsEntityRendererMixin<T extends Entity> {
    @ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"))
    private int bitsandbalance$stabilizeBioluminescenceSelfLight(int original, T entity, BlockPos pos) {
        BioluminescenceLightManager.observeEntity(entity);
        int entityLuminance = BioluminescenceLightManager.getEntityLuminance(entity);
        return Math.max(original, entityLuminance);
    }
}
