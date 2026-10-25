package org.onenonly.bitsandbalance.common.validation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleHelper;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabDestroyParticleGeometry;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabVerticalDestroyParticleCapture;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Live client regression for material-correct Vertical Slab destruction debris. */
public final class EnhancedSlabVerticalDestroyParticleFixture {
    public record Result(boolean passed, int checked, List<String> details) { }

    private EnhancedSlabVerticalDestroyParticleFixture() {
    }

    private static VisualState visual;

    private static final class VisualState {
        final Minecraft client;
        final MinecraftServer server;
        final ServerLevel level;
        final net.minecraft.server.level.ServerPlayer player;
        final Vec3 origin;
        final float yaw, pitch;
        final net.minecraft.world.level.GameType mode;
        final boolean pause, shift;
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        final net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock> clock;
        //? endif
        final long oldTime;
        final BlockPos pos;
        final Checks checks = new Checks();
        final String prefix = "bnb-vertical-particle-" + System.currentTimeMillis();
        int stage, ticks;

        VisualState(Minecraft client) {
            this.client = client;
            server = client.getSingleplayerServer();
            level = server.getLevel(client.level.dimension());
            player = server.getPlayerList().getPlayer(client.player.getUUID());
            origin = player.position();
            yaw = player.getYRot();
            pitch = player.getXRot();
            mode = player.gameMode.getGameModeForPlayer();
            pause = client.options.pauseOnLostFocus;
            shift = client.player.isShiftKeyDown();
            pos = player.blockPosition().offset(0, 12, 0);
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            clock = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                    .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
            //? if mc == 26.3
            oldTime = server.clockManager().getInstance(clock).totalTicks();
            //? else
            oldTime = server.clockManager().getTotalTicks(clock);
            //? endif
            //? else
            oldTime = level.getDayTime();
            //? endif
        }

        String name() {
            return switch (stage) {
                case 0 -> "whole-uniform";
                case 1 -> "mixed-noncrouched";
                case 2 -> "mixed-knee-first";
                default -> "vanilla-birch-control";
            };
        }
    }

    /** Screenshots and real startDestroyBlock prediction/packet path, never production hooks alone. */
    public static Result tickVisual(Minecraft client) {
        try {
            if (visual == null) {
                MinecraftServer server = client.getSingleplayerServer();
                if (!Boolean.getBoolean("bitsandbalance.fixtureAllowMutation") || server == null
                        || !server.getWorldPath(LevelResource.ROOT).toString().toLowerCase().contains("scratch")
                        || !Files.isRegularFile(server.getWorldPath(LevelResource.ROOT).resolve(".bnb-mutable-fixture"))) {
                    return new Result(false, 0, List.of("particle-visual-requires-marked-scratch"));
                }
                visual = new VisualState(client);
                VisualState test = visual;
                if (!test.server.submit(() -> test.level.getBlockState(test.pos).isAir()).get(10, TimeUnit.SECONDS)) {
                    return new Result(false, 0, List.of("particle-visual-area-not-air"));
                }
                test.client.options.pauseOnLostFocus = false;
                test.server.submit(() -> {
                    test.player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
                    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
                    test.server.clockManager().setTotalTicks(test.clock, 6000);
                    test.server.getPlayerList().broadcastAll(test.server.clockManager().createFullSyncPacket());
                    //? else
                    test.level.setDayTime(6000);
                    //? endif
                    test.player.teleportTo(test.pos.getX() + 0.5, test.pos.getY() - 0.5, test.pos.getZ() + 3.3);
                    return true;
                }).get(10, TimeUnit.SECONDS);
                prepareVisual(test);
                return null;
            }
            VisualState test = visual;
            client.options.pauseOnLostFocus = false;
            client.setScreen(null);
            client.setCameraEntity(client.player);
            client.player.setPos(test.pos.getX() + 0.5, test.pos.getY() - 0.5, test.pos.getZ() + 3.3);
            client.player.setYRot(180);
            client.player.setXRot(12);
            client.player.yRotO = 180;
            client.player.xRotO = 12;
            client.player.setShiftKeyDown(test.stage == 2);
            client.gameMode.setLocalMode(net.minecraft.world.level.GameType.CREATIVE);
            int tick = ++test.ticks;
            if (tick == 39) captureVisual(test, "before");
            if (tick == 40) {
                BlockState owner = client.level.getBlockState(test.pos);
                test.checks.check(!owner.isAir(), test.name() + ".owner=" + owner);
                test.checks.details.add(test.name() + ".materials=" + java.util.Arrays.toString(
                        EnhancedSlabMaterialSavedData.getClient(client.level, test.pos.asLong())));
                EnhancedSlabVerticalDestroyParticleCapture.resetDebugTrace();
                clearVisualParticles(test);
                client.hitResult = new BlockHitResult(new Vec3(test.pos.getX() + 0.25,
                        test.pos.getY() + 0.5, test.pos.getZ() + 1), Direction.SOUTH, test.pos, false);
                test.checks.check(client.gameMode.startDestroyBlock(test.pos, Direction.SOUTH), test.name() + ".real-start-destroy");
                traceQueuedParticles(test);
            }
            if (tick == 42 || tick == 44 || tick == 48) {
                captureVisual(test, "after-" + (tick - 40) + "-ticks");
                test.checks.details.add(test.name() + ".trace-tick=" + tick + ":"
                        + EnhancedSlabVerticalDestroyParticleCapture.debugSnapshot());
            }
            if (tick < 80) return null;
            test.checks.details.add(test.name() + ".remaining-client=" + client.level.getBlockState(test.pos));
            if (++test.stage < 4) {
                test.ticks = 0;
                prepareVisual(test);
                return null;
            }
            restoreVisual(test);
            return test.checks.result();
        } catch (Throwable failure) {
            if (visual != null) {
                visual.checks.passed = false;
                visual.checks.details.add("visual-exception=" + failure);
                try { restoreVisual(visual); } catch (Exception ignored) { }
                return visual.checks.result();
            }
            return new Result(false, 0, List.of("visual-exception=" + failure));
        }
    }

