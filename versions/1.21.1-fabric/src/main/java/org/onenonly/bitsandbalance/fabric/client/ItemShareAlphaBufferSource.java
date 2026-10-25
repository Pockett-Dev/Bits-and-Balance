package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class ItemShareAlphaBufferSource implements MultiBufferSource {
	private final MultiBufferSource delegate;
	private final float alphaMultiplier;

	public ItemShareAlphaBufferSource(MultiBufferSource delegate, float alphaMultiplier) {
		this.delegate = delegate;
		this.alphaMultiplier = Math.max(0.0F, Math.min(1.0F, alphaMultiplier));
	}

	@Override
	public VertexConsumer getBuffer(RenderType renderType) {
		return new AlphaMultiplierVertexConsumer(delegate.getBuffer(renderType), alphaMultiplier);
	}

	private static final class AlphaMultiplierVertexConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final float alphaMultiplier;

		private AlphaMultiplierVertexConsumer(VertexConsumer delegate, float alphaMultiplier) {
			this.delegate = delegate;
			this.alphaMultiplier = alphaMultiplier;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			delegate.addVertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			delegate.setColor(red, green, blue, bitsandbalance$scaleAlpha(alpha));
			return this;
		}

		@Override
		public VertexConsumer setColor(int argb) {
			int alpha = (argb >>> 24) & 0xFF;
			int rgb = argb & 0x00FFFFFF;
			delegate.setColor((bitsandbalance$scaleAlpha(alpha) << 24) | rgb);
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

		private int bitsandbalance$scaleAlpha(int alpha) {
			return Math.max(0, Math.min(255, Math.round(alpha * alphaMultiplier)));
		}
	}
}