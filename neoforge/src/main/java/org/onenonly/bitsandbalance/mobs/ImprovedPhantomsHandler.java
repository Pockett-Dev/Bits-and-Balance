package org.onenonly.bitsandbalance.mobs;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;


@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class ImprovedPhantomsHandler {
    private static final int HIT_DISMOUNT_SLOW_FALL_TICKS = 40;

    private ImprovedPhantomsHandler() {}

    private static final String KEY_LAST_PROC_TICK = "bitsandbalance_phantom_last_proc_tick";
    private static final String KEY_PICKUP_TIME = "bitsandbalance_phantom_pickup_time";
    private static final String KEY_PICKUP_VEHICLE_ID = "bitsandbalance_phantom_pickup_vehicle_id";
    private static final String KEY_HIT_DURING_CARRY = "bitsandbalance_phantom_hit_during_carry";

    private static final java.util.Map<java.util.UUID, MountedHitState> MOUNTED_HIT_STATE = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Identifier PHANTOM_CARRY_SPEED_ID = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "phantom_carry_speed");

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!Config.enableImprovedPhantoms) return;

        var living = event.getEntity();
        if (living.level().isClientSide()) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // Phantom -> Player: slowness stacking + pickup attempt
        if (living instanceof Player player && attacker instanceof Phantom phantom) {
            long nowTick = player.level().getGameTime();
            long lastTick = player.getPersistentData().getLong(KEY_LAST_PROC_TICK).orElse(-1L);
            if (lastTick == nowTick) return;
            player.getPersistentData().putLong(KEY_LAST_PROC_TICK, nowTick);

            if (Config.enablePhantomSlownessStacking) {
                applyStackingSlowness(player);
            }

            if (Config.enablePhantomPickup && !player.isPassenger()) {
                double roll = phantom.getRandom().nextDouble();
                if (roll < Config.phantomPickupChance) {
                    attemptPhantomPickup(phantom, player);
                }
            }
            return;
        }

        // Player -> Phantom: track mounted hits, optionally amplify damage, and optionally force a drop.
        if (living instanceof Phantom phantom && attacker instanceof Player player) {
            if (player.isPassenger() && player.getVehicle() == phantom) {
                player.getPersistentData().putBoolean(KEY_HIT_DURING_CARRY, true);

                if (Config.enablePhantomDoubleDamage) {
                    event.setAmount(event.getAmount() * 2.0F);
                }

                if (Config.enablePhantomDropOnHit) {
                    forceDrop(player, phantom, true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!Config.enableImprovedPhantoms) return;

        var entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        // Enforce minimum pickup dismount delay by re-mounting if the player dismounts too early.
        if (entity instanceof Player player) {
            enforcePickupMinDelay(player);
            handleMountedPhantomAttack(player);
            return;
        }

        // Apply phantom carry speed modifier when carrying a player.
        if (entity instanceof Phantom phantom) {
            handleAutomaticDrop(phantom);
            applyCarrySpeedModifier(phantom);
        }
    }

    /**
     * Vanilla usually prevents attacking your own mount.
     * While carried by a phantom, we detect arm swings and apply a hit directly.
     */
    private static void handleMountedPhantomAttack(Player player) {
        if (!Config.enablePhantomDoubleDamage) return;
        if (player.level().isClientSide()) return;

        MountedHitState state = MOUNTED_HIT_STATE.computeIfAbsent(player.getUUID(), id -> new MountedHitState());

        if (state.cooldownTicks > 0) {
            state.cooldownTicks--;
        }

        boolean isSwinging;
        try {
            isSwinging = player.swinging;
        } catch (Throwable t) {
            isSwinging = false;
        }

        boolean mountedOnPhantom = player.isPassenger() && player.getVehicle() instanceof Phantom;
        if (mountedOnPhantom && state.cooldownTicks == 0 && isSwinging && !state.wasSwinging) {
            Phantom phantom = (Phantom) player.getVehicle();
            if (phantom != null && phantom.isAlive()) {
                float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (baseDamage > 0.0F) {
                    phantom.hurt(player.damageSources().playerAttack(player), baseDamage);
                    state.cooldownTicks = 10;
                }
            }
        }

        // Track edge-trigger for next tick.
        state.wasSwinging = isSwinging;
    }

    private static final class MountedHitState {
        private boolean wasSwinging = false;
        private int cooldownTicks = 0;
    }

    private static void applyStackingSlowness(Player player) {
        MobEffectInstance current = player.getEffect(MobEffects.SLOWNESS);

        int targetLevel = 1; // Slowness I
        int durationTicks = Math.max(1, Config.phantomSlownessDuration) * 20;

        if (current != null) {
            int currentLevel = current.getAmplifier() + 1;
            targetLevel = Math.min(currentLevel + 1, Config.phantomSlownessMaxLevel);
            durationTicks = Math.max(durationTicks, current.getDuration());
        }

        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, durationTicks, targetLevel - 1));
    }

    private static void attemptPhantomPickup(Phantom phantom, Player player) {
        boolean success;
        try {
            success = player.startRiding(phantom, true, true);
        } catch (Throwable t) {
            success = false;
        }

        if (!success) return;

        long currentTime = player.level().getGameTime();
        player.getPersistentData().putLong(KEY_PICKUP_TIME, currentTime);
        player.getPersistentData().putInt(KEY_PICKUP_VEHICLE_ID, phantom.getId());
        player.getPersistentData().putBoolean(KEY_HIT_DURING_CARRY, false);
    }

    private static void enforcePickupMinDelay(Player player) {
        int minDelaySeconds = Math.max(0, Config.phantomPickupMinDelay);
        if (minDelaySeconds <= 0) {
            // If delay is disabled, clean up any stale data.
            if (hasTrackedCarry(player) && !(player.isPassenger() && player.getVehicle() instanceof Phantom)) {
                finalizeEndedCarry(player);
            }
            return;
        }

        if (!hasTrackedCarry(player)) {
            return;
        }

        long pickupTime = player.getPersistentData().getLong(KEY_PICKUP_TIME).orElse(0L);
        long now = player.level().getGameTime();
        long elapsed = now - pickupTime;
        long minDelayTicks = (long) minDelaySeconds * 20L;

        if (elapsed >= minDelayTicks) {
            int trackedPhantomId = player.getPersistentData().getInt(KEY_PICKUP_VEHICLE_ID).orElse(-1);
            boolean stillCarriedByTrackedPhantom = player.isPassenger()
                    && player.getVehicle() instanceof Phantom phantom
                    && phantom.getId() == trackedPhantomId;
            if (!stillCarriedByTrackedPhantom) {
                finalizeEndedCarry(player);
            }
            return;
        }

        // Still within min delay window: ensure player stays mounted.
        if (player.isPassenger() && player.getVehicle() instanceof Phantom) return;

        int phantomId = player.getPersistentData().getInt(KEY_PICKUP_VEHICLE_ID).orElse(-1);
        if (phantomId < 0) return;

        Entity maybe = player.level().getEntity(phantomId);
        if (!(maybe instanceof Phantom phantom) || !phantom.isAlive()) {
            finalizeEndedCarry(player);
            return;
        }

        // Re-mount if they're still close enough to reasonably be “carried”.
        if (player.distanceTo(phantom) > 8.0F) return;

        try {
            player.startRiding(phantom, true, true);
        } catch (Throwable t) {
            // ignore
        }
    }

    private static void handleAutomaticDrop(Phantom phantom) {
        if (!(phantom.getFirstPassenger() instanceof Player player)) return;

        long now = phantom.level().getGameTime();
        int phantomId = phantom.getId();

        if (!hasTrackedCarry(player)
                || player.getPersistentData().getInt(KEY_PICKUP_VEHICLE_ID).orElse(-1) != phantomId) {
            player.getPersistentData().putLong(KEY_PICKUP_TIME, now);
            player.getPersistentData().putInt(KEY_PICKUP_VEHICLE_ID, phantomId);
            player.getPersistentData().putBoolean(KEY_HIT_DURING_CARRY, false);
        }

        if (!Config.enablePhantomAutoDrop) return;

        long pickupTime = player.getPersistentData().getLong(KEY_PICKUP_TIME).orElse(now);
        long elapsedTicks = now - pickupTime;
        long autoDropTicks = (long) Math.max(1, Config.phantomAutoDropSeconds) * 20L;
        if (elapsedTicks < autoDropTicks) return;

        forceDrop(player, phantom, false);
    }

    private static void forceDrop(Player player, Phantom phantom, boolean hitTriggered) {
        if (hitTriggered) {
            player.getPersistentData().putBoolean(KEY_HIT_DURING_CARRY, true);
        }

        boolean hitDuringCarry = player.getPersistentData().getBoolean(KEY_HIT_DURING_CARRY).orElse(false);
        clearCarryState(player);

        if (player.isPassenger() && player.getVehicle() == phantom) {
            double dropX = player.getX();
            double dropY = Math.min(player.getY(), phantom.getY()) - 0.25D;
            double dropZ = player.getZ();

            player.stopRiding();
            player.teleportTo(dropX, dropY, dropZ);
            player.setDeltaMovement(player.getDeltaMovement().add(0.0D, -0.2D, 0.0D));
        }

        if (hitDuringCarry) {
            applyHitDismountSlowFalling(player);
        }
    }

    private static void finalizeEndedCarry(Player player) {
        boolean hitDuringCarry = player.getPersistentData().getBoolean(KEY_HIT_DURING_CARRY).orElse(false);
        clearCarryState(player);

        if (hitDuringCarry) {
            applyHitDismountSlowFalling(player);
        }
    }

    private static void clearCarryState(Player player) {
        player.getPersistentData().remove(KEY_PICKUP_TIME);
        player.getPersistentData().remove(KEY_PICKUP_VEHICLE_ID);
        player.getPersistentData().remove(KEY_HIT_DURING_CARRY);
    }

    private static boolean hasTrackedCarry(Player player) {
        return player.getPersistentData().contains(KEY_PICKUP_TIME)
                && player.getPersistentData().contains(KEY_PICKUP_VEHICLE_ID);
    }

    private static void applyHitDismountSlowFalling(Player player) {
        if (!Config.enablePhantomSlowFallingOnHitDismount) return;

        MobEffectInstance current = player.getEffect(MobEffects.SLOW_FALLING);
        int durationTicks = HIT_DISMOUNT_SLOW_FALL_TICKS;
        int amplifier = 0;

        if (current != null) {
            durationTicks = Math.max(durationTicks, current.getDuration());
            amplifier = Math.max(amplifier, current.getAmplifier());
        }

        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, durationTicks, amplifier));
    }

    private static void applyCarrySpeedModifier(Phantom phantom) {
        if (!Config.enablePhantomPickup) {
            removeCarrySpeedModifier(phantom);
            return;
        }

        boolean carryingPlayer = phantom.getPassengers().stream().anyMatch(p -> p instanceof Player);
        if (!carryingPlayer) {
            removeCarrySpeedModifier(phantom);
            return;
        }

        AttributeInstance moveSpeed = phantom.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) return;

        double multiplier = Config.phantomPickupSpeedMultiplier;
        // Config is defined as 0.1..1.0 (1.0 = normal speed)
        multiplier = Math.max(0.1D, Math.min(1.0D, multiplier));

        double amount = multiplier - 1.0D; // MULTIPLY_TOTAL expects delta from 1.0

        AttributeModifier existing = moveSpeed.getModifier(PHANTOM_CARRY_SPEED_ID);
        if (existing != null && Double.compare(existing.amount(), amount) == 0 && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            return;
        }

        moveSpeed.addOrUpdateTransientModifier(new AttributeModifier(PHANTOM_CARRY_SPEED_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeCarrySpeedModifier(Phantom phantom) {
        AttributeInstance moveSpeed = phantom.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) return;
        moveSpeed.removeModifier(PHANTOM_CARRY_SPEED_ID);
    }
}
