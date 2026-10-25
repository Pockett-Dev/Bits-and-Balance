package org.onenonly.bitsandbalance.client;

/**
 * Implemented via mixin on Minecraft's GuiItemRenderState so we can carry per-item
 * chat fade alpha through the deferred GUI item atlas pipeline.
 */
public interface ItemShareGuiItemAlpha {
	float bitsandbalance$getItemShareAlpha();
	void bitsandbalance$setItemShareAlpha(float alpha);
	boolean bitsandbalance$hasItemShareAlpha();
}
