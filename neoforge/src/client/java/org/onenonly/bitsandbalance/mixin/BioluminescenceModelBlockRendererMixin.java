package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelBlockRenderer.class)
public abstract class BioluminescenceModelBlockRendererMixin {
    // NeoForge 26.1 no longer exposes the old ModelBlockRenderer cache surface
    // this hook used to target. The active dynamic-light path remains through
    // the LevelRenderer light hook until a NeoForge-specific block-model light
    // injection surface is mapped again.
}