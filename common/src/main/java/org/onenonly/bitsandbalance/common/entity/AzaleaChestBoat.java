package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.content.ModAzalea;

public class AzaleaChestBoat extends ChestBoat {
    public AzaleaChestBoat(EntityType<? extends ChestBoat> entityType, Level level) {
        super(entityType, level, () -> BuiltInRegistries.ITEM
            .get(ModAzalea.AZALEA_CHEST_BOAT_ID)
            .map(holder -> holder.value())
            .orElse(Items.AIR));
    }

    public AzaleaChestBoat(Level level, double x, double y, double z) {
        this(ModAzalea.AZALEA_CHEST_BOAT_ENTITY_TYPE, level);
        this.setInitialPos(x, y, z);
    }

    @Override
    public Component getName() {
        return Component.translatable("entity." + BitsAndBalanceCommon.MOD_ID + ".azalea_chest_boat");
    }
}
