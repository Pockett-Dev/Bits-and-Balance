package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.api.IUsageTickerOverride;
import org.onenonly.bitsandbalance.api.UsageTickerSlot;
import org.onenonly.bitsandbalance.api.event.UsageTickerGetCountEvent;
import org.onenonly.bitsandbalance.api.event.UsageTickerGetStackEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Quark-like Usage Ticker overlay.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID, value = Dist.CLIENT)
public class UsageTickerClient {

    private static final int MAX_TIME = 60;
    private static final int ANIM_TIME = 10;
    private static final int OFFHAND_UI_WIDTH = 29;
    private static final int SECOND_ICON_GAP = 20;

    private static Item lastHotbarItem = null;
    private static boolean justScrolled = false;

    private static boolean suppressedMain = false;
    private static boolean suppressedOff = false;
    private static ItemStack suppressedMainStack = ItemStack.EMPTY;
    private static ItemStack suppressedOffStack = ItemStack.EMPTY;
    private static int suppressedMainCount = -1;
    private static int suppressedOffCount = -1;

    private static boolean isSuppressed(UsageTickerSlot slot, ItemStack real) {
        if (slot == UsageTickerSlot.MAIN_HAND) {
            if (suppressedMain) {
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
            return false;
        } else if (slot == UsageTickerSlot.OFF_HAND) {
            if (suppressedOff) {
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
            return false;
        }
        return false;
    }

    private static void setSuppressed(UsageTickerSlot slot, ItemStack real) {
        if (real == null || real.isEmpty()) return;
        if (slot == UsageTickerSlot.MAIN_HAND) {
            suppressedMain = true;
            suppressedMainStack = real.copyWithCount(1);
            suppressedMainCount = real.getCount();
        } else if (slot == UsageTickerSlot.OFF_HAND) {
            suppressedOff = true;
            suppressedOffStack = real.copyWithCount(1);
            suppressedOffCount = real.getCount();
        }
    }

    private static final TickerElement MAIN_HAND = new TickerElement(UsageTickerSlot.MAIN_HAND);
    private static final TickerElement OFF_HAND = new TickerElement(UsageTickerSlot.OFF_HAND);

    private static final List<TickerElement> ALL = new ArrayList<>(2);

    static {
        ALL.add(MAIN_HAND);
        ALL.add(OFF_HAND);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.usageTickerEnabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        Player player = minecraft.player;

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

        for (TickerElement element : ALL) {
            element.tick(player);
        }

        justScrolled = false;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!Config.usageTickerEnabled) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        Player player = minecraft.player;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Window window = minecraft.getWindow();
        Font font = minecraft.font;

        boolean invert = Config.usageTickerInvert;
        float partial = event.getPartialTick().getGameTimeDeltaTicks();

        ItemStack offLogical = OFF_HAND.getDisplayStack();
        int offTotal = OFF_HAND.getCount();
        boolean renderOff = OFF_HAND.getLiveTicks() > 0 && !offLogical.isEmpty() && offTotal >= 2;
        boolean mainDisplayed = MAIN_HAND.getLiveTicks() > 0 && !MAIN_HAND.getDisplayStack().isEmpty();

        for (TickerElement element : ALL) {
            if (element.slot == UsageTickerSlot.OFF_HAND) continue;

            element.render(guiGraphics, window, font, player, invert, partial);
            if (element.slot == UsageTickerSlot.MAIN_HAND && renderOff) {
                int dx = mainDisplayed ? -SECOND_ICON_GAP : 0;
                int lt = OFF_HAND.getLiveTicks();
                element.renderCustom(guiGraphics, window, font, player, invert, partial, offLogical, offTotal, lt, dx, 0);
            }
        }
    }

    private static class TickerElement {
        final UsageTickerSlot slot;
        private ItemStack currDisplayStack = ItemStack.EMPTY;
        private ItemStack currRealStack = ItemStack.EMPTY;
        private int currCount = 0;
        private int liveTicks = 0;
        private float liveTicksPartial = 0.0f;
        private int stableTicks = 0;
        private boolean fadingOut = false;
        private boolean pendingSuppress = false;
        private boolean singleMode = false;
        private int singleModeTicks = 0;
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
            liveTicksPartial = 0.0f;
            stableTicks = 0;
            fadingOut = false;
            pendingSuppress = false;
            singleMode = false;
            singleModeTicks = 0;
            lastItemKey = null;
            lastDamageKey = -1;
            lastCountKey = -1;
        }

        void tick(Player player) {
            ItemStack real = getRealStackForSlot(player, slot);
            currRealStack = real;

            ItemStack logical = getLogicalStack(player, slot, real, false);
            boolean keptAliveOnScroll = false;

            if (logical.isEmpty()) {
                if (justScrolled && slot == UsageTickerSlot.MAIN_HAND && liveTicks > 0 && !singleMode) {
                    fadingOut = false;
                    pendingSuppress = false;
                    if (stableTicks < MAX_TIME) stableTicks++;
                    keptAliveOnScroll = true;
                } else {
                    pendingSuppress = false;
                    if (liveTicks > 0) {
                        fadingOut = true;
                        stableTicks = MAX_TIME;
                        liveTicks = Math.max(0, liveTicks - 1);
                    } else {
                        hideNow();
                    }
                    return;
                }
            }

            if (keptAliveOnScroll) {
                if (!fadingOut && liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                return;
            }

            int total = getStackCount(player, slot, logical, real, false);

            if (total < 2) {
                if (slot == UsageTickerSlot.MAIN_HAND) {
                    if (justScrolled && !logical.isEmpty() && (logical.isStackable() || logical.getItem() instanceof BlockItem)) {
                        singleMode = true;
                        singleModeTicks = MAX_TIME;
                        currDisplayStack = logical;
                        currCount = logical.getCount();
                        fadingOut = false;
                        pendingSuppress = false;
                        stableTicks = 0;
                        if (liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                        return;
                    }
                    if (singleMode && !logical.isEmpty()) {
                        currDisplayStack = logical;
                        currCount = logical.getCount();
                        if (singleModeTicks > 0) {
                            singleModeTicks--;
                            if (liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                        }
                        if (singleModeTicks <= 0) {
                            pendingSuppress = true;
                            fadingOut = true;
                        }
                    } else if (!singleMode) {
                        pendingSuppress = false;
                        if (liveTicks > 0) {
                            fadingOut = true;
                            stableTicks = MAX_TIME;
                            liveTicks = Math.max(0, liveTicks - 1);
                        } else {
                            hideNow();
                        }
                        return;
                    }
                } else {
                    pendingSuppress = false;
                    if (liveTicks > 0) {
                        fadingOut = true;
                        stableTicks = MAX_TIME;
                        liveTicks = Math.max(0, liveTicks - 1);
                    } else {
                        hideNow();
                    }
                    return;
                }
            }

            boolean changed = shouldChange(logical, total);
            if (changed) {
                currDisplayStack = logical;
                currCount = total;
                fadingOut = false;
                pendingSuppress = false;
                if (total >= 2) {
                    singleMode = false;
                    singleModeTicks = 0;
                }
                stableTicks = 0;
                if (liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
            } else {
                currCount = total;
                if (!fadingOut) {
                    if (stableTicks < MAX_TIME) stableTicks++;
                    if (stableTicks >= MAX_TIME) {
                        fadingOut = true;
                        pendingSuppress = true;
                    }
                }
            }

            if (fadingOut) {
                if (liveTicks > 0) {
                    liveTicksPartial = Math.max(0, liveTicksPartial - 0.5f);
                    liveTicks = (int) liveTicksPartial;
                    if (liveTicks <= 0) {
                        if (pendingSuppress) {
                            setSuppressed(slot, currRealStack);
                            pendingSuppress = false;
                        }
                        hideNow();
                    }
                } else {
                    if (pendingSuppress) {
                        setSuppressed(slot, currRealStack);
                        pendingSuppress = false;
                    }
                    hideNow();
                }
            } else if (liveTicks < ANIM_TIME) {
                liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                liveTicksPartial = liveTicks;
            }
        }

        ItemStack getDisplayStack() {
            return currDisplayStack;
        }

        int getCount() {
            return currCount;
        }

        int getLiveTicks() {
            return liveTicks;
        }

        void render(GuiGraphics guiGraphics, Window window, Font font, Player player, boolean invert, float partial) {
            if (liveTicks <= 0 || currDisplayStack.isEmpty()) return;
            liveTicksPartial = liveTicks + partial;
            renderInternal(guiGraphics, window, font, player, invert, partial, currDisplayStack, currCount, liveTicks, 0, 0);
        }

        void renderCustom(GuiGraphics guiGraphics, Window window, Font font, Player player, boolean invert, float partial, ItemStack stack, int count, int customLiveTicks, int dx, int dy) {
            if (customLiveTicks <= 0 || stack == null || stack.isEmpty()) return;
            renderInternal(guiGraphics, window, font, player, invert, partial, stack, count, customLiveTicks, dx, dy);
        }

        private void renderInternal(GuiGraphics guiGraphics, Window window, Font font, Player player, boolean invert, float partial, ItemStack stack, int count, int lt, int dx, int dy) {
            int screenW = window.getGuiScaledWidth();
            int screenH = window.getGuiScaledHeight();
            int cx = screenW / 2;

            float prog = Math.max(0f, Math.min(ANIM_TIME, lt + partial)) / (float) ANIM_TIME;
            float s = prog * prog * prog * (prog * (prog * 6f - 15f) + 10f);
            float anim = s * 20f;

            int baseY = screenH - 24 - (int) anim;
            int baseX = cx;

            int offX = Config.usageTickerOffsetX;
            int offY = Config.usageTickerOffsetY;

            int hotbarLeft = cx - 182 / 2;
            int hotbarRight = hotbarLeft + 182;
            if (slot == UsageTickerSlot.MAIN_HAND) {
                baseX = hotbarLeft - 24;
                if (!player.getOffhandItem().isEmpty()) {
                    baseX -= OFFHAND_UI_WIDTH;
                }
            } else if (slot == UsageTickerSlot.OFF_HAND) {
                baseX = hotbarRight + 24;
            }

            baseY += 32;
            baseX += offX + dx;
            baseY += offY + dy;

            guiGraphics.renderItem(stack, baseX - 8, baseY - 8);
            guiGraphics.renderItemDecorations(font, stack, baseX - 8, baseY - 8, count > 1 ? Integer.toString(count) : null);
        }

        private boolean shouldChange(ItemStack logical, int total) {
            Item item = logical.getItem();
            int dmg = logical.isDamageableItem() ? logical.getDamageValue() : 0;
            boolean itemChanged = item != lastItemKey || dmg != lastDamageKey;
            boolean countChanged = total != lastCountKey;
            if (itemChanged || countChanged) {
                lastItemKey = item;
                lastDamageKey = dmg;
                lastCountKey = total;
                return true;
            }
            return false;
        }
    }

    private static ItemStack getRealStackForSlot(Player player, UsageTickerSlot slot) {
        return switch (slot) {
            case MAIN_HAND -> player.getMainHandItem();
            case OFF_HAND -> player.getOffhandItem();
            default -> ItemStack.EMPTY;
        };
    }

    public static ItemStack getLogicalStack(Player player, UsageTickerSlot slot, ItemStack real, boolean renderPass) {
        if (real.isEmpty()) return ItemStack.EMPTY;
        if (isSuppressed(slot, real)) return ItemStack.EMPTY;

        if (real.getItem() instanceof IUsageTickerOverride override) {
            ItemStack custom = override.getUsageTickerItem(real);
            if (custom.isEmpty()) return ItemStack.EMPTY;
            UsageTickerGetStackEvent event = new UsageTickerGetStackEvent(slot, custom, real, 0, renderPass, player);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) return ItemStack.EMPTY;
            return event.getResultStack();
        }

        if (real.getItem() instanceof ProjectileWeaponItem) {
            boolean hasInfinity = hasInfinity(real);
            if (!hasInfinity) {
                ItemStack ammo = player.getProjectile(real);
                if (!ammo.isEmpty()) {
                    UsageTickerGetStackEvent event = new UsageTickerGetStackEvent(slot, ammo, real, 0, renderPass, player);
                    NeoForge.EVENT_BUS.post(event);
                    if (event.isCanceled()) return ItemStack.EMPTY;
                    return event.getResultStack();
                }
            }
        }

        UsageTickerGetStackEvent event = new UsageTickerGetStackEvent(slot, real, real, 0, renderPass, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return ItemStack.EMPTY;
        return event.getResultStack();
    }

    public static int getStackCount(Player player, UsageTickerSlot slot, ItemStack logical, ItemStack original, boolean renderPass) {
        int total = 0;
        Predicate<ItemStack> predicate = stack -> ItemStack.isSameItemSameComponents(stack, logical);

        Container inventory = player.getInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            if (predicate.test(stack)) total += stack.getCount();
            if (stack.getItem() instanceof IUsageTickerOverride override) {
                try {
                    total += override.getUsageTickerCountForItem(stack, predicate);
                } catch (Throwable ignored) {
                }
            }
        }

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        for (ItemStack stack : new ItemStack[]{head, chest, legs, feet}) {
            if (predicate.test(stack)) total += stack.getCount();
            if (stack.getItem() instanceof IUsageTickerOverride override) {
                try {
                    total += override.getUsageTickerCountForItem(stack, predicate);
                } catch (Throwable ignored) {
                }
            }
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            if (predicate.test(offhand)) total += offhand.getCount();
            if (offhand.getItem() instanceof IUsageTickerOverride override) {
                try {
                    total += override.getUsageTickerCountForItem(offhand, predicate);
                } catch (Throwable ignored) {
                }
            }
        }

        total = Math.max(total, logical.getCount());
        if (total < 2) {
            return 0;
        }

        boolean isBlock = logical.getItem() instanceof BlockItem;
        boolean stackable = logical.isStackable();
        if (!stackable && !isBlock && (slot == UsageTickerSlot.MAIN_HAND || slot == UsageTickerSlot.OFF_HAND)) {
            return 0;
        }

        boolean checkMatchSize = false;
        if (original.getItem() instanceof IUsageTickerOverride override) {
            try {
                checkMatchSize = override.shouldUsageTickerCheckMatchSize(original);
            } catch (Throwable ignored) {
            }
        }
        if (checkMatchSize && total == original.getCount()) {
            return 0;
        }

        UsageTickerGetCountEvent event = new UsageTickerGetCountEvent(slot, logical, original, total, renderPass, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return 0;
        return Math.max(0, event.getResultCount());
    }

    private static boolean hasInfinity(ItemStack bow) {
        try {
            ItemEnchantments enchantments = bow.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var holder : enchantments.keySet()) {
                if (holder.is(Enchantments.INFINITY)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
