package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.onenonly.bitsandbalance.compat.BetterCombatCompat;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to modify damage dealt to phantoms when players are mounted on them.
 * This works with Better Combat's damage system by modifying the damage amount
 * after Better Combat has calculated it.
 */
@Mixin(LivingEntity.class)
public abstract class BetterCombatDamageMixin {

    /**
     * Modify damage dealt to phantoms when the attacker is mounted on them.
     * This applies our 2x damage multiplier for mounted phantom attacks.
     */
    @ModifyVariable(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"), argsOnly = true)
    private float bitsandbalance$modifyPhantomDamage(float amount, DamageSource source) {
        if (((LivingEntity) (Object) this).level().isClientSide()) {
            return amount;
        }

        if (!(((LivingEntity) (Object) this) instanceof Phantom)) {
            return amount;
        }
        Phantom phantom = (Phantom) ((LivingEntity) (Object) this);

        if (!(source.getEntity() instanceof Player)) {
            return amount;
        }
        Player player = (Player) source.getEntity();
        
        if (player == null) {
            return amount;
        }

        if (!BetterCombatCompat.isBetterCombatLoaded() || !Config.enableBetterCombatIntegration) {
            return amount;
        }

        // Check if the player is mounted on this phantom
        if (player.isPassenger() && player.getVehicle() == phantom) {

            // Apply our 2x damage multiplier
            return amount * 2.0f;
        }

        return amount;
    }
}
