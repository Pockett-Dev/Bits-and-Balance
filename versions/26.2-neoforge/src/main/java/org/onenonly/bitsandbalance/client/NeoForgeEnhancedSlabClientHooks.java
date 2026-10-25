package org.onenonly.bitsandbalance.client;

import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.onenonly.bitsandbalance.client.renderer.StepBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.VerticalSlabBlockEntityRenderer;
import org.onenonly.bitsandbalance.client.renderer.VerticalStepBlockEntityRenderer;
import org.onenonly.bitsandbalance.registry.ModBlockEntities;

public final class NeoForgeEnhancedSlabClientHooks {

    private NeoForgeEnhancedSlabClientHooks() {
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        NeoForgeEnhancedSlabModelOffset.wrapAll(event.getBakingResult().blockStateModels());
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.VERTICAL_SLAB.get(), VerticalSlabBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STEP.get(), StepBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VERTICAL_STEP.get(), VerticalStepBlockEntityRenderer::new);
    }
}
