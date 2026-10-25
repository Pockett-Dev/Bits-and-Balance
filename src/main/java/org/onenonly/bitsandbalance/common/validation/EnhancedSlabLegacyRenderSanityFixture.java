package org.onenonly.bitsandbalance.common.validation;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

//? if mc == 1.21.1 || mc == 1.21.11
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
//? endif
//? if mc == 1.21.1 && neoforge
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
//? endif

/** Live older-version sanity checks. Never runs against an unmarked player world. */
public final class EnhancedSlabLegacyRenderSanityFixture {
    public record Result(boolean passed, int checked, List<String> details) { }
    private EnhancedSlabLegacyRenderSanityFixture() { }

    //? if mc == 1.21.1 || mc == 1.21.11
    private static Live live;
    private static final class Live {
        final MinecraftServer server;
        final ServerLevel level;
        final ServerPlayer player;
        final Vec3 origin;
        final float yaw, pitch;
        final GameType gameType;
        final long dayTime;
        final BlockPos base;
        final List<BlockPos> changed = new ArrayList<>();
        final List<String> details = new ArrayList<>();
        final String prefix = "bnb-legacy-slab-sanity-" + System.currentTimeMillis();
        int ticks, count = 4, checked;
        boolean passed = true;
        Live(MinecraftServer server, ServerLevel level, ServerPlayer player) {
            this.server = server; this.level = level; this.player = player;
            origin = player.position(); yaw = player.getYRot(); pitch = player.getXRot();
            gameType = player.gameMode.getGameModeForPlayer();
            dayTime = level.getDayTime();
            base = player.blockPosition().offset(-10, 16, -4);
        }
        BlockPos pos(int family) { return base.offset(1 + 3 * family, 1, 1); }
        void check(boolean okay, String detail) {
            checked++; passed &= okay; details.add(detail + ",ok=" + okay);
        }
    }
    //? endif

