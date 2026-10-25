package org.onenonly.bitsandbalance.mixin.sable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.neoforge.platform.SableSubLevelRenderPlatformImpl", remap = false)
public abstract class SableNeoForgeSingleBlockModelDataMixin {
    @ModifyExpressionValue(
            method = "tesselateBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getModelData(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/client/model/data/ModelData;"
            ),
            require = 0
    )
    private ModelData bitsandbalance$enrichTessellationModelData(ModelData base,
                                                                 @Coerce Object wrapper,
                                                                 BakedModel model,
                                                                 BlockState state,
                                                                 BlockPos pos) {
        return bitsandbalance$enrichModelData(base, wrapper, model, state, pos);
    }

    @ModifyExpressionValue(
            method = "getRenderLayers",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/sublevel/render/vanilla/SingleBlockSubLevelWrapper;getModelData(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/client/model/data/ModelData;"
            ),
            require = 0
    )
    private ModelData bitsandbalance$enrichRenderLayerModelData(ModelData base,
                                                                @Coerce Object wrapper,
                                                                BakedModel model,
                                                                BlockState state,
                                                                BlockPos pos) {
        return bitsandbalance$enrichModelData(base, wrapper, model, state, pos);
    }

    private static ModelData bitsandbalance$enrichModelData(ModelData base,
                                                            Object wrapper,
                                                            BakedModel model,
                                                            BlockState state,
                                                            BlockPos pos) {
        ModelData safeBase = base == null ? ModelData.EMPTY : base;
        if (!(wrapper instanceof BlockAndTintGetter level)
                || model == null
                || !EnhancedSlabCreateCompat.supports(state)) {
            return safeBase;
        }

        ModelData enriched = model.getModelData(level, pos, state, safeBase);
        return enriched == null ? safeBase : enriched;
    }
}
