package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class ColorMultiplierVertexConsumer implements VertexConsumer {
	private final VertexConsumer delegate;
	private final float rMul;
	private final float gMul;
	private final float bMul;

	public ColorMultiplierVertexConsumer(VertexConsumer delegate, float rMul, float gMul, float bMul) {
		this.delegate = delegate;
		this.rMul = rMul;
		this.gMul = gMul;
		this.bMul = bMul;
	}

	private static int clampByte(int value) {
		if (value < 0) return 0;
		if (value > 255) return 255;
		return value;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		delegate.addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		int rr = clampByte(Math.round(r * rMul));
		int gg = clampByte(Math.round(g * gMul));
		int bb = clampByte(Math.round(b * bMul));
		delegate.setColor(rr, gg, bb, a);
		return this;
	}

	@Override
	public VertexConsumer setColor(int argb) {
		int a = (argb >>> 24) & 0xFF;
		int r = (argb >>> 16) & 0xFF;
		int g = (argb >>> 8) & 0xFF;
		int b = argb & 0xFF;
		return setColor(r, g, b, a);
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
	public VertexConsumer setLineWidth(float lineWidth) {
		delegate.setLineWidth(lineWidth);
		return this;
	}
}
