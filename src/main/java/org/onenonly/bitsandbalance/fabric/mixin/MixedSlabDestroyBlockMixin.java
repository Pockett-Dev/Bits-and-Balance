package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes MixedSlab half-breaking flicker by avoiding the intermediate AIR state.
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class MixedSlabDestroyBlockMixin {

    @Shadow @Final protected ServerPlayer player;

    @Shadow protected ServerLevel level;

    @Unique
    private long bitsandbalance$lockedMixedDestroyPosLong;

    @Unique
    private SlabType bitsandbalance$lockedMixedDestroyHalf;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void bitsandbalance$lockMixedDestroyHalf(BlockPos pos,
                                                     ServerboundPlayerActionPacket.Action action,
                                                     Direction direction,
                                                     int maxBuildHeight,
                                                     int sequence,
                                                     CallbackInfo ci) {
        if (!FabricTweaksConfig.enableEnhancedSlabs || !FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs) {
            return;
        }

        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            BlockState state = level.getBlockState(pos);
            if (state != null && state.getBlock() instanceof MixedSlabBlock) {
                bitsandbalance$lockedMixedDestroyPosLong = pos.asLong();
                bitsandbalance$lockedMixedDestroyHalf = MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
                return;
            }
        }

        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
                && bitsandbalance$lockedMixedDestroyPosLong == pos.asLong()) {
            bitsandbalance$lockedMixedDestroyHalf = null;
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
    private void bitsandbalance$destroyMixedSlabNoFlash(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs) return;

        BlockState mixedState = level.getBlockState(pos);
        if (!(mixedState.getBlock() instanceof MixedSlabBlock)) return;

        BlockState mined = MixedSlabBlock.bitsandbalance$peekCachedMinedSlab(pos);
        BlockState remaining = MixedSlabBlock.bitsandbalance$peekCachedRemainingSlab(pos);
        SlabType lockedHalf = bitsandbalance$lockedMixedDestroyPosLong == pos.asLong()
                ? bitsandbalance$lockedMixedDestroyHalf
                : null;

        if (lockedHalf != null) {
            if (mined == null || remaining == null) {
                BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
                mined = lockedHalf == SlabType.TOP ? bitsandbalance$slot(stored, 1) : bitsandbalance$slot(stored, 0);
                remaining = lockedHalf == SlabType.TOP ? bitsandbalance$slot(stored, 0) : bitsandbalance$slot(stored, 1);
            }
        } else if (mined == null || remaining == null) {
            {
                SlabType half = MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
                BlockState[] stored = EnhancedSlabMaterialSavedData.get(level, pos.asLong());
                mined = half == SlabType.TOP ? bitsandbalance$slot(stored, 1) : bitsandbalance$slot(stored, 0);
                remaining = half == SlabType.TOP ? bitsandbalance$slot(stored, 0) : bitsandbalance$slot(stored, 1);
                lockedHalf = half;
            }
        }

        if (mined == null || remaining == null) return;

        SlabType brokenHalf = lockedHalf != null
                ? lockedHalf
                : MixedSlabBlock.bitsandbalance$getTargetedHalf(player, pos);
        SlabType remainingHalf = brokenHalf == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP;
        boolean waterlogged = mixedState.hasProperty(MixedSlabBlock.WATERLOGGED)
                && mixedState.getValue(MixedSlabBlock.WATERLOGGED);
        remaining = bitsandbalance$singleSlabHalf(remaining, remainingHalf, waterlogged);

        level.setBlock(pos, remaining, Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong());

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();
            try {
                tool.mineBlock(level, mined, pos, player);
            } catch (Throwable ignored) {
            }

            boolean correctTool = !mined.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(mined);
            if (correctTool) {
                Block.dropResources(mined, level, pos, null, player, tool);
            }

            player.awardStat(Stats.BLOCK_MINED.get(mined.getBlock()));
            player.causeFoodExhaustion(0.005F);
        }

        MixedSlabBlock.bitsandbalance$clearCachedBreak();
        bitsandbalance$lockedMixedDestroyHalf = null;
        cir.setReturnValue(true);
    }

    private static BlockState bitsandbalance$slot(BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static BlockState bitsandbalance$singleSlabHalf(BlockState slab, SlabType half, boolean waterlogged) {
        BlockState result = slab;
        if (result.hasProperty(SlabBlock.TYPE)) {
            result = result.setValue(SlabBlock.TYPE, half);
        }
        if (result.hasProperty(SlabBlock.WATERLOGGED)) {
            result = result.setValue(SlabBlock.WATERLOGGED, waterlogged);
        }
        return result;
    }
}
