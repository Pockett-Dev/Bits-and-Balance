package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Private moving flames that suggest up to five well-spaced light placements. */
public final class PonderLightAuditClient {
    private static final int HORIZONTAL_RADIUS = 36;
    private static final int VERTICAL_RADIUS = 12;
    private static final int HOVER_TICKS = 200;
    private static final int AUDIT_TICKS = 100;
    public static final int MARKER_LIGHT_LEVEL = 7;
    public static final double MARKER_LIGHT_RANGE = 7.0D;
    private static final double LIGHT_REBUILD_DISTANCE_SQR = 2.25D;
    private static final double TRAVEL_BLOCKS_PER_TICK = 0.22D;
    private static ClientLevel level;
    private static List<Marker> markers = List.of();
    private static List<BlockPos> darkFloors = List.of();
    private static volatile LightSource[] lightSources = new LightSource[0];
    private static LightSource[] rebuiltLightSources = new LightSource[0];
    private static long lightRevision;
    private static int age;

    private PonderLightAuditClient() {
    }

    public static void start(double x, double y, double z, int emission) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        updateLightSources(new LightSource[0]);
        level = minecraft.level;
        Vec3 origin = new Vec3(x, y, z);
        int reach = Math.max(3, Math.min(15, emission));
        List<BlockPos> dark = findDarkFloors(minecraft, origin);
        darkFloors = dark.stream()
                .filter(pos -> Math.floorMod(pos.getX(), 3) == 0 && Math.floorMod(pos.getZ(), 3) == 0)
                .toList();
        if (darkFloors.isEmpty()) {
            darkFloors = dark;
        }
        age = 0;
        // Score all dark floor cells, but sample a placement grid in large rooms to keep the
        // one-time scan bounded even when several open floors are stacked vertically.
        List<BlockPos> candidates = dark.size() > 100 ? dark.stream()
                .filter(pos -> Math.floorMod(pos.getX(), 2) == 0 && Math.floorMod(pos.getZ(), 2) == 0)
                .toList() : dark;
        if (candidates.isEmpty()) {
            candidates = dark;
        }
        List<Marker> chosen = new ArrayList<>(5);
        boolean[] covered = new boolean[darkFloors.size()];
        for (int selection = 0; selection < 5; selection++) {
            BlockPos best = null;
            int bestScore = 0;
            for (BlockPos candidate : candidates) {
                if (chosen.stream().anyMatch(marker -> marker.pos.distSqr(candidate)
                        < (double) (reach - 1) * (reach - 1))) {
                    continue;
                }
                int score = 0;
                for (int i = 0; i < darkFloors.size(); i++) {
                    if (!covered[i] && manhattan(candidate, darkFloors.get(i)) < reach) {
                        score++;
                    }
                }
                if (score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
            if (best == null) {
                break;
            }
            chosen.add(new Marker(selection, best, origin));
            for (int i = 0; i < darkFloors.size(); i++) {
                if (manhattan(best, darkFloors.get(i)) < reach) {
                    covered[i] = true;
                }
            }
        }
        markers = chosen;
    }

    private static List<BlockPos> findDarkFloors(Minecraft minecraft, Vec3 castPoint) {
        ClientLevel world = minecraft.level;
        BlockPos origin = BlockPos.containing(castPoint);
        List<BlockPos> result = new ArrayList<>();
        for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
            for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
                for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
                    if (dx * dx + dz * dz > HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) {
                        continue;
                    }
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!world.hasChunkAt(pos) || !world.getBlockState(pos).isAir()
                            || !world.getBlockState(pos.above()).isAir()
                            || world.getBrightness(LightLayer.BLOCK, pos) > 0) {
                        continue;
                    }
                    BlockState floor = world.getBlockState(pos.below());
                    if (floor.isFaceSturdy(world, pos.below(), Direction.UP)
                            && Blocks.TORCH.defaultBlockState().canSurvive(world, pos)
                            && visible(minecraft, castPoint, center(pos))) {
                        result.add(pos.immutable());
                    }
                }
            }
        }
        return result;
    }

    private static boolean visible(Minecraft minecraft, Vec3 from, Vec3 to) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(BlockPos.containing(to));
    }

    private static int manhattan(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY())
                + Math.abs(a.getZ() - b.getZ());
    }

    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D);
    }

    public static void tick(Minecraft minecraft) {
        if (level == null) {
            return;
        }
        if (level != minecraft.level || minecraft.player == null) {
            updateLightSources(new LightSource[0]);
            level = null;
            markers = List.of();
            darkFloors = List.of();
            return;
        }
        if (age++ < AUDIT_TICKS && !darkFloors.isEmpty()) {
            // Cycle across sampled dark floors, with a strict per-tick particle budget.
            int count = Math.min(24, darkFloors.size());
            for (int i = 0; i < count; i++) {
                BlockPos pos = darkFloors.get((age * count + i) % darkFloors.size());
                if (level.getBrightness(LightLayer.BLOCK, pos) == 0 && level.getBlockState(pos).isAir()) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                            pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D,
                            0.0D, 0.0D, 0.0D);
                }
            }
        }
        List<Marker> active = new ArrayList<>(markers.size());
        List<LightSource> activeLights = new ArrayList<>(markers.size());
        for (Marker marker : markers) {
            if (level.getBrightness(LightLayer.BLOCK, marker.pos) > 0) {
                continue;
            }
            Vec3 destination = center(marker.pos);
            double distance = marker.origin.distanceTo(destination);
            if (marker.travel < distance) {
                marker.travel = Math.min(distance, marker.travel + TRAVEL_BLOCKS_PER_TICK);
                Vec3 point = marker.origin.lerp(destination, distance == 0.0D ? 1.0D : marker.travel / distance);
                if (marker.travel < distance) {
                    addFlame(point, marker);
                    activeLights.add(new LightSource(marker.id, point.x, point.y, point.z, ++lightRevision));
                    active.add(marker);
                    continue;
                }
            }
            if (marker.hover++ >= HOVER_TICKS) {
                continue;
            }
            double bob = Math.sin(marker.hover * 0.18D) * 0.08D;
            Vec3 point = destination.add(0.0D, bob, 0.0D);
            addFlame(point, marker);
            if (marker.hover % 5 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = (i + marker.hover * 0.04D) * Math.PI / 2.0D;
                    level.addParticle(ParticleTypes.FLAME,
                            destination.x + Math.cos(angle) * 0.22D, destination.y + bob,
                            destination.z + Math.sin(angle) * 0.22D, 0.0D, 0.0D, 0.0D);
                }
            }
            activeLights.add(new LightSource(marker.id, point.x, point.y, point.z, ++lightRevision));
            active.add(marker);
        }
        markers = active;
        updateLightSources(activeLights.toArray(LightSource[]::new));
        if (markers.isEmpty() && age >= AUDIT_TICKS) {
            level = null;
            darkFloors = List.of();
        }
    }

    private static void addFlame(Vec3 point, Marker marker) {
        level.addParticle(ParticleTypes.FLAME, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
        if ((age + marker.id) % 4 == 0) {
            level.addParticle(ParticleTypes.SMOKE, point.x, point.y + 0.06D, point.z,
                    0.0D, 0.005D, 0.0D);
        }
    }

    public static double getDynamicLightLevel(double x, double y, double z) {
        double brightest = 0.0D;
        for (LightSource source : lightSources) {
            double dx = x - source.x;
            double dy = y - source.y;
            double dz = z - source.z;
            double distanceSqr = dx * dx + dy * dy + dz * dz;
            if (distanceSqr < MARKER_LIGHT_RANGE * MARKER_LIGHT_RANGE) {
                brightest = Math.max(brightest,
                        MARKER_LIGHT_LEVEL * (1.0D - Math.sqrt(distanceSqr) / MARKER_LIGHT_RANGE));
            }
        }
        return brightest;
    }

    public static LightSource[] getLightSources() {
        return lightSources;
    }

    private static void updateLightSources(LightSource[] next) {
        LightSource[] previous = rebuiltLightSources;
        // Render rebuilds can start immediately, so publish the new light positions first.
        lightSources = next;
        for (LightSource old : previous) {
            LightSource replacement = findSource(next, old.id);
            if (replacement == null || distanceSqr(old, replacement) >= LIGHT_REBUILD_DISTANCE_SQR) {
                scheduleSections(old);
            }
        }
        LightSource[] rebuilt = new LightSource[next.length];
        for (int i = 0; i < next.length; i++) {
            LightSource source = next[i];
            LightSource old = findSource(previous, source.id);
            if (old == null || distanceSqr(old, source) >= LIGHT_REBUILD_DISTANCE_SQR) {
                scheduleSections(source);
                rebuilt[i] = source;
            } else {
                rebuilt[i] = old;
            }
        }
        rebuiltLightSources = rebuilt;
    }

    private static LightSource findSource(LightSource[] sources, int id) {
        for (LightSource source : sources) {
            if (source.id == id) {
                return source;
            }
        }
        return null;
    }

    private static double distanceSqr(LightSource a, LightSource b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void scheduleSections(LightSource source) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.levelRenderer instanceof BioluminescenceLevelRendererAccess access)) {
            return;
        }
        int minX = SectionPos.blockToSectionCoord(Mth.floor(source.x - MARKER_LIGHT_RANGE));
        int maxX = SectionPos.blockToSectionCoord(Mth.floor(source.x + MARKER_LIGHT_RANGE));
        int minY = SectionPos.blockToSectionCoord(Mth.floor(source.y - MARKER_LIGHT_RANGE));
        int maxY = SectionPos.blockToSectionCoord(Mth.floor(source.y + MARKER_LIGHT_RANGE));
        int minZ = SectionPos.blockToSectionCoord(Mth.floor(source.z - MARKER_LIGHT_RANGE));
        int maxZ = SectionPos.blockToSectionCoord(Mth.floor(source.z + MARKER_LIGHT_RANGE));
        for (int sectionX = minX; sectionX <= maxX; sectionX++) {
            for (int sectionY = minY; sectionY <= maxY; sectionY++) {
                for (int sectionZ = minZ; sectionZ <= maxZ; sectionZ++) {
                    access.bitsandbalance$scheduleChunkRebuild(sectionX, sectionY, sectionZ, false);
                }
            }
        }
    }

    public record LightSource(int id, double x, double y, double z, long revision) {
    }

    private static final class Marker {
        private final int id;
        private final BlockPos pos;
        private final Vec3 origin;
        private double travel;
        private int hover;

        private Marker(int id, BlockPos pos, Vec3 origin) {
            this.id = id;
            this.pos = pos;
            this.origin = origin;
        }
    }
}
