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
    private static int bitsandbalance$tryGetEntityId(HumanoidRenderState state) {
        if (state == null) return -1;

        try {
            try {
                var f = state.getClass().getField("id");
                Object v = f.get(state);
                if (v instanceof Integer i) return i;
            } catch (NoSuchFieldException ignored) {
            }

            try {
                var f = state.getClass().getField("entityId");
                Object v = f.get(state);
                if (v instanceof Integer i) return i;
            } catch (NoSuchFieldException ignored) {
            }

            try {
                var f = state.getClass().getDeclaredField("id");
                f.setAccessible(true);
                return f.getInt(state);
            } catch (NoSuchFieldException ignored) {
            }

            try {
                var f = state.getClass().getDeclaredField("entityId");
                f.setAccessible(true);
                return f.getInt(state);
            } catch (NoSuchFieldException ignored) {
            }
        } catch (Throwable ignored) {
        }

        return -1;
    }

    @Unique
    private static boolean bitsandbalance$trySetRidingLikeFlag(HumanoidRenderState state) {
        if (state == null) return false;

        String[] names = new String[] {
                "isPassenger",
                "passenger",
                "riding",
                "isRiding",
                "isMounted",
                "mounted"
        };

        for (String name : names) {
            try {
                var f = state.getClass().getField(name);
                if (f.getType() == boolean.class) {
                    f.setBoolean(state, true);
                    return true;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return false;
            }

            try {
                var f = state.getClass().getDeclaredField(name);
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    f.setBoolean(state, true);
                    return true;
                }
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return false;
            }
        }

        return false;
    }
}
