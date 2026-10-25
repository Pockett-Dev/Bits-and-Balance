package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext", remap = false)
public interface SodiumAbstractBlockRenderContextAccessorMixin {
    @Accessor("level")
    BlockAndTintGetter bitsandbalance$getLevel();
}
