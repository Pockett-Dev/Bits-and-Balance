package org.onenonly.bitsandbalance.fabric.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//? if mc != 1.21.1
import net.minecraft.core.registries.Registries;
//? endif
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
//? if mc != 1.21.1
import net.minecraft.resources.ResourceKey;
//? endif
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.onenonly.bitsandbalance.common.entity.SeatEntity;
import org.onenonly.bitsandbalance.fabric.BitsAndBalanceFabric;

public final class FabricEntityTypes {
    private FabricEntityTypes() {
    }

    //? if mc == 1.21.1
    public static final ResourceLocation SEAT_ID = ResourceLocation.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "seat");
    //? else
    public static final Identifier SEAT_ID = Identifier.fromNamespaceAndPath(BitsAndBalanceFabric.MOD_ID, "seat");
    //? endif

    public static final EntityType<SeatEntity> SEAT = EntityType.Builder
            .<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
            .sized(0.0F, 0.0F)
            .clientTrackingRange(10)
            //? if mc == 1.21.1
            .build(SEAT_ID.toString());
            //? else
            .build(ResourceKey.create(Registries.ENTITY_TYPE, SEAT_ID));
            //? endif

    public static void register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SEAT_ID, SEAT);
    }
}
