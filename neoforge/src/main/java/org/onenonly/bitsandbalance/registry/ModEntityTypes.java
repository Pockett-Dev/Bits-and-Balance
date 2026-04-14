package org.onenonly.bitsandbalance.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.entity.AzaleaBoat;
import org.onenonly.bitsandbalance.entity.AzaleaChestBoat;

/**
 * Registers custom entity types for the mod.
 */
public class ModEntityTypes {
    
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, BitsAndBalance.MODID);
    
    // Azalea boat entity
    public static final DeferredHolder<EntityType<?>, EntityType<AzaleaBoat>> AZALEA_BOAT =
        ENTITY_TYPES.register("azalea_boat", () -> EntityType.Builder.<AzaleaBoat>of(AzaleaBoat::new, MobCategory.MISC)
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "azalea_boat"))));
    
    // Azalea chest boat entity
    public static final DeferredHolder<EntityType<?>, EntityType<AzaleaChestBoat>> AZALEA_CHEST_BOAT =
        ENTITY_TYPES.register("azalea_chest_boat", () -> EntityType.Builder.<AzaleaChestBoat>of(AzaleaChestBoat::new, MobCategory.MISC)
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "azalea_chest_boat"))));

    /**
     * Registers the entity types to the mod event bus.
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        BitsAndBalance.LOGGER.info("Registered Azalea boat entity types");
    }
}


