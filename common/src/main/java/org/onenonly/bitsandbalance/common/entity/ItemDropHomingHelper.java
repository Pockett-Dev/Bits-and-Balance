package org.onenonly.bitsandbalance.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ItemDropHomingHelper {
    private static final String DROP_HOME_UUID_PREFIX = "bitsandbalance:drop_home_uuid:";
    private static final String DROP_HOME_EXP_PREFIX = "bitsandbalance:drop_home_exp:";
    private static Field entityNoPhysicsField;
    private static boolean entityNoPhysicsFieldResolved;

    private ItemDropHomingHelper() {
    }

    public static void popExactResource(Level level, BlockPos pos, ItemStack stack, @Nullable Player player, long durationTicks) {
        if (level.isClientSide() || stack.isEmpty()) {
            return;
        }

        Vec3 spawnPos = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D);
        ItemEntity itemEntity = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, stack.copy());
        itemEntity.setDefaultPickUpDelay();

        Vec3 velocity = Vec3.ZERO;
        if (player != null && player.isAlive()) {
            Vec3 toPlayer = player.position().subtract(spawnPos);
            Vec3 horizontal = new Vec3(toPlayer.x, 0.0D, toPlayer.z);
            double horizontalLength = horizontal.length();
            if (horizontalLength > 1.0E-4D) {
                Vec3 horizontalDir = horizontal.scale(1.0D / horizontalLength);
                velocity = new Vec3(horizontalDir.x * 0.18D, 0.0D, horizontalDir.z * 0.18D);
            }
        }

        itemEntity.setDeltaMovement(velocity);
        markDropHomeToPlayer(itemEntity, player, durationTicks);
        level.addFreshEntity(itemEntity);
    }

    public static void markDropHomeToPlayer(ItemEntity itemEntity, @Nullable Player player, long durationTicks) {
        if (itemEntity == null || player == null || !player.isAlive() || durationTicks <= 0L) {
            return;
        }
        if (!(itemEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        clearDropHoming(itemEntity);
        itemEntity.setNoGravity(true);
        setDropNoPhysics(itemEntity, true);
        itemEntity.addTag(DROP_HOME_UUID_PREFIX + player.getUUID());
        itemEntity.addTag(DROP_HOME_EXP_PREFIX + (serverLevel.getGameTime() + durationTicks));
    }

    public static @Nullable String getDropHomeUuid(ItemEntity itemEntity) {
        for (String tag : getEntityTags(itemEntity)) {
            if (tag.startsWith(DROP_HOME_UUID_PREFIX)) {
                String uuid = tag.substring(DROP_HOME_UUID_PREFIX.length());
                return uuid.isEmpty() ? null : uuid;
            }
        }
        return null;
    }

    public static long getDropHomeExpiresAt(ItemEntity itemEntity) {
        for (String tag : getEntityTags(itemEntity)) {
            if (tag.startsWith(DROP_HOME_EXP_PREFIX)) {
                String raw = tag.substring(DROP_HOME_EXP_PREFIX.length());
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    public static void clearDropHoming(ItemEntity itemEntity) {
        if (itemEntity == null) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        for (String tag : getEntityTags(itemEntity)) {
            if (tag.startsWith(DROP_HOME_UUID_PREFIX) || tag.startsWith(DROP_HOME_EXP_PREFIX)) {
                toRemove.add(tag);
            }
        }

        for (String tag : toRemove) {
            itemEntity.removeTag(tag);
        }
    }

    public static void setDropNoPhysics(ItemEntity itemEntity, boolean noPhysics) {
        if (itemEntity == null) {
            return;
        }

        Field field = getEntityNoPhysicsField(itemEntity.getClass());
        if (field == null) {
            return;
        }

        try {
            field.setBoolean(itemEntity, noPhysics);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static @Nullable Field getEntityNoPhysicsField(Class<?> startClass) {
        if (entityNoPhysicsFieldResolved) {
            return entityNoPhysicsField;
        }

        Class<?> current = startClass;
        while (current != null) {
            try {
                Field field = current.getDeclaredField("noPhysics");
                field.setAccessible(true);
                entityNoPhysicsField = field;
                entityNoPhysicsFieldResolved = true;
                return field;
            } catch (ReflectiveOperationException ignored) {
                current = current.getSuperclass();
            }
        }

        entityNoPhysicsFieldResolved = true;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getEntityTags(Entity entity) {
        if (entity == null) {
            return Collections.emptySet();
        }

        try {
            Method entityTagsMethod = entity.getClass().getMethod("entityTags");
            Object tags = entityTagsMethod.invoke(entity);
            if (tags instanceof Set<?> typedSet) {
                return (Set<String>) typedSet;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method getTagsMethod = entity.getClass().getMethod("getTags");
            Object tags = getTagsMethod.invoke(entity);
            if (tags instanceof Set<?> typedSet) {
                return (Set<String>) typedSet;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return Collections.emptySet();
    }
}