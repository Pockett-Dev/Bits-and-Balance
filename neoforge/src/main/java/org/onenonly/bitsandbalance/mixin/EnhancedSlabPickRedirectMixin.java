package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.Mth;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-only pick redirect for Enhanced Slab visual offsets.
 *
 * <p>When we visually move a block down onto a bottom slab, part of its (visual) model can
 * overlap the slab's block-space. Vanilla ray picking will hit the slab first, which makes it
 * feel like the bottom part of the block can't be hovered/outlined.
 *
 * <p>This mixin redirects the pick result from the supporting slab to the visually-offset block
 * above when the ray actually intersects the offset block's outline shape.
 *
 * <p>Targets {@link Entity} because {@code pick()} is defined there (not overridden by Player
 * in 1.21.11+). Only runs logic when the entity is a Player.
 */
@Mixin(Entity.class)
public abstract class EnhancedSlabPickRedirectMixin {

    @Inject(method = "pick(DFZ)Lnet/minecraft/world/phys/HitResult;", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$redirectPick(double maxDistance, float partialTick, boolean hitFluids,
                                             CallbackInfoReturnable<HitResult> cir) {
        if (!((Object) this instanceof Player self)) return;
        if (!Config.enableEnhancedSlabs) return;

        Level level = self.level();
        Vec3 start = self.getEyePosition(partialTick);
        Vec3 end = start.add(self.getViewVector(partialTick).scale(maxDistance));

        HitResult base = cir.getReturnValue();

        // Don't interfere with entity targeting.
        if (base.getType() == HitResult.Type.ENTITY) return;

        // Only override vanilla if we find an offset-hit that is closer than the original.
        double maxAllowedDist2 = maxDistance * maxDistance;
        if (base instanceof BlockHitResult baseHit && baseHit.getType() == HitResult.Type.BLOCK) {
            maxAllowedDist2 = baseHit.getLocation().distanceToSqr(start) + 1.0e-7;
        }

        // Vanilla can return MISS when the visually-lowered portion of a block lives
        // in the slab's block-space. Do our own short traversal and test offset shapes.
        boolean expandScanTriggers = base.getType() == HitResult.Type.MISS;
        BlockHitResult redirected = bitsandbalance$findOffsetHit(self, level, start, end, maxDistance, expandScanTriggers, maxAllowedDist2);
        if (redirected != null) {
            cir.setReturnValue(redirected);
        }
    }

    private static BlockHitResult bitsandbalance$findOffsetHit(Player self, Level level, Vec3 start, Vec3 end, double maxDistance,
                                                              boolean expandScanTriggers, double maxAllowedDist2) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        int x = Mth.floor(start.x);
        int y = Mth.floor(start.y);
        int z = Mth.floor(start.z);

        int stepX = bitsandbalance$sign(dx);
        int stepY = bitsandbalance$sign(dy);
        int stepZ = bitsandbalance$sign(dz);

        double tMaxX = bitsandbalance$intBound(start.x, dx);
        double tMaxY = bitsandbalance$intBound(start.y, dy);
        double tMaxZ = bitsandbalance$intBound(start.z, dz);

        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dy);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);

        // Conservative cap: at most ~3 cells per block of reach.
        int maxSteps = (int) (maxDistance * 3.0) + 16;

        BlockHitResult best = null;
        double bestDist2 = maxAllowedDist2;

        for (int steps = 0; steps < maxSteps; steps++) {
            BlockPos cellPos = new BlockPos(x, y, z);
            BlockState cellState = level.getBlockState(cellPos);

            // Only bother scanning around slab cells (common case for down-shift) and
            // door cells (two-block tall shapes can overlap the neighboring cell after offset).
            boolean shouldScan = bitsandbalance$isOffsetAnchor(level, cellPos, cellState);
            boolean scanUp = true;
            boolean scanDown = true;

            // When vanilla returns MISS, the ray can be traveling through the "gap" left by a
            // visually-shifted full cube (e.g., a block moved down by a BS). In that case the
            // intersected shape often belongs to a different position entirely.
            if (!shouldScan && expandScanTriggers && !cellState.isAir() && !EnhancedSlabHelper.shouldExcludeFromVisualOffset(cellState)) {
                double cellOff = EnhancedSlabHelper.getVisualYOffset(level, cellPos, cellState);
                if (cellOff != 0.0) {
                    shouldScan = true;
                    scanUp = cellOff < 0.0;
                    scanDown = cellOff > 0.0;
                }
            }

            // If we're in an air voxel while vanilla reports MISS, the hit shape may belong to a
            // neighboring block whose offset moved part of its outline into this air cell.
            // Only scan if there's evidence nearby to avoid doing heavy work when looking at the sky.
            if (!shouldScan && expandScanTriggers && cellState.isAir()) {
                shouldScan = bitsandbalance$airCellNearOffsetCandidate(level, cellPos);
                if (shouldScan) {
                    scanUp = true;
                    scanDown = true;
                }
            }

            if (shouldScan) {
                best = bitsandbalance$scanVertical(self, level, start, end, cellPos, best, bestDist2, scanUp, scanDown);
                if (best != null) {
                    bestDist2 = best.getLocation().distanceToSqr(start);
                    // Early exit if we're extremely close; no need to continue.
                    if (bestDist2 < 1.0e-6) return best;
                }
            }

            // Step to next voxel cell along ray.
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }

            // Stop once we've advanced past the end of the segment.
            double t = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));
            if (t > 1.0) break;
        }

        return best;
    }

    private static boolean bitsandbalance$airCellNearOffsetCandidate(Level level, BlockPos cellPos) {
        // 6-neighborhood. If any neighbor is a slab/door (common) or has non-zero visual offset,
        // then this air cell might contain shifted outline geometry.
        BlockPos[] neighbors = new BlockPos[] {
                cellPos.above(),
                cellPos.below(),
                cellPos.north(),
                cellPos.south(),
                cellPos.east(),
                cellPos.west()
        };

        for (BlockPos nPos : neighbors) {
            BlockState nState = level.getBlockState(nPos);
            if (nState.isAir()) continue;
            if (bitsandbalance$isOffsetAnchor(level, nPos, nState)) return true;
            if (EnhancedSlabHelper.shouldExcludeFromVisualOffset(nState)) continue;
            double off = EnhancedSlabHelper.getVisualYOffset(level, nPos, nState);
            if (off != 0.0) return true;
        }

        return false;
    }

    private static BlockHitResult bitsandbalance$scanVertical(Player self, Level level, Vec3 start, Vec3 end, BlockPos basePos,
                                                             BlockHitResult best, double bestDist2,
                                                             boolean scanUp, boolean scanDown) {
        // Scan a reasonable window above and below the slab cell.
        // This supports chains with multiple bottom slabs (cumulative offsets).
        final int maxScan = 32;

        if (scanUp) {
            for (int up = 1; up <= maxScan; up++) {
                BlockPos pos = basePos.above(up);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (EnhancedSlabHelper.shouldExcludeFromVisualOffset(state)) continue;
                double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
                if (yOff >= 0.0) continue;

                VoxelShape shape = state.getShape(level, pos, CollisionContext.of(self));
                BlockHitResult hit = shape.clip(start, end, pos);
                if (hit == null) continue;

                double dist2 = hit.getLocation().distanceToSqr(start);
                if (dist2 < bestDist2) {
                    bestDist2 = dist2;
                    best = hit;
                }
            }
        }

        if (scanDown) {
            for (int down = 1; down <= maxScan; down++) {
                BlockPos pos = basePos.below(down);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (EnhancedSlabHelper.shouldExcludeFromVisualOffset(state)) continue;
                double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
                if (yOff <= 0.0) continue;

                VoxelShape shape = state.getShape(level, pos, CollisionContext.of(self));
                BlockHitResult hit = shape.clip(start, end, pos);
                if (hit == null) continue;

                double dist2 = hit.getLocation().distanceToSqr(start);
                if (dist2 < bestDist2) {
                    bestDist2 = dist2;
                    best = hit;
                }
            }
        }

        return best;
    }

    private static boolean bitsandbalance$isOffsetAnchor(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof SlabBlock
            || state.getBlock() instanceof DoorBlock
            || EnhancedSlabHelper.hasFullBottomSurface(level, pos, state);
    }

    private static int bitsandbalance$sign(double v) {
        if (v > 0.0) return 1;
        if (v < 0.0) return -1;
        return 0;
    }

    private static double bitsandbalance$intBound(double s, double ds) {
        if (ds > 0.0) {
            return (Math.floor(s + 1.0) - s) / ds;
        }
        if (ds < 0.0) {
            return (s - Math.floor(s)) / (-ds);
        }
        return Double.POSITIVE_INFINITY;
    }
}
