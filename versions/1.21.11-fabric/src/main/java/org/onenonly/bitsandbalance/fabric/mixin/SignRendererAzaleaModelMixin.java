package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.onenonly.bitsandbalance.common.registry.ModWoodTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(SignRenderer.class)
public abstract class SignRendererAzaleaModelMixin {
    @Shadow
    @Final
    @Mutable
    private Map<WoodType, Object> signModels;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bitsandbalance$addAzaleaSignGeometry(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        if (ModWoodTypes.AZALEA_WOOD_TYPE == WoodType.OAK || signModels.containsKey(ModWoodTypes.AZALEA_WOOD_TYPE)) {
            return;
        }

        Object oakModels = signModels.get(WoodType.OAK);
        if (oakModels == null) {
            return;
        }

        Map<WoodType, Object> copy = new HashMap<>(signModels);
        copy.put(ModWoodTypes.AZALEA_WOOD_TYPE, oakModels);
        signModels = copy;
    }
}
