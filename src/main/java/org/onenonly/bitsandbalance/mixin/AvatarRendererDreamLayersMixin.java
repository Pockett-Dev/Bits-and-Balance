package org.onenonly.bitsandbalance.mixin;

//? if mc != 1.21.1
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererDreamLayersMixin {
    @Inject(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z",
            at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$hideDreamEquipment(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
        if (DreamProjectionClient.isProjected(state.id)) {
            cir.setReturnValue(false);
        }
    }
}
//? endif
