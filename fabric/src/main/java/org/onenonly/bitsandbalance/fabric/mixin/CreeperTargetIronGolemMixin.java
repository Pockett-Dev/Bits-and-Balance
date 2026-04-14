package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
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
            bitsandbalance$clearGolemRetaliation();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bitsandbalance$clearAnyGolemTarget(CallbackInfo ci) {
        if (!FabricMobsConfig.enableIronGolemsKillCreepers) return;

        Creeper self = (Creeper) (Object) this;
        if (self.getTarget() instanceof IronGolem) {
            self.setTarget(null);
            bitsandbalance$clearGolemRetaliation();
        }
    }

    private void bitsandbalance$clearGolemRetaliation() {
        Creeper self = (Creeper) (Object) this;
        try {
            self.setSwellDir(-1);
        } catch (Throwable ignored) {
        }
    }
}
