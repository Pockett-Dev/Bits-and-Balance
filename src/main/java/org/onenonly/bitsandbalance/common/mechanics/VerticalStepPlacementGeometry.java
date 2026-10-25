package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.Direction;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;

/**
 * Pure geometry helpers for placing a vertical step against an occupied vertical-step quadrant.
 */
public final class VerticalStepPlacementGeometry {

    private VerticalStepPlacementGeometry() {
    }

    /**
     * Chooses the target quadrant that shares the clicked horizontal face with an occupied source
     * quadrant. The axis perpendicular to that face is copied from the source quadrant instead of
     * being inferred from a potentially transformed or boundary-aligned hit coordinate.
     */
    public static AdjacentPlacement faceConnectedPlacement(Direction clickedFace, int occupiedSourceIndex) {
        if (clickedFace == null || !clickedFace.getAxis().isHorizontal()
                || occupiedSourceIndex < 0 || occupiedSourceIndex >= 4) {
            throw new IllegalArgumentException("A horizontal face and occupied vertical-step index are required");
        }

        Direction facing = clickedFace.getAxis() == Direction.Axis.Z
                ? clickedFace.getOpposite()
                : QuadVerticalStepBlock.facingForIndex(occupiedSourceIndex);
        Direction side = clickedFace.getAxis() == Direction.Axis.X
                ? clickedFace.getOpposite()
                : QuadVerticalStepBlock.sideForIndex(occupiedSourceIndex);
        return new AdjacentPlacement(facing, side);
    }

    /**
     * Resolves a raw hit quadrant to an occupied quadrant on the clicked outer face. The paired
     * quadrant fallback is what makes exact half-boundary hits deterministic.
     */
    public static int resolveOccupiedSourceIndex(Direction clickedFace, int rawHitIndex, int occupiedMask) {
        if (clickedFace == null || !clickedFace.getAxis().isHorizontal()
                || rawHitIndex < 0 || rawHitIndex >= 4) {
            return -1;
        }

        boolean east = clickedFace.getAxis() == Direction.Axis.X
                ? clickedFace == Direction.EAST
                : QuadVerticalStepBlock.isEast(rawHitIndex);
        boolean south = clickedFace.getAxis() == Direction.Axis.Z
                ? clickedFace == Direction.SOUTH
                : QuadVerticalStepBlock.isSouth(rawHitIndex);
        int preferred = QuadVerticalStepBlock.toIndex(east, south);
        if ((occupiedMask & (1 << preferred)) != 0) {
            return preferred;
        }

        int pairedOnFace = clickedFace.getAxis() == Direction.Axis.X
                ? QuadVerticalStepBlock.toIndex(east, !south)
                : QuadVerticalStepBlock.toIndex(!east, south);
        return (occupiedMask & (1 << pairedOnFace)) != 0 ? pairedOnFace : -1;
    }

    /** Used by runtime validation to guard all faces and source quadrants. */
    public static boolean sharesClickedFace(Direction clickedFace,
                                            int occupiedSourceIndex,
                                            AdjacentPlacement target) {
        if (clickedFace == null || !clickedFace.getAxis().isHorizontal()
                || occupiedSourceIndex < 0 || occupiedSourceIndex >= 4 || target == null) {
            return false;
        }

        Direction sourceFacing = QuadVerticalStepBlock.facingForIndex(occupiedSourceIndex);
        Direction sourceSide = QuadVerticalStepBlock.sideForIndex(occupiedSourceIndex);
        return clickedFace.getAxis() == Direction.Axis.X
                ? sourceSide == clickedFace
                && target.side() == clickedFace.getOpposite()
                && target.facing() == sourceFacing
                : sourceFacing == clickedFace
                && target.facing() == clickedFace.getOpposite()
                && target.side() == sourceSide;
    }

    /** Regression invariant covering all source quadrants and both horizontal axes. */
    public static boolean validatesBoundaryFallbackAndConnectivity() {
        for (int sourceIndex = 0; sourceIndex < 4; sourceIndex++) {
            int occupiedMask = 1 << sourceIndex;
            Direction xFace = QuadVerticalStepBlock.sideForIndex(sourceIndex);
            int oppositeDepthHit = QuadVerticalStepBlock.toIndex(
                    QuadVerticalStepBlock.isEast(sourceIndex),
                    !QuadVerticalStepBlock.isSouth(sourceIndex)
            );
            int resolvedX = resolveOccupiedSourceIndex(xFace, oppositeDepthHit, occupiedMask);
            if (resolvedX != sourceIndex
                    || !sharesClickedFace(xFace, resolvedX, faceConnectedPlacement(xFace, resolvedX))) {
                return false;
            }

            Direction zFace = QuadVerticalStepBlock.facingForIndex(sourceIndex);
            int oppositeWidthHit = QuadVerticalStepBlock.toIndex(
                    !QuadVerticalStepBlock.isEast(sourceIndex),
                    QuadVerticalStepBlock.isSouth(sourceIndex)
            );
            int resolvedZ = resolveOccupiedSourceIndex(zFace, oppositeWidthHit, occupiedMask);
            if (resolvedZ != sourceIndex
                    || !sharesClickedFace(zFace, resolvedZ, faceConnectedPlacement(zFace, resolvedZ))) {
                return false;
            }
        }
        return true;
    }

    public record AdjacentPlacement(Direction facing, Direction side) {
    }
}
