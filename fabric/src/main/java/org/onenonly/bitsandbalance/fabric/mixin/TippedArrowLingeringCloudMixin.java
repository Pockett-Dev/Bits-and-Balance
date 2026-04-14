package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class TippedArrowLingeringCloudMixin {
    @Unique
    private boolean bitsandbalance$spawnedLingeringCloud;

    @Invoker("setPickupItemStack")
    protected abstract void bitsandbalance$invokeSetPickupItemStack(ItemStack pickupItemStack);

    @Inject(method = "onHitBlock", at = @At("TAIL"))
    private void bitsandbalance$tippedArrowCloud(BlockHitResult hit, CallbackInfo ci) {
        bitsandbalance$spawnCloudIfNeeded();
    }

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void bitsandbalance$tippedArrowCloud(EntityHitResult hit, CallbackInfo ci) {
        bitsandbalance$spawnCloudIfNeeded();
    }

    private void bitsandbalance$spawnCloudIfNeeded() {
        if (!FabricTweaksConfig.enableTippedArrowLingeringClouds) return;

        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;
        if (!(self instanceof Arrow)) return;
        if (bitsandbalance$spawnedLingeringCloud) return;

        ItemStack pickup = self.getPickupItemStackOrigin();
        if (!pickup.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) return;

        AreaEffectCloud cloud = new AreaEffectCloud(self.level(), self.getX(), self.getY(), self.getZ());
        if (self.getOwner() instanceof net.minecraft.world.entity.LivingEntity living) {
            cloud.setOwner(living);
        }

        cloud.setRadius(3.0F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setDuration(600);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.applyComponentsFromItemStack(pickup);
        level.addFreshEntity(cloud);

        this.bitsandbalance$invokeSetPickupItemStack(new ItemStack(Items.ARROW));
        bitsandbalance$spawnedLingeringCloud = true;
    }
}
