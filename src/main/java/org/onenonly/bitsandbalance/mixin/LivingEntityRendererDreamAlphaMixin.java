package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
//? if mc == 1.21.1
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
//? else
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.renderer.state.level.CameraRenderState;
//? else
import net.minecraft.client.renderer.state.CameraRenderState;
//? endif
//? endif

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererDreamAlphaMixin {
    //? if mc == 1.21.1
    @ModifyConstant(method = "render", constant = @Constant(intValue = 654311423))
    private int bitsandbalance$dreamAlpha(int original, LivingEntity entity, float yaw, float partialTick,
                                          PoseStack pose, MultiBufferSource buffers, int light) {
        return DreamProjectionClient.isGhost(entity)
                || entity instanceof Player player && DreamProjectionClient.isProjected(player)
                ? DreamProjectionClient.ghostColor(entity) : original;
    }
    //? else
    @ModifyConstant(method = "submit", constant = @Constant(intValue = 654311423))
    private int bitsandbalance$dreamAlpha(int original, LivingEntityRenderState state, PoseStack pose,
                                          SubmitNodeCollector collector, CameraRenderState camera) {
        return state instanceof AvatarRenderState avatar && DreamProjectionClient.isProjected(avatar.id)
                ? DreamProjectionClient.ghostColor(avatar.id) : original;
    }
    //? endif
}
