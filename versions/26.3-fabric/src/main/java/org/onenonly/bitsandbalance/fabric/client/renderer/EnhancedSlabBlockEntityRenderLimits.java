package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class EnhancedSlabBlockEntityRenderLimits {
    private static final int DEFAULT_VIEW_DISTANCE = 96;
    private static final int MIN_VIEW_DISTANCE = 32;
    private static final int MAX_VIEW_DISTANCE = 256;
    private static final int VIEW_DISTANCE = clamp(
            Integer.getInteger("bitsandbalance.enhancedSlabRenderDistance", DEFAULT_VIEW_DISTANCE),
            MIN_VIEW_DISTANCE,
            MAX_VIEW_DISTANCE
    );
    private static final double SUBMIT_DISTANCE_SQUARED = (VIEW_DISTANCE + 1.0D) * (VIEW_DISTANCE + 1.0D);

    private EnhancedSlabBlockEntityRenderLimits() {
    }

    public static int viewDistance() {
        return VIEW_DISTANCE;
    }

    static boolean shouldSubmit(BlockEntityRenderState state, CameraRenderState cameraRenderState) {
        if (state == null || state.blockPos == null || cameraRenderState == null) {
            return true;
        }
        return shouldSubmit(state.blockPos, cameraRenderState);
    }

    public static boolean shouldSubmit(BlockPos blockPos, CameraRenderState cameraRenderState) {
        if (blockPos == null || cameraRenderState == null) {
            return true;
        }
        Vec3 cameraPos = cameraRenderState.pos;
        if (cameraPos == null) {
            return true;
        }
        double dx = blockPos.getX() + 0.5D - cameraPos.x();
        double dy = blockPos.getY() + 0.5D - cameraPos.y();
        double dz = blockPos.getZ() + 0.5D - cameraPos.z();
        return dx * dx + dy * dy + dz * dz <= SUBMIT_DISTANCE_SQUARED;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}