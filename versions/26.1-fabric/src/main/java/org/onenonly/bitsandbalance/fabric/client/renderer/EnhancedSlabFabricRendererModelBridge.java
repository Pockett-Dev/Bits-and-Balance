package org.onenonly.bitsandbalance.fabric.client.renderer;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.client.EnhancedSlabTerrainModelOffset;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class EnhancedSlabFabricRendererModelBridge {
    private static final int EMITTER_METHOD_CACHE_LIMIT = 64;
    private static final int Y_OFFSET_TRANSFORM_CACHE_LIMIT = 512;
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fabric-enhanced-slab-models");
    private static final AtomicBoolean LOGGED_WRAPPED = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_EMITTED = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_VISUAL_OFFSET_EMITTED = new AtomicBoolean(false);
    private static volatile Class<?> fabricBlockStateModelClass;
    private static volatile Class<?> fabricQuadTransformClass;
    private static volatile Class<?> fabricMutableQuadViewClass;
    private static volatile Method mutableQuadXMethod;
    private static volatile Method mutableQuadYMethod;
    private static volatile Method mutableQuadZMethod;
    private static volatile Method mutableQuadPosMethod;
    private static final BoundedConcurrentCache<EmitterMethodsKey, EmitterMethods> EMITTER_METHODS =
            new BoundedConcurrentCache<>(EMITTER_METHOD_CACHE_LIMIT);
    private static final BoundedConcurrentCache<YOffsetTransformKey, Object> Y_OFFSET_TRANSFORMS =
            new BoundedConcurrentCache<>(Y_OFFSET_TRANSFORM_CACHE_LIMIT);
    private static final ThreadLocal<FallbackParts> FALLBACK_PARTS = ThreadLocal.withInitial(FallbackParts::new);

    private EnhancedSlabFabricRendererModelBridge() {
    }

    public static void clearCaches() {
        EMITTER_METHODS.clear();
        Y_OFFSET_TRANSFORMS.clear();
    }

    public static Object wrap(Object model, BlockState state) {
        if (!(model instanceof BlockStateModel blockStateModel) || state == null || !bitsandbalance$shouldWrap(state)) {
            return model;
        }

        Class<?> fabricModelClass = bitsandbalance$fabricBlockStateModelClass();
        Class<?>[] interfaces = fabricModelClass == null
                ? new Class<?>[]{BlockStateModel.class}
                : new Class<?>[]{BlockStateModel.class, fabricModelClass};

        InvocationHandler handler = new Handler(blockStateModel, state);
        Object proxy = Proxy.newProxyInstance(
                EnhancedSlabFabricRendererModelBridge.class.getClassLoader(),
                interfaces,
                handler
        );
        if (LOGGED_WRAPPED.compareAndSet(false, true)) {
            LOGGER.info("Wrapped enhanced slab-compatible baked model for Fabric renderer: {}", state.getBlock());
        }
        return proxy;
    }

    private static boolean bitsandbalance$shouldWrap(BlockState state) {
        return EnhancedSlabTerrainRenderHelper.handles(state)
                || (FabricTweaksConfig.enableEnhancedSlabs && EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(state));
    }

    private static Class<?> bitsandbalance$fabricBlockStateModelClass() {
        Class<?> local = fabricBlockStateModelClass;
        if (local != null) {
            return local;
        }
        try {
            local = Class.forName("net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel");
            fabricBlockStateModelClass = local;
            return local;
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Class<?> bitsandbalance$fabricQuadTransformClass() {
        Class<?> local = fabricQuadTransformClass;
        if (local != null) {
            return local;
        }
        local = bitsandbalance$classOrNull("net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform");
        if (local == null) {
            local = bitsandbalance$classOrNull("net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform");
        }
        if (local == null) {
            local = bitsandbalance$classOrNull("net.fabricmc.fabric.api.client.renderer.v1.render.RenderContext$QuadTransform");
        }
        if (local == null) {
            local = bitsandbalance$classOrNull("net.fabricmc.fabric.api.renderer.v1.render.RenderContext$QuadTransform");
        }
        fabricQuadTransformClass = local;
        return local;
    }

    private static Class<?> bitsandbalance$fabricMutableQuadViewClass() {
        Class<?> local = fabricMutableQuadViewClass;
        if (local != null) {
            return local;
        }
        local = bitsandbalance$classOrNull("net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView");
        if (local == null) {
            local = bitsandbalance$classOrNull("net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView");
        }
        fabricMutableQuadViewClass = local;
        return local;
    }

    private static Class<?> bitsandbalance$classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private record EnhancedGeometryKey(Object base, Object materials) {
    }

    private record Handler(BlockStateModel delegate, BlockState bakedState) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("collectParts".equals(name) && args != null && args.length == 2 && args[1] instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Object> parts = (List<Object>) list;
                if (EnhancedSlabTerrainRenderHelper.collectCreateModelParts(bakedState, parts)) {
                    return null;
                }
                if (EnhancedSlabTerrainRenderHelper.collectModelParts(bakedState, parts)) {
                    return null;
                }
                RandomSource random = args[0] instanceof RandomSource randomSource ? randomSource : RandomSource.create(0L);
                @SuppressWarnings({"rawtypes", "unchecked"})
                List<BlockStateModelPart> typedParts = (List) parts;
                delegate.collectParts(random, typedParts);
                return null;
            }

            if ("emitQuads".equals(name) && args != null && args.length == 6) {
                BlockPos pos = args[2] instanceof BlockPos blockPos ? blockPos : BlockPos.ZERO;
                BlockState state = args[3] instanceof BlockState blockState ? blockState : bakedState;
                if (EnhancedSlabTerrainRenderHelper.emitFabricRendererQuads(args[0], args[1], pos, state, args[5])) {
                    if (LOGGED_EMITTED.compareAndSet(false, true)) {
                        LOGGER.info("Emitted enhanced slab-family quads through Fabric renderer: {}", state.getBlock());
                    }
                    return null;
                }

                double yOff = bitsandbalance$visualYOffset(args, pos, state);
                if (yOff != 0.0D) {
                    if (bitsandbalance$emitDelegateQuadsWithYOffset(method, args, (float) yOff, true)) {
                        if (LOGGED_VISUAL_OFFSET_EMITTED.compareAndSet(false, true)) {
                            LOGGER.info("Emitted enhanced slab visual-offset quads through Fabric renderer: block={} yOff={}", state.getBlock(), yOff);
                        }
                        return null;
                    }
                }

                if (bitsandbalance$emitDelegateQuads(method, args, false)) {
                    return null;
                }
                return bitsandbalance$invokeDelegate(method, args);
            }

            if ("createGeometryKey".equals(name) && args != null && args.length >= 4) {
                Object base = bitsandbalance$invokeDelegate(method, args);
                BlockPos pos = args[1] instanceof BlockPos blockPos ? blockPos : BlockPos.ZERO;
                BlockState state = args[2] instanceof BlockState blockState ? blockState : bakedState;
                Object enhancedKey = EnhancedSlabTerrainRenderHelper.geometryKey(args[0], pos, state);
                Object key = enhancedKey == null ? base : new EnhancedGeometryKey(base, enhancedKey);
                double yOff = bitsandbalance$visualYOffset(args, pos, state);
                if (yOff == 0.0D) {
                    return key;
                }
                return new OffsetKey(key, yOff > 0.0D ? 1 : -1);
            }

            if ("toString".equals(name)) {
                return "EnhancedSlabFabricRendererModelBridge[" + bakedState.getBlock() + "]";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == (args != null && args.length > 0 ? args[0] : null);
            }

            return bitsandbalance$invokeDelegate(method, args);
        }

        private Object bitsandbalance$invokeDelegate(Method method, Object[] args) throws Throwable {
            if (!method.getDeclaringClass().isInstance(delegate)) {
                return bitsandbalance$defaultValue(method.getReturnType());
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            } catch (IllegalArgumentException exception) {
                return bitsandbalance$defaultValue(method.getReturnType());
            }
        }

        private double bitsandbalance$visualYOffset(Object[] args, BlockPos pos, BlockState state) {
            if (!FabricTweaksConfig.enableEnhancedSlabs || state == null || !EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(state)) {
                return 0.0D;
            }
            return args != null && args.length > 1 && args[1] instanceof BlockGetter blockGetter
                    ? EnhancedSlabHelper.getVisualYOffset(blockGetter, pos, state)
                    : 0.0D;
        }

        private boolean bitsandbalance$emitDelegateQuadsWithYOffset(Method method, Object[] args, float yOffset, boolean forceUnculled) throws Throwable {
            if (!method.getDeclaringClass().isInstance(delegate)) {
                return bitsandbalance$emitDelegateParts(args, yOffset, forceUnculled);
            }
            if (bitsandbalance$emitDelegateQuadsWithTransform(method, args, yOffset)) {
                return true;
            }
            return bitsandbalance$emitDelegateParts(args, yOffset, forceUnculled);
        }

        private boolean bitsandbalance$emitDelegateQuadsWithTransform(Method method, Object[] args, float yOffset) throws Throwable {
            Object emitter = args != null && args.length > 0 ? args[0] : null;
            if (emitter == null) {
                return false;
            }

            Class<?> transformClass = bitsandbalance$fabricQuadTransformClass();
            if (transformClass == null) {
                return false;
            }

            EmitterMethods emitterMethods = EMITTER_METHODS.getOrCompute(
                    new EmitterMethodsKey(emitter.getClass(), transformClass),
                    key -> bitsandbalance$findEmitterMethods(key.emitterClass(), key.transformClass())
            );
            Method pushTransform = emitterMethods.pushTransform();
            Method popTransform = emitterMethods.popTransform();
            if (pushTransform == null || popTransform == null) {
                return false;
            }

            Object transform = bitsandbalance$createYOffsetTransform(transformClass, yOffset);
            if (transform == null) {
                return false;
            }

            try {
                pushTransform.invoke(emitter, transform);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }

            try {
                return bitsandbalance$emitDelegateQuads(method, args, true);
            } finally {
                try {
                    popTransform.invoke(emitter);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }

        private Object bitsandbalance$createYOffsetTransform(Class<?> transformClass, float yOffset) {
            Class<?> mutableQuadViewClass = bitsandbalance$fabricMutableQuadViewClass();
            if (mutableQuadViewClass == null) {
                return null;
            }

            YOffsetTransformKey key = new YOffsetTransformKey(transformClass, Float.floatToIntBits(yOffset));
            return Y_OFFSET_TRANSFORMS.getOrCompute(key, ignored -> Proxy.newProxyInstance(
                    EnhancedSlabFabricRendererModelBridge.class.getClassLoader(),
                    new Class<?>[]{transformClass},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("transform".equals(name) && args != null && args.length == 1 && args[0] != null) {
                            Object quad = args[0];
                            Method xMethod = bitsandbalance$mutableQuadXMethod();
                            Method yMethod = bitsandbalance$mutableQuadYMethod();
                            Method zMethod = bitsandbalance$mutableQuadZMethod();
                            Method posMethod = bitsandbalance$mutableQuadPosMethod();
                            for (int index = 0; index < 4; index++) {
                                float x = ((Number) xMethod.invoke(quad, index)).floatValue();
                                float y = ((Number) yMethod.invoke(quad, index)).floatValue();
                                float z = ((Number) zMethod.invoke(quad, index)).floatValue();
                                posMethod.invoke(quad, index, x, y + yOffset, z);
                            }
                            return true;
                        }
                        if ("toString".equals(name)) {
                            return "EnhancedSlabVisualYOffsetTransform[" + yOffset + "]";
                        }
                        if ("hashCode".equals(name)) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(name)) {
                            return proxy == (args != null && args.length > 0 ? args[0] : null);
                        }
                        return bitsandbalance$defaultValue(method.getReturnType());
                    }
            ));
        }

        private Method bitsandbalance$mutableQuadXMethod() throws ReflectiveOperationException {
            Method local = mutableQuadXMethod;
            if (local == null) {
                local = bitsandbalance$fabricMutableQuadViewClass().getMethod("x", int.class);
                mutableQuadXMethod = local;
            }
            return local;
        }

        private Method bitsandbalance$mutableQuadYMethod() throws ReflectiveOperationException {
            Method local = mutableQuadYMethod;
            if (local == null) {
                local = bitsandbalance$fabricMutableQuadViewClass().getMethod("y", int.class);
                mutableQuadYMethod = local;
            }
            return local;
        }

        private Method bitsandbalance$mutableQuadZMethod() throws ReflectiveOperationException {
            Method local = mutableQuadZMethod;
            if (local == null) {
                local = bitsandbalance$fabricMutableQuadViewClass().getMethod("z", int.class);
                mutableQuadZMethod = local;
            }
            return local;
        }

        private Method bitsandbalance$mutableQuadPosMethod() throws ReflectiveOperationException {
            Method local = mutableQuadPosMethod;
            if (local == null) {
                local = bitsandbalance$fabricMutableQuadViewClass().getMethod("pos", int.class, float.class, float.class, float.class);
                mutableQuadPosMethod = local;
            }
            return local;
        }

        private static EmitterMethods bitsandbalance$findEmitterMethods(Class<?> owner, Class<?> transformClass) {
            return new EmitterMethods(
                    bitsandbalance$findMethod(owner, "pushTransform", 1, transformClass),
                    bitsandbalance$findMethod(owner, "popTransform", 0, null)
            );
        }

        private static Method bitsandbalance$findMethod(Class<?> owner, String name, int parameterCount, Class<?> requiredParameter) {
            for (Method method : owner.getMethods()) {
                if (!name.equals(method.getName()) || method.getParameterCount() != parameterCount) {
                    continue;
                }
                if (requiredParameter != null && !method.getParameterTypes()[0].isAssignableFrom(requiredParameter)) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                } catch (RuntimeException ignored) {
                }
                return method;
            }
            return null;
        }

        private boolean bitsandbalance$emitDelegateQuads(Method method, Object[] args, boolean requireDelegateMethod) throws Throwable {
            if (!method.getDeclaringClass().isInstance(delegate)) {
                return !requireDelegateMethod && bitsandbalance$emitDelegateParts(args, 0.0F, false);
            }
            try {
                method.invoke(delegate, args);
                return true;
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            } catch (IllegalArgumentException exception) {
                return !requireDelegateMethod && bitsandbalance$emitDelegateParts(args, 0.0F, false);
            }
        }

        private boolean bitsandbalance$emitDelegateParts(Object[] args, float yOffset, boolean forceUnculled) {
            Object emitter = args != null && args.length > 0 ? args[0] : null;
            if (emitter == null) {
                return false;
            }

            FallbackParts scratch = FALLBACK_PARTS.get();
            ArrayList<BlockStateModelPart> parts = scratch.acquire();
            RandomSource random = args.length > 4 && args[4] instanceof RandomSource randomSource
                    ? randomSource
                    : RandomSource.create(0L);
            try {
                delegate.collectParts(random, parts);
                if (parts.isEmpty()) {
                    return false;
                }

                if (yOffset != 0.0F) {
                    EnhancedSlabTerrainModelOffset.wrapPartsInPlace(parts, yOffset, forceUnculled);
                }

                boolean emitted = false;
                Object cullTest = args.length > 5 ? args[5] : null;
                for (BlockStateModelPart part : parts) {
                    emitted |= FabricRuntimeModelCompat.emitFabricPartQuads(part, emitter, cullTest);
                }
                return emitted;
            } finally {
                scratch.release(parts);
            }
        }

        private static Object bitsandbalance$defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == char.class) {
                return (char) 0;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0.0F;
            }
            if (type == double.class) {
                return 0.0D;
            }
            return null;
        }
    }

    private record OffsetKey(Object baseKey, int yOffSign) {
    }

    private record EmitterMethods(Method pushTransform, Method popTransform) {
    }

    private record EmitterMethodsKey(Class<?> emitterClass, Class<?> transformClass) {
    }

    private record YOffsetTransformKey(Class<?> transformClass, int yOffsetBits) {
    }

    private static final class BoundedConcurrentCache<K, V> {
        private final int limit;
        private final ConcurrentHashMap<K, V> values = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<K> insertionOrder = new ConcurrentLinkedQueue<>();

        private BoundedConcurrentCache(int limit) {
            this.limit = limit;
        }

        private V getOrCompute(K key, Function<K, V> factory) {
            V cached = values.get(key);
            if (cached != null) {
                return cached;
            }
            V created = factory.apply(key);
            V raced = values.putIfAbsent(key, created);
            if (raced != null) {
                return raced;
            }
            insertionOrder.offer(key);
            trim();
            return created;
        }

        private void clear() {
            values.clear();
            insertionOrder.clear();
        }

        private void trim() {
            while (values.size() > limit) {
                K eldest = insertionOrder.poll();
                if (eldest == null) {
                    return;
                }
                values.remove(eldest);
            }
        }
    }

    private static final class FallbackParts {
        private final ArrayList<BlockStateModelPart> reusable = new ArrayList<>();
        private boolean inUse;

        private ArrayList<BlockStateModelPart> acquire() {
            if (inUse) {
                return new ArrayList<>();
            }
            inUse = true;
            reusable.clear();
            return reusable;
        }

        private void release(ArrayList<BlockStateModelPart> parts) {
            parts.clear();
            if (parts == reusable) {
                inUse = false;
            }
        }
    }
}
