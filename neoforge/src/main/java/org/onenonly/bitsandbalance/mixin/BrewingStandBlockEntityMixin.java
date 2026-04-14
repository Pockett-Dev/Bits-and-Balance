package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.compat.LithiumCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.onenonly.bitsandbalance.potions.CustomBrewing;

import org.onenonly.bitsandbalance.Config;
/**
 * Timed custom brewing for Withering potions that mimics vanilla brewing progression.
 * Lithium-compatible: integrates with Lithium's sleeping block entity optimization.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    private static final Logger LOG = LogUtils.getLogger();

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void rebalance$witheringTimed(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity be, CallbackInfo ci) {
        try {
            if (!Config.enableWitheringPotion) return;

            ItemStack reagent = be.getItem(3);
            if (reagent.isEmpty()) return;
            // reduced periodic logging for production; use DEBUG if needed
            // long t = level.getGameTime();
            // if (t % 20 == 0) {
            //     LOG.debug("[rebalance][brew] Reagent present: {} (fuel={} brewTime={})", reagent.getItem(), acc0.getFuel(), acc0.getBrewTime());
            // }

            // Find a matching custom brewing rule against any bottle slot
            CustomBrewing.Rule matchedRule = null;
            Identifier matchedBaseId = null;
            int matchedIndex = -1;
            for (int i = 0; i < 3; i++) {
                ItemStack stack = be.getItem(i);
                if (stack.isEmpty()) { LOG.debug("[rebalance][brew] Slot {} empty", i); continue; }
                Item item = stack.getItem();
                if (!(item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)) { LOG.debug("[rebalance][brew] Slot {} not a potion item: {}", i, item); continue; }
                PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                var baseOpt = contents.potion();
                if (baseOpt.isEmpty()) { LOG.debug("[rebalance][brew] Slot {} has potion item but no base potion tag", i); continue; }
                var rule = CustomBrewing.findMatch(baseOpt.get(), reagent.getItem());
                if (rule != null) {
                    matchedRule = rule;
                    matchedBaseId = BuiltInRegistries.POTION.getKey(baseOpt.get().value());
                    matchedIndex = i;
                    LOG.debug("[rebalance][brew] Matched rule at slot {} base={} reagent={} -> result={}", i, matchedBaseId, BuiltInRegistries.ITEM.getKey(reagent.getItem()), rule.resultPotion());
                    break;
                } else {
                    var baseId = BuiltInRegistries.POTION.getKey(baseOpt.get().value());
                    LOG.debug("[rebalance][brew] Slot {} base={} no rule for reagent {}", i, baseId, BuiltInRegistries.ITEM.getKey(reagent.getItem()));
                }
            }
            if (matchedRule == null) {
                LOG.debug("[rebalance][brew] No custom brewing rule matched for reagent {}", BuiltInRegistries.ITEM.getKey(reagent.getItem()));
                return; // let vanilla handle others
            }

            var resultHolder = CustomBrewing.resultHolder(matchedRule);
            if (resultHolder == null) return;

            // From here on, we are handling a custom brew. Manage brewTime and fuel akin to vanilla.
            BrewingStandAccessor acc = (BrewingStandAccessor) (Object) be;
            int brewTime = acc.getBrewTime();
            int fuel = acc.getFuel();

            // Load fuel if needed
            if (fuel <= 0) {
                ItemStack fuelStack = be.getItem(4);
                if (!fuelStack.isEmpty() && fuelStack.is(Items.BLAZE_POWDER)) {
                    fuelStack.shrink(1);
                    acc.setFuel(20);
                    fuel = 20;
                    LOG.debug("[rebalance][brew] Loaded fuel -> 20 units");
                }
            }

            // If we still don't have fuel, abort and allow vanilla to tick (but it won't match our recipe)
            if (fuel <= 0) {
                LOG.debug("[rebalance][brew] No fuel; cannot brew custom recipe.");
                return;
            }

            // Start brewing if not already brewing
            if (brewTime <= 0) {
                // Lithium compat: Wake up the brewing stand when starting custom brewing
                LithiumCompat.wakeUp(be);
                
                acc.setBrewTime(400); // vanilla duration
                acc.setFuel(fuel - 1); // consume one fuel unit per brew start
                be.setChanged();
                LOG.debug("[rebalance][brew] Started custom brew at slot {} base={} reagent={} -> {}", matchedIndex, matchedBaseId, BuiltInRegistries.ITEM.getKey(reagent.getItem()), matchedRule.resultPotion());
                ci.cancel();
                return;
            }

            // Continue brewing countdown
            brewTime -= 1;
            acc.setBrewTime(brewTime);
            if (brewTime % 40 == 0 || brewTime < 10) {
                LOG.debug("[rebalance][brew] Brewing... brewTime={} fuel={}", brewTime, acc.getFuel());
            }

            // If conditions broke mid-brew (reagent removed or bottles no longer match), cancel brew
            boolean anyStillMatch = false;
            for (int i = 0; i < 3; i++) {
                ItemStack stack = be.getItem(i);
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                if (!(item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)) continue;
                PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                var baseOpt = contents.potion();
                if (baseOpt.isEmpty()) continue;
                var rule = CustomBrewing.findMatch(baseOpt.get(), reagent.getItem());
                if (rule != null && rule.resultPotion().equals(matchedRule.resultPotion())) { anyStillMatch = true; break; }
            }
            if (reagent.isEmpty() || !anyStillMatch) {
                acc.setBrewTime(0);
                be.setChanged();
                LOG.debug("[rebalance][brew] Brew cancelled due to changed inputs");
                
                // Lithium compat: Allow sleep since brewing was cancelled and stand is now idle
                LithiumCompat.allowSleep(be);
                
                ci.cancel();
                return;
            }

            // Finish brewing
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
                    var rule = CustomBrewing.findMatch(baseOpt.get(), reagent.getItem());
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
                    // Play brew complete sound for baseline Withering result only
                    try {
                        if (!level.isClientSide()) {
                            Identifier baselineWithering = Identifier.fromNamespaceAndPath(BitsAndBalance.MODID, "withering");
                            if (matchedRule.resultPotion().equals(baselineWithering)) {
                                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
                            }
                        }
                    } catch (Throwable ignored2) {
                        // Never break brewing due to sound issues
                    }
                    LOG.debug("[rebalance][brew] Finished custom brew; reagent consumed");
                }
                acc.setBrewTime(0);
                
                // Lithium compat: Allow the brewing stand to enter sleep mode now that brewing is complete
                // This is called after brewTime is set to 0, so Lithium can put it to sleep on next tick
                LithiumCompat.allowSleep(be);
            }

            // Cancel vanilla tick while we manage the custom brew this tick
            ci.cancel();
        } catch (Throwable ignored) {
            // Never break vanilla tick
        }
    }
}
