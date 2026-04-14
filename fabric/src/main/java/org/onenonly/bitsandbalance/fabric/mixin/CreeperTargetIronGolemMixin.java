package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperTargetIronGolemMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$preventTargetIronGolem(LivingEntity target, CallbackInfo ci) {
        if (!FabricMobsConfig.enableIronGolemsKillCreepers) return;
        if (target instanceof IronGolem) {
            ci.cancel();

            // If the creeper was swelling, attempt to stop it.
            Creeper self = (Creeper) (Object) this;
            try {
                self.setSwellDir(-1);
            } catch (Throwable ignored) {
                // ignore if mappings differ
            }
        }
    }
}
