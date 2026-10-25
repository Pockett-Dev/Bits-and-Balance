package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Level.class)
public abstract class AutoWaterloggingSetBlockMixin {
    @ModifyArgs(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 0
    )
    private void bitsandbalance$prepareAutoWaterloggedSetBlockState(Args args) {
        bitsandbalance$prepareAutoWaterloggedState(args);
    }

    @ModifyArgs(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            require = 0
    )
    private void bitsandbalance$prepareAutoWaterloggedSetBlockState1211(Args args) {
        bitsandbalance$prepareAutoWaterloggedState(args);
    }

    private void bitsandbalance$prepareAutoWaterloggedState(Args args) {
        BlockPos pos = args.get(0);
        BlockState state = args.get(1);
        args.set(1, AutoWaterloggingRuntime.prepareSetBlockState((Level) (Object) this, pos, state));
    }
}
