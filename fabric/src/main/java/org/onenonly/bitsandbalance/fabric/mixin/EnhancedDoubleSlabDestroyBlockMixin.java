package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric: Avoid AIR flicker and support creative-mode half-breaking for vanilla DOUBLE slabs.
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
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!FabricTweaksConfig.enhancedSlabsKneeSlabMining) return;
        if (!player.isShiftKeyDown()) return; // Only trigger when crouching

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SlabBlock)) return;
        if (!EnhancedSlabHelper.isDoubleSlab(state)) return;

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

        cir.setReturnValue(true);
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
