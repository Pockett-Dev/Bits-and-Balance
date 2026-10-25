package org.onenonly.bitsandbalance.mixin.sable;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.network.EnhancedSlabMaterialSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Synchronizes position-backed slab materials after Sable's ordered sublevel full-sync bundle. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem", remap = false)
public abstract class SableSubLevelTrackingEnhancedSlabMaterialSyncMixin {
    @Inject(
            method = "sendFullSync(Lnet/minecraft/server/level/ServerPlayer;Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("TAIL"),
            require = 0,
            remap = false
    )
    private void bitsandbalance$syncEnhancedSlabMaterials(ServerPlayer player,
                                                           @Coerce Object serverSubLevel,
                                                           CustomPacketPayload supplementalPayload,
                                                           CallbackInfo ci) {
        EnhancedSlabMaterialSync.onSableFullSync(player, serverSubLevel);
    }
}
