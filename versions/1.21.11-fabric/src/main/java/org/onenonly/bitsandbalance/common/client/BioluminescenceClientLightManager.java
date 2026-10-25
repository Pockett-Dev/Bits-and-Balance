package org.onenonly.bitsandbalance.common.client;

import net.fabricmc.loader.api.FabricLoader;
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
    private static final double LAMB_DYNAMIC_LIGHTS_MAX_RADIUS = 7.75D;
    private static final int LIGHTMAP_BLOCK_MASK = 0x000fffff;
    private static final Map<Integer, TrackedLightSource> TRACKED_SOURCES = new HashMap<>();
    private static final Map<Integer, SyncedBioluminescenceState> SYNCED_BIOLUMINESCENCE = new HashMap<>();
    private static volatile TrackedLightSource[] ACTIVE_SOURCES = new TrackedLightSource[0];
    private static volatile LightSourceSnapshot[] ACTIVE_SOURCE_SNAPSHOTS = new LightSourceSnapshot[0];
    private static volatile ActiveLightBounds ACTIVE_SOURCE_BOUNDS = ActiveLightBounds.EMPTY;
    private static volatile long ACTIVE_SOURCE_REVISION;

    private static ClientLevel trackedLevel;

    private BioluminescenceClientLightManager() {
    }

    public static void tickClient() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.levelRenderer == null) {
            clearTrackedSources();
            BioluminescenceLambDynamicLightsBridge.resetRegistration();
            trackedLevel = null;
            return;
        }

        if (trackedLevel != level) {
            clearTrackedSources();
            BioluminescenceLambDynamicLightsBridge.resetRegistration();
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

        boolean preciseDynamicLightTracking = shouldUsePreciseDynamicLightTracking();
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
            if (!previous.hasChanged(updated.x, updated.y, updated.z, updated.lightLevel, updated.range, preciseDynamicLightTracking)) {
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

        BioluminescenceLambDynamicLightsBridge.tick();
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
        boolean preciseDynamicLightTracking = shouldUsePreciseDynamicLightTracking();
        TrackedLightSource tracked = TRACKED_SOURCES.get(entityId);
        if (tracked == null) {
            TRACKED_SOURCES.put(entityId, new TrackedLightSource(x, y, z, lightValues.lightLevel, lightValues.range));
            if (scheduleRebuilds) {
                scheduleSectionsForSource(x, y, z, lightValues.range);
            }
            refreshActiveSources();
            return;
        }

        if (!tracked.hasChanged(x, y, z, lightValues.lightLevel, lightValues.range, preciseDynamicLightTracking)) {
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

    static long getActiveSourceRevision() {
        return ACTIVE_SOURCE_REVISION;
    }

    static ActiveLightBounds getActiveSourceBounds() {
        return ACTIVE_SOURCE_BOUNDS;
    }

    static LightSourceSnapshot[] getActiveSourceSnapshots() {
        PonderLightAuditClient.LightSource[] ponder = PonderLightAuditClient.getLightSources();
        if (ponder.length == 0) {
            return ACTIVE_SOURCE_SNAPSHOTS;
        }
        LightSourceSnapshot[] combined = java.util.Arrays.copyOf(ACTIVE_SOURCE_SNAPSHOTS,
                ACTIVE_SOURCE_SNAPSHOTS.length + ponder.length);
        for (int i = 0; i < ponder.length; i++) {
            PonderLightAuditClient.LightSource source = ponder[i];
            combined[ACTIVE_SOURCE_SNAPSHOTS.length + i] = new LightSourceSnapshot(
                    Integer.MIN_VALUE + source.id(), source.x(), source.y(), source.z(),
                    PonderLightAuditClient.MARKER_LIGHT_LEVEL,
                    PonderLightAuditClient.MARKER_LIGHT_RANGE, source.revision());
        }
        return combined;
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
        ACTIVE_SOURCE_SNAPSHOTS = new LightSourceSnapshot[0];
        ACTIVE_SOURCE_BOUNDS = ActiveLightBounds.EMPTY;
        ACTIVE_SOURCE_REVISION++;
    }

    private static void expireSyncedBioluminescence(long gameTime) {
        SYNCED_BIOLUMINESCENCE.entrySet().removeIf(entry -> entry.getValue().expiryTick <= gameTime);
    }

    private static void refreshActiveSources() {
        TrackedLightSource[] sources = new TrackedLightSource[TRACKED_SOURCES.size()];
        LightSourceSnapshot[] snapshots = new LightSourceSnapshot[TRACKED_SOURCES.size()];
        int index = 0;
        for (Map.Entry<Integer, TrackedLightSource> entry : TRACKED_SOURCES.entrySet()) {
            TrackedLightSource source = entry.getValue();
            sources[index] = source;
            snapshots[index] = new LightSourceSnapshot(
                    entry.getKey(),
                    source.x,
                    source.y,
                    source.z,
                    source.lightLevel,
                    source.range,
                    ACTIVE_SOURCE_REVISION + 1L
            );
            index++;
        }
        ACTIVE_SOURCES = sources;
        ACTIVE_SOURCE_SNAPSHOTS = snapshots;
        ACTIVE_SOURCE_BOUNDS = ActiveLightBounds.from(sources);
        ACTIVE_SOURCE_REVISION++;
    }

    private static void scheduleSectionsForSource(double x, double y, double z, double range) {
        if (isLambDynamicLightsLoaded()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !(client.levelRenderer instanceof BioluminescenceLevelRendererAccess access) || range <= 0.0D) {
            return;
        }

        int radius = Math.max(1, Mth.ceil(range));
        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(x - radius)) - 1;
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(x + radius)) + 1;
        int minSectionY = Math.max(SectionPos.blockToSectionCoord(getLevelMinY(client.level)), SectionPos.blockToSectionCoord(Mth.floor(y - radius)) - 1);
        int maxSectionY = Math.min(SectionPos.blockToSectionCoord(getLevelMaxY(client.level) - 1), SectionPos.blockToSectionCoord(Mth.floor(y + radius)) + 1);
        int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(z - radius)) - 1;
        int maxSectionZ = SectionPos.blockToSectionCoord(Mth.floor(z + radius)) + 1;

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                    access.bitsandbalance$scheduleChunkRebuild(sectionX, sectionY, sectionZ, true);
                }
            }
        }
    }

    private static boolean isLambDynamicLightsLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded("lambdynlights");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean shouldUsePreciseDynamicLightTracking() {
        return isLambDynamicLightsLoaded();
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

        private boolean hasChanged(double x, double y, double z, double lightLevel, double range, boolean preciseTracking) {
            double positionThreshold = preciseTracking ? 0.0D : UPDATE_THRESHOLD;
            double lightThreshold = preciseTracking ? 0.0D : LIGHT_LEVEL_UPDATE_THRESHOLD;
            return Math.abs(this.x - x) > positionThreshold
                    || Math.abs(this.y - y) > positionThreshold
                    || Math.abs(this.z - z) > positionThreshold
                    || Math.abs(this.lightLevel - lightLevel) > lightThreshold
                    || Math.abs(this.range - range) > lightThreshold;
        }
    }

    record ActiveLightBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private static final ActiveLightBounds EMPTY = new ActiveLightBounds(0, 0, 0, 0, 0, 0);

        private static ActiveLightBounds from(TrackedLightSource[] sources) {
            if (sources.length == 0) {
                return EMPTY;
            }

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (TrackedLightSource source : sources) {
                double range = Math.max(0.0D, source.range);
                minX = Math.min(minX, Mth.floor(source.x - range));
                minY = Math.min(minY, Mth.floor(source.y - range));
                minZ = Math.min(minZ, Mth.floor(source.z - range));
                maxX = Math.max(maxX, Mth.ceil(source.x + range));
                maxY = Math.max(maxY, Mth.ceil(source.y + range));
                maxZ = Math.max(maxZ, Mth.ceil(source.z + range));
            }

            return new ActiveLightBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    record LightSourceSnapshot(int entityId, double x, double y, double z, double lightLevel, double range, long revision) {
        double lightAt(BlockPos pos, double falloffRatio) {
            if (this.lightLevel <= 0.0D || this.range <= 0.0D) {
                return 0.0D;
            }

            double deltaX = (pos.getX() + 0.5D) - this.x;
            double deltaY = (pos.getY() + 0.5D) - this.y;
            double deltaZ = (pos.getZ() + 0.5D) - this.z;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (distance >= this.range) {
                return 0.0D;
            }

            return Mth.clamp(this.lightLevel * (1.0D - (distance / this.range)), 0.0D, 15.0D);
        }

        ActiveLightBounds bounds() {
            double indexedCoreRadius = Math.max(0.0D, this.range - LAMB_DYNAMIC_LIGHTS_MAX_RADIUS);
            int blockX = Mth.floor(this.x);
            int blockY = Mth.floor(this.y);
            int blockZ = Mth.floor(this.z);
            return new ActiveLightBounds(
                    Mth.floor(this.x - indexedCoreRadius),
                    Mth.floor(this.y - indexedCoreRadius),
                    Mth.floor(this.z - indexedCoreRadius),
                    Math.max(blockX + 1, Mth.ceil(this.x + indexedCoreRadius)),
                    Math.max(blockY + 1, Mth.ceil(this.y + indexedCoreRadius)),
                    Math.max(blockZ + 1, Mth.ceil(this.z + indexedCoreRadius))
            );
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
