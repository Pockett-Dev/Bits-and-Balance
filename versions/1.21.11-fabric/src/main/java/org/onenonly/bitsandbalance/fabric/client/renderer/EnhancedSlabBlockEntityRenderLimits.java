package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class EnhancedSlabBlockEntityRenderLimits {
    private static final int DEFAULT_CHUNK_PATH_FALLBACK_VIEW_DISTANCE = 96;
    private static final int DEFAULT_BLOCK_ENTITY_FALLBACK_VIEW_DISTANCE = 16;
    private static final int MIN_VIEW_DISTANCE = 4;
    private static final int MAX_VIEW_DISTANCE = 256;
    private static final int VIEW_DISTANCE = clamp(
            Integer.getInteger("bitsandbalance.enhancedSlabRenderDistance", defaultViewDistance()),
            MIN_VIEW_DISTANCE,
            MAX_VIEW_DISTANCE
    );
    private static final double SUBMIT_DISTANCE_SQUARED = (VIEW_DISTANCE + 1.0D) * (VIEW_DISTANCE + 1.0D);

    private static volatile Field cameraPosField;
    private static volatile Method cameraPosMethod;
    private static volatile boolean accessorResolved;

    private EnhancedSlabBlockEntityRenderLimits() {
    }

    public static int viewDistance() {
        return VIEW_DISTANCE;
    }

    private static int defaultViewDistance() {
        return EnhancedSlabRendererCompat.preferChunkRenderPath()
                ? DEFAULT_CHUNK_PATH_FALLBACK_VIEW_DISTANCE
                : DEFAULT_BLOCK_ENTITY_FALLBACK_VIEW_DISTANCE;
    }

    public static boolean shouldSubmit(BlockEntityRenderState state, Object cameraRenderState) {
        if (state == null || state.blockPos == null) {
            return true;
        }
        return shouldSubmit(state.blockPos, cameraRenderState);
    }

    public static boolean shouldSubmit(BlockPos blockPos, Object cameraRenderState) {
        if (blockPos == null || cameraRenderState == null) {
            return true;
        }

        Vec3 cameraPos = extractCameraPos(cameraRenderState);
        if (cameraPos == null) {
            return true;
        }

        double dx = blockPos.getX() + 0.5D - cameraPos.x();
        double dy = blockPos.getY() + 0.5D - cameraPos.y();
        double dz = blockPos.getZ() + 0.5D - cameraPos.z();
        return dx * dx + dy * dy + dz * dz <= SUBMIT_DISTANCE_SQUARED;
    }

    public static boolean shouldSubmit(BlockPos blockPos, Vec3 cameraPos) {
        if (blockPos == null || cameraPos == null) {
            return true;
        }

        double dx = blockPos.getX() + 0.5D - cameraPos.x();
        double dy = blockPos.getY() + 0.5D - cameraPos.y();
        double dz = blockPos.getZ() + 0.5D - cameraPos.z();
        return dx * dx + dy * dy + dz * dz <= SUBMIT_DISTANCE_SQUARED;
    }

    private static Vec3 extractCameraPos(Object cameraRenderState) {
        if (cameraRenderState instanceof Vec3 vec3) {
            return vec3;
        }

        resolveAccessor(cameraRenderState.getClass());
        try {
            if (cameraPosField != null) {
                Object value = cameraPosField.get(cameraRenderState);
                if (value instanceof Vec3 vec3) {
                    return vec3;
                }
            }
            if (cameraPosMethod != null) {
                Object value = cameraPosMethod.invoke(cameraRenderState);
                if (value instanceof Vec3 vec3) {
                    return vec3;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static void resolveAccessor(Class<?> cameraRenderStateClass) {
        if (accessorResolved) {
            return;
        }

        synchronized (EnhancedSlabBlockEntityRenderLimits.class) {
            if (accessorResolved) {
                return;
            }

            cameraPosField = findField(cameraRenderStateClass, "pos", "position");
            cameraPosMethod = findMethod(cameraRenderStateClass, "pos", "position", "getPos", "getPosition");
            accessorResolved = true;
        }
    }

    private static Field findField(Class<?> type, String... names) {
        for (String name : names) {
            Class<?> current = type;
            while (current != null) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String... names) {
        for (String name : names) {
            Class<?> current = type;
            while (current != null) {
                try {
                    Method method = current.getDeclaredMethod(name);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    current = current.getSuperclass();
                }
            }
        }
        return null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