    private static void prepareVisual(VisualState test) throws Exception {
        test.server.submit(() -> {
            test.level.setBlock(test.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(test.level, test.pos.asLong());
            Block ownerBlock = test.stage == 0 ? VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.BIRCH_SLAB)
                    : CommonBlocks.VERTICAL_SLAB;
            BlockState owner = test.stage == 3 ? Blocks.BIRCH_PLANKS.defaultBlockState()
                    : ownerBlock.defaultBlockState().setValue(VerticalSlabBlock.FACING, Direction.WEST)
                    .setValue(VerticalSlabBlock.DOUBLE, true).setValue(VerticalSlabBlock.WATERLOGGED, false);
            test.level.setBlock(test.pos, owner, Block.UPDATE_ALL);
            if (test.stage != 3) {
                BlockState facing = Blocks.BIRCH_SLAB.defaultBlockState();
                BlockState opposite = (test.stage == 0 ? Blocks.BIRCH_SLAB : Blocks.SPRUCE_SLAB).defaultBlockState();
                if (test.level.getBlockEntity(test.pos) instanceof VerticalSlabBlockEntity slab) slab.setSlabs(facing, opposite);
                else EnhancedSlabMaterialSavedData.remember(test.level, test.pos.asLong(), facing, opposite);
            }
            test.player.setShiftKeyDown(test.stage == 2);
            return true;
        }).get(10, TimeUnit.SECONDS);
        EnhancedSlabVerticalDestroyParticleCapture.clear(test.client.level, test.pos);
    }

    private static void captureVisual(VisualState test, String phase) {
        String name = test.prefix + "-" + test.name() + "-" + phase + ".png";
        test.checks.details.add("screenshot=" + test.client.gameDirectory.toPath().resolve("screenshots").resolve(name));
        //? if mc == 1.21.1
        net.minecraft.client.Screenshot.grab(test.client.gameDirectory, name, test.client.getMainRenderTarget(), message -> { });
        //? else
        //? if mc == 1.21.11 || mc == 26.1
        net.minecraft.client.Screenshot.grab(test.client.gameDirectory, name, test.client.getMainRenderTarget(), 1, message -> { });
        //? else
        net.minecraft.client.Screenshot.grab(test.client.gameDirectory, name, test.client.gameRenderer.mainRenderTarget(), 1, message -> { });
        //? endif
        //? endif
    }

