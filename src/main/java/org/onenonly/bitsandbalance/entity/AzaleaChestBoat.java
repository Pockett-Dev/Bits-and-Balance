package org.onenonly.bitsandbalance.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.ModItems;
import org.onenonly.bitsandbalance.registry.ModEntityTypes;

/**
 * Custom chest boat entity for azalea wood.
 * Behaves like vanilla chest boats but uses custom textures and drops azalea chest boat item.
 */
public class AzaleaChestBoat extends ChestBoat {

    public AzaleaChestBoat(EntityType<? extends ChestBoat> entityType, Level level) {
        super(entityType, level, ModItems.AZALEA_CHEST_BOAT::get);
    }
    
    public AzaleaChestBoat(Level level, double x, double y, double z) {
        this(ModEntityTypes.AZALEA_CHEST_BOAT.get(), level);
        this.setInitialPos(x, y, z);
    }
}
