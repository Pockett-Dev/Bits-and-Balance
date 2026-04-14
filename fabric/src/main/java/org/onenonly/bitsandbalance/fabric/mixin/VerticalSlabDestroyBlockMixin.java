package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
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

    @Inject(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        ),
        cancellable = true
    )
    private void bitsandbalance$destroyVerticalSlabNoFlash(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.isVerticalSlabRuntimeEnabled()) return;

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
        cir.setReturnValue(true);
    }
}