    public static Result tick(Minecraft client) {
        //? if mc == 1.21.1 || mc == 1.21.11
        try {
            if (live == null) {
                MinecraftServer server = client.getSingleplayerServer();
                if (!Boolean.getBoolean("bitsandbalance.fixtureAllowMutation") || server == null
                        || !server.getWorldPath(LevelResource.ROOT).toString().toLowerCase().contains("scratch")
                        || !Files.isRegularFile(server.getWorldPath(LevelResource.ROOT).resolve(".bnb-mutable-fixture"))) {
                    return new Result(false, 0, List.of("requires-marked-scratch-world-and-mutation-flag"));
                }
                ServerPlayer player = server.getPlayerList().getPlayer(client.player.getUUID());
                live = new Live(server, server.getLevel(client.level.dimension()), player);
                Live test = live;
                server.submit(() -> {
                    for (int x = 0; x < 22; x++) for (int z = 0; z < 4; z++) {
                        BlockPos floor = test.base.offset(x, 0, z);
                        if (!test.level.getBlockState(floor).isAir()) throw new IllegalStateException("scene-floor-is-not-air");
                        test.changed.add(floor);
                    }
                    for (int family = 0; family < 7; family++) {
                        BlockPos pos = test.pos(family);
                        if (!test.level.getBlockState(pos).isAir()) throw new IllegalStateException("scene-block-is-not-air");
                        test.changed.add(pos);
                    }
                    for (BlockPos pos : test.changed) if (pos.getY() == test.base.getY())
                        test.level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
                    test.level.setDayTime(6000);
                    player.teleportTo(test.base.getX() + 10.5, test.base.getY() + 3, test.base.getZ() + 15);
                    setStage(test, 4);
                    return true;
                }).get(10, TimeUnit.SECONDS);
                return null;
            }
            Live test = live;
            client.setCameraEntity(client.player);
            client.player.setPos(test.base.getX() + 10.5, test.base.getY() + 3, test.base.getZ() + 15);
            client.player.setYRot(180); client.player.setXRot(12);
            client.player.yRotO = 180; client.player.xRotO = 12;
            if (++test.ticks < 60) return null;
            test.ticks = 0;
            for (int family = 0; family < 7; family++) {
                BlockPos pos = test.pos(family);
                BlockState state = client.level.getBlockState(pos);
                double volume = state.getCollisionShape(client.level, pos).toAabbs().stream()
                        .mapToDouble(box -> (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ)).sum();
                double expected = family < 3 ? test.count / 4.0 : family < 5 ? 1 : .25;
                test.check(Math.abs(volume - expected) < .000001, "collision family=" + family + ",stage=" + test.count
                        + ",volume=" + volume + ",expected=" + expected);
                Object model = client.getBlockRenderer().getBlockModel(state);
                test.check(model != null, "loaded-model family=" + family + ",type=" + (model == null ? "null" : model.getClass().getName()));
                //? if mc == 1.21.1 && neoforge
                if (family == 2) checkVerticalMesh(client, test, pos, state, (BakedModel) model);
                //? endif
                if (family < 3) {
                    BlockEntity entity = client.level.getBlockEntity(pos);
                    int count = entity instanceof QuadStepBlockEntity quad ? quad.getCount()
                            : entity instanceof QuadVerticalStepBlockEntity quad ? quad.getCount()
                            : state.getBlock() instanceof StepBlock || state.getBlock() instanceof VerticalStepBlock ? 1 : 0;
                    if (entity == null && (state.getBlock() instanceof QuadStepBlock || state.getBlock() instanceof QuadVerticalStepBlock)) {
                        count = 0;
                        EnhancedSlabMaterialSavedData.MaterialView view = EnhancedSlabMaterialSavedData.getClientView(client.level, pos.asLong());
                        if (view != null) for (int i = 0; i < 4; i++) if (view.slot(i) != null) count++;
                    }
                    test.check(count == test.count, "client-slots family=" + family + ",expected=" + test.count + ",actual=" + count);
                } else if (family < 5) {
                    EnhancedSlabMaterialSavedData.MaterialView view = EnhancedSlabMaterialSavedData.getClientView(client.level, pos.asLong());
                    BlockState material = test.count >= 3 ? Blocks.OAK_SLAB.defaultBlockState() : Blocks.BIRCH_SLAB.defaultBlockState();
                    test.check(view != null && material.equals(view.slot(0)) && Blocks.SPRUCE_SLAB.defaultBlockState().equals(view.slot(1)),
                            "mixed-client-material family=" + family + ",material=" + material);
                } else {
                    Block slab = test.count >= 3 ? Blocks.OAK_SLAB : Blocks.BIRCH_SLAB;
                    Block expectedBlock = family == 5 ? StepDynamicRegistry.getStepForSlab(slab)
                            : VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(VerticalSlabDynamicRegistry.getVerticalForSlab(slab));
                    test.check(state.getBlock() == expectedBlock, "fixed-material family=" + family + ",expected=" + expectedBlock);
                }
            }
            String filename = test.prefix + "-" + test.count + "-pieces.png";
            test.details.add("screenshot=" + client.gameDirectory.toPath().resolve("screenshots").resolve(filename));
            //? if mc == 1.21.1
            Screenshot.grab(client.gameDirectory, filename, client.getMainRenderTarget(), message -> { });
            //? else
            Screenshot.grab(client.gameDirectory, filename, client.getMainRenderTarget(), 1, message -> { });
            //? endif
            if (test.count > 1) {
                test.count--;
                test.server.submit(() -> { setStage(test, test.count); return true; }).get(10, TimeUnit.SECONDS);
                return null;
            }
            restore(test);
            test.details.add("scratch-scene-restored");
            return new Result(test.passed, test.checked, List.copyOf(test.details));
        } catch (Throwable failure) {
            if (live != null) try { restore(live); } catch (Exception ignored) { }
            return new Result(false, live == null ? 0 : live.checked, List.of("legacy-live-exception=" + failure));
        }
        //? else
        return new Result(false, 0, List.of("legacy-sanity-requires-1.21.1-or-1.21.11"));
        //? endif
    }

