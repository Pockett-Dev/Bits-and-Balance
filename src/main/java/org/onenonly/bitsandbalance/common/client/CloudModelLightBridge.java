package org.onenonly.bitsandbalance.common.client;

//? if mc == 1.21.11
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//? endif

public final class CloudModelLightBridge {
    //? if mc == 1.21.11
    private static final Map<Class<?>, Method> SET_EXTERNAL_LIGHT = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> RESET_EXTERNAL_LIGHT = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> BACKING_LEVEL = new ConcurrentHashMap<>();

    public static BlockAndTintGetter wrapForModelShading(BlockAndTintGetter level) {
        Level backingLevel = getBackingLevel(level);
        if (backingLevel == null) {
            return level;
        }
        return new CardinalLightingWrappedLevel(level, backingLevel);
    }

    public static boolean setExternalLight(BlockAndTintGetter level, int packedLight) {
        BlockAndTintGetter unwrapped = unwrapLevel(level);
        if (unwrapped == null) {
            return false;
        }
        Method method = SET_EXTERNAL_LIGHT.computeIfAbsent(unwrapped.getClass(), CloudModelLightBridge::findSetExternalLight);
        if (method == null) {
            return false;
        }
        try {
            method.invoke(unwrapped, packedLight);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void resetExternalLight(BlockAndTintGetter level) {
        BlockAndTintGetter unwrapped = unwrapLevel(level);
        if (unwrapped == null) {
            return;
        }
        Method method = RESET_EXTERNAL_LIGHT.computeIfAbsent(unwrapped.getClass(), CloudModelLightBridge::findResetExternalLight);
        if (method == null) {
            return;
        }
        try {
            method.invoke(unwrapped);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static BlockAndTintGetter unwrapLevel(BlockAndTintGetter level) {
        if (level instanceof CardinalLightingWrappedLevel wrappedLevel) {
            return wrappedLevel.delegate;
        }
        return level;
    }

    private static @Nullable Level getBackingLevel(BlockAndTintGetter level) {
        BlockAndTintGetter unwrapped = unwrapLevel(level);
        if (!(unwrapped instanceof Level directLevel)) {
            return null;
        }

        Field field = BACKING_LEVEL.computeIfAbsent(directLevel.getClass(), CloudModelLightBridge::findBackingLevel);
        if (field == null) {
            return null;
        }
        try {
            Object value = field.get(directLevel);
            return value instanceof Level backingLevel ? backingLevel : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method findSetExternalLight(Class<?> type) {
        try {
            return type.getMethod("setExternalLight", int.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findResetExternalLight(Class<?> type) {
        try {
            return type.getMethod("resetExternalLight");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Field findBackingLevel(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField("level");
                if (!Level.class.isAssignableFrom(field.getType())) {
                    current = current.getSuperclass();
                    continue;
                }
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static final class CardinalLightingWrappedLevel implements BlockAndTintGetter {
        private final BlockAndTintGetter delegate;
        private final Level backingLevel;

        private CardinalLightingWrappedLevel(BlockAndTintGetter delegate, Level backingLevel) {
            this.delegate = delegate;
            this.backingLevel = backingLevel;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return delegate.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return delegate.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return delegate.getFluidState(pos);
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public int getMinY() {
            return delegate.getMinY();
        }

        @Override
        public float getShade(Direction direction, boolean shaded) {
            return backingLevel.getShade(direction, shaded);
        }

        @Override
        public int getBrightness(LightLayer lightLayer, BlockPos pos) {
            return delegate.getBrightness(lightLayer, pos);
        }

        @Override
        public int getRawBrightness(BlockPos pos, int amount) {
            return delegate.getRawBrightness(pos, amount);
        }

        @Override
        public boolean canSeeSky(BlockPos pos) {
            return delegate.canSeeSky(pos);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return delegate.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
            return delegate.getBlockTint(blockPos, colorResolver);
        }
    }
    //? endif

    private CloudModelLightBridge() {
    }
}
