package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.Equippable;
import org.onenonly.bitsandbalance.common.client.UsesForCursesClient;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Equippable.class)
public abstract class EquippableCurseUsesMixin {

    @Inject(
            method = "cameraOverlay()Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void bitsandbalance$hideVanishingPumpkinCameraOverlay(CallbackInfoReturnable<Optional<Identifier>> cir) {
        Optional<Identifier> overlay = cir.getReturnValue();
        if (overlay.isEmpty()) {
            return;
        }

        if (UsesForCursesClient.shouldHidePumpkinCameraOverlayId(overlay.get(), FabricTweaksConfig.curseHidePumpkinOverlayOnVanishing)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}