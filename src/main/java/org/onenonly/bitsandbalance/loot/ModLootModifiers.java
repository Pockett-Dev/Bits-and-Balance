package org.onenonly.bitsandbalance.loot;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.onenonly.bitsandbalance.BitsAndBalance;

import java.util.function.Supplier;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = 
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BitsAndBalance.MODID);

    public static final Supplier<MapCodec<ConfigurableLootModifier>> CONFIGURABLE_ADD_TABLE = 
            LOOT_MODIFIERS.register("configurable_add_table", () -> ConfigurableLootModifier.CODEC);

    //? if mc == 1.21.1 || mc == 1.21.11
    private static final DeferredRegister<net.minecraft.world.level.storage.loot.predicates.LootItemConditionType> CONDITIONS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_CONDITION_TYPE, BitsAndBalance.MODID);
    private static final DeferredRegister<net.minecraft.world.level.storage.loot.functions.LootItemFunctionType<?>> FUNCTIONS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_FUNCTION_TYPE, BitsAndBalance.MODID);
    static {
        CONDITIONS.register("brushing_loot", () -> org.onenonly.bitsandbalance.common.loot.BrushingLoot.ENABLED_TYPE);
        FUNCTIONS.register("resurfacing_potion", () -> org.onenonly.bitsandbalance.common.loot.BrushingLoot.RESURFACING_TYPE);
        FUNCTIONS.register("brush_fortune", () -> org.onenonly.bitsandbalance.common.loot.BrushingLoot.FORTUNE_TYPE);
    }
    //? else
    private static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.storage.loot.predicates.LootItemCondition>> CONDITIONS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_CONDITION_TYPE, BitsAndBalance.MODID);
    private static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.storage.loot.functions.LootItemFunction>> FUNCTIONS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.LOOT_FUNCTION_TYPE, BitsAndBalance.MODID);
    static {
        CONDITIONS.register("brushing_loot", () -> org.onenonly.bitsandbalance.common.loot.BrushingLoot.ENABLED_CODEC);
        FUNCTIONS.register("resurfacing_potion", () -> org.onenonly.bitsandbalance.common.loot.BrushingLoot.RESURFACING_CODEC);
        FUNCTIONS.register("brush_fortune", () -> org.onenonly.bitsandbalance.common.loot.BrushingLoot.FORTUNE_CODEC);
    }
    //? endif

    public static void register(IEventBus modEventBus) {
        CONDITIONS.register(modEventBus);
        FUNCTIONS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
    }
}
