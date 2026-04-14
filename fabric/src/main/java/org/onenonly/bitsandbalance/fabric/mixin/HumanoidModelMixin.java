package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.onenonly.bitsandbalance.fabric.client.FabricClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client: apply sitting pose to player models.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends HumanoidRenderState> {

    @Shadow
    public ModelPart rightLeg;

    @Shadow
    public ModelPart leftLeg;

    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("HEAD"))
    private void bitsandbalance$markRidingForPose(T state, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        int entityId = bitsandbalance$tryGetEntityId(state);
        if (entityId < 0) return;

        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof Player player)) return;

        boolean isLocal = mc.player != null && mc.player.getUUID().equals(player.getUUID());
        boolean sitting = (isLocal && FabricClientState.sitting)
            || FabricClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
        if (!sitting) return;
        if (player.isPassenger()) return;

        // In 1.21.x the model pose is commonly driven by render-state flags rather than a model field.
        // We set a riding/passenger-like flag reflectively to trigger vanilla pose logic.
        boolean forced = bitsandbalance$trySetRidingLikeFlag(state);
        bitsandbalance$forcedVanillaRidingPose = forced;
    }

    @Unique
    private boolean bitsandbalance$forcedVanillaRidingPose = false;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void bitsandbalance$applyVisualSitting(T state, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        int entityId = bitsandbalance$tryGetEntityId(state);
        if (entityId < 0) return;

        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof Player player)) return;

        boolean isLocal = mc.player != null && mc.player.getUUID().equals(player.getUUID());
        boolean sitting = (isLocal && FabricClientState.sitting)
            || FabricClientState.SYNCED_SITTING_PLAYERS.contains(player.getUUID());
        if (!sitting) return;
        if (player.isPassenger()) return;

        // If we successfully triggered vanilla riding/passenger pose logic, don't re-apply angles.
        if (bitsandbalance$forcedVanillaRidingPose) {
            bitsandbalance$forcedVanillaRidingPose = false;
            return;
        }

        try {
            float xr = -1.4137167F; // ~-81 degrees
            float yr = 0.31415927F; // ~18 degrees
            float zr = 0.07853982F; // ~4.5 degrees

            // Apply vanilla riding pose without mounting.
            // (Vanilla typically *adds* this to the current arm pose.)
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

    private static int bitsandbalance$tryGetEntityId(HumanoidRenderState state) {
        if (state == null) return -1;

        try {
            // Yarn/MC tends to expose an int entity id on render states.
            // We resolve it reflectively to avoid hard dependency on a specific subclass.
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

    private static boolean bitsandbalance$trySetRidingLikeFlag(HumanoidRenderState state) {
        if (state == null) return false;

        // Candidate field names seen across mappings/versions.
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
