package org.onenonly.bitsandbalance.fabric.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;
import org.onenonly.bitsandbalance.fabric.config.FabricWorldgenConfig;

public final class FabricDogMusicDiscDrops {
    private FabricDogMusicDiscDrops() {
    }

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (FabricWorldgenConfig.isDogMusicDiscEnabled()) {
                return;
            }
            if (!(entity instanceof Creeper creeper)) {
                return;
            }
            if (!(damageSource.getEntity() instanceof AbstractSkeleton)) {
                return;
            }
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            AABB searchBox = creeper.getBoundingBox().inflate(2.0D);
            serverLevel.getServer().execute(() -> replaceNearbyDroppedDogDiscs(serverLevel, searchBox));
        });
    }

    private static void replaceNearbyDroppedDogDiscs(ServerLevel serverLevel, AABB searchBox) {
        Item dogDisc = ModDogMusicDisc.musicDiscDog();
        if (dogDisc == Items.AIR) {
            return;
        }

        for (ItemEntity itemEntity : serverLevel.getEntitiesOfClass(ItemEntity.class, searchBox, entity -> entity.isAlive() && entity.getItem().is(dogDisc))) {
            Item replacement = ModDogMusicDisc.randomReplacementCreeperDropDisc(serverLevel.getRandom());
            if (replacement == Items.AIR) {
                itemEntity.discard();
                continue;
            }

            ItemStack stack = itemEntity.getItem();
            itemEntity.setItem(new ItemStack(replacement, stack.getCount()));
        }
    }
}