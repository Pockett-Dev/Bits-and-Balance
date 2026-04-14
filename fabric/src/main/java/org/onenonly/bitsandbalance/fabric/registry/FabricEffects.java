package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.effects.BioluminescenceEffect;
import org.onenonly.bitsandbalance.fabric.effects.DisplacementEffect;
import org.onenonly.bitsandbalance.fabric.effects.ResurfacingEffect;
import org.onenonly.bitsandbalance.fabric.effects.ReturningEffect;

public final class FabricEffects {
    private FabricEffects() {
    }

    public static final MobEffect RESURFACING = new ResurfacingEffect();
    public static final MobEffect DISPLACEMENT = new DisplacementEffect();
    public static final MobEffect RETURNING = new ReturningEffect();
    public static final MobEffect BIOLUMINESCENCE = new BioluminescenceEffect();

    public static void register() {
        Registry.register(BuiltInRegistries.MOB_EFFECT, id("resurfacing"), RESURFACING);
        Registry.register(BuiltInRegistries.MOB_EFFECT, id("displacement"), DISPLACEMENT);
        Registry.register(BuiltInRegistries.MOB_EFFECT, id("returning"), RETURNING);
        Registry.register(BuiltInRegistries.MOB_EFFECT, id("bioluminescence"), BIOLUMINESCENCE);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, path);
    }
}
