package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Enhanced Slab Behavior — KneeSlab-like Mining (vanilla double slabs)
 *
 * Runs in {@code ServerPlayerGameMode.destroyBlock} before vanilla removes the block,
 * so we can replace a DOUBLE slab with the remaining half without an intermediate AIR
 * state (no flicker). Also works in creative (no drops).
 */
@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class EnhancedDoubleSlabDestroyBlockMixin {

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
    private void bitsandbalance$destroyVanillaDoubleSlabHalf(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        if (!bitsandbalance$tryDestroyVanillaDoubleSlabHalf(pos, state)) return;
        cir.setReturnValue(true);
    }

    @Redirect(
        method = "destroyBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;onDestroyedByPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/world/level/material/FluidState;)Z"
        ),
        require = 0
    )
    private boolean bitsandbalance$neoForgeDestroyVanillaDoubleSlabHalf(
            BlockState state,
            net.minecraft.world.level.Level ignoredLevel,
            BlockPos pos,
            net.minecraft.world.entity.player.Player ignoredPlayer,
            ItemStack ignoredTool,
            boolean willHarvest,
            FluidState fluid) {
        if (bitsandbalance$tryDestroyVanillaDoubleSlabHalf(pos, level.getBlockState(pos))) {
            return true;
        }
        return state.onDestroyedByPlayer(level, pos, player, player.getMainHandItem(), willHarvest, fluid);
    }

    private boolean bitsandbalance$tryDestroyVanillaDoubleSlabHalf(BlockPos pos, BlockState state) {
        if (!Config.enableEnhancedSlabs) return false;
        if (!Config.enhancedSlabsKneeSlabMining) return false;
        if (!player.isCreative() && !player.isShiftKeyDown()) return false;
        if (!(state.getBlock() instanceof SlabBlock)) return false;
        if (!EnhancedSlabHelper.isDoubleSlab(state)) return false;

        SlabType targetedHalf = getTargetedHalfFromRayToFullCube(player, pos);
        BlockState minedHalfState = EnhancedSlabHelper.getHalfSlabState(state, targetedHalf);
        BlockState remainingState = EnhancedSlabHelper.getRemainingHalfState(state, targetedHalf);

        level.setBlock(pos, remainingState, Block.UPDATE_ALL);

        if (!player.isCreative()) {
            ItemStack tool = player.getMainHandItem();

            try {
                tool.mineBlock(level, minedHalfState, pos, player);
            } catch (Throwable ignored) {
            }

            boolean correctTool = !minedHalfState.requiresCorrectToolForDrops() || player.hasCorrectToolForDrops(minedHalfState);
            if (correctTool) {
                Block.dropResources(minedHalfState, level, pos, null, player, tool);
            }

            player.awardStat(Stats.BLOCK_MINED.get(minedHalfState.getBlock()));
            player.causeFoodExhaustion(0.005F);
        }

        return true;
    }

    private static SlabType getTargetedHalfFromRayToFullCube(ServerPlayer player, BlockPos pos) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));

        BlockHitResult clipped = Shapes.block().clip(start, end, pos);
        if (clipped != null) {
            return EnhancedSlabHelper.getTargetedHalf(pos, clipped.getLocation());
        }

        double relativeY = player.getEyeY() - pos.getY();
        return relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM;
    }
}
