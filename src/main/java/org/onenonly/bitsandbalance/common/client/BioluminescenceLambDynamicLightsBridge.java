package org.onenonly.bitsandbalance.common.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class BioluminescenceLambDynamicLightsBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-bioluminescence-lambdynlights");
    private static final String LAMB_DYN_LIGHTS_CLASS = "dev.lambdaurora.lambdynlights.LambDynLights";
    private static final String DYNAMIC_LIGHT_BEHAVIOR_CLASS = "dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior";
    private static final String DYNAMIC_LIGHT_BOUNDING_BOX_CLASS = "dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior$BoundingBox";
    private static final int REGISTRATION_RETRY_TICKS = 40;

    private static final Map<Integer, RegisteredSourceBehavior> REGISTERED_SOURCE_BEHAVIORS = new HashMap<>();

    private static boolean initialized;
    private static boolean warningLogged;
    private static long nextRegistrationAttemptTick;
    private static Object behaviorManager;
    private static Class<?> behaviorClass;
    private static Method addBehaviorMethod;
    private static Method removeBehaviorMethod;
    private static Constructor<?> boundingBoxConstructor;

    private BioluminescenceLambDynamicLightsBridge() {
    }

    public static void initializeFromLambDynamicLights(Object lambDynamicLights) {
        if (lambDynamicLights == null || initialized) {
            return;
        }

        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            behaviorClass = Class.forName(DYNAMIC_LIGHT_BEHAVIOR_CLASS, false, contextLoader);
            Class<?> boundingBoxClass = Class.forName(DYNAMIC_LIGHT_BOUNDING_BOX_CLASS, false, contextLoader);
            boundingBoxConstructor = boundingBoxClass.getConstructor(
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class
            );

            behaviorManager = lambDynamicLights.getClass().getMethod("dynamicLightBehaviorManager").invoke(lambDynamicLights);
            addBehaviorMethod = behaviorManager.getClass().getMethod("add", behaviorClass);
            removeBehaviorMethod = behaviorManager.getClass().getMethod("remove", behaviorClass);
            initialized = true;
            warningLogged = false;
            LOGGER.info("Connected Bits and Balance bioluminescence to LambDynamicLights dynamic behavior manager.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            initialized = false;
            if (!warningLogged) {
                warningLogged = true;
                LOGGER.warn("Could not connect Bits and Balance bioluminescence to LambDynamicLights.", exception);
            }
        }
    }

    static void tick() {
        if (!isLambDynamicLightsLoaded()) {
            resetRegistration();
            return;
        }
        if (!initialized) {
            tryInitializeFromLambSingleton();
        }
        if (!initialized) {
            return;
        }

        syncSourceBehaviors();
    }

    public static void syncNow() {
        tick();
    }

    static void resetRegistration() {
        for (RegisteredSourceBehavior registeredSource : REGISTERED_SOURCE_BEHAVIORS.values()) {
            removeSourceBehavior(registeredSource);
        }
        REGISTERED_SOURCE_BEHAVIORS.clear();
        initialized = false;
        behaviorManager = null;
        behaviorClass = null;
        addBehaviorMethod = null;
        removeBehaviorMethod = null;
        boundingBoxConstructor = null;
        nextRegistrationAttemptTick = 0L;
    }

    private static void tryInitializeFromLambSingleton() {
        long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
        if (gameTime < nextRegistrationAttemptTick) {
            return;
        }

        try {
            Class<?> lambDynLightsClass = Class.forName(LAMB_DYN_LIGHTS_CLASS, false, Thread.currentThread().getContextClassLoader());
            initializeFromLambDynamicLights(lambDynLightsClass.getMethod("get").invoke(null));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            nextRegistrationAttemptTick = gameTime + REGISTRATION_RETRY_TICKS;
            if (!warningLogged) {
                warningLogged = true;
                LOGGER.warn("Could not find LambDynamicLights singleton for Bits and Balance bioluminescence.", exception);
            }
        }
    }

    private static void syncSourceBehaviors() {
        Set<Integer> activeIds = new HashSet<>();
        for (BioluminescenceClientLightManager.LightSourceSnapshot snapshot : BioluminescenceClientLightManager.getActiveSourceSnapshots()) {
            activeIds.add(snapshot.entityId());
            RegisteredSourceBehavior registeredSource = REGISTERED_SOURCE_BEHAVIORS.get(snapshot.entityId());
            if (registeredSource == null) {
                registerSourceBehavior(snapshot);
            } else {
                updateSourceBehavior(registeredSource, snapshot);
            }
        }

        Iterator<Map.Entry<Integer, RegisteredSourceBehavior>> iterator = REGISTERED_SOURCE_BEHAVIORS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, RegisteredSourceBehavior> entry = iterator.next();
            if (activeIds.contains(entry.getKey())) {
                continue;
            }

            removeSourceBehavior(entry.getValue());
            iterator.remove();
        }
    }

    private static void registerSourceBehavior(BioluminescenceClientLightManager.LightSourceSnapshot snapshot) {
        try {
            SourceBehaviorHandler handler = new SourceBehaviorHandler(snapshot);
            Object proxy = Proxy.newProxyInstance(
                    behaviorClass.getClassLoader(),
                    new Class<?>[]{behaviorClass},
                    handler
            );
            addBehaviorMethod.invoke(behaviorManager, proxy);
            REGISTERED_SOURCE_BEHAVIORS.put(snapshot.entityId(), new RegisteredSourceBehavior(proxy, handler));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (!warningLogged) {
                warningLogged = true;
                LOGGER.warn("Could not register a Bits and Balance bioluminescence source with LambDynamicLights.", exception);
            }
        }
    }

    private static void updateSourceBehavior(RegisteredSourceBehavior registeredSource,
                                             BioluminescenceClientLightManager.LightSourceSnapshot snapshot) {
        try {
            registeredSource.handler.update(snapshot);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (!warningLogged) {
                warningLogged = true;
                LOGGER.warn("Could not update a Bits and Balance bioluminescence source for LambDynamicLights.", exception);
            }
        }
    }

    private static void removeSourceBehavior(RegisteredSourceBehavior registeredSource) {
        registeredSource.handler.remove();
        if (behaviorManager == null || removeBehaviorMethod == null) {
            return;
        }
        try {
            removeBehaviorMethod.invoke(behaviorManager, registeredSource.proxy);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (!warningLogged) {
                warningLogged = true;
                LOGGER.warn("Could not remove a Bits and Balance bioluminescence source from LambDynamicLights.", exception);
            }
        }
    }

    private static Object createBoundingBox(BioluminescenceClientLightManager.ActiveLightBounds bounds) throws ReflectiveOperationException {
        Constructor<?> constructor = boundingBoxConstructor;
        if (constructor == null) {
            throw new IllegalStateException("LambDynamicLights bounding box constructor has not been resolved.");
        }
        return constructor.newInstance(
                bounds.minX(),
                bounds.minY(),
                bounds.minZ(),
                bounds.maxX(),
                bounds.maxY(),
                bounds.maxZ()
        );
    }

    private static boolean isLambDynamicLightsLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded("lambdynlights");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private record RegisteredSourceBehavior(Object proxy, SourceBehaviorHandler handler) {
    }

    private static final class SourceBehaviorHandler implements InvocationHandler {
        private BioluminescenceClientLightManager.LightSourceSnapshot snapshot;
        private BioluminescenceClientLightManager.ActiveLightBounds bounds;
        private Object boundingBox;
        private long observedRevision = Long.MIN_VALUE;
        private boolean removed;

        private SourceBehaviorHandler(BioluminescenceClientLightManager.LightSourceSnapshot snapshot) throws ReflectiveOperationException {
            update(snapshot);
        }

        private void update(BioluminescenceClientLightManager.LightSourceSnapshot snapshot) throws ReflectiveOperationException {
            BioluminescenceClientLightManager.ActiveLightBounds nextBounds = snapshot.bounds();
            Object nextBoundingBox = this.bounds != null && this.bounds.equals(nextBounds)
                    ? this.boundingBox
                    : createBoundingBox(nextBounds);
            this.snapshot = snapshot;
            this.bounds = nextBounds;
            this.boundingBox = nextBoundingBox;
        }

        private void remove() {
            this.removed = true;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "lightAtPos" -> lightAtPos(args);
                case "getBoundingBox" -> this.boundingBox;
                case "hasChanged" -> hasChanged();
                case "isRemoved" -> this.removed;
                case "toString" -> "BitsAndBalanceBioluminescenceLambDynamicLightBehavior[" + this.snapshot.entityId() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> args != null && args.length == 1 && proxy == args[0];
                default -> throw new UnsupportedOperationException("Unsupported LambDynamicLights behavior method: " + method);
            };
        }

        private double lightAtPos(Object[] args) {
            if (this.removed || args == null || args.length == 0 || !(args[0] instanceof BlockPos pos)) {
                return 0.0D;
            }
            double falloffRatio = args.length > 1 && args[1] instanceof Number number ? number.doubleValue() : 1.0D;
            return this.snapshot.lightAt(pos, falloffRatio);
        }

        private boolean hasChanged() {
            if (this.removed) {
                return true;
            }

            long revision = this.snapshot.revision();
            if (revision == this.observedRevision) {
                return false;
            }
            this.observedRevision = revision;
            return true;
        }
    }
}
