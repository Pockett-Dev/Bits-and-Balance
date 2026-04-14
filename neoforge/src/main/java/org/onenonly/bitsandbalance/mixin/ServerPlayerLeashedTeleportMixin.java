package org.onenonly.bitsandbalance.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.mechanics.modules.LeashedTeleportModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge fallback hook: ensures Leashed Teleport runs even when a teleport path
 * doesn't emit a NeoForge teleport event (e.g., some short-range/player-only teleports).
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerLeashedTeleportMixin {

    @Inject(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
            at = @At("RETURN")
    )
    private void bitsandbalance$leashedTeleport_postTeleport(CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        LeashedTeleportModule.onPlayerTeleportedByMixin((ServerPlayer) (Object) this);
    }
}
