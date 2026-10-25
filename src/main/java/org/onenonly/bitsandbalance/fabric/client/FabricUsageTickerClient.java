package org.onenonly.bitsandbalance.fabric.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.BlockItem;
import org.onenonly.bitsandbalance.api.IUsageTickerOverride;
import org.onenonly.bitsandbalance.api.UsageTickerSlot;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fabric implementation of the Usage Ticker overlay.
 *
 * This intentionally avoids NeoForge event APIs (NeoForge-only events in api/event)
 * and instead supports the shared IUsageTickerOverride hook which is loader-agnostic.
 */
public final class FabricUsageTickerClient {
    private FabricUsageTickerClient() {
    }

    private static final int MAX_TIME = 60; // ticks visible without change
    private static final int ANIM_TIME = 10; // ticks for in/out ease

    // Width in pixels of the vanilla off-hand UI next to the hotbar
    private static final int OFFHAND_UI_WIDTH = 29;

    private static boolean justScrolled = false;
    private static Item lastHotbarItem = null;

    // Suppression state: after timing out, don't reappear until held item changes
    private static boolean suppressedMain = false;
    private static ItemStack suppressedMainStack = ItemStack.EMPTY;
    private static int suppressedMainCount = -1;

    private static boolean suppressedOff = false;
    private static ItemStack suppressedOffStack = ItemStack.EMPTY;
    private static int suppressedOffCount = -1;

    private static boolean isSuppressed(UsageTickerSlot slot, ItemStack real) {
        if (slot != UsageTickerSlot.MAIN_HAND && slot != UsageTickerSlot.OFF_HAND) {
            return false;
        }

        if (slot == UsageTickerSlot.MAIN_HAND) {
            if (!suppressedMain) return false;
            if (real.isEmpty()
                    || !ItemStack.isSameItemSameComponents(real, suppressedMainStack)
                    || real.getCount() != suppressedMainCount) {
                suppressedMain = false;
                suppressedMainStack = ItemStack.EMPTY;
                suppressedMainCount = -1;
                return false;
            }
            return true;
        }

        // OFF_HAND
        if (!suppressedOff) return false;
        if (real.isEmpty()
                || !ItemStack.isSameItemSameComponents(real, suppressedOffStack)
                || real.getCount() != suppressedOffCount) {
            suppressedOff = false;
            suppressedOffStack = ItemStack.EMPTY;
            suppressedOffCount = -1;
            return false;
        }
        return true;
    }

    private static void setSuppressed(UsageTickerSlot slot, ItemStack real) {
        if (slot != UsageTickerSlot.MAIN_HAND && slot != UsageTickerSlot.OFF_HAND) {
            return;
        }
        if (real == null || real.isEmpty()) {
            return;
        }

        if (slot == UsageTickerSlot.MAIN_HAND) {
            suppressedMain = true;
            suppressedMainStack = real.copyWithCount(1);
            suppressedMainCount = real.getCount();
            return;
        }

        suppressedOff = true;
        suppressedOffStack = real.copyWithCount(1);
        suppressedOffCount = real.getCount();
    }

    private static final TickerElement MAIN_HAND = new TickerElement(UsageTickerSlot.MAIN_HAND);
    private static final TickerElement OFF_HAND = new TickerElement(UsageTickerSlot.OFF_HAND);
    private static final TickerElement ARMOR_HEAD = new TickerElement(UsageTickerSlot.ARMOR_HEAD);
    private static final TickerElement ARMOR_CHEST = new TickerElement(UsageTickerSlot.ARMOR_CHEST);
    private static final TickerElement ARMOR_LEGS = new TickerElement(UsageTickerSlot.ARMOR_LEGS);
    private static final TickerElement ARMOR_FEET = new TickerElement(UsageTickerSlot.ARMOR_FEET);

    private static final List<TickerElement> ALL = new ArrayList<>(2);

    static {
        ALL.add(MAIN_HAND);
        ALL.add(OFF_HAND);
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!FabricClientConfig.usageTickerEnabled) {
                hideAll();
                return;
            }
            if (client == null || client.player == null || client.level == null) return;

            Player player = client.player;

