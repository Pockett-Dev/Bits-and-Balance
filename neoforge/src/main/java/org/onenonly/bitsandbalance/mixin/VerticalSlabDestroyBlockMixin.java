package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes vertical slab half-breaking flicker by avoiding the intermediate AIR state.
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class VerticalSlabDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @org.spongepowered.asm.mixin.Unique private boolean bitsandbalance$needsPostDestroyVerticalRefresh;
    @org.spongepowered.asm.mixin.Unique private long bitsandbalance$postDestroyVerticalRefreshPosLong;

    @org.spongepowered.asm.mixin.Unique
    private static void bitsandbalance$forceBreakerVerticalRefresh(ServerLevel level, ServerPlayer breaker, long posLong) {
        if (level == null || breaker == null || breaker.connection == null) return;
        try {
            BlockPos pos = BlockPos.of(posLong);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VerticalSlabBlockEntity) {
                Packet<ClientGamePacketListener> pkt = be.getUpdatePacket();
                if (pkt != null) {
                    try { breaker.connection.send(pkt); } catch (Throwable ignored) { }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void bitsandbalance$resetPostDestroyVerticalRefresh(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$needsPostDestroyVerticalRefresh = false;
        bitsandbalance$postDestroyVerticalRefreshPosLong = 0L;
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void bitsandbalance$postDestroyVerticalRefresh(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!bitsandbalance$needsPostDestroyVerticalRefresh || pos == null || !cir.getReturnValue()) return;
        if (pos.asLong() != bitsandbalance$postDestroyVerticalRefreshPosLong) return;

        ServerLevel capturedLevel = this.level;
        ServerPlayer breaker = this.player;
        BlockPos[] refreshPositions = new BlockPos[] {
                pos,
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west()
        };

        for (BlockPos refreshPos : refreshPositions) {
            bitsandbalance$forceBreakerVerticalRefresh(capturedLevel, breaker, refreshPos.asLong());
        }

        try {
            var server = capturedLevel.getServer();
            if (server == null) return;
            server.execute(new net.minecraft.server.TickTask(1, () -> {
                for (BlockPos refreshPos : refreshPositions) {
                    try {
                        bitsandbalance$forceBreakerVerticalRefresh(capturedLevel, breaker, refreshPos.asLong());
                    } catch (Throwable ignored) {
                    }
                }
            }));
        } catch (Throwable ignored) {
        }
    }

    @Inject(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        ),
        cancellable = true
    )
    private void bitsandbalance$destroyVerticalSlabNoFlash(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableEnhancedSlabs) return;
        if (!Config.enhancedSlabsVerticalSlabs) return;

        BlockState verticalState = level.getBlockState(pos);
        if (!(verticalState.getBlock() instanceof VerticalSlabBlock)) return;
        if (!verticalState.getValue(VerticalSlabBlock.DOUBLE)) return;

        BlockState mined = VerticalSlabBlock.bitsandbalance$peekCachedMinedSlab(pos);
        BlockState remainingSlab = VerticalSlabBlock.bitsandbalance$peekCachedRemainingSlab(pos);
        BlockState remainingState = VerticalSlabBlock.bitsandbalance$peekCachedRemainingState(pos);

        if (mined == null || remainingSlab == null || remainingState == null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VerticalSlabBlockEntity vbe
                    && VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, verticalState, vbe)) {
                var facing = verticalState.getValue(VerticalSlabBlock.FACING);
                var targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
                mined = (targeted == facing) ? vbe.getFacingSlab() : vbe.getOppositeSlab();
                remainingSlab = (targeted == facing) ? vbe.getOppositeSlab() : vbe.getFacingSlab();
                var remainingFacing = (targeted == facing) ? facing.getOpposite() : facing;
                remainingState = ((VerticalSlabBlock) verticalState.getBlock()).defaultBlockState()
                        .setValue(VerticalSlabBlock.WATERLOGGED, verticalState.getValue(VerticalSlabBlock.WATERLOGGED))
                        .setValue(VerticalSlabBlock.FACING, remainingFacing)
                        .setValue(VerticalSlabBlock.DOUBLE, false);
            }
        }

        if (mined == null || remainingSlab == null || remainingState == null) return;

        level.setBlock(pos, remainingState, Block.UPDATE_ALL);

        BlockEntity be2 = level.getBlockEntity(pos);
        if (be2 instanceof VerticalSlabBlockEntity vbe2) {
            vbe2.setSingleFacingSlab(remainingSlab);
        }

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
            try {
                tool.mineBlock(level, mined, pos, player);
            } catch (Throwable ignored) {
            }

            boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
            if (correctTool) {
                ItemStack drop = VerticalSlabBlock.bitsandbalance$makeHalfDropItem(mined);
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }

            player.awardStat(Stats.BLOCK_MINED.get(mined.getBlock()));
            player.causeFoodExhaustion(0.005F);
        }

        VerticalSlabBlock.bitsandbalance$clearCachedBreak();
        bitsandbalance$needsPostDestroyVerticalRefresh = true;
        bitsandbalance$postDestroyVerticalRefreshPosLong = pos.asLong();
        cir.setReturnValue(true);
    }
}
