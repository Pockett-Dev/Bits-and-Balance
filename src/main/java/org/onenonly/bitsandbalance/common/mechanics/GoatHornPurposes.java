package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Instrument;

/** Server-owned switches for the optional powers of the eight vanilla goat horns. */
public final class GoatHornPurposes {
    public static volatile boolean enabled = false;
    public static volatile boolean separateCooldowns = false;
    public static volatile boolean ponder = false;
    public static volatile boolean sing = false;
    public static volatile boolean seek = false;
    public static volatile boolean feel = false;
    public static volatile boolean admire = false;
    public static volatile boolean call = false;
    public static volatile boolean yearn = false;
    public static volatile boolean dream = false;

    private GoatHornPurposes() {
    }

    public static String horn(ItemStack stack) {
        if (!stack.is(Items.GOAT_HORN)) {
            return "";
        }
        //? if mc == 1.21.1
        Holder<Instrument> instrument = stack.get(DataComponents.INSTRUMENT);
        if (instrument == null) return "";
        java.util.Optional<net.minecraft.resources.ResourceKey<Instrument>> key = instrument.unwrapKey();
        //? else
        //? if mc == 1.21.11
        net.minecraft.world.item.component.InstrumentComponent component = stack.get(DataComponents.INSTRUMENT);
        java.util.Optional<net.minecraft.resources.ResourceKey<Instrument>> key =
                component == null ? java.util.Optional.empty() : component.instrument().key();
        //? else
        net.minecraft.world.item.component.InstrumentComponent component = stack.get(DataComponents.INSTRUMENT);
        Holder<Instrument> instrument = component == null ? null : component.instrument();
        java.util.Optional<net.minecraft.resources.ResourceKey<Instrument>> key =
                instrument == null ? java.util.Optional.empty() : instrument.unwrapKey();
        //? endif
        //? endif
        return key
                //? if mc == 1.21.1
                .filter(hornKey -> "minecraft".equals(hornKey.location().getNamespace()))
                .map(hornKey -> hornKey.location().getPath())
                //? else
                .filter(hornKey -> "minecraft".equals(hornKey.identifier().getNamespace()))
                .map(hornKey -> hornKey.identifier().getPath())
                //? endif
                .filter(path -> path.endsWith("_goat_horn"))
                .map(path -> path.substring(0, path.length() - "_goat_horn".length()))
                .orElse("");
    }

    public static boolean isEnabled(String horn) {
        if (!enabled) {
            return false;
        }
        return switch (horn) {
            case "ponder" -> ponder;
            case "sing" -> sing;
            case "seek" -> seek;
            case "feel" -> feel;
            case "admire" -> admire;
            case "call" -> call;
            case "yearn" -> yearn;
            case "dream" -> dream;
            default -> false;
        };
    }

    public static boolean hasDreamHorn(net.minecraft.world.entity.player.Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if ("dream".equals(horn(player.getInventory().getItem(slot)))) {
                return true;
            }
        }
        return false;
    }
}
