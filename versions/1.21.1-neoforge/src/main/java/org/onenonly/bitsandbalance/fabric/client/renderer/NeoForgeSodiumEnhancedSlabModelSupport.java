package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;

public final class NeoForgeSodiumEnhancedSlabModelSupport {
    private NeoForgeSodiumEnhancedSlabModelSupport() {
    }

    public static BakedModel wrapStorage(BakedModel originalModel, NeoForgeEnhancedSlabStorageBakedModel.Kind kind) {
        return hasSodium()
                ? NeoForgeSodiumEnhancedSlabStorageBakedModel.wrap(originalModel, kind)
                : NeoForgeEnhancedSlabStorageBakedModel.wrap(originalModel, kind);
    }

    public static BakedModel wrapStatic(BakedModel originalModel,
                                        BakedModel sourceModel,
                                        BlockState sourceSlabState,
                                        EnhancedSlabItemSupport.ModelType modelType) {
        return hasSodium()
                ? NeoForgeSodiumEnhancedSlabStaticBakedModel.wrap(originalModel, sourceModel, sourceSlabState, modelType)
                : NeoForgeEnhancedSlabStaticBakedModel.wrap(originalModel, sourceModel, sourceSlabState, modelType);
    }

    private static boolean hasSodium() {
        try {
            return ModList.get().isLoaded("sodium");
        } catch (Throwable ignored) {
            return false;
        }
    }
}
