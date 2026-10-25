package org.onenonly.bitsandbalance.compat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;

public final class CreateEnhancedSlabMovementCompat {
    private static final String CHECKS_CLASS = "com.simibubi.create.api.contraption.BlockMovementChecks";
    private static final String CHECK_RESULT_CLASS = CHECKS_CLASS + "$CheckResult";
    private static boolean registered;

    private CreateEnhancedSlabMovementCompat() {
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static synchronized void registerMovementChecks() {
        if (registered) {
            return;
        }

        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> checksClass = Class.forName(CHECKS_CLASS, false, loader);
            Class<?> resultClass = Class.forName(CHECK_RESULT_CLASS, false, loader);
            Object success = Enum.valueOf(resultClass.asSubclass(Enum.class), "SUCCESS");
            Object fail = Enum.valueOf(resultClass.asSubclass(Enum.class), "FAIL");
            Object pass = Enum.valueOf(resultClass.asSubclass(Enum.class), "PASS");

            registerCheck(loader, checksClass, "MovementNecessaryCheck", "registerMovementNecessaryCheck", success, pass,
                    (args) -> args != null && args.length == 3 && args[0] instanceof BlockState state
                            && args[1] instanceof Level && args[2] instanceof BlockPos
                            && EnhancedSlabCreateCompat.supportsMovement(state));
            registerCheck(loader, checksClass, "MovementAllowedCheck", "registerMovementAllowedCheck", success, pass,
                    (args) -> args != null && args.length == 3 && args[0] instanceof BlockState state
                            && args[1] instanceof Level && args[2] instanceof BlockPos
                            && EnhancedSlabCreateCompat.supportsMovement(state));
            registerCheck(loader, checksClass, "AttachedCheck", "registerAttachedCheck", success, pass,
                    (args) -> args != null && args.length == 4 && args[0] instanceof BlockState state
                            && args[1] instanceof Level level && args[2] instanceof BlockPos pos && args[3] instanceof Direction direction
                            && EnhancedSlabCreateCompat.supportsMovement(state)
                            && EnhancedSlabCreateCompat.isAttachedTowards(state, level, pos, direction));

            registered = true;
            BitsAndBalance.LOGGER.info("[{}] Registered Create movement checks for enhanced slab-family blocks", BitsAndBalance.MODID);
        } catch (ClassNotFoundException ignored) {
            // Create is optional.
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            BitsAndBalance.LOGGER.warn("[{}] Failed to register Create movement checks for enhanced slab-family blocks: {}",
                    BitsAndBalance.MODID, e.toString());
        }
    }

    private static void registerCheck(ClassLoader loader,
                                      Class<?> checksClass,
                                      String nestedInterfaceName,
                                      String registerMethodName,
                                      Object handledResult,
                                      Object passResult,
                                      CheckPredicate predicate) throws ReflectiveOperationException {
        Class<?> checkInterface = Class.forName(CHECKS_CLASS + "$" + nestedInterfaceName, false, loader);
        Object proxy = Proxy.newProxyInstance(
                loader,
                new Class<?>[] { checkInterface },
                new CreateCheckHandler(handledResult, passResult, predicate)
        );
        Method registerMethod = checksClass.getMethod(registerMethodName, checkInterface);
        registerMethod.invoke(null, proxy);
    }

    private interface CheckPredicate {
        boolean test(Object[] args);
    }

    private record CreateCheckHandler(Object handledResult, Object passResult, CheckPredicate predicate) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            return predicate.test(args) ? handledResult : passResult;
        }
    }
}
