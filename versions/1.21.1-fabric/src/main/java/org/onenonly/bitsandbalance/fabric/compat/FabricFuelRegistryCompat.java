package org.onenonly.bitsandbalance.fabric.compat;

import net.minecraft.world.item.Item;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Consumer;

public final class FabricFuelRegistryCompat {
    @FunctionalInterface
    public interface Registrar {
        void add(Item item, int burnTime);
    }

    private FabricFuelRegistryCompat() {
    }

    public static void register(Consumer<Registrar> callback) {
        try {
            if (tryRegisterWithEvents(callback)) {
                return;
            }

            if (tryRegisterWithLegacyRegistry(callback)) {
                return;
            }

            throw new IllegalStateException("Fabric fuel registry API is unavailable on this node");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Fabric fuel registry API is unavailable on this node", exception);
        }
    }

    private static boolean tryRegisterWithEvents(Consumer<Registrar> callback) throws ReflectiveOperationException {
        try {
            Class<?> fuelRegistryEventsClass = Class.forName("net.fabricmc.fabric.api.registry.FuelRegistryEvents");
            Object buildEvent = fuelRegistryEventsClass.getField("BUILD").get(null);
            Class<?> buildCallbackClass = Class.forName("net.fabricmc.fabric.api.registry.FuelRegistryEvents$BuildCallback");

            Object listener = Proxy.newProxyInstance(
                buildCallbackClass.getClassLoader(),
                new Class<?>[]{buildCallbackClass},
                (proxy, method, args) -> {
                    if ("build".equals(method.getName()) && args != null && args.length >= 1) {
                        Object builder = args[0];
                        callback.accept((item, burnTime) -> invokeBuilderAdd(builder, item, burnTime));
                    }
                    return null;
                }
            );

            findSingleArgumentMethod(buildEvent.getClass(), "register").invoke(buildEvent, listener);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static boolean tryRegisterWithLegacyRegistry(Consumer<Registrar> callback) throws ReflectiveOperationException {
        try {
            Class<?> fuelRegistryClass = Class.forName("net.fabricmc.fabric.api.registry.FuelRegistry");
            Object registry = fuelRegistryClass.getField("INSTANCE").get(null);

            callback.accept((item, burnTime) -> invokeBuilderAdd(registry, item, burnTime));
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static void invokeBuilderAdd(Object builder, Item item, int burnTime) {
        try {
            for (Method method : builder.getClass().getMethods()) {
                if (!"add".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean burnTimeMatches = parameterTypes[1] == int.class || parameterTypes[1] == Integer.class;
                if (!burnTimeMatches || !parameterTypes[0].isAssignableFrom(item.getClass())) {
                    continue;
                }

                method.invoke(builder, item, burnTime);
                return;
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Fabric fuel value for item " + item, exception);
        }

        throw new IllegalStateException("No compatible Fabric fuel builder add(Item, int) method was found");
    }

    private static Method findSingleArgumentMethod(Class<?> owner, String name) {
        return Arrays.stream(owner.getMethods())
            .filter(method -> name.equals(method.getName()) && method.getParameterCount() == 1)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing method " + name + " on " + owner.getName()));
    }
}