package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VirtualRenderWorldLightBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-virtual-render-bridge");
    private static volatile boolean LOGGED_SAMPLE_QUAD;
    private static volatile boolean LOGGED_RENDER_BATCHED_MISSING;
    private static volatile boolean LOGGED_RENDER_BATCHED_FAILURE;
    private static final Map<Class<?>, Method> RENDER_BATCHED = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> BLOCK_RENDERER_ACCESSOR = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> BLOCK_RENDERER_FIELD = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> SET_EXTERNAL_LIGHT = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> RESET_EXTERNAL_LIGHT = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> EXTERNAL_PACKED_LIGHT = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> GET_QUADS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> QUAD_POSITION = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> QUAD_PACKED_UV = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> QUAD_DIRECTION = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> QUAD_MATERIAL_INFO = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> MATERIAL_INFO_SPRITE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> SPRITE_WRAP = new ConcurrentHashMap<>();

    private VirtualRenderWorldLightBridge() {
    }

    public static void renderBatched(BlockState blockState,
                                     BlockPos blockPos,
                                     Object level,
                                     PoseStack poseStack,
                                     VertexConsumer consumer,
                                     boolean checkSides,
                                     List<?> parts,
                                     int packedLight) {
        boolean externalLightApplied = bitsandbalance$setExternalLight(level, packedLight);
        try {
            Object minecraft = Minecraft.getInstance();
            Object blockRenderer = bitsandbalance$getBlockRenderer(minecraft);
            if (blockRenderer != null) {
                Method method = RENDER_BATCHED.computeIfAbsent(blockRenderer.getClass(), VirtualRenderWorldLightBridge::bitsandbalance$findRenderBatched);
                if (method != null) {
                    method.invoke(blockRenderer, blockState, blockPos, level, poseStack, consumer, checkSides, parts);
                    return;
                }
            }
            if (!LOGGED_RENDER_BATCHED_MISSING) {
                LOGGED_RENDER_BATCHED_MISSING = true;
                LOGGER.warn("Falling back to direct enhanced slab quad emission because no compatible renderBatched method was found for {}", blockRenderer != null ? blockRenderer.getClass().getName() : "null");
            }
            bitsandbalance$emitPartsDirect(poseStack, consumer, parts, packedLight);
        } catch (ReflectiveOperationException throwable) {
            if (!LOGGED_RENDER_BATCHED_FAILURE) {
                LOGGED_RENDER_BATCHED_FAILURE = true;
                LOGGER.warn("Falling back to direct enhanced slab quad emission after renderBatched failure", throwable);
            }
            bitsandbalance$emitPartsDirect(poseStack, consumer, parts, packedLight);
        } finally {
            if (externalLightApplied) {
                bitsandbalance$resetExternalLight(level);
            }
        }
    }

    public static int capturePackedLight(Object level, int fallbackPackedLight) {
        if (level == null) {
            return fallbackPackedLight;
        }
        Field field = EXTERNAL_PACKED_LIGHT.computeIfAbsent(level.getClass(), VirtualRenderWorldLightBridge::bitsandbalance$findExternalPackedLight);
        if (field == null) {
            return fallbackPackedLight;
        }
        try {
            int packedLight = field.getInt(level);
            return packedLight != 0 ? packedLight : fallbackPackedLight;
        } catch (ReflectiveOperationException ignored) {
            return fallbackPackedLight;
        }
    }

    private static boolean bitsandbalance$setExternalLight(Object level, int packedLight) {
        if (level == null) {
            return false;
        }
        Method method = SET_EXTERNAL_LIGHT.computeIfAbsent(level.getClass(), VirtualRenderWorldLightBridge::bitsandbalance$findSetExternalLight);
        if (method == null) {
            return false;
        }
        try {
            method.invoke(level, packedLight);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void bitsandbalance$resetExternalLight(Object level) {
        if (level == null) {
            return;
        }
        Method method = RESET_EXTERNAL_LIGHT.computeIfAbsent(level.getClass(), VirtualRenderWorldLightBridge::bitsandbalance$findResetExternalLight);
        if (method == null) {
            return;
        }
        try {
            method.invoke(level);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method bitsandbalance$findSetExternalLight(Class<?> type) {
        try {
            return type.getMethod("setExternalLight", int.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method bitsandbalance$findRenderBatched(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals("renderBatched") || method.getParameterCount() != 7) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!BlockState.class.isAssignableFrom(parameterTypes[0])) {
                continue;
            }
            if (!BlockPos.class.isAssignableFrom(parameterTypes[1])) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }

    private static Method bitsandbalance$findNoArgMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object bitsandbalance$getBlockRenderer(Object minecraft) throws ReflectiveOperationException {
        if (minecraft == null) {
            return null;
        }

        Method accessor = BLOCK_RENDERER_ACCESSOR.computeIfAbsent(minecraft.getClass(), type -> {
            Method named = bitsandbalance$findNoArgMethod(type, "getBlockRenderer");
            if (named != null) {
                named.setAccessible(true);
                return named;
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == Void.TYPE || returnType.isPrimitive()) {
                    continue;
                }
                if (bitsandbalance$findRenderBatched(returnType) != null) {
                    method.setAccessible(true);
                    return method;
                }
            }
            for (Method method : type.getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == Void.TYPE || returnType.isPrimitive()) {
                    continue;
                }
                if (bitsandbalance$findRenderBatched(returnType) != null) {
                    method.setAccessible(true);
                    return method;
                }
            }
            return null;
        });
        if (accessor != null) {
            Object blockRenderer = accessor.invoke(minecraft);
            if (blockRenderer != null) {
                return blockRenderer;
            }
        }

        Field field = BLOCK_RENDERER_FIELD.computeIfAbsent(minecraft.getClass(), type -> {
            Class<?> current = type;
            while (current != null) {
                for (Field candidate : current.getDeclaredFields()) {
                    if (bitsandbalance$findRenderBatched(candidate.getType()) != null) {
                        candidate.setAccessible(true);
                        return candidate;
                    }
                }
                current = current.getSuperclass();
            }
            return null;
        });
        return field != null ? field.get(minecraft) : null;
    }

    private static void bitsandbalance$emitPartsDirect(PoseStack poseStack, VertexConsumer consumer, List<?> parts, int packedLight) {
        if (parts == null || parts.isEmpty()) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        for (Object part : parts) {
            if (part == null) {
                continue;
            }
            bitsandbalance$emitPartDirect(part, pose, consumer, packedLight);
        }
    }

    @SuppressWarnings("unchecked")
    private static void bitsandbalance$emitPartDirect(Object part, PoseStack.Pose pose, VertexConsumer consumer, int packedLight) {
        try {
            Method getQuads = GET_QUADS.computeIfAbsent(part.getClass(), type -> {
                try {
                    return type.getMethod("getQuads", Direction.class);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            if (getQuads == null) {
                return;
            }
            for (Direction direction : Direction.values()) {
                List<Object> quads = (List<Object>) getQuads.invoke(part, direction);
                if (quads == null || quads.isEmpty()) {
                    continue;
                }
                for (Object quad : quads) {
                    if (quad != null) {
                        bitsandbalance$emitQuadDirect(quad, pose, consumer, packedLight);
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void bitsandbalance$emitQuadDirect(Object quad, PoseStack.Pose pose, VertexConsumer consumer, int packedLight) {
        try {
            Method positionMethod = QUAD_POSITION.computeIfAbsent(quad.getClass(), type -> {
                try {
                    return type.getMethod("position", int.class);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            Method packedUvMethod = QUAD_PACKED_UV.computeIfAbsent(quad.getClass(), type -> {
                try {
                    return type.getMethod("packedUV", int.class);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            Method directionMethod = QUAD_DIRECTION.computeIfAbsent(quad.getClass(), type -> {
                try {
                    return type.getMethod("direction");
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            Method materialInfoMethod = QUAD_MATERIAL_INFO.computeIfAbsent(quad.getClass(), type -> {
                try {
                    return type.getMethod("materialInfo");
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            if (positionMethod == null || packedUvMethod == null || directionMethod == null) {
                return;
            }

            Direction direction = (Direction) directionMethod.invoke(quad);
            float nx = direction != null ? direction.getStepX() : 0.0f;
            float ny = direction != null ? direction.getStepY() : 1.0f;
            float nz = direction != null ? direction.getStepZ() : 0.0f;
            int shade = bitsandbalance$shade(direction);
            Object spriteForLog = null;
            if (materialInfoMethod != null) {
                Object materialInfo = materialInfoMethod.invoke(quad);
                if (materialInfo != null) {
                    Method spriteMethod = MATERIAL_INFO_SPRITE.computeIfAbsent(materialInfo.getClass(), type -> {
                        try {
                            return type.getMethod("sprite");
                        } catch (NoSuchMethodException e) {
                            return null;
                        }
                    });
                    if (spriteMethod != null) {
                        Object sprite = spriteMethod.invoke(materialInfo);
                        spriteForLog = sprite;
                    }
                }
            }

            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc position = (Vector3fc) positionMethod.invoke(quad, vertex);
                long packedUv = ((Number) packedUvMethod.invoke(quad, vertex)).longValue();
                float atlasV = Float.intBitsToFloat((int) packedUv);
                float atlasU = Float.intBitsToFloat((int) (packedUv >>> 32));
                if (!LOGGED_SAMPLE_QUAD && vertex == 0) {
                    LOGGED_SAMPLE_QUAD = true;
                    LOGGER.warn("Sample fallback quad: direction={}, atlasU={}, atlasV={}, sprite={}",
                            direction, atlasU, atlasV, spriteForLog);
                }

                consumer.addVertex(pose, position.x(), position.y(), position.z())
            .setColor(shade, shade, shade, 255)
                        .setUv(atlasU, atlasV)
                        .setOverlay(0)
                        .setLight(packedLight)
                        .setNormal(pose, nx, ny, nz);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method bitsandbalance$findResetExternalLight(Class<?> type) {
        try {
            return type.getMethod("resetExternalLight");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Field bitsandbalance$findExternalPackedLight(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField("externalPackedLight");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static int bitsandbalance$shade(Direction direction) {
        float shade = switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
            case null -> 1.0F;
        };
        return Math.max(0, Math.min(255, Math.round(shade * 255.0F)));
    }
}