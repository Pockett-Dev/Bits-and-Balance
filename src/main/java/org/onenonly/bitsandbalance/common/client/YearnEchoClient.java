package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A private crit-particle guide that follows a walkable route to a cave mouth. */
public final class YearnEchoClient {
    public static final int SEARCH_RADIUS = 128;
    private static final int MAX_VISITED = 24_000;
    private static final int MAX_ROUTE_STEPS = 160;
    private static final int MAX_MOUTH_HALF_WIDTH = 12;
    private static final int MIN_TRAIL_TICKS = 200;
    private static final int MAX_TRAIL_TICKS = 600;
    private static final double GUIDE_AHEAD_BLOCKS = 7.0D;
    private static final double MAX_CORNER_RADIUS = 3.0D;
    private static final int ROUTE_LOOKAHEAD_POINTS = 12;
    private static final double MOTION_PARTICLE_SPACING = 0.18D;
    private static final int CAVE_PROBE_LIMIT = 96;
    private static final int CAVE_MIN_CELLS = 14;
    private static final int CAVE_MIN_DEPTH = 5;
    private static final int MAX_AIR_START_OFFSET = 6;
    private static final int[] HEIGHT_STEPS = {0, 1, -1};

    private static ClientLevel level;
    private static List<Vec3> trail = List.of();
    private static double[] distances = new double[0];
    private static double trailLength;
    private static BlockPos entrance;
    private static Vec3 entranceCenter;
    private static int entranceWidth;
    private static int entranceHeight;
    private static int arrivalPoofs;
    private static boolean arrived;
    private static int age;
    private static int trailTicks;
    private static int pulseGeneration;
    private static int emittedParticles;
    private static double lastScanMillis;
    private static double playerDistance;
    private static double guideDistance;
    private static double guideStep;
    private static int roundedCorners;
    private static double largestCornerRadius;
    private static int softenedSamples;
    private static double largestSmoothingOffset;
    private static double sharpestTurnDegrees;
    private static double sharpestTurnDistance;

    private YearnEchoClient() {
    }

    public static void start(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        clear();
        level = minecraft.level;
        long scanStarted = System.nanoTime();
        Vec3 castPoint = new Vec3(x, y, z);
        BlockPos castPos = BlockPos.containing(x, y, z);
        CaveEntrance selected = findEntrance(level, castPos, castPoint);
        lastScanMillis = (System.nanoTime() - scanStarted) / 1_000_000.0D;
        pulseGeneration++;
        if (selected == null) {
            level = null;
            return;
        }
        List<BlockPos> route = selected.route();
        entrance = selected.cell();
        entranceCenter = selected.center();
        entranceWidth = selected.width();
        entranceHeight = selected.height();
        List<Vec3> points = new ArrayList<>(route.size() + 1);
        points.add(castPoint);
        for (BlockPos pos : route) {
            points.add(new Vec3(pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D));
        }
        if (points.get(points.size() - 1).distanceToSqr(entranceCenter) > 1.0E-6D) {
            points.add(entranceCenter);
        }
        SmoothingStats smoothing = new SmoothingStats();
        trail = softenPolyline(level, roundLevelCorners(level,
                simplifyOpenSegments(level, points), smoothing));
        roundedCorners = smoothing.roundedCorners;
        largestCornerRadius = smoothing.largestRadius;
        distances = new double[trail.size()];
        for (int i = 1; i < trail.size(); i++) {
            distances[i] = distances[i - 1] + trail.get(i).distanceTo(trail.get(i - 1));
        }
        trailLength = distances[distances.length - 1];
        sharpestTurnDegrees = sharpestVisibleTurn();
        // Allow walking time along the actual route, with a short grace period at each end.
        trailTicks = Math.min(MAX_TRAIL_TICKS,
                Math.max(MIN_TRAIL_TICKS, (int) Math.ceil(trailLength * 5.0D) + 40));
    }

