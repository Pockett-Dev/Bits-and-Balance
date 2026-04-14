package org.onenonly.bitsandbalance;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom potions for the Rebalance mod.
 */
public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, BitsAndBalance.MODID);

    // Wither I for 22 seconds
    public static final DeferredHolder<Potion, Potion> WITHERING = POTIONS.register("withering",
            () -> new Potion("withering", new MobEffectInstance(MobEffects.WITHER, 22 * 20, 0)));

    // Long Wither I for 1 minute (extended variant)
    public static final DeferredHolder<Potion, Potion> LONG_WITHERING = POTIONS.register("long_withering",
            () -> new Potion("long_withering", new MobEffectInstance(MobEffects.WITHER, 1 * 60 * 20, 0)));

    // Wither II for 11 seconds (strong variant)
    public static final DeferredHolder<Potion, Potion> STRONG_WITHERING = POTIONS.register("strong_withering",
            () -> new Potion("strong_withering", new MobEffectInstance(MobEffects.WITHER, 11 * 20, 1)));

    // Resurfacing - instant teleportation to surface
    public static final DeferredHolder<Potion, Potion> RESURFACING = POTIONS.register("resurfacing",
            () -> new Potion("resurfacing", new MobEffectInstance(ModEffects.RESURFACING, 1, 0)));

    // Displacement - instant random teleportation within 100-block radius
    public static final DeferredHolder<Potion, Potion> DISPLACEMENT = POTIONS.register("displacement",
            () -> new Potion("displacement", new MobEffectInstance(ModEffects.DISPLACEMENT, 1, 0)));

    // Returning - instant teleportation to latest death location
    public static final DeferredHolder<Potion, Potion> RETURNING = POTIONS.register("returning",
            () -> new Potion("returning", new MobEffectInstance(ModEffects.RETURNING, 1, 0)));

    // Levitation I for 30 seconds
    public static final DeferredHolder<Potion, Potion> LEVITATION = POTIONS.register("levitation",
            () -> new Potion("levitation", new MobEffectInstance(MobEffects.LEVITATION, 30 * 20, 0)));

    // Long Levitation I for 1 minute
    public static final DeferredHolder<Potion, Potion> LONG_LEVITATION = POTIONS.register("long_levitation",
            () -> new Potion("long_levitation", new MobEffectInstance(MobEffects.LEVITATION, 1 * 60 * 20, 0)));

    // Strong Levitation II for 20 seconds
    public static final DeferredHolder<Potion, Potion> STRONG_LEVITATION = POTIONS.register("strong_levitation",
            () -> new Potion("strong_levitation", new MobEffectInstance(MobEffects.LEVITATION, 20 * 20, 1)));

    // Haste I for 2 minutes (25% of 8 minutes)
    public static final DeferredHolder<Potion, Potion> HASTE = POTIONS.register("haste",
            () -> new Potion("haste", new MobEffectInstance(MobEffects.HASTE, 2 * 60 * 20, 0)));

    // Long Haste I for 4 minutes (25% of 16 minutes)
    public static final DeferredHolder<Potion, Potion> LONG_HASTE = POTIONS.register("long_haste",
            () -> new Potion("long_haste", new MobEffectInstance(MobEffects.HASTE, 4 * 60 * 20, 0)));

    // Strong Haste II for 1 minute (25% of 4 minutes)
    public static final DeferredHolder<Potion, Potion> STRONG_HASTE = POTIONS.register("strong_haste",
            () -> new Potion("strong_haste", new MobEffectInstance(MobEffects.HASTE, 1 * 60 * 20, 1)));

    public static final DeferredHolder<Potion, Potion> GLOWING = POTIONS.register("glowing",
            () -> new Potion("glowing", new MobEffectInstance(MobEffects.GLOWING, 4 * 60 * 20, 0)));

    public static final DeferredHolder<Potion, Potion> LONG_GLOWING = POTIONS.register("long_glowing",
            () -> new Potion("long_glowing", new MobEffectInstance(MobEffects.GLOWING, 8 * 60 * 20, 0)));

    public static final DeferredHolder<Potion, Potion> BIOLUMINESCENCE = POTIONS.register("bioluminescence",
            () -> new Potion("bioluminescence", new MobEffectInstance(ModEffects.BIOLUMINESCENCE, 4 * 60 * 20, 0)));

    public static final DeferredHolder<Potion, Potion> STRONG_BIOLUMINESCENCE = POTIONS.register("strong_bioluminescence",
            () -> new Potion("strong_bioluminescence", new MobEffectInstance(ModEffects.BIOLUMINESCENCE, 2 * 60 * 20, 1)));

    public static final DeferredHolder<Potion, Potion> LONG_STRONG_BIOLUMINESCENCE = POTIONS.register("long_strong_bioluminescence",
            () -> new Potion("long_strong_bioluminescence", new MobEffectInstance(ModEffects.BIOLUMINESCENCE, 4 * 60 * 20, 1)));

    public static final DeferredHolder<Potion, Potion> LONG_BIOLUMINESCENCE = POTIONS.register("long_bioluminescence",
            () -> new Potion("long_bioluminescence", new MobEffectInstance(ModEffects.BIOLUMINESCENCE, 8 * 60 * 20, 0)));

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }
}
