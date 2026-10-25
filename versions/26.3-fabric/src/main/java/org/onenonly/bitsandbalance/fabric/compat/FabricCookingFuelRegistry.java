package org.onenonly.bitsandbalance.fabric.compat;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;

import java.util.IdentityHashMap;
import java.util.Map;

/** 26.3 replacement for the removed Fabric FuelValueEvents API. */
public final class FabricCookingFuelRegistry {
    private static final Map<Item, Integer> BURN_TIMES = new IdentityHashMap<>();
    private static final Map<Item, DerivedFuel> DERIVED_FUELS = new IdentityHashMap<>();
    private static boolean initialized;

    private FabricCookingFuelRegistry() {
    }

    public static void register(Item item, int burnTime) {
        if (item == null || burnTime <= 0) {
            return;
        }
        initialize();
        BURN_TIMES.put(item, burnTime);
    }

    /**
     * Registers fuel whose eligibility follows a source slab. Source item components are not
     * bound during mod initialization in 26.3, so this relationship must be evaluated when
     * Fabric applies default item-component modifications.
     */
    public static void registerDerived(Item item, Block sourceSlab, int burnTime) {
        if (item == null || sourceSlab == null || burnTime <= 0) {
            return;
        }
        initialize();
        DERIVED_FUELS.put(item, new DerivedFuel(sourceSlab, burnTime));
    }

    private static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        DefaultItemComponentEvents.MODIFY.register(context -> BURN_TIMES.forEach((item, burnTime) ->
                context.modify(item, builder -> builder.set(
                        DataComponents.COOKING_FUEL,
                        new CookingFuel(new ResolvableInt.Constant(burnTime), new ResolvableFloat.Constant(1.0F))
                ))
        ));
        DefaultItemComponentEvents.MODIFY.register(context -> DERIVED_FUELS.forEach((item, derivedFuel) ->
                context.modify(item, builder -> {
                    if (DynamicVariantMaterialHelper.isFuelSourceSlab(derivedFuel.sourceSlab())) {
                        builder.set(
                                DataComponents.COOKING_FUEL,
                                new CookingFuel(
                                        new ResolvableInt.Constant(derivedFuel.burnTime()),
                                        new ResolvableFloat.Constant(1.0F)
                                )
                        );
                    }
                })
        ));
    }

    private record DerivedFuel(Block sourceSlab, int burnTime) {
    }
}
