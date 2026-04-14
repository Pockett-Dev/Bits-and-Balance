package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;
import org.onenonly.bitsandbalance.fabric.entity.SeatEntity;

public final class FabricEntityTypes {
    private FabricEntityTypes() {
    }

    public static final Identifier SEAT_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "seat");

    public static final EntityType<SeatEntity> SEAT = EntityType.Builder
            .<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
            .sized(0.0F, 0.0F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, SEAT_ID));

    public static void register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SEAT_ID, SEAT);
    }
}
