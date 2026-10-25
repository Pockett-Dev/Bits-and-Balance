package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.onenonly.bitsandbalance.common.client.SoulFireFlameRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateSoulFireMixin implements SoulFireFlameRenderStateAccess {

    @Unique
    private boolean bitsandbalance$soulFireFlame;

    @Override
    public boolean bitsandbalance$isSoulFireFlame() {
        return bitsandbalance$soulFireFlame;
    }

    @Override
    public void bitsandbalance$setSoulFireFlame(boolean soulFire) {
        this.bitsandbalance$soulFireFlame = soulFire;
    }
}
