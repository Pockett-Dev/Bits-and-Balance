package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.core.BlockPos;
import org.onenonly.bitsandbalance.common.access.BitsAndBalanceBlockDisplayFadeRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockDisplayEntityRenderState.class)
public final class BlockDisplayEntityRenderStateBottleCloudFadeMixin implements BitsAndBalanceBlockDisplayFadeRenderStateAccess {
    @Unique
    private boolean bitsandbalance$bottleCloudFade;

    @Unique
    private float bitsandbalance$bottleCloudFadeAlpha = 1.0F;

    @Unique
    private BlockPos bitsandbalance$bottleCloudBlockPos = BlockPos.ZERO;

    @Override
    public void bitsandbalance$setBottleCloudFade(boolean value) {
        this.bitsandbalance$bottleCloudFade = value;
    }

    @Override
    public boolean bitsandbalance$isBottleCloudFade() {
        return this.bitsandbalance$bottleCloudFade;
    }

    @Override
    public void bitsandbalance$setBottleCloudFadeAlpha(float alpha) {
        this.bitsandbalance$bottleCloudFadeAlpha = alpha;
    }

    @Override
    public float bitsandbalance$getBottleCloudFadeAlpha() {
        return this.bitsandbalance$bottleCloudFadeAlpha;
    }

    @Override
    public void bitsandbalance$setBottleCloudBlockPos(BlockPos pos) {
        this.bitsandbalance$bottleCloudBlockPos = pos;
    }

    @Override
    public BlockPos bitsandbalance$getBottleCloudBlockPos() {
        return this.bitsandbalance$bottleCloudBlockPos;
    }
}
