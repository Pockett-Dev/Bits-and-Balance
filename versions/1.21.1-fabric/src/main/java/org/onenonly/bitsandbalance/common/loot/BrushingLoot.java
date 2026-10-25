package org.onenonly.bitsandbalance.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
//? if mc == 1.21.1 || mc == 1.21.11
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
//? endif
import org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags;

/** Config predicates and a disabled-potion-safe function for ordinary loot-table JSON. */
public final class BrushingLoot {
    private static final ResourceLocation RESURFACING = ResourceLocation.parse("bitsandbalance:resurfacing");
    public static final MapCodec<Enabled> ENABLED_CODEC = Codec.STRING.fieldOf("site")
            .xmap(Enabled::new, Enabled::site);
    public static final MapCodec<Resurfacing> RESURFACING_CODEC = MapCodec.unit(new Resurfacing());
    public static final MapCodec<Fortune> FORTUNE_CODEC = Codec.BOOL.optionalFieldOf("rare", false)
            .xmap(Fortune::new, Fortune::rare);
    //? if mc == 1.21.1 || mc == 1.21.11
    public static final LootItemFunctionType<Fortune> FORTUNE_TYPE = new LootItemFunctionType<>(FORTUNE_CODEC);
    public static final LootItemConditionType ENABLED_TYPE = new LootItemConditionType(ENABLED_CODEC);
    public static final LootItemFunctionType<Resurfacing> RESURFACING_TYPE = new LootItemFunctionType<>(RESURFACING_CODEC);
    //? endif
    private BrushingLoot() {}
    public record Enabled(String site) implements LootItemCondition {
        @Override public boolean test(LootContext context) {
            if (site.equals("resurfacing"))
                return PotionFeatureFlags.isResurfacingPotionEnabled()
                        && BuiltInRegistries.POTION.getOptional(RESURFACING).isPresent();
            return BrushingLootSettings.enabled(site);
        }
        //? if mc == 1.21.1 || mc == 1.21.11
        @Override public LootItemConditionType getType() { return ENABLED_TYPE; }
        //? else
        @Override public MapCodec<Enabled> codec() { return ENABLED_CODEC; }
        //? endif
    }
    /** Apply after count/enchantment/potion functions so the chosen reward keeps all its components. */
    public record Fortune(boolean rare) implements LootItemFunction {
        @Override public ItemStack apply(ItemStack stack, LootContext context) {
            if (stack.isEmpty() || BrushFortune.isItemBlacklisted(stack)) return stack;
            //? if mc == 1.21.1
            var entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            //? else
            //? if mc == 26.3
            var entity = context.getOptional(LootContextParams.THIS_ENTITY);
            //? else
            var entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
            //? endif
            //? endif
            if (!(entity instanceof LivingEntity brusher)) return stack;
            ItemStack brush = BrushFortune.activeBrush(brusher);
            // 26.x includes the actual tool, including off-hand and non-player use.
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            //? if mc == 26.3
            var tool = context.getOptional(LootContextParams.TOOL);
            //? else
            var tool = context.getOptionalParameter(LootContextParams.TOOL);
            //? endif
            if (tool instanceof ItemStack actual) brush = actual;
            //? endif
            int fortune = BrushFortune.level(brush, context.getLevel());
            if (fortune == 0) return stack;
            if (rare) {
                if (stack.getCount() == 1 && context.getRandom().nextInt(3) < fortune) stack.setCount(2);
            } else stack.setCount(BrushFortune.commonCount(stack.getCount(), fortune));
            return stack;
        }
        //? if mc == 1.21.1 || mc == 1.21.11
        @Override public LootItemFunctionType<Fortune> getType() { return FORTUNE_TYPE; }
        //? else
        @Override public MapCodec<Fortune> codec() { return FORTUNE_CODEC; }
        //? endif
    }
    /** Resolve at roll time: Fabric omits disabled potions from its registry at startup. */
    public static final class Resurfacing implements LootItemFunction {
        @Override public ItemStack apply(ItemStack stack, LootContext context) {
            var potion = BuiltInRegistries.POTION.getOptional(RESURFACING);
            if (!PotionFeatureFlags.isResurfacingPotionEnabled() || potion.isEmpty()) return ItemStack.EMPTY;
            stack.set(DataComponents.POTION_CONTENTS,
                    new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(potion.get())));
            return stack;
        }
        //? if mc == 1.21.1 || mc == 1.21.11
        @Override public LootItemFunctionType<Resurfacing> getType() { return RESURFACING_TYPE; }
        //? else
        @Override public MapCodec<Resurfacing> codec() { return RESURFACING_CODEC; }
        //? endif
    }
}
