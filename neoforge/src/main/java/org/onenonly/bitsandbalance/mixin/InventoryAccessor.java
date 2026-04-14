package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface InventoryAccessor {
	@Accessor("selected")
	int bitsandbalance$getSelected();

	@Accessor("selected")
	void bitsandbalance$setSelected(int selected);
}
