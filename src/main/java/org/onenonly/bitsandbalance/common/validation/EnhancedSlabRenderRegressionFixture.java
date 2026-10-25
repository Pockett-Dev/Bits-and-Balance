package org.onenonly.bitsandbalance.common.validation;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
//? endif
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
//? endif

/** Model/cache regressions plus an explicitly enabled, marked-scratch-world live fixture. */
public final class EnhancedSlabRenderRegressionFixture {
    public record Result(boolean passed, int checked, List<String> details) {
    }

    private EnhancedSlabRenderRegressionFixture() {
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static LiveState live;

    private static final class LiveState {
        final MinecraftServer server;
        final ServerLevel level;
        final ServerPlayer player;
        final Vec3 origin;
        final float yaw, pitch;
        final GameType gameType;
        final BlockPos base;
        final Minecraft client;
        final boolean pauseOnLostFocus;
        final Holder<WorldClock> daylightClock;
        final long originalDaylightTicks;
        final List<BlockPos> changed = new ArrayList<>();
        final List<String> details = new ArrayList<>();
        final String screenshotPrefix = "bnb-quad-regression-" + System.currentTimeMillis();
        int ticks, count = 4;
        boolean passed = true;
        int checked;

        LiveState(Minecraft client, MinecraftServer server, ServerLevel level, ServerPlayer player, BlockPos base) {
            this.client = client;
            this.server = server;
            this.level = level;
            this.player = player;
            this.base = base;
            this.origin = player.position();
            this.yaw = player.getYRot();
            this.pitch = player.getXRot();
            this.gameType = player.gameMode.getGameModeForPlayer();
            this.pauseOnLostFocus = client.options.pauseOnLostFocus;
            this.daylightClock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
            //? if mc == 26.3
            this.originalDaylightTicks = server.clockManager().getInstance(daylightClock).totalTicks();
            //? else
            this.originalDaylightTicks = server.clockManager().getTotalTicks(daylightClock);
            //? endif
        }

        BlockPos quadPos(int family) {
            return base.offset(1 + 4 * family, 1, 1);
        }
    }
    //? endif

    /** Real packet/rebuild regression, restricted to explicitly enabled marked scratch worlds. */
    public static Result tickLive(Minecraft client) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        try {
            if (live == null) {
                MinecraftServer server = client.getSingleplayerServer();
                if (!Boolean.getBoolean("bitsandbalance.fixtureAllowMutation") || server == null
                        || !server.getWorldPath(LevelResource.ROOT).toString().toLowerCase().contains("scratch")
                        || !Files.isRegularFile(server.getWorldPath(LevelResource.ROOT).resolve(".bnb-mutable-fixture"))) {
                    return new Result(false, 0, List.of("live-render-requires-marked-scratch-world-and-mutation-flag"));
                }
                ServerPlayer player = server.getPlayerList().getPlayer(client.player.getUUID());
                ServerLevel level = server.getLevel(client.level.dimension());
                BlockPos base = player.blockPosition().offset(-5, 12, -4);
                live = new LiveState(client, server, level, player, base);
                LiveState test = live;
                client.options.pauseOnLostFocus = false;
                Result models = run(client);
                test.checked = models.checked();
                test.passed = models.passed();
                test.details.add("model-regression=" + models.passed() + ",checks=" + models.checked());
                if (!models.passed()) test.details.addAll(models.details());
                server.submit(() -> {
                    for (int x = 0; x < 11; x++) for (int z = 0; z < 4; z++) {
                        BlockPos floor = base.offset(x, 0, z);
                        if (!level.getBlockState(floor).isAir()) throw new IllegalStateException("fixture-floor-is-not-air");
                        test.changed.add(floor);
                    }
                    for (int family = 0; family < 3; family++) {
                        BlockPos pos = test.quadPos(family);
                        if (!level.getBlockState(pos).isAir()) throw new IllegalStateException("fixture-quad-is-not-air");
                        test.changed.add(pos);
                    }
                    for (BlockPos floor : test.changed) if (floor.getY() == base.getY()) {
                        level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
                    server.clockManager().setTotalTicks(test.daylightClock, 6000);
                    server.getPlayerList().broadcastAll(server.clockManager().createFullSyncPacket());
                    player.setYRot(180);
                    player.setXRot(18);
                    player.teleportTo(base.getX() + 5.5, base.getY() + 2.0, base.getZ() + 9.0);
                    setLiveCount(test, 4);
                    return true;
                }).get(10, TimeUnit.SECONDS);
                return null;
            }
            LiveState test = live;
            client.options.pauseOnLostFocus = false;
            client.setScreen(null);
            client.setCameraEntity(client.player);
            client.player.setPos(test.base.getX() + 5.5, test.base.getY() + 2.0, test.base.getZ() + 9.0);
            client.player.setYRot(180);
            client.player.setXRot(18);
            client.player.yRotO = 180;
            client.player.xRotO = 18;
            if (++test.ticks < 40) return null;
            test.ticks = 0;
            for (int family = 0; family < 3; family++) {
                BlockPos pos = test.quadPos(family);
                BlockEntity entity = client.level.getBlockEntity(pos);
                BlockState state = client.level.getBlockState(pos);
                int count = entity instanceof QuadStepBlockEntity quad ? quad.getCount()
                        : entity instanceof QuadVerticalStepBlockEntity quad ? quad.getCount()
                        : state.getBlock() instanceof StepBlock || state.getBlock() instanceof VerticalStepBlock ? 1 : 0;
                if (entity == null && (state.getBlock() instanceof QuadStepBlock || state.getBlock() instanceof QuadVerticalStepBlock)) {
                    EnhancedSlabMaterialSavedData.MaterialView slots = EnhancedSlabMaterialSavedData.getClientView(client.level, pos.asLong());
                    count = 0;
                    if (slots != null) for (int i = 0; i < 4; i++) if (slots.slot(i) != null) count++;
                }
                test.checked++;
                test.passed &= count == test.count;
                test.details.add("live-client family=" + family + ",expected=" + test.count + ",count=" + count);
            }
            String name = test.screenshotPrefix + "-" + test.count + "-pieces.png";
            test.details.add("screenshot=" + client.gameDirectory.toPath().resolve("screenshots").resolve(name));
            //? if mc == 26.1
            Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), 1,
                    message -> { });
            //? else
            Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1,
                    message -> { });
            //? endif
            if (test.count > 1) {
                test.count--;
                test.server.submit(() -> { setLiveCount(test, test.count); return true; }).get(10, TimeUnit.SECONDS);
                return null;
            }
            restoreLive(test);
            return new Result(test.passed, test.checked, List.copyOf(test.details));
        } catch (Throwable failure) {
            if (live != null) {
                try { restoreLive(live); } catch (Exception ignored) { }
            }
            return new Result(false, live == null ? 0 : live.checked, List.of("live-exception=" + failure));
        }
        //? else
        return new Result(false, 0, List.of("live-render-regression-requires-26.x"));
        //? endif
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static void setLiveCount(LiveState test, int count) {
        BlockState[] slots = new BlockState[4];
        for (int i = 0; i < count; i++) slots[i] = Blocks.BIRCH_SLAB.defaultBlockState();
        for (int family = 0; family < 3; family++) {
            BlockPos pos = test.quadPos(family);
            if (count == 1) {
                if (family < 2) {
                    QuadStepBlock.CachedBreak remaining = new QuadStepBlock.CachedBreak();
                    remaining.remainingSlabs = slots;
                    remaining.axis = family == 0 ? Direction.Axis.X : Direction.Axis.Z;
                    QuadStepBlock.bitsandbalance$restoreRemaining(test.level, pos, remaining);
                } else {
                    QuadVerticalStepBlock.CachedBreak remaining = new QuadVerticalStepBlock.CachedBreak();
                    remaining.remainingSlabs = slots;
                    QuadVerticalStepBlock.bitsandbalance$restoreRemaining(test.level, pos, remaining);
                }
                continue;
            }
            if (count == 4) {
                BlockState state = family < 2 ? CommonBlocks.QUAD_STEP.defaultBlockState()
                        .setValue(QuadStepBlock.AXIS, family == 0 ? Direction.Axis.X : Direction.Axis.Z)
                        : CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState();
                test.level.setBlock(pos, state, Block.UPDATE_ALL);
            }
            BlockEntity entity = test.level.getBlockEntity(pos);
            if (entity instanceof QuadStepBlockEntity quad) quad.setAllSlabs(slots);
            else if (entity instanceof QuadVerticalStepBlockEntity quad) quad.setAllSlabs(slots);
            else EnhancedSlabMaterialSavedData.remember(test.level, pos.asLong(), slots);
        }
    }

    private static void restoreLive(LiveState test) throws Exception {
        test.client.options.pauseOnLostFocus = test.pauseOnLostFocus;
        test.server.submit(() -> {
            for (BlockPos pos : test.changed) {
                test.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                EnhancedSlabMaterialSavedData.remember(test.level, pos.asLong(), new BlockState[4]);
            }
            test.player.gameMode.changeGameModeForPlayer(test.gameType);
            test.player.setYRot(test.yaw);
            test.player.setXRot(test.pitch);
            test.player.teleportTo(test.origin.x, test.origin.y, test.origin.z);
            test.server.clockManager().setTotalTicks(test.daylightClock, test.originalDaylightTicks);
            test.server.getPlayerList().broadcastAll(test.server.clockManager().createFullSyncPacket());
            return true;
        }).get(10, TimeUnit.SECONDS);
    }
    //? endif

    public static Result run(Minecraft client) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        ArrayList<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;
        BlockPos pos = client.player.blockPosition().offset(0, 128, 0);
        EnhancedSlabMaterialSavedData.MaterialView previous = EnhancedSlabMaterialSavedData.getClientView(client.level, pos.asLong());
        try {
            for (int family = 0; family < 3; family++) {
                boolean horizontal = family < 2;
                Direction.Axis axis = family == 0 ? Direction.Axis.X : Direction.Axis.Z;
                BlockState owner = horizontal ? CommonBlocks.QUAD_STEP.defaultBlockState().setValue(QuadStepBlock.AXIS, axis)
                        : CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState();
                BlockEntity entity = horizontal ? new QuadStepBlockEntity(pos, owner) : new QuadVerticalStepBlockEntity(pos, owner);
                BlockAndTintGetter world = (BlockAndTintGetter) Proxy.newProxyInstance(
                        EnhancedSlabRenderRegressionFixture.class.getClassLoader(), new Class<?>[]{BlockAndTintGetter.class},
                        (proxy, method, args) -> {
                            if (args != null && args.length > 0 && pos.equals(args[0])) {
                                if (method.getName().equals("getBlockEntity")) return owner.hasBlockEntity() ? entity : null;
                                if (method.getName().equals("getBlockState")) return owner;
                            }
                            return method.invoke(client.level, args);
                        });
                Object model = client.getModelManager().getBlockStateModelSet().get(owner);
                Method keyMethod = model.getClass().getMethod("createGeometryKey", BlockAndTintGetter.class,
                        BlockPos.class, BlockState.class, RandomSource.class);
                keyMethod.setAccessible(true);
                Map<Object, Double> geometryCache = new HashMap<>();
                Map<Object, Integer> occupancyByKey = new HashMap<>();
                Object fullKey = null;
                for (int mask : new int[]{15, 7, 3, 1, 15, 14, 12, 8, 15, 11, 9, 1, 15, 13, 5, 1}) {
                    BlockState[] slots = new BlockState[4];
                    for (int i = 0; i < 4; i++) if ((mask & (1 << i)) != 0) slots[i] = Blocks.BIRCH_SLAB.defaultBlockState();
                    if (entity instanceof QuadStepBlockEntity quad) quad.setAllSlabs(slots);
                    else ((QuadVerticalStepBlockEntity) entity).setAllSlabs(slots);
                    Object key = keyMethod.invoke(model, world, pos, owner, RandomSource.create(0));
                    if (mask == 15 || mask == 7 || mask == 3 || mask == 1) details.add("solid-probe family=" + family
                            + ",mask=" + mask + ",solid=" + owner.isSolid() + ",solid-render=" + owner.isSolidRender()
                            + ",full-collision=" + owner.isCollisionShapeFullBlock(world, pos));
                    boolean stable = mask != 15 || fullKey == null || fullKey.equals(key);
                    if (mask == 15) fullKey = key;
                    double area = renderedArea(world, pos, owner);
                    double cachedArea = geometryCache.computeIfAbsent(key, ignored -> area);
                    double expected = expectedArea(mask, horizontal, axis);
                    Integer cachedMask = occupancyByKey.putIfAbsent(key, mask);
                    boolean valid = key != null && stable && (cachedMask == null || cachedMask == mask)
                            && Math.abs(cachedArea - expected) < 1.0E-5D;
                    checked++;
                    passed &= valid;
                    details.add("family=" + family + ",mask=" + mask + ",cached-area=" + cachedArea
                            + ",expected=" + expected + ",key-stable=" + stable + ",ok=" + valid);
                    for (Direction direction : Direction.values()) {
                        boolean expectedFull = expectedFullFace(mask, horizontal, axis, direction);
                        boolean actualFull = owner.isFaceSturdy(world, pos, direction, SupportType.FULL);
                        checked++;
                        passed &= expectedFull == actualFull;
                        if (expectedFull != actualFull) details.add("partial-face-as-full family=" + family
                                + ",mask=" + mask + ",face=" + direction + ",actual=" + actualFull);
                    }
                }
                Object beforeMaterialEdit = keyMethod.invoke(model, world, pos, owner, RandomSource.create(0));
                BlockState[] changedMaterial = new BlockState[]{Blocks.OAK_SLAB.defaultBlockState(), null, null, null};
                if (entity instanceof QuadStepBlockEntity quad) quad.setAllSlabs(changedMaterial);
                else ((QuadVerticalStepBlockEntity) entity).setAllSlabs(changedMaterial);
                Object afterMaterialEdit = keyMethod.invoke(model, world, pos, owner, RandomSource.create(0));
                boolean materialChangedKey = !beforeMaterialEdit.equals(afterMaterialEdit);
                checked++;
                passed &= materialChangedKey;
                details.add("material-edit family=" + family + ",key-changed=" + materialChangedKey);
            }
            BlockState single = StepDynamicRegistry.getStepForSlab(Blocks.BIRCH_SLAB).defaultBlockState();
            details.add("single-solid-probe solid=" + single.isSolid() + ",solid-render=" + single.isSolidRender()
                    + ",full-collision=" + single.isCollisionShapeFullBlock(client.level, pos));
            for (Direction facing : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                for (boolean top : new boolean[]{false, true}) {
                    BlockState state = single.setValue(StepBlock.FACING, facing).setValue(StepBlock.TOP, top);
                    BlockAndTintGetter singleWorld = (BlockAndTintGetter) Proxy.newProxyInstance(
                            EnhancedSlabRenderRegressionFixture.class.getClassLoader(), new Class<?>[]{BlockAndTintGetter.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("getBlockState") && args != null && pos.equals(args[0])) return state;
                                return method.invoke(client.level, args);
                            });
                    for (Direction direction : Direction.values()) {
                        boolean expectedFull = SupportType.FULL.isSupporting(state, singleWorld, pos, direction);
                        boolean actualFull = state.isFaceSturdy(singleWorld, pos, direction, SupportType.FULL);
                        checked++;
                        passed &= expectedFull == actualFull;
                        if (expectedFull != actualFull) details.add("single-partial-face-as-full facing=" + facing
                                + ",top=" + top + ",face=" + direction);
                    }
                    boolean attachmentPreserved = state.isFaceSturdy(client.level, pos, facing, SupportType.FULL)
                            && state.isFaceSturdy(client.level, pos, top ? Direction.UP : Direction.DOWN, SupportType.FULL);
                    checked++;
                    passed &= attachmentPreserved;
                    if (!attachmentPreserved) details.add("single-attachment-regression facing=" + facing + ",top=" + top);
                }
            }
        } catch (Throwable failure) {
            passed = false;
            details.add("exception=" + failure);
        } finally {
            if (previous == null) EnhancedSlabMaterialSavedData.removeClient(client.level, pos.asLong());
            else EnhancedSlabMaterialSavedData.rememberClient(client.level, pos.asLong(), previous.copySlots());
        }
        return new Result(passed, checked, details);
        //? else
        return new Result(true, 0, List.of("26.x-render-regression-not-applicable"));
        //? endif
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    private static double renderedArea(BlockAndTintGetter world, BlockPos pos, BlockState state) throws Exception {
        BlockEntity entity = world.getBlockEntity(pos);
        EnhancedSlabMaterialSavedData.MaterialView stored = EnhancedSlabMaterialSavedData.getClientView(pos.asLong());
        int mask = 0;
        for (int i = 0; i < 4; i++) {
            BlockState slab = entity instanceof QuadStepBlockEntity quad ? quad.getSlabAt(i)
                    : entity instanceof QuadVerticalStepBlockEntity quad ? quad.getSlabAt(i)
                    : stored == null ? null : stored.slot(i);
            if (slab != null) mask |= 1 << i;
        }
        boolean horizontal = state.getBlock() instanceof QuadStepBlock;
        Direction.Axis axis = horizontal ? state.getValue(QuadStepBlock.AXIS) : Direction.Axis.X;
        //? if fabric
        ArrayList<Object> parts = new ArrayList<>();
        org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper.collectModelParts(world, pos, state, parts);
        //? else
        List<?> parts = org.onenonly.bitsandbalance.client.renderer.EnhancedSlabTerrainRenderHelper.collectModelParts(world, pos, state);
        //? endif
        double area = 0;
        for (Object part : parts) {
            Method getQuads = part.getClass().getMethod("getQuads", Direction.class);
            getQuads.setAccessible(true);
            for (Direction direction : new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH,
                    Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                for (Object quad : (List<?>) getQuads.invoke(part, new Object[]{direction})) {
                    Method position = quad.getClass().getMethod("position", int.class);
                    Vector3fc a = (Vector3fc) position.invoke(quad, 0);
                    Vector3fc b = (Vector3fc) position.invoke(quad, 1);
                    Vector3fc c = (Vector3fc) position.invoke(quad, 2);
                    Vector3fc d = (Vector3fc) position.invoke(quad, 3);
                    Direction normal = (Direction) quad.getClass().getMethod("direction").invoke(quad);
                    // Count the exposed surface only; some loader paths also submit hidden shared faces.
                    int x = (int) Math.floor((a.x() + b.x() + c.x() + d.x()) / 2.0 + normal.getStepX() * 0.001);
                    int y = (int) Math.floor((a.y() + b.y() + c.y() + d.y()) / 2.0 + normal.getStepY() * 0.001);
                    int z = (int) Math.floor((a.z() + b.z() + c.z() + d.z()) / 2.0 + normal.getStepZ() * 0.001);
                    if (occupied(mask, horizontal, axis, x, y, z)) continue;
                    area += triangleArea(a, b, c) + triangleArea(a, c, d);
                }
            }
        }
        return area;
    }

    private static double triangleArea(Vector3fc a, Vector3fc b, Vector3fc c) {
        double ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
        double vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();
        double x = uy * vz - uz * vy, y = uz * vx - ux * vz, z = ux * vy - uy * vx;
        return Math.sqrt(x * x + y * y + z * z) / 2;
    }

    /** Independent eight-cell surface oracle; counts only faces next to empty space. */
    private static boolean expectedFullFace(int mask, boolean horizontal, Direction.Axis axis, Direction face) {
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            boolean boundary = switch (face) {
                case DOWN -> y == 0;
                case UP -> y == 1;
                case WEST -> x == 0;
                case EAST -> x == 1;
                case NORTH -> z == 0;
                case SOUTH -> z == 1;
            };
            if (boundary && !occupied(mask, horizontal, axis, x, y, z)) return false;
        }
        return true;
    }

    /** Independent eight-cell surface oracle; counts only faces next to empty space. */
    private static double expectedArea(int mask, boolean horizontal, Direction.Axis axis) {
        double area = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            if (!occupied(mask, horizontal, axis, x, y, z)) continue;
            for (Direction direction : Direction.values()) {
                if (!occupied(mask, horizontal, axis, x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ())) area += 0.25;
            }
        }
        return area;
    }

    private static boolean occupied(int mask, boolean horizontal, Direction.Axis axis, int x, int y, int z) {
        if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) return false;
        int slot = horizontal ? (axis == Direction.Axis.X ? x : z) + 2 * y : x + 2 * z;
        return (mask & (1 << slot)) != 0;
    }
    //? endif
}
