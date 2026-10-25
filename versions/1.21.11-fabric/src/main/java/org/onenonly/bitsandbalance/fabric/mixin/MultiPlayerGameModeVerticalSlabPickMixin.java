package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.onenonly.bitsandbalance.fabric.pickblock.FabricPickBlockCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeVerticalSlabPickMixin {
	@Shadow @Final
	private Minecraft minecraft;

	@ModifyVariable(
			method = "handlePickItemFromBlock",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"
			),
			require = 0
	)
	private ItemStack bitsandbalance$pickTargetedEnhancedSlabPart(ItemStack original) {
		try {
			Minecraft mc = this.minecraft;
			if (mc == null || mc.level == null || mc.player == null) return original;
			if (!(mc.hitResult instanceof BlockHitResult hit)) return original;

			FabricPickBlockCompat.PickTarget pickTarget =
				FabricPickBlockCompat.resolve(mc.level, mc.player, hit);
			return pickTarget == null || pickTarget.stack().isEmpty() ? original : pickTarget.stack();
		} catch (Throwable ignored) {
			return original;
		}
	}
}
