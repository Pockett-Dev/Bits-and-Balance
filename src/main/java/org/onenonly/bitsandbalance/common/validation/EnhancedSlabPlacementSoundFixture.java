package org.onenonly.bitsandbalance.common.validation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Actual item-placement regression with a reversible, late source-sound replacement. */
public final class EnhancedSlabPlacementSoundFixture {
    public record Result(boolean passed, int checked, List<String> details) { }
    private EnhancedSlabPlacementSoundFixture() { }
    private static boolean prepared;
    private static boolean originalPauseOnLostFocus;

    public static Result run(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        if (!Boolean.getBoolean("bitsandbalance.fixtureAllowMutation") || server == null
                || !server.getWorldPath(LevelResource.ROOT).toString().toLowerCase().contains("scratch")
                || !Files.isRegularFile(server.getWorldPath(LevelResource.ROOT).resolve(".bnb-mutable-fixture"))) {
            return new Result(false, 0, List.of("sound-fixture-requires-marked-scratch-and-mutation-flag"));
        }
        if (!prepared) {
            prepared = true;
            originalPauseOnLostFocus = client.options.pauseOnLostFocus;
            client.options.pauseOnLostFocus = false;
            client.setScreen(null);
            return null;
        }
        if (client.isPaused()) {
            client.setScreen(null);
            return null;
        }
        try {
            return server.submit(() -> test(server, client)).get(30, TimeUnit.SECONDS);
        } catch (Exception failure) {
            return new Result(false, 0, List.of("sound-fixture-exception=" + failure));
        } finally {
            client.options.pauseOnLostFocus = originalPauseOnLostFocus;
        }
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
    }

