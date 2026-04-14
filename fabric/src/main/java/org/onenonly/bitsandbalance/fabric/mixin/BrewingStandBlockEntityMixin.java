package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricPotionConfig;
import org.onenonly.bitsandbalance.fabric.potions.FabricCustomBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Timed custom brewing to support Bits and Balance potion recipes with non-vanilla reagents.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void bitsandbalance$customBrewingTick(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity be, CallbackInfo ci) {
        try {
            if (!FabricPotionConfig.enableCustomBrewing) return;

            ItemStack reagent = be.getItem(3);
            if (reagent.isEmpty()) return;

            FabricCustomBrewing.Rule matchedRule = null;
            for (int i = 0; i < 3; i++) {
                ItemStack stack = be.getItem(i);
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                if (!(item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)) continue;

                PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                var baseOpt = contents.potion();
                if (baseOpt.isEmpty()) continue;

                var rule = FabricCustomBrewing.findMatch(baseOpt.get(), reagent.getItem());
                if (rule != null) {
                    matchedRule = rule;
                    break;
                }
            }
            if (matchedRule == null) return;

            var resultHolder = FabricCustomBrewing.resultHolder(matchedRule);
            if (resultHolder == null) return;

            BrewingStandAccessor acc = (BrewingStandAccessor) (Object) be;
            int brewTime = acc.bitsandbalance$getBrewTime();
            int fuel = acc.bitsandbalance$getFuel();

            if (fuel <= 0) {
                ItemStack fuelStack = be.getItem(4);
                if (!fuelStack.isEmpty() && fuelStack.getItem() == Items.BLAZE_POWDER) {
                    fuelStack.shrink(1);
                    acc.bitsandbalance$setFuel(20);
                    fuel = 20;
                }
            }

            if (fuel <= 0) return;

            if (brewTime <= 0) {
                acc.bitsandbalance$setBrewTime(400);
                acc.bitsandbalance$setFuel(fuel - 1);
                be.setChanged();
                ci.cancel();
                return;
            }

            // Continue brewing countdown
            brewTime -= 1;
            acc.bitsandbalance$setBrewTime(brewTime);

            // Cancel if reagent removed or rule no longer matches any slot
            boolean anyStillMatch = !reagent.isEmpty();
            if (anyStillMatch) {
                anyStillMatch = false;
                for (int i = 0; i < 3; i++) {
                    ItemStack stack = be.getItem(i);
                    if (stack.isEmpty()) continue;
                    Item item = stack.getItem();
                    if (!(item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)) continue;

                    PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                    var baseOpt = contents.potion();
                    if (baseOpt.isEmpty()) continue;

                    var rule = FabricCustomBrewing.findMatch(baseOpt.get(), reagent.getItem());
                    if (rule != null && rule.resultPotion().equals(matchedRule.resultPotion())) {
                        anyStillMatch = true;
                        break;
                    }
                }
            }
            if (!anyStillMatch) {
                acc.bitsandbalance$setBrewTime(0);
                be.setChanged();
                ci.cancel();
                return;
            }

            if (brewTime <= 0) {
                boolean converted = false;
                for (int i = 0; i < 3; i++) {
                    ItemStack stack = be.getItem(i);
                    if (stack.isEmpty()) continue;
                    Item item = stack.getItem();
                    if (!(item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)) continue;

                    PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                    var baseOpt = contents.potion();
                    if (baseOpt.isEmpty()) continue;

                    var rule = FabricCustomBrewing.findMatch(baseOpt.get(), reagent.getItem());
                    if (rule != null && rule.resultPotion().equals(matchedRule.resultPotion())) {
                        ItemStack out = PotionContents.createItemStack(item, resultHolder);
                        out.setCount(stack.getCount());
                        be.setItem(i, out);
                        converted = true;
                    }
                }
                if (converted) {
                    reagent.shrink(1);
                    be.setChanged();
                }
                acc.bitsandbalance$setBrewTime(0);
            }

            ci.cancel();
        } catch (Throwable ignored) {
            // Never break vanilla behavior.
        }
    }
}
