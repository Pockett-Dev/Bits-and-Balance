package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlockEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge compatibility: dynamically-registered enhanced slab-family blocks need to be accepted by
 * their {@link BlockEntityType} or the game can discard the block entity during world or virtual-world creation.
 *
 * <p>Some environments/versions make it hard to reliably expand BlockEntityType's internal valid-block set
 * via reflection. This mixin provides a stable fallback: treat our vertical slab blocks as valid for the
 * BitsAndBalance vertical slab block entity type.</p>
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeVerticalSlabValidMixin {

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true, require = 0)
    private void bitsandbalance$verticalSlabsAreValid(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state == null) return;

        Object self = this;

        if (state.getBlock() instanceof VerticalSlabBlock) {
            if (CommonBlockEntities.VERTICAL_SLAB != null && self == CommonBlockEntities.VERTICAL_SLAB) {
                cir.setReturnValue(true);
                return;
            }
            if (bitsandbalance$matchesRegistryId(self, "vertical_slab")) {
                cir.setReturnValue(true);
            }
            return;
        }

        if (state.getBlock() instanceof StepBlock) {
            if (CommonBlockEntities.STEP != null && self == CommonBlockEntities.STEP) {
                cir.setReturnValue(true);
                return;
            }
            if (bitsandbalance$matchesRegistryId(self, "step")) {
                cir.setReturnValue(true);
            }
            return;
        }

        if (state.getBlock() instanceof VerticalStepBlock) {
            if (CommonBlockEntities.VERTICAL_STEP != null && self == CommonBlockEntities.VERTICAL_STEP) {
                cir.setReturnValue(true);
                return;
            }
            if (bitsandbalance$matchesRegistryId(self, "vertical_step")) {
                cir.setReturnValue(true);
            }
        }
    }

    private static boolean bitsandbalance$matchesRegistryId(Object self, String path) {
        try {
            Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>) self);
            return id != null && "bitsandbalance".equals(id.getNamespace()) && path.equals(id.getPath());
        } catch (Throwable ignored) {
            return false;
        }
    }
}
