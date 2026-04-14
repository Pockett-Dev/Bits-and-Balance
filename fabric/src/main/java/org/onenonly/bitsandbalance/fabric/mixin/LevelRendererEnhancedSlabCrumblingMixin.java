package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabCrackProxyBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepCrackProxyBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCrumbleState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererEnhancedSlabCrumblingMixin {

    @Shadow @Final private Minecraft minecraft;
    private static volatile Method bitsandbalance$getBlockStateModelSetMethod;
    private static volatile Method bitsandbalance$getBlockStateModelMethod;
    private static volatile Method bitsandbalance$submitBreakingBlockModelMethod;

    @Inject(method = "submitBlockDestroyAnimation", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$submitEnhancedSlabCrumbling(PoseStack poseStack,
                                                            SubmitNodeCollector submitNodeCollector,
                                                            LevelRenderState levelRenderState,
                                                            CallbackInfo ci) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) {
            return;
        }

        Player player = this.minecraft.player;
        if (player == null) {
            return;
        }

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();

        for (BlockBreakingRenderState breakingState : levelRenderState.blockBreakingRenderStates) {
            BlockState state = breakingState.blockState();
            if (state == null) {
                continue;
            }

            BlockPos pos = breakingState.blockPos();
            List<BlockState> crumbleStates = bitsandbalance$resolveCrumbleStates(pos, state, player);
            if (crumbleStates.isEmpty()) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
            try {
                for (BlockState crumbleState : crumbleStates) {
                    if (crumbleState != null && crumbleState.getRenderShape() == RenderShape.MODEL) {
                        double yOffset = bitsandbalance$crumbleYOffset(this.minecraft.level, pos, player, state);
                        if (yOffset != 0.0D) {
                            poseStack.pushPose();
                            poseStack.translate(0.0D, yOffset, 0.0D);
                        }
                        try {
                            BlockStateModel model = bitsandbalance$getBreakingModel(crumbleState);
                            if (model != null) {
                                bitsandbalance$submitBreakingBlockModel(submitNodeCollector, poseStack, model, crumbleState.getSeed(pos), breakingState.progress());
                            }
                        } finally {
                            if (yOffset != 0.0D) {
                                poseStack.popPose();
                            }
                        }
                    }
                }
            } finally {
                poseStack.popPose();
            }
        }

        ci.cancel();
    }

    private static void bitsandbalance$submitBreakingBlockModel(SubmitNodeCollector submitNodeCollector,
                                                                PoseStack poseStack,
                                                                BlockStateModel model,
                                                                long seed,
                                                                int progress) {
        try {
            Method submitMethod = bitsandbalance$submitBreakingBlockModelMethod;
            if (submitMethod == null) {
                submitMethod = submitNodeCollector.getClass().getMethod(
                        "submitBreakingBlockModel",
                        PoseStack.class,
                        BlockStateModel.class,
                        long.class,
                        int.class
                );
                bitsandbalance$submitBreakingBlockModelMethod = submitMethod;
            }

            submitMethod.invoke(submitNodeCollector, poseStack, model, seed, progress);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to submit enhanced slab breaking model", exception);
        }
    }

    private BlockStateModel bitsandbalance$getBreakingModel(BlockState state) {
        try {
            Method getModelSetMethod = bitsandbalance$getBlockStateModelSetMethod;
            if (getModelSetMethod == null) {
                getModelSetMethod = this.minecraft.getModelManager().getClass().getMethod("getBlockStateModelSet");
                bitsandbalance$getBlockStateModelSetMethod = getModelSetMethod;
            }

            Object modelSet = getModelSetMethod.invoke(this.minecraft.getModelManager());
            if (modelSet == null) {
                return null;
            }

            Method getModelMethod = bitsandbalance$getBlockStateModelMethod;
            if (getModelMethod == null) {
                getModelMethod = modelSet.getClass().getMethod("get", BlockState.class);
                bitsandbalance$getBlockStateModelMethod = getModelMethod;
            }

            Object model = getModelMethod.invoke(modelSet, state);
            return model instanceof BlockStateModel blockStateModel ? blockStateModel : null;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to resolve block destroy model for enhanced slab crumbling", exception);
        }
    }

    private static double bitsandbalance$crumbleYOffset(Object renderLevel,
                                                        BlockPos pos,
                                                        Player player,
                                                        BlockState state) {
        if (!(renderLevel instanceof BlockGetter blockGetter) || state == null) {
            return 0.0D;
        }

        try {
            BlockHitResult hitResult = Minecraft.getInstance().hitResult instanceof BlockHitResult bhr && pos.equals(bhr.getBlockPos())
                    ? bhr
                    : null;
            BlockState anchorState = EnhancedSlabHelper.resolveVisualOffsetAnchorState(blockGetter, player, hitResult, pos, state);
            return EnhancedSlabHelper.getVisualYOffset(blockGetter, pos, anchorState);
        } catch (Throwable ignored) {
            return 0.0D;
        }
    }

    private List<BlockState> bitsandbalance$resolveCrumbleStates(BlockPos pos, BlockState state, Player player) {
        BlockState locked = EnhancedSlabCrumbleState.get(pos);
        if (locked != null) {
            return List.of(locked);
        }

        if (FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs && state.getBlock() instanceof MixedSlabBlock) {
            if (this.minecraft.level != null) {
                BlockEntity be = this.minecraft.level.getBlockEntity(pos);
                if (be instanceof MixedSlabBlockEntity mixedBe) {
                    SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
                    BlockState halfState = targetedHalf == SlabType.TOP ? mixedBe.getTopSlab() : mixedBe.getBottomSlab();
                    if (halfState != null) {
                        return List.of(halfState);
                    }
                }
            }
            return List.of(state);
        }

        if (FabricTweaksConfig.enhancedSlabsVerticalSlabs && state.getBlock() instanceof VerticalSlabBlock) {
            if (state.getValue(VerticalSlabBlock.DOUBLE) && !player.isShiftKeyDown() && this.minecraft.level != null) {
                BlockEntity be = this.minecraft.level.getBlockEntity(pos);
                if (be instanceof org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity verticalSlab
                        && !VerticalSlabBlock.bitsandbalance$shouldTargetHalf(player, state, verticalSlab)) {
                    List<BlockState> fullDoubleStates = bitsandbalance$fullVerticalSlabCrumbleStates(state);
                    if (!fullDoubleStates.isEmpty()) {
                        return fullDoubleStates;
                    }
                    return List.of();
                }
            }

            BlockState sourceState = bitsandbalance$verticalSlabCrumbleState(state, pos, player);
            return sourceState != null ? List.of(sourceState) : List.of(state);
        }

        if (FabricTweaksConfig.enhancedSlabsSteps && (state.getBlock() instanceof StepBlock || state.getBlock() instanceof QuadStepBlock)) {
            if (state.getBlock() instanceof QuadStepBlock && !player.isShiftKeyDown()) {
                return bitsandbalance$selectedQuadStepCrumbleStates(pos, state, player);
            }

            BlockState sourceState = bitsandbalance$stepCrumbleState(state, pos, player);
            return sourceState != null ? List.of(sourceState) : List.of(state);
        }

        if (FabricTweaksConfig.enhancedSlabsSteps && (state.getBlock() instanceof VerticalStepBlock || state.getBlock() instanceof QuadVerticalStepBlock)) {
            if (state.getBlock() instanceof QuadVerticalStepBlock && !player.isShiftKeyDown()) {
                return bitsandbalance$selectedQuadVerticalStepCrumbleStates(pos, state, player);
            }

            BlockState sourceState = bitsandbalance$verticalStepCrumbleState(state, pos, player);
            return sourceState != null ? List.of(sourceState) : List.of(state);
        }

        if (FabricTweaksConfig.enhancedSlabsKneeSlabMining
                && player.isShiftKeyDown()
                && state.getBlock() instanceof SlabBlock
                && EnhancedSlabHelper.isDoubleSlab(state)) {
            SlabType targetedHalf = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
            return List.of(EnhancedSlabHelper.getHalfSlabState(state, targetedHalf));
        }

        return List.of(state);
    }

    private static List<BlockState> bitsandbalance$fullVerticalSlabCrumbleStates(BlockState state) {
        VerticalSlabCrackProxyBlock proxy = CommonBlocks.VERTICAL_SLAB_CRACK_PROXY;
        if (proxy == null) {
            return List.of();
        }

        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        return List.of(
                proxy.defaultBlockState().setValue(VerticalSlabCrackProxyBlock.FACING, facing),
                proxy.defaultBlockState().setValue(VerticalSlabCrackProxyBlock.FACING, facing.getOpposite())
        );
    }

    private List<BlockState> bitsandbalance$selectedQuadStepCrumbleStates(BlockPos pos,
                                                                           BlockState state,
                                                                           Player player) {
        if (this.minecraft.level == null) {
            return List.of();
        }

        BlockEntity be = this.minecraft.level.getBlockEntity(pos);
        if (!(be instanceof QuadStepBlockEntity quad)) {
            return List.of();
        }

        StepCrackProxyBlock proxy = CommonBlocks.STEP_CRACK_PROXY;
        if (proxy == null) {
            return List.of();
        }

        Direction.Axis axis = state.getValue(QuadStepBlock.AXIS);
        int targetedIndex = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis));
        if (targetedIndex < 0) {
            return List.of();
        }

        BlockState selected = quad.getSlabAt(targetedIndex);
        if (selected == null) {
            return List.of();
        }

        ArrayList<BlockState> result = new ArrayList<>();
        for (int idx = 0; idx < 4; idx++) {
            BlockState slab = quad.getSlabAt(idx);
            if (slab == null || !selected.equals(slab)) {
                continue;
            }
            result.add(proxy.defaultBlockState()
                    .setValue(StepCrackProxyBlock.FACING, QuadStepBlock.sideForIndex(axis, idx))
                    .setValue(StepCrackProxyBlock.TOP, QuadStepBlock.isTop(idx)));
        }
        return result;
    }

    private List<BlockState> bitsandbalance$selectedQuadVerticalStepCrumbleStates(BlockPos pos,
                                                                                   BlockState state,
                                                                                   Player player) {
        if (this.minecraft.level == null) {
            return List.of();
        }

        BlockEntity be = this.minecraft.level.getBlockEntity(pos);
        if (!(be instanceof QuadVerticalStepBlockEntity quad)) {
            return List.of();
        }

        VerticalStepCrackProxyBlock proxy = CommonBlocks.VERTICAL_STEP_CRACK_PROXY;
        if (proxy == null) {
            return List.of();
        }

        int targetedIndex = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos));
        if (targetedIndex < 0) {
            return List.of();
        }

        BlockState selected = quad.getSlabAt(targetedIndex);
        if (selected == null) {
            return List.of();
        }

        ArrayList<BlockState> result = new ArrayList<>();
        for (int idx = 0; idx < 4; idx++) {
            BlockState slab = quad.getSlabAt(idx);
            if (slab == null || !selected.equals(slab)) {
                continue;
            }
            result.add(proxy.defaultBlockState()
                    .setValue(VerticalStepCrackProxyBlock.FACING, QuadVerticalStepBlock.facingForIndex(idx))
                    .setValue(VerticalStepCrackProxyBlock.SIDE, QuadVerticalStepBlock.sideForIndex(idx)));
        }
        return result;
    }

    @org.jetbrains.annotations.Nullable
    private static BlockState bitsandbalance$verticalSlabCrumbleState(BlockState vertState, BlockPos pos, Player player) {
        try {
            VerticalSlabCrackProxyBlock proxy = CommonBlocks.VERTICAL_SLAB_CRACK_PROXY;
            if (proxy == null) return null;
            Direction facing = vertState.getValue(VerticalSlabBlock.FACING);
            if (vertState.getValue(VerticalSlabBlock.DOUBLE) && player != null) {
                facing = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
            }
            return proxy.defaultBlockState().setValue(VerticalSlabCrackProxyBlock.FACING, facing);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @org.jetbrains.annotations.Nullable
    private static BlockState bitsandbalance$stepCrumbleState(BlockState stepState, BlockPos pos, Player player) {
        try {
            StepCrackProxyBlock proxy = CommonBlocks.STEP_CRACK_PROXY;
            if (proxy == null) return null;

            Direction facing;
            boolean top;
            if (stepState.getBlock() instanceof QuadStepBlock) {
                if (player == null) return null;
                Direction.Axis axis = stepState.getValue(QuadStepBlock.AXIS);
                int idx = QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis);
                facing = QuadStepBlock.sideForIndex(axis, idx);
                top = QuadStepBlock.isTop(idx);
            } else {
                facing = stepState.getValue(StepBlock.FACING);
                top = stepState.getValue(StepBlock.TOP);
            }

            return proxy.defaultBlockState()
                    .setValue(StepCrackProxyBlock.FACING, facing)
                    .setValue(StepCrackProxyBlock.TOP, top);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @org.jetbrains.annotations.Nullable
    private static BlockState bitsandbalance$verticalStepCrumbleState(BlockState stepState, BlockPos pos, Player player) {
        try {
            VerticalStepCrackProxyBlock proxy = CommonBlocks.VERTICAL_STEP_CRACK_PROXY;
            if (proxy == null) return null;

            Direction facing;
            Direction side;
            if (stepState.getBlock() instanceof QuadVerticalStepBlock) {
                if (player == null) return null;
                int idx = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos);
                facing = QuadVerticalStepBlock.facingForIndex(idx);
                side = QuadVerticalStepBlock.sideForIndex(idx);
            } else {
                facing = stepState.getValue(VerticalStepBlock.FACING);
                side = stepState.getValue(VerticalStepBlock.SIDE);
            }

            return proxy.defaultBlockState()
                    .setValue(VerticalStepCrackProxyBlock.FACING, facing)
                    .setValue(VerticalStepCrackProxyBlock.SIDE, side);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