    private static void clearVisualParticles(VisualState test) throws Exception {
        //? if mc == 1.21.1
        java.lang.reflect.Method clear = test.client.particleEngine.getClass().getDeclaredMethod("clearParticles");
        clear.setAccessible(true);
        clear.invoke(test.client.particleEngine);
        //? else
        test.client.particleEngine.clearParticles();
        //? endif
    }

    private static void traceQueuedParticles(VisualState test) throws Exception {
        java.lang.reflect.Field queueField = test.client.particleEngine.getClass().getDeclaredField("particlesToAdd");
        queueField.setAccessible(true);
        Map<String, Integer> grouped = new LinkedHashMap<>();
        int terrainParticles = 0;
        int zeroLightParticles = 0;
        for (Object particle : (java.util.Collection<?>) queueField.get(test.client.particleEngine)) {
            if (!(particle instanceof net.minecraft.client.particle.TerrainParticle)) continue;
            terrainParticles++;
            Object sprite = reflectParticleField(particle, "sprite");
            String spriteName = ((net.minecraft.client.renderer.texture.TextureAtlasSprite) sprite).contents().name().toString();
            //? if mc == 1.21.1 || mc == 1.21.11
            java.lang.reflect.Method light = net.minecraft.client.particle.Particle.class.getDeclaredMethod("getLightColor", float.class);
            //? else
            java.lang.reflect.Method light = net.minecraft.client.particle.Particle.class.getDeclaredMethod("getLightCoords", float.class);
            //? endif
            light.setAccessible(true);
            int packedLight = (Integer) light.invoke(particle, 0.0F);
            if (packedLight == 0) zeroLightParticles++;
            String key = spriteName + ",rgb=" + reflectParticleField(particle, "rCol") + "/"
                    + reflectParticleField(particle, "gCol") + "/" + reflectParticleField(particle, "bCol")
                    + ",light=" + packedLight;
            grouped.merge(key, 1, Integer::sum);
        }
        test.checks.details.add(test.name() + ".queued-terrain-particles=" + grouped);
        test.checks.check(terrainParticles > 0, test.name() + ".terrain-particles=" + terrainParticles);
        test.checks.check(zeroLightParticles == 0, test.name() + ".zero-light-particles=" + zeroLightParticles);
    }

