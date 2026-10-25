package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonHeadRenderer.class)
public class PistonHeadRendererMixedSlabRenderMixin {
	@Shadow @Final private BlockRenderDispatcher blockRenderer;

	@Inject(
			method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At("HEAD"),
			cancellable = true,
			require = 0
	)
	private void bitsandbalance$renderMovingEnhancedSlab(PistonMovingBlockEntity piston,
														 float partialTick,
														 PoseStack poseStack,
														 MultiBufferSource buffer,
														 int packedLight,
														 int packedOverlay,
														 CallbackInfo ci) {
		Level level = piston.getLevel();
		BlockState movedState = piston.getMovedState();
		if (level == null || movedState == null || !EnhancedSlabTerrainRenderHelper.handles(movedState)) {
			return;
		}

		BlockPos destPos = piston.getBlockPos();
		BlockPos renderPos = destPos.relative(piston.getMovementDirection().getOpposite());
		poseStack.pushPose();
		poseStack.translate(piston.getXOff(partialTick), piston.getYOff(partialTick), piston.getZOff(partialTick));
		try {
			if (EnhancedSlabTerrainRenderHelper.renderMoving(this.blockRenderer, level, renderPos, destPos, movedState, piston, poseStack, buffer, packedOverlay)) {
				ci.cancel();
			}
		} finally {
			poseStack.popPose();
		}
	}
}