    /** Keep the moving guide about seven route blocks ahead of its owner. */
    public static void tick(Minecraft minecraft) {
        if (level == null) {
            return;
        }
        if (level != minecraft.level || minecraft.player == null || ++age > trailTicks) {
            clear();
            return;
        }
        if (arrived) {
            return;
        }
        double previousPlayerDistance = playerDistance;
        double nearest = nearestRouteDistance(minecraft.player.position().add(0.0D, 1.1D, 0.0D));
        if (Double.isFinite(nearest)) {
            playerDistance = nearest;
        }
        double playerStep = playerDistance - previousPlayerDistance;
        double desired = Math.min(trailLength, playerDistance + GUIDE_AHEAD_BLOCKS);
        double correction = (desired - guideDistance) * 0.3D;
        double maxForward = Math.min(0.7D, 0.18D + Math.max(0.0D, playerStep) * 1.6D);
        maxForward = Math.min(maxForward, turnSpeedLimit(guideDistance));
        double maxBackward = Math.min(0.5D, 0.25D + Math.max(0.0D, -playerStep) * 1.2D);
        double requestedStep = Math.max(-maxBackward, Math.min(maxForward, playerStep + correction));
        double nextGuide = guideDistance + requestedStep;
        if (guideDistance <= desired && nextGuide > desired) {
            nextGuide = desired;
        }
        double previousGuide = guideDistance;
        guideDistance = Math.max(0.0D, Math.min(trailLength, nextGuide));
        guideStep = guideDistance - previousGuide;
        emitMotionTrail(previousGuide, guideDistance);
        if (guideDistance >= trailLength - 1.0E-4D) {
            guideDistance = trailLength;
            emitArrivalPoof();
            arrived = true;
            return;
        }
        emitGuide();
    }

    /** Give tight bends several visible ticks instead of crossing them in one step. */
    private static double turnSpeedLimit(double distance) {
        Vec3 near = sample(Math.min(trailLength, distance + 0.35D))
                .subtract(sample(distance));
        Vec3 ahead = sample(Math.min(trailLength, distance + 1.05D))
                .subtract(sample(Math.min(trailLength, distance + 0.7D)));
        if (near.lengthSqr() < 1.0E-4D || ahead.lengthSqr() < 1.0E-4D) {
            return 0.7D;
        }
        double alignment = near.normalize().dot(ahead.normalize());
        return alignment >= 0.86D ? 0.7D
                : Math.max(0.2D, 0.4D - 0.2D * (1.0D - alignment));
    }

