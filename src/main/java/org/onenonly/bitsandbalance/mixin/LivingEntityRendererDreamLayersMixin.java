package org.onenonly.bitsandbalance.mixin;

//? if mc == 1.21.1
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererDreamLayersMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"))
    private void bitsandbalance$hideDreamEquipment(RenderLayer<?, ?> layer, PoseStack pose,
                                                     MultiBufferSource buffers, int light, Entity entity,
                                                     float limbSwing, float limbSwingAmount, float partialTick,
                                                     float age, float headYaw, float headPitch) {
        if (!(entity instanceof Player player && DreamProjectionClient.isProjected(player))) {
            ((RenderLayer) layer).render(pose, buffers, light, (LivingEntity) entity, limbSwing, limbSwingAmount,
                    partialTick, age, headYaw, headPitch);
        }
    }
}
//? endif
