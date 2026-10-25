package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class TorchFuelHandler {
    private TorchFuelHandler() {}

    @SubscribeEvent
    public static void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        if (!Config.enableTorchFuel) return;

        ItemStack stack = event.getItemStack();
        if (stack.is(Items.TORCH)) {
            event.setBurnTime(Config.torchBurnTime);
        }
    }
}
