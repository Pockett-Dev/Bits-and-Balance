package org.onenonly.bitsandbalance.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;

@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.util.assembly.SimAssemblyContraption", remap = false)
public abstract class SableSimAssemblyContraptionEnhancedSlabConnectivityMixin {
    @WrapOperation(
            method = "moveBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z",
                    ordinal = 3
            ),
            require = 0
    )
    private boolean bitsandbalance$queueEnhancedSlabNeighborByShape(Queue<BlockPos> queue,
                                                                     Object candidate,
                                                                     Operation<Boolean> original,
                                                                     @Local(argsOnly = true, ordinal = 0) Level level,
                                                                     @Local(index = 5) BlockPos sourcePos,
                                                                     @Local(index = 6) BlockState sourceState,
                                                                     @Local(index = 15) BlockPos neighborPos,
                                                                     @Local(index = 16) BlockState neighborState,
                                                                     @Local(index = 18) boolean glued) {
        if (!(candidate instanceof BlockPos) || glued || SableCompat.isEnhancedSlabAssemblyConnected(level, sourcePos, sourceState, neighborPos, neighborState)) {
            return original.call(queue, candidate);
        }
        return false;
    }
}
