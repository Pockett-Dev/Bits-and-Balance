package org.onenonly.bitsandbalance.common.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared particle placement math for partial-block Enhanced Slab shapes. */
public final class EnhancedSlabDestroyParticleGeometry {
    private EnhancedSlabDestroyParticleGeometry() {
    }

    /**
     * Emits the same regular debris grid used by the live particle paths. Each particle's velocity
     * is relative to the center of its occupied shape box, rather than the center of the block.
     */
    public static int emitGrid(BlockPos pos, VoxelShape shape, ParticleSink sink) {
        if (pos == null || shape == null || shape.isEmpty() || sink == null) return 0;

        int count = 0;
        for (AABB box : shape.toAabbs()) {
            double sizeX = box.maxX - box.minX;
            double sizeY = box.maxY - box.minY;
            double sizeZ = box.maxZ - box.minZ;
            int stepsX = Math.max(2, (int) Math.ceil(sizeX * 4.0D));
            int stepsY = Math.max(2, (int) Math.ceil(sizeY * 4.0D));
            int stepsZ = Math.max(2, (int) Math.ceil(sizeZ * 4.0D));

            for (int ix = 0; ix < stepsX; ix++) {
                for (int iy = 0; iy < stepsY; iy++) {
                    for (int iz = 0; iz < stepsZ; iz++) {
                        double x = pos.getX() + box.minX + ((ix + 0.5D) / stepsX) * sizeX;
                        double y = pos.getY() + box.minY + ((iy + 0.5D) / stepsY) * sizeY;
                        double z = pos.getZ() + box.minZ + ((iz + 0.5D) / stepsZ) * sizeZ;
                        ParticleMotion motion = motionFor(pos, box, x, y, z);
                        sink.emit(box, motion);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** Returns outward motion from the center of one occupied shape box. */
    public static ParticleMotion motionFor(BlockPos pos, AABB box, double x, double y, double z) {
        double centerX = pos.getX() + (box.minX + box.maxX) * 0.5D;
        double centerY = pos.getY() + (box.minY + box.maxY) * 0.5D;
        double centerZ = pos.getZ() + (box.minZ + box.maxZ) * 0.5D;
        return new ParticleMotion(x, y, z, x - centerX, y - centerY, z - centerZ);
    }

    @FunctionalInterface
    public interface ParticleSink {
        void emit(AABB box, ParticleMotion motion);
    }

    public record ParticleMotion(double x, double y, double z, double vx, double vy, double vz) {
    }
}
