package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererSittingOffsetMixin {
    private static final double SITTING_RENDER_Y_OFFSET = -0.65D;

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$applySittingRenderOffset(AbstractClientPlayer player, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (player.isPassenger()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean isLocalPlayer = minecraft != null
                && minecraft.player != null
                && minecraft.player.getUUID().equals(player.getUUID());

        boolean sitting = isLocalPlayer
                ? FabricClientState.sitting
                : FabricClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
        if (!sitting) {
            return;
        }

        cir.setReturnValue(cir.getReturnValue().add(0.0D, SITTING_RENDER_Y_OFFSET, 0.0D));
    }
}