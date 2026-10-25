package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Tweaks: Armed Armor Stands
 * When a player places an Armor Stand using the item, enable arms so items can be held.
 */
@Mixin(ArmorStandItem.class)
public abstract class ArmorStandItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void bitsandbalance$enableArmsOnPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FabricTweaksConfig.enableArmedArmorStands) return;
        Level level = context.getLevel();
        if (level.isClientSide()) return;

        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) return;

        BlockPos pos = context.getClickedPos();
        if (pos == null) return;

        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(center.x - 1.0, center.y - 2.0, center.z - 1.0,
                center.x + 1.0, center.y + 2.0, center.z + 1.0);
        try {
            List<ArmorStand> stands = level.getEntitiesOfClass(ArmorStand.class, box,
                    as -> as != null && as.tickCount <= 5 && !as.isRemoved());
            for (ArmorStand as : stands) {
                try {
                    if (!as.showArms()) {
                        as.setShowArms(true);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
