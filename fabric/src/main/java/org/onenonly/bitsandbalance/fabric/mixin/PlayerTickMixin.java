package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.onenonly.bitsandbalance.fabric.mobs.ImprovedPhantomCarryHelper;
import org.onenonly.bitsandbalance.fabric.mobs.PhantomPickupTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detect player attacks while mounted on (or just dismounted from) a phantom.
 */
@Mixin(Player.class)
public abstract class PlayerTickMixin implements PhantomPickupTracker {

    @Unique
    private long bitsandbalance$phantomLastProcTick = -1L;

    @Unique
    private long bitsandbalance$phantomLastCarryTick = -1L;

    @Unique
    private long bitsandbalance$phantomPickupTime = -1L;

    @Unique
    private int bitsandbalance$phantomPickupVehicleId = -1;

    @Unique
    private boolean bitsandbalance$phantomHitDuringCarry = false;

    @Inject(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            require = 1
    )
    private void bitsandbalance$onHurtServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricMobsConfig.enableImprovedPhantoms) return;

        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof Phantom phantom)) return;

        long nowTick = player.level().getGameTime();
        if (bitsandbalance$phantomLastProcTick == nowTick) return;
        bitsandbalance$phantomLastProcTick = nowTick;

        if (FabricMobsConfig.enablePhantomSlownessStacking) {
            bitsandbalance$applyStackingSlowness(player);
        }

        if (FabricMobsConfig.enablePhantomPickup && !player.isPassenger()) {
            double roll = phantom.getRandom().nextDouble();
            if (roll < FabricMobsConfig.phantomPickupChance) {
                bitsandbalance$attemptPhantomPickup(phantom, player);
            }
        }
    }
    @Unique
    private boolean bitsandbalance$wasSwinging = false;

    @Unique
    private int bitsandbalance$mountedHitCooldown = 0;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void bitsandbalance$afterTick(CallbackInfo ci) {
        bitsandbalance$handleCarryTick();
    }

    @Inject(method = "rideTick", at = @At("TAIL"), require = 0)
    private void bitsandbalance$afterRideTick(CallbackInfo ci) {
        bitsandbalance$handleCarryTick();
    }

    @Unique
    private void bitsandbalance$handleCarryTick() {
        if (!FabricMobsConfig.enableImprovedPhantoms) return;

        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) return;

        long nowTick = player.level().getGameTime();
        if (bitsandbalance$phantomLastCarryTick == nowTick) return;
        bitsandbalance$phantomLastCarryTick = nowTick;

        bitsandbalance$enforcePickupMinDelay(player);

        if (bitsandbalance$mountedHitCooldown > 0) {
            bitsandbalance$mountedHitCooldown--;
        }

        // Vanilla often doesn't let you "target" your own mount. NeoForge only multiplies incoming damage,
        // but on Fabric we also need to ensure a hit happens at all while carried.
        if ((FabricMobsConfig.enablePhantomDoubleDamage || FabricMobsConfig.enablePhantomDropOnHit || FabricMobsConfig.enablePhantomSlowFallingOnHitDismount)
                && bitsandbalance$mountedHitCooldown == 0
                && player.isPassenger()
                && player.getVehicle() instanceof Phantom phantom) {

            boolean isSwinging = player.swinging;
            if (isSwinging && !bitsandbalance$wasSwinging) {
                float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (baseDamage > 0.0f && phantom.isAlive()) {
                    this.bitsandbalance$setPhantomHitDuringCarry(true);
                    phantom.hurt(player.damageSources().playerAttack(player), baseDamage);
                    if (FabricMobsConfig.enablePhantomDropOnHit) {
                        ImprovedPhantomCarryHelper.forceDrop(player, phantom, this, true);
                    }
                    bitsandbalance$mountedHitCooldown = 10;
                }
            }

            bitsandbalance$wasSwinging = isSwinging;
            return;
        }

        bitsandbalance$wasSwinging = player.swinging;
    }

    @Unique
    private static void bitsandbalance$enforcePickupMinDelay(Player player) {
        int minDelaySeconds = Math.max(0, FabricMobsConfig.phantomPickupMinDelay);
        if (minDelaySeconds <= 0) {
            PlayerTickMixin self;
            try {
                self = (PlayerTickMixin) (Object) player;
            } catch (ClassCastException ignored) {
                return;
            }

            if (!(player.isPassenger() && player.getVehicle() instanceof Phantom) && self.bitsandbalance$phantomPickupTime >= 0L) {
                ImprovedPhantomCarryHelper.finalizeEndedCarry(player, self);
            }
            return;
        }

        PlayerTickMixin self;
        try {
            self = (PlayerTickMixin) (Object) player;
        } catch (ClassCastException ignored) {
            return;
        }

        if (self.bitsandbalance$phantomPickupTime < 0L || self.bitsandbalance$phantomPickupVehicleId < 0) return;

        long now = player.level().getGameTime();
        long elapsed = now - self.bitsandbalance$phantomPickupTime;
        long minDelayTicks = (long) minDelaySeconds * 20L;

        if (elapsed >= minDelayTicks) {
            boolean stillCarriedBySamePhantom = player.isPassenger()
                    && player.getVehicle() instanceof Phantom phantom
                    && phantom.getId() == self.bitsandbalance$phantomPickupVehicleId;
            if (!stillCarriedBySamePhantom) {
                ImprovedPhantomCarryHelper.finalizeEndedCarry(player, self);
            }
            return;
        }

        if (player.isPassenger() && player.getVehicle() instanceof Phantom) return;

        int phantomId = self.bitsandbalance$phantomPickupVehicleId;
        if (phantomId < 0) return;

        var maybe = player.level().getEntity(phantomId);
        if (!(maybe instanceof Phantom phantom) || !phantom.isAlive()) {
            ImprovedPhantomCarryHelper.finalizeEndedCarry(player, self);
            return;
        }

        if (player.distanceTo(phantom) > 8.0F) return;

        try {
            player.startRiding(phantom, true, true);
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static void bitsandbalance$applyStackingSlowness(Player player) {
        MobEffectInstance current = player.getEffect(MobEffects.SLOWNESS);

        int targetLevel = 1;
        int durationTicks = Math.max(1, FabricMobsConfig.phantomSlownessDuration) * 20;

        if (current != null) {
            int currentLevel = current.getAmplifier() + 1;
            targetLevel = Math.min(currentLevel + 1, FabricMobsConfig.phantomSlownessMaxLevel);
            durationTicks = Math.max(durationTicks, current.getDuration());
        }

        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, targetLevel - 1));
    }

    @Unique
    private void bitsandbalance$attemptPhantomPickup(Phantom phantom, Player player) {
        boolean success;
        try {
            success = player.startRiding(phantom, true, true);
        } catch (Throwable t) {
            success = false;
        }
        if (!success) return;

        this.bitsandbalance$phantomPickupTime = player.level().getGameTime();
        this.bitsandbalance$phantomPickupVehicleId = phantom.getId();
        this.bitsandbalance$phantomHitDuringCarry = false;
    }

    @Override
    public long bitsandbalance$getPhantomPickupTime() {
        return bitsandbalance$phantomPickupTime;
    }

    @Override
    public int bitsandbalance$getPhantomPickupVehicleId() {
        return bitsandbalance$phantomPickupVehicleId;
    }

    @Override
    public boolean bitsandbalance$didHitPhantomDuringCarry() {
        return bitsandbalance$phantomHitDuringCarry;
    }

    @Override
    public void bitsandbalance$setPhantomPickupState(long pickupTime, int vehicleId) {
        this.bitsandbalance$phantomPickupTime = pickupTime;
        this.bitsandbalance$phantomPickupVehicleId = vehicleId;
        this.bitsandbalance$phantomHitDuringCarry = false;
    }

    @Override
    public void bitsandbalance$setPhantomHitDuringCarry(boolean hitDuringCarry) {
        this.bitsandbalance$phantomHitDuringCarry = hitDuringCarry;
    }

    @Override
    public void bitsandbalance$clearPhantomPickupState() {
        this.bitsandbalance$phantomPickupTime = -1L;
        this.bitsandbalance$phantomPickupVehicleId = -1;
        this.bitsandbalance$phantomHitDuringCarry = false;
    }
}
