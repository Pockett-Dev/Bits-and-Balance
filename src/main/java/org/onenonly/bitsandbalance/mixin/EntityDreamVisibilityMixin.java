package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornPurposes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDreamVisibilityMixin {
    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$avatarInvisibility(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (DreamProjectionClient.isGhost(entity)) {
            cir.setReturnValue(true);
        } else if (DreamProjectionClient.isBody(entity)) {
            Player local = Minecraft.getInstance().player;
            cir.setReturnValue(local == null || !local.getUUID().equals(
                    DreamProjectionClient.owner(entity, DreamProjection.BODY_NAME_PREFIX)));
        } else if (entity instanceof Player projected && DreamProjectionClient.isProjected(projected)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$dreamVisibility(Player viewer, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (DreamProjectionClient.isGhost(entity)) {
            cir.setReturnValue(viewer.getUUID().equals(
                    DreamProjectionClient.owner(entity, DreamProjection.GHOST_NAME_PREFIX))
                    || !GoatHornPurposes.hasDreamHorn(viewer));
        } else if (DreamProjectionClient.isBody(entity)) {
            cir.setReturnValue(!viewer.getUUID().equals(
                    DreamProjectionClient.owner(entity, DreamProjection.BODY_NAME_PREFIX)));
        } else if (entity instanceof Player projected && DreamProjectionClient.isProjected(projected)) {
            cir.setReturnValue(!GoatHornPurposes.hasDreamHorn(viewer));
        }
    }
}
