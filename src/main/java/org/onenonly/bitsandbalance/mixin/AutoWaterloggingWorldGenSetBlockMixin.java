package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WorldGenRegion.class)
public abstract class AutoWaterloggingWorldGenSetBlockMixin {
    @ModifyArgs(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 0
    )
    private void bitsandbalance$prepareAutoWaterloggedWorldgenState1211(Args args) {
        bitsandbalance$prepareAutoWaterloggedWorldgenState(args);
    }

    @ModifyArgs(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 0
    )
    private void bitsandbalance$prepareAutoWaterloggedWorldgenStateModern(Args args) {
        bitsandbalance$prepareAutoWaterloggedWorldgenState(args);
    }

    private void bitsandbalance$prepareAutoWaterloggedWorldgenState(Args args) {
        BlockPos pos = args.get(0);
        BlockState state = args.get(1);
        args.set(1, AutoWaterloggingRuntime.prepareWorldgenSetBlockState(
                (WorldGenLevel) (Object) this,
                pos,
                state
        ));
    }
}