            // Hotbar scroll detection (main-hand item change)
            try {
                ItemStack selected = player.getMainHandItem();
                Item selectedItem = selected.getItem();
                if (lastHotbarItem == null) {
                    lastHotbarItem = selectedItem;
                    justScrolled = false;
                } else {
                    justScrolled = selectedItem != lastHotbarItem;
                    lastHotbarItem = selectedItem;
                }
            } catch (Throwable ignored) {
                justScrolled = false;
            }

            for (TickerElement el : ALL) {
                el.tick(player);
            }

            justScrolled = false;
        });

        HudRenderCallback.EVENT.register((drawContext, deltaTracker) -> {
            if (!FabricClientConfig.usageTickerEnabled) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) return;

            // On newer MC versions this callback provides a DeltaTracker; we only need a partial tick.
            // Use 0 for simplicity (keeps logic correct; just removes sub-tick smoothing).
            float tickDelta = 0.0f;

            Player player = mc.player;
            GuiGraphics gg = drawContext;
            Font font = mc.font;

            boolean invert = FabricClientConfig.usageTickerInvert;

            // Off-hand element is ticked for timing/state, but is never rendered directly.
            ItemStack offLogical = OFF_HAND.getDisplayStack();
            int offTotal = OFF_HAND.getCount();
            boolean renderOff = OFF_HAND.getLiveTicks() > 0 && !offLogical.isEmpty() && offTotal >= 2;

            boolean armorDisplayed = false;

            boolean mainRendered = MAIN_HAND.getLiveTicks() > 0 && MAIN_HAND.getCount() >= 2 && !MAIN_HAND.getDisplayStack().isEmpty();

            for (TickerElement el : ALL) {
                if (el.slot == UsageTickerSlot.OFF_HAND) continue;
                if (el.slot == UsageTickerSlot.MAIN_HAND && armorDisplayed) continue;

                el.render(gg, font, invert, tickDelta);

                if (el.slot == UsageTickerSlot.MAIN_HAND && renderOff) {
                    int dx = mainRendered ? -20 : 0;
                    int lt = OFF_HAND.getLiveTicks();
                    el.renderCustom(gg, font, invert, tickDelta, offLogical, offTotal, lt, dx, 0);
                }
            }
        });
    }

    private static void hideAll() {
        for (TickerElement el : ALL) {
            el.hideNow();
        }
    }

    private static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3.0f);
    }

    private static class TickerElement {
        final UsageTickerSlot slot;

        private ItemStack currDisplayStack = ItemStack.EMPTY;
        private ItemStack currRealStack = ItemStack.EMPTY;
        private int currCount = 0;

        private int liveTicks = 0;
        private int stableTicks = 0;
        private boolean fadingOut = false;

        // When we timeout due to inactivity, suppress reappearance until item changes.
        private boolean pendingSuppress = false;

        // keys for change detection
        private Item lastItemKey = null;
        private int lastDamageKey = -1;
        private int lastCountKey = -1;

        TickerElement(UsageTickerSlot slot) {
            this.slot = slot;
        }

        void hideNow() {
            currDisplayStack = ItemStack.EMPTY;
            currRealStack = ItemStack.EMPTY;
            currCount = 0;
            liveTicks = 0;
            stableTicks = 0;
            fadingOut = false;
            pendingSuppress = false;
            lastItemKey = null;
            lastDamageKey = -1;
            lastCountKey = -1;
        }

        void tick(Player p) {
            ItemStack real = getRealStackForSlot(p, slot);
            currRealStack = real;

            if (isSuppressed(slot, real)) {
                hideNow();
                return;
            }

            ItemStack logical = getLogicalStack(p, slot, real);

            if (logical.isEmpty()) {
                // If we just scrolled, keep visible for a moment instead of snapping off
                if (justScrolled && slot == UsageTickerSlot.MAIN_HAND && liveTicks > 0) {
                    fadingOut = false;
                    stableTicks = 0;
                    return;
                }
                startFadeOut();
                tickFade();
                return;
            }

            int count = getStackCount(p, slot, logical, real);

            // For most slots: only show when count >= 2.
            // Armor slots show durability remaining style count; we keep same rule: only show if >=2.
            if (count < 2) {
                // Allow quick hint on scroll for main-hand singles
                if (justScrolled && slot == UsageTickerSlot.MAIN_HAND) {
                    setCurrent(logical, real, Math.max(count, 1));
                    stableTicks = 0;
                    fadingOut = false;
                    pendingSuppress = false;
                    tickFadeIn();
                    return;
                }
                startFadeOut();
                tickFade();
                return;
            }

            // detect changes
            Item itemKey = logical.getItem();
            int damageKey = logical.isDamageableItem() ? logical.getDamageValue() : 0;
            int countKey = count;

            boolean changed = (lastItemKey != itemKey) || (lastDamageKey != damageKey) || (lastCountKey != countKey);
            if (changed) {
                setCurrent(logical, real, count);
                stableTicks = 0;
                fadingOut = false;
                pendingSuppress = false;
                tickFadeIn();
            } else {
                stableTicks++;
                if (stableTicks >= MAX_TIME) {
                    // Inactivity-driven fade-out should suppress reappearance until item changes.
                    if (slot == UsageTickerSlot.MAIN_HAND || slot == UsageTickerSlot.OFF_HAND) {
                        pendingSuppress = true;
                    }
                    startFadeOut();
                }
                if (fadingOut) {
                    tickFade();
                } else {
                    tickFadeIn();
                }
            }
        }

        private void setCurrent(ItemStack logical, ItemStack real, int count) {
            currDisplayStack = logical.copyWithCount(1);
            currRealStack = real.copyWithCount(1);
            currCount = count;
            lastItemKey = currDisplayStack.getItem();
            lastDamageKey = currDisplayStack.isDamageableItem() ? currDisplayStack.getDamageValue() : 0;
            lastCountKey = currCount;
        }

        private void startFadeOut() {
            fadingOut = true;
        }

        private void tickFadeIn() {
            if (liveTicks < ANIM_TIME) liveTicks++;
        }

        private void tickFade() {
            if (liveTicks > 0) {
                liveTicks--;
            }
            if (liveTicks <= 0) {
                if (pendingSuppress) {
                    setSuppressed(slot, currRealStack);
                    pendingSuppress = false;
                }
                hideNow();
            }
        }

        void render(GuiGraphics gg, Font font, boolean invert, float tickDelta) {
            if (liveTicks <= 0) return;
            if (currDisplayStack.isEmpty() || currCount < 2) return;

            int screenW = gg.guiWidth();
            int screenH = gg.guiHeight();

            int baseX;
            int baseY = screenH - 19 + FabricClientConfig.usageTickerOffsetY;

            if (slot == UsageTickerSlot.MAIN_HAND) {
                int cx = screenW / 2;
                int hotbarLeft = cx - 91;
                // Match NeoForge: place icon 16px left of the first hotbar slot.
                baseX = (hotbarLeft - 24) + FabricClientConfig.usageTickerOffsetX;
                if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.getOffhandItem().isEmpty()) {
                    baseX -= OFFHAND_UI_WIDTH;
                }
            } else {
                // Keep existing behavior for armor/other slots for now.
                baseX = screenW / 2 + 91 + FabricClientConfig.usageTickerOffsetX;
            }

            float t = Math.min(1.0f, (liveTicks + tickDelta) / (float) ANIM_TIME);
            float a = easeOutCubic(t);
            if (invert) a = 1.0f - a;

            int y = Math.round(baseY + (1.0f - a) * 12.0f);

            gg.renderItem(currDisplayStack, baseX, y);
            gg.renderItemDecorations(font, currDisplayStack, baseX, y, String.valueOf(currCount));
        }

        void renderCustom(
                GuiGraphics gg,
                Font font,
                boolean invert,
                float tickDelta,
                ItemStack displayStack,
                int count,
                int liveTicks,
                int dx,
                int dy
        ) {
            if (liveTicks <= 0) return;
            if (displayStack.isEmpty() || count < 2) return;

            int screenW = gg.guiWidth();
            int screenH = gg.guiHeight();

            int baseX;
            if (slot == UsageTickerSlot.MAIN_HAND) {
                int cx = screenW / 2;
                int hotbarLeft = cx - 91;
                // Match NeoForge: place icon 16px left of the first hotbar slot.
                baseX = (hotbarLeft - 24) + FabricClientConfig.usageTickerOffsetX + dx;
                if (Minecraft.getInstance().player != null && !Minecraft.getInstance().player.getOffhandItem().isEmpty()) {
                    baseX -= OFFHAND_UI_WIDTH;
                }
            } else {
                baseX = screenW / 2 + 91 + FabricClientConfig.usageTickerOffsetX + dx;
            }
            int baseY = screenH - 19 + FabricClientConfig.usageTickerOffsetY + dy;

            float t = Math.min(1.0f, (liveTicks + tickDelta) / (float) ANIM_TIME);
            float a = easeOutCubic(t);
            if (invert) a = 1.0f - a;

            int y = Math.round(baseY + (1.0f - a) * 12.0f);

            ItemStack one = displayStack.copyWithCount(1);
            gg.renderItem(one, baseX, y);
            gg.renderItemDecorations(font, one, baseX, y, String.valueOf(count));
        }

        int getLiveTicks() {
            return liveTicks;
        }

        int getCount() {
            return currCount;
        }

        ItemStack getDisplayStack() {
            return currDisplayStack;
        }
    }

    private static ItemStack getRealStackForSlot(Player p, UsageTickerSlot slot) {
        if (p == null) return ItemStack.EMPTY;
        return switch (slot) {
            case MAIN_HAND -> p.getMainHandItem();
            case OFF_HAND -> p.getOffhandItem();
            case ARMOR_HEAD -> p.getItemBySlot(EquipmentSlot.HEAD);
            case ARMOR_CHEST -> p.getItemBySlot(EquipmentSlot.CHEST);
            case ARMOR_LEGS -> p.getItemBySlot(EquipmentSlot.LEGS);
            case ARMOR_FEET -> p.getItemBySlot(EquipmentSlot.FEET);
        };
    }

    private static ItemStack getLogicalStack(Player p, UsageTickerSlot slot, ItemStack real) {
        if (real.isEmpty()) return ItemStack.EMPTY;

        // Suppress reappearance after inactivity timeout until the player changes item.
        if (isSuppressed(slot, real)) return ItemStack.EMPTY;

        // Allow item-defined overrides
        if (real.getItem() instanceof IUsageTickerOverride ov) {
            try {
                ItemStack custom = ov.getUsageTickerItem(real);
                if (custom != null) return custom;
            } catch (Throwable ignored) {
            }
        }

        // Projectile weapon ammo display (e.g., bow -> arrows) when not Infinity.
        if (p != null && real.getItem() instanceof ProjectileWeaponItem && !hasInfinity(real)) {
            try {
                ItemStack ammo = p.getProjectile(real);
                if (!ammo.isEmpty()) {
                    return ammo;
                }
            } catch (Throwable ignored) {
            }
        }

        return real;
    }

    private static int getStackCount(Player p, UsageTickerSlot slot, ItemStack logical, ItemStack original) {
        if (logical.isEmpty()) return 0;

        // Armor slots: treat as "present" only (NeoForge keeps armor logic separate)
        if (slot.isArmor()) {
            return logical.getCount();
        }

        Predicate<ItemStack> pred = s -> ItemStack.isSameItemSameComponents(s, logical);

        // Allow item-defined custom counting
        if (original.getItem() instanceof IUsageTickerOverride ov) {
            try {
                return ov.getUsageTickerCountForItem(original, pred);
            } catch (Throwable ignored) {
            }
        }

        // Default behavior: count matching stacks in inventory
        int total = 0;
        try {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.isEmpty()) continue;
                if (pred.test(s)) total += s.getCount();

                if (s.getItem() instanceof IUsageTickerOverride ov) {
                    try {
                        total += ov.getUsageTickerCountForItem(s, pred);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Ensure at least the logical stack's own count
        total = Math.max(total, logical.getCount());

        // New rule: only show for stacked totals
        if (total < 2) {
            return 0;
        }

        // Hide rules (match NeoForge): for hands, hide non-stackable non-block items.
        boolean isBlock = logical.getItem() instanceof BlockItem;
        boolean stackable = logical.isStackable();
        if (!stackable && !isBlock && (slot == UsageTickerSlot.MAIN_HAND || slot == UsageTickerSlot.OFF_HAND)) {
            return 0;
        }

        boolean checkMatchSize = false;
        if (original.getItem() instanceof IUsageTickerOverride ov) {
            try {
                checkMatchSize = ov.shouldUsageTickerCheckMatchSize(original);
            } catch (Throwable ignored) {
            }
        }
        if (checkMatchSize && total == original.getCount()) {
            return 0;
        }

        return total;
    }

    private static boolean hasInfinity(ItemStack bow) {
        try {
            ItemEnchantments ench = bow.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var holder : ench.keySet()) {
                if (holder.is(Enchantments.INFINITY)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
