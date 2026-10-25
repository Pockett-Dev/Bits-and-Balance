package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
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

    private static final int MAX_TIME = 60; // ticks visible without change
    private static final int ANIM_TIME = 10; // ticks for in/out ease (requested duration)
    
    // Easing function for smooth animation
    private static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3.0f);
    }
    // Width in pixels of the vanilla off-hand UI next to the hotbar
    private static final int OFFHAND_UI_WIDTH = 29;
    // Gap between two icons when rendering both near the main-hand side
    private static final int SECOND_ICON_GAP = 20;

    // Hotbar scroll detection
    private static int lastHotbarIndex = -1;
    private static Item lastHotbarItem = null;
    private static boolean justScrolled = false;

    // Suppression state: after timing out, don't reappear until held item changes
    private static boolean suppressedMain = false;
    private static boolean suppressedOff = false;
    private static ItemStack suppressedMainStack = ItemStack.EMPTY;
    private static ItemStack suppressedOffStack = ItemStack.EMPTY;
    private static int suppressedMainCount = -1;
    private static int suppressedOffCount = -1;
    
    // Armor damage tracking - persistent across ticks
    private static final java.util.Map<UsageTickerSlot, Integer> armorDamageTracking = new java.util.HashMap<>();

    private static boolean isSuppressed(UsageTickerSlot slot, ItemStack real) {
        if (slot == UsageTickerSlot.MAIN_HAND) {
            if (suppressedMain) {
                // Auto-clear if player changed item
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
    private static final TickerElement ARMOR_HEAD = new TickerElement(UsageTickerSlot.ARMOR_HEAD);
    private static final TickerElement ARMOR_CHEST = new TickerElement(UsageTickerSlot.ARMOR_CHEST);
    private static final TickerElement ARMOR_LEGS = new TickerElement(UsageTickerSlot.ARMOR_LEGS);
    private static final TickerElement ARMOR_FEET = new TickerElement(UsageTickerSlot.ARMOR_FEET);

    private static final List<TickerElement> ALL = new ArrayList<>(2);
    static {
        ALL.add(MAIN_HAND);
        ALL.add(OFF_HAND);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        if (!Config.usageTickerEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        Player p = mc.player;

        // Detect hotbar scroll once per tick
        try {
            if (p != null) {
                ItemStack selected = p.getMainHandItem();
                Item selectedItem = selected.getItem();
                if (lastHotbarItem == null) {
                    lastHotbarItem = selectedItem;
                    justScrolled = false;
                } else {
                    justScrolled = (selectedItem != lastHotbarItem);
                    lastHotbarItem = selectedItem;
                }
            }
        } catch (Throwable ignored) {
            justScrolled = false;
        }

        // Tick state for each element
        for (TickerElement el : ALL) {
            el.tick(p);
        }

        // Reset one-shot flag
        justScrolled = false;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        if (!Config.usageTickerEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        Player p = mc.player;
        GuiGraphics gg = e.getGuiGraphics();
        Window win = mc.getWindow();
        Font font = mc.font;

        boolean invert = Config.usageTickerInvert;
        float partial = e.getPartialTick().getGameTimeDeltaTicks();

        // Off-hand element is ticked for timing/state, but is never rendered directly.
        ItemStack offLogical = OFF_HAND.getDisplayStack();
        int offTotal = OFF_HAND.getCount();
        boolean renderOff = OFF_HAND.getLiveTicks() > 0 && !offLogical.isEmpty() && offTotal >= 2;

        // Main-hand can be visible in single-mode (count < 2); treat that as occupying the main slot.
        boolean mainDisplayed = MAIN_HAND.getLiveTicks() > 0 && !MAIN_HAND.getDisplayStack().isEmpty();
        
        // Check if any armor is being displayed (has priority over main hand)
        boolean armorDisplayed = false;
        
        for (TickerElement el : ALL) {
            if (el.slot == UsageTickerSlot.OFF_HAND) continue; // never render off-hand directly
            
            // Skip main hand if armor is being displayed (armor takes priority)
            if (el.slot == UsageTickerSlot.MAIN_HAND && armorDisplayed) continue;
            
            el.render(gg, win, font, p, invert, partial);
            if (el.slot == UsageTickerSlot.MAIN_HAND && renderOff) {
                // If main is currently displayed (including single-mode), place off-hand to the left with a gap.
                int dx = mainDisplayed ? -SECOND_ICON_GAP : 0;
                int lt = OFF_HAND.getLiveTicks();
                el.renderCustom(gg, win, font, p, invert, partial, offLogical, offTotal, lt, dx, 0);
            }
        }
    }

    private static class TickerElement {
        final UsageTickerSlot slot;
        private ItemStack currDisplayStack = ItemStack.EMPTY; // what we render
        private ItemStack currRealStack = ItemStack.EMPTY;    // the actual slot's stack
        private int currCount = 0;
        private int liveTicks = 0; // animation progress 0..ANIM_TIME (0 = hidden)
        private float liveTicksPartial = 0.0f; // partial tick for smooth animation
        private int stableTicks = 0; // time since last change
        private boolean fadingOut = false; // currently fading out
        private boolean pendingSuppress = false; // mark to suppress after timeout fade completes
        private boolean singleMode = false; // transient display for singles after scroll
        private int singleModeTicks = 0; // remaining ticks for single-mode
        private int fadeOutDelayTicks = 0; // delay before starting fade out (for chestplate/leggings)

        // cached comparison keys to avoid allocations
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
            fadeOutDelayTicks = 0;
            lastItemKey = null;
            lastDamageKey = -1;
            lastCountKey = -1;
        }

        void tick(Player p) {
            ItemStack real = getRealStackForSlot(p, slot);
            currRealStack = real;

            ItemStack logical = getLogicalStack(p, slot, real, false);
            boolean keptAliveOnScroll = false;

            // If nothing logical to show
            if (logical.isEmpty()) {
                // If we just scrolled the hotbar, keep the current display alive instead of hiding immediately
                if (justScrolled && slot == UsageTickerSlot.MAIN_HAND && liveTicks > 0 && !singleMode) {
                    fadingOut = false;
                    pendingSuppress = false;
                    if (stableTicks < MAX_TIME) stableTicks++;
                    keptAliveOnScroll = true;
                    // Do not change liveTicks so the current animation state persists
                } else {
                    // Not an inactivity case; ensure we won't suppress on completion
                    pendingSuppress = false;
                    if (liveTicks > 0) {
                        fadingOut = true;
                        stableTicks = MAX_TIME; // force fade path
                        liveTicks = Math.max(0, liveTicks - 1);
                    } else {
                        hideNow();
                    }
                    return;
                }
                // If we chose to keep alive due to scroll, proceed below
            }

            // Short-circuit on kept-alive scroll to avoid computing counts with an empty logical stack
            if (keptAliveOnScroll) {
                // Optionally allow fade-in to progress slightly
                if (!fadingOut && liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                return;
            }

            int total = getStackCount(p, slot, logical, real, false);

            // Check for armor damage changes to trigger display
            boolean armorDamaged = false;
            if (slot.isArmor() && !real.isEmpty()) {
                int currentDamage = real.getDamageValue();
                Integer lastDamage = armorDamageTracking.get(slot);
                
                // Initialize damage tracking if this is the first time we see this armor
                if (lastDamage == null) {
                    armorDamageTracking.put(slot, currentDamage);
                    // Don't trigger display on first detection - just initialize tracking
                    // Continue with normal processing but skip damage detection
                } else {
                    // Reset damage tracking if armor item changed (different item or repaired)
                    if (lastDamage > currentDamage) {
                        // Armor was repaired or replaced, reset tracking
                        armorDamageTracking.put(slot, currentDamage);
                    }
                    // Only trigger if damage increased (durability decreased) and damage > 0
                    else if (currentDamage > lastDamage && currentDamage > 0) {
                        armorDamaged = true;
                        // Only update lastDamageKey when we actually detect damage increase
                        armorDamageTracking.put(slot, currentDamage);
                    }
                }
            }

            // Special case: armor damage triggers display even with count < 2
            if (armorDamaged && total < 2) {
                currDisplayStack = logical;
                currCount = total;
                fadingOut = false;
                pendingSuppress = false;
                stableTicks = 0; // Reset stable timer when damage occurs
                // Set fade-out delay for chestplate and leggings (1 tick)
                if (slot == UsageTickerSlot.ARMOR_CHEST || slot == UsageTickerSlot.ARMOR_LEGS) {
                    fadeOutDelayTicks = 1;
                } else {
                    fadeOutDelayTicks = 0;
                }
                if (liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                return;
            }
            
            // For armor slots, if we're not taking new damage and count < 2, use normal stable/fade behavior
            if (slot.isArmor() && total < 2 && !armorDamaged) {
                // Use normal stable display logic - let it linger for MAX_TIME ticks
                if (liveTicks > 0) {
                    // We have a display, check if we should start fading
                    if (stableTicks < MAX_TIME) {
                        stableTicks++;
                        // Keep visible and stable
                        if (liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                    } else {
                        // MAX_TIME reached, check for fade-out delay
                        if (fadeOutDelayTicks > 0) {
                            fadeOutDelayTicks--;
                        } else {
                            // Delay finished, start fade out
                            fadingOut = true;
                            // Fade out at half speed using smooth frame-based timing
                            liveTicksPartial = Math.max(0, liveTicksPartial - 0.5f);
                            liveTicks = (int) liveTicksPartial;
                        }
                    }
                } else {
                    // No display, hide completely
                    hideNow();
                }
                return;
            }

            // If rules say "hide" (e.g., total < 2) and no armor damage
            if (total < 2 && !armorDamaged) {
                if (slot == UsageTickerSlot.MAIN_HAND) {
                    if (justScrolled && !logical.isEmpty() && (logical.isStackable() || logical.getItem() instanceof BlockItem)) {
                        // Start/refresh transient single-mode display for up to 3s
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
                        // Maintain single-mode display until timer expires
                        currDisplayStack = logical;
                        currCount = logical.getCount();
                        if (singleModeTicks > 0) {
                            singleModeTicks--;
                            // keep visible
                            if (liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                        }
                        if (singleModeTicks <= 0) {
                            // Begin fade-out due to single-mode timeout and suppress until item changes
                            pendingSuppress = true;
                            fadingOut = true;
                        }
                        // Do not proceed to normal stable/fade logic while in single-mode path
                        // Animation step processed below
                    } else if (!singleMode) {
                        // Not in single-mode and does not qualify; use normal fade/hide
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
                    // Non-main slots do not use single-mode; normal fade/hide
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
                if (total >= 2) { singleMode = false; singleModeTicks = 0; }
                stableTicks = 0;
                if (liveTicks < ANIM_TIME) liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
            } else {
                // Stable display
                currCount = total;
                if (!fadingOut) {
                    if (stableTicks < MAX_TIME) stableTicks++;
                    if (stableTicks >= MAX_TIME) {
                        // Begin inactivity-driven fade-out; mark for suppression on completion
                        fadingOut = true;
                        pendingSuppress = true;
                    }
                }
            }

            // Drive animation progress
            if (fadingOut) {
                if (liveTicks > 0) {
                    // Fade out at half speed using smooth frame-based timing
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
            } else {
                // Fade-in to full visibility then hold
                if (liveTicks < ANIM_TIME) {
                    liveTicks = Math.min(ANIM_TIME, liveTicks + 1);
                    liveTicksPartial = liveTicks;
                }
            }
        }

        // Accessors for coordination
        ItemStack getDisplayStack() { return currDisplayStack; }
        int getCount() { return currCount; }
        int getLiveTicks() { return liveTicks; }

        void render(GuiGraphics gg, Window win, Font font, Player p, boolean invert, float partial) {
            if (liveTicks <= 0 || currDisplayStack.isEmpty()) return;
            // Update smooth animation timing for 60 FPS
            liveTicksPartial = liveTicks + partial;
            renderInternal(gg, win, font, p, invert, partial, currDisplayStack, currCount, liveTicks, 0, 0);
        }

        // Render a provided stack/count at this element's position with additional offset
        void renderCustom(GuiGraphics gg, Window win, Font font, Player p, boolean invert, float partial, ItemStack stack, int count, int customLiveTicks, int dx, int dy) {
            if (customLiveTicks <= 0 || stack == null || stack.isEmpty()) return;
            renderInternal(gg, win, font, p, invert, partial, stack, count, customLiveTicks, dx, dy);
        }

        private void renderInternal(GuiGraphics gg, Window win, Font font, Player p, boolean invert, float partial, ItemStack stack, int count, int lt, int dx, int dy) {
            int screenW = win.getGuiScaledWidth();
            int screenH = win.getGuiScaledHeight();
            int cx = screenW / 2;

            // quintic smootherstep ease in/out over ANIM_TIME ticks for very smooth motion
            float prog = Math.max(0f, Math.min(ANIM_TIME, lt + partial)) / (float) ANIM_TIME; // 0..1
            // s = 6t^5 - 15t^4 + 10t^3
            float s = prog * prog * prog * (prog * (prog * 6f - 15f) + 10f);
            float anim = s * 20f;

            int baseY = screenH - 24 - (int) anim; // just above hotbar
            int baseX = cx;

            // group offsets
            int offX = slot.isArmor() ? Config.usageTickerArmorOffsetX : Config.usageTickerOffsetX;
            int offY = slot.isArmor() ? Config.usageTickerArmorOffsetY : Config.usageTickerOffsetY;

            // Position near hotbar edges for hands; armor spreads more
            if (!slot.isArmor()) {
                // Anchor relative to hotbar (vanilla width 182px)
                int hotbarLeft = cx - 182 / 2;
                int hotbarRight = hotbarLeft + 182;
                if (slot == UsageTickerSlot.MAIN_HAND) {
                    // Place 16px to the left of the first slot (icon is 16px, so center is left+8)
                    baseX = hotbarLeft - 24;
                    // Shift left by offhand UI width when off-hand has any item
                    if (!p.getOffhandItem().isEmpty()) {
                        baseX -= OFFHAND_UI_WIDTH;
                    }
                } else if (slot == UsageTickerSlot.OFF_HAND) {
                    // Place 16px to the right of the ninth slot (right edge of hotbar)
                    baseX = hotbarRight + 24;
                }
                // Move hand icons down by 32 pixels
                baseY += 32;
            } else {
                // Armor positioning: helmet/chestplate left side, leggings/boots right side
                int hotbarLeft = cx - 182 / 2;
                int hotbarRight = hotbarLeft + 182;
                int spacing = 20; // spacing between armor pieces (reduced from 32)
                
                switch (slot) {
                     case ARMOR_HEAD -> {
                         // Helmet on left side of hotbar - 1 tick delay before starting
                         baseX = hotbarLeft - spacing;
                         int startY = screenH + Config.armorHelmetStartY;  // Start off-screen below
                         int endY = screenH - Config.armorHelmetEndY;      // End position from screen bottom
                         int delayTicks = 1;  // 1 tick delay before helmet starts
                         int animDuration = 10;  // Fixed 10 tick animation duration
                         // Apply delay and clamp animation progress with smooth interpolation
                         float smoothAnim = liveTicksPartial;
                         float progress = Math.min(Math.max(0, (smoothAnim - delayTicks) / (float) animDuration), 1.0f);
                         // Apply easing for smoother animation
                         progress = easeOutCubic(progress);
                         baseY = (int) (startY + (endY - startY) * progress);
                     }
                    case ARMOR_CHEST -> {
                        // Chestplate above helmet (left side) - starts immediately
                        baseX = hotbarLeft - spacing;
                        int startY = screenH + Config.armorChestplateStartY;  // Start off-screen below
                        int endY = screenH - Config.armorChestplateEndY;      // End position from screen bottom
                        int animDuration = 10;  // Fixed 10 tick animation duration
                        // No delay for chestplate - starts immediately with smooth animation
                        float smoothAnim = liveTicksPartial;
                        float progress = Math.min(smoothAnim / (float) animDuration, 1.0f);
                        // Apply easing for smoother animation
                        progress = easeOutCubic(progress);
                        baseY = (int) (startY + (endY - startY) * progress);
                    }
                    case ARMOR_LEGS -> {
                        // Leggings on right side of hotbar - starts immediately
                        baseX = hotbarRight + spacing;
                        int startY = screenH + Config.armorLeggingsStartY;  // Start off-screen below
                        int endY = screenH - Config.armorLeggingsEndY;      // End position from screen bottom
                        int animDuration = 10;  // Fixed 10 tick animation duration
                        // No delay for leggings - starts immediately with smooth animation
                        float smoothAnim = liveTicksPartial;
                        float progress = Math.min(smoothAnim / (float) animDuration, 1.0f);
                        // Apply easing for smoother animation
                        progress = easeOutCubic(progress);
                        baseY = (int) (startY + (endY - startY) * progress);
                    }
                    case ARMOR_FEET -> {
                        // Boots above leggings (right side) - 1 tick delay before starting
                        baseX = hotbarRight + spacing;
                        int startY = screenH + Config.armorBootsStartY;  // Start off-screen below
                        int endY = screenH - Config.armorBootsEndY;      // End position from screen bottom
                        int delayTicks = 1;  // 1 tick delay before boots start
                        int animDuration = 10;  // Fixed 10 tick animation duration
                        // Apply delay and clamp animation progress with smooth interpolation
                        float smoothAnim = liveTicksPartial;
                        float progress = Math.min(Math.max(0, (smoothAnim - delayTicks) / (float) animDuration), 1.0f);
                        // Apply easing for smoother animation
                        progress = easeOutCubic(progress);
                        baseY = (int) (startY + (endY - startY) * progress);
                    }
                    default -> {
                        baseX = cx;
                        baseY -= 8;
                    }
                }
            }

            baseX += offX + dx;
            baseY += offY + dy;

            // Render item and count decorations
            gg.renderItem(stack, baseX - 8, baseY - 8);
            gg.renderItemDecorations(font, stack, baseX - 8, baseY - 8, count > 1 ? Integer.toString(count) : null);
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

    // --- Logic helpers ---

    private static ItemStack getRealStackForSlot(Player p, UsageTickerSlot slot) {
        return switch (slot) {
            case MAIN_HAND -> p.getMainHandItem();
            case OFF_HAND -> p.getOffhandItem();
            case ARMOR_HEAD -> p.getItemBySlot(EquipmentSlot.HEAD);
            case ARMOR_CHEST -> p.getItemBySlot(EquipmentSlot.CHEST);
            case ARMOR_LEGS -> p.getItemBySlot(EquipmentSlot.LEGS);
            case ARMOR_FEET -> p.getItemBySlot(EquipmentSlot.FEET);
        };
    }

    public static ItemStack getLogicalStack(Player p, UsageTickerSlot slot, ItemStack real, boolean renderPass) {
        if (real.isEmpty()) return ItemStack.EMPTY;
        // Suppress reappearance if this slot recently timed out and player still holds same item
        if (isSuppressed(slot, real)) return ItemStack.EMPTY;

        // Overrides on held item
        if (real.getItem() instanceof IUsageTickerOverride ov) {
            ItemStack custom = ov.getUsageTickerItem(real);
            if (custom.isEmpty()) return ItemStack.EMPTY;
            // Fire event for stack mutation
            UsageTickerGetStackEvent ev = new UsageTickerGetStackEvent(slot, custom, real, 0, renderPass, p);
            NeoForge.EVENT_BUS.post(ev);
            if (ev.isCanceled()) return ItemStack.EMPTY;
            return ev.getResultStack();
        }

        // Projectile weapon ammo display (no Infinity for bows)
        if (real.getItem() instanceof ProjectileWeaponItem) {
            boolean hasInfinity = hasInfinity(real);
            if (!hasInfinity) {
                ItemStack ammo = p.getProjectile(real);
                if (!ammo.isEmpty()) {
                    UsageTickerGetStackEvent ev = new UsageTickerGetStackEvent(slot, ammo, real, 0, renderPass, p);
                    NeoForge.EVENT_BUS.post(ev);
                    if (ev.isCanceled()) return ItemStack.EMPTY;
                    return ev.getResultStack();
                }
            }
        }

        // Default: display the held item itself
        UsageTickerGetStackEvent ev = new UsageTickerGetStackEvent(slot, real, real, 0, renderPass, p);
        NeoForge.EVENT_BUS.post(ev);
        if (ev.isCanceled()) return ItemStack.EMPTY;
        return ev.getResultStack();
    }

    public static int getStackCount(Player p, UsageTickerSlot slot, ItemStack logical, ItemStack original, boolean renderPass) {
        int total = 0;
        
        // For armor slots, just return the count of the armor piece itself (always 1 if not empty)
        if (slot.isArmor()) {
            return logical.isEmpty() ? 0 : logical.getCount();
        }
        
        final Predicate<ItemStack> predicate = s -> ItemStack.isSameItemSameComponents(s, logical);

        // Sum all stacks in inventory (main) + equipment (armor/offhand)
        Container inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (predicate.test(s)) total += s.getCount();
            if (s.getItem() instanceof IUsageTickerOverride ov) {
                try { total += ov.getUsageTickerCountForItem(s, predicate); } catch (Throwable ignored) {}
            }
        }

        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = p.getItemBySlot(EquipmentSlot.FEET);

        for (ItemStack s : new ItemStack[]{head, chest, legs, feet}) {
            if (predicate.test(s)) total += s.getCount();
            if (s.getItem() instanceof IUsageTickerOverride ov) {
                try { total += ov.getUsageTickerCountForItem(s, predicate); } catch (Throwable ignored) {}
            }
        }

        ItemStack offhand = p.getOffhandItem();
        if (!offhand.isEmpty()) {
            if (predicate.test(offhand)) total += offhand.getCount();
            if (offhand.getItem() instanceof IUsageTickerOverride ov) {
                try { total += ov.getUsageTickerCountForItem(offhand, predicate); } catch (Throwable ignored) {}
            }
        }

        // Ensure at least the logical stack count
        total = Math.max(total, logical.getCount());

        // New rule: only show ticker for stacked items (count >= 2)
        // Note: Armor damage handling is done in the tick() method, not here
        if (total < 2) {
            return 0;
        }

        // Hide rules
        boolean isBlock = logical.getItem() instanceof BlockItem;
        boolean stackable = logical.isStackable();
        if (!stackable && !isBlock && (slot == UsageTickerSlot.MAIN_HAND || slot == UsageTickerSlot.OFF_HAND)) {
            return 0; // hide
        }

        boolean checkMatchSize = false;
        if (original.getItem() instanceof IUsageTickerOverride ov) {
            try { checkMatchSize = ov.shouldUsageTickerCheckMatchSize(original); } catch (Throwable ignored) {}
        }
        if (checkMatchSize && total == original.getCount()) {
            return 0; // hide when only held stack exists
        }

        // Fire event for count mutation
        UsageTickerGetCountEvent ev = new UsageTickerGetCountEvent(slot, logical, original, total, renderPass, p);
        NeoForge.EVENT_BUS.post(ev);
        if (ev.isCanceled()) return 0;
        return Math.max(0, ev.getResultCount());
    }

    private static boolean hasInfinity(ItemStack bow) {
        try {
            ItemEnchantments ench = bow.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var holder : ench.keySet()) {
                if (holder.is(Enchantments.INFINITY)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