    private static Object reflectParticleField(Object particle, String name) throws Exception {
        Class<?> type = particle.getClass();
        while (type != null) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(particle);
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static void restoreVisual(VisualState test) throws Exception {
        test.client.options.pauseOnLostFocus = test.pause;
        test.client.player.setShiftKeyDown(test.shift);
        test.server.submit(() -> {
            test.level.setBlock(test.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(test.level, test.pos.asLong());
            test.player.gameMode.changeGameModeForPlayer(test.mode);
            test.player.setYRot(test.yaw);
            test.player.setXRot(test.pitch);
            test.player.teleportTo(test.origin.x, test.origin.y, test.origin.z);
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            test.server.clockManager().setTotalTicks(test.clock, test.oldTime);
            test.server.getPlayerList().broadcastAll(test.server.clockManager().createFullSyncPacket());
            //? else
            test.level.setDayTime(test.oldTime);
            //? endif
            return true;
        }).get(10, TimeUnit.SECONDS);
        EnhancedSlabVerticalDestroyParticleCapture.clear(test.client.level, test.pos);
        test.checks.details.add("visual-scratch-world-restored");
    }

    public static Result run(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        if (!Boolean.getBoolean("bitsandbalance.fixtureAllowMutation") || server == null
                || !server.getWorldPath(LevelResource.ROOT).toString().toLowerCase().contains("scratch")
                || !Files.isRegularFile(server.getWorldPath(LevelResource.ROOT).resolve(".bnb-mutable-fixture"))) {
            return new Result(false, 0, List.of("vertical-destroy-particle-fixture-requires-marked-scratch"));
        }

        Checks checks = new Checks();
        checkCenteredGeometry(checks);
        BlockPos mixedPos = client.player.blockPosition().offset(4, 12, 4);
        BlockPos wholePos = mixedPos.east(2);
        HitResult oldHit = client.hitResult;
        BlockState oldMixed = client.level.getBlockState(mixedPos);
        BlockState oldWhole = client.level.getBlockState(wholePos);

        try {
            ServerLevel serverLevel = server.getLevel(client.level.dimension());
            boolean serverAreaClear = server.submit(() -> serverLevel.getBlockState(mixedPos).isAir()
                    && serverLevel.getBlockState(wholePos).isAir()).get(10, TimeUnit.SECONDS);
            checks.check(serverAreaClear, "server-scratch-area-clear");
            checks.check(oldMixed.isAir() && oldWhole.isAir(), "client-scratch-area-clear");
            if (!serverAreaClear || !oldMixed.isAir() || !oldWhole.isAir()) {
                return checks.result();
            }

            BlockState mixedOwner = CommonBlocks.VERTICAL_SLAB.defaultBlockState()
                    .setValue(VerticalSlabBlock.FACING, Direction.EAST)
                    .setValue(VerticalSlabBlock.DOUBLE, true)
                    .setValue(VerticalSlabBlock.WATERLOGGED, false);
            client.level.setBlock(mixedPos, mixedOwner, Block.UPDATE_ALL);
            BlockEntity mixedBlockEntity = client.level.getBlockEntity(mixedPos);
            if (mixedBlockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
                verticalSlab.setSlabs(Blocks.BIRCH_SLAB.defaultBlockState(), Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState());
            }
            EnhancedSlabMaterialSavedData.rememberClient(client.level, mixedPos.asLong(),
                    Blocks.BIRCH_SLAB.defaultBlockState(), Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState());
            client.hitResult = new BlockHitResult(
                    new Vec3(mixedPos.getX() + 0.75D, mixedPos.getY() + 0.5D, mixedPos.getZ() + 0.5D),
                    Direction.WEST, mixedPos, false);

            EnhancedSlabVerticalDestroyParticleCapture.resetDebugTrace();
            mixedOwner.getBlock().playerWillDestroy(client.level, mixedPos, mixedOwner, client.player);
            EnhancedSlabVerticalDestroyParticleCapture.DebugSnapshot mixed =
                    EnhancedSlabVerticalDestroyParticleCapture.debugSnapshot();
            checks.check(mixed.captures() == 1, "mixed-first.capture-count=" + mixed.captures());
            checks.check(mixed.spawns() == 1, "mixed-first.spawn-count=" + mixed.spawns());
            checks.check(mixed.spawnedStates().size() == 1
                            && (mixed.spawnedStates().getFirst().is(Blocks.BIRCH_SLAB)
                            || mixed.spawnedStates().getFirst().is(Blocks.DEEPSLATE_BRICK_SLAB)),
                    "mixed-first.material=" + describeFirst(mixed.spawnedStates()));
            checks.check(mixed.spawnedParticleCounts().equals(List.of(32)),
                    "mixed-first.vertical-half-particles=" + mixed.spawnedParticleCounts());
            checks.check(mixed.spawnedStates().stream().noneMatch(state -> state.getBlock() instanceof VerticalSlabBlock),
                    "mixed-first.no-generated-owner-particles");

            boolean mixedPredictionSuppressed = EnhancedSlabDestroyParticleHelper.spawnForState(
                    client.level, client.player, mixedPos, mixedOwner);
            EnhancedSlabVerticalDestroyParticleCapture.DebugSnapshot mixedAfterPrediction =
                    EnhancedSlabVerticalDestroyParticleCapture.debugSnapshot();
            checks.check(mixedPredictionSuppressed && mixedAfterPrediction.spawns() == 1
                            && mixedAfterPrediction.duplicateSuppressions() == 1,
                    "mixed-first.prediction-duplicate-suppressed=" + mixedAfterPrediction.details());

            Block uniformVertical = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.BIRCH_SLAB);
            checks.check(uniformVertical instanceof VerticalSlabBlock, "uniform-vertical-registered=" + uniformVertical);
            if (uniformVertical instanceof VerticalSlabBlock) {
                BlockState wholeOwner = uniformVertical.defaultBlockState()
                        .setValue(VerticalSlabBlock.FACING, Direction.EAST)
                        .setValue(VerticalSlabBlock.DOUBLE, true)
                        .setValue(VerticalSlabBlock.WATERLOGGED, false);
                client.level.setBlock(wholePos, wholeOwner, Block.UPDATE_ALL);
                BlockEntity wholeBlockEntity = client.level.getBlockEntity(wholePos);
                if (wholeBlockEntity instanceof VerticalSlabBlockEntity verticalSlab) {
                    verticalSlab.setSlabs(Blocks.BIRCH_SLAB.defaultBlockState(), Blocks.BIRCH_SLAB.defaultBlockState());
                }
                EnhancedSlabMaterialSavedData.rememberClient(client.level, wholePos.asLong(),
                        Blocks.BIRCH_SLAB.defaultBlockState(), Blocks.BIRCH_SLAB.defaultBlockState());
                client.hitResult = new BlockHitResult(Vec3.atCenterOf(wholePos), Direction.WEST, wholePos, false);

                EnhancedSlabVerticalDestroyParticleCapture.resetDebugTrace();
                wholeOwner.getBlock().playerWillDestroy(client.level, wholePos, wholeOwner, client.player);
                EnhancedSlabVerticalDestroyParticleCapture.DebugSnapshot whole =
                        EnhancedSlabVerticalDestroyParticleCapture.debugSnapshot();
                checks.check(whole.captures() == 1, "whole-double.capture-count=" + whole.captures());
                checks.check(whole.spawns() == 1, "whole-double.spawn-count=" + whole.spawns());
                checks.check(whole.spawnedStates().size() == 1 && whole.spawnedStates().getFirst().is(Blocks.BIRCH_SLAB),
                        "whole-double.material=" + describeFirst(whole.spawnedStates()));
                checks.check(whole.spawnedParticleCounts().equals(List.of(64)),
                        "whole-double.full-block-particles=" + whole.spawnedParticleCounts());

                boolean wholePredictionSuppressed = EnhancedSlabDestroyParticleHelper.spawnForState(
                        client.level, client.player, wholePos, wholeOwner);
                EnhancedSlabVerticalDestroyParticleCapture.DebugSnapshot wholeAfterPrediction =
                        EnhancedSlabVerticalDestroyParticleCapture.debugSnapshot();
                checks.check(wholePredictionSuppressed && wholeAfterPrediction.spawns() == 1
                                && wholeAfterPrediction.duplicateSuppressions() == 1,
                        "whole-double.prediction-duplicate-suppressed=" + wholeAfterPrediction.details());
            }

            EnhancedSlabVerticalDestroyParticleCapture.resetDebugTrace();
            EnhancedSlabVerticalDestroyParticleCapture.clear(client.level, mixedPos);
            EnhancedSlabMaterialSavedData.rememberClient(client.level, mixedPos.asLong());
            client.level.setBlock(mixedPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            client.level.addDestroyBlockEffect(mixedPos, mixedOwner);
            EnhancedSlabVerticalDestroyParticleCapture.DebugSnapshot stale =
                    EnhancedSlabVerticalDestroyParticleCapture.debugSnapshot();
            checks.check(stale.spawns() == 0 && stale.suppressedOwnerFallbacks() == 1,
                    "stale-owner-black-fallback-suppressed=" + stale.details());
        } catch (Throwable failure) {
            checks.passed = false;
            checks.details.add("exception=" + failure);
        } finally {
            client.hitResult = oldHit;
            EnhancedSlabVerticalDestroyParticleCapture.clear(client.level, mixedPos);
            EnhancedSlabVerticalDestroyParticleCapture.clear(client.level, wholePos);
            EnhancedSlabMaterialSavedData.rememberClient(client.level, mixedPos.asLong());
            EnhancedSlabMaterialSavedData.rememberClient(client.level, wholePos.asLong());
            client.level.setBlock(mixedPos, oldMixed, Block.UPDATE_ALL);
            client.level.setBlock(wholePos, oldWhole, Block.UPDATE_ALL);
        }
        checks.details.add("client-scratch-blocks-and-hit-result-restored");
        return checks.result();
    }

    private static String describeFirst(List<BlockState> states) {
        return states.isEmpty() ? "none" : states.getFirst().getBlock().toString();
    }

    private static void checkCenteredGeometry(Checks checks) {
        checkCentered(checks, "mixed-bottom", Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0));
        checkCentered(checks, "mixed-top", Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0));

        checkCentered(checks, "vertical-north", Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0));
        checkCentered(checks, "vertical-south", Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0));
        checkCentered(checks, "vertical-west", Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0));
        checkCentered(checks, "vertical-east", Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0));

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            checkCentered(checks, "step-" + facing.getName() + "-bottom",
                    StepBlock.quadrantShape(facing, false));
            checkCentered(checks, "step-" + facing.getName() + "-top",
                    StepBlock.quadrantShape(facing, true));
        }

        for (Direction facing : List.of(Direction.NORTH, Direction.SOUTH)) {
            for (Direction side : List.of(Direction.WEST, Direction.EAST)) {
                checkCentered(checks, "vertical-step-" + facing.getName() + "-" + side.getName(),
                        VerticalStepBlock.quadrantShape(facing, side));
            }
        }

        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            for (int index = 0; index < 4; index++) {
                checkCentered(checks, "quad-step-" + axis.getName() + "-" + index,
                        QuadStepBlock.shapeForIndex(axis, index));
            }
        }
        for (int index = 0; index < 4; index++) {
            checkCentered(checks, "quad-vertical-step-" + index,
                    QuadVerticalStepBlock.shapeForIndex(index));
        }
    }

    private static void checkCentered(Checks checks, String name, VoxelShape shape) {
        BlockPos origin = new BlockPos(13, 23, -7);
        Map<AABB, CenterStats> stats = new LinkedHashMap<>();
        boolean[] exact = {true};
        int count = EnhancedSlabDestroyParticleGeometry.emitGrid(origin, shape, (box, motion) -> {
            double centerX = origin.getX() + (box.minX + box.maxX) * 0.5D;
            double centerY = origin.getY() + (box.minY + box.maxY) * 0.5D;
            double centerZ = origin.getZ() + (box.minZ + box.maxZ) * 0.5D;
            exact[0] &= close(motion.x() - motion.vx(), centerX)
                    && close(motion.y() - motion.vy(), centerY)
                    && close(motion.z() - motion.vz(), centerZ)
                    && inside(motion.x(), origin.getX() + box.minX, origin.getX() + box.maxX)
                    && inside(motion.y(), origin.getY() + box.minY, origin.getY() + box.maxY)
                    && inside(motion.z(), origin.getZ() + box.minZ, origin.getZ() + box.maxZ);
            stats.computeIfAbsent(box, ignored -> new CenterStats()).add(motion);
        });

        for (Map.Entry<AABB, CenterStats> entry : stats.entrySet()) {
            AABB box = entry.getKey();
            CenterStats sample = entry.getValue();
            exact[0] &= close(sample.meanX(), origin.getX() + (box.minX + box.maxX) * 0.5D)
                    && close(sample.meanY(), origin.getY() + (box.minY + box.maxY) * 0.5D)
                    && close(sample.meanZ(), origin.getZ() + (box.minZ + box.maxZ) * 0.5D)
                    && close(sample.meanVx(), 0.0D)
                    && close(sample.meanVy(), 0.0D)
                    && close(sample.meanVz(), 0.0D);
        }
        checks.check(exact[0] && count > 0 && !stats.isEmpty(),
                name + ".piece-centered-particles=" + count + ",boxes=" + stats.size());
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 1.0E-9D;
    }

    private static boolean inside(double value, double min, double max) {
        return value >= min - 1.0E-9D && value <= max + 1.0E-9D;
    }

    private static final class CenterStats {
        private int count;
        private double x;
        private double y;
        private double z;
        private double vx;
        private double vy;
        private double vz;

        void add(EnhancedSlabDestroyParticleGeometry.ParticleMotion motion) {
            count++;
            x += motion.x();
            y += motion.y();
            z += motion.z();
            vx += motion.vx();
            vy += motion.vy();
            vz += motion.vz();
        }

        double meanX() { return x / count; }
        double meanY() { return y / count; }
        double meanZ() { return z / count; }
        double meanVx() { return vx / count; }
        double meanVy() { return vy / count; }
        double meanVz() { return vz / count; }
    }

    private static final class Checks {
        int count;
        boolean passed = true;
        final List<String> details = new ArrayList<>();

        void check(boolean okay, String detail) {
            count++;
            passed &= okay;
            details.add(detail + ",ok=" + okay);
        }

        Result result() {
            return new Result(passed, count, List.copyOf(details));
        }
    }
}
