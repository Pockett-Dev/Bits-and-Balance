package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor extends BioluminescenceLevelRendererAccess {
    @Override
    @Invoker("setSectionDirty")
    void bitsandbalance$scheduleChunkRebuild(int x, int y, int z, boolean important);
}