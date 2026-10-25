package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Tweaks: Brushing XP Rewards
 */
@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin {

    @Unique
    private boolean bitsandbalance$xpGiven;

    @Inject(
            method = "brushingCompleted(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL")
    )
    private void bitsandbalance$brushingCompleted(Player brusher, CallbackInfo ci) {
        if (!FabricTweaksConfig.brushingXpEnabled || bitsandbalance$xpGiven) {
            return;
        }

        int xp = computeRandomXp();
        if (xp <= 0) {
            return;
        }

        BrushableBlockEntity self = (BrushableBlockEntity) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)) {
            return;
        }

        xp = org.onenonly.bitsandbalance.common.loot.BrushFortune.experience(xp,
                org.onenonly.bitsandbalance.common.loot.BrushFortune.activeBrush(brusher), level);
        ExperienceOrb.award(level, Vec3.atCenterOf(self.getBlockPos()), xp);
        bitsandbalance$xpGiven = true;
    }

    private static int computeRandomXp() {
        return org.onenonly.bitsandbalance.common.loot.BrushFortune.baseExperience(
                FabricTweaksConfig.brushingXpMin, FabricTweaksConfig.brushingXpMax);
    }
}