    private static Result test(MinecraftServer server, Minecraft client) {
        ServerPlayer player = server.getPlayerList().getPlayer(client.player.getUUID());
        ServerLevel level = server.getLevel(client.level.dimension());
        Checks checks = new Checks();
        ItemStack oldHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
        GameType oldMode = player.gameMode.getGameModeForPlayer();
        BlockPos base = player.blockPosition().offset(3, 12, 3);
        ArrayList<BlockPos> created = new ArrayList<>();
        try {
            // Complete validation before mutating even this explicitly marked scratch scene.
            for (int x = 0; x < 6; x++) for (int z = 0; z < 4; z++) for (int y = 0; y < 3; y++) {
                BlockPos pos = base.offset(x, y, z);
                if (!level.getBlockState(pos).isAir()) throw new IllegalStateException("sound-scene-not-air:" + pos);
                created.add(pos);
            }
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            int soundsSourcePairs = 0;
            boolean soundsPresent = optionalField(Blocks.BIRCH_SLAB.getClass(), "associatedtagpair") != null;
            for (Block source : new Block[]{Blocks.BIRCH_SLAB, Blocks.STONE_SLAB, Blocks.MUD_BRICK_SLAB}) {
                SourceSoundProbe probe = SourceSoundProbe.install(source);
                if (probe.usesSoundsPair()) soundsSourcePairs++;
                try {
                    for (boolean vertical : new boolean[]{false, true}) {
                        Block itemBlock = vertical ? VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(
                                VerticalSlabDynamicRegistry.getVerticalForSlab(source)) : StepDynamicRegistry.getStepForSlab(source);
                        BlockItem item = (BlockItem) itemBlock.asItem();
                        BlockPos floor = base.offset(vertical ? 3 : 0, 0, 0);
                        BlockPos owner = floor.above();
                        level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 64));
                        probe.reset();
                        UseOnContext first = context(player, floor, Direction.UP,
                                new Vec3(floor.getX() + .25, floor.getY() + 1, floor.getZ() + .25));
                        InteractionResult result = item.useOn(first);
                        verify(checks, level, owner, probe, result, 1, source, vertical, "useOn");
                        BlockState single = level.getBlockState(owner);
                        Direction firstFacing = single.getValue(vertical ? VerticalStepBlock.FACING : StepBlock.FACING);
                        Direction firstSide = vertical ? single.getValue(VerticalStepBlock.SIDE) : firstFacing;
                        boolean firstTop = !vertical && single.getValue(StepBlock.TOP);
                        for (int count = 2; count <= 4; count++) {
                            BlockHitResult hit = nextHit(owner, vertical, firstFacing, firstSide, firstTop, count);
                            probe.reset();
                            result = item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                            verify(checks, level, owner, probe, result, count, source, vertical, "useOn");
                        }
                        clear(level, owner);
                        // Direct place() exercises vanilla fallback independently of useOn mixins.
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 64));
                        probe.reset();
                        result = item.place(new BlockPlaceContext(first));
                        verify(checks, level, owner, probe, result, 1, source, vertical, "vanilla-place");
                        clear(level, owner);
                    }
                } finally {
                    probe.restore();
                }
            }
            if (soundsPresent) {
                checks.check(soundsSourcePairs > 0, "sounds-source-pairs=" + soundsSourcePairs);
            }
            checks.details.add("normal-block-sound-state-not-rewritten");
            checks.check(org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStepPlacementHelper
                    .placementSoundState(Blocks.STONE.defaultBlockState()) == Blocks.STONE.defaultBlockState(), "normal-block-identity");
        } catch (Throwable failure) {
            checks.passed = false;
            checks.details.add("exception=" + failure);
        } finally {
            for (BlockPos pos : created) clear(level, pos);
            player.setItemInHand(InteractionHand.MAIN_HAND, oldHand);
            player.gameMode.changeGameModeForPlayer(oldMode);
        }
        checks.details.add("source-sounds-hand-mode-and-scratch-scene-restored");
        return new Result(checks.passed, checks.count, List.copyOf(checks.details));
    }

    private static void verify(Checks checks, ServerLevel level, BlockPos owner, SourceSoundProbe probe,
                               InteractionResult result, int pieces, Block source, boolean vertical, String path) {
        MarkerSound marker = probe.marker;
        MarkerSound bypass = probe.bypass;
        String key = "source=" + source + ",vertical=" + vertical + ",pieces=" + pieces + ",path=" + path
                + ",sounds-pair=" + probe.usesSoundsPair();
        double volume = level.getBlockState(owner).getCollisionShape(level, owner).toAabbs().stream()
                .mapToDouble(box -> (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ)).sum();
        checks.check(result.consumesAction(), key + ",placed=" + result);
        checks.check(Math.abs(volume - pieces / 4.0) < .000001, key + ",collision-volume=" + volume);
        checks.check(marker.placeCalls == 1 && (bypass == null || bypass.placeCalls == 0),
                key + ",source-place-sound-calls=" + marker.placeCalls
                        + ",bypass-place-sound-calls=" + (bypass == null ? 0 : bypass.placeCalls));
        checks.check(marker.volumeCalls == 1 && marker.pitchCalls == 1
                        && (bypass == null || (bypass.volumeCalls == 0 && bypass.pitchCalls == 0)),
                key + ",source-volume-calls=" + marker.volumeCalls + ",source-pitch-calls=" + marker.pitchCalls
                        + ",bypass-volume-calls=" + (bypass == null ? 0 : bypass.volumeCalls)
                        + ",bypass-pitch-calls=" + (bypass == null ? 0 : bypass.pitchCalls));
    }

    private static UseOnContext context(ServerPlayer player, BlockPos pos, Direction face, Vec3 point) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, pos, false));
    }

    private static BlockHitResult nextHit(BlockPos pos, boolean vertical, Direction facing, Direction side, boolean top, int count) {
        Direction face;
        double x = .25, y = .5, z = .25;
        if (vertical) {
            boolean east = side == Direction.EAST, south = facing == Direction.SOUTH;
            if (count == 3) {
                face = facing.getOpposite();
                x = east ? .75 : .25;
                z = south ? .4999 : .5001;
            } else {
                face = side.getOpposite();
                x = east ? .4999 : .5001;
                z = (count == 4 ? !south : south) ? .75 : .25;
            }
        } else {
            boolean positive = facing == Direction.EAST || facing == Direction.SOUTH;
            if (count >= 3) {
                face = top ? Direction.DOWN : Direction.UP;
                y = top ? .4999 : .5001;
                boolean targetPositive = count == 4 ? !positive : positive;
                if (facing.getAxis() == Direction.Axis.X) x = targetPositive ? .75 : .25;
                else z = targetPositive ? .75 : .25;
            } else {
                face = facing.getOpposite();
                y = (count == 4 ? !top : top) ? .75 : .25;
                if (facing.getAxis() == Direction.Axis.X) x = positive ? .4999 : .5001;
                else z = positive ? .4999 : .5001;
            }
        }
        return new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), face, pos, false);
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong(), new BlockState[4]);
    }

    private static Field soundField(Class<?> type) throws ReflectiveOperationException {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            try {
                Field field = owner.getDeclaredField("soundType");
                if (field.getType() == SoundType.class) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(type.getName() + ".soundType");
    }

    private static Field optionalField(Class<?> type, String nameFragment) {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            for (Field field : owner.getDeclaredFields()) {
                if (field.getName().toLowerCase().contains(nameFragment)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }

    private static Field optionalSoundTypeField(Class<?> type, String nameFragment) {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            for (Field field : owner.getDeclaredFields()) {
                if (field.getType() == SoundType.class
                        && field.getName().toLowerCase().contains(nameFragment)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }

    private static final class SourceSoundProbe {
        final Block source;
        final Field sourceField;
        final SoundType originalSource;
        final MarkerSound marker;
        final MarkerSound bypass;
        final Object soundsPair;
        final Field soundsGroupField;
        final SoundType originalSoundsGroup;

        private SourceSoundProbe(Block source, Field sourceField, SoundType originalSource, MarkerSound marker,
                                 MarkerSound bypass, Object soundsPair, Field soundsGroupField,
                                 SoundType originalSoundsGroup) {
            this.source = source;
            this.sourceField = sourceField;
            this.originalSource = originalSource;
            this.marker = marker;
            this.bypass = bypass;
            this.soundsPair = soundsPair;
            this.soundsGroupField = soundsGroupField;
            this.originalSoundsGroup = originalSoundsGroup;
        }

        static SourceSoundProbe install(Block source) throws ReflectiveOperationException {
            Field sourceField = soundField(source.getClass());
            SoundType originalSource = (SoundType) sourceField.get(source);
            Field pairField = optionalField(source.getClass(), "associatedtagpair");
            Object pair = null;
            Field groupField = null;
            SoundType originalGroup = null;
            MarkerSound marker;
            MarkerSound bypass = null;

            if (pairField != null) {
                // Let Sounds resolve the source slab through its real tag cache before installing probes.
                source.defaultBlockState().getSoundType();
                pair = pairField.get(source);
            }
            if (pair != null) {
                groupField = optionalSoundTypeField(pair.getClass(), "group");
                if (groupField == null) {
                    throw new NoSuchFieldException(pair.getClass().getName() + ".group");
                }
                originalGroup = (SoundType) groupField.get(pair);
                marker = new MarkerSound(originalGroup);
                bypass = new MarkerSound(originalSource);
                // Distinct probes prove Sounds handled the source lookup; a vanilla fallback is a failure.
                sourceField.set(source, bypass);
                groupField.set(pair, marker);
            } else {
                marker = new MarkerSound(originalSource);
                // Generated Steps already copied the original sound during registration. Replace only the
                // source now, as a runtime sound-changing mod can do.
                sourceField.set(source, marker);
            }
            return new SourceSoundProbe(source, sourceField, originalSource, marker, bypass,
                    pair, groupField, originalGroup);
        }

        boolean usesSoundsPair() {
            return soundsPair != null;
        }

        void reset() {
            marker.reset();
            if (bypass != null) bypass.reset();
        }

        void restore() throws IllegalAccessException {
            if (soundsGroupField != null) soundsGroupField.set(soundsPair, originalSoundsGroup);
            sourceField.set(source, originalSource);
        }
    }

    private static final class MarkerSound extends SoundType {
        int placeCalls, volumeCalls, pitchCalls;
        MarkerSound(SoundType original) {
            super(original.getVolume(), original.getPitch(), original.getBreakSound(), original.getStepSound(),
                    original.getPlaceSound(),
                    original.getHitSound(), original.getFallSound());
        }
        void reset() { placeCalls = volumeCalls = pitchCalls = 0; }
        @Override public net.minecraft.sounds.SoundEvent getPlaceSound() { placeCalls++; return super.getPlaceSound(); }
        @Override public float getVolume() { volumeCalls++; return super.getVolume(); }
        @Override public float getPitch() { pitchCalls++; return super.getPitch(); }
    }
}
