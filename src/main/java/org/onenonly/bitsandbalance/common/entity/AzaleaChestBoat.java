package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
//? if mc == 1.21.1
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
//? else
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
//? endif
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModAzalea;
//? if mc == 1.21.1
//? if neoforge
import org.onenonly.bitsandbalance.registry.ModEntityTypes;
//? endif
//? endif

public class AzaleaChestBoat extends ChestBoat {
    //? if mc == 1.21.1
    public AzaleaChestBoat(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }
    //? else
    public AzaleaChestBoat(EntityType<? extends ChestBoat> entityType, Level level) {
        super(entityType, level, () -> BuiltInRegistries.ITEM
            .get(ModAzalea.AZALEA_CHEST_BOAT_ID)
            .map(holder -> holder.value())
            .orElse(Items.AIR));
    }
    //? endif

    public AzaleaChestBoat(Level level, double x, double y, double z) {
        //? if mc == 1.21.1
        //? if neoforge
        this(ModEntityTypes.AZALEA_CHEST_BOAT.get(), level);
        //? else
        this(ModAzalea.AZALEA_CHEST_BOAT_ENTITY_TYPE, level);
        //? endif
        this.setPos(x, y, z);
        //? else
        this(ModAzalea.AZALEA_CHEST_BOAT_ENTITY_TYPE, level);
        this.setInitialPos(x, y, z);
        //? endif
    }

    //? if mc == 1.21.1
    @Override
    public Item getDropItem() {
        Item item = BuiltInRegistries.ITEM.get(ModAzalea.AZALEA_CHEST_BOAT_ID);
        return item != null ? item : Items.AIR;
    }
    //? endif

    @Override
    public Component getName() {
        return Component.translatable("entity." + BitsAndBalanceCommon.MOD_ID + ".azalea_chest_boat");
    }
}
