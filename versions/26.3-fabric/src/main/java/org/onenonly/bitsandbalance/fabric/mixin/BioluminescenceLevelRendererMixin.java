package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public abstract class BioluminescenceLevelRendererMixin implements BioluminescenceLevelRendererAccess {
    @Override
    public void bitsandbalance$scheduleChunkRebuild(int x, int y, int z, boolean important) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            level.setSectionDirtyWithNeighbors(x, y, z);
        }
    }
}
