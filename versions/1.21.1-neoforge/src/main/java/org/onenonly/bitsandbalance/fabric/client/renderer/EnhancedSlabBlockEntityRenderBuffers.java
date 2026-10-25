package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

final class EnhancedSlabBlockEntityRenderBuffers {
    private EnhancedSlabBlockEntityRenderBuffers() {
    }

    static MultiBufferSource withoutBlockEntityCrumbling(MultiBufferSource buffer) {
        if (buffer == null || buffer instanceof MultiBufferSource.BufferSource) {
            return buffer;
        }

        String className = buffer.getClass().getName();
        if (!className.contains("LevelRenderer") || !className.contains("$$Lambda")) {
            return buffer;
        }

        return Minecraft.getInstance().renderBuffers().bufferSource();
    }
}