package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tweaks: Cascading Ladders
 * When a ladder is broken, any unsupported ladders below will also break.
 */
public final class FabricCascadingLadders {
    private FabricCascadingLadders() {
    }

    private static final TagKey<net.minecraft.world.level.block.Block> LADDERS_TAG =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ladders"));

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return;
            if (player == null) return;

            if (!(state.getBlock() instanceof LadderBlock) && !state.is(LADDERS_TAG)) return;

            breakUnsupportedLaddersBelow(level, player, pos);
        });
    }

    private static void breakUnsupportedLaddersBelow(ServerLevel level, Player player, BlockPos startPos) {
        BlockPos currentPos = startPos.below();

        while (currentPos.getY() >= level.getMinY()) {
            BlockState currentState = level.getBlockState(currentPos);

            if (!(currentState.getBlock() instanceof LadderBlock) && !currentState.is(LADDERS_TAG)) {
                break;
            }

            if (isLadderSupported(level, currentPos)) {
                break;
            }

            level.destroyBlock(currentPos, true, player);
            currentPos = currentPos.below();
        }
    }

    private static boolean isLadderSupported(Level level, BlockPos ladderPos) {
        BlockState ladderState = level.getBlockState(ladderPos);
        if (!(ladderState.getBlock() instanceof LadderBlock) && !ladderState.is(LADDERS_TAG)) return false;

        Direction facing = ladderState.getValue(LadderBlock.FACING);
        BlockPos supportPos = ladderPos.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(supportPos);

        if (supportState.isFaceSturdy(level, supportPos, facing) && !supportState.isAir()) {
            return true;
        }

        BlockPos abovePos = ladderPos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        return aboveState.getBlock() instanceof LadderBlock || aboveState.is(LADDERS_TAG);
    }
}