    private static void emitArrivalPoof() {
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 10; i++) {
                double angle = (i + ring * 0.35D) * Math.PI * 2.0D / 10.0D;
                double speed = 0.07D + ring * 0.025D;
                double rise = (ring - 1) * 0.045D + 0.025D;
                level.addParticle(ParticleTypes.CRIT, entranceCenter.x, entranceCenter.y,
                        entranceCenter.z, Math.cos(angle) * speed, rise,
                        Math.sin(angle) * speed);
                emittedParticles++;
            }
        }
        arrivalPoofs++;
    }

    private static void emitGuide() {
        Vec3 head = sample(guideDistance);
        Vec3 tangent = sample(Math.min(trailLength, guideDistance + 0.3D))
                .subtract(sample(Math.max(0.0D, guideDistance - 0.3D)));
        if (tangent.lengthSqr() > 1.0E-6D) {
            tangent = tangent.normalize();
        } else {
            tangent = Vec3.ZERO;
        }
        for (int i = 0; i < 3; i++) {
            double phase = age * 0.4D + i * Math.PI * 2.0D / 3.0D;
            double sideways = Math.cos(phase) * 0.12D;
            Vec3 point = head.add(-tangent.z * sideways, Math.sin(phase) * 0.12D,
                    tangent.x * sideways);
            emit(point, tangent);
        }
        for (int i = 1; i <= 2; i++) {
            emit(sample(Math.max(0.0D, guideDistance - i * 0.45D)), tangent);
        }
    }

    private static void emitMotionTrail(double from, double to) {
        double movement = Math.abs(to - from);
        int sections = (int) Math.ceil(movement / MOTION_PARTICLE_SPACING);
        for (int i = 1; i <= sections; i++) {
            double along = from + (to - from) * i / sections;
            Vec3 tangent = sample(Math.min(trailLength, along + 0.12D))
                    .subtract(sample(Math.max(0.0D, along - 0.12D)));
            emit(sample(along), tangent.lengthSqr() > 1.0E-6D ? tangent.normalize() : Vec3.ZERO);
        }
    }

    private static void emit(Vec3 point, Vec3 tangent) {
        level.addParticle(ParticleTypes.CRIT, point.x, point.y, point.z,
                tangent.x * 0.02D, 0.015D, tangent.z * 0.02D);
        emittedParticles++;
    }

    private static double nearestRouteDistance(Vec3 player) {
        double bestScore = Double.POSITIVE_INFINITY;
        double bestDistance = Double.NaN;
        for (int i = 1; i < trail.size(); i++) {
            Vec3 from = trail.get(i - 1);
            Vec3 segment = trail.get(i).subtract(from);
            double lengthSqr = segment.lengthSqr();
            if (lengthSqr <= 1.0E-6D) {
                continue;
            }
            double fraction = Math.max(0.0D,
                    Math.min(1.0D, player.subtract(from).dot(segment) / lengthSqr));
            double distance = distances[i - 1] + (distances[i] - distances[i - 1]) * fraction;
            double offsetSqr = player.distanceToSqr(from.add(segment.scale(fraction)));
            if (offsetSqr > 9.0D) {
                continue;
            }
            // Prefer the current part of a route if the path doubles back nearby.
            double score = offsetSqr + Math.abs(distance - playerDistance) * 0.01D;
            if (score < bestScore) {
                bestScore = score;
                bestDistance = distance;
            }
        }
        return bestDistance;
    }

    private static Vec3 sample(double distance) {
        int index = Arrays.binarySearch(distances, distance);
        if (index >= 0) {
            return trail.get(index);
        }
        int after = Math.min(trail.size() - 1, -index - 1);
        int before = Math.max(0, after - 1);
        double section = distances[after] - distances[before];
        return section <= 0.0D ? trail.get(after)
                : trail.get(before).lerp(trail.get(after), (distance - distances[before]) / section);
    }

    /** Remove grid turns where a clear route stays close to supported ground. */
    private static List<Vec3> simplifyOpenSegments(ClientLevel world, List<Vec3> points) {
        List<Vec3> simplified = new ArrayList<>();
        simplified.add(points.get(0));
        for (int index = 0; index < points.size() - 1;) {
            int next = index + 1;
            for (int candidate = Math.min(points.size() - 1, index + ROUTE_LOOKAHEAD_POINTS);
                 candidate > next; candidate--) {
                if (groundedLineClear(world, points.get(index), points.get(candidate))) {
                    next = candidate;
                    break;
                }
            }
            appendDistinct(simplified, points.get(next));
            index = next;
        }
        return simplified;
    }

    private static boolean groundedLineClear(ClientLevel world, Vec3 from, Vec3 to) {
        double heightChange = Math.abs(from.y - to.y);
        if (heightChange > 2.05D) {
            return false;
        }
        if (heightChange > 0.05D) {
            return particleLineClear(world, from, to);
        }
        int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.25D));
        for (int i = 1; i < samples; i++) {
            if (!clearForParticles(world, from.lerp(to, i / (double) samples))) {
                return false;
            }
        }
        return true;
    }

    /** Keep the whole crit cluster clear of walls and above walkable ground. */
    private static boolean clearForParticles(ClientLevel world, Vec3 point) {
        double feet = point.y - 1.1D + 1.0E-4D;
        for (int xi = -1; xi <= 1; xi += 2) {
            for (int zi = -1; zi <= 1; zi += 2) {
                double dx = xi * 0.15D;
                double dz = zi * 0.15D;
                if (!standable(world, BlockPos.containing(point.x + dx, feet, point.z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Use broad curves when clear, shrinking them only near terrain or tight turns. */
    private static List<Vec3> roundLevelCorners(ClientLevel world, List<Vec3> points,
                                                 SmoothingStats smoothing) {
        if (points.size() < 3) {
            return List.copyOf(points);
        }
        List<Vec3> rounded = new ArrayList<>(points.size() * 5);
        rounded.add(points.get(0));
        for (int i = 1; i < points.size() - 1; i++) {
            Vec3 previous = points.get(i - 1);
            Vec3 corner = points.get(i);
            Vec3 next = points.get(i + 1);
            Vec3 incoming = corner.subtract(previous);
            Vec3 outgoing = next.subtract(corner);
            double inLength = incoming.horizontalDistance();
            double outLength = outgoing.horizontalDistance();
            if (Math.abs(previous.y - corner.y) <= 1.05D
                    && Math.abs(next.y - corner.y) <= 1.05D
                    && inLength >= 0.75D && outLength >= 0.75D) {
                Vec3 inDirection = incoming.scale(1.0D / inLength);
                Vec3 outDirection = outgoing.scale(1.0D / outLength);
                double turn = inDirection.dot(outDirection);
                if (turn < 0.985D && turn > -0.75D) {
                    double roomBefore = corner.subtract(rounded.get(rounded.size() - 1))
                            .horizontalDistance();
                    double limit = Math.min(MAX_CORNER_RADIUS,
                            Math.min(roomBefore, outLength) * 0.8D);
                    boolean curved = false;
                    for (double radius = limit; radius >= 0.25D; radius -= 0.25D) {
                        Vec3 entry = corner.subtract(inDirection.scale(radius));
                        Vec3 exit = corner.add(outDirection.scale(radius));
                        int segments = Math.max(8, (int) Math.ceil(radius * 8.0D));
                        if (!curveClear(world, entry, corner, exit, segments)) {
                            continue;
                        }
                        smoothing.roundedCorners++;
                        smoothing.largestRadius = Math.max(smoothing.largestRadius, radius);
                        curved = true;
                        appendDistinct(rounded, entry);
                        for (int segment = 1; segment <= segments; segment++) {
                            appendDistinct(rounded, curvePoint(entry, corner, exit,
                                    segment / (double) segments));
                        }
                        break;
                    }
                    if (curved) {
                        continue;
                    }
                }
            }
            appendDistinct(rounded, corner);
        }
        appendDistinct(rounded, points.get(points.size() - 1));
        return List.copyOf(rounded);
    }

    private static final class SmoothingStats {
        int roundedCorners;
        double largestRadius;
    }

    /** Exercise a broad turn on the fixture's open stone platform. */
    public static double fixtureBroadCornerRadius(ClientLevel world, BlockPos base) {
        double y = base.getY() + 1.1D;
        List<Vec3> openTurn = List.of(
                new Vec3(base.getX() + 0.5D, y, base.getZ() - 1.5D),
                new Vec3(base.getX() + 3.5D, y, base.getZ() - 1.5D),
                new Vec3(base.getX() + 3.5D, y, base.getZ() + 3.5D));
        SmoothingStats smoothing = new SmoothingStats();
        roundLevelCorners(world, openTurn, smoothing);
        return smoothing.largestRadius;
    }

    private static boolean curveClear(ClientLevel world, Vec3 entry, Vec3 corner,
                                      Vec3 exit, int segments) {
        for (int i = 0; i <= segments; i++) {
            if (!clearForParticles(world, curvePoint(entry, corner, exit,
                    i / (double) segments))) {
                return false;
            }
        }
        return true;
    }

    private static Vec3 curvePoint(Vec3 entry, Vec3 corner, Vec3 exit, double t) {
        double remaining = 1.0D - t;
        return entry.scale(remaining * remaining)
                .add(corner.scale(2.0D * remaining * t))
                .add(exit.scale(t * t));
    }

    /** Spread changes of direction over several blocks when the particles have room. */
    private static List<Vec3> softenPolyline(ClientLevel world, List<Vec3> points) {
        if (points.size() < 3) {
            return points;
        }
        double[] pathDistances = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            pathDistances[i] = pathDistances[i - 1]
                    + points.get(i).distanceTo(points.get(i - 1));
        }
        double length = pathDistances[pathDistances.length - 1];
        List<Vec3> softened = new ArrayList<>((int) Math.ceil(length * 4.0D) + 1);
        softened.add(points.get(0));
        int samples = Math.max(1, (int) Math.ceil(length / 0.25D));
        for (int i = 1; i < samples; i++) {
            double distance = length * i / samples;
            Vec3 original = samplePath(points, pathDistances, distance);
            Vec3 candidate = original;
            if (distance >= 2.0D && length - distance >= 2.0D) {
                candidate = samplePath(points, pathDistances, distance - 3.0D)
                        .add(samplePath(points, pathDistances, distance - 1.5D).scale(2.0D))
                        .add(original.scale(3.0D))
                        .add(samplePath(points, pathDistances, distance + 1.5D).scale(2.0D))
                        .add(samplePath(points, pathDistances, distance + 3.0D))
                        .scale(1.0D / 9.0D);
                Vec3 nextOriginal = samplePath(points, pathDistances,
                        Math.min(length, length * (i + 1) / samples));
                if (candidate.distanceToSqr(original) > 1.21D
                        || !particleLineClear(world, original, candidate)
                        || !particleLineClear(world, softened.get(softened.size() - 1), candidate)
                        || !particleLineClear(world, candidate, nextOriginal)) {
                    candidate = original;
                } else if (candidate.distanceToSqr(original) > 1.0E-4D) {
                    softenedSamples++;
                    largestSmoothingOffset = Math.max(largestSmoothingOffset,
                            candidate.distanceTo(original));
                }
            }
            appendDistinct(softened, candidate);
        }
        appendDistinct(softened, points.get(points.size() - 1));
        return List.copyOf(softened);
    }

    private static Vec3 samplePath(List<Vec3> points, double[] pathDistances, double distance) {
        distance = Math.max(0.0D, Math.min(pathDistances[pathDistances.length - 1], distance));
        int index = Arrays.binarySearch(pathDistances, distance);
        if (index >= 0) {
            return points.get(index);
        }
        int after = Math.min(points.size() - 1, -index - 1);
        int before = Math.max(0, after - 1);
        double section = pathDistances[after] - pathDistances[before];
        return section <= 0.0D ? points.get(after)
                : points.get(before).lerp(points.get(after),
                (distance - pathDistances[before]) / section);
    }

    private static boolean particleLineClear(ClientLevel world, Vec3 from, Vec3 to) {
        int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.2D));
        for (int i = 0; i <= samples; i++) {
            if (!clearNearRoute(world, from.lerp(to, i / (double) samples))) {
                return false;
            }
        }
        return true;
    }

    private static boolean clearNearRoute(ClientLevel world, Vec3 point) {
        double feet = point.y - 1.1D + 1.0E-4D;
        for (int xi = -1; xi <= 1; xi += 2) {
            for (int zi = -1; zi <= 1; zi += 2) {
                BlockPos foot = BlockPos.containing(point.x + xi * 0.15D, feet,
                        point.z + zi * 0.15D);
                if (!passable(world, foot) || !passable(world, foot.above())) {
                    return false;
                }
                BlockPos below = foot.below();
                BlockPos lower = below.below();
                if ((!world.hasChunkAt(below) || world.getBlockState(below)
                        .getCollisionShape(world, below).isEmpty())
                        && (!world.hasChunkAt(lower) || world.getBlockState(lower)
                        .getCollisionShape(world, lower).isEmpty())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void appendDistinct(List<Vec3> points, Vec3 point) {
        if (points.isEmpty() || points.get(points.size() - 1).distanceToSqr(point) > 1.0E-8D) {
            points.add(point);
        }
    }

    private static double sharpestVisibleTurn() {
        double sharpest = 0.0D;
        for (double distance = 0.5D; distance < trailLength - 0.5D; distance += 0.25D) {
            Vec3 center = sample(distance);
            Vec3 incoming = center.subtract(sample(distance - 0.5D));
            Vec3 outgoing = sample(distance + 0.5D).subtract(center);
            double length = incoming.horizontalDistance() * outgoing.horizontalDistance();
            if (length > 1.0E-4D) {
                double cosine = Math.max(-1.0D, Math.min(1.0D,
                        (incoming.x * outgoing.x + incoming.z * outgoing.z) / length));
                double angle = Math.toDegrees(Math.acos(cosine));
                if (angle > sharpest) {
                    sharpest = angle;
                    sharpestTurnDistance = distance;
                }
            }
        }
        return sharpest;
    }

    private static CaveEntrance findEntrance(ClientLevel world, BlockPos castPos, Vec3 castPoint) {
        BlockPos start = standableStart(world, castPos, castPoint);
        if (start == null) {
            return null;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Integer> steps = new HashMap<>();
        Set<BlockPos> rejectedDarkCells = new HashSet<>();
        CaveEntrance best = null;
        queue.add(start);
        parent.put(start, start);
        steps.put(start, 0);
        boolean startingOutside = world.canSeeSky(start);
        while (!queue.isEmpty() && parent.size() < MAX_VISITED) {
            BlockPos current = queue.removeFirst();
            int currentSteps = steps.get(current);
            if (currentSteps >= MAX_ROUTE_STEPS) {
                continue;
            }
            boolean currentSky = world.canSeeSky(current);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int dy : HEIGHT_STEPS) {
                    BlockPos next = current.relative(direction).offset(0, dy, 0);
                    if (parent.containsKey(next) || !withinRadius(next, start, SEARCH_RADIUS)
                            || !standable(world, next)
                            || dy == 1 && !passable(world, current.above(2))) {
                        continue;
                    }
                    parent.put(next, current);
                    int nextSteps = currentSteps + 1;
                    steps.put(next, nextSteps);
                    boolean nextSky = world.canSeeSky(next);
                    if (!nextSky) {
                        Aperture opening = apertureAt(world, next, direction);
                        if (opening != null && (currentSky || apertureAt(world, current, direction) == null)
                                && hasCaveBeyond(world, next, rejectedDarkCells)) {
                            CaveEntrance candidate = new CaveEntrance(routeTo(parent, next), next,
                                    opening.center(), opening.width(), opening.height(), nextSteps);
                            if (betterEntrance(candidate, best)) {
                                best = candidate;
                            }
                        }
                    }
                    if (!startingOutside && nextSky && !currentSky) {
                        CaveEntrance candidate = mouthOnExitRoute(world, parent, steps,
                                current, direction, rejectedDarkCells);
                        if (candidate != null && betterEntrance(candidate, best)) {
                            best = candidate;
                        }
                    }
                    queue.addLast(next);
                }
            }
        }
        return best;
    }

    /** From a cave exit, walk back under any open overhang to the enclosed mouth. */
    private static CaveEntrance mouthOnExitRoute(ClientLevel world,
                                                  Map<BlockPos, BlockPos> parent,
                                                  Map<BlockPos, Integer> steps,
                                                  BlockPos darkEdge, Direction towardSky,
                                                  Set<BlockPos> rejectedDarkCells) {
        BlockPos cell = darkEdge;
        Direction outbound = towardSky;
        while (cell != null && !world.canSeeSky(cell)) {
            Aperture opening = apertureAt(world, cell, outbound);
            if (opening != null && hasCaveBeyond(world, cell, rejectedDarkCells)) {
                return new CaveEntrance(routeTo(parent, cell), cell, opening.center(),
                        opening.width(), opening.height(), steps.get(cell));
            }
            BlockPos inward = parent.get(cell);
            if (inward == null || inward.equals(cell)) {
                break;
            }
            int dx = cell.getX() - inward.getX();
            int dz = cell.getZ() - inward.getZ();
            outbound = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST
                    : dz > 0 ? Direction.SOUTH : Direction.NORTH;
            cell = inward;
        }
        return null;
    }

    private static boolean betterEntrance(CaveEntrance candidate, CaveEntrance current) {
        return current == null || candidate.area() > current.area()
                || candidate.area() == current.area() && (candidate.width() > current.width()
                || candidate.width() == current.width() && candidate.steps() < current.steps());
    }

    /** Find the horizontal center between two natural walls at a roofed opening. */
    private static Aperture apertureAt(ClientLevel world, BlockPos pos, Direction approach) {
        int height = 0;
        for (int dy = 2; dy <= 8; dy++) {
            BlockPos above = pos.above(dy);
            if (!world.hasChunkAt(above)) {
                return null;
            }
            BlockState state = world.getBlockState(above);
            if (!state.getCollisionShape(world, above).isEmpty()) {
                height = natural(state) ? dy : 0;
                break;
            }
        }
        if (height == 0) {
            return null;
        }
        Direction side = approach.getClockWise();
        int left = naturalWallDistance(world, pos, side.getOpposite());
        int right = naturalWallDistance(world, pos, side);
        if (left == 0 || right == 0) {
            return null;
        }
        double centerOffset = (right - left) * 0.5D;
        if (Math.abs(centerOffset) > 0.5D) {
            return null;
        }
        Vec3 center = new Vec3(pos.getX() + 0.5D + side.getStepX() * centerOffset,
                pos.getY() + height * 0.5D,
                pos.getZ() + 0.5D + side.getStepZ() * centerOffset);
        return new Aperture(center, left + right - 1, height);
    }

    private static int naturalWallDistance(ClientLevel world, BlockPos pos, Direction side) {
        for (int distance = 1; distance <= MAX_MOUTH_HALF_WIDTH; distance++) {
            BlockPos wall = pos.relative(side, distance);
            boolean naturalWall = false;
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos block = wall.above(dy);
                if (!world.hasChunkAt(block)) {
                    return 0;
                }
                BlockState state = world.getBlockState(block);
                if (!state.getCollisionShape(world, block).isEmpty() && natural(state)) {
                    naturalWall = true;
                }
            }
            if (naturalWall) {
                return distance;
            }
            if (!standable(world, wall)) {
                return 0;
            }
        }
        return 0;
    }

    private record Aperture(Vec3 center, int width, int height) {
    }

    private record CaveEntrance(List<BlockPos> route, BlockPos cell, Vec3 center,
                                int width, int height, int steps) {
        int area() {
            return width * height;
        }
    }

    /** Find a visible landing point below the horn, even when it is used high in the air. */
    private static BlockPos standableStart(ClientLevel world, BlockPos castPos, Vec3 castPoint) {
        //? if mc == 1.21.1
        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight();
        //? else
        int minY = world.getMinY();
        int maxY = world.getMaxY();
        //? endif
        int topY = Math.min(castPos.getY(), maxY - 2);
        BlockPos best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int radius = 0; radius <= MAX_AIR_START_OFFSET; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int y = topY; y > minY; y--) {
                        BlockPos candidate = new BlockPos(castPos.getX() + dx, y, castPos.getZ() + dz);
                        if (!standable(world, candidate)) {
                            continue;
                        }
                        Vec3 landing = new Vec3(candidate.getX() + 0.5D,
                                candidate.getY() + 1.1D, candidate.getZ() + 0.5D);
                        if (airLineClear(world, castPoint, landing)) {
                            double distance = castPoint.distanceToSqr(landing);
                            if (distance < bestDistance) {
                                best = candidate;
                                bestDistance = distance;
                            }
                        }
                        break;
                    }
                }
            }
            if (bestDistance <= (radius + 0.5D) * (radius + 0.5D)) {
                break;
            }
        }
        return best;
    }

    private static boolean airLineClear(ClientLevel world, Vec3 from, Vec3 to) {
        int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.2D));
        for (int i = 0; i <= samples; i++) {
            BlockPos point = BlockPos.containing(from.lerp(to, i / (double) samples));
            if (!world.isOutsideBuildHeight(point) && !passable(world, point)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCaveBeyond(ClientLevel world, BlockPos dark,
                                         Set<BlockPos> rejectedDarkCells) {
        if (rejectedDarkCells.contains(dark)) {
            return false;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> depth = new HashMap<>();
        queue.add(dark);
        depth.put(dark, 0);
        int naturalCells = 0;
        int deepest = 0;
        while (!queue.isEmpty() && depth.size() <= CAVE_PROBE_LIMIT) {
            BlockPos current = queue.removeFirst();
            naturalCells += touchesNaturalTerrain(world, current) ? 1 : 0;
            deepest = Math.max(deepest, depth.get(current));
            if (depth.size() >= CAVE_MIN_CELLS && deepest >= CAVE_MIN_DEPTH && naturalCells >= 4) {
                return true;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int dy : HEIGHT_STEPS) {
                    BlockPos next = current.relative(direction).offset(0, dy, 0);
                    if (!depth.containsKey(next) && withinRadius(next, dark, 12)
                            && standable(world, next) && !world.canSeeSky(next)
                            && (dy != 1 || passable(world, current.above(2)))) {
                        depth.put(next, depth.get(current) + 1);
                        queue.addLast(next);
                    }
                }
            }
        }
        rejectedDarkCells.addAll(depth.keySet());
        return false;
    }

    private static boolean touchesNaturalTerrain(ClientLevel world, BlockPos pos) {
        // A dirt floor under leaves is not a cave. Require a natural roof or wall.
        for (int dy = 2; dy <= 8; dy++) {
            BlockPos roof = pos.above(dy);
            BlockState state = world.getBlockState(roof);
            if (!state.getCollisionShape(world, roof).isEmpty()) {
                if (natural(state)) {
                    return true;
                }
                break;
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (natural(world.getBlockState(pos.relative(direction)))
                    || natural(world.getBlockState(pos.above().relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean natural(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.CALCITE)
                || state.is(Blocks.DRIPSTONE_BLOCK) || state.is(Blocks.MUD);
    }

    private static boolean withinRadius(BlockPos pos, BlockPos center, int radius) {
        int dx = pos.getX() - center.getX();
        int dy = pos.getY() - center.getY();
        int dz = pos.getZ() - center.getZ();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static boolean standable(ClientLevel world, BlockPos pos) {
        BlockPos below = pos.below();
        return passable(world, pos) && passable(world, pos.above())
                && !world.isOutsideBuildHeight(below) && world.hasChunkAt(below)
                && !world.getBlockState(below).getCollisionShape(world, below).isEmpty();
    }

    private static boolean passable(ClientLevel world, BlockPos pos) {
        return !world.isOutsideBuildHeight(pos) && world.hasChunkAt(pos)
                && world.getFluidState(pos).isEmpty()
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    private static List<BlockPos> routeTo(Map<BlockPos, BlockPos> parent, BlockPos end) {
        List<BlockPos> route = new ArrayList<>();
        BlockPos current = end;
        while (true) {
            route.add(current);
            BlockPos previous = parent.get(current);
            if (previous == null || previous.equals(current)) {
                break;
            }
            current = previous;
        }
        Collections.reverse(route);
        return route;
    }

    private static void clear() {
        level = null;
        trail = List.of();
        distances = new double[0];
        trailLength = 0.0D;
        entrance = null;
        entranceCenter = null;
        entranceWidth = 0;
        entranceHeight = 0;
        arrivalPoofs = 0;
        arrived = false;
        age = 0;
        trailTicks = 0;
        emittedParticles = 0;
        playerDistance = 0.0D;
        guideDistance = 0.0D;
        guideStep = 0.0D;
        roundedCorners = 0;
        largestCornerRadius = 0.0D;
        softenedSamples = 0;
        largestSmoothingOffset = 0.0D;
        sharpestTurnDegrees = 0.0D;
        sharpestTurnDistance = 0.0D;
    }

    public static int pulseGeneration() {
        return pulseGeneration;
    }

    public static int routePoints() {
        return trail.size();
    }

    public static double routeLength() {
        return trailLength;
    }

    public static boolean hasEntrance() {
        return entrance != null;
    }

    public static BlockPos entrancePosition() {
        return entrance;
    }

    public static Vec3 entranceCenter() {
        return entranceCenter;
    }

    public static int entranceWidth() {
        return entranceWidth;
    }

    public static int entranceHeight() {
        return entranceHeight;
    }

    public static int arrivalPoofs() {
        return arrivalPoofs;
    }

    public static int emittedParticles() {
        return emittedParticles;
    }

    public static int trailDurationTicks() {
        return trailTicks;
    }

    public static double playerRouteDistance() {
        return playerDistance;
    }

    public static double guideRouteDistance() {
        return guideDistance;
    }

    public static double guideStep() {
        return guideStep;
    }

    public static int roundedCorners() {
        return roundedCorners;
    }

    public static double largestCornerRadius() {
        return largestCornerRadius;
    }

    public static int softenedSamples() {
        return softenedSamples;
    }

    public static double largestSmoothingOffset() {
        return largestSmoothingOffset;
    }

    public static double sharpestTurnDegrees() {
        return sharpestTurnDegrees;
    }

    public static String sharpestTurnPath() {
        if (trail.isEmpty()) {
            return "none";
        }
        return sample(Math.max(0.0D, sharpestTurnDistance - 1.0D)) + " -> "
                + sample(sharpestTurnDistance) + " -> "
                + sample(Math.min(trailLength, sharpestTurnDistance + 1.0D));
    }

    public static double lastScanMillis() {
        return lastScanMillis;
    }
}
