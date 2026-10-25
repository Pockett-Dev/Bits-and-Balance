package org.onenonly.bitsandbalance.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class PickBlockHelperCompat {
    private static final String HELPER_CLASS = "org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPickBlockHelper";

    private PickBlockHelperCompat() {
    }

    public record PickTarget(BlockState sourceState, ItemStack stack) {
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, BlockPos pos) {
        try {
            Class<?> helperClass = Class.forName(HELPER_CLASS);
            Method resolve = helperClass.getMethod("resolve", LevelReader.class, Player.class, BlockPos.class);
            return adapt(resolve.invoke(null, level, player, pos));
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, @Nullable BlockHitResult hitResult) {
        if (hitResult == null) return null;
        try {
            Class<?> helperClass = Class.forName(HELPER_CLASS);
            Method resolve = helperClass.getMethod("resolve", LevelReader.class, Player.class, BlockHitResult.class);
            return adapt(resolve.invoke(null, level, player, hitResult));
        } catch (Throwable ignored) {
            return resolve(level, player, hitResult.getBlockPos());
        }
    }

    private static @Nullable PickTarget adapt(@Nullable Object raw) {
        if (raw == null) return null;

        try {
            Method sourceState = raw.getClass().getMethod("sourceState");
            Method stack = raw.getClass().getMethod("stack");
            Object stateValue = sourceState.invoke(raw);
            Object stackValue = stack.invoke(raw);
            if (!(stateValue instanceof BlockState state)) return null;
            if (!(stackValue instanceof ItemStack itemStack)) return null;
            if (itemStack.isEmpty()) return null;
            return new PickTarget(state, itemStack);
        } catch (Throwable ignored) {
            return null;
        }
    }
}