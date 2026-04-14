package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.onenonly.bitsandbalance.fabric.mobs.MountedPhantomAttackContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;

/**
 * Applies the 2x damage multiplier when a player hits a phantom while being carried by it.
 *
 * This is intentionally not tied to Better Combat presence: it also covers vanilla/manual attacks.
 */
@Mixin(LivingEntity.class)
public abstract class BetterCombatDamageMixin {

    @ModifyArgs(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"
            ),
            require = 1
    )
    private void bitsandbalance$modifyPhantomDamage(Args args) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!FabricMobsConfig.enableImprovedPhantoms || !FabricMobsConfig.enablePhantomDoubleDamage) return;
        if (!(self instanceof Phantom phantom)) return;

        float amount = args.get(2);
        if (amount <= 0.0f) return;

        if (MountedPhantomAttackContext.matches(phantom)) {
            args.set(2, amount * 2.0f);
            return;
        }

        Object maybeSource = args.get(1);
        if (!(maybeSource instanceof DamageSource source)) return;

        Player player = null;
        if (source.getEntity() instanceof Player p) {
            player = p;
        } else if (source.getDirectEntity() instanceof Player p) {
            player = p;
        }
        if (player == null) return;

        if (player.isPassenger() && player.getVehicle() == phantom) {
            args.set(2, amount * 2.0f);
        }
    }
}
