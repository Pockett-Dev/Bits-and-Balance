package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerAttributesMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void bitsandbalance$addAerodynamicDrag(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(BitsAndBalanceFabric.AERODYNAMIC_DRAG), 0.0D);
    }
}
