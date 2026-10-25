package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererAccessor implements BioluminescenceLevelRendererAccess {
    @Override
    public void bitsandbalance$scheduleChunkRebuild(int x, int y, int z, boolean important) {
        Minecraft.getInstance().levelExtractor.setSectionDirty(x, y, z);
    }
}
