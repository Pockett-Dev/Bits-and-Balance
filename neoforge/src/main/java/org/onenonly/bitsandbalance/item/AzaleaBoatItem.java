package org.onenonly.bitsandbalance.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.entity.AzaleaBoat;
import org.onenonly.bitsandbalance.entity.AzaleaChestBoat;

import java.util.List;
import java.util.function.Predicate;

/**
 * Custom boat item for azalea wood.
 * Spawns custom azalea boat entities with proper textures and behavior.
 */
public class AzaleaBoatItem extends Item {
    
    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
    private final boolean hasChest;
    
    public AzaleaBoatItem(boolean hasChest, Item.Properties properties) {
        super(properties);
        this.hasChest = hasChest;
    }
    
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        
        Vec3 viewVector = player.getViewVector(1.0F);
        List<Entity> nearbyEntities = level.getEntities(
            player, 
            player.getBoundingBox().expandTowards(viewVector.scale(5.0)).inflate(1.0), 
            ENTITY_PREDICATE
        );
        
        if (!nearbyEntities.isEmpty()) {
            Vec3 eyePosition = player.getEyePosition();
            for (Entity entity : nearbyEntities) {
                AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (aabb.contains(eyePosition)) {
                    return InteractionResult.PASS;
                }
            }
        }
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Create custom azalea boat entities
            AbstractBoat boat = this.hasChest 
                ? new AzaleaChestBoat(level, hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z)
                : new AzaleaBoat(level, hitResult.getLocation().x, hitResult.getLocation().y, hitResult.getLocation().z);
            
            // Don't set variant - custom renderer handles texture
            boat.setYRot(player.getYRot());
            
            if (!level.noCollision(boat, boat.getBoundingBox())) {
                return InteractionResult.FAIL;
            }
            
            if (!level.isClientSide()) {
                level.addFreshEntity(boat);
                level.gameEvent(player, GameEvent.ENTITY_PLACE, hitResult.getLocation());
                itemStack.consume(1, player);
            }
            
            player.awardStat(Stats.ITEM_USED.get(this));
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }
}

