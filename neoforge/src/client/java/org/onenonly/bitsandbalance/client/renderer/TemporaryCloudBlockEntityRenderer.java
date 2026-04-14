package org.onenonly.bitsandbalance.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.client.TemporaryCloudFadeRenderer;
import org.onenonly.bitsandbalance.mixin.BlockEntityRenderStateAccessor;

public final class TemporaryCloudBlockEntityRenderer implements BlockEntityRenderer<TemporaryCloudBlockEntity, TemporaryCloudBlockEntityRenderer.State> {
    public TemporaryCloudBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TemporaryCloudBlockEntity blockEntity, State state, float partialTick,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakOverlay);
        state.level = blockEntity.getLevel();
        state.alpha = blockEntity.getRenderAlpha(partialTick);
        state.sampledLight = state.lightCoords;

        BlockState blockState = ((BlockEntityRenderStateAccessor) (BlockEntityRenderState) state).bitsandbalance$getBlockState();
        if (blockState != null && blockState.hasProperty(CloudBlock.TEMPORARY) && blockState.getValue(CloudBlock.TEMPORARY)) {
            state.renderState = blockState.setValue(CloudBlock.TEMPORARY, Boolean.FALSE);
            if (state.level != null && state.blockPos != null) {
                state.sampledLight = bitsandbalance$samplePackedLight(state.level, state.renderState, state.blockPos, state.sampledLight);
            }
        } else {
            state.renderState = null;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        if (state.alpha <= 0 || state.blockPos == null || state.level == null || state.renderState == null) {
            return;
        }

        TemporaryCloudFadeRenderer.queue(poseStack.last(), state.blockPos, state.alpha, state.sampledLight);
    }

    private static int bitsandbalance$samplePackedLight(Object level, BlockState state, BlockPos pos, int fallbackPackedLight) {
        try {
            Class<?> levelRendererClass = Class.forName("net.minecraft.client.renderer.LevelRenderer");
            Class<?> brightnessGetterClass = null;
            for (Class<?> nested : levelRendererClass.getDeclaredClasses()) {
                if (nested.getSimpleName().equals("BrightnessGetter")) {
                    brightnessGetterClass = nested;
                    break;
                }
            }
            if (brightnessGetterClass == null) {
                return fallbackPackedLight;
            }

            Object brightnessGetter = brightnessGetterClass.getField("DEFAULT").get(null);
            java.lang.reflect.Method target = null;
            for (java.lang.reflect.Method candidate : levelRendererClass.getMethods()) {
                if (!candidate.getName().equals("getLightColor") || candidate.getParameterCount() != 4) {
                    continue;
                }
                Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (!parameterTypes[0].isAssignableFrom(brightnessGetterClass) && !brightnessGetterClass.isAssignableFrom(parameterTypes[0])) {
                    continue;
                }
                if (!BlockState.class.isAssignableFrom(parameterTypes[2])) {
                    continue;
                }
                if (!BlockPos.class.isAssignableFrom(parameterTypes[3])) {
                    continue;
                }
                target = candidate;
                break;
            }
            if (target == null) {
                return fallbackPackedLight;
            }

            Object result = target.invoke(null, brightnessGetter, level, state, pos);
            return result instanceof Integer light ? light : fallbackPackedLight;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return fallbackPackedLight;
        }
    }

    public static final class State extends BlockEntityRenderState {
        public Level level;
        public BlockState renderState;
        public int alpha;
        public int sampledLight;
    }
}