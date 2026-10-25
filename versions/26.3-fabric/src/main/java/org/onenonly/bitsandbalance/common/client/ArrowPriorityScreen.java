package org.onenonly.bitsandbalance.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.entity.HumanoidArm;
import org.onenonly.bitsandbalance.common.mechanics.ArrowSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * Non-screen HUD selector. Keeping minecraft.screen null preserves movement,
 * mouse look, and the normal captured cursor while the key is held.
 */
public final class ArrowPriorityScreen {
    private static final int HOTBAR_WIDTH = 62;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int SLOT_STEP = 20;
    private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final Identifier HOTBAR_OFFHAND_LEFT = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final Identifier HOTBAR_OFFHAND_RIGHT = Identifier.withDefaultNamespace("hud/hotbar_offhand_right");
    private static ArrowPriorityScreen active;

    private final LocalPlayer player;
    private final InteractionHand hand;
    private final ItemStack weapon;
    private final KeyMapping binding;
    private final List<ItemStack> choices = new ArrayList<>();
    private final Consumer<ItemStack> apply;
    private final int unavailableIndex;
    private int selectedIndex;
    private boolean leftHeld;
    private boolean rightHeld;

    private ArrowPriorityScreen(LocalPlayer player, InteractionHand hand, KeyMapping binding,
                                List<ItemStack> arrows, ItemStack selected, Consumer<ItemStack> apply) {
        this.player = player;
        this.hand = hand;
        this.weapon = player.getItemInHand(hand);
        this.binding = binding;
        this.apply = apply;
        this.choices.add(ItemStack.EMPTY); // Vanilla ammunition selection.
        this.choices.addAll(arrows);
        if (!selected.isEmpty() && arrows.stream().noneMatch(
                arrow -> ItemStack.isSameItemSameComponents(arrow, selected))) {
            this.unavailableIndex = choices.size();
            this.choices.add(selected.copyWithCount(1));
        } else {
            this.unavailableIndex = -1;
        }
        for (int index = 0; index < choices.size(); index++) {
            if (ItemStack.isSameItemSameComponents(choices.get(index), selected)) {
                this.selectedIndex = index;
                break;
            }
        }
    }

    public static void openIfHolding(Minecraft minecraft, KeyMapping binding,
                                     BiConsumer<InteractionHand, ItemStack> sendSelection) {
        if (active != null || minecraft.screen != null || minecraft.player == null) return;
        LocalPlayer player = minecraft.player;
        InteractionHand hand = ArrowSelection.isRangedWeapon(player.getMainHandItem())
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack weapon = player.getItemInHand(hand);
        if (!ArrowSelection.isRangedWeapon(weapon)) return;

        List<ItemStack> arrows = ArrowSelection.availableArrows(player, weapon);
        if (arrows.size() < 2) return;
        active = new ArrowPriorityScreen(player, hand, binding, arrows,
                ArrowSelection.getSelected(weapon, player.registryAccess()),
                arrow -> sendSelection.accept(hand, arrow));
    }

    public static void tick(Minecraft minecraft) {
        ArrowPriorityScreen selector = active;
        if (selector == null) return;
        if (minecraft.screen != null || minecraft.player != selector.player
                || selector.player.getItemInHand(selector.hand) != selector.weapon
                || ArrowSelection.availableArrows(selector.player, selector.weapon).size() < 2) {
            active = null;
            return;
        }
        if (!selector.bindingHeld()) {
            selector.submit();
            active = null;
            return;
        }

        boolean left = selector.keyDown(minecraft, InputConstants.KEY_LEFT)
                || selector.keyDown(minecraft, InputConstants.KEY_UP);
        boolean right = selector.keyDown(minecraft, InputConstants.KEY_RIGHT)
                || selector.keyDown(minecraft, InputConstants.KEY_DOWN);
        if (left && !selector.leftHeld) selector.move(-1);
        if (right && !selector.rightHeld) selector.move(1);
        selector.leftHeld = left;
        selector.rightHeld = right;
    }

