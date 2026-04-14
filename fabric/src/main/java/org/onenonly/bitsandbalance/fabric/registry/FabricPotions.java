package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;

public final class FabricPotions {
    private FabricPotions() {
    }

    public static final Potion WITHERING = new Potion("withering", new MobEffectInstance(MobEffects.WITHER, 22 * 20, 0));
    public static final Potion LONG_WITHERING = new Potion("long_withering", new MobEffectInstance(MobEffects.WITHER, 1 * 60 * 20, 0));
    public static final Potion STRONG_WITHERING = new Potion("strong_withering", new MobEffectInstance(MobEffects.WITHER, 11 * 20, 1));

    public static final Potion RESURFACING = new Potion("resurfacing", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FabricEffects.RESURFACING), 1, 0));
    public static final Potion DISPLACEMENT = new Potion("displacement", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FabricEffects.DISPLACEMENT), 1, 0));
    public static final Potion RETURNING = new Potion("returning", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FabricEffects.RETURNING), 1, 0));

    public static final Potion LEVITATION = new Potion("levitation", new MobEffectInstance(MobEffects.LEVITATION, 30 * 20, 0));
    public static final Potion LONG_LEVITATION = new Potion("long_levitation", new MobEffectInstance(MobEffects.LEVITATION, 1 * 60 * 20, 0));
    public static final Potion STRONG_LEVITATION = new Potion("strong_levitation", new MobEffectInstance(MobEffects.LEVITATION, 20 * 20, 1));

    public static final Potion HASTE = new Potion("haste", new MobEffectInstance(MobEffects.HASTE, 2 * 60 * 20, 0));
    public static final Potion LONG_HASTE = new Potion("long_haste", new MobEffectInstance(MobEffects.HASTE, 4 * 60 * 20, 0));
    public static final Potion STRONG_HASTE = new Potion("strong_haste", new MobEffectInstance(MobEffects.HASTE, 1 * 60 * 20, 1));

    public static final Potion GLOWING = new Potion("glowing", new MobEffectInstance(MobEffects.GLOWING, 4 * 60 * 20, 0));
    public static final Potion LONG_GLOWING = new Potion("long_glowing", new MobEffectInstance(MobEffects.GLOWING, 8 * 60 * 20, 0));

    public static final Potion BIOLUMINESCENCE = new Potion("bioluminescence", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FabricEffects.BIOLUMINESCENCE), 4 * 60 * 20, 0));
    public static final Potion STRONG_BIOLUMINESCENCE = new Potion("strong_bioluminescence", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FabricEffects.BIOLUMINESCENCE), 2 * 60 * 20, 1));
    public static final Potion LONG_STRONG_BIOLUMINESCENCE = new Potion("long_strong_bioluminescence", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FabricEffects.BIOLUMINESCENCE), 4 * 60 * 20, 1));
    public static final Potion LONG_BIOLUMINESCENCE = new Potion("long_bioluminescence", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(FabricEffects.BIOLUMINESCENCE), 8 * 60 * 20, 0));

    public static void register() {
        if (FabricPotionConfig.enableWitheringPotion) {
            Registry.register(BuiltInRegistries.POTION, id("withering"), WITHERING);
            Registry.register(BuiltInRegistries.POTION, id("long_withering"), LONG_WITHERING);
            Registry.register(BuiltInRegistries.POTION, id("strong_withering"), STRONG_WITHERING);
        }

        if (FabricPotionConfig.enableResurfacingPotion) {
            Registry.register(BuiltInRegistries.POTION, id("resurfacing"), RESURFACING);
        }
        if (FabricPotionConfig.enableDisplacementPotion) {
            Registry.register(BuiltInRegistries.POTION, id("displacement"), DISPLACEMENT);
        }
        if (FabricPotionConfig.enableReturningPotion) {
            Registry.register(BuiltInRegistries.POTION, id("returning"), RETURNING);
        }

        if (FabricPotionConfig.enableLevitationPotion) {
            Registry.register(BuiltInRegistries.POTION, id("levitation"), LEVITATION);
            Registry.register(BuiltInRegistries.POTION, id("long_levitation"), LONG_LEVITATION);
            Registry.register(BuiltInRegistries.POTION, id("strong_levitation"), STRONG_LEVITATION);
        }

        if (FabricPotionConfig.enableHastePotion) {
            Registry.register(BuiltInRegistries.POTION, id("haste"), HASTE);
            Registry.register(BuiltInRegistries.POTION, id("long_haste"), LONG_HASTE);
            Registry.register(BuiltInRegistries.POTION, id("strong_haste"), STRONG_HASTE);
        }

        if (FabricPotionConfig.enableGlowingPotion) {
            Registry.register(BuiltInRegistries.POTION, id("glowing"), GLOWING);
            Registry.register(BuiltInRegistries.POTION, id("long_glowing"), LONG_GLOWING);
        }

        if (FabricPotionConfig.enableBioluminescencePotion) {
            Registry.register(BuiltInRegistries.POTION, id("bioluminescence"), BIOLUMINESCENCE);
            Registry.register(BuiltInRegistries.POTION, id("strong_bioluminescence"), STRONG_BIOLUMINESCENCE);
            Registry.register(BuiltInRegistries.POTION, id("long_strong_bioluminescence"), LONG_STRONG_BIOLUMINESCENCE);
            Registry.register(BuiltInRegistries.POTION, id("long_bioluminescence"), LONG_BIOLUMINESCENCE);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }
}
