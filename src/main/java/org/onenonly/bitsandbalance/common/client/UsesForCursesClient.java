package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Optional;

public final class UsesForCursesClient {

    private UsesForCursesClient() {
    }

    public static boolean shouldHidePumpkinOverlayFromHead(boolean enabled) {
        if (!enabled) return false;

        try {
            Player player = Minecraft.getInstance().player;
            if (player == null) return false;

            return isVanishingPumpkin(player.getItemBySlot(EquipmentSlot.HEAD));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldHidePumpkinHeadOverlay(EquipmentSlot slot, ItemStack stack, boolean enabled) {
        if (!enabled || slot != EquipmentSlot.HEAD) return false;

        try {
            return isVanishingPumpkin(stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldSuppressEquippableComponent(ItemStack stack, DataComponentType<?> componentType, boolean enabled) {
        if (!enabled || componentType != DataComponents.EQUIPPABLE) return false;

        try {
            return isVanishingPumpkin(stack);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Equippable withoutCameraOverlay(Equippable equippable) {
        if (equippable == null || equippable.cameraOverlay().isEmpty()) {
            return equippable;
        }

        return new Equippable(
                equippable.slot(),
                equippable.equipSound(),
                equippable.assetId(),
                Optional.empty(),
                equippable.allowedEntities(),
                equippable.dispensable(),
                equippable.swappable(),
                equippable.damageOnHurt(),
                equippable.equipOnInteract(),
                equippable.canBeSheared(),
                equippable.shearingSound()
        );
    }

    public static boolean shouldHidePumpkinOverlay(Identifier overlayTex, boolean enabled) {
        if (!enabled || overlayTex == null) return false;

        try {
            Player player = Minecraft.getInstance().player;
            if (player == null) return false;

            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!isVanishingPumpkin(head)) return false;

            if (isPumpkinOverlayId(overlayTex)) {
                return true;
            }

            return matchesHeadCameraOverlay(head, overlayTex) || isLegacyPumpkinOverlay(overlayTex);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldHidePumpkinCameraOverlayId(Identifier overlayId, boolean enabled) {
        if (!enabled || overlayId == null) return false;

        try {
            Player player = Minecraft.getInstance().player;
            if (player == null) return false;

            return isVanishingPumpkin(player.getItemBySlot(EquipmentSlot.HEAD)) && isPumpkinOverlayId(overlayId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isVanishingPumpkin(ItemStack head) {
        if (head.isEmpty() || head.getItem() != Items.CARVED_PUMPKIN) return false;

        ItemEnchantments enchants = head.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var holder : enchants.keySet()) {
            if (holder.is(Enchantments.VANISHING_CURSE)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesHeadCameraOverlay(ItemStack head, Identifier overlayTex) {
        Equippable equippable = head.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return false;

        return equippable.cameraOverlay()
                .map(cameraOverlay -> matchesCameraOverlayId(cameraOverlay, overlayTex))
                .orElse(false);
    }

    private static boolean matchesCameraOverlayId(Identifier configuredOverlay, Identifier renderedOverlay) {
        if (!configuredOverlay.getNamespace().equals(renderedOverlay.getNamespace())) return false;

        String configuredPath = configuredOverlay.getPath();
        String renderedPath = renderedOverlay.getPath();
        String normalizedConfiguredPath = normalizeOverlayPath(configuredPath);
        String normalizedRenderedPath = normalizeOverlayPath(renderedPath);

        return configuredPath.equals(renderedPath)
                || normalizedConfiguredPath.equals(renderedPath)
                || configuredPath.equals(normalizedRenderedPath)
                || normalizedConfiguredPath.equals(normalizedRenderedPath);
    }

    private static String normalizeOverlayPath(String path) {
        String normalized = path;
        if (!normalized.startsWith("textures/")) {
            normalized = "textures/" + normalized;
        }
        if (!normalized.endsWith(".png")) {
            normalized = normalized + ".png";
        }
        return normalized;
    }

    private static boolean isLegacyPumpkinOverlay(Identifier overlayTex) {
        return "minecraft".equals(overlayTex.getNamespace())
                && "textures/misc/pumpkinblur.png".equals(overlayTex.getPath());
    }

    private static boolean isPumpkinOverlayId(Identifier overlayTex) {
        if (overlayTex == null) {
            return false;
        }

        String path = overlayTex.getPath();
        String normalizedPath = normalizeOverlayPath(path);
        return isLegacyPumpkinOverlay(overlayTex)
                || "textures/misc/pumpkinblur.png".equals(normalizedPath)
                || path.contains("pumpkin");
    }
}