package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectiveLitBlockModelTesselationBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fabric-lit-tessellation");

    private static final String CLASS_MODEL_BLOCK_RENDERER = "net.minecraft.client.renderer.block.ModelBlockRenderer";
    private static final String CLASS_BLOCK_STATE_MODEL = "net.minecraft.client.renderer.block.dispatch.BlockStateModel";
    private static final String CLASS_BLOCK_QUAD_OUTPUT = "net.minecraft.client.renderer.block.BlockQuadOutput";
    private static final String CLASS_BLOCK_AND_TINT_GETTER = "net.minecraft.client.renderer.block.BlockAndTintGetter";

    private static volatile Object renderer;
    private static volatile Constructor<?> rendererCtor3;
    private static volatile Constructor<?> rendererCtor1;
    private static volatile Method modernTesselateMethod;
    private static volatile Method legacyTesselateMethod;
    private static volatile Object emptyBlockAndTintGetter;
    private static volatile Class<?> blockStateModelClass;
    private static volatile Class<?> blockQuadOutputClass;
    private static final Map<Class<?>, Method> PUT_BLOCK_BAKED_QUAD = new ConcurrentHashMap<>();
    private static volatile boolean loggedFailure;

    private ReflectiveLitBlockModelTesselationBridge() {
    }

    public static void render(PoseStack.Pose pose,
                              VertexConsumer consumer,
                              Object level,
                              BlockPos blockPos,
                              BlockState sourceState,
                              List<?> parts,
                              int packedLight) {
        if (pose == null || consumer == null || blockPos == null || sourceState == null || parts == null || parts.isEmpty()) {
            return;
        }

        try {
            Object modelBlockRenderer = getRenderer();
            Method modern = getModernTesselateMethod(modelBlockRenderer.getClass());
            if (modern != null) {
                Vector3f translation = bitsandbalance$extractTranslation(pose);
                modern.invoke(
                        modelBlockRenderer,
                        createBlockQuadOutputProxy(consumer),
                        translation.x,
                        translation.y,
                        translation.z,
                        getBlockAndTintGetter(level),
                        blockPos,
                        sourceState,
                        createBlockStateModelProxy(parts),
                        RandomSource.create(blockPos.asLong()).nextLong()
                );
                return;
            }

            Method legacy = getLegacyTesselateMethod(modelBlockRenderer.getClass());
            if (legacy != null) {
                PoseStack tmp = new PoseStack();
                tmp.last().pose().set(pose.pose());
                tmp.last().normal().set(pose.normal());
                legacy.invoke(
                        modelBlockRenderer,
                        getBlockAndTintGetter(level),
                        parts,
                        sourceState,
                        blockPos,
                        tmp,
                        consumer,
                        false,
                        packedLight
                );
                return;
            }
        } catch (ReflectiveOperationException throwable) {
            if (!loggedFailure) {
                loggedFailure = true;
                LOGGER.warn("Fabric lit tessellation bridge failed; falling back to direct quad emission", throwable);
            }
        }

        VirtualRenderWorldLightBridge.renderBatched(sourceState, blockPos, level, toPoseStack(pose), consumer, false, parts, packedLight);
    }

    private static Vector3f bitsandbalance$extractTranslation(PoseStack.Pose pose) {
        return pose.pose().getTranslation(new Vector3f());
    }

    private static PoseStack toPoseStack(PoseStack.Pose pose) {
        PoseStack tmp = new PoseStack();
        tmp.last().pose().set(pose.pose());
        tmp.last().normal().set(pose.normal());
        return tmp;
    }

    private static Object getRenderer() throws ReflectiveOperationException {
        Object current = renderer;
        if (current != null) {
            return current;
        }

        Class<?> rendererClass = Class.forName(CLASS_MODEL_BLOCK_RENDERER);
        Object blockColors = Minecraft.getInstance().getBlockColors();

        Constructor<?> ctor = rendererCtor3;
        if (ctor == null) {
            for (Constructor<?> candidate : rendererClass.getConstructors()) {
                Class<?>[] parameterTypes = candidate.getParameterTypes();
                if (parameterTypes.length == 3) {
                    ctor = candidate;
                    candidate.setAccessible(true);
                    rendererCtor3 = candidate;
                    break;
                }
            }
        }
        if (ctor != null) {
            current = ctor.newInstance(true, false, blockColors);
            renderer = current;
            return current;
        }

        ctor = rendererCtor1;
        if (ctor == null) {
            for (Constructor<?> candidate : rendererClass.getConstructors()) {
                if (candidate.getParameterCount() == 1) {
                    ctor = candidate;
                    candidate.setAccessible(true);
                    rendererCtor1 = candidate;
                    break;
                }
            }
        }
        if (ctor != null) {
            current = ctor.newInstance(blockColors);
            renderer = current;
            return current;
        }

        throw new NoSuchMethodException("No compatible ModelBlockRenderer constructor found");
    }

    private static Method getModernTesselateMethod(Class<?> type) {
        Method local = modernTesselateMethod;
        if (local != null) {
            return local;
        }
        for (Method method : type.getMethods()) {
            if (!method.getName().equals("tesselateBlock") || method.getParameterCount() != 9) {
                continue;
            }
            method.setAccessible(true);
            modernTesselateMethod = method;
            return method;
        }
        return null;
    }

    private static Method getLegacyTesselateMethod(Class<?> type) {
        Method local = legacyTesselateMethod;
        if (local != null) {
            return local;
        }
        for (Method method : type.getMethods()) {
            if (!method.getName().equals("tesselateBlock") || method.getParameterCount() != 8) {
                continue;
            }
            method.setAccessible(true);
            legacyTesselateMethod = method;
            return method;
        }
        return null;
    }

    private static Object getBlockAndTintGetter(Object level) throws ReflectiveOperationException {
        Class<?> getterClass = Class.forName(CLASS_BLOCK_AND_TINT_GETTER);
        if (getterClass.isInstance(level)) {
            return level;
        }
        Object local = emptyBlockAndTintGetter;
        if (local == null) {
            local = getterClass.getField("EMPTY").get(null);
            emptyBlockAndTintGetter = local;
        }
        return local;
    }

    private static Object createBlockStateModelProxy(List<?> parts) throws ReflectiveOperationException {
        Class<?> modelClass = blockStateModelClass;
        if (modelClass == null) {
            modelClass = Class.forName(CLASS_BLOCK_STATE_MODEL);
            blockStateModelClass = modelClass;
        }

        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "collectParts" -> {
                @SuppressWarnings("unchecked")
                List<Object> out = (List<Object>) args[1];
                out.addAll(parts);
                yield null;
            }
            case "particleMaterial" -> parts.isEmpty() ? null : parts.getFirst().getClass().getMethod("particleMaterial").invoke(parts.getFirst());
            case "materialFlags" -> parts.isEmpty() ? 0 : parts.getFirst().getClass().getMethod("materialFlags").invoke(parts.getFirst());
            case "toString" -> "ReflectiveBlockStateModelProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
            default -> null;
        };

        return Proxy.newProxyInstance(
                ReflectiveLitBlockModelTesselationBridge.class.getClassLoader(),
                new Class<?>[]{modelClass},
                handler
        );
    }

    private static Object createBlockQuadOutputProxy(VertexConsumer consumer) throws ReflectiveOperationException {
        Class<?> outputClass = blockQuadOutputClass;
        if (outputClass == null) {
            outputClass = Class.forName(CLASS_BLOCK_QUAD_OUTPUT);
            blockQuadOutputClass = outputClass;
        }

        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("put") && args != null && args.length == 5) {
                Method putBlockBakedQuad = PUT_BLOCK_BAKED_QUAD.computeIfAbsent(consumer.getClass(), ignored -> findPutBlockBakedQuad(consumer.getClass()));
                if (putBlockBakedQuad != null) {
                    putBlockBakedQuad.invoke(consumer, args[0], args[1], args[2], args[3], args[4]);
                }
                return null;
            }
            if (method.getName().equals("toString")) {
                return "ReflectiveBlockQuadOutputProxy";
            }
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            if (method.getName().equals("equals")) {
                return proxy == (args != null && args.length > 0 ? args[0] : null);
            }
            return null;
        };

        return Proxy.newProxyInstance(
                ReflectiveLitBlockModelTesselationBridge.class.getClassLoader(),
                new Class<?>[]{outputClass},
                handler
        );
    }

    private static Method findPutBlockBakedQuad(Class<?> consumerClass) {
        for (Method method : consumerClass.getMethods()) {
            if (!method.getName().equals("putBlockBakedQuad") || method.getParameterCount() != 5) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        return null;
    }
}