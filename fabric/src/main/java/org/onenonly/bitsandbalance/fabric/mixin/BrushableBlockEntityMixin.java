package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Tweaks: Brushing XP Rewards
 */
@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin {

    @Unique
    private boolean bitsandbalance$xpGiven;

    @Inject(
            method = "brushingCompleted(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("TAIL")
    )
    private void bitsandbalance$brushingCompleted(ServerLevel level, LivingEntity brusher, ItemStack stack, CallbackInfo ci) {
        if (!FabricTweaksConfig.brushingXpEnabled || bitsandbalance$xpGiven) {
            return;
        }

        int xp = computeRandomXp();
        if (xp <= 0) {
            return;
        }

        BrushableBlockEntity self = (BrushableBlockEntity) (Object) this;
        ExperienceOrb.award(level, Vec3.atCenterOf(self.getBlockPos()), xp);
        bitsandbalance$xpGiven = true;
    }

    private static int computeRandomXp() {
        int min = Math.max(0, FabricTweaksConfig.brushingXpMin);
        int max = Math.max(min, FabricTweaksConfig.brushingXpMax);

        if (max <= 0) return 0;
        if (max == min) return min;

        int bound = max - min + 1;
        return min + ThreadLocalRandom.current().nextInt(bound);
    }
}
