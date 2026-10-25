package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;
import org.onenonly.bitsandbalance.fabric.mobs.ImprovedPhantomCarryHelper;
import org.onenonly.bitsandbalance.fabric.mobs.PhantomDismountTracker;
import org.onenonly.bitsandbalance.fabric.mobs.PhantomPickupTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Phantom improvements:
 * - Stacking slowness on successful phantom hits.
 * - Optional pickup/carry.
 * - Speed reduction while carrying.
 */
@Mixin(Phantom.class)
public abstract class PhantomMixin implements PhantomDismountTracker {

    @Unique
    private static final Identifier BITSANDBALANCE$PHANTOM_CARRY_SPEED_ID =
            Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, "phantom_carry_speed");

    @Unique
    private UUID bitsandbalance$hadPlayerRider;

    @Unique
    private UUID bitsandbalance$lastRider;

    @Unique
    private long bitsandbalance$lastDismountTime = -1L;

    @Override
    public UUID bitsandbalance$getLastRider() {
        return bitsandbalance$lastRider;
    }

    @Override
    public long bitsandbalance$getLastDismountTime() {
        return bitsandbalance$lastDismountTime;
    }

    @Override
    public void bitsandbalance$recordDismount(UUID riderUuid, long dismountTime) {
        bitsandbalance$lastRider = riderUuid;
        bitsandbalance$lastDismountTime = dismountTime;
        bitsandbalance$hadPlayerRider = null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$afterPhantomTick(CallbackInfo ci) {
        if (!FabricMobsConfig.enableImprovedPhantoms) return;

        Phantom phantom = (Phantom) (Object) this;
        if (phantom.level().isClientSide()) return;

        Player currentRider = null;
        for (var passenger : phantom.getPassengers()) {
            if (passenger instanceof Player p) {
                currentRider = p;
                break;
            }
        }

        if (currentRider != null) {
            bitsandbalance$handleAutomaticDrop(phantom, currentRider);
            bitsandbalance$hadPlayerRider = currentRider.getUUID();
        } else if (bitsandbalance$hadPlayerRider != null) {
            bitsandbalance$recordDismount(bitsandbalance$hadPlayerRider, phantom.level().getGameTime());
        }

        bitsandbalance$applyCarrySpeedModifier(phantom);
    }

    @Unique
    private void bitsandbalance$handleAutomaticDrop(Phantom phantom, Player player) {
        if (!(player instanceof PhantomPickupTracker tracker)) return;

        long now = phantom.level().getGameTime();
        if (tracker.bitsandbalance$getPhantomPickupTime() < 0L
                || tracker.bitsandbalance$getPhantomPickupVehicleId() != phantom.getId()) {
            tracker.bitsandbalance$setPhantomPickupState(now, phantom.getId());
        }

        if (!FabricMobsConfig.enablePhantomAutoDrop) return;

        long elapsedTicks = now - tracker.bitsandbalance$getPhantomPickupTime();
        long autoDropTicks = (long) Math.max(1, FabricMobsConfig.phantomAutoDropSeconds) * 20L;
        if (elapsedTicks < autoDropTicks) return;

        bitsandbalance$recordDismount(player.getUUID(), now);
        var server = phantom.level().getServer();
        if (server == null) {
            ImprovedPhantomCarryHelper.forceDrop(player, phantom, tracker, false);
            return;
        }

        server.execute(() -> ImprovedPhantomCarryHelper.forceDrop(player, phantom, tracker, false));
    }

    @Unique
    private static void bitsandbalance$applyCarrySpeedModifier(Phantom phantom) {
        if (!FabricMobsConfig.enablePhantomPickup) {
            bitsandbalance$removeCarrySpeedModifier(phantom);
            return;
        }

        boolean carryingPlayer = phantom.getPassengers().stream().anyMatch(p -> p instanceof Player);
        if (!carryingPlayer) {
            bitsandbalance$removeCarrySpeedModifier(phantom);
            return;
        }

        AttributeInstance moveSpeed = phantom.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) return;

        double multiplier = FabricMobsConfig.phantomPickupSpeedMultiplier;
        multiplier = Math.max(0.1D, Math.min(1.0D, multiplier));
        double amount = multiplier - 1.0D;

        AttributeModifier existing = moveSpeed.getModifier(BITSANDBALANCE$PHANTOM_CARRY_SPEED_ID);
        if (existing != null && Double.compare(existing.amount(), amount) == 0 && existing.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            return;
        }

        moveSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                BITSANDBALANCE$PHANTOM_CARRY_SPEED_ID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    @Unique
    private static void bitsandbalance$removeCarrySpeedModifier(Phantom phantom) {
        AttributeInstance moveSpeed = phantom.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed == null) return;
        moveSpeed.removeModifier(BITSANDBALANCE$PHANTOM_CARRY_SPEED_ID);
    }
}
