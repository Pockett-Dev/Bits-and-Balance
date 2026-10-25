package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCrumbleState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
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
    private BlockPos bitsandbalance$lockedMixedDestroyPos;

    @Unique
    private SlabType bitsandbalance$lockedMixedDestroyHalf;

    @Unique
    private static final ThreadLocal<BlockPos> bitsandbalance$PENDING_DESTROY_POS = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<BlockState> bitsandbalance$PENDING_DESTROY_STATE = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<SlabType> bitsandbalance$PENDING_DESTROY_HALF = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Boolean> bitsandbalance$PENDING_DESTROY_PARTICLES_SPAWNED = ThreadLocal.withInitial(() -> false);

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), require = 0)
    private void bitsandbalance$lockMixedDestroyHalf(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$lockedMixedDestroyPos = null;
        bitsandbalance$lockedMixedDestroyHalf = null;

        if (!FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs || minecraft.level == null) {
            return;
        }

        BlockState state = minecraft.level.getBlockState(pos);
        if (state != null && state.getBlock() instanceof MixedSlabBlock) {
            bitsandbalance$lockedMixedDestroyPos = pos.immutable();
            bitsandbalance$lockedMixedDestroyHalf = bitsandbalance$targetedHalfAt(pos);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureDestroyParticles(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        bitsandbalance$PENDING_DESTROY_PARTICLES_SPAWNED.set(false);
        if (!FabricTweaksConfig.enableEnhancedSlabs || minecraft.level == null) {
            bitsandbalance$PENDING_DESTROY_POS.remove();
            bitsandbalance$PENDING_DESTROY_STATE.remove();
            bitsandbalance$PENDING_DESTROY_HALF.remove();
            return;
        }

        BlockState state = minecraft.level.getBlockState(pos);
        if (state == null || !bitsandbalance$shouldSpawnEnhancedDebris(state)) {
            bitsandbalance$PENDING_DESTROY_POS.remove();
            bitsandbalance$PENDING_DESTROY_STATE.remove();
            bitsandbalance$PENDING_DESTROY_HALF.remove();
            return;
        }

        bitsandbalance$PENDING_DESTROY_POS.set(pos.immutable());
        bitsandbalance$PENDING_DESTROY_STATE.set(state);
        bitsandbalance$PENDING_DESTROY_HALF.set(bitsandbalance$lockedMixedDestroyHalf(pos));
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"), require = 0)
    private void bitsandbalance$spawnCapturedDestroyParticles(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockPos pendingPos = bitsandbalance$PENDING_DESTROY_POS.get();
        BlockState pendingState = bitsandbalance$PENDING_DESTROY_STATE.get();
        boolean alreadySpawned = bitsandbalance$PENDING_DESTROY_PARTICLES_SPAWNED.get();
        bitsandbalance$PENDING_DESTROY_POS.remove();
        bitsandbalance$PENDING_DESTROY_STATE.remove();
        bitsandbalance$PENDING_DESTROY_HALF.remove();
        bitsandbalance$PENDING_DESTROY_PARTICLES_SPAWNED.remove();

        if (alreadySpawned || !cir.getReturnValueZ() || pendingPos == null || pendingState == null || minecraft.level == null || minecraft.player == null) {
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
        if (!FabricTweaksConfig.enableEnhancedSlabs) {
            return level.setBlock(pos, newState, flags);
        }

        if (!(level instanceof ClientLevel clientLevel)) {
            return level.setBlock(pos, newState, flags);
        }

        BlockState current = clientLevel.getBlockState(pos);

        // Quad steps: avoid writing AIR during crouch-mining, otherwise the client deletes the BE
        // and sometimes fails to recreate it (leaving the container invisible but collidable).
        if (FabricTweaksConfig.enhancedSlabsSteps
                && minecraft.player != null) {
            try {
                if (current.getBlock() instanceof QuadStepBlock) {
                    BlockEntity be = clientLevel.getBlockEntity(pos);
                    QuadStepBlockEntity quad = be instanceof QuadStepBlockEntity q ? q : null;
                    BlockState[] slots = quad != null
                            ? quad.getAllSlabs()
                            : EnhancedSlabMaterialSavedData.getClient(clientLevel, pos.asLong());
                    if (slots != null
                            && (minecraft.player.isShiftKeyDown()
                            || (quad != null ? quad.bitsandbalance$requiresKneeslabBreaking() : bitsandbalance$requiresTargetedBreaking(slots)))) {
                        int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(
                                minecraft.player,
                                pos,
                                current.getValue(QuadStepBlock.AXIS)
                        );
                        idx = quad != null
                                ? QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx)
                                : bitsandbalance$resolveQuadStepOccupiedIndex(slots, idx);
                        BlockState selected = bitsandbalance$slot(slots, idx);
                        if (idx >= 0 && idx < 4 && selected != null) {
                            bitsandbalance$clearLocalVerticalStepBreakVisuals(clientLevel, pos);
                            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                            bitsandbalance$markDestroyParticlesSpawned();
                            boolean singlePieceOnly = minecraft.player.isShiftKeyDown();
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = bitsandbalance$slot(slots, i);
                                if (slab == null) continue;
                                if (singlePieceOnly) {
                                    if (quad != null && i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (quad != null && selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            BlockState[] predictedSlabs = quad != null ? quad.getAllSlabs() : slots.clone();
                            bitsandbalance$clearPredictedMaterial(predictedSlabs, idx, singlePieceOnly);
                            EnhancedSlabMaterialSavedData.rememberClient(clientLevel, pos.asLong(), predictedSlabs);
                            int remainingCount = bitsandbalance$countSlots(predictedSlabs);
                            if (remainingCount > 1) {
                                EnhancedSlabPlacementPredictionCache.rememberQuadStep(clientLevel, pos, current, predictedSlabs);
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (remainingCount == 1) {
                                QuadStepBlock.CachedBreak cached = new QuadStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = predictedSlabs;
                                cached.waterlogged = current.getValue(QuadStepBlock.WATERLOGGED);
                                cached.axis = current.getValue(QuadStepBlock.AXIS);
                                cached.targetedIndex = idx;
                                QuadStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                bitsandbalance$rememberSingleStepPrediction(clientLevel, pos);
                                return true;
                            }
                        }
                    }
                } else if (current.getBlock() instanceof QuadVerticalStepBlock) {
                    BlockEntity be = clientLevel.getBlockEntity(pos);
                    QuadVerticalStepBlockEntity quad = be instanceof QuadVerticalStepBlockEntity q ? q : null;
                    BlockState[] slots = quad != null
                            ? quad.getAllSlabs()
                            : EnhancedSlabMaterialSavedData.getClient(clientLevel, pos.asLong());
                    if (slots != null
                            && (minecraft.player.isShiftKeyDown()
                            || (quad != null ? quad.bitsandbalance$requiresKneeslabBreaking() : bitsandbalance$requiresTargetedBreaking(slots)))) {
                        int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(minecraft.player, pos);
                        idx = quad != null
                                ? QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, idx)
                                : bitsandbalance$resolveQuadVerticalStepOccupiedIndex(slots, idx);
                        BlockState selected = bitsandbalance$slot(slots, idx);
                        if (idx >= 0 && idx < 4 && selected != null) {
                            bitsandbalance$clearLocalVerticalStepBreakVisuals(clientLevel, pos);
                            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                            bitsandbalance$markDestroyParticlesSpawned();
                            boolean singlePieceOnly = minecraft.player.isShiftKeyDown();
                            for (int i = 0; i < 4; i++) {
                                BlockState slab = bitsandbalance$slot(slots, i);
                                if (slab == null) continue;
                                if (singlePieceOnly) {
                                    if (quad != null && i == idx) {
                                        quad.bitsandbalance$setSlabAtSilent(i, null);
                                    }
                                } else if (quad != null && selected.equals(slab)) {
                                    quad.bitsandbalance$setSlabAtSilent(i, null);
                                }
                            }
                            BlockState[] predictedSlabs = quad != null ? quad.getAllSlabs() : slots.clone();
                            bitsandbalance$clearPredictedMaterial(predictedSlabs, idx, singlePieceOnly);
                            EnhancedSlabMaterialSavedData.rememberClient(clientLevel, pos.asLong(), predictedSlabs);
                            int remainingCount = bitsandbalance$countSlots(predictedSlabs);
                            if (remainingCount > 1) {
                                EnhancedSlabPlacementPredictionCache.rememberQuadVerticalStep(clientLevel, pos, current, predictedSlabs);
                                clientLevel.sendBlockUpdated(pos, current, current, flags);
                                return true;
                            }
                            if (remainingCount == 1) {
                                QuadVerticalStepBlock.CachedBreak cached = new QuadVerticalStepBlock.CachedBreak();
                                cached.posLong = pos.asLong();
                                cached.remainingSlabs = predictedSlabs;
                                cached.waterlogged = current.getValue(QuadVerticalStepBlock.WATERLOGGED);
                                cached.targetedIndex = idx;
                                QuadVerticalStepBlock.bitsandbalance$restoreRemaining(clientLevel, pos, cached);
                                bitsandbalance$rememberSingleVerticalStepPrediction(clientLevel, pos);
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        SlabType targetedHalf = bitsandbalance$pendingTargetedHalf(pos);
        if (targetedHalf == null) {
            targetedHalf = bitsandbalance$lockedMixedDestroyHalf(pos);
        }

        if (targetedHalf == null) {
            return level.setBlock(pos, newState, flags);
        }

        if (FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs && current.getBlock() instanceof MixedSlabBlock) {
            BlockState[] stored = EnhancedSlabMaterialSavedData.getClient(clientLevel, pos.asLong());
            if (stored == null) {
                return level.setBlock(pos, newState, flags);
            }

            BlockState bottom = bitsandbalance$slot(stored, 0);
            BlockState top = bitsandbalance$slot(stored, 1);
            BlockState mined = targetedHalf == SlabType.TOP ? top : bottom;
            BlockState remaining = targetedHalf == SlabType.TOP ? bottom : top;
            if (remaining == null) {
                return level.setBlock(pos, newState, flags);
            }

            if (mined != null) {
                if (!EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current)) {
                    clientLevel.addDestroyBlockEffect(pos, bitsandbalance$singleSlabHalf(mined, targetedHalf, false));
                }
                bitsandbalance$markDestroyParticlesSpawned();
            }

            remaining = bitsandbalance$singleSlabHalf(
                    remaining,
                    targetedHalf == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP,
                    current.hasProperty(MixedSlabBlock.WATERLOGGED) && current.getValue(MixedSlabBlock.WATERLOGGED)
            );

            EnhancedSlabCrumbleState.clear(pos);
            EnhancedSlabMaterialSavedData.rememberClient(clientLevel, pos.asLong());
            boolean ok = level.setBlock(pos, remaining, flags);
            bitsandbalance$markClientMixedSlabDirty(pos, current, remaining);
            bitsandbalance$clearLockedMixedDestroyHalf(pos);
            return ok;
        }

        if (FabricTweaksConfig.enhancedSlabsVerticalSlabs && current.getBlock() instanceof VerticalSlabBlock) {
            if (!current.getValue(VerticalSlabBlock.DOUBLE)) {
                EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
                bitsandbalance$markDestroyParticlesSpawned();
                return level.setBlock(pos, newState, flags);
            }

            BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
            VerticalSlabBlockEntity vbe = blockEntity instanceof VerticalSlabBlockEntity verticalSlab ? verticalSlab : null;
            BlockState[] stored = vbe == null ? EnhancedSlabMaterialSavedData.getClient(clientLevel, pos.asLong()) : null;
            if (vbe == null && stored == null) {
                return level.setBlock(pos, newState, flags);
            }

            if (!VerticalSlabBlock.bitsandbalance$shouldTargetHalf(minecraft.player, current, vbe, stored)) {
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

            BlockState facingSlab = vbe != null ? vbe.getFacingSlab() : bitsandbalance$slot(stored, 0);
            BlockState oppositeSlab = vbe != null ? vbe.getOppositeSlab() : bitsandbalance$slot(stored, 1);
            BlockState remainingSlab = (targetedDir == facing) ? oppositeSlab : facingSlab;
            if (remainingSlab == null) {
                return level.setBlock(pos, newState, flags);
            }

            net.minecraft.core.Direction remainingFacing = (targetedDir == facing) ? facing.getOpposite() : facing;
            BlockState remainingVertical = bitsandbalance$remainingVerticalSlabState(current, remainingSlab, remainingFacing);

            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
            bitsandbalance$markDestroyParticlesSpawned();
            if (remainingVertical.getBlock() == current.getBlock()) {
                EnhancedSlabMaterialSavedData.rememberClient(clientLevel, pos.asLong(), remainingSlab);
            } else {
                EnhancedSlabMaterialSavedData.rememberClient(clientLevel, pos.asLong());
            }
            boolean ok = level.setBlock(pos, remainingVertical, flags);
            BlockEntity newBe = clientLevel.getBlockEntity(pos);
            if (newBe instanceof VerticalSlabBlockEntity newVbe) {
                newVbe.setSingleFacingSlab(remainingSlab);
            }
            return ok;
        }

        if (FabricTweaksConfig.enhancedSlabsSteps
                && (current.getBlock() instanceof StepBlock || current.getBlock() instanceof VerticalStepBlock)) {
            bitsandbalance$clearLocalVerticalStepBreakVisuals(clientLevel, pos);
            EnhancedSlabDestroyParticleHelper.spawnForState(clientLevel, minecraft.player, pos, current);
            bitsandbalance$markDestroyParticlesSpawned();
            return level.setBlock(pos, newState, flags);
        }

        if (FabricTweaksConfig.enhancedSlabsKneeSlabMining
                && current.getBlock() instanceof SlabBlock
                && EnhancedSlabHelper.isDoubleSlab(current)) {
            BlockState remaining = EnhancedSlabHelper.getRemainingHalfState(current, targetedHalf);
            EnhancedSlabCrumbleState.clear(pos);
            return level.setBlock(pos, remaining, flags);
        }

        return level.setBlock(pos, newState, flags);
    }

    @Unique
    private static boolean bitsandbalance$shouldSpawnEnhancedDebris(BlockState state) {
        return state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    @Unique
    private static void bitsandbalance$markDestroyParticlesSpawned() {
        bitsandbalance$PENDING_DESTROY_PARTICLES_SPAWNED.set(true);
    }

    @Unique
    private SlabType bitsandbalance$pendingTargetedHalf(BlockPos pos) {
        BlockPos pendingPos = bitsandbalance$PENDING_DESTROY_POS.get();
        return pendingPos != null && pendingPos.equals(pos) ? bitsandbalance$PENDING_DESTROY_HALF.get() : null;
    }

    @Unique
    private SlabType bitsandbalance$lockedMixedDestroyHalf(BlockPos pos) {
        if (bitsandbalance$lockedMixedDestroyPos != null && bitsandbalance$lockedMixedDestroyPos.equals(pos)) {
            return bitsandbalance$lockedMixedDestroyHalf;
        }
        return bitsandbalance$targetedHalfAt(pos);
    }

    @Unique
    private void bitsandbalance$clearLockedMixedDestroyHalf(BlockPos pos) {
        if (bitsandbalance$lockedMixedDestroyPos != null && bitsandbalance$lockedMixedDestroyPos.equals(pos)) {
            bitsandbalance$lockedMixedDestroyPos = null;
            bitsandbalance$lockedMixedDestroyHalf = null;
        }
    }

    @Unique
    private SlabType bitsandbalance$targetedHalfAt(BlockPos pos) {
        if (minecraft.hitResult instanceof BlockHitResult bhr && pos.equals(bhr.getBlockPos())) {
            return EnhancedSlabHelper.getTargetedHalf(pos, bhr.getLocation());
        }
        if (minecraft.player != null) {
            return EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(minecraft.player, pos);
        }
        return null;
    }

    @Unique
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

    @Unique
    private static BlockState bitsandbalance$remainingVerticalSlabState(BlockState current,
                                                                        BlockState remainingSlab,
                                                                        net.minecraft.core.Direction remainingFacing) {
        boolean waterlogged = current.getValue(VerticalSlabBlock.WATERLOGGED);
        if (remainingSlab != null && remainingSlab.equals(remainingSlab.getBlock().defaultBlockState())) {
            var verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(remainingSlab.getBlock());
            if (verticalBlock instanceof VerticalSlabBlock verticalSlab) {
                return verticalSlab.defaultBlockState()
                        .setValue(VerticalSlabBlock.WATERLOGGED, waterlogged)
                        .setValue(VerticalSlabBlock.FACING, remainingFacing)
                        .setValue(VerticalSlabBlock.DOUBLE, false);
            }
        }
        return ((VerticalSlabBlock) current.getBlock()).defaultBlockState()
                .setValue(VerticalSlabBlock.WATERLOGGED, waterlogged)
                .setValue(VerticalSlabBlock.FACING, remainingFacing)
                .setValue(VerticalSlabBlock.DOUBLE, false);
    }

    @Unique
    private void bitsandbalance$markClientMixedSlabDirty(BlockPos pos, BlockState oldState, BlockState newState) {
        if (minecraft.levelRenderer != null) {
            minecraft.levelRenderer.setBlockDirty(pos, oldState, newState);
        }
    }

    @Unique
    private void bitsandbalance$clearLocalVerticalStepBreakVisuals(ClientLevel clientLevel, BlockPos pos) {
        EnhancedSlabCrumbleState.clear(pos);
        EnhancedSlabPlacementPredictionCache.clearQuadStep(clientLevel, pos);
        EnhancedSlabPlacementPredictionCache.clearSingleStep(clientLevel, pos);
        EnhancedSlabPlacementPredictionCache.clearQuadVerticalStep(clientLevel, pos);
        EnhancedSlabPlacementPredictionCache.clearSingleVerticalStep(clientLevel, pos);
        if (minecraft.player != null) {
            clientLevel.destroyBlockProgress(minecraft.player.getId(), pos, -1);
        }
    }

    @Unique
    private void bitsandbalance$rememberSingleStepPrediction(ClientLevel clientLevel, BlockPos pos) {
        BlockState stepState = clientLevel.getBlockState(pos);
        if (!(stepState.getBlock() instanceof StepBlock)) {
            return;
        }

        BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
        BlockState slab = null;
        if (blockEntity instanceof org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity stepBlockEntity) {
            slab = stepBlockEntity.getSlabState();
        }
        if (slab == null) {
            slab = bitsandbalance$slot(EnhancedSlabMaterialSavedData.getClient(clientLevel, pos.asLong()), 0);
        }
        if (slab != null) {
            EnhancedSlabPlacementPredictionCache.rememberSingleStep(clientLevel, pos, stepState, slab);
            clientLevel.sendBlockUpdated(pos, stepState, stepState, 11);
        }
    }

    @Unique
    private void bitsandbalance$rememberSingleVerticalStepPrediction(ClientLevel clientLevel, BlockPos pos) {
        BlockState stepState = clientLevel.getBlockState(pos);
        if (!(stepState.getBlock() instanceof VerticalStepBlock)) {
            return;
        }

        BlockEntity blockEntity = clientLevel.getBlockEntity(pos);
        BlockState slab = null;
        if (blockEntity instanceof org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity stepBlockEntity) {
            slab = stepBlockEntity.getSlabState();
        }
        if (slab == null) {
            slab = bitsandbalance$slot(EnhancedSlabMaterialSavedData.getClient(clientLevel, pos.asLong()), 0);
        }
        if (slab != null) {
            EnhancedSlabPlacementPredictionCache.rememberSingleVerticalStep(clientLevel, pos, stepState, slab);
            clientLevel.sendBlockUpdated(pos, stepState, stepState, 11);
        }
    }

    @Unique
    private static int bitsandbalance$countSlots(BlockState[] slots) {
        int count = 0;
        if (slots != null) {
            for (BlockState slot : slots) {
                if (slot != null) count++;
            }
        }
        return count;
    }

    @Unique
    private static boolean bitsandbalance$requiresTargetedBreaking(BlockState[] slots) {
        BlockState first = null;
        if (slots == null) return false;
        for (BlockState slot : slots) {
            if (slot == null) continue;
            if (first == null) {
                first = slot;
            } else if (!first.equals(slot)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static BlockState bitsandbalance$slot(BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    @Unique
    private static void bitsandbalance$clearPredictedMaterial(BlockState[] slots, int targetedIndex, boolean singlePieceOnly) {
        BlockState selected = bitsandbalance$slot(slots, targetedIndex);
        if (selected == null) {
            return;
        }
        if (singlePieceOnly) {
            slots[targetedIndex] = null;
            return;
        }
        for (int i = 0; i < slots.length; i++) {
            if (selected.equals(slots[i])) {
                slots[i] = null;
            }
        }
    }

    @Unique
    private static int bitsandbalance$resolveQuadStepOccupiedIndex(BlockState[] slots, int preferredIndex) {
        int resolved = bitsandbalance$occupiedIndex(slots, preferredIndex);
        if (resolved >= 0) return resolved;
        resolved = bitsandbalance$occupiedIndex(slots, bitsandbalance$pairedStepVerticalIndex(preferredIndex));
        if (resolved >= 0) return resolved;
        resolved = bitsandbalance$occupiedIndex(slots, bitsandbalance$pairedStepHorizontalIndex(preferredIndex));
        if (resolved >= 0) return resolved;
        resolved = bitsandbalance$occupiedIndex(slots,
                bitsandbalance$pairedStepVerticalIndex(bitsandbalance$pairedStepHorizontalIndex(preferredIndex)));
        return resolved >= 0 ? resolved : bitsandbalance$firstOccupiedIndex(slots);
    }

    @Unique
    private static int bitsandbalance$resolveQuadVerticalStepOccupiedIndex(BlockState[] slots, int preferredIndex) {
        int resolved = bitsandbalance$occupiedIndex(slots, preferredIndex);
        if (resolved >= 0) return resolved;
        resolved = bitsandbalance$occupiedIndex(slots, bitsandbalance$pairedVerticalDepthIndex(preferredIndex));
        if (resolved >= 0) return resolved;
        resolved = bitsandbalance$occupiedIndex(slots, bitsandbalance$pairedVerticalWidthIndex(preferredIndex));
        if (resolved >= 0) return resolved;
        resolved = bitsandbalance$occupiedIndex(slots,
                bitsandbalance$pairedVerticalDepthIndex(bitsandbalance$pairedVerticalWidthIndex(preferredIndex)));
        return resolved >= 0 ? resolved : bitsandbalance$firstOccupiedIndex(slots);
    }

    @Unique
    private static int bitsandbalance$occupiedIndex(BlockState[] slots, int index) {
        if (bitsandbalance$slot(slots, index) != null) return index;
        return -1;
    }

    @Unique
    private static int bitsandbalance$firstOccupiedIndex(BlockState[] slots) {
        if (slots != null) {
            for (int i = 0; i < Math.min(4, slots.length); i++) {
                if (slots[i] != null) return i;
            }
        }
        return -1;
    }

    @Unique
    private static int bitsandbalance$pairedStepVerticalIndex(int index) {
        return switch (index) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 0;
            case 3 -> 1;
            default -> -1;
        };
    }

    @Unique
    private static int bitsandbalance$pairedStepHorizontalIndex(int index) {
        return switch (index) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 2;
            default -> -1;
        };
    }

    @Unique
    private static int bitsandbalance$pairedVerticalDepthIndex(int index) {
        return switch (index) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 0;
            case 3 -> 1;
            default -> -1;
        };
    }

    @Unique
    private static int bitsandbalance$pairedVerticalWidthIndex(int index) {
        return switch (index) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 2;
            default -> -1;
        };
    }

}
