package org.onenonly.bitsandbalance.fabric.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricAxiomInfiniteReachCompat {

    private static final double RANGE_EPSILON = 1.0E-4D;
    private static final int REASON_INFINITE_REACH = 64;

    private static volatile boolean bitsandbalance$resolved;
    private static @Nullable Method bitsandbalance$isSpecialPlacingMethod;
    private static @Nullable Constructor<?> bitsandbalance$setBlockPacketConstructor;
    private static @Nullable Method bitsandbalance$setBlockPacketSendMethod;
    private static @Nullable Method bitsandbalance$getPredictionHandlerMethod;
    private static @Nullable Method bitsandbalance$currentSequenceMethod;

    private FabricAxiomInfiniteReachCompat() {
    }


    public static void maybeSendStepPlacementPacket(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null || level == null || hand == null || hitResult == null || !level.isClientSide()) {
            return;
        }

        bitsandbalance$resolve();
        if (bitsandbalance$isSpecialPlacingMethod == null
                || bitsandbalance$setBlockPacketConstructor == null
                || bitsandbalance$setBlockPacketSendMethod == null) {
            return;
        }

        if (!bitsandbalance$isInfiniteReachSpecialPlacement(player, level, hitResult)) {
            return;
        }

        try {
            int sequence = bitsandbalance$currentPredictionSequence(level);
            Object packet = bitsandbalance$setBlockPacketConstructor.newInstance(
                    new LinkedHashMap<>(),
                    true,
                    REASON_INFINITE_REACH,
                    false,
                    hitResult,
                    hand,
                    sequence
            );
            bitsandbalance$setBlockPacketSendMethod.invoke(packet);
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$isInfiniteReachSpecialPlacement(Player player, Level level, BlockHitResult hitResult) {
        try {
            Object active = bitsandbalance$isSpecialPlacingMethod.invoke(null, (BlockGetter) level);
            if (!(active instanceof Boolean isActive) || !isActive) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }

        return hitResult.getLocation().distanceTo(player.getEyePosition()) > player.blockInteractionRange() + RANGE_EPSILON;
    }

    private static int bitsandbalance$currentPredictionSequence(Level level) {
        Method getPredictionHandler = bitsandbalance$getPredictionHandlerMethod;
        Method currentSequence = bitsandbalance$currentSequenceMethod;
        if (getPredictionHandler == null || currentSequence == null) {
            return -1;
        }

        try {
            Object handler = getPredictionHandler.invoke(level);
            if (handler == null) {
                return -1;
            }
            Object value = currentSequence.invoke(handler);
            return value instanceof Number number ? number.intValue() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static void bitsandbalance$resolve() {
        if (bitsandbalance$resolved) return;

        synchronized (FabricAxiomInfiniteReachCompat.class) {
            if (bitsandbalance$resolved) return;

            try {
                Class<?> specialPlaceClass = Class.forName("com.moulberry.axiom.capabilities.SpecialPlace");
                bitsandbalance$isSpecialPlacingMethod = specialPlaceClass.getMethod("isSpecialPlacing", BlockGetter.class);

                Class<?> packetClass = Class.forName("com.moulberry.axiom.packets.AxiomServerboundSetBlock");
                bitsandbalance$setBlockPacketConstructor = packetClass.getConstructor(
                        Map.class,
                        boolean.class,
                        int.class,
                        boolean.class,
                        BlockHitResult.class,
                        InteractionHand.class,
                        int.class
                );
                bitsandbalance$setBlockPacketSendMethod = packetClass.getMethod("send");

                bitsandbalance$getPredictionHandlerMethod = levelMethod("net.minecraft.client.multiplayer.ClientLevel", "getBlockStatePredictionHandler");
                if (bitsandbalance$getPredictionHandlerMethod != null) {
                    Class<?> handlerClass = bitsandbalance$getPredictionHandlerMethod.getReturnType();
                    bitsandbalance$currentSequenceMethod = handlerClass.getMethod("currentSequence");
                }
            } catch (Throwable ignored) {
                bitsandbalance$isSpecialPlacingMethod = null;
                bitsandbalance$setBlockPacketConstructor = null;
                bitsandbalance$setBlockPacketSendMethod = null;
                bitsandbalance$getPredictionHandlerMethod = null;
                bitsandbalance$currentSequenceMethod = null;
            }

            bitsandbalance$resolved = true;
        }
    }

    private static @Nullable Method levelMethod(String className, String methodName) {
        try {
            return Class.forName(className).getMethod(methodName);
        } catch (Throwable ignored) {
            return null;
        }
    }
}