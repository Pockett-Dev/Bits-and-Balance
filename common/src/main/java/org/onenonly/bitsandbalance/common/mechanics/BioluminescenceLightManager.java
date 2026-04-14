package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.client.BioluminescenceLevelRendererAccess;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BioluminescenceLightManager {
    private static final double UPDATE_THRESHOLD = 0.1D;
    private static final double LIGHT_LEVEL_UPDATE_THRESHOLD = 1.0D / 16.0D;
    private static final Map<Integer, TrackedLightSource> TRACKED_SOURCES = new HashMap<>();
    private static final Map<Integer, SyncedBioluminescenceState> SYNCED_BIOLUMINESCENCE = new HashMap<>();
    private static volatile TrackedLightSource[] ACTIVE_SOURCES = new TrackedLightSource[0];

    private static ClientLevel trackedLevel;

    private BioluminescenceLightManager() {
    }

    public static void tickClient(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.levelRenderer == null) {
            bitsandbalance$clearTrackedSources();
            trackedLevel = null;
            return;
        }

        if (trackedLevel != level) {
            bitsandbalance$clearTrackedSources();
            trackedLevel = level;
        }

        bitsandbalance$expireSyncedBioluminescence(level.getGameTime());

        for (Entity entity : level.entitiesForRendering()) {
            observeEntity(entity);
        }

        if (client.player != null) {
            observeEntity(client.player);
        }
        if (client.getCameraEntity() instanceof LivingEntity cameraLiving && cameraLiving != client.player) {
            observeEntity(cameraLiving);
        }

        Iterator<Map.Entry<Integer, TrackedLightSource>> iterator = TRACKED_SOURCES.entrySet().iterator();
        boolean sourcesChanged = false;
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedLightSource> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity livingEntity)) {
                bitsandbalance$scheduleSectionsForSource(entry.getValue().x, entry.getValue().y, entry.getValue().z, entry.getValue().range);
                iterator.remove();
                sourcesChanged = true;
                continue;
            }

            TrackedLightValues lightValues = bitsandbalance$getTrackedLightValues(livingEntity);
            if (lightValues.lightLevel <= 0.0D || lightValues.range <= 0.0D) {
                bitsandbalance$scheduleSectionsForSource(entry.getValue().x, entry.getValue().y, entry.getValue().z, entry.getValue().range);
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

            bitsandbalance$scheduleSectionsForSource(previous.x, previous.y, previous.z, previous.range);
            entry.setValue(updated);
            bitsandbalance$scheduleSectionsForSource(updated.x, updated.y, updated.z, updated.range);
            sourcesChanged = true;
        }

        if (sourcesChanged) {
            bitsandbalance$refreshActiveSources();
        }
    }

    public static void observeEntity(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        int entityId = entity.getId();
        TrackedLightValues lightValues = bitsandbalance$getTrackedLightValues(livingEntity);
        if (lightValues.lightLevel <= 0.0D || lightValues.range <= 0.0D) {
            TrackedLightSource removed = TRACKED_SOURCES.remove(entityId);
            if (removed != null) {
                bitsandbalance$scheduleSectionsForSource(removed.x, removed.y, removed.z, removed.range);
                bitsandbalance$refreshActiveSources();
            }
            return;
        }

        double x = entity.getX();
        double y = entity.getY(0.5D);
        double z = entity.getZ();
        TrackedLightSource tracked = TRACKED_SOURCES.get(entityId);
        if (tracked == null) {
            TRACKED_SOURCES.put(entityId, new TrackedLightSource(x, y, z, lightValues.lightLevel, lightValues.range));
            bitsandbalance$scheduleSectionsForSource(x, y, z, lightValues.range);
            bitsandbalance$refreshActiveSources();
            return;
        }

        if (!tracked.hasChanged(x, y, z, lightValues.lightLevel, lightValues.range)) {
            return;
        }

        bitsandbalance$scheduleSectionsForSource(tracked.x, tracked.y, tracked.z, tracked.range);
        TrackedLightSource updated = new TrackedLightSource(x, y, z, lightValues.lightLevel, lightValues.range);
        TRACKED_SOURCES.put(entityId, updated);
        bitsandbalance$scheduleSectionsForSource(updated.x, updated.y, updated.z, updated.range);
        bitsandbalance$refreshActiveSources();
    }

    public static int getLightmapWithDynamicLight(BlockPos pos, int lightmap) {
        double dynamicLightLevel = getDynamicLightLevel(pos);
        if (dynamicLightLevel <= 0.0D) {
            return lightmap;
        }

        int vanillaBlockLevel = (lightmap & 0x000fffff) >> 4;
        if (dynamicLightLevel <= vanillaBlockLevel) {
            return lightmap;
        }

        int luminance = (int) (dynamicLightLevel * 16.0D);
        return (lightmap & 0xfff00000) | (luminance & 0x000fffff);
    }

    public static double getDynamicLightLevel(BlockPos pos) {
        TrackedLightSource[] sources = ACTIVE_SOURCES;
        if (sources.length == 0 || !GlowGooRuntime.enabled || !GlowGooRuntime.bioluminescenceEnabled) {
            return 0.0D;
        }

        double sampleX = pos.getX() + 0.5D;
        double sampleY = pos.getY() + 0.5D;
        double sampleZ = pos.getZ() + 0.5D;
        double brightest = 0.0D;

        for (TrackedLightSource source : sources) {
            double lightLevel = bitsandbalance$getLightAt(source, sampleX, sampleY, sampleZ);
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
        return bitsandbalance$getTrackedLightValues(entity).lightLevel;
    }

    public static int getBioluminescenceRemainingTicks(LivingEntity entity) {
        MobEffectInstance bioluminescence = bitsandbalance$getBioluminescenceInstance(entity);
        if (bioluminescence != null) {
            return bioluminescence.getDuration();
        }

        return bitsandbalance$getSyncedRemainingDuration(entity.getId());
    }

    public static int getBioluminescenceAmplifier(LivingEntity entity) {
        MobEffectInstance bioluminescence = bitsandbalance$getBioluminescenceInstance(entity);
        if (bioluminescence != null) {
            return bioluminescence.getAmplifier();
        }

        return bitsandbalance$getSyncedAmplifier(entity.getId());
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

    private static MobEffectInstance bitsandbalance$getBioluminescenceInstance(LivingEntity entity) {
        return entity.getEffect(ModGlowGoo.bioluminescence());
    }

    private static double bitsandbalance$getLightAt(TrackedLightSource source, double sampleX, double sampleY, double sampleZ) {
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

    private static double bitsandbalance$getDesiredLightLevel(int remainingDurationTicks) {
        return BioluminescenceFadeHelper.getDesiredLightLevel(remainingDurationTicks, GlowGooRuntime.bioluminescenceLightLevel);
    }

    private static double bitsandbalance$getDesiredLightRange(int remainingDurationTicks, int amplifier) {
        double baseRange = GlowGooRuntime.bioluminescenceLightLevel * (Math.max(0, amplifier) + 1.0D);
        return BioluminescenceFadeHelper.getDesiredValue(remainingDurationTicks, baseRange);
    }

    private static void bitsandbalance$clearTrackedSources() {
        TRACKED_SOURCES.clear();
        SYNCED_BIOLUMINESCENCE.clear();
        ACTIVE_SOURCES = new TrackedLightSource[0];
    }

    private static int bitsandbalance$getClientVisibleRemainingDuration(LivingEntity entity) {
        return getBioluminescenceRemainingTicks(entity);
    }

    private static int bitsandbalance$getSyncedRemainingDuration(int entityId) {
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

    private static int bitsandbalance$getSyncedAmplifier(int entityId) {
        SyncedBioluminescenceState syncedState = SYNCED_BIOLUMINESCENCE.get(entityId);
        return syncedState != null ? syncedState.amplifier : 0;
    }

    private static void bitsandbalance$expireSyncedBioluminescence(long gameTime) {
        SYNCED_BIOLUMINESCENCE.entrySet().removeIf(entry -> entry.getValue().expiryTick <= gameTime);
    }

    private static void bitsandbalance$refreshActiveSources() {
        ACTIVE_SOURCES = TRACKED_SOURCES.values().toArray(TrackedLightSource[]::new);
    }

    private static void bitsandbalance$scheduleSectionsForSource(double x, double y, double z, double range) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !(client.levelRenderer instanceof BioluminescenceLevelRendererAccess access) || range <= 0.0D) {
            return;
        }

        int radius = Math.max(1, Mth.ceil(range));
        int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(x - radius));
        int maxSectionX = SectionPos.blockToSectionCoord(Mth.floor(x + radius));
        int minSectionY = Math.max(client.level.getMinSectionY(), SectionPos.blockToSectionCoord(Mth.floor(y - radius)));
        int maxSectionY = Math.min(client.level.getMaxSectionY() - 1, SectionPos.blockToSectionCoord(Mth.floor(y + radius)));
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

    private static TrackedLightValues bitsandbalance$getTrackedLightValues(LivingEntity entity) {
        if (!GlowGooRuntime.enabled || !GlowGooRuntime.bioluminescenceEnabled || GlowGooRuntime.bioluminescenceLightLevel <= 0) {
            return TrackedLightValues.NONE;
        }

        int remainingDurationTicks = bitsandbalance$getClientVisibleRemainingDuration(entity);
        if (remainingDurationTicks <= 0 || entity.isRemoved() || !entity.isAlive()) {
            return TrackedLightValues.NONE;
        }

        int amplifier = getBioluminescenceAmplifier(entity);
        double lightLevel = bitsandbalance$getDesiredLightLevel(remainingDurationTicks);
        double range = bitsandbalance$getDesiredLightRange(remainingDurationTicks, amplifier);
        if (lightLevel <= 0.0D || range <= 0.0D) {
            return TrackedLightValues.NONE;
        }

        return new TrackedLightValues(lightLevel, range);
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