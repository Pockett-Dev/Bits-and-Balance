package org.onenonly.bitsandbalance.events;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.content.ModDogMusicDisc;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class DogMusicDiscDrops {
    private DogMusicDiscDrops() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (Config.enableDogMusicDisc) {
            return;
        }
        if (!(event.getEntity() instanceof Creeper creeper)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof AbstractSkeleton)) {
            return;
        }

        Item dogDisc = ModDogMusicDisc.musicDiscDog();
        if (dogDisc == Items.AIR) {
            return;
        }

        int removedCount = 0;
        var iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            ItemStack stack = itemEntity.getItem();
            if (stack.is(dogDisc)) {
                removedCount += stack.getCount();
                iterator.remove();
            }
        }

        if (removedCount <= 0) {
            return;
        }

        Item replacement = ModDogMusicDisc.randomReplacementCreeperDropDisc(creeper.getRandom());
        if (replacement == Items.AIR) {
            return;
        }

        event.getDrops().add(new ItemEntity(creeper.level(), creeper.getX(), creeper.getY(), creeper.getZ(), new ItemStack(replacement, removedCount)));
    }
}