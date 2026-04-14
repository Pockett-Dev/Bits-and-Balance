package org.onenonly.bitsandbalance.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.onenonly.bitsandbalance.api.UsageTickerSlot;

/**
 * Allows external code to modify or replace the stack displayed by the Usage Ticker.
 * Cancel this event to hide the ticker for this slot.
 */
public class UsageTickerGetStackEvent extends Event implements ICancellableEvent {
    private final UsageTickerSlot slot;
    private final ItemStack originalStack;
    private ItemStack resultStack;
    private int currentCount;
    private final boolean renderPass;
    private final Player player;

    public UsageTickerGetStackEvent(UsageTickerSlot slot, ItemStack currentStack, ItemStack originalStack, int currentCount, boolean renderPass, Player player) {
        this.slot = slot;
        this.originalStack = originalStack;
        this.resultStack = currentStack;
        this.currentCount = currentCount;
        this.renderPass = renderPass;
        this.player = player;
    }

    public UsageTickerSlot getSlot() {
        return slot;
    }

    public ItemStack getOriginalStack() {
        return originalStack;
    }

    public ItemStack getResultStack() {
        return resultStack;
    }

    public void setResultStack(ItemStack resultStack) {
        this.resultStack = resultStack;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    public boolean isRenderPass() {
        return renderPass;
    }

    public Player getPlayer() {
        return player;
    }
}
