package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabRenderOffsetContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class EnhancedSlabBlockEntityRenderOffsetMixin {

    private static final ThreadLocal<Boolean> bitsandbalance$pushed =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "setupAndRender", at = @At("HEAD"))
    private static void bitsandbalance$offsetBlockEntity(
            BlockEntityRenderer<BlockEntity> renderer, BlockEntity blockEntity, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, CallbackInfo ci) {

        if (!Config.enableEnhancedSlabs) return;
        if (blockEntity == null) return;
        if (!(blockEntity.getLevel() instanceof ClientLevel level)) return;

        double yOff = EnhancedSlabHelper.getVisualYOffset(level, blockEntity.getBlockPos(), blockEntity.getBlockState());
        if (yOff != 0.0) {
            poseStack.pushPose();
            poseStack.translate(0.0, yOff, 0.0);
            EnhancedSlabRenderOffsetContext.pushBlockEntitySubmit();
            bitsandbalance$pushed.set(Boolean.TRUE);
        }
    }

    @Inject(method = "setupAndRender", at = @At("RETURN"))
    private static void bitsandbalance$endSubmit(
            BlockEntityRenderer<BlockEntity> renderer, BlockEntity blockEntity, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, CallbackInfo ci) {
        if (bitsandbalance$pushed.get()) {
            EnhancedSlabRenderOffsetContext.popBlockEntitySubmit();
            poseStack.popPose();
            bitsandbalance$pushed.set(Boolean.FALSE);
        }
    }

    @Inject(method = "renderItem", at = @At("HEAD"), require = 0)
    private <E extends BlockEntity> void bitsandbalance$resetLevelLessItemBlockEntityShaderColor(
            E blockEntity,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            CallbackInfoReturnable<Boolean> cir) {
        if (blockEntity == null || blockEntity.getLevel() != null) {
            return;
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}