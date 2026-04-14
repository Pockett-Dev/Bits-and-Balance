package org.onenonly.bitsandbalance.fabric.mixin;

import org.onenonly.bitsandbalance.fabric.compat.BlockEntityTypeExt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin implements BlockEntityTypeExt {

    @Shadow
    @Final
    @Mutable
    private Set<Block> validBlocks;

    @Override
    public void bitsandbalance$addValidBlocks(Block... blocks) {
        if (!(validBlocks instanceof HashSet)) {
            validBlocks = new HashSet<>(validBlocks);
        }
        validBlocks.addAll(Arrays.asList(blocks));
    }
}
