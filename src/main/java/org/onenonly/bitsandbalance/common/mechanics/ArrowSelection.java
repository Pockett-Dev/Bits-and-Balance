package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/** Per-weapon preferred arrow, stored with the bow/crossbow rather than the player. */
public final class ArrowSelection {
    private static final String KEY = "bitsandbalance_selected_arrow";

    private ArrowSelection() {
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem);
    }

    public static boolean isArrow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ArrowItem;
    }

    public static ItemStack getSelected(ItemStack weapon, HolderLookup.Provider registries) {
        if (!isRangedWeapon(weapon)) return ItemStack.EMPTY;
        CustomData data = weapon.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ItemStack.EMPTY;
        CompoundTag tag = data.copyTag();
        //? if mc == 1.21.1
        if (!tag.contains(KEY, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        CompoundTag arrowTag = tag.getCompound(KEY);
        //? else
        CompoundTag arrowTag = tag.getCompoundOrEmpty(KEY);
        if (arrowTag.isEmpty()) return ItemStack.EMPTY;
        //? endif
        ItemStack arrow = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), arrowTag)
                .result().orElse(ItemStack.EMPTY);
        return isArrow(arrow) ? arrow.copyWithCount(1) : ItemStack.EMPTY;
    }

    public static void setSelected(ItemStack weapon, ItemStack arrow, HolderLookup.Provider registries) {
        if (!isRangedWeapon(weapon) || (!arrow.isEmpty() && !isArrow(arrow))) return;
        CustomData existing = weapon.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing == null ? new CompoundTag() : existing.copyTag();
        tag.remove(KEY);
        if (!arrow.isEmpty()) {
            Tag encoded = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                    arrow.copyWithCount(1)).result().orElse(null);
            if (!(encoded instanceof CompoundTag)) return;
            tag.put(KEY, encoded);
        }
        if (tag.isEmpty()) weapon.remove(DataComponents.CUSTOM_DATA);
        else weapon.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Distinct arrow types currently carried, including potion and other item components. */
    public static List<ItemStack> availableArrows(Player player) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!isArrow(stack)) continue;
            if (result.stream().noneMatch(value -> ItemStack.isSameItemSameComponents(value, stack))) {
                result.add(stack.copyWithCount(1));
            }
        }
        return result;
    }

    /** Inventory arrows accepted by this bow or crossbow's projectile predicate. */
    public static List<ItemStack> availableArrows(Player player, ItemStack weapon) {
        if (!isRangedWeapon(weapon)) return List.of();
        ProjectileWeaponItem item = (ProjectileWeaponItem) weapon.getItem();
        return availableArrows(player).stream()
                .filter(item.getAllSupportedProjectiles())
                .toList();
    }

    /** Returns the actual inventory stack so vanilla loading consumes the chosen ammunition. */
    public static ItemStack findPreferredProjectile(Player player, ItemStack weapon) {
        ItemStack selected = getSelected(weapon, player.registryAccess());
        if (selected.isEmpty()) return ItemStack.EMPTY;
        ProjectileWeaponItem item = (ProjectileWeaponItem) weapon.getItem();
        if (!item.getAllSupportedProjectiles().test(selected)) return ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, selected)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Validate the current hand and inventory on the server before applying a client request. */
    public static void setSelectedIfHeld(Player player, net.minecraft.world.InteractionHand hand, ItemStack arrow) {
        ItemStack weapon = player.getItemInHand(hand);
        if (!isRangedWeapon(weapon)) return;
        if (arrow.isEmpty()) {
            setSelected(weapon, ItemStack.EMPTY, player.registryAccess());
            return;
        }
        if (!isArrow(arrow) || !((ProjectileWeaponItem) weapon.getItem()).getAllSupportedProjectiles().test(arrow)) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, arrow)) {
                setSelected(weapon, candidate, player.registryAccess());
                return;
            }
        }
    }
}
