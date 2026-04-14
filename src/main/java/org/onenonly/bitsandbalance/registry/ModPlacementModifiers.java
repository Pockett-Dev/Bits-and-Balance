package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.worldgen.placement.AirExposureFilter;
import org.onenonly.bitsandbalance.worldgen.placement.ConfigEnabledFilter;

public final class ModPlacementModifiers {
    private ModPlacementModifiers() {}

    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
        DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, BitsAndBalance.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ConfigEnabledFilter>> CONFIG_ENABLED =
        PLACEMENT_MODIFIERS.register("config_enabled", () -> () -> ConfigEnabledFilter.CODEC);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<AirExposureFilter>> AIR_EXPOSURE_FILTER =
        PLACEMENT_MODIFIERS.register("air_exposure_filter", () -> () -> AirExposureFilter.CODEC);

    public static void register(IEventBus modEventBus) {
        PLACEMENT_MODIFIERS.register(modEventBus);
    }
}
