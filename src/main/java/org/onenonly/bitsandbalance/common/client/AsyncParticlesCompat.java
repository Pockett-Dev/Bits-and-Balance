package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntSupplier;

/**
 * Gives custom block debris the same neighbor-light scope that AsyncParticles installs around
 * vanilla block destruction. Without it, particles centered inside a partial block can cache
 * light level zero for their entire lifetime.
 */
public final class AsyncParticlesCompat {
    private static final String[] PACKAGE_PREFIXES = {"", "fabric.", "neoforge."};
    private static final String PARTICLE_HELPER =
            "fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper";
    private static final String[] GAME_UTILS = {
            "fun.qu_an.minecraft.asyncparticles.client.core.GameUtil",
            "fun.qu_an.minecraft.asyncparticles.client.util.GameUtil"
    };

    private AsyncParticlesCompat() {
    }

    public static void withDestroyLight(ClientLevel level, BlockPos pos, Runnable action) {
        withDestroyLight(level, pos, () -> {
            action.run();
            return 0;
        });
    }

    public static int withDestroyLight(ClientLevel level, BlockPos pos, IntSupplier action) {
        Access access = Holder.ACCESS;
        if (access == null) {
            return action.getAsInt();
        }

        Object previous;
        try {
            previous = access.get.invoke(access.lightCache);
            int neighborLight = (Integer) access.getLightColorFromNeighbor.invoke(null, level, pos);
            access.set.invoke(access.lightCache, neighborLight);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return action.getAsInt();
        }

        try {
            return action.getAsInt();
        } finally {
            try {
                if (previous != null) {
                    access.set.invoke(access.lightCache, previous);
                } else {
                    access.remove.invoke(access.lightCache);
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
    }

    private static Access findAccess() {
        ClassLoader loader = AsyncParticlesCompat.class.getClassLoader();
        for (String prefix : PACKAGE_PREFIXES) {
            for (String gameUtilName : GAME_UTILS) {
                try {
                    Class<?> particleHelper = Class.forName(prefix + PARTICLE_HELPER, false, loader);
                    Class<?> gameUtil = Class.forName(prefix + gameUtilName, false, loader);
                    Field cacheField = particleHelper.getField("DESTRUCTION_LIGHT_CACHE");
                    Object lightCache = cacheField.get(null);
                    Class<?> cacheType = lightCache.getClass();
                    return new Access(
                            lightCache,
                            cacheType.getMethod("get"),
                            cacheType.getMethod("set", Object.class),
                            cacheType.getMethod("remove"),
                            gameUtil.getMethod("getLightColorFromNeighbor", ClientLevel.class, BlockPos.class)
                    );
                } catch (ReflectiveOperationException | LinkageError ignored) {
                }
            }
        }
        return null;
    }

    private static final class Holder {
        private static final Access ACCESS = findAccess();
    }

    private record Access(Object lightCache,
                          Method get,
                          Method set,
                          Method remove,
                          Method getLightColorFromNeighbor) {
    }
}
