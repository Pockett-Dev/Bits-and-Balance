package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.onenonly.bitsandbalance.common.entity.SoulFireBlockHelper;
import org.onenonly.bitsandbalance.common.entity.SoulFireBurnAccess;
import org.onenonly.bitsandbalance.network.SoulFireBurnSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class LivingEntitySoulFireBurnMixin implements SoulFireBurnAccess {
    @Unique
    private static final String BITSANDBALANCE$SOUL_FIRE_BURN_KEY = "bitsandbalance_soul_fire_burn";

    @Unique
    private boolean bitsandbalance$soulFireBurn;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bitsandbalance$trackSoulFireBurn(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        boolean wasSoulFireBurn = bitsandbalance$isSoulFireBurn();

        if (!self.isOnFire()) {
            bitsandbalance$setSoulFireBurn(false);
            bitsandbalance$syncIfChanged(self, wasSoulFireBurn);
            return;
        }

        AABB fireBox;
        try {
            fireBox = SoulFireBlockHelper.fireCheckBox(self.getBoundingBox());
        } catch (Throwable ignored) {
            return;
        }

        if (SoulFireBlockHelper.isTouchingSoulFire(level, fireBox)) {
            bitsandbalance$setSoulFireBurn(true);
            bitsandbalance$syncIfChanged(self, wasSoulFireBurn);
            return;
        }

        if (SoulFireBlockHelper.isTouchingNormalFire(level, fireBox)) {
            bitsandbalance$setSoulFireBurn(false);
        }

        bitsandbalance$syncIfChanged(self, wasSoulFireBurn);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void bitsandbalance$readSoulFireBurn(ValueInput input, CallbackInfo ci) {
        bitsandbalance$setSoulFireBurn(input.getBooleanOr(BITSANDBALANCE$SOUL_FIRE_BURN_KEY, false));
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void bitsandbalance$writeSoulFireBurn(ValueOutput output, CallbackInfo ci) {
        if (bitsandbalance$isSoulFireBurn()) {
            output.putBoolean(BITSANDBALANCE$SOUL_FIRE_BURN_KEY, true);
        }
    }

    @Override
    public boolean bitsandbalance$isSoulFireBurn() {
        return bitsandbalance$soulFireBurn;
    }

    @Override
    public void bitsandbalance$setSoulFireBurn(boolean soulFireBurn) {
        bitsandbalance$soulFireBurn = soulFireBurn;
    }

    @Unique
    private void bitsandbalance$syncIfChanged(Entity self, boolean previousSoulFireBurn) {
        boolean currentSoulFireBurn = bitsandbalance$isSoulFireBurn();
        if (currentSoulFireBurn != previousSoulFireBurn) {
            SoulFireBurnSync.syncIfServer(self, currentSoulFireBurn);
        }
    }
}