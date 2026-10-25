package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class AlphaMultiplierVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alphaMultiplier;

    public AlphaMultiplierVertexConsumer(VertexConsumer delegate, int alpha) {
        this.delegate = delegate;
        this.alphaMultiplier = Math.max(0.0F, Math.min(1.0F, alpha / 255.0F));
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.delegate.setColor(red, green, blue, scaleAlpha(alpha));
        return this;
    }

    @Override
    public VertexConsumer setColor(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        this.delegate.setColor((scaleAlpha(alpha) << 24) | rgb);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    //? if mc == 26.3
    @Override
    public VertexConsumer setUv3(float u, float v) {
        this.delegate.setUv3(u, v);
        return this;
    }
    //? endif

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.delegate.setNormal(x, y, z);
        return this;
    }

    //? if mc != 1.21.1
    @Override
    public VertexConsumer setLineWidth(float width) {
        this.delegate.setLineWidth(width);
        return this;
    }
    //? endif

    private int scaleAlpha(int alpha) {
        return Math.max(0, Math.min(255, Math.round(alpha * this.alphaMultiplier)));
    }
}
