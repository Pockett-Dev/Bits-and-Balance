package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Finds a safe Returning Potion destination near the recorded death position.
 *
 * <p>The search only traverses connected player-sized space around the death
 * position. This prevents dimension-wide surface lookups from selecting the
 * Nether roof, an Overworld cave surface, or an equivalent ceiling in a custom
 * dimension.</p>
 */
public final class ReturningTeleportSafety {
    private static final int HORIZONTAL_SEARCH_RADIUS = 10;
    private static final int VERTICAL_SEARCH_RADIUS = 16;
    private static final Direction[] SEARCH_DIRECTIONS = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
        Direction.UP,
        Direction.DOWN
    };

    private ReturningTeleportSafety() {
    }

    public static BlockPos findSafeDestination(ServerLevel level, Player player, BlockPos deathPos) {
        BlockPos origin = deathPos.immutable();
        if (isSafeDestination(level, player, origin)) {
            return origin;
        }

        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(origin);
        visited.add(origin);

        while (!pending.isEmpty()) {
            BlockPos current = pending.removeFirst();
            for (Direction direction : SEARCH_DIRECTIONS) {
                BlockPos candidate = current.relative(direction).immutable();
                if (!isWithinSearchBounds(origin, candidate) || visited.contains(candidate)) {
                    continue;
                }
                if (!hasPlayerSpace(level, player, candidate)) {
                    continue;
                }
                if (!hasClearPathStep(level, player, current, candidate)) {
                    continue;
                }
                visited.add(candidate);
                if (hasSafeBodyEnvironment(level, candidate) && hasSafeSupport(level, candidate)) {
                    return candidate;
                }
                pending.addLast(candidate);
            }
        }

        return null;
    }

    private static boolean isWithinSearchBounds(BlockPos origin, BlockPos candidate) {
        return Math.abs(candidate.getX() - origin.getX()) <= HORIZONTAL_SEARCH_RADIUS
            && Math.abs(candidate.getY() - origin.getY()) <= VERTICAL_SEARCH_RADIUS
            && Math.abs(candidate.getZ() - origin.getZ()) <= HORIZONTAL_SEARCH_RADIUS;
    }

    private static boolean isSafeDestination(ServerLevel level, Player player, BlockPos pos) {
        return hasPlayerSpace(level, player, pos)
            && hasSafeBodyEnvironment(level, pos)
            && hasSafeSupport(level, pos);
    }

    private static boolean hasPlayerSpace(ServerLevel level, Player player, BlockPos pos) {
        //? if mc == 1.21.1
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        //? else
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        //? endif
        if (pos.getY() < minY || pos.getY() >= maxY - 1) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockPos headPos = pos.above();
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(headPos);
        if (!feetState.getCollisionShape(level, pos).isEmpty()
            || !headState.getCollisionShape(level, headPos).isEmpty()) {
            return false;
        }
        return level.noCollision(player, playerBoundsAt(player, pos));
    }

    private static boolean hasClearPathStep(ServerLevel level, Player player, BlockPos from, BlockPos to) {
        AABB sweptBounds = playerBoundsAt(player, from).expandTowards(
            to.getX() - from.getX(),
            to.getY() - from.getY(),
            to.getZ() - from.getZ()
        );
        return level.noCollision(player, sweptBounds);
    }

    private static AABB playerBoundsAt(Player player, BlockPos pos) {
        return player.getBoundingBox().move(
            pos.getX() + 0.5D - player.getX(),
            pos.getY() - player.getY(),
            pos.getZ() + 0.5D - player.getZ()
        );
    }

    private static boolean hasSafeBodyEnvironment(ServerLevel level, BlockPos pos) {
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(pos.above());
        return feetState.getFluidState().isEmpty()
            && headState.getFluidState().isEmpty()
            && !isHazardous(feetState)
            && !isHazardous(headState);
    }

    private static boolean hasSafeSupport(ServerLevel level, BlockPos pos) {
        BlockPos groundPos = pos.below();
        BlockState groundState = level.getBlockState(groundPos);
        return !groundState.getCollisionShape(level, groundPos).isEmpty()
            && groundState.getFluidState().isEmpty()
            && !isHazardous(groundState);
    }

    private static boolean isHazardous(BlockState state) {
        return state.is(Blocks.FIRE)
            || state.is(Blocks.SOUL_FIRE)
            || state.is(Blocks.LAVA)
            || state.is(Blocks.MAGMA_BLOCK)
            || state.is(Blocks.CACTUS)
            || state.is(Blocks.CAMPFIRE)
            || state.is(Blocks.SOUL_CAMPFIRE)
            || state.is(Blocks.SWEET_BERRY_BUSH)
            || state.is(Blocks.WITHER_ROSE)
            || state.is(Blocks.POWDER_SNOW)
            || state.is(Blocks.POINTED_DRIPSTONE)
            || state.is(Blocks.NETHER_PORTAL)
            || state.is(Blocks.END_PORTAL)
            || state.is(Blocks.END_GATEWAY);
    }
}
