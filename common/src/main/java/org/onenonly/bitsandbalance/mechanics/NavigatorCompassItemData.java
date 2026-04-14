package org.onenonly.bitsandbalance.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;

/**
 * Stores Navigator Compass state on a vanilla compass via DataComponents.CUSTOM_DATA.
 *
 * This avoids using the vanilla LODESTONE_TRACKER component, which makes the compass behave like a
 * Lodestone Compass and can interact with real lodestones.
 */
public final class NavigatorCompassItemData {
    private NavigatorCompassItemData() {
    }

    private static final String KEY_TARGET_X = "bitsandbalance.navigator_compass_target_x";
    private static final String KEY_TARGET_Y = "bitsandbalance.navigator_compass_target_y";
    private static final String KEY_TARGET_Z = "bitsandbalance.navigator_compass_target_z";
    private static final String KEY_TARGET_DIM = "bitsandbalance.navigator_compass_target_dim";

    // 0 = coords, 1 = distance
    private static final String KEY_MODE = "bitsandbalance.navigator_compass_mode";

    public static boolean isCompass(ItemStack stack) {
        return stack != null && stack.is(Items.COMPASS);
    }

    public static boolean hasTarget(ItemStack stack) {
        return getTargetPos(stack) != null && getTargetDimensionId(stack) != null;
    }

    private static boolean hasCustomNavigatorTargetKeys(ItemStack stack) {
        if (!isCompass(stack)) return false;

        CompoundTag tag = getCustomTag(stack);
        if (tag == null) return false;

        return tag.contains(KEY_TARGET_X)
            && tag.contains(KEY_TARGET_Y)
            && tag.contains(KEY_TARGET_Z)
            && tag.contains(KEY_TARGET_DIM);
    }