    public static boolean onScroll(double verticalAmount) {
        ArrowPriorityScreen selector = active;
        if (selector == null || verticalAmount == 0.0D) return false;
        selector.move(verticalAmount > 0.0D ? -1 : 1);
        return true;
    }

    private boolean bindingHeld() {
        return binding.isDown();
    }

    private boolean keyDown(Minecraft minecraft, int key) {
        return InputConstants.isKeyDown(key);
    }

    private void submit() {
        if (selectedIndex != unavailableIndex) apply.accept(choices.get(selectedIndex));
    }

    private void move(int amount) {
        selectedIndex = Math.max(0, Math.min(choices.size() - 1, selectedIndex + amount));
    }

    private int firstVisibleIndex() {
        return Math.max(0, Math.min(selectedIndex - 1, choices.size() - 3));
    }

    private int selectedSlot() {
        return selectedIndex - firstVisibleIndex();
    }

    private ItemStack choiceAtSlot(int slot) {
        return choices.get(firstVisibleIndex() + slot);
    }

    public static void render(GuiGraphicsExtractor graphics, int offsetX, int offsetY, boolean showArrowSlot) {
        ArrowPriorityScreen selector = active;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) return;
        if (showArrowSlot) renderSelectedArrow(graphics, minecraft);
        if (selector == null || minecraft.player != selector.player) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int centerX = Math.max(85, Math.min(width - 85, width / 2 + offsetX));
        int x = centerX - HOTBAR_WIDTH / 2;
        int y = Math.max(13, Math.min(height - 37, height / 2 + 22 + offsetY));

        // The final pixel comes from the real right edge of the nine-slot sprite.
        // A straight crop would borrow the fourth slot's left edge and distort slot three.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, x, y, HOTBAR_WIDTH - 1, HOTBAR_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 181, 0, x + HOTBAR_WIDTH - 1, y, 1, HOTBAR_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, x + selector.selectedSlot() * SLOT_STEP - 1, y - 1, 24, 23);

        for (int slot = 0; slot < 3; slot++) {
            ItemStack arrow = selector.choiceAtSlot(slot);
            if (arrow.isEmpty()) {
                graphics.text(minecraft.font, "V", x + slot * SLOT_STEP + 8, y + 7, 0xFFFFFFFF);
            } else {
                graphics.item(arrow, x + slot * SLOT_STEP + 3, y + 3);
            }
        }

        ItemStack focused = selector.choices.get(selector.selectedIndex);
        String label = focused.isEmpty() ? "Vanilla" : focused.getHoverName().getString()
                + (selector.selectedIndex == selector.unavailableIndex ? " (unavailable)" : "");
        graphics.centeredText(minecraft.font, minecraft.font.plainSubstrByWidth(label, 170),
                centerX, y + 27, 0xFFFFFFFF);
    }

    public static boolean isActive() {
        return active != null;
    }

    private static void renderSelectedArrow(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ItemStack weapon = player.getMainHandItem();
        if (!ArrowSelection.isRangedWeapon(weapon)) {
            weapon = player.getOffhandItem();
        }
        if (!ArrowSelection.isRangedWeapon(weapon)
                || ArrowSelection.getSelected(weapon, player.registryAccess()).isEmpty()) return;

        ItemStack projectile;
        if (weapon.getItem() instanceof CrossbowItem) {
            ChargedProjectiles charged = weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            projectile = charged.isEmpty() ? player.getProjectile(weapon)
                    : charged.itemCopies().findFirst().orElse(ItemStack.EMPTY);
        } else {
            projectile = player.getProjectile(weapon);
        }
        if (projectile.isEmpty()) return;

        boolean arrowOnLeft = player.getMainArm() == HumanoidArm.LEFT;
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int slotX = arrowOnLeft ? centerX - 91 - 29 : centerX + 91;
        int slotY = minecraft.getWindow().getGuiScaledHeight() - 23;
        Identifier sprite = arrowOnLeft ? HOTBAR_OFFHAND_LEFT : HOTBAR_OFFHAND_RIGHT;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, slotX, slotY, 29, 24);
        graphics.item(projectile, slotX + (arrowOnLeft ? 3 : 10), slotY + 4);
    }
}
