package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.SoulFireFlameRenderStateAccess;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererSoulFireFlamesMixin {

    @Inject(
            method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private void bitsandbalance$markSoulFireFlameState(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (!(state instanceof SoulFireFlameRenderStateAccess access)) return;

        boolean soul = false;
        if (Config.soulFireOverlayEnabled && entity != null) {
            Level level = entity.level();
            soul = SoulFireOverlayClient.shouldUseSoulFireFlamesNow(level, entity);
        }

        access.bitsandbalance$setSoulFireFlame(soul);
    }
}
