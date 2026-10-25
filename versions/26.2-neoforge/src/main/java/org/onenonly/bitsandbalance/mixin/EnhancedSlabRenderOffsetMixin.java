package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
 
/**
 * NeoForge 26.1 no longer uses the old BlockRenderDispatcher.renderBatched
 * terrain path this mixin used to target. Enhanced slab terrain offsets are
 * now applied through NeoForgeEnhancedSlabModelOffset's BlockStateModel
 * wrapping, so this client-only mixin remains as a no-op placeholder for the
 * existing mixin registration.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class EnhancedSlabRenderOffsetMixin {
}