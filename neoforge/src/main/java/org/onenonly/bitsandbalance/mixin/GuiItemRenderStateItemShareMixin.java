package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.gui.render.state.GuiItemRenderState;
import org.onenonly.bitsandbalance.client.ItemShareGuiItemAlpha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiItemRenderState.class)
public class GuiItemRenderStateItemShareMixin implements ItemShareGuiItemAlpha {
	@Unique
	private float bitsandbalance$itemShareAlpha = 1.0F;
	@Unique
	private boolean bitsandbalance$hasItemShareAlpha = false;

	@Override
	public float bitsandbalance$getItemShareAlpha() {
		return this.bitsandbalance$itemShareAlpha;
	}

	@Override
	public void bitsandbalance$setItemShareAlpha(float alpha) {
		this.bitsandbalance$itemShareAlpha = alpha;
		this.bitsandbalance$hasItemShareAlpha = true;
	}

	@Override
	public boolean bitsandbalance$hasItemShareAlpha() {
		return this.bitsandbalance$hasItemShareAlpha;
	}
}
