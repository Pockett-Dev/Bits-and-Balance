package org.onenonly.bitsandbalance.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

/**
 * Client-only visual adjustment: scales the perceived per-tick speed of the eat/drink
 * hand animation so it matches the actual use duration for foods/drinks. This is
 * purely cosmetic and does not affect use duration or gameplay timing.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public final class ConsumeAnimationHandler {
    private ConsumeAnimationHandler() {}

    // WIP: consume animation scaling temporarily disabled so users don't experience it
    private static final boolean WIP_DISABLE_CONSUME_ANIM = true;

    // Track a short client-side pre-use window so bobbing starts immediately when right-click is held
    private static long preUseStartTick = -1L;
    private static net.minecraft.world.item.Item preUseItem = null;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (WIP_DISABLE_CONSUME_ANIM) return; // WIP guard: disable feature for end users
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            var player = mc.player;

            boolean using = false;
            try { using = player != null && player.isUsingItem(); } catch (Throwable ignored) {}

            // Resolve current candidate stack/anim: when not using yet, prefer main-hand, then off-hand
            ItemStack stack = ItemStack.EMPTY;
            ItemUseAnimation anim = ItemUseAnimation.NONE;
            if (using && player != null) {
                stack = player.getUseItem();
            } else if (player != null) {
                try { stack = player.getMainHandItem(); } catch (Throwable ignored) {}
                if (stack == null || stack.isEmpty()) {
                    try { stack = player.getOffhandItem(); } catch (Throwable ignored2) {}
                }
            }
            if (stack == null || stack.isEmpty()) {
                // No item to animate; reset pre-use tracking
                preUseStartTick = -1L; preUseItem = null;
                return;
            }

            try { anim = stack.getUseAnimation(); } catch (Throwable ignored) { anim = ItemUseAnimation.NONE; }
            boolean isConsumable = (anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK);

            // Detect use key; only consider pre-use when key is held and item is consumable
            boolean useDown = false;
            try { useDown = mc.options.keyUse.isDown(); } catch (Throwable ignored) {}

            if (!using) {
                if (!useDown || !isConsumable) {
                    // Not in pre-use state; reset and bail
                    preUseStartTick = -1L; preUseItem = null; return;
                }
                // Avoid pre-use bobbing when eating is not allowed (e.g., full hunger bar)
                if (anim == ItemUseAnimation.EAT && player instanceof net.minecraft.world.entity.player.Player pp) {
                    boolean canEat = true;
                    try { canEat = pp.canEat(false); } catch (Throwable ignored) {}
                    if (!canEat) { preUseStartTick = -1L; preUseItem = null; return; }
                }
                // Start or continue pre-use tracking
                net.minecraft.world.item.Item it = stack.getItem();
                long now = mc.level == null ? 0L : mc.level.getGameTime();
                if (preUseStartTick < 0L || preUseItem != it) {
                    preUseStartTick = now;
                    preUseItem = it;
                }
            } else {
                // Using has begun; clear pre-use
                preUseStartTick = -1L; preUseItem = null;
                if (!isConsumable) return;
            }

            if (!isConsumable) return;

            // Use vanilla duration
            int adjustedDuration = 0;
            try {
                if (player != null) {
                    adjustedDuration = stack.getUseDuration(player);
                }
            } catch (Throwable t) {
                adjustedDuration = 0;
            }
            if (adjustedDuration <= 0) return;

            float partial = 0.0f;
            try { partial = event.getPartialTick(); } catch (Throwable ignored) {}

            float usedTicks;
            if (using && player != null) {
                int remaining = 0;
                try { remaining = player.getUseItemRemainingTicks(); } catch (Throwable ignored) {}
                usedTicks = Math.max(0.0f, (float) adjustedDuration - ((float) remaining - partial));
            } else {
                long now = mc.level == null ? 0L : mc.level.getGameTime();
                float elapsed = (float) Math.max(0L, now - Math.max(0L, preUseStartTick));
                usedTicks = elapsed + partial; // start immediately
            }

            // Compute a base and scaled phase (radians). Vanilla chew pace is effectively constant per tick.
            // We scale phase so the perceived frequency fits entirely within the adjusted duration.
            float phaseFactor = 0.9f; // subtle effect scaling

            // Baseline (vanilla-like) phase grows ~1 per tick
            float basePhase = usedTicks * phaseFactor;

            // Target cycles tuned for vanilla ~32-tick food; scale to fit adjusted duration
            double coreMult = 32.0D / Math.max(1.0D, adjustedDuration);
            double finalMult = coreMult;

            float scaledPhase = (float) (usedTicks * finalMult) * phaseFactor;

            float sinScaled = (float) Math.sin(scaledPhase);
            float cosScaled = (float) Math.cos(scaledPhase);
            float sinBase = (float) Math.sin(basePhase);
            float cosBase = (float) Math.cos(basePhase);

            float blendSin;
            float blendCos;
            if (!using) {
                // During pre-use, apply additive motion immediately
                blendSin = sinScaled;
                blendCos = cosScaled;
            } else {
                // Blend from additive (full effect from tick 0) to delta (avoid double bob once vanilla kicks in)
                // Over ~6 ticks after a short 2-tick grace, fade to delta.
                float fade = Math.max(0.0f, Math.min(1.0f, (usedTicks - 2.0f) / 6.0f));
                float addSin = sinScaled;
                float addCos = cosScaled;
                float deltaSin = sinScaled - sinBase;
                float deltaCos = cosScaled - cosBase;
                blendSin = (1.0f - fade) * addSin + fade * deltaSin;
                blendCos = (1.0f - fade) * addCos + fade * deltaCos;
            }

            var pose = event.getPoseStack();

            // Subtle additive transforms to replace vanilla bobbing rate with duration-scaled one.
            float intensity;
            if (!using) {
                // During pre-use, show a vanilla-like amplitude so animation is visible immediately
                intensity = 1.0f;
            } else {
                // While actually using, apply full intensity so the delta cancels vanilla and replaces it
                // with the scaled motion. This ensures the bobbing speed matches the consume duration.
                intensity = 1.0f;
            }
            float ampY = 0.10f * intensity;     // vertical bob amplitude (blocks)
            float ampXRot = 10.0f * intensity;  // degrees forward/back tilt
            float ampZRot = 12.0f * intensity;  // degrees side sway

            pose.translate(0.0F, -blendSin * ampY, 0.0F);
            pose.mulPose(Axis.XP.rotationDegrees(blendSin * ampXRot));
            pose.mulPose(Axis.ZP.rotationDegrees(blendCos * ampZRot));
        } catch (Throwable ignored) {
        }
    }
}
