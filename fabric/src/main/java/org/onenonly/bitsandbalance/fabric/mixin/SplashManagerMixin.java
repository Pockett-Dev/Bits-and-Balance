package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Splash Text: Add custom splash texts to the vanilla splash pool.
 */
@Mixin(SplashManager.class)
public abstract class SplashManagerMixin {

    private static final List<String> CUSTOM_SPLASH_TEXTS = List.of(
            "Mark. Look, I made a Steak.",
            "A Steak?!",
            "Are you sure?",
            "Pretty sure...",
            "Threw a trashbag... into space...",
            "WHERE IS OMNI-MAN?",
            "Fish-slap",
            "It was me, DIO!",
            "Oh, you're approaching me?",
            "Yo, Angelo.",
            "Dirty Deeds Done Dirt Cheap",
            "D4C!",
            "Goodbye Jojo!",
            "Muda muda muda!",
            "WRYYYYYY!",
            "Za Warudo!",
            "Kono Dio da!",
            "Yare yare daze",
            "Nani?!",
            "This is the way.",
            "I am inevitable.",
            "Perfectly balanced, as all things should be.",
            "I am Groot!",
            "Dojyaaan!",
            "That is the taste of a liar!"
    );

    @Inject(method = "prepare", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$addCustomSplashes(
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfoReturnable<List<Component>> cir
    ) {
        if (!FabricClientConfig.enableCustomSplashTexts) {
            return;
        }

        List<Component> base = cir.getReturnValue();
        if (base == null) {
            return;
        }

        int multiplier = FabricClientConfig.customSplashMultiplier;
        if (multiplier < 1) multiplier = 1;
        if (multiplier > 10) multiplier = 10;

        ArrayList<Component> combined = new ArrayList<>(base.size() + (CUSTOM_SPLASH_TEXTS.size() * multiplier));
        combined.addAll(base);
        for (int i = 0; i < multiplier; i++) {
            for (String splash : CUSTOM_SPLASH_TEXTS) {
				// Vanilla splashes render in yellow; explicitly style custom splashes to match.
                combined.add(Component.literal(splash).withStyle(ChatFormatting.YELLOW));
            }
        }

        cir.setReturnValue(combined);
    }
}
