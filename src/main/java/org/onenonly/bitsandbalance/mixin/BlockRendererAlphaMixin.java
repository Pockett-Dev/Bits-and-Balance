package org.onenonly.bitsandbalance.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.CloudModelAlphaRenderContext;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRendererAlphaMixin {

    @ModifyVariable(method = "renderBatched", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private VertexConsumer bitsandbalance$wrapBottleCloudAlpha(VertexConsumer original, BlockState state) {
        int alpha = CloudModelAlphaRenderContext.currentAlpha();
        if (!CloudModelAlphaRenderContext.isActive() || alpha >= 255 || state.getBlock() != ModBottleOfCloud.cloudBlock()) {
            return original;
        }
        return new AlphaMultiplierVertexConsumer(original, alpha);
    }

    private static final class AlphaMultiplierVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alphaMultiplier;

        private AlphaMultiplierVertexConsumer(VertexConsumer delegate, int alpha) {
            this.delegate = delegate;
            this.alphaMultiplier = Math.max(0.0F, Math.min(1.0F, alpha / 255.0F));
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, scaleAlpha(alpha));
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            int alpha = (argb >>> 24) & 0xFF;
            int rgb = argb & 0x00FFFFFF;
            delegate.setColor((scaleAlpha(alpha) << 24) | rgb);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            delegate.setLineWidth(width);
            return this;
        }

        private int scaleAlpha(int alpha) {
            return Math.max(0, Math.min(255, Math.round(alpha * alphaMultiplier)));
        }
    }
}