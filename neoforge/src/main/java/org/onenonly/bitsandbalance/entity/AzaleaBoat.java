package org.onenonly.bitsandbalance.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;
import org.onenonly.bitsandbalance.ModItems;
import org.onenonly.bitsandbalance.registry.ModEntityTypes;

/**
 * Custom boat entity for azalea wood.
 * Behaves like vanilla boats but uses custom textures and drops azalea boat item.
 */
public class AzaleaBoat extends Boat {

    public AzaleaBoat(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level, ModItems.AZALEA_BOAT::get);
    }
    
    public AzaleaBoat(Level level, double x, double y, double z) {
        this(ModEntityTypes.AZALEA_BOAT.get(), level);
        this.setInitialPos(x, y, z);
    }
}
