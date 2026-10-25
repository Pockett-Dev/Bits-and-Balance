package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
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
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ReflectiveLitBlockModelTesselationBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fabric-lit-tessellation");

    private static final String CLASS_MODEL_BLOCK_RENDERER = "net.minecraft.client.renderer.block.ModelBlockRenderer";
    private static final String CLASS_BLOCK_STATE_MODEL = "net.minecraft.client.renderer.block.dispatch.BlockStateModel";
    private static final String CLASS_BLOCK_QUAD_OUTPUT = "net.minecraft.client.renderer.block.BlockQuadOutput";
    private static final String CLASS_BLOCK_AND_TINT_GETTER = "net.minecraft.client.renderer.block.BlockAndTintGetter";
    private static final String CLASS_FABRIC_BLOCK_STATE_MODEL = "net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel";

    private static volatile Object renderer;
    private static volatile Constructor<?> rendererCtor3;
    private static volatile Constructor<?> rendererCtor1;
    private static volatile Method modernTesselateMethod;
    private static volatile Method legacyTesselateMethod;
    private static volatile Object emptyBlockAndTintGetter;
    private static volatile Class<?> blockStateModelClass;
    private static volatile Class<?> fabricBlockStateModelClass;
    private static volatile Class<?> blockQuadOutputClass;
    private static final int BLOCK_STATE_MODEL_PROXY_LIMIT = 8192;
    private static final Map<Class<?>, Method> PUT_BLOCK_BAKED_QUAD = new ConcurrentHashMap<>();
    private static final Map<List<?>, Object> BLOCK_STATE_MODEL_PROXIES = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<List<?>> BLOCK_STATE_MODEL_PROXY_KEYS = new ConcurrentLinkedQueue<>();
    private static final Map<Class<?>, Object> BLOCK_QUAD_OUTPUT_PROXIES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Vector3f> TRANSLATION = ThreadLocal.withInitial(Vector3f::new);
    private static final ThreadLocal<VertexConsumer> CURRENT_VERTEX_CONSUMER = new ThreadLocal<>();
    private static volatile boolean loggedFailure;

    private ReflectiveLitBlockModelTesselationBridge() {
    }

    public static void clearCaches() {
        BLOCK_STATE_MODEL_PROXIES.clear();
        BLOCK_STATE_MODEL_PROXY_KEYS.clear();
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
                CURRENT_VERTEX_CONSUMER.set(consumer);
                try {
                    modern.invoke(
                            modelBlockRenderer,
                            getBlockQuadOutputProxy(),
                            translation.x,
                            translation.y,
                            translation.z,
                            getBlockAndTintGetter(level),
                            blockPos,
                            sourceState,
                            createBlockStateModelProxy(parts),
                            blockPos.asLong()
                    );
                } finally {
                    CURRENT_VERTEX_CONSUMER.remove();
                }
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
        return pose.pose().getTranslation(TRANSLATION.get());
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

    public static BlockStateModel createBlockStateModelProxy(List<?> parts) throws ReflectiveOperationException {
        Object cached = BLOCK_STATE_MODEL_PROXIES.get(parts);
        if (cached != null) {
            return (BlockStateModel) cached;
        }

        Class<?> modelClass = blockStateModelClass;
        if (modelClass == null) {
            modelClass = Class.forName(CLASS_BLOCK_STATE_MODEL);
            blockStateModelClass = modelClass;
        }

        BlockStateModelPart firstPart = !parts.isEmpty() && parts.getFirst() instanceof BlockStateModelPart typedPart
                ? typedPart
                : null;

        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "collectParts" -> {
                @SuppressWarnings("unchecked")
                List<Object> out = (List<Object>) args[1];
                out.addAll(parts);
                yield null;
            }
            case "emitQuads" -> {
                Object emitter = args != null && args.length > 0 ? args[0] : null;
                Object cullTest = args != null && args.length > 5 ? args[5] : null;
                for (Object part : parts) {
                    if (part instanceof BlockStateModelPart blockStateModelPart) {
                        FabricRuntimeModelCompat.emitFabricPartQuads(blockStateModelPart, emitter, cullTest);
                    }
                }
                yield null;
            }
            case "createGeometryKey" -> parts;
            case "particleMaterial" -> firstPart != null ? firstPart.particleMaterial() : null;
            case "materialFlags" -> firstPart != null ? firstPart.materialFlags() : 0;
            case "hasMaterialFlag" -> {
                int flags = firstPart != null ? firstPart.materialFlags() : 0;
                Object lastArg = args != null && args.length > 0 ? args[args.length - 1] : null;
                int flag = lastArg instanceof Number number ? number.intValue() : 0;
                yield (flags & flag) != 0;
            }
            case "toString" -> "ReflectiveBlockStateModelProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
            default -> null;
        };

        Class<?> fabricModelClass = getFabricBlockStateModelClass();
        Class<?>[] interfaces = fabricModelClass != null && !fabricModelClass.isAssignableFrom(modelClass)
                ? new Class<?>[]{modelClass, fabricModelClass}
                : new Class<?>[]{modelClass};
        Object proxy = Proxy.newProxyInstance(
                ReflectiveLitBlockModelTesselationBridge.class.getClassLoader(),
                interfaces,
                handler
        );
        Object existing = BLOCK_STATE_MODEL_PROXIES.putIfAbsent(parts, proxy);
        if (existing != null) {
            return (BlockStateModel) existing;
        }
        BLOCK_STATE_MODEL_PROXY_KEYS.offer(parts);
        while (BLOCK_STATE_MODEL_PROXIES.size() > BLOCK_STATE_MODEL_PROXY_LIMIT) {
            List<?> eldest = BLOCK_STATE_MODEL_PROXY_KEYS.poll();
            if (eldest == null) {
                break;
            }
            BLOCK_STATE_MODEL_PROXIES.remove(eldest);
        }
        return (BlockStateModel) proxy;
    }

    private static Class<?> getFabricBlockStateModelClass() {
        Class<?> local = fabricBlockStateModelClass;
        if (local != null) {
            return local;
        }
        try {
            local = Class.forName(CLASS_FABRIC_BLOCK_STATE_MODEL);
            fabricBlockStateModelClass = local;
            return local;
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object getBlockQuadOutputProxy() throws ReflectiveOperationException {
        Class<?> outputClass = blockQuadOutputClass;
        if (outputClass == null) {
            outputClass = Class.forName(CLASS_BLOCK_QUAD_OUTPUT);
            blockQuadOutputClass = outputClass;
        }

        Object cached = BLOCK_QUAD_OUTPUT_PROXIES.get(outputClass);
        if (cached != null) {
            return cached;
        }

        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("put") && args != null && args.length == 5) {
                VertexConsumer consumer = CURRENT_VERTEX_CONSUMER.get();
                if (consumer == null) {
                    return null;
                }
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

        Object proxy = Proxy.newProxyInstance(
                ReflectiveLitBlockModelTesselationBridge.class.getClassLoader(),
                new Class<?>[]{outputClass},
                handler
        );
        Object existing = BLOCK_QUAD_OUTPUT_PROXIES.putIfAbsent(outputClass, proxy);
        return existing != null ? existing : proxy;
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
