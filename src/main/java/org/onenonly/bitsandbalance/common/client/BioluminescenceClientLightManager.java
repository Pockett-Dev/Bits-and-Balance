package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceFadeHelper;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BioluminescenceClientLightManager {
    private static final double UPDATE_THRESHOLD = 0.025D;
    private static final double LIGHT_LEVEL_UPDATE_THRESHOLD = 1.0D / 64.0D;
    private static final int LIGHTMAP_BLOCK_MASK = 0x000fffff;
    private static final Map<Integer, TrackedLightSource> TRACKED_SOURCES = new HashMap<>();
    private static final Map<Integer, SyncedBioluminescenceState> SYNCED_BIOLUMINESCENCE = new HashMap<>();
    private static volatile TrackedLightSource[] ACTIVE_SOURCES = new TrackedLightSource[0];

    private static ClientLevel trackedLevel;

    private BioluminescenceClientLightManager() {
    }

    public static void tickClient() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.levelRenderer == null) {
            clearTrackedSources();
            trackedLevel = null;
            return;
        }

        if (trackedLevel != level) {
            clearTrackedSources();
            trackedLevel = level;
        }

        expireSyncedBioluminescence(level.getGameTime());

        for (Entity entity : level.entitiesForRendering()) {
            observeEntity(entity, true);
        }

        if (client.player != null) {
            observeEntity(client.player, true);
        }
        if (client.getCameraEntity() instanceof LivingEntity cameraLiving && cameraLiving != client.player) {
            observeEntity(cameraLiving, true);
        }

        Iterator<Map.Entry<Integer, TrackedLightSource>> iterator = TRACKED_SOURCES.entrySet().iterator();
        boolean sourcesChanged = false;
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedLightSource> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity livingEntity)) {
                scheduleSectionsForSource(entry.getValue().x, entry.getValue().y, entry.getValue().z, entry.getValue().range);
                iterator.remove();
                sourcesChanged = true;
                continue;
            }

            TrackedLightValues lightValues = getTrackedLightValues(livingEntity);
            if (lightValues.lightLevel <= 0.0D || lightValues.range <= 0.0D) {
                scheduleSectionsForSource(entry.getValue().x, entry.getValue().y, entry.getValue().z, entry.getValue().range);
                iterator.remove();
                sourcesChanged = true;
                continue;
            }

            TrackedLightSource previous = entry.getValue();
            TrackedLightSource updated = new TrackedLightSource(
                    livingEntity.getX(),
                    livingEntity.getY(0.5D),
                    livingEntity.getZ(),
                    lightValues.lightLevel,
                    lightValues.range
            );
            if (!previous.hasChanged(updated.x, updated.y, updated.z, updated.lightLevel, updated.range)) {
                continue;
            }

            scheduleSectionsForSource(previous.x, previous.y, previous.z, previous.range);
            entry.setValue(updated);
            scheduleSectionsForSource(updated.x, updated.y, updated.z, updated.range);
            sourcesChanged = true;
        }

        if (sourcesChanged) {
            refreshActiveSources();
        }
    }

    public static void observeEntity(Entity entity) {
        observeEntity(entity, false);
    }

    private static void observeEntity(Entity entity, boolean scheduleRebuilds) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        int entityId = entity.getId();
        TrackedLightValues lightValues = getTrackedLightValues(livingEntity);
        if (lightValues.lightLevel <= 0.0D || lightValues.range <= 0.0D) {
            TrackedLightSource removed = TRACKED_SOURCES.remove(entityId);
            if (removed != null) {
                if (scheduleRebuilds) {
                    scheduleSectionsForSource(removed.x, removed.y, removed.z, removed.range);
                }
                refreshActiveSources();
            }
            return;
        }

        double x = entity.getX();
        double y = entity.getY(0.5D);
        double z = entity.getZ();
        TrackedLightSource tracked = TRACKED_SOURCES.get(entityId);
        if (tracked == null) {
            TRACKED_SOURCES.put(entityId, new TrackedLightSource(x, y, z, lightValues.lightLevel, lightValues.range));
            if (scheduleRebuilds) {
                scheduleSectionsForSource(x, y, z, lightValues.range);
            }
            refreshActiveSources();
            return;
        }

        if (!tracked.hasChanged(x, y, z, lightValues.lightLevel, lightValues.range)) {
            return;
        }

        if (scheduleRebuilds) {
            scheduleSectionsForSource(tracked.x, tracked.y, tracked.z, tracked.range);
        }
        TrackedLightSource updated = new TrackedLightSource(x, y, z, lightValues.lightLevel, lightValues.range);
        TRACKED_SOURCES.put(entityId, updated);
        if (scheduleRebuilds) {
            scheduleSectionsForSource(updated.x, updated.y, updated.z, updated.range);
        }
        refreshActiveSources();
    }

    public static int getLightmapWithDynamicLight(BlockPos pos, int lightmap) {
        return getLightmapWithDynamicLight(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, lightmap);
    }

    public static int getLightmapWithDynamicLight(BlockAndTintGetter level, BlockPos pos, int lightmap) {
        return getLightmapWithDynamicLight(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, lightmap);
    }

    public static int getLightmapWithDynamicLight(double sampleX, double sampleY, double sampleZ, int lightmap) {
        return getLightmapWithDynamicLight(null, sampleX, sampleY, sampleZ, lightmap);
    }

    public static int getLightmapWithDynamicLight(BlockAndTintGetter level, double sampleX, double sampleY, double sampleZ, int lightmap) {
        return getLightmapWithDynamicLight(level, sampleX, sampleY, sampleZ, null, lightmap);
    }

    public static int getLightmapWithDynamicLight(BlockAndTintGetter level, double sampleX, double sampleY, double sampleZ, Direction faceDirection, int lightmap) {
        double dynamicLightLevel = getDynamicLightLevel(sampleX, sampleY, sampleZ);
        if (dynamicLightLevel <= 0.0D) {
            return lightmap;
        }

        int existingBlockLight = getExistingBlockLightLevel(level, sampleX, sampleY, sampleZ, faceDirection, lightmap);
        if (dynamicLightLevel <= existingBlockLight) {
            return lightmap;
        }

        int luminance = Mth.clamp((int) (dynamicLightLevel * 16.0D), 0, 240);
        int existingLuminance = existingBlockLight << 4;
        if (luminance <= existingLuminance) {
            return lightmap;
        }

        return (lightmap & ~LIGHTMAP_BLOCK_MASK) | (luminance & LIGHTMAP_BLOCK_MASK);
    }

    private static int getExistingBlockLightLevel(BlockAndTintGetter level, double sampleX, double sampleY, double sampleZ, Direction faceDirection, int lightmap) {
        int existing = (lightmap >> 4) & 15;
        if (level == null) {
            return existing;
        }

        BlockPos samplePos = BlockPos.containing(sampleX, sampleY, sampleZ);
        existing = Math.max(existing, level.getBrightness(LightLayer.BLOCK, samplePos));
        if (faceDirection == null) {
            return existing;
        }

        double offsetX = faceDirection.getStepX() * 0.001D;
        double offsetY = faceDirection.getStepY() * 0.001D;
        double offsetZ = faceDirection.getStepZ() * 0.001D;
        BlockPos outsidePos = BlockPos.containing(sampleX + offsetX, sampleY + offsetY, sampleZ + offsetZ);
        BlockPos insidePos = BlockPos.containing(sampleX - offsetX, sampleY - offsetY, sampleZ - offsetZ);
        existing = Math.max(existing, level.getBrightness(LightLayer.BLOCK, outsidePos));
        return Math.max(existing, level.getBrightness(LightLayer.BLOCK, insidePos));
    }

    public static double getDynamicLightLevel(BlockPos pos) {
        return getDynamicLightLevel(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    public static double getDynamicLightLevel(double sampleX, double sampleY, double sampleZ) {
        TrackedLightSource[] sources = ACTIVE_SOURCES;
        double ponderLight = PonderLightAuditClient.getDynamicLightLevel(sampleX, sampleY, sampleZ);
        if (sources.length == 0 || !GlowGooRuntime.enabled || !GlowGooRuntime.bioluminescenceEnabled) {
            return ponderLight;
        }

        double brightest = ponderLight;

        for (TrackedLightSource source : sources) {
            double lightLevel = getLightAt(source, sampleX, sampleY, sampleZ);
            if (lightLevel > brightest) {
                brightest = lightLevel;
            }
        }

        return Mth.clamp(brightest, 0.0D, 15.0D);
    }

    public static int getEntityLuminance(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return 0;
        }
        return getEntityLuminance(livingEntity);
    }

    public static int getEntityLuminance(LivingEntity entity) {
        return Mth.clamp(Mth.ceil(getEntityDynamicLuminance(entity)), 0, 15);
    }

    public static double getEntityDynamicLuminance(LivingEntity entity) {
        return getTrackedLightValues(entity).lightLevel;
    }

    public static void syncClientBioluminescence(int entityId, int remainingTicks, int amplifier) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        if (remainingTicks <= 0) {
            SYNCED_BIOLUMINESCENCE.remove(entityId);
            return;
        }

        SYNCED_BIOLUMINESCENCE.put(entityId, new SyncedBioluminescenceState(level.getGameTime() + remainingTicks, Math.max(0, amplifier)));
    }

    public static int getSyncedRemainingDuration(int entityId) {
        ClientLevel level = trackedLevel;
        if (level == null) {
            return 0;
        }

        SyncedBioluminescenceState syncedState = SYNCED_BIOLUMINESCENCE.get(entityId);
        if (syncedState == null) {
            return 0;
        }

        return Math.max(0, (int) (syncedState.expiryTick - level.getGameTime()));
    }

    public static int getSyncedAmplifier(int entityId) {
        SyncedBioluminescenceState syncedState = SYNCED_BIOLUMINESCENCE.get(entityId);
        return syncedState != null ? syncedState.amplifier : 0;
    }

    private static double getLightAt(TrackedLightSource source, double sampleX, double sampleY, double sampleZ) {
        if (source.lightLevel <= 0.0D || source.range <= 0.0D) {
            return 0.0D;
        }

        double deltaX = sampleX - source.x;
        double deltaY = sampleY - source.y;
        double deltaZ = sampleZ - source.z;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (distance >= source.range) {
            return 0.0D;
        }

        return source.lightLevel * (1.0D - (distance / source.range));
    }

    private static double getDesiredLightLevel(int remainingDurationTicks) {
        return BioluminescenceFadeHelper.getDesiredLightLevel(remainingDurationTicks, GlowGooRuntime.bioluminescenceLightLevel);
    }

    private static double getDesiredLightRange(int remainingDurationTicks, int amplifier) {
        double baseRange = GlowGooRuntime.bioluminescenceLightLevel * (1.0D + Math.max(0, amplifier));
        return BioluminescenceFadeHelper.getDesiredValue(remainingDurationTicks, baseRange);
    }

    private static void clearTrackedSources() {
        TRACKED_SOURCES.clear();
        SYNCED_BIOLUMINESCENCE.clear();
        ACTIVE_SOURCES = new TrackedLightSource[0];
    }

    private static void expireSyncedBioluminescence(long gameTime) {
        SYNCED_BIOLUMINESCENCE.entrySet().removeIf(entry -> entry.getValue().expiryTick <= gameTime);
    }

    private static void refreshActiveSources() {
        ACTIVE_SOURCES = TRACKED_SOURCES.values().toArray(TrackedLightSource[]::new);
    }

    private static void scheduleSectionsForSource(double x, double y, double z, double range) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !(client.levelRenderer instanceof BioluminescenceLevelRendererAccess access) || range <= 0.0D) {
            return;
        }

        int radius = Math.max(1, Mth.ceil(range));
        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(x - radius));
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(x + radius));
        int minSectionY = Math.max(SectionPos.blockToSectionCoord(getLevelMinY(client.level)), SectionPos.blockToSectionCoord(Mth.floor(y - radius)));
        int maxSectionY = Math.min(SectionPos.blockToSectionCoord(getLevelMaxY(client.level) - 1), SectionPos.blockToSectionCoord(Mth.floor(y + radius)));
        int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(z - radius));
        int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(z + radius));

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    access.bitsandbalance$scheduleChunkRebuild(sectionX, sectionY, sectionZ, false);
                }
            }
        }
    }

    private static TrackedLightValues getTrackedLightValues(LivingEntity entity) {
        if (!GlowGooRuntime.enabled || !GlowGooRuntime.bioluminescenceEnabled || GlowGooRuntime.bioluminescenceLightLevel <= 0) {
            return TrackedLightValues.NONE;
        }

        int remainingDurationTicks = BioluminescenceLightManager.getBioluminescenceRemainingTicks(entity);
        if (remainingDurationTicks <= 0 || entity.isRemoved() || !entity.isAlive()) {
            return TrackedLightValues.NONE;
        }

        int amplifier = BioluminescenceLightManager.getBioluminescenceAmplifier(entity);
        double lightLevel = getDesiredLightLevel(remainingDurationTicks);
        double range = getDesiredLightRange(remainingDurationTicks, amplifier);
        if (lightLevel <= 0.0D || range <= 0.0D) {
            return TrackedLightValues.NONE;
        }

        return new TrackedLightValues(lightLevel, range);
    }

    private static int getLevelMinY(Object level) {
        Integer value = invokeIntNoArgs(level, "getMinY");
        if (value != null) {
            return value;
        }
        value = invokeIntNoArgs(level, "getMinBuildHeight");
        return value != null ? value : -64;
    }

    private static int getLevelMaxY(Object level) {
        Integer value = invokeIntNoArgs(level, "getMaxY");
        if (value != null) {
            return value;
        }
        value = invokeIntNoArgs(level, "getMaxBuildHeight");
        return value != null ? value : 320;
    }

    private static Integer invokeIntNoArgs(Object owner, String methodName) {
        if (owner == null) {
            return null;
        }
        try {
            Object value = owner.getClass().getMethod(methodName).invoke(owner);
            return value instanceof Number number ? number.intValue() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static final class TrackedLightSource {
        private final double x;
        private final double y;
        private final double z;
        private final double lightLevel;
        private final double range;

        private TrackedLightSource(double x, double y, double z, double lightLevel, double range) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lightLevel = lightLevel;
            this.range = range;
        }

        private boolean hasChanged(double x, double y, double z, double lightLevel, double range) {
            return Math.abs(this.x - x) > UPDATE_THRESHOLD
                    || Math.abs(this.y - y) > UPDATE_THRESHOLD
                    || Math.abs(this.z - z) > UPDATE_THRESHOLD
                    || Math.abs(this.lightLevel - lightLevel) > LIGHT_LEVEL_UPDATE_THRESHOLD
                    || Math.abs(this.range - range) > LIGHT_LEVEL_UPDATE_THRESHOLD;
        }
    }

    private record TrackedLightValues(double lightLevel, double range) {
        private static final TrackedLightValues NONE = new TrackedLightValues(0.0D, 0.0D);
    }

    private static final class SyncedBioluminescenceState {
        private final long expiryTick;
        private final int amplifier;

        private SyncedBioluminescenceState(long expiryTick, int amplifier) {
            this.expiryTick = expiryTick;
            this.amplifier = amplifier;
        }
    }
}
