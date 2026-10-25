package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.client.ClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge client: in 1.21.x the player model pose is primarily driven by render-state flags.
 *
 * When we want a player to visually "sit" without actually mounting a vehicle, we emulate the
 * riding/passenger flag on the HumanoidRenderState so vanilla pose logic kicks in.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelRenderStateSittingMixin<T extends HumanoidRenderState> {

    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Unique
    private boolean bitsandbalance$forcedVanillaRidingPose = false;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("HEAD"))
    private void bitsandbalance$markRidingForPose(T state, CallbackInfo ci) {
        try {
            if (!bitsandbalance$hasAnySittingPlayers()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return;

            int entityId = bitsandbalance$tryGetEntityId(state);
            if (entityId < 0) return;

            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof Player player)) return;

            boolean isLocal = mc.player != null && mc.player.getUUID().equals(player.getUUID());
            boolean sitting = (isLocal && ClientState.sitting)
                    || ClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
            if (!sitting) return;
            if (player.isPassenger()) return;

            boolean forced = bitsandbalance$trySetRidingLikeFlag(state);
            bitsandbalance$forcedVanillaRidingPose = forced;
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void bitsandbalance$applyVisualSitting(T state, CallbackInfo ci) {
        try {
            if (!bitsandbalance$hasAnySittingPlayers()) {
                bitsandbalance$forcedVanillaRidingPose = false;
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return;

            int entityId = bitsandbalance$tryGetEntityId(state);
            if (entityId < 0) return;

            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof Player player)) return;

            boolean isLocal = mc.player != null && mc.player.getUUID().equals(player.getUUID());
            boolean sitting = (isLocal && ClientState.sitting)
                    || ClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
            if (!sitting) return;
            if (player.isPassenger()) return;

            // If we successfully triggered vanilla riding/passenger pose logic, don't re-apply angles.
            if (bitsandbalance$forcedVanillaRidingPose) {
                bitsandbalance$forcedVanillaRidingPose = false;
                return;
            }

            float xr = -1.4137167F; // ~-81 degrees
            float yr = 0.31415927F; // ~18 degrees
            float zr = 0.07853982F; // ~4.5 degrees

            // Approximate vanilla riding pose without mounting.
            this.rightArm.xRot += -0.62831855F;
            this.leftArm.xRot += -0.62831855F;

            this.rightLeg.xRot = xr;
            this.rightLeg.yRot = yr;
            this.rightLeg.zRot = zr;

            this.leftLeg.xRot = xr;
            this.leftLeg.yRot = -yr;
            this.leftLeg.zRot = -zr;
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static final Map<Class<?>, Field> bitsandbalance$entityIdFields = new ConcurrentHashMap<>();

    @Unique
    private static final Set<Class<?>> bitsandbalance$entityIdMissing = ConcurrentHashMap.newKeySet();

    @Unique
    private static final Map<Class<?>, Field> bitsandbalance$ridingFlagFields = new ConcurrentHashMap<>();

    @Unique
    private static final Set<Class<?>> bitsandbalance$ridingFlagMissing = ConcurrentHashMap.newKeySet();

    @Unique
    private static boolean bitsandbalance$hasAnySittingPlayers() {
        return ClientState.sitting || !ClientState.SYNCED_SITTING_PLAYERS.isEmpty();
    }

    @Unique
    private static int bitsandbalance$tryGetEntityId(HumanoidRenderState state) {
        if (state == null) return -1;

        Class<?> type = state.getClass();
        Field cached = bitsandbalance$entityIdFields.get(type);
        if (cached != null) {
            try {
                return cached.getInt(state);
            } catch (Throwable ignored) {
                bitsandbalance$entityIdFields.remove(type, cached);
            }
        }
        if (bitsandbalance$entityIdMissing.contains(type)) return -1;

        try {
            Field field = bitsandbalance$findField(type, int.class, "id", "entityId");
            if (field != null) {
                bitsandbalance$entityIdFields.put(type, field);
                return field.getInt(state);
            }
        } catch (Throwable ignored) {
        }

        bitsandbalance$entityIdMissing.add(type);
        return -1;
    }

    @Unique
    private static boolean bitsandbalance$trySetRidingLikeFlag(HumanoidRenderState state) {
        if (state == null) return false;

        Class<?> type = state.getClass();
        Field cached = bitsandbalance$ridingFlagFields.get(type);
        if (cached != null) {
            try {
                cached.setBoolean(state, true);
                return true;
            } catch (Throwable ignored) {
                bitsandbalance$ridingFlagFields.remove(type, cached);
            }
        }
        if (bitsandbalance$ridingFlagMissing.contains(type)) return false;

        try {
            Field field = bitsandbalance$findField(type, boolean.class,
                    "isPassenger", "passenger", "riding", "isRiding", "isMounted", "mounted");
            if (field != null) {
                bitsandbalance$ridingFlagFields.put(type, field);
                field.setBoolean(state, true);
                return true;
            }
        } catch (Throwable ignored) {
        }

        bitsandbalance$ridingFlagMissing.add(type);
        return false;
    }

    @Unique
    private static Field bitsandbalance$findField(Class<?> type, Class<?> expectedType, String... names) {
        for (String name : names) {
            try {
                Field field = type.getField(name);
                if (field.getType() == expectedType) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return null;
            }
        }

        for (String name : names) {
            try {
                Field field = type.getDeclaredField(name);
                if (field.getType() == expectedType) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }
}
