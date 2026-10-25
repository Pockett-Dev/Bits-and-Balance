package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//? else
import net.minecraft.world.level.BlockAndTintGetter;
//? endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class BioluminescenceSodiumLightPipelineHelper {
    private BioluminescenceSodiumLightPipelineHelper() {
    }

    public static void apply(Object pipeline, Object quad, BlockPos pos, Object lightData, Direction cullFace, Direction lightFace) {
        int[] lightmaps = getLightmaps(lightData);
        if (quad == null || pos == null || lightmaps == null || lightmaps.length == 0) {
            return;
        }

        BlockAndTintGetter level = getBlockAndTintGetter(getLevel(pipeline));
        Direction faceDirection = lightFace != null ? lightFace : cullFace;
        if (faceDirection == null) {
            faceDirection = getLightFace(quad);
        }

        int vertexCount = Math.min(4, lightmaps.length);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            double sampleX = pos.getX() + getVertexCoordinate(quad, "getX", vertex);
            double sampleY = pos.getY() + getVertexCoordinate(quad, "getY", vertex);
            double sampleZ = pos.getZ() + getVertexCoordinate(quad, "getZ", vertex);
            lightmaps[vertex] = BioluminescenceLightManager.getLightmapWithDynamicLight(
                    level,
                    sampleX,
                    sampleY,
                    sampleZ,
                    faceDirection,
                    lightmaps[vertex]
            );
        }
    }

    private static int[] getLightmaps(Object lightData) {
        if (lightData == null) {
            return null;
        }

        try {
            Field field = lightData.getClass().getField("lm");
            Object value = field.get(lightData);
            return value instanceof int[] lightmaps ? lightmaps : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object getLevel(Object pipeline) {
        if (pipeline == null) {
            return null;
        }

        Object directLevel = getFieldValue(pipeline, "level");
        if (directLevel != null) {
            return directLevel;
        }

        Object lightCache = getFieldValue(pipeline, "lightCache");
        if (lightCache != null) {
            Object level = invokeNoArgs(lightCache, "getLevel");
            if (level != null) {
                return level;
            }
            level = getFieldValue(lightCache, "level");
            if (level != null) {
                return level;
            }
        }

        return invokeNoArgs(pipeline, "getLevel");
    }

    private static BlockAndTintGetter getBlockAndTintGetter(Object level) {
        if (level instanceof BlockAndTintGetter blockAndTintGetter) {
            return blockAndTintGetter;
        }

        Object clientLevel = Minecraft.getInstance().level;
        return clientLevel instanceof BlockAndTintGetter blockAndTintGetter ? blockAndTintGetter : null;
    }

    private static Object getFieldValue(Object owner, String fieldName) {
        Field field = getDeclaredField(owner, fieldName);
        if (field == null) {
            return null;
        }

        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Field getDeclaredField(Object owner, String fieldName) {
        if (owner == null) {
            return null;
        }

        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                return type.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static Object invokeNoArgs(Object owner, String methodName) {
        if (owner == null) {
            return null;
        }

        try {
            Method method = owner.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(owner);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Direction getLightFace(Object quad) {
        try {
            Method getLightFace = quad.getClass().getMethod("getLightFace");
            getLightFace.setAccessible(true);
            Object value = getLightFace.invoke(quad);
            return value instanceof Direction direction ? direction : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static float getVertexCoordinate(Object quad, String methodName, int vertex) {
        try {
            Method method = quad.getClass().getMethod(methodName, int.class);
            method.setAccessible(true);
            Object value = method.invoke(quad, vertex);
            return value instanceof Number number ? number.floatValue() : 0.5F;
        } catch (ReflectiveOperationException ignored) {
            return 0.5F;
        }
    }
}