    //? if mc == 1.21.1 && neoforge
    /** Independent numeric mesh bounds for the legacy NeoForge vertical-quad renderer. */
    private static void checkVerticalMesh(Minecraft client, Live test, BlockPos pos, BlockState state, BakedModel model) throws Exception {
        ModelData data = model.getModelData(client.level, pos, state, ModelData.EMPTY);
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        List<BakedQuad> quads = new ArrayList<>();
        quads.addAll(model.getQuads(state, null, RandomSource.create(0), data, RenderType.solid()));
        for (Direction direction : Direction.values())
            quads.addAll(model.getQuads(state, direction, RandomSource.create(0), data, RenderType.solid()));
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) for (int axis = 0; axis < 3; axis++) {
                float coordinate = Float.intBitsToFloat(vertices[vertex * stride + axis]);
                min[axis] = Math.min(min[axis], coordinate);
                max[axis] = Math.max(max[axis], coordinate);
            }
        }
        Object kind = "static-model";
        Object slots = "static-model";
        try {
            java.lang.reflect.Field kindField = model.getClass().getDeclaredField("RENDER_KIND");
            kindField.setAccessible(true);
            kind = data.get((ModelProperty<?>) kindField.get(null));
            java.lang.reflect.Field slotsField = model.getClass().getDeclaredField("MATERIAL_SLOTS");
            slotsField.setAccessible(true);
            slots = java.util.Arrays.toString((BlockState[]) data.get((ModelProperty<?>) slotsField.get(null)));
        } catch (NoSuchFieldException ignored) { }
        double expectedX = test.count == 1 ? .5 : 1;
        double expectedZ = test.count <= 2 ? .5 : 1;
        boolean valid = !quads.isEmpty() && Math.abs(min[0]) < .000001 && Math.abs(min[1]) < .000001
                && Math.abs(min[2]) < .000001 && Math.abs(max[0] - expectedX) < .000001
                && Math.abs(max[1] - 1) < .000001 && Math.abs(max[2] - expectedZ) < .000001;
        test.check(valid, "vertical-mesh stage=" + test.count + ",state=" + state + ",kind=" + kind
                + ",slots=" + slots + ",quad-count=" + quads.size() + ",min=" + java.util.Arrays.toString(min)
                + ",max=" + java.util.Arrays.toString(max) + ",expected-max=[" + expectedX + ",1.0," + expectedZ + "]"
                + ",collision-bounds=" + state.getCollisionShape(client.level, pos).bounds());
    }
    //? endif

    //? if mc == 1.21.1 || mc == 1.21.11
    private static void setStage(Live test, int count) {
        BlockState[] slots = new BlockState[4];
        for (int i = 0; i < count; i++) slots[i] = Blocks.BIRCH_SLAB.defaultBlockState();
        for (int family = 0; family < 3; family++) {
            BlockPos pos = test.pos(family);
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
            if (count == 4) test.level.setBlock(pos, family < 2 ? CommonBlocks.QUAD_STEP.defaultBlockState()
                    .setValue(QuadStepBlock.AXIS, family == 0 ? Direction.Axis.X : Direction.Axis.Z)
                    : CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState(), Block.UPDATE_ALL);
            BlockEntity entity = test.level.getBlockEntity(pos);
            if (entity instanceof QuadStepBlockEntity quad) quad.setAllSlabs(slots);
            else if (entity instanceof QuadVerticalStepBlockEntity quad) quad.setAllSlabs(slots);
            else EnhancedSlabMaterialSavedData.remember(test.level, pos.asLong(), slots);
            test.level.sendBlockUpdated(pos, test.level.getBlockState(pos), test.level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
        Block slab = count >= 3 ? Blocks.OAK_SLAB : Blocks.BIRCH_SLAB;
        BlockState[] pair = new BlockState[]{slab.defaultBlockState(), Blocks.SPRUCE_SLAB.defaultBlockState(), null, null};
        for (int family = 3; family < 5; family++) {
            BlockPos pos = test.pos(family);
            if (count == 4) test.level.setBlock(pos, family == 3 ? CommonBlocks.MIXED_SLAB.defaultBlockState()
                    : CommonBlocks.VERTICAL_SLAB.defaultBlockState().setValue(VerticalSlabBlock.DOUBLE, true), Block.UPDATE_ALL);
            BlockEntity entity = test.level.getBlockEntity(pos);
            if (entity instanceof VerticalSlabBlockEntity vertical) vertical.setSlabs(pair[0], pair[1]);
            EnhancedSlabMaterialSavedData.remember(test.level, pos.asLong(), pair);
            test.level.sendBlockUpdated(pos, test.level.getBlockState(pos), test.level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
        test.level.setBlock(test.pos(5), StepDynamicRegistry.getStepForSlab(slab).defaultBlockState(), Block.UPDATE_ALL);
        test.level.setBlock(test.pos(6), VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(
                VerticalSlabDynamicRegistry.getVerticalForSlab(slab)).defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void restore(Live test) throws Exception {
        test.server.submit(() -> {
            for (BlockPos pos : test.changed) {
                test.level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                EnhancedSlabMaterialSavedData.remember(test.level, pos.asLong(), new BlockState[4]);
            }
            test.player.gameMode.changeGameModeForPlayer(test.gameType);
            test.level.setDayTime(test.dayTime);
            test.player.setYRot(test.yaw); test.player.setXRot(test.pitch);
            test.player.teleportTo(test.origin.x, test.origin.y, test.origin.z);
            return true;
        }).get(10, TimeUnit.SECONDS);
    }
    //? endif
}
