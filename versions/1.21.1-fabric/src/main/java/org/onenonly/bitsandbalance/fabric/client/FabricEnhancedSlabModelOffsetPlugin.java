package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

import java.util.function.Supplier;

public final class FabricEnhancedSlabModelOffsetPlugin {
    private FabricEnhancedSlabModelOffsetPlugin() {
    }

    public static void init() {
        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE,
                        (model, context) -> {
                            if (model == null || context.topLevelId() == null) {
                                return model;
                            }

                            if (model.isCustomRenderer()) {
                                return model;
                            }

                            return new OffsetBakedModel(model);
                        }));
    }

    private static final class OffsetBakedModel extends ForwardingBakedModel {
        private OffsetBakedModel(BakedModel delegate) {
            this.wrapped = delegate;
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public void emitBlockQuads(BlockAndTintGetter world,
                                   BlockState state,
                                   BlockPos pos,
                                   Supplier<RandomSource> randomSupplier,
                                   RenderContext context) {
            if (!FabricTweaksConfig.enableEnhancedSlabs
                    || (!FabricTweaksConfig.enhancedSlabsPlaceOnTop
                    && !FabricTweaksConfig.enhancedSlabsHangBelow)) {
                super.emitBlockQuads(world, state, pos, randomSupplier, context);
                return;
            }

            double yOff = EnhancedSlabHelper.getVisualYOffset(world, pos, state);
            if (yOff == 0.0) {
                super.emitBlockQuads(world, state, pos, randomSupplier, context);
                return;
            }

            final float yOffF = (float) yOff;
            context.pushTransform(new RenderContext.QuadTransform() {
                @Override
                public boolean transform(MutableQuadView quad) {
                    for (int i = 0; i < 4; i++) {
                        quad.pos(i, quad.x(i), quad.y(i) + yOffF, quad.z(i));
                    }
                    quad.cullFace(null);
                    return true;
                }
            });

            try {
                super.emitBlockQuads(world, state, pos, randomSupplier, context);
            } finally {
                context.popTransform();
            }
        }
    }
}
