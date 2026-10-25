package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.content.ModKiln;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the vanilla furnace block-entity type live inside the Kiln block. */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeKilnValidMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$kilnIsAValidFurnace(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!ModKiln.isKiln(state)) {
            return;
        }

        // Comparing registry IDs also covers MC 26.2's BlockEntityTypes holder class.
        var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>) (Object) this);
        if (id != null && "minecraft".equals(id.getNamespace()) && "furnace".equals(id.getPath())) {
            cir.setReturnValue(true);
        }
    }
}
