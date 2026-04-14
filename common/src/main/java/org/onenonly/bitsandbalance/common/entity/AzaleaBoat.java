package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModAzalea;

public class AzaleaBoat extends Boat {
    public AzaleaBoat(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level, () -> BuiltInRegistries.ITEM
            .get(ModAzalea.AZALEA_BOAT_ID)
            .map(holder -> holder.value())
            .orElse(Items.AIR));
    }

    public AzaleaBoat(Level level, double x, double y, double z) {
        this(ModAzalea.AZALEA_BOAT_ENTITY_TYPE, level);
        this.setInitialPos(x, y, z);
    }

    @Override
    public Component getName() {
        return Component.translatable("entity." + BitsAndBalanceCommon.MOD_ID + ".azalea_boat");
    }
}