    private static boolean isNavigatorCompassLike(ItemStack stack) {
        if (!isCompass(stack)) return false;

        CompoundTag tag = getCustomTag(stack);
        if (tag != null) {
            // If it has our target keys, it's definitely a Navigator Compass.
            if (tag.contains(KEY_TARGET_X)
                || tag.contains(KEY_TARGET_Y)
                || tag.contains(KEY_TARGET_Z)
                || tag.contains(KEY_TARGET_DIM)) {
                return true;
            }
        }

        // If it has our custom name, treat it as a Navigator Compass.
        // (A converted Lodestone Compass deliberately has its custom name removed.)
        try {
            var name = stack.get(DataComponents.CUSTOM_NAME);
            if (name != null && "Navigator Compass".equals(name.getString())) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    public static BlockPos getTargetPos(ItemStack stack) {
        if (!isCompass(stack)) return null;

        CompoundTag tag = getCustomTag(stack);
        if (tag != null && tag.contains(KEY_TARGET_X) && tag.contains(KEY_TARGET_Y) && tag.contains(KEY_TARGET_Z)) {
            int x = tag.getInt(KEY_TARGET_X).orElse(0);
            int y = tag.getInt(KEY_TARGET_Y).orElse(0);
            int z = tag.getInt(KEY_TARGET_Z).orElse(0);
            return new BlockPos(x, y, z);
        }

        // Legacy fallback: read lodestone tracker if present (server can migrate it).
        try {
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (tracker != null && tracker.target().isPresent()) {
                return tracker.target().get().pos();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static String getTargetDimensionId(ItemStack stack) {
        if (!isCompass(stack)) return null;

        CompoundTag tag = getCustomTag(stack);
        if (tag != null && tag.contains(KEY_TARGET_DIM)) {
            return tag.getStringOr(KEY_TARGET_DIM, "");
        }

        try {
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (tracker != null && tracker.target().isPresent()) {
                return tracker.target().get().dimension().identifier().toString();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static void setTarget(ItemStack stack, String dimensionId, BlockPos pos) {
        if (!isCompass(stack) || dimensionId == null || pos == null) return;

        // Remove lodestone tracker so it doesn't become a Lodestone Compass.
        try {
            stack.remove(DataComponents.LODESTONE_TRACKER);
        } catch (Throwable ignored) {
        }

        CompoundTag tag = getOrCreateCustomTag(stack);
        tag.putInt(KEY_TARGET_X, pos.getX());
        tag.putInt(KEY_TARGET_Y, pos.getY());
        tag.putInt(KEY_TARGET_Z, pos.getZ());
        tag.putString(KEY_TARGET_DIM, dimensionId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearTarget(ItemStack stack) {
        if (!isCompass(stack)) return;

        try {
            stack.remove(DataComponents.LODESTONE_TRACKER);
        } catch (Throwable ignored) {
        }

        CompoundTag tag = getCustomTag(stack);
        if (tag == null) return;

        tag.remove(KEY_TARGET_X);
        tag.remove(KEY_TARGET_Y);
        tag.remove(KEY_TARGET_Z);
        tag.remove(KEY_TARGET_DIM);

        if (tag.isEmpty()) {
            try {
                stack.remove(DataComponents.CUSTOM_DATA);
            } catch (Throwable ignored) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            }
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /**
     * Clears only the Navigator Compass custom target keys (x/y/z/dimension) while preserving
     * other custom-data fields like the coords/distance display mode.
     *
     * This does NOT touch {@link DataComponents#LODESTONE_TRACKER}.
     */
    public static void clearNavigatorTargetOnly(ItemStack stack) {
        if (!isCompass(stack)) return;

        CompoundTag tag = getCustomTag(stack);
        if (tag == null) return;

        tag.remove(KEY_TARGET_X);
        tag.remove(KEY_TARGET_Y);
        tag.remove(KEY_TARGET_Z);
        tag.remove(KEY_TARGET_DIM);

        if (tag.isEmpty()) {
            try {
                stack.remove(DataComponents.CUSTOM_DATA);
            } catch (Throwable ignored) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            }
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /**
     * Server-side helper: if the compass has a legacy lodestone tracker target, migrate it into
     * custom data and remove the lodestone tracker.
     */
    public static void migrateLegacyLodestoneTargetIfPresent(ItemStack stack) {
        if (!isCompass(stack)) return;

        // Important: do NOT migrate/clear real Lodestone Compasses.
        // Only compasses that already look like our Navigator Compass should be migrated.
        if (!isNavigatorCompassLike(stack)) return;

        // If we already have a custom Navigator target stored, ensure we remove any lodestone tracker.
        // (This prevents Navigator Compasses from behaving like Lodestone Compasses.)
        if (hasCustomNavigatorTargetKeys(stack)) {
            try {
                stack.remove(DataComponents.LODESTONE_TRACKER);
            } catch (Throwable ignored) {
            }
            return;
        }

        try {
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (tracker == null || tracker.target().isEmpty()) return;

            var target = tracker.target().get();
            setTarget(stack, target.dimension().identifier().toString(), target.pos());
        } catch (Throwable ignored) {
        }
    }

    public static boolean isDistanceMode(ItemStack stack) {
        if (!isCompass(stack)) return false;
        CompoundTag tag = getCustomTag(stack);
        if (tag == null || !tag.contains(KEY_MODE)) return false;
        int mode = tag.getInt(KEY_MODE).orElse(0);
        return mode == 1;
    }

    public static void toggleMode(ItemStack stack) {
        if (!isCompass(stack)) return;
        boolean distance = isDistanceMode(stack);
        setDistanceMode(stack, !distance);
    }

    public static void setDistanceMode(ItemStack stack, boolean distanceMode) {
        if (!isCompass(stack)) return;
        CompoundTag tag = getOrCreateCustomTag(stack);
        tag.putInt(KEY_MODE, distanceMode ? 1 : 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static CompoundTag getCustomTag(ItemStack stack) {
        try {
            CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
            if (custom == null) return null;
            return custom.copyTag();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static CompoundTag getOrCreateCustomTag(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        try {
            CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
            if (existing != null) {
                tag = existing.copyTag();
            }
        } catch (Throwable ignored) {
        }
        return tag;
    }
}
