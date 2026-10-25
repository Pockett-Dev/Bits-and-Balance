package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.onenonly.bitsandbalance.fabric.pickblock.FabricPickBlockCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MultiPlayerGameModeVerticalSlabPickMixin {
	@Shadow
	public HitResult hitResult;

	@Shadow
	public ClientLevel level;

	@Shadow
	public LocalPlayer player;

	@ModifyVariable(
			method = "pickBlock",
			at = @At(
					value = "INVOKE_ASSIGN",
					target = "Lnet/minecraft/world/level/block/Block;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"
			),
			require = 0
	)
	private ItemStack bitsandbalance$pickTargetedEnhancedSlabPart(ItemStack original) {
		try {
			if (this.level == null || this.player == null) return original;
			if (!(this.hitResult instanceof BlockHitResult hit)) return original;

			FabricPickBlockCompat.PickTarget pickTarget =
				FabricPickBlockCompat.resolve(this.level, this.player, hit);
			return pickTarget == null || pickTarget.stack().isEmpty() ? original : pickTarget.stack();
		} catch (Throwable ignored) {
			return original;
		}
	}
}
