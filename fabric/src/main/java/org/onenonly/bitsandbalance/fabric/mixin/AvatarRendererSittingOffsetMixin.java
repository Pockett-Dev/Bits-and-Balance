package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client: translate sitting players down so they sit on the ground.
 *
 * NeoForge does this via RenderPlayerEvent.Pre; on Fabric we adjust the renderer's render offset.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererSittingOffsetMixin {

    private static final double SITTING_RENDER_Y_OFFSET = -0.65D;

    @Inject(
            method = "getRenderOffset(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void bitsandbalance$applySittingRenderOffset(AvatarRenderState state, CallbackInfoReturnable<Vec3> cir) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return;

            int entityId = state.id;
            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof Player player)) return;
            if (player.isPassenger()) return;

            boolean isLocal = mc.player != null && mc.player.getUUID().equals(player.getUUID());
            boolean sitting = (isLocal && FabricClientState.sitting)
                    || FabricClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
            if (!sitting) return;

            Vec3 original = cir.getReturnValue();
            if (original == null) original = Vec3.ZERO;

            // Boat/riding-style seated pose raises feet; lower the model so it rests on the ground.
            // Tuned by request.
            cir.setReturnValue(original.add(0.0D, SITTING_RENDER_Y_OFFSET, 0.0D));
        } catch (Throwable ignored) {
        }
    }
}
