package org.onenonly.bitsandbalance.fabric.pickblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.util.PickBlockHelperCompat;

public final class FabricPickBlockCompat {
    private FabricPickBlockCompat() {
    }

    public record PickTarget(BlockState sourceState, ItemStack stack) {
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, BlockPos pos) {
        PickBlockHelperCompat.PickTarget target = PickBlockHelperCompat.resolve(level, player, pos);
        if (target == null || target.stack().isEmpty()) {
            return null;
        }
        return new PickTarget(target.sourceState(), target.stack());
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, @Nullable BlockHitResult hitResult) {
        PickBlockHelperCompat.PickTarget target = PickBlockHelperCompat.resolve(level, player, hitResult);
        if (target == null || target.stack().isEmpty()) {
            return null;
        }
        return new PickTarget(target.sourceState(), target.stack());
    }
}