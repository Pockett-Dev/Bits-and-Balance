package org.onenonly.bitsandbalance.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.onenonly.bitsandbalance.api.UsageTickerSlot;

/**
 * Allows external code to modify or replace the count displayed by the Usage Ticker.
 * Cancel this event to hide the ticker for this slot.
 */
public class UsageTickerGetCountEvent extends Event implements ICancellableEvent {
    private final UsageTickerSlot slot;
    private final ItemStack originalStack;
    private final ItemStack displayStack;
    private int resultCount;
    private final boolean renderPass;
    private final Player player;

    public UsageTickerGetCountEvent(UsageTickerSlot slot, ItemStack displayStack, ItemStack originalStack, int currentCount, boolean renderPass, Player player) {
        this.slot = slot;
        this.originalStack = originalStack;
        this.displayStack = displayStack;
        this.resultCount = currentCount;
        this.renderPass = renderPass;
        this.player = player;
    }

    public UsageTickerSlot getSlot() {
        return slot;
    }

    public ItemStack getOriginalStack() {
        return originalStack;
    }

    public ItemStack getDisplayStack() {
        return displayStack;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public boolean isRenderPass() {
        return renderPass;
    }

    public Player getPlayer() {
        return player;
    }
}
