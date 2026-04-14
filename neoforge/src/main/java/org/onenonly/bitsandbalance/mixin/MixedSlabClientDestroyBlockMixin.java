package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side mixed slab break prediction.
 *
 * <p>Vanilla client prediction writes AIR in {@code MultiPlayerGameMode.destroyBlock}.
 * For mixed slabs, the server immediately replaces only the mined half, so AIR causes
 * a one-frame flicker. Redirect the local setBlock write to the remaining half.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MixedSlabClientDestroyBlockMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique
    private static final ThreadLocal<BlockPos> bitsandbalance$PENDING_DESTROY_POS = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<BlockState> bitsandbalance$PENDING_DESTROY_STATE = new ThreadLocal<>();

    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureDestroyParticles(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableEnhancedSlabs || minecraft.level == null) {
            bitsandbalance$PENDING_DESTROY_POS.remove();
            bitsandbalance$PENDING_DESTROY_STATE.remove();
            return;
        }

        BlockState state = minecraft.level.getBlockState(pos);
        if (state == null || !bitsandbalance$shouldSpawnEnhancedDebris(state)) {
            bitsandbalance$PENDING_DESTROY_POS.remove();
            bitsandbalance$PENDING_DESTROY_STATE.remove();
            return;
        }

        bitsandbalance$PENDING_DESTROY_POS.set(pos.immutable());
        bitsandbalance$PENDING_DESTROY_STATE.set(state);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"), require = 0)
    private void bitsandbalance$spawnCapturedDestroyParticles(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos pendingPos = bitsandbalance$PENDING_DESTROY_POS.get();
        BlockState pendingState = bitsandbalance$PENDING_DESTROY_STATE.get();
        bitsandbalance$PENDING_DESTROY_POS.remove();
        bitsandbalance$PENDING_DESTROY_STATE.remove();

        if (!cir.getReturnValueZ() || pendingPos == null || pendingState == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (!pendingPos.equals(pos)) {
            return;
        }

        EnhancedSlabDestroyParticleHelper.spawnForState(minecraft.level, minecraft.player, pendingPos, pendingState);
    }

    @Redirect(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            require = 0
    )
    private boolean bitsandbalance$avoidMixedSlabAirFlash(Level level, BlockPos pos, BlockState newState, int flags,
                                                           BlockPos destroyedPos) {
        if (!Config.enableEnhancedSlabs) {
            return level.setBlock(pos, newState, flags);
        }

        if (!(level instanceof ClientLevel clientLevel)) {
            return level.setBlock(pos, newState, flags);
        }

        BlockState current = clientLevel.getBlockState(pos);

        if (Config.enhancedSlabsSteps
                && minecraft.player != null) {
            try {
                if (current.getBlock() instanceof QuadStepBlock) {
                    BlockEntity be = clientLevel.getBlockEntity(pos);
                    if (be instanceof QuadStepBlockEntity quad
                            && (minecraft.player.isShiftKeyDown() || quad.bitsandbalance$requiresKneeslabBreaking())) {
                        int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(
                                minecraft.player,
                                pos,
                                current.getValue(QuadStepBlock.AXIS)
                        );
                        idx = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                        if (idx >= 0 && idx < 4 && quad.getSlabAt(idx) != null) {
                            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                            BlockState selected = quad.getSlabAt(idx);
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = quad.getSlabAt(i);
                                if (slab == null) continue;
                                if (minecraft.player.isShiftKeyDown()) {
                                    if (i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            if (quad.getCount() > 1) {
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (quad.getCount() == 1) {
                                QuadStepBlock.CachedBreak cached = new QuadStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = quad.getAllSlabs();
                                cached.waterlogged = current.getValue(QuadStepBlock.WATERLOGGED);
                                cached.axis = current.getValue(QuadStepBlock.AXIS);
                                cached.targetedIndex = idx;
                                QuadStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                return true;
                            }
                        }
                    }
                } else if (current.getBlock() instanceof QuadVerticalStepBlock) {
                    BlockEntity be = clientLevel.getBlockEntity(pos);
                    if (be instanceof QuadVerticalStepBlockEntity quad
                            && (minecraft.player.isShiftKeyDown() || quad.bitsandbalance$requiresKneeslabBreaking())) {
                        int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(minecraft.player, pos);
                        idx = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                        if (idx >= 0 && idx < 4 && quad.getSlabAt(idx) != null) {
                            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                            BlockState selected = quad.getSlabAt(idx);
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = quad.getSlabAt(i);
                                if (slab == null) continue;
                                if (minecraft.player.isShiftKeyDown()) {
                                    if (i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            if (quad.getCount() > 1) {
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (quad.getCount() == 1) {
                                QuadVerticalStepBlock.CachedBreak cached = new QuadVerticalStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = quad.getAllSlabs();
                                cached.waterlogged = current.getValue(QuadVerticalStepBlock.WATERLOGGED);
                                cached.targetedIndex = idx;
                                QuadVerticalStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        SlabType targetedHalf = null;
        if (minecraft.hitResult instanceof BlockHitResult bhr && pos.equals(bhr.getBlockPos())) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, bhr.getLocation());
        } else if (minecraft.player != null) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(minecraft.player, pos);
        }

        if (targetedHalf == null) {
            return level.setBlock(pos, newState, flags);
        }

        if (Config.enhancedSlabsMixedDoubleSlabs && current.getBlock() instanceof MixedSlabBlock) {
            BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof MixedSlabBlockEntity mixedBe)) {
                return level.setBlock(pos, newState, flags);
            }

            BlockState mined = targetedHalf == SlabType.TOP ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
            BlockState remaining = targetedHalf == SlabType.TOP ? mixedBe.getBottomSlab() : mixedBe.getTopSlab();
            if (remaining == null) {
                return level.setBlock(pos, newState, flags);
            }

            if (mined != null) {
                clientLevel.addDestroyBlockEffect(pos, mined);
            }

            if (current.hasProperty(MixedSlabBlock.WATERLOGGED)
                    && current.getValue(MixedSlabBlock.WATERLOGGED)
                    && remaining.hasProperty(SlabBlock.WATERLOGGED)) {
                remaining = remaining.setValue(SlabBlock.WATERLOGGED, true);
            }

            return level.setBlock(pos, remaining, flags);
        }

        if (Config.enhancedSlabsVerticalSlabs && current.getBlock() instanceof VerticalSlabBlock) {
            if (!current.getValue(VerticalSlabBlock.DOUBLE)) {
                EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                return level.setBlock(pos, newState, flags);
            }

            BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof VerticalSlabBlockEntity vbe)) {
                return level.setBlock(pos, newState, flags);
            }

            if (!VerticalSlabBlock.bitsandbalance$shouldTargetHalf(minecraft.player, current, vbe)) {
                return level.setBlock(pos, newState, flags);
            }

            var facing = current.getValue(VerticalSlabBlock.FACING);
            net.minecraft.core.Direction targetedDir;
            if (minecraft.hitResult instanceof BlockHitResult bhr && pos.equals(bhr.getBlockPos())) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(pos, bhr.getLocation(), facing);
            } else if (minecraft.player != null) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(minecraft.player, pos, facing);
            } else {
                return level.setBlock(pos, newState, flags);
            }

            BlockState remainingSlab = (targetedDir == facing) ? vbe.getOppositeSlab() : vbe.getFacingSlab();
            if (remainingSlab == null) {
                return level.setBlock(pos, newState, flags);
            }

            net.minecraft.core.Direction remainingFacing = (targetedDir == facing) ? facing.getOpposite() : facing;
            BlockState remainingVertical = ((VerticalSlabBlock) current.getBlock()).defaultBlockState()
                    .setValue(VerticalSlabBlock.WATERLOGGED, current.getValue(VerticalSlabBlock.WATERLOGGED))
                    .setValue(VerticalSlabBlock.FACING, remainingFacing)
                    .setValue(VerticalSlabBlock.DOUBLE, false);

            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
            boolean ok = level.setBlock(pos, remainingVertical, flags);
            BlockEntity newBe = clientLevel.getBlockEntity(pos);
            if (newBe instanceof VerticalSlabBlockEntity newVbe) {
                newVbe.setSingleFacingSlab(remainingSlab);
            }
            return ok;
        }

        if (Config.enhancedSlabsSteps
                && (current.getBlock() instanceof StepBlock || current.getBlock() instanceof VerticalStepBlock)) {
            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
            return level.setBlock(pos, newState, flags);
        }

        if (Config.enhancedSlabsKneeSlabMining
                && current.getBlock() instanceof SlabBlock
                && EnhancedSlabHelper.isDoubleSlab(current)) {
            BlockState remaining = EnhancedSlabHelper.getRemainingHalfState(current, targetedHalf);
            return level.setBlock(pos, remaining, flags);
        }

        return level.setBlock(pos, newState, flags);
    }

    /**
     * NeoForge replaces the vanilla {@code level.setBlock(pos, AIR, flags)} call in
     * {@code MultiPlayerGameMode.destroyBlock} with {@code state.onDestroyedByPlayer(...)}.
     * The {@code Level.setBlock} redirect above is dead code on NeoForge.
     * This redirect targets the NeoForge-specific call instead.
     */
    @Redirect(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                target = "Lnet/minecraft/world/level/block/state/BlockState;onDestroyedByPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/world/level/material/FluidState;)Z"
            ),
            require = 0
    )
    private boolean bitsandbalance$neoForgeAvoidEnhancedSlabAirFlash(
            BlockState state, Level level, BlockPos pos, Player player,
            ItemStack tool, boolean willHarvest, FluidState fluid) {
        if (!Config.enableEnhancedSlabs) {
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        if (!(level instanceof ClientLevel clientLevel)) {
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        BlockState current = clientLevel.getBlockState(pos);
        final int flags = 11; // client-side flags: UPDATE_NEIGHBORS | UPDATE_CLIENTS | UPDATE_IMMEDIATE

        if (Config.enhancedSlabsSteps
                && minecraft.player != null) {
            try {
                if (current.getBlock() instanceof QuadStepBlock) {
                    BlockEntity be = clientLevel.getBlockEntity(pos);
                    if (be instanceof QuadStepBlockEntity quad
                            && (minecraft.player.isShiftKeyDown() || quad.bitsandbalance$requiresKneeslabBreaking())) {
                        int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(
                                minecraft.player,
                                pos,
                                current.getValue(QuadStepBlock.AXIS)
                        );
                        idx = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                        if (idx >= 0 && idx < 4 && quad.getSlabAt(idx) != null) {
                            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                            bitsandbalance$PENDING_DESTROY_POS.remove();
                            bitsandbalance$PENDING_DESTROY_STATE.remove();
                            BlockState selected = quad.getSlabAt(idx);
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = quad.getSlabAt(i);
                                if (slab == null) continue;
                                if (minecraft.player.isShiftKeyDown()) {
                                    if (i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            if (quad.getCount() > 1) {
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (quad.getCount() == 1) {
                                QuadStepBlock.CachedBreak cached = new QuadStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = quad.getAllSlabs();
                                cached.waterlogged = current.getValue(QuadStepBlock.WATERLOGGED);
                                cached.axis = current.getValue(QuadStepBlock.AXIS);
                                cached.targetedIndex = idx;
                                QuadStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                return true;
                            }
                        }
                    }
                } else if (current.getBlock() instanceof QuadVerticalStepBlock) {
                    BlockEntity be = clientLevel.getBlockEntity(pos);
                    if (be instanceof QuadVerticalStepBlockEntity quad
                            && (minecraft.player.isShiftKeyDown() || quad.bitsandbalance$requiresKneeslabBreaking())) {
                        int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(minecraft.player, pos);
                        idx = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx);
                        if (idx >= 0 && idx < 4 && quad.getSlabAt(idx) != null) {
                            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                            bitsandbalance$PENDING_DESTROY_POS.remove();
                            bitsandbalance$PENDING_DESTROY_STATE.remove();
                            BlockState selected = quad.getSlabAt(idx);
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = quad.getSlabAt(i);
                                if (slab == null) continue;
                                if (minecraft.player.isShiftKeyDown()) {
                                    if (i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            if (quad.getCount() > 1) {
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (quad.getCount() == 1) {
                                QuadVerticalStepBlock.CachedBreak cached = new QuadVerticalStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = quad.getAllSlabs();
                                cached.waterlogged = current.getValue(QuadVerticalStepBlock.WATERLOGGED);
                                cached.targetedIndex = idx;
                                QuadVerticalStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        SlabType targetedHalf = null;
        if (minecraft.hitResult instanceof BlockHitResult bhr && pos.equals(bhr.getBlockPos())) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalf(pos, bhr.getLocation());
        } else if (minecraft.player != null) {
            targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(minecraft.player, pos);
        }

        if (targetedHalf == null) {
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        if (Config.enhancedSlabsMixedDoubleSlabs && current.getBlock() instanceof MixedSlabBlock) {
            BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof MixedSlabBlockEntity mixedBe)) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            BlockState mined = targetedHalf == SlabType.TOP ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
            BlockState remaining = targetedHalf == SlabType.TOP ? mixedBe.getBottomSlab() : mixedBe.getTopSlab();
            if (remaining == null) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            if (mined != null) {
                clientLevel.addDestroyBlockEffect(pos, mined);
            }

            if (current.hasProperty(MixedSlabBlock.WATERLOGGED)
                    && current.getValue(MixedSlabBlock.WATERLOGGED)
                    && remaining.hasProperty(SlabBlock.WATERLOGGED)) {
                remaining = remaining.setValue(SlabBlock.WATERLOGGED, true);
            }

            return level.setBlock(pos, remaining, flags);
        }

        if (Config.enhancedSlabsVerticalSlabs && current.getBlock() instanceof VerticalSlabBlock) {
            if (!current.getValue(VerticalSlabBlock.DOUBLE)) {
                EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof VerticalSlabBlockEntity vbe)) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            if (!VerticalSlabBlock.bitsandbalance$shouldTargetHalf(minecraft.player, current, vbe)) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            var facing = current.getValue(VerticalSlabBlock.FACING);
            net.minecraft.core.Direction targetedDir;
            if (minecraft.hitResult instanceof BlockHitResult bhr && pos.equals(bhr.getBlockPos())) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(pos, bhr.getLocation(), facing);
            } else if (minecraft.player != null) {
                targetedDir = VerticalSlabBlock.bitsandbalance$getTargetedHalf(minecraft.player, pos, facing);
            } else {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            BlockState remainingSlab = (targetedDir == facing) ? vbe.getOppositeSlab() : vbe.getFacingSlab();
            if (remainingSlab == null) {
                return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
            }

            net.minecraft.core.Direction remainingFacing = (targetedDir == facing) ? facing.getOpposite() : facing;
            BlockState remainingVertical = ((VerticalSlabBlock) current.getBlock()).defaultBlockState()
                    .setValue(VerticalSlabBlock.WATERLOGGED, current.getValue(VerticalSlabBlock.WATERLOGGED))
                    .setValue(VerticalSlabBlock.FACING, remainingFacing)
                    .setValue(VerticalSlabBlock.DOUBLE, false);

            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
            boolean ok = level.setBlock(pos, remainingVertical, flags);
            BlockEntity newBe = clientLevel.getBlockEntity(pos);
            if (newBe instanceof VerticalSlabBlockEntity newVbe) {
                newVbe.setSingleFacingSlab(remainingSlab);
            }
            return ok;
        }

        if (Config.enhancedSlabsSteps
                && (current.getBlock() instanceof StepBlock || current.getBlock() instanceof VerticalStepBlock)) {
            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
            return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
        }

        if (Config.enhancedSlabsKneeSlabMining
                && current.getBlock() instanceof SlabBlock
                && EnhancedSlabHelper.isDoubleSlab(current)) {
            BlockState remaining = EnhancedSlabHelper.getRemainingHalfState(current, targetedHalf);
            return level.setBlock(pos, remaining, flags);
        }

        return state.onDestroyedByPlayer(level, pos, player, tool, willHarvest, fluid);
    }

    @Unique
    private static boolean bitsandbalance$shouldSpawnEnhancedDebris(BlockState state) {
        return state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

}
