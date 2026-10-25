package org.onenonly.bitsandbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve the quantity produced by loot-table functions, including separate unstackable rewards. */
@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockDropsMixin {
    @Shadow private ItemStack item;
    @Shadow private Direction hitDirection;

    @ModifyExpressionValue(method = "unpackLootTable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;J)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> bitsandbalance$keepIdenticalRewards(ObjectArrayList<ItemStack> rolled) {
        if (rolled.size() < 2) return rolled;
        ItemStack first = rolled.getFirst();
        int count = 0;
        for (ItemStack stack : rolled) {
            if (!ItemStack.isSameItemSameComponents(first, stack)) return rolled;
            count += stack.getCount();
        }
        // The display/save stack may temporarily exceed its normal stack limit. Drop valid stacks below.
        if (count > 64) return rolled;
        var combined = new ObjectArrayList<ItemStack>();
        combined.add(first.copyWithCount(count));
        return combined;
    }

    @Inject(method = "dropContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), cancellable = true)
    private void bitsandbalance$dropWholeReward(CallbackInfo ci) {
        var self = (BrushableBlockEntity) (Object) this;
        if (this.item.getCount() <= 1 || !(self.getLevel() instanceof ServerLevel level)) return;
        var direction = this.hitDirection == null ? Direction.UP : this.hitDirection;
        var pos = self.getBlockPos().relative(direction);
        while (!this.item.isEmpty()) {
            ItemStack drop = this.item.split(Math.min(this.item.getCount(), this.item.getMaxStackSize()));
            var entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
            entity.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(entity);
        }
        this.item = ItemStack.EMPTY;
        ci.cancel();
    }
}
