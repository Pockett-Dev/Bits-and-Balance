package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.worldgen.feature.VeinConfiguration;
import org.onenonly.bitsandbalance.worldgen.feature.VeinFeature;
import org.onenonly.bitsandbalance.worldgen.feature.MojangStyleVeinConfiguration;
import org.onenonly.bitsandbalance.worldgen.feature.MojangStyleVeinFeature;

public final class ModFeatures {
    private ModFeatures() {}

    private static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, BitsAndBalance.MODID);

    public static final DeferredHolder<Feature<?>, VeinFeature> NOODLE_VEIN =
        FEATURES.register("noodle_vein", () -> new VeinFeature(VeinConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, MojangStyleVeinFeature> MOJANG_STYLE_VEIN =
        FEATURES.register("mojang_style_vein", () -> new MojangStyleVeinFeature(MojangStyleVeinConfiguration.CODEC));

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
