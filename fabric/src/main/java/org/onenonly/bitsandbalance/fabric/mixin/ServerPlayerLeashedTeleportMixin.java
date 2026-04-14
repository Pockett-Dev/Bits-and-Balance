package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.fabric.mechanics.FabricLeashedTeleport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks player teleports so leashed followers are captured even for short-range teleports
 * (chorus fruit, ender pearls, modded teleports, etc.).
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerLeashedTeleportMixin {

    @Inject(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
            at = @At("HEAD")
    )
    private void bitsandbalance$leashedTeleport_preTeleport(CallbackInfoReturnable<Boolean> cir) {
        FabricLeashedTeleport.capturePreTeleport((ServerPlayer) (Object) this);
    }

    @Inject(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
            at = @At("RETURN")
    )
    private void bitsandbalance$leashedTeleport_postTeleport(CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        FabricLeashedTeleport.processPostTeleport((ServerPlayer) (Object) this);
    }
}
