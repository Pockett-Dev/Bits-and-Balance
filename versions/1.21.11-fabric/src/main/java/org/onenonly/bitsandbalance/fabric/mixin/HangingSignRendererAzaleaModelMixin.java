package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
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

@Mixin(HangingSignRenderer.class)
public abstract class HangingSignRendererAzaleaModelMixin {
    @Shadow
    @Final
    @Mutable
    private Map<HangingSignRenderer.ModelKey, Model.Simple> hangingSignModels;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bitsandbalance$addAzaleaHangingSignGeometry(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        if (ModWoodTypes.AZALEA_WOOD_TYPE == WoodType.OAK) {
            return;
        }

        Map<HangingSignRenderer.ModelKey, Model.Simple> copy = new HashMap<>(hangingSignModels);
        for (HangingSignRenderer.AttachmentType attachmentType : HangingSignRenderer.AttachmentType.values()) {
            HangingSignRenderer.ModelKey azaleaKey = new HangingSignRenderer.ModelKey(ModWoodTypes.AZALEA_WOOD_TYPE, attachmentType);
            if (copy.containsKey(azaleaKey)) {
                continue;
            }

            Model.Simple oakModel = hangingSignModels.get(new HangingSignRenderer.ModelKey(WoodType.OAK, attachmentType));
            if (oakModel != null) {
                copy.put(azaleaKey, oakModel);
            }
        }
        hangingSignModels = copy;
    }
}
