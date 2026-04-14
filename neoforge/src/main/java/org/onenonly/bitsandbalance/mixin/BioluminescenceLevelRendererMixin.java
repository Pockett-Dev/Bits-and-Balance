package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public abstract class BioluminescenceLevelRendererMixin implements BioluminescenceLevelRendererAccess {
    @Invoker("setSectionDirty")
    protected abstract void bitsandbalance$invokeSetSectionDirty(int x, int y, int z, boolean important);

    @Override
    public void bitsandbalance$scheduleChunkRebuild(int x, int y, int z, boolean important) {
        bitsandbalance$invokeSetSectionDirty(x, y, z, important);
    }
}