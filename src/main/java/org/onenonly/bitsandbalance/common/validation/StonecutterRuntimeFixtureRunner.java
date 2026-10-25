package org.onenonly.bitsandbalance.common.validation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Instruments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
//? if mc != 1.21.1
import net.minecraft.world.item.component.InstrumentComponent;
//? endif
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.CloudBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.onenonly.bitsandbalance.common.client.DreamProjectionClient;
import org.onenonly.bitsandbalance.common.client.YearnEchoClient;
import org.onenonly.bitsandbalance.common.compat.litematica.LitematicaEnhancedSlabCompat;
import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.onenonly.bitsandbalance.common.content.ModBottleOfCloud;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabLandingParticleHelper;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabCreateCompat;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonData;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPistonHooks;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPortableNbt;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabStorageOptimizer;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingBlockSupport;
import org.onenonly.bitsandbalance.common.mechanics.AutoWaterloggingRuntime;
import org.onenonly.bitsandbalance.common.mechanics.DreamProjection;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornCooldowns;
import org.onenonly.bitsandbalance.common.mechanics.GoatHornPurposes;
import org.onenonly.bitsandbalance.common.mechanics.OreSmeltingXpRuntime;
import org.onenonly.bitsandbalance.common.mechanics.YearnEcho;
import org.onenonly.bitsandbalance.common.mechanics.VerticalStepPlacementGeometry;
import org.onenonly.bitsandbalance.common.registry.CommonBlocks;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.common.util.PickBlockHelperCompat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class StonecutterRuntimeFixtureRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-fixture");
    private static final String ENABLE_PROPERTY = "bitsandbalance.fixtureRunner";
    private static final String ENABLE_ENV = "BITSANDBALANCE_FIXTURE_RUNNER";
    private static final String TARGET_PROPERTY = "bitsandbalance.fixtureTarget";
    private static final String TARGET_ENV = "BITSANDBALANCE_FIXTURE_TARGET";
    private static final String EXIT_PROPERTY = "bitsandbalance.fixtureExit";
    private static final String EXIT_ENV = "BITSANDBALANCE_FIXTURE_EXIT";
    private static final String ALLOW_MUTATION_PROPERTY = "bitsandbalance.fixtureAllowMutation";
    private static final String SCENARIO_PROPERTY = "bitsandbalance.fixtureScenario";
    private static final String PHASE_PROPERTY = "bitsandbalance.fixturePhase";
    private static final String BASE_POS_PROPERTY = "bitsandbalance.fixtureBasePos";
    private static final String RUNNER_LEDGER = "enhanced-slab-ledger";
    private static final String RUNNER_MUTABLE = "enhanced-slab-mutable";
    private static final String RUNNER_LITEMATICA_SCHEMATIC_SMOKE = "litematica-schematic-smoke";
    private static final String RUNNER_CLIENT_RENDER_SMOKE = "client-render-smoke";
    private static final String RUNNER_DREAM_PROJECTION_SMOKE = "dream-projection-smoke";
    private static final String RUNNER_ORE_SMELTING_XP = "ore-smelting-xp";
    private static final String RUNNER_DREAM_CLIENT_MOTION_SMOKE = "dream-client-motion-smoke";
    private static final String RUNNER_DREAM_HORIZONTAL_MOTION_SMOKE = "dream-horizontal-motion-smoke";
    private static final String RUNNER_YEARN_ECHO_SMOKE = "yearn-echo-smoke";
    private static final String RUNNER_YEARN_ROUTE_SMOKE = "yearn-route-smoke";
    private static final String RUNNER_YEARN_INSIDE_ROUTE_SMOKE = "yearn-inside-route-smoke";
    private static final int YEARN_TEST_MOUTH_X = 12;
    private static final int YEARN_TEST_END_X = 24;
    private static final int YEARN_TEST_OVERHANG_X = 4;
    private static final int YEARN_TEST_SMALL_END_X = 11;
    private static final String MUTABLE_FIXTURE_MARKER = ".bnb-mutable-fixture";
    private static final String LEGACY_ID_MIXED_SLAB = "bitsandbalance:mixed_slab";
    private static final String LEGACY_ID_VERTICAL_SLAB = "bitsandbalance:vertical_slab";
    private static final String LEGACY_ID_STEP = "bitsandbalance:step";
    private static final String LEGACY_ID_VERTICAL_STEP = "bitsandbalance:vertical_step";
    private static final String LEGACY_ID_QUAD_STEP = "bitsandbalance:quad_step";
    private static final String LEGACY_ID_QUAD_VERTICAL_STEP = "bitsandbalance:quad_vertical_step";
    private static final String TEMPORARY_CLOUD_BLOCK_ENTITY_CLASS = "org.onenonly.bitsandbalance.common.blockentity.TemporaryCloudBlockEntity";
    private static final String SOUL_FIRE_BURN_ACCESS_CLASS = "org.onenonly.bitsandbalance.common.entity.SoulFireBurnAccess";
    private static final int WAIT_TICKS_AFTER_JOIN = 40;
    private static final int MUTABLE_REGION_LENGTH = 12;
    private static final int MUTABLE_REGION_DEPTH = 2;
    private static final double SABLE_POSE_POSITION_TOLERANCE = 1.0E-4D;

    private static boolean loggedDisabled;
    private static boolean completed;
    private static int readyTicks;
    private static DreamClientMotionState dreamClientMotion;
    private static DreamHorizontalMotionState dreamHorizontalMotion;
    private static YearnEchoState yearnEcho;
    private static YearnRouteState yearnRoute;

    private StonecutterRuntimeFixtureRunner() {
    }

    public static void tick(Minecraft client) {
        if (!enabled() || completed) {
            return;
        }

        if (client == null || client.player == null || client.level == null) {
            readyTicks = 0;
            return;
        }

        int waitTicks = RUNNER_DREAM_PROJECTION_SMOKE.equals(runnerName())
                || RUNNER_DREAM_CLIENT_MOTION_SMOKE.equals(runnerName())
                || RUNNER_DREAM_HORIZONTAL_MOTION_SMOKE.equals(runnerName())
                || RUNNER_YEARN_ECHO_SMOKE.equals(runnerName())
                || RUNNER_YEARN_ROUTE_SMOKE.equals(runnerName())
                || RUNNER_YEARN_INSIDE_ROUTE_SMOKE.equals(runnerName()) ? 100 : WAIT_TICKS_AFTER_JOIN;
        if (++readyTicks < waitTicks) {
            return;
        }

        FixtureResult result;
        if ("enhanced-slab-portable-client".equals(runnerName())) {
            EnhancedSlabPortableClientFixture.Result portable = EnhancedSlabPortableClientFixture.tick(client);
            if (portable == null) return;
            result = new FixtureResult(request().target(), portable.passed(), portable.checked(), 0, portable.details());
        } else if ("enhanced-slab-vertical-destroy-particles".equals(runnerName())) {
            EnhancedSlabVerticalDestroyParticleFixture.Result particles =
                    "visual".equals(System.getProperty(SCENARIO_PROPERTY))
                            ? EnhancedSlabVerticalDestroyParticleFixture.tickVisual(client)
                            : EnhancedSlabVerticalDestroyParticleFixture.run(client);
            if (particles == null) return;
            result = new FixtureResult(request().target(), particles.passed(), particles.checked(), 0, particles.details());
        } else if ("enhanced-slab-placement-sound".equals(runnerName())) {
            EnhancedSlabPlacementSoundFixture.Result sound = EnhancedSlabPlacementSoundFixture.run(client);
            if (sound == null) return;
            result = new FixtureResult(request().target(), sound.passed(), sound.checked(), 0, sound.details());
        } else if ("enhanced-slab-legacy-sanity".equals(runnerName())) {
            EnhancedSlabLegacyRenderSanityFixture.Result live = EnhancedSlabLegacyRenderSanityFixture.tick(client);
            if (live == null) return;
            result = new FixtureResult(request().target(), live.passed(), live.checked(), 0, live.details());
        } else if ("enhanced-slab-render-live".equals(runnerName())) {
            EnhancedSlabRenderRegressionFixture.Result live = EnhancedSlabRenderRegressionFixture.tickLive(client);
            if (live == null) return;
            result = new FixtureResult(request().target(), live.passed(), live.checked(), 0, live.details());
        } else if (RUNNER_YEARN_ROUTE_SMOKE.equals(runnerName())
                || RUNNER_YEARN_INSIDE_ROUTE_SMOKE.equals(runnerName())) {
            result = tickYearnRoute(client, request());
            if (result == null) {
                return;
            }
        } else if (RUNNER_YEARN_ECHO_SMOKE.equals(runnerName())) {
            result = tickYearnEcho(client, request());
            if (result == null) {
                return;
            }
        } else if (RUNNER_DREAM_HORIZONTAL_MOTION_SMOKE.equals(runnerName())) {
            result = tickDreamHorizontalMotion(client, request());
            if (result == null) {
                return;
            }
        } else if (RUNNER_DREAM_CLIENT_MOTION_SMOKE.equals(runnerName())) {
            result = tickDreamClientMotion(client, request());
            if (result == null) {
                return;
            }
        } else {
            result = run(client);
        }
        completed = true;
        if (result.passed()) {
            LOGGER.info("BNB_FIXTURE PASS target={} checked={} skipped={} details={}",
                    result.target(), result.checked(), result.skipped(), String.join(";", result.details()));
        } else {
            LOGGER.error("BNB_FIXTURE FAIL target={} checked={} skipped={} details={}",
                    result.target(), result.checked(), result.skipped(), String.join(";", result.details()));
        }

        if (exitAfterRun()) {
            stopClient(client);
        }
    }

    private static FixtureResult run(Minecraft client) {
        FixtureRequest request = request();
        return switch (request.runner()) {
            case RUNNER_LEDGER -> runLedger(client, request);
            case RUNNER_MUTABLE -> runMutable(client, request);
            case RUNNER_LITEMATICA_SCHEMATIC_SMOKE -> runLitematicaSchematicSmoke(client, request);
            case RUNNER_CLIENT_RENDER_SMOKE -> runClientRenderSmoke(client, request);
            case "enhanced-slab-render-regression" -> runEnhancedSlabRenderRegression(client, request);
            case RUNNER_DREAM_PROJECTION_SMOKE -> runDreamProjectionSmoke(client, request);
            case RUNNER_ORE_SMELTING_XP -> runOreSmeltingXp(client, request);
            default -> new FixtureResult(request.target(), false, 0, 0, List.of("unknown-runner=" + request.runner()));
        };
    }

    private static FixtureResult runOreSmeltingXp(Minecraft client, FixtureRequest request) {
        MinecraftServer server = singleplayerServer(client);
        if (server == null) {
            return new FixtureResult(request.target(), false, 0, 0, List.of("no-integrated-server"));
        }

        List<String> failures = new ArrayList<>();
        Set<String> covered = new LinkedHashSet<>();
        List<AbstractCookingRecipe> diamondRecipes = new ArrayList<>();
        int checked = 0;
        RecipeManager recipeManager = server.getRecipeManager();

        for (var holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof AbstractCookingRecipe recipe)
                    || (!(recipe instanceof SmeltingRecipe) && !(recipe instanceof BlastingRecipe))) {
                continue;
            }
            ItemStack inputStack = bitsandbalance$singleCookingInput(recipe);
            if (inputStack.isEmpty()) {
                continue;
            }

            float expected = OreSmeltingXpRuntime.configuredExperience(inputStack.getItem());
            if (Float.isNaN(expected)) {
                continue;
            }

            float actual = bitsandbalance$cookingExperience(recipe);
            checked++;
            String key = BuiltInRegistries.ITEM.getKey(inputStack.getItem()) + "/"
                    + (recipe instanceof BlastingRecipe ? "blasting" : "smelting");
            covered.add(key);
            if (Math.abs(actual - expected) > 1.0E-5F) {
                failures.add(key + " expected=" + expected + " actual=" + actual);
            }
            if (inputStack.is(Items.DIAMOND_ORE)) {
                diamondRecipes.add(recipe);
            }
        }

        if (covered.size() != 38) {
            failures.add("supported-vanilla-recipes expected=38 actual=" + covered.size());
        }

        boolean originallyEnabled = OreSmeltingXpRuntime.enabled;
        double originalDiamond = OreSmeltingXpRuntime.diamond;
        try {
            OreSmeltingXpRuntime.enabled = true;
            OreSmeltingXpRuntime.diamond = 7.25D;
            for (AbstractCookingRecipe recipe : diamondRecipes) {
                float actual = bitsandbalance$cookingExperience(recipe);
                checked++;
                if (Math.abs(actual - 7.25F) > 1.0E-5F) {
                    failures.add("custom-diamond-xp expected=7.25 actual=" + actual);
                }
            }

            OreSmeltingXpRuntime.enabled = false;
            for (AbstractCookingRecipe recipe : diamondRecipes) {
                float actual = bitsandbalance$cookingExperience(recipe);
                checked++;
                if (Math.abs(actual - 1.0F) > 1.0E-5F) {
                    failures.add("disabled-diamond-vanilla-xp expected=1.0 actual=" + actual);
                }
            }
        } finally {
            OreSmeltingXpRuntime.enabled = originallyEnabled;
            OreSmeltingXpRuntime.diamond = originalDiamond;
        }

        if (!originallyEnabled) {
            failures.add("feature-disabled-in-default-runtime-config");
        }
        if (diamondRecipes.size() != 2) {
            failures.add("diamond-toggle-recipes expected=2 actual=" + diamondRecipes.size());
        }

        List<String> details = new ArrayList<>();
        details.add("supported-recipes=" + covered.size());
        details.add("custom-diamond=7.25");
        details.add("configured-range=coal:0.5,lapis:1.0,quartz:1.0,iron:3.5,copper:3.5,redstone:3.5,gold:5.0,netherGold:5.0,diamond:5.0,emerald:5.0,ancientDebris:10.0");
        details.addAll(failures);
        return new FixtureResult(request.target(), failures.isEmpty(), checked, 0, details);
    }

    private static ItemStack bitsandbalance$singleCookingInput(AbstractCookingRecipe recipe) {
        try {
            //? if mc == 1.21.1
            if (recipe.getIngredients().isEmpty()) return ItemStack.EMPTY;
            var items = recipe.getIngredients().getFirst().getItems();
            return items.length == 1 ? items[0] : ItemStack.EMPTY;
            //? else
            var items = recipe.input().items().toList();
            return items.size() == 1 ? new ItemStack(items.getFirst().value()) : ItemStack.EMPTY;
            //? endif
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static float bitsandbalance$cookingExperience(AbstractCookingRecipe recipe) {
        //? if mc == 1.21.1
        return recipe.getExperience();
        //? else
        return recipe.experience();
        //? endif
    }

    private static FixtureResult tickYearnEcho(Minecraft client, FixtureRequest request) {
        try {
            if (yearnEcho == null) {
                MinecraftServer server = singleplayerServer(client);
                ServerPlayer player = serverPlayer(server, client.player);
                if (server == null || player == null) {
                    return new FixtureResult(request.target(), false, 0, 0, List.of("no-integrated-player"));
                }
                int before = YearnEchoClient.pulseGeneration();
                yearnEcho = callServer(server, () -> {
                    boolean master = GoatHornPurposes.enabled;
                    boolean yearn = GoatHornPurposes.yearn;
                    boolean ponderEnabled = GoatHornPurposes.ponder;
                    boolean separateCooldowns = GoatHornPurposes.separateCooldowns;
                    boolean masterBlocked;
                    boolean yearnBlocked;
                    try {
                        GoatHornPurposes.enabled = false;
                        masterBlocked = !YearnEcho.start(player);
                        GoatHornPurposes.enabled = true;
                        GoatHornPurposes.yearn = false;
                        yearnBlocked = !YearnEcho.start(player);
                    } finally {
                        GoatHornPurposes.enabled = true;
                        GoatHornPurposes.yearn = true;
                        GoatHornPurposes.ponder = true;
                        GoatHornPurposes.separateCooldowns = true;
                    }
                    ItemStack previousHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                    ItemStack horn = new ItemStack(Items.GOAT_HORN);
                    //? if mc == 1.21.1
                    horn.set(DataComponents.INSTRUMENT, player.registryAccess().registryOrThrow(Registries.INSTRUMENT)
                            .getHolderOrThrow(Instruments.YEARN_GOAT_HORN));
                    //? else
                    //? if mc == 1.21.11
                    horn.set(DataComponents.INSTRUMENT, new InstrumentComponent(Instruments.YEARN_GOAT_HORN));
                    //? else
                    horn.set(DataComponents.INSTRUMENT, new InstrumentComponent(player.registryAccess()
                            .lookupOrThrow(Registries.INSTRUMENT).getOrThrow(Instruments.YEARN_GOAT_HORN)));
                    //? endif
                    //? endif
                    boolean recognized = "yearn".equals(GoatHornPurposes.horn(horn));
                    player.stopUsingItem();
                    player.setItemInHand(InteractionHand.MAIN_HAND, horn);
                    player.gameMode.useItem(player, player.level(), horn, InteractionHand.MAIN_HAND);
                    boolean yearnCooling = GoatHornCooldowns.remainingTicks(player, "yearn") > 0;
                    ItemStack ponder = new ItemStack(Items.GOAT_HORN);
                    //? if mc == 1.21.1
                    ponder.set(DataComponents.INSTRUMENT, player.registryAccess().registryOrThrow(Registries.INSTRUMENT)
                            .getHolderOrThrow(Instruments.PONDER_GOAT_HORN));
                    //? else
                    //? if mc == 1.21.11
                    ponder.set(DataComponents.INSTRUMENT, new InstrumentComponent(Instruments.PONDER_GOAT_HORN));
                    //? else
                    ponder.set(DataComponents.INSTRUMENT, new InstrumentComponent(player.registryAccess()
                            .lookupOrThrow(Registries.INSTRUMENT).getOrThrow(Instruments.PONDER_GOAT_HORN)));
                    //? endif
                    //? endif
                    player.stopUsingItem();
                    player.setItemInHand(InteractionHand.MAIN_HAND, ponder);
                    player.gameMode.useItem(player, player.level(), ponder, InteractionHand.MAIN_HAND);
                    boolean ponderIndependent = player.isUsingItem()
                            && GoatHornCooldowns.remainingTicks(player, "ponder") > 0
                            && GoatHornCooldowns.remainingTicks(player, "yearn") > 0;
                    player.stopUsingItem();
                    player.setItemInHand(InteractionHand.MAIN_HAND, horn);
                    player.gameMode.useItem(player, player.level(), horn, InteractionHand.MAIN_HAND);
                    boolean yearnRepeatBlocked = !player.isUsingItem()
                            && GoatHornCooldowns.remainingTicks(player, "yearn") > 0;
                    return new YearnEchoState(server, player, previousHand, before,
                            masterBlocked, yearnBlocked, recognized,
                            yearnCooling, ponderIndependent, yearnRepeatBlocked,
                            master, yearn, ponderEnabled, separateCooldowns);
                });
                return null;
            }
            YearnEchoState state = yearnEcho;
            state.ticks++;
            if (state.ticks < 35) {
                return null;
            }
            int routePoints = YearnEchoClient.routePoints();
            double routeLength = YearnEchoClient.routeLength();
            int roundedCorners = YearnEchoClient.roundedCorners();
            int particles = YearnEchoClient.emittedParticles();
            boolean entranceFound = YearnEchoClient.hasEntrance();
            boolean packetReceived = YearnEchoClient.pulseGeneration() > state.beforeGeneration;
            callServer(state.server, () -> {
                state.player.stopUsingItem();
                state.player.setItemInHand(InteractionHand.MAIN_HAND, state.previousHand);
                GoatHornPurposes.enabled = state.previousMaster;
                GoatHornPurposes.yearn = state.previousYearn;
                GoatHornPurposes.ponder = state.previousPonder;
                GoatHornPurposes.separateCooldowns = state.previousSeparateCooldowns;
                return true;
            });
            boolean trailWorking = !entranceFound || routePoints > 1 && routeLength > 0.0D && particles > 0;
            boolean passed = state.masterBlocked && state.yearnBlocked && state.recognized
                    && state.yearnCooling && state.ponderIndependent && state.yearnRepeatBlocked
                    && packetReceived && trailWorking;
            return new FixtureResult(request.target(), passed, entranceFound ? 8 : 7, entranceFound ? 0 : 1,
                    List.of("masterBlocked=" + state.masterBlocked,
                            "yearnBlocked=" + state.yearnBlocked,
                            "hornRecognized=" + state.recognized,
                            "yearnCooling=" + state.yearnCooling,
                            "ponderIndependent=" + state.ponderIndependent,
                            "yearnRepeatBlocked=" + state.yearnRepeatBlocked,
                            "packetReceived=" + packetReceived,
                            "entranceFound=" + entranceFound,
                            "routePoints=" + routePoints,
                            "routeLength=" + routeLength,
                            "roundedCorners=" + roundedCorners,
                            "largestCornerRadius=" + YearnEchoClient.largestCornerRadius(),
                            "softenedSamples=" + YearnEchoClient.softenedSamples(),
                            "largestSmoothingOffset=" + YearnEchoClient.largestSmoothingOffset(),
                            "sharpestTurnDegrees=" + YearnEchoClient.sharpestTurnDegrees(),
                            "sharpestTurnPath=" + YearnEchoClient.sharpestTurnPath(),
                            "critParticles=" + particles,
                            "trailWorking=" + trailWorking,
                            "scanMillis=" + YearnEchoClient.lastScanMillis(),
                            "searchRadius=" + YearnEchoClient.SEARCH_RADIUS));
        } catch (Exception exception) {
            return new FixtureResult(request.target(), false, 0, 0,
                    List.of("exception=" + exception.getClass().getSimpleName() + ":" + exception.getMessage()));
        }
    }

    private static final class YearnEchoState {
        final MinecraftServer server;
        final ServerPlayer player;
        final ItemStack previousHand;
        final int beforeGeneration;
        final boolean masterBlocked;
        final boolean yearnBlocked;
        final boolean recognized;
        final boolean yearnCooling;
        final boolean ponderIndependent;
        final boolean yearnRepeatBlocked;
        final boolean previousMaster;
        final boolean previousYearn;
        final boolean previousPonder;
        final boolean previousSeparateCooldowns;
        int ticks;

        YearnEchoState(MinecraftServer server, ServerPlayer player, ItemStack previousHand,
                       int beforeGeneration, boolean masterBlocked, boolean yearnBlocked, boolean recognized,
                       boolean yearnCooling, boolean ponderIndependent, boolean yearnRepeatBlocked,
                       boolean previousMaster, boolean previousYearn, boolean previousPonder,
                       boolean previousSeparateCooldowns) {
            this.server = server;
            this.player = player;
            this.previousHand = previousHand;
            this.beforeGeneration = beforeGeneration;
            this.masterBlocked = masterBlocked;
            this.yearnBlocked = yearnBlocked;
            this.recognized = recognized;
            this.yearnCooling = yearnCooling;
            this.ponderIndependent = ponderIndependent;
            this.yearnRepeatBlocked = yearnRepeatBlocked;
            this.previousMaster = previousMaster;
            this.previousYearn = previousYearn;
            this.previousPonder = previousPonder;
            this.previousSeparateCooldowns = previousSeparateCooldowns;
        }
    }

    private static FixtureResult tickYearnRoute(Minecraft client, FixtureRequest request) {
        try {
            if (yearnRoute == null) {
                MinecraftServer server = singleplayerServer(client);
                ServerPlayer player = serverPlayer(server, client.player);
                if (server == null || player == null) {
                    return new FixtureResult(request.target(), false, 0, 0, List.of("no-integrated-player"));
                }
                boolean previousForward = client.options.keyUp.isDown();
                boolean previousSprint = client.options.keySprint.isDown();
                yearnRoute = callServer(server, () -> createYearnRouteState(
                        server, player, previousForward, previousSprint,
                        RUNNER_YEARN_INSIDE_ROUTE_SMOKE.equals(request.runner())));
                if (yearnRoute == null) {
                    return new FixtureResult(request.target(), false, 0, 0, List.of("no-empty-test-area"));
                }
                return null;
            }
            YearnRouteState state = yearnRoute;
            state.ticks++;
            if (state.ticks > 240) {
                return finishYearnRoute(request, state, "timeout");
            }
            if (!state.triggered) {
                if (client.player.position().distanceToSqr(state.spawn) > 2.0D
                        || !client.level.getBlockState(state.base.offset(YEARN_TEST_MOUTH_X, 2, 0)).is(Blocks.STONE)) {
                    return null;
                }
                state.beforeGeneration = YearnEchoClient.pulseGeneration();
                state.triggered = callServer(state.server, () -> YearnEcho.start(state.player));
                state.triggerTick = state.ticks;
                return null;
            }
            int sinceTrigger = state.ticks - state.triggerTick;
            if (sinceTrigger < 35) {
                return null;
            }
            if (sinceTrigger == 35) {
                state.idleGuideDistance = YearnEchoClient.guideRouteDistance();
                state.idleGap = state.idleGuideDistance - YearnEchoClient.playerRouteDistance();
                state.idleGuideStep = YearnEchoClient.guideStep();
                client.options.keyUp.setDown(true);
                client.options.keySprint.setDown(true);
                return null;
            }
            state.maxFollowStep = Math.max(state.maxFollowStep, YearnEchoClient.guideStep());
            if (sinceTrigger == 45) {
                state.followGap = YearnEchoClient.guideRouteDistance()
                        - YearnEchoClient.playerRouteDistance();
            }
            if (sinceTrigger < 65) {
                return null;
            }
            return finishYearnRoute(request, state, "measured");
        } catch (Exception exception) {
            if (yearnRoute != null) {
                client.options.keyUp.setDown(yearnRoute.previousForward);
                client.options.keySprint.setDown(yearnRoute.previousSprint);
                try {
                    callServer(yearnRoute.server, () -> { restoreYearnRoute(yearnRoute); return true; });
                } catch (Exception ignored) {
                }
            }
            return new FixtureResult(request.target(), false, 0, 0,
                    List.of("exception=" + exception.getClass().getSimpleName() + ":" + exception.getMessage()));
        }
    }

    private static YearnRouteState createYearnRouteState(MinecraftServer server, ServerPlayer player,
                                                        boolean previousForward, boolean previousSprint,
                                                        boolean inside) {
        ServerLevel world = (ServerLevel) player.level();
        BlockPos base = emptyYearnArena(world, player.blockPosition());
        if (base == null) {
            return null;
        }
        YearnRouteState state = new YearnRouteState(server, player, base, player.position(),
                player.getYRot(), player.getXRot(), player.getItemInHand(InteractionHand.MAIN_HAND).copy(),
                previousForward, previousSprint, inside);
        try {
            for (int x = 0; x <= YEARN_TEST_END_X; x++) {
                for (int z = -6; z <= 4; z++) {
                    placeYearnStone(world, state, base.offset(x, -1, z));
                    if (x >= YEARN_TEST_OVERHANG_X) {
                        placeYearnStone(world, state, base.offset(x, 2, z));
                    }
                }
                if (x >= YEARN_TEST_OVERHANG_X && x <= YEARN_TEST_SMALL_END_X) {
                    for (int y = 0; y <= 1; y++) {
                        placeYearnStone(world, state, base.offset(x, y, -6));
                        placeYearnStone(world, state, base.offset(x, y, -2));
                    }
                }
                if (x >= YEARN_TEST_MOUTH_X) {
                    for (int y = 0; y <= 1; y++) {
                        placeYearnStone(world, state, base.offset(x, y, -4));
                        placeYearnStone(world, state, base.offset(x, y, 3));
                    }
                }
            }
            for (int z = -5; z <= -3; z++) {
                for (int y = 0; y <= 1; y++) {
                    placeYearnStone(world, state, base.offset(YEARN_TEST_SMALL_END_X, y, z));
                }
            }
            for (int z = -3; z <= 2; z++) {
                for (int y = 0; y <= 1; y++) {
                    placeYearnStone(world, state, base.offset(YEARN_TEST_END_X, y, z));
                }
            }
            player.connection.teleport(state.spawn.x, state.spawn.y, state.spawn.z,
                    inside ? 90.0F : -90.0F, 0.0F);
            return state;
        } catch (Exception exception) {
            restoreYearnRoute(state);
            throw exception;
        }
    }

    private static BlockPos emptyYearnArena(ServerLevel world, BlockPos near) {
        for (int rise = 16; rise <= 128; rise += 8) {
            BlockPos base = near.offset(0, rise, 0);
            boolean empty = true;
            for (int x = 0; x <= YEARN_TEST_END_X && empty; x++) {
                for (int z = -6; z <= 4 && empty; z++) {
                    for (int y = -1; y <= 2; y++) {
                        BlockPos pos = base.offset(x, y, z);
                        if (world.isOutsideBuildHeight(pos) || !world.getBlockState(pos).isAir()) {
                            empty = false;
                            break;
                        }
                    }
                }
            }
            if (empty) {
                return base;
            }
        }
        return null;
    }

    private static void placeYearnStone(ServerLevel world, YearnRouteState state, BlockPos pos) {
        if (state.built.add(pos)) {
            world.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
    }

    private static FixtureResult finishYearnRoute(FixtureRequest request, YearnRouteState state, String outcome)
            throws Exception {
        boolean packetReceived = YearnEchoClient.pulseGeneration() > state.beforeGeneration;
        boolean entranceFound = YearnEchoClient.hasEntrance();
        BlockPos entrancePos = YearnEchoClient.entrancePosition();
        Vec3 entranceCenter = YearnEchoClient.entranceCenter();
        boolean reachedMouth = entrancePos != null
                && entrancePos.getX() == state.base.getX() + YEARN_TEST_MOUTH_X
                && YearnEchoClient.entranceWidth() == 6
                && entranceCenter != null
                && Math.abs(entranceCenter.z - state.base.getZ()) < 0.01D
                && Math.abs(entranceCenter.y - (state.base.getY() + 1.0D)) < 0.01D;
        int routePoints = YearnEchoClient.routePoints();
        double routeLength = YearnEchoClient.routeLength();
        int particles = YearnEchoClient.emittedParticles();
        int duration = YearnEchoClient.trailDurationTicks();
        double playerDistance = YearnEchoClient.playerRouteDistance();
        double guideDistance = YearnEchoClient.guideRouteDistance();
        Minecraft client = Minecraft.getInstance();
        double clientSpawnDistance = client.player.position().distanceToSqr(state.spawn);
        boolean clientMouthRoof = client.level.getBlockState(
                state.base.offset(YEARN_TEST_MOUTH_X, 2, 0)).is(Blocks.STONE);
        double openCornerRadius = YearnEchoClient.fixtureBroadCornerRadius(client.level, state.base);
        client.options.keyUp.setDown(state.previousForward);
        client.options.keySprint.setDown(state.previousSprint);
        callServer(state.server, () -> { restoreYearnRoute(state); return true; });
        boolean passed = "measured".equals(outcome) && state.triggered && packetReceived
                && entranceFound && reachedMouth
                && routePoints >= 3
                && routeLength >= (state.inside ? 8.0D : 12.0D) && particles > 0
                && YearnEchoClient.arrivalPoofs() == 1
                && openCornerRadius >= 2.0D
                && duration >= 200 && duration <= 600
                && state.idleGap >= 5.5D && state.idleGap <= 7.5D
                && playerDistance >= 1.0D && guideDistance > state.idleGuideDistance + 1.0D
                && state.followGap >= 5.5D && state.followGap <= 8.5D
                && Math.abs(guideDistance - Math.min(routeLength, playerDistance + 7.0D)) <= 1.0D
                && state.maxFollowStep > state.idleGuideStep + 0.05D;
        return new FixtureResult(request.target(), passed, 8, 0,
                List.of("outcome=" + outcome, "inside=" + state.inside,
                        "triggered=" + state.triggered,
                        "clientSpawnDistanceSq=" + clientSpawnDistance,
                        "clientMouthRoof=" + clientMouthRoof,
                        "openCornerRadius=" + openCornerRadius,
                        "packetReceived=" + packetReceived,
                        "entranceFound=" + entranceFound, "reachedMouth=" + reachedMouth,
                        "entrancePos=" + entrancePos, "entranceCenter=" + entranceCenter,
                        "entranceWidth=" + YearnEchoClient.entranceWidth(),
                        "arrivalPoofs=" + YearnEchoClient.arrivalPoofs(),
                        "roundedCorners=" + YearnEchoClient.roundedCorners(),
                        "largestCornerRadius=" + YearnEchoClient.largestCornerRadius(),
                        "softenedSamples=" + YearnEchoClient.softenedSamples(),
                        "largestSmoothingOffset=" + YearnEchoClient.largestSmoothingOffset(),
                        "sharpestTurnDegrees=" + YearnEchoClient.sharpestTurnDegrees(),
                        "routePoints=" + routePoints,
                        "routeLength=" + routeLength, "critParticles=" + particles,
                        "durationTicks=" + duration,
                        "idleGap=" + state.idleGap, "idleGuideStep=" + state.idleGuideStep,
                        "followGap=" + state.followGap,
                        "playerDistance=" + playerDistance, "guideDistance=" + guideDistance,
                        "maxFollowStep=" + state.maxFollowStep,
                        "scanMillis=" + YearnEchoClient.lastScanMillis(),
                        "restoredBlocks=" + state.built.size()));
    }

    private static void restoreYearnRoute(YearnRouteState state) {
        state.player.stopUsingItem();
        state.player.setItemInHand(InteractionHand.MAIN_HAND, state.previousHand);
        state.player.connection.teleport(state.previousPosition.x, state.previousPosition.y,
                state.previousPosition.z, state.previousYaw, state.previousPitch);
        ServerLevel world = (ServerLevel) state.player.level();
        for (BlockPos pos : state.built) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static final class YearnRouteState {
        final MinecraftServer server;
        final ServerPlayer player;
        final BlockPos base;
        final Vec3 spawn;
        final Vec3 previousPosition;
        final float previousYaw;
        final float previousPitch;
        final ItemStack previousHand;
        final boolean previousForward;
        final boolean previousSprint;
        final boolean inside;
        final Set<BlockPos> built = new LinkedHashSet<>();
        int beforeGeneration;
        int ticks;
        int triggerTick;
        boolean triggered;
        double idleGuideDistance;
        double idleGap;
        double idleGuideStep;
        double followGap;
        double maxFollowStep;

        YearnRouteState(MinecraftServer server, ServerPlayer player, BlockPos base, Vec3 previousPosition,
                       float previousYaw, float previousPitch, ItemStack previousHand,
                       boolean previousForward, boolean previousSprint, boolean inside) {
            this.server = server;
            this.player = player;
            this.base = base;
            this.spawn = new Vec3(base.getX() + (inside ? 20.5D : 0.5D),
                    base.getY(), base.getZ() + 0.5D);
            this.previousPosition = previousPosition;
            this.previousYaw = previousYaw;
            this.previousPitch = previousPitch;
            this.previousHand = previousHand;
            this.previousForward = previousForward;
            this.previousSprint = previousSprint;
            this.inside = inside;
        }
    }

    private static FixtureResult tickDreamHorizontalMotion(Minecraft client, FixtureRequest request) {
        try {
            LocalPlayer local = client.player;
            if (dreamHorizontalMotion == null) {
                MinecraftServer server = singleplayerServer(client);
                ServerPlayer body = serverPlayer(server, local);
                if (server == null || body == null) {
                    return new FixtureResult(request.target(), false, 0, 0, List.of("no-integrated-player"));
                }
                dreamHorizontalMotion = new DreamHorizontalMotionState(server, body,
                        client.options.keyUp.isDown(), client.options.keySprint.isDown());
                return null;
            }
            DreamHorizontalMotionState state = dreamHorizontalMotion;
            state.ticks++;
            if (state.ticks > 100) {
                return finishDreamHorizontalMotion(client, request, state, "timeout");
            }
            if (state.phase == 0) {
                Vec3 position = callServer(state.server, state.body::position);
                state.baselineStart = position.add(0.0D, 30.0D, 0.0D);
                callServer(state.server, () -> {
                    state.body.connection.teleport(state.baselineStart.x, state.baselineStart.y,
                            state.baselineStart.z, state.body.getYRot(), state.body.getXRot());
                    return true;
                });
                state.phase = 1;
                return null;
            }
            if (state.phase == 1) {
                if (local.position().distanceToSqr(state.baselineStart) > 1.0D) {
                    return null;
                }
                local.setDeltaMovement(Vec3.ZERO);
                local.setOnGround(false);
                local.getAbilities().flying = false;
                local.getAbilities().mayfly = false;
                client.options.keySprint.setDown(false);
                client.options.keyUp.setDown(true);
                state.baselineStart = local.position();
                state.phase = 2;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 2 && state.ticks - state.phaseTick >= 12) {
                state.baselineDistance = horizontalDistance(state.baselineStart, local.position());
                state.baselineVelocity = horizontalSpeed(local.getDeltaMovement());
                state.baselineSprinting = local.isSprinting();
                client.options.keyUp.setDown(false);
                state.baselineRelease = local.position();
                state.phase = 3;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 3 && state.ticks - state.phaseTick >= 6) {
                state.baselineCoast = horizontalDistance(state.baselineRelease, local.position());
                double entryYaw = Math.toRadians(local.getYRot());
                local.setDeltaMovement(-Math.sin(entryYaw) * 0.18D,
                        local.getDeltaMovement().y, Math.cos(entryYaw) * 0.18D);
                state.entrySpeedBefore = horizontalSpeed(local.getDeltaMovement());
                if (!callServer(state.server, () -> {
                    Vec3 velocity = state.body.getDeltaMovement();
                    state.body.setDeltaMovement(0.0D, velocity.y, 0.0D);
                    return DreamProjection.start(state.body);
                })) {
                    return finishDreamHorizontalMotion(client, request, state, "projection-did-not-start");
                }
                state.phase = 4;
                return null;
            }
            if (state.phase == 4) {
                if (!DreamProjectionClient.isProjected(local)) {
                    return null;
                }
                state.entrySpeedAfter = horizontalSpeed(local.getDeltaMovement());
                local.setDeltaMovement(0.0D, local.getDeltaMovement().y, 0.0D);
                state.ghostStart = local.position();
                state.bodyAtGhostStart = callServer(state.server, state.body::position);
                client.options.keyUp.setDown(true);
                state.phase = 5;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 5 && state.ticks - state.phaseTick >= 12) {
                state.ghostDistance = horizontalDistance(state.ghostStart, local.position());
                state.ghostVelocity = horizontalSpeed(local.getDeltaMovement());
                state.bodyGhostDistance = horizontalDistance(state.bodyAtGhostStart,
                        callServer(state.server, state.body::position));
                state.ghostSprinting = local.isSprinting();
                client.options.keyUp.setDown(false);
                state.ghostRelease = local.position();
                state.phase = 6;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 6 && state.ticks - state.phaseTick >= 6) {
                state.ghostCoast = horizontalDistance(state.ghostRelease, local.position());
                double yaw = Math.toRadians(local.getYRot());
                local.setDeltaMovement(-Math.sin(yaw) * 0.5D,
                        local.getDeltaMovement().y, Math.cos(yaw) * 0.5D);
                client.options.keyUp.setDown(true);
                state.maxStoredSpeed = local.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.95D;
                state.phase = 7;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 7 && state.ticks - state.phaseTick >= 2) {
                state.speedAfterHighMotion = horizontalSpeed(local.getDeltaMovement());
                return finishDreamHorizontalMotion(client, request, state, "measured");
            }
            return null;
        } catch (Exception exception) {
            return new FixtureResult(request.target(), false, 0, 0,
                    List.of("exception=" + exception.getClass().getSimpleName() + ":" + exception.getMessage()));
        }
    }

    private static double horizontalDistance(Vec3 from, Vec3 to) {
        return Math.hypot(to.x - from.x, to.z - from.z);
    }

    private static double horizontalSpeed(Vec3 motion) {
        return Math.hypot(motion.x, motion.z);
    }

    private static FixtureResult finishDreamHorizontalMotion(Minecraft client, FixtureRequest request,
                                                              DreamHorizontalMotionState state, String outcome) {
        client.options.keyUp.setDown(state.previousForward);
        client.options.keySprint.setDown(state.previousSprint);
        boolean passed = "measured".equals(outcome)
                && state.entrySpeedAfter >= state.entrySpeedBefore * 0.5D
                && state.ghostDistance <= state.baselineDistance * 1.1D
                && state.ghostCoast <= state.baselineCoast * 1.1D
                && state.speedAfterHighMotion <= state.maxStoredSpeed + 0.001D
                && state.bodyGhostDistance < 0.05D;
        return new FixtureResult(request.target(), passed, 5, 0,
                List.of("outcome=" + outcome, "baselineDistance=" + state.baselineDistance,
                        "entrySpeedBefore=" + state.entrySpeedBefore,
                        "entrySpeedAfter=" + state.entrySpeedAfter,
                        "ghostDistance=" + state.ghostDistance,
                        "baselineVelocity=" + state.baselineVelocity,
                        "ghostVelocity=" + state.ghostVelocity,
                        "baselineCoast=" + state.baselineCoast,
                        "ghostCoast=" + state.ghostCoast,
                        "maxStoredSpeed=" + state.maxStoredSpeed,
                        "speedAfterHighMotion=" + state.speedAfterHighMotion,
                        "baselineSprinting=" + state.baselineSprinting,
                        "ghostSprinting=" + state.ghostSprinting,
                        "bodyGhostDistance=" + state.bodyGhostDistance));
    }

    private static final class DreamHorizontalMotionState {
        final MinecraftServer server;
        final ServerPlayer body;
        final boolean previousForward;
        final boolean previousSprint;
        int ticks;
        int phase;
        int phaseTick;
        Vec3 baselineStart;
        Vec3 baselineRelease;
        Vec3 ghostStart;
        Vec3 ghostRelease;
        Vec3 bodyAtGhostStart;
        double baselineDistance;
        double baselineVelocity;
        double baselineCoast;
        double entrySpeedBefore;
        double entrySpeedAfter;
        double ghostDistance;
        double ghostVelocity;
        double ghostCoast;
        double bodyGhostDistance;
        double maxStoredSpeed;
        double speedAfterHighMotion;
        boolean baselineSprinting;
        boolean ghostSprinting;
        DreamHorizontalMotionState(MinecraftServer server, ServerPlayer body, boolean previousForward,
                                   boolean previousSprint) {
            this.server = server;
            this.body = body;
            this.previousForward = previousForward;
            this.previousSprint = previousSprint;
        }
    }

    private static FixtureResult tickDreamClientMotion(Minecraft client, FixtureRequest request) {
        try {
            LocalPlayer local = client.player;
            if (dreamClientMotion == null) {
                MinecraftServer server = singleplayerServer(client);
                ServerPlayer body = serverPlayer(server, local);
                if (server == null || body == null) {
                    return new FixtureResult(request.target(), false, 0, 0, List.of("no-integrated-player"));
                }
                if (!callServer(server, () -> DreamProjection.start(body))) {
                    return new FixtureResult(request.target(), false, 0, 0, List.of("projection-did-not-start"));
                }
                dreamClientMotion = new DreamClientMotionState(server, body, client.options.keyJump.isDown());
                return null;
            }
            DreamClientMotionState state = dreamClientMotion;
            state.ticks++;
            if (state.ticks > 300) {
                return finishDreamClientMotion(client, request, state, "timeout");
            }
            boolean projected = DreamProjectionClient.isProjected(local);
            if (state.phase == 0) {
                if (!projected) {
                    return null;
                }
                state.jumpStartY = local.getY();
                local.setOnGround(true);
                local.setDeltaMovement(Vec3.ZERO);
                client.options.keyJump.setDown(true);
                state.phase = 1;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 1 && state.ticks - state.phaseTick >= 1) {
                state.jumpImpulse = local.getDeltaMovement().y;
                state.jumped = state.jumpImpulse > 0.25D && local.getY() > state.jumpStartY;
                state.phase = 2;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 2 && state.ticks - state.phaseTick >= 8) {
                state.floatSpeed = local.getDeltaMovement().y;
                state.floated = local.getY() > state.jumpStartY + 1.0D
                        && state.floatSpeed >= 0.10D && state.floatSpeed <= 0.13D;
                client.options.keyJump.setDown(false);
                state.ghostStart = local.position().add(0.0D, 4.0D, 0.0D);
                local.setPos(state.ghostStart);
                local.setOnGround(false);
                local.setDeltaMovement(0.0D, -0.3D, 0.0D);
                state.phase = 3;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 3 && state.ticks - state.phaseTick >= 6) {
                double fallSpeed = local.getDeltaMovement().y;
                state.fallSpeed = fallSpeed;
                state.fallDelta = local.getY() - state.ghostStart.y;
                state.wasFlyingAtGlide = local.getAbilities().flying;
                state.glide = local.getY() < state.ghostStart.y
                        && fallSpeed < 0.0D && fallSpeed >= -0.39D;
                state.ascentStartY = local.getY();
                client.options.keyJump.setDown(true);
                state.phase = 4;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 4 && state.ticks - state.phaseTick >= 8) {
                double riseSpeed = local.getDeltaMovement().y;
                state.ascent = local.getY() > state.ascentStartY + 0.2D
                        && riseSpeed >= 0.10D && riseSpeed <= 0.13D;
                client.options.keyJump.setDown(false);
                state.bodyBeforePush = callServer(state.server, state.body::position);
                callServer(state.server, () -> {
                    state.body.push(0.45D, 0.1D, 0.0D);
                    return true;
                });
                state.phase = 5;
                state.phaseTick = state.ticks;
                return null;
            }
            if (state.phase == 5 && state.ticks - state.phaseTick >= 10) {
                Vec3 bodyPosition = callServer(state.server, state.body::position);
                state.bodyMoved = bodyPosition.distanceToSqr(state.bodyBeforePush) > 0.01D;
                state.ghostUnpushed = Math.abs(local.getX() - state.ghostStart.x) < 0.35D
                        && Math.abs(local.getZ() - state.ghostStart.z) < 0.35D;
                state.phase = 6;
            }
            state.warned = state.warned || DreamProjectionClient.warningCount() >= 3;
            if (state.phase == 6 && !projected
                    && !callServer(state.server, () -> DreamProjection.isProjected(state.body))) {
                Vec3 bodyPosition = callServer(state.server, state.body::position);
                boolean clientFlying = local.getAbilities().flying;
                boolean clientMayfly = local.getAbilities().mayfly;
                state.abilitiesMatch = callServer(state.server, () ->
                        clientFlying == state.body.getAbilities().flying
                                && clientMayfly == state.body.getAbilities().mayfly);
                if (local.position().distanceToSqr(bodyPosition) < 0.5D && state.abilitiesMatch) {
                    return finishDreamClientMotion(client, request, state, "returned");
                }
            }
            return null;
        } catch (Exception exception) {
            return new FixtureResult(request.target(), false, 0, 0,
                    List.of("exception=" + exception.getClass().getSimpleName() + ":" + exception.getMessage()));
        }
    }

    private static FixtureResult finishDreamClientMotion(Minecraft client, FixtureRequest request,
                                                          DreamClientMotionState state, String outcome) {
        client.options.keyJump.setDown(state.previousJumpDown);
        boolean passed = "returned".equals(outcome) && state.jumped && state.floated
                && state.glide && state.ascent
                && state.bodyMoved && state.ghostUnpushed && state.warned && state.abilitiesMatch;
        return new FixtureResult(request.target(), passed, 8, 0,
                List.of("outcome=" + outcome, "jumped=" + state.jumped, "jumpImpulse=" + state.jumpImpulse,
                        "floated=" + state.floated, "floatSpeed=" + state.floatSpeed,
                        "glide=" + state.glide, "ascent=" + state.ascent,
                        "fallSpeed=" + state.fallSpeed, "fallDelta=" + state.fallDelta,
                        "flying=" + state.wasFlyingAtGlide,
                        "bodyMoved=" + state.bodyMoved, "ghostUnpushed=" + state.ghostUnpushed,
                        "threeWarnings=" + state.warned, "abilitiesMatch=" + state.abilitiesMatch));
    }

    private static final class DreamClientMotionState {
        final MinecraftServer server;
        final ServerPlayer body;
        final boolean previousJumpDown;
        int ticks;
        int phase;
        int phaseTick;
        Vec3 ghostStart;
        Vec3 bodyBeforePush;
        double jumpStartY;
        double jumpImpulse;
        double floatSpeed;
        double ascentStartY;
        double fallSpeed;
        double fallDelta;
        boolean wasFlyingAtGlide;
        boolean jumped;
        boolean floated;
        boolean glide;
        boolean ascent;
        boolean bodyMoved;
        boolean ghostUnpushed;
        boolean warned;
        boolean abilitiesMatch;

        DreamClientMotionState(MinecraftServer server, ServerPlayer body, boolean previousJumpDown) {
            this.server = server;
            this.body = body;
            this.previousJumpDown = previousJumpDown;
        }
    }

    private static FixtureResult runDreamProjectionSmoke(Minecraft client, FixtureRequest request) {
        MinecraftServer server = singleplayerServer(client);
        ServerPlayer player = serverPlayer(server, client.player);
        if (server == null || player == null) {
            return new FixtureResult(request.target(), false, 0, 0, List.of("no-integrated-player"));
        }
        try {
            DreamFixtureStart start = callServer(server, () -> {
                boolean master = GoatHornPurposes.enabled;
                boolean dream = GoatHornPurposes.dream;
                boolean separateCooldowns = GoatHornPurposes.separateCooldowns;
                boolean masterBlocked;
                boolean dreamBlocked;
                try {
                    GoatHornPurposes.enabled = false;
                    masterBlocked = !DreamProjection.start(player);
                    GoatHornPurposes.enabled = true;
                    GoatHornPurposes.dream = false;
                    dreamBlocked = !DreamProjection.start(player);
                } finally {
                    GoatHornPurposes.enabled = true;
                    GoatHornPurposes.dream = true;
                    GoatHornPurposes.separateCooldowns = true;
                }
                Vec3 origin = player.position();
                float health = player.getHealth();
                ItemStack previousHand = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                ItemStack horn = new ItemStack(Items.GOAT_HORN);
                //? if mc == 1.21.1
                horn.set(DataComponents.INSTRUMENT, player.registryAccess().registryOrThrow(Registries.INSTRUMENT)
                        .getHolderOrThrow(Instruments.DREAM_GOAT_HORN));
                //? else
                //? if mc == 1.21.11
                horn.set(DataComponents.INSTRUMENT, new InstrumentComponent(Instruments.DREAM_GOAT_HORN));
                //? else
                horn.set(DataComponents.INSTRUMENT, new InstrumentComponent(player.registryAccess()
                        .lookupOrThrow(Registries.INSTRUMENT).getOrThrow(Instruments.DREAM_GOAT_HORN)));
                //? endif
                //? endif
                player.setItemInHand(InteractionHand.MAIN_HAND, horn);
                float initialYaw = player.getYRot();
                float initialPitch = player.getXRot();
                float initialBodyYaw = player.yBodyRot;
                float initialHeadYaw = player.getYHeadRot();
                boolean initialNoGravity = player.isNoGravity();
                long startedAt = player.level().getGameTime();
                player.gameMode.useItem(player, player.level(), horn, InteractionHand.MAIN_HAND);
                boolean started = DreamProjection.isProjected(player);
                //? if mc == 1.21.1
                boolean hornCooldownVisible = GoatHornCooldowns.percent(player, horn, 0.0F) > 0.0F;
                //? else
                boolean hornCooldownVisible = player.getCooldowns().isOnCooldown(horn)
                        && GoatHornCooldowns.percent(player, horn, 0.0F) > 0.0F;
                //? endif
                ItemStack otherHorn = new ItemStack(Items.GOAT_HORN);
                //? if mc == 1.21.1
                otherHorn.set(DataComponents.INSTRUMENT, player.registryAccess().registryOrThrow(Registries.INSTRUMENT)
                        .getHolderOrThrow(Instruments.YEARN_GOAT_HORN));
                boolean otherHornAvailable = !player.getCooldowns().isOnCooldown(Items.GOAT_HORN)
                        && GoatHornCooldowns.remainingTicks(player, "yearn") == 0;
                //? else
                //? if mc == 1.21.11
                otherHorn.set(DataComponents.INSTRUMENT, new InstrumentComponent(Instruments.YEARN_GOAT_HORN));
                //? else
                otherHorn.set(DataComponents.INSTRUMENT, new InstrumentComponent(player.registryAccess()
                        .lookupOrThrow(Registries.INSTRUMENT).getOrThrow(Instruments.YEARN_GOAT_HORN)));
                //? endif
                boolean otherHornAvailable = !player.getCooldowns().isOnCooldown(otherHorn)
                        && GoatHornCooldowns.remainingTicks(player, "yearn") == 0;
                //? endif
                boolean lookPreserved = player.getYRot() == initialYaw && player.getXRot() == initialPitch;
                net.minecraft.world.entity.Entity body = player.level().getEntities(player,
                                new AABB(origin, origin).inflate(3.0D), DreamProjection::isBody)
                        .stream().findFirst().orElse(null);
                net.minecraft.world.entity.Entity ghost = player.level().getEntities(player,
                                new AABB(origin, origin).inflate(3.0D), DreamProjection::isGhost)
                        .stream().findFirst().orElse(null);
                boolean bodyAtOrigin = body != null && body.position().distanceToSqr(origin) < 0.01D
                        && player.position().distanceToSqr(origin) < 0.01D && ghost != null;
                boolean gravityPreserved = player.isNoGravity() == initialNoGravity;
                boolean bodyDamage = false;
                boolean damageChecked = started;
                if (damageChecked) {
                    boolean invulnerable = player.getAbilities().invulnerable;
                    try {
                        player.getAbilities().invulnerable = false;
                        if (player.getHealth() < 2.0F) {
                            player.setHealth(4.0F);
                        }
                        float beforeDamage = player.getHealth();
                        // The real player at the origin is the vulnerable body.
                        //? if mc == 1.21.1
                        player.hurt(player.damageSources().generic(), 1.0F);
                        //? else
                        player.hurtServer(player.level(), player.damageSources().generic(), 1.0F);
                        //? endif
                        bodyDamage = player.getHealth() < beforeDamage;
                    } finally {
                        player.getAbilities().invulnerable = invulnerable;
                    }
                }
                if (started) {
                    // An external shove must move the real AFK body while ghost input stays separate.
                    player.push(0.45D, 0.25D, 0.0D);
                }
                return new DreamFixtureStart(masterBlocked, dreamBlocked, started, bodyAtOrigin,
                        damageChecked, bodyDamage, lookPreserved, gravityPreserved, initialYaw, initialPitch,
                        initialBodyYaw, initialHeadYaw, startedAt, origin, health, previousHand,
                        "dream".equals(GoatHornPurposes.horn(horn)), hornCooldownVisible,
                        otherHornAvailable, master, dream, separateCooldowns);
            });
            Vec3 ghostDestination = start.origin().add(0.0D, 2.0D, 0.0D);
            //? if mc == 1.21.1
            client.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                    ghostDestination.x, ghostDestination.y, ghostDestination.z,
                    start.yaw() + 90.0F, start.pitch(), true));
            //? else
            client.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                    ghostDestination, start.yaw() + 90.0F, start.pitch(), true, false));
            //? endif
            Thread.sleep(250L);
            DreamFixtureMotion motion = callServer(server, () -> {
                net.minecraft.world.entity.Entity ghost = player.level().getEntities(player,
                                new AABB(start.origin(), start.origin()).inflate(4.0D), DreamProjection::isGhost)
                        .stream().findFirst().orElse(null);
                net.minecraft.world.entity.Entity body = player.level().getEntities(player,
                                new AABB(player.position(), player.position()).inflate(3.0D), DreamProjection::isBody)
                        .stream().findFirst().orElse(null);
                // The client keeps gliding during the network round trip, and the body marker can
                // render one tick behind the real body's smooth yaw update.
                boolean separated = ghost != null
                        && Math.abs(ghost.getX() - ghostDestination.x) < 0.1D
                        && Math.abs(ghost.getZ() - ghostDestination.z) < 0.1D
                        && ghost.getY() <= ghostDestination.y + 0.1D
                        && ghost.getY() >= ghostDestination.y - 1.0D
                        && ghost.getYRot() == start.yaw() + 90.0F
                        && player.position().distanceToSqr(ghostDestination) > 1.0D
                        && body instanceof net.minecraft.world.entity.LivingEntity living
                        && body.position().distanceToSqr(player.position()) < 0.01D
                        && Math.abs(net.minecraft.util.Mth.wrapDegrees(body.getYRot() - player.yBodyRot)) < 5.0F
                        && Math.abs(net.minecraft.util.Mth.wrapDegrees(living.yBodyRot - player.yBodyRot)) < 5.0F
                        && Math.abs(net.minecraft.util.Mth.wrapDegrees(
                                living.getYHeadRot() - player.getYHeadRot())) < 5.0F;
                return new DreamFixtureMotion(separated,
                        player.position().distanceToSqr(start.origin()) > 0.01D,
                        "body=" + player.position() + ",ghost=" + (ghost == null ? "missing" : ghost.position())
                                + ",ghostTarget=" + ghostDestination
                                + ",ghostYaw=" + (ghost == null ? "missing" : ghost.getYRot())
                                + ",targetYaw=" + (start.yaw() + 90.0F)
                                + ",bodyView=" + (body == null ? "missing" : body.position())
                                + ",bodyYaw=" + player.yBodyRot
                                + ",viewYaw=" + (body == null ? "missing" : body.getYRot()));
            });
            // Busy modded worlds can take more than ten real seconds to advance 200 game ticks.
            long deadline = System.nanoTime() + 45_000_000_000L;
            while (System.nanoTime() < deadline && callServer(server, () -> DreamProjection.isProjected(player))) {
                Thread.sleep(250L);
            }
            DreamFixtureEnd end = callServer(server, () -> {
                long elapsedTicks = player.level().getGameTime() - start.startedAt();
                boolean returned = elapsedTicks >= DreamProjection.DURATION_TICKS
                        && !DreamProjection.isProjected(player)
                        && player.position().distanceToSqr(start.origin()) > 0.01D;
                int cooldownLeft = DreamProjection.cooldownRemainingTicks(player);
                boolean cooldownBlocked = cooldownLeft > 0 && cooldownLeft < DreamProjection.COOLDOWN_TICKS
                        && !DreamProjection.start(player);
                boolean bodyGone = player.level().getEntities(player,
                        new AABB(start.origin(), start.origin()).inflate(8.0D),
                        entity -> DreamProjection.isBody(entity) || DreamProjection.isGhost(entity)).isEmpty();
                player.setHealth(start.health());
                player.setItemInHand(InteractionHand.MAIN_HAND, start.previousHand());
                GoatHornPurposes.enabled = start.previousMaster();
                GoatHornPurposes.dream = start.previousDream();
                GoatHornPurposes.separateCooldowns = start.previousSeparateCooldowns();
                return new DreamFixtureEnd(returned, bodyGone, elapsedTicks, cooldownBlocked, cooldownLeft);
            });
            boolean passed = start.masterBlocked() && start.dreamBlocked() && start.hornRecognized() && start.started()
                    && start.hornCooldownVisible() && start.otherHornAvailable()
                    && start.lookPreserved() && start.gravityPreserved()
                    && motion.separated() && motion.bodyMoved()
                    && start.bodyAtOrigin() && (!start.damageChecked() || start.bodyDamage())
                    && end.returned() && end.bodyGone() && end.cooldownBlocked();
            return new FixtureResult(request.target(), passed, 14 + (start.damageChecked() ? 1 : 0),
                    start.damageChecked() ? 0 : 1, List.of("masterBlocked=" + start.masterBlocked(),
                            "dreamBlocked=" + start.dreamBlocked(), "hornRecognized=" + start.hornRecognized(),
                            "started=" + start.started(),
                            "hornCooldownVisible=" + start.hornCooldownVisible(),
                            "otherHornAvailable=" + start.otherHornAvailable(),
                            "lookPreserved=" + start.lookPreserved(),
                            "gravityPreserved=" + start.gravityPreserved(),
                            "motionSeparated=" + motion.separated(), "bodyMoved=" + motion.bodyMoved(),
                            "motionDebug=" + motion.debug(),
                            "bodyAtOrigin=" + start.bodyAtOrigin(), "bodyDamage=" + start.bodyDamage(),
                            "returned=" + end.returned(), "bodyGone=" + end.bodyGone(),
                            "cooldownBlocked=" + end.cooldownBlocked(), "cooldownLeft=" + end.cooldownLeft(),
                            "elapsedTicks=" + end.elapsedTicks()));
        } catch (Exception exception) {
            return new FixtureResult(request.target(), false, 0, 0,
                    List.of("exception=" + exception.getClass().getSimpleName() + ":" + exception.getMessage()));
        }
    }

    private record DreamFixtureStart(boolean masterBlocked, boolean dreamBlocked, boolean started,
                                     boolean bodyAtOrigin, boolean damageChecked, boolean bodyDamage,
                                     boolean lookPreserved, boolean gravityPreserved,
                                     float yaw, float pitch, float bodyYaw, float headYaw, long startedAt,
                                     Vec3 origin, float health, ItemStack previousHand, boolean hornRecognized,
                                     boolean hornCooldownVisible, boolean otherHornAvailable,
                                     boolean previousMaster, boolean previousDream,
                                     boolean previousSeparateCooldowns) {
    }

    private record DreamFixtureEnd(boolean returned, boolean bodyGone, long elapsedTicks,
                                   boolean cooldownBlocked, int cooldownLeft) {
    }

    private record DreamFixtureMotion(boolean separated, boolean bodyMoved, String debug) {
    }

    private static FixtureResult runEnhancedSlabRenderRegression(Minecraft client, FixtureRequest request) {
        EnhancedSlabRenderRegressionFixture.Result result = EnhancedSlabRenderRegressionFixture.run(client);
        return new FixtureResult(request.target(), result.passed(), result.checked(), 0, result.details());
    }

    private static FixtureResult runClientRenderSmoke(Minecraft client, FixtureRequest request) {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;

        ClientLevel clientLevel = client.level;
        MinecraftServer server = singleplayerServer(client);
        ServerLevel serverLevel = server == null || clientLevel == null ? null : server.getLevel(clientLevel.dimension());
        if (serverLevel == null || client.player == null) {
            return new FixtureResult(request.target(), false, 0, 0, List.of("no-integrated-client-server-level"));
        }

        BlockPos playerPos = client.player.blockPosition();
        BlockPos cloudPos = findNearbyAir(serverLevel, playerPos.offset(4, 2, 0));
        BlockPos soulFirePos = playerPos;
        BlockState oldCloudServer;
        BlockState oldSoulFireServer;
        try {
            oldCloudServer = callServer(server, () -> serverLevel.getBlockState(cloudPos));
            oldSoulFireServer = callServer(server, () -> serverLevel.getBlockState(soulFirePos));
        } catch (Exception exception) {
            return new FixtureResult(request.target(), false, 0, 0,
                    List.of("server-state-read-failed=" + exception.getClass().getSimpleName() + ":" + exception.getMessage()));
        }
        BlockState oldCloudClient = clientLevel.getBlockState(cloudPos);
        BlockState oldSoulFireClient = clientLevel.getBlockState(soulFirePos);
        int oldFireTicks = client.player.getRemainingFireTicks();
        boolean oldSoulFireBurn = soulFireBurn(client.player);

        try {
            BlockState cloudState = ModBottleOfCloud.cloudBlock().defaultBlockState().setValue(CloudBlock.TEMPORARY, Boolean.TRUE);
            if (classAvailable(TEMPORARY_CLOUD_BLOCK_ENTITY_CLASS)) {
                boolean serverCloudPresent = callServer(server, () -> {
                    serverLevel.setBlock(cloudPos, cloudState, Block.UPDATE_ALL);
                    BlockEntity blockEntity = serverLevel.getBlockEntity(cloudPos);
                    return configureTemporaryCloudFade(blockEntity, serverLevel.getGameTime() - 50L, 100, 100);
                });
                clientLevel.setBlock(cloudPos, cloudState, Block.UPDATE_ALL);

                BlockEntity clientCloud = clientLevel.getBlockEntity(cloudPos);
                boolean clientCloudPresent = configureTemporaryCloudFade(clientCloud, clientLevel.getGameTime() - 50L, 100, 100);

                int cloudAlpha = temporaryCloudRenderAlpha(clientCloud, 0.0F);
                boolean cloudPassed = serverCloudPresent
                        && clientCloudPresent
                        && temporaryCloudHasActiveFade(clientCloud)
                        && cloudAlpha > 0
                        && cloudAlpha < 255
                        && cloudState.getRenderShape() == RenderShape.INVISIBLE;
                checked++;
                passed &= cloudPassed;
                details.add("temporary-cloud=" + (cloudPassed ? "ok" : "fail")
                        + ",serverBE=" + serverCloudPresent
                        + ",clientBE=" + clientCloudPresent
                        + ",alpha=" + cloudAlpha
                        + ",shape=" + cloudState.getRenderShape());
            } else {
                details.add("temporary-cloud=skipped:unavailable");
            }

            callServer(server, () -> {
                serverLevel.setBlock(soulFirePos, Blocks.SOUL_FIRE.defaultBlockState(), Block.UPDATE_ALL);
                return null;
            });
            clientLevel.setBlock(soulFirePos, Blocks.SOUL_FIRE.defaultBlockState(), Block.UPDATE_ALL);
            client.player.setRemainingFireTicks(80);
            setSoulFireBurn(client.player, true);
            SoulFireOverlayClient.tick(client, true);

            boolean soulFirePassed = client.player.isOnFire()
                    && SoulFireOverlayClient.shouldUseSoulFireFlamesNow(clientLevel, client.player);
            checked++;
            passed &= soulFirePassed;
            details.add("soul-fire-flame-state=" + (soulFirePassed ? "ok" : "fail")
                    + ",onFire=" + client.player.isOnFire());
        } catch (Throwable throwable) {
            passed = false;
            details.add("exception=" + throwable.getClass().getSimpleName() + ":" + throwable.getMessage());
        } finally {
            restoreBlock(server, serverLevel, clientLevel, cloudPos, oldCloudServer, oldCloudClient);
            restoreBlock(server, serverLevel, clientLevel, soulFirePos, oldSoulFireServer, oldSoulFireClient);
            client.player.setRemainingFireTicks(oldFireTicks);
            setSoulFireBurn(client.player, oldSoulFireBurn);
        }

        return new FixtureResult(request.target(), passed, checked, 0, details);
    }

    private static FixtureResult runLedger(Minecraft client, FixtureRequest request) {
        String target = request.target();
        List<FixturePoint> points = FixturePoint.forTarget(target);
        List<String> details = new ArrayList<>();
        int checked = 0;
        int skipped = 0;
        boolean passed = true;

        ClientLevel clientLevel = client.level;
        MinecraftServer server = singleplayerServer(client);
        ServerLevel serverLevel = server == null || clientLevel == null ? null : server.getLevel(clientLevel.dimension());
        if (serverLevel == null) {
            return new FixtureResult(target, false, 0, points.size(), List.of("no-integrated-server-level"));
        }

        if (points.isEmpty()) {
            return new FixtureResult(target, true, 0, 1, List.of("no-saved-slot-fixtures-configured"));
        }
        details.addAll(legacyMigrationDiagnostics(target));

        EnhancedSlabMaterialSavedData serverData = EnhancedSlabMaterialSavedData.peek(serverLevel);
        if (serverData == null) {
            serverData = EnhancedSlabMaterialSavedData.get(serverLevel);
        }

        for (FixturePoint point : points) {
            BlockState liveState = serverLevel.getBlockState(point.pos());
            BlockEntity liveBlockEntity = serverLevel.getBlockEntity(point.pos());
            if (liveState.isAir()) {
                passed = false;
                details.add(point.name() + ".block=air@" + point.pos().toShortString());
            } else {
                details.add(point.name() + ".block=" + blockId(liveState));
            }
            details.add(point.name() + ".blockEntity=" + blockEntityName(liveBlockEntity));

            BlockState[] serverSlots = serverData.get(point.pos().asLong());
            if (hasAnySlot(serverSlots)) {
                checked++;
                String actual = describeSlots(serverSlots);
                if (!actual.equals(point.expectedSlots())) {
                    passed = false;
                    details.add(point.name() + "=mismatch expected:" + point.expectedSlots() + " actual:" + actual);
                } else {
                    details.add(point.name() + "=" + actual);
                }
                continue;
            }

            BlockState[] clientSlots = EnhancedSlabMaterialSavedData.getClient(clientLevel, point.pos().asLong());
            if (hasAnySlot(clientSlots)) {
                checked++;
                String actual = describeSlots(clientSlots);
                if (!actual.equals(point.expectedSlots())) {
                    passed = false;
                    details.add(point.name() + "=client-mismatch expected:" + point.expectedSlots() + " actual:" + actual);
                } else {
                    details.add(point.name() + "=client:" + actual);
                }
                continue;
            }

            skipped++;
            passed = false;
            details.add(point.name() + "=missing@" + point.pos().toShortString());
        }

        return new FixtureResult(target, passed, checked, skipped, details);
    }

    private static List<String> legacyMigrationDiagnostics(String target) {
        String className = switch (target) {
            case "1.21.1-fabric" -> "org.onenonly.bitsandbalance.fabric.mechanics.FabricEnhancedSlabLegacyMigration";
            case "1.21.1-neoforge" -> "org.onenonly.bitsandbalance.mechanics.NeoForgeEnhancedSlabLegacyMigration";
            case "26.1-neoforge", "26.2-neoforge" -> "org.onenonly.bitsandbalance.mechanics.NeoForgeEnhancedSlabFixedBlockEntityMigration";
            default -> null;
        };
        if (className == null) {
            return List.of();
        }
        try {
            Class<?> migrationClass = Class.forName(className);
            Method method = migrationClass.getMethod("diagnosticSummary");
            Object summary = method.invoke(null);
            return List.of("legacyMigration=" + summary);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return List.of("legacyMigration=unavailable:" + exception.getClass().getSimpleName());
        }
    }

    private static boolean classAvailable(String className) {
        return findClass(className) != null;
    }

    private static @Nullable Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return null;
        }
    }

    private static boolean configureTemporaryCloudFade(@Nullable Object blockEntity, long now, int durationTicks, int fadeLastTicks) {
        if (!instanceOf(TEMPORARY_CLOUD_BLOCK_ENTITY_CLASS, blockEntity)) {
            return false;
        }
        try {
            Method configureFade = blockEntity.getClass().getMethod("configureFade", long.class, int.class, int.class);
            configureFade.invoke(blockEntity, now, durationTicks, fadeLastTicks);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    private static boolean temporaryCloudHasActiveFade(@Nullable Object blockEntity) {
        if (!instanceOf(TEMPORARY_CLOUD_BLOCK_ENTITY_CLASS, blockEntity)) {
            return false;
        }
        try {
            Method hasActiveFade = blockEntity.getClass().getMethod("hasActiveFade");
            return Boolean.TRUE.equals(hasActiveFade.invoke(blockEntity));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    private static int temporaryCloudRenderAlpha(@Nullable Object blockEntity, float partialTick) {
        if (!instanceOf(TEMPORARY_CLOUD_BLOCK_ENTITY_CLASS, blockEntity)) {
            return -1;
        }
        try {
            Method getRenderAlpha = blockEntity.getClass().getMethod("getRenderAlpha", float.class);
            Object result = getRenderAlpha.invoke(blockEntity, partialTick);
            return result instanceof Number number ? number.intValue() : -1;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return -1;
        }
    }

    private static boolean soulFireBurn(@Nullable Object entity) {
        if (!instanceOf(SOUL_FIRE_BURN_ACCESS_CLASS, entity)) {
            return false;
        }
        try {
            Method getter = entity.getClass().getMethod("bitsandbalance$isSoulFireBurn");
            return Boolean.TRUE.equals(getter.invoke(entity));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    private static void setSoulFireBurn(@Nullable Object entity, boolean value) {
        if (!instanceOf(SOUL_FIRE_BURN_ACCESS_CLASS, entity)) {
            return;
        }
        try {
            Method setter = entity.getClass().getMethod("bitsandbalance$setSoulFireBurn", boolean.class);
            setter.invoke(entity, value);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
    }

    private static boolean instanceOf(String className, @Nullable Object value) {
        Class<?> clazz = findClass(className);
        return clazz != null && value != null && clazz.isInstance(value);
    }

    private static FixtureResult runMutable(Minecraft client, FixtureRequest request) {
        if (!allowMutation()) {
            return new FixtureResult(request.target(), false, 0, 0, List.of("mutation-not-allowed"));
        }

        List<String> details = new ArrayList<>();
        details.add("scenario=" + request.scenario());
        details.add("phase=" + request.phase());

        BlockPos basePos = parseBasePos(request.basePos());
        if (basePos == null) {
            details.add("basePos-required-format=x,y,z");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }
        details.add("basePos=" + basePos.toShortString());

        if (!isScratchScenario(request)) {
            details.add("scenario-must-contain-scratch-mutable-or-fresh");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }

        ClientLevel clientLevel = client.level;
        MinecraftServer server = singleplayerServer(client);
        ServerLevel serverLevel = server == null || clientLevel == null ? null : server.getLevel(clientLevel.dimension());
        if (serverLevel == null) {
            details.add("no-integrated-server-level");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }
        String levelName = levelName(server);
        Path worldPath = worldPath(server);
        details.add("level=" + levelName);
        details.add("worldPath=" + worldPath);
        if (!isScratchName(levelName) && !isScratchName(worldPath.toString())) {
            details.add("loaded-world-must-contain-scratch-mutable-or-fresh");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }
        if (!Files.isRegularFile(worldPath.resolve(MUTABLE_FIXTURE_MARKER))) {
            details.add("missing-mutable-fixture-marker=" + worldPath.resolve(MUTABLE_FIXTURE_MARKER));
            return new FixtureResult(request.target(), false, 0, 0, details);
        }

        String phase = request.phase().toLowerCase(Locale.ROOT);
        MutableFixtureContext context = new MutableFixtureContext(client, serverLevel, client.player, serverPlayer(server, client.player), basePos, details);
        return switch (phase) {
            case "seed", "reload-seed" -> seedMutable(context, request);
            case "verify", "reload-verify" -> verifyMutable(context, request);
            case "legacy-migration" -> runLegacyMigrationFixture(context, request);
            case "portable-copy" -> runPortableCopyFixture(context, request);
            case "piston-live" -> runLivePistonFixture(context, request);
            case "sable-steps" -> runSableStepFixture(context, request);
            case "vertical-slab-placement" -> runVerticalSlabPlacementFixture(context, request);
            case "cascading" -> runCascadingFixture(context, request);
            case "cleanup" -> cleanupMutable(context, request);
            case "all", "default" -> runMutableAll(context, request);
            default -> new FixtureResult(request.target(), false, 0, 0, withDetail(details, "unknown-phase=" + request.phase()));
        };
    }

    private static FixtureResult runLitematicaSchematicSmoke(Minecraft client, FixtureRequest request) {
        if (!allowMutation()) {
            return new FixtureResult(request.target(), false, 0, 0, List.of("mutation-not-allowed"));
        }

        List<String> details = new ArrayList<>();
        details.add("scenario=" + request.scenario());

        BlockPos basePos = parseBasePos(request.basePos());
        if (basePos == null) {
            details.add("basePos-required-format=x,y,z");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }
        details.add("basePos=" + basePos.toShortString());

        if (!isScratchScenario(request)) {
            details.add("scenario-must-contain-scratch-mutable-or-fresh");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }

        ClientLevel clientLevel = client.level;
        MinecraftServer server = singleplayerServer(client);
        ServerLevel serverLevel = server == null || clientLevel == null ? null : server.getLevel(clientLevel.dimension());
        if (server == null || serverLevel == null) {
            details.add("no-integrated-server-level");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }

        String levelName = levelName(server);
        Path worldPath = worldPath(server);
        details.add("level=" + levelName);
        details.add("worldPath=" + worldPath);
        if (!isScratchName(levelName) && !isScratchName(worldPath.toString())) {
            details.add("loaded-world-must-contain-scratch-mutable-or-fresh");
            return new FixtureResult(request.target(), false, 0, 0, details);
        }
        if (!Files.isRegularFile(worldPath.resolve(MUTABLE_FIXTURE_MARKER))) {
            details.add("missing-mutable-fixture-marker=" + worldPath.resolve(MUTABLE_FIXTURE_MARKER));
            return new FixtureResult(request.target(), false, 0, 0, details);
        }

        MutableFixtureContext context = new MutableFixtureContext(
                client,
                serverLevel,
                client.player,
                serverPlayer(server, client.player),
                basePos,
                details
        );
        try {
            CheckResult check = callServer(server, () -> checkLitematicaSchematicRoundTrip(
                    context,
                    MutableFixtureSpec.standard(basePos)
            ));
            details.addAll(check.details());
            return new FixtureResult(request.target(), check.passed(), check.checked(), 0, details);
        } catch (Exception exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            details.add("litematica-round-trip-exception=" + cause.getClass().getSimpleName() + ":" + cause.getMessage());
            return new FixtureResult(request.target(), false, 0, 0, details);
        }
    }

    private static CheckResult checkLitematicaSchematicRoundTrip(MutableFixtureContext context,
                                                                  MutableFixtureSpec spec) {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;
        Path tempDir = null;
        Path schematicFile = null;

        cleanupRegion(context);
        try {
            seedSpec(context, spec);
            CheckResult seeded = checkSpec(context.serverLevel(), spec);
            checked += seeded.checked();
            passed &= seeded.passed();
            details.addAll(prefixDetails("seed.", seeded.details()));

            ClassLoader loader = StonecutterRuntimeFixtureRunner.class.getClassLoader();
            Class<?> schematicClass = Class.forName(
                    "fi.dy.masa.litematica.schematic.LitematicaSchematic",
                    true,
                    loader
            );
            Class<?> selectionClass = Class.forName(
                    "fi.dy.masa.litematica.selection.AreaSelection",
                    true,
                    loader
            );
            Class<?> boxClass = Class.forName(
                    "fi.dy.masa.litematica.selection.Box",
                    true,
                    loader
            );
            Class<?> saveInfoClass = Class.forName(
                    "fi.dy.masa.litematica.schematic.LitematicaSchematic$SchematicSaveInfo",
                    true,
                    loader
            );
            Class<?> immutableMapClass = Class.forName(
                    "com.google.common.collect.ImmutableMap",
                    true,
                    loader
            );
            Class<?> positionUtilsClass = Class.forName(
                    "fi.dy.masa.litematica.util.PositionUtils",
                    true,
                    loader
            );

            String regionName = "bnb_litematica_round_trip";
            Object selection = selectionClass.getConstructor().newInstance();
            selectionClass.getMethod("setName", String.class).invoke(selection, regionName);
            selectionClass.getMethod("setExplicitOrigin", BlockPos.class).invoke(selection, context.basePos());
            Object box = boxClass.getConstructor(BlockPos.class, BlockPos.class, String.class).newInstance(
                    context.basePos(),
                    context.basePos().offset(spec.points().size() - 1, 0, 0),
                    regionName
            );
            Object added = selectionClass.getMethod("addSubRegionBox", boxClass, boolean.class)
                    .invoke(selection, box, true);
            checked++;
            if (!Boolean.TRUE.equals(added)) {
                passed = false;
                details.add("selection=region-not-added");
            } else {
                details.add("selection=6-block-region");
            }

            Object saveInfo = saveInfoClass.getConstructor(boolean.class, boolean.class)
                    .newInstance(false, false);
            Object captured = schematicClass
                    .getMethod("createEmptySchematic", selectionClass, String.class)
                    .invoke(null, selection, "Bits and Balance fixture");
            checked++;
            if (captured == null) {
                passed = false;
                details.add("capture=null");
                return new CheckResult(false, checked, details);
            }

            Object subRegions = selectionClass.getMethod("getAllSubRegions").invoke(selection);
            Method getBoxesWithinChunk = positionUtilsClass.getMethod(
                    "getBoxesWithinChunk",
                    int.class,
                    int.class,
                    immutableMapClass
            );
            Method takeBlocksWithinChunk = schematicClass.getMethod(
                    "takeBlocksFromWorldWithinChunk",
                    Level.class,
                    immutableMapClass,
                    immutableMapClass,
                    saveInfoClass
            );
            BlockPos endPos = context.basePos().offset(spec.points().size() - 1, 0, 0);
            int minChunkX = Math.floorDiv(Math.min(context.basePos().getX(), endPos.getX()), 16);
            int maxChunkX = Math.floorDiv(Math.max(context.basePos().getX(), endPos.getX()), 16);
            int minChunkZ = Math.floorDiv(Math.min(context.basePos().getZ(), endPos.getZ()), 16);
            int maxChunkZ = Math.floorDiv(Math.max(context.basePos().getZ(), endPos.getZ()), 16);
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    Object volumes = getBoxesWithinChunk.invoke(null, chunkX, chunkZ, subRegions);
                    takeBlocksWithinChunk.invoke(
                            captured,
                            context.serverLevel(),
                            volumes,
                            subRegions,
                            saveInfo
                    );
                }
            }
            details.add("capture-path=TaskSaveSchematic.chunk-processing");

            CheckResult capturedCheck = checkLitematicaSchematicContents(captured, regionName, spec, "captured");
            checked += capturedCheck.checked();
            passed &= capturedCheck.passed();
            details.addAll(capturedCheck.details());

            tempDir = Files.createTempDirectory("bnb-litematica-round-trip-");
            String fileName = "bnb_litematica_round_trip.litematic";
            schematicFile = tempDir.resolve(fileName);
            Object writeResult;
            try {
                writeResult = schematicClass
                        .getMethod("writeToFile", Path.class, String.class, boolean.class)
                        .invoke(captured, tempDir, fileName, true);
            } catch (NoSuchMethodException ignored) {
                writeResult = schematicClass
                        .getMethod("writeToFile", java.io.File.class, String.class, boolean.class)
                        .invoke(captured, tempDir.toFile(), fileName, true);
            }
            checked++;
            if (!Boolean.TRUE.equals(writeResult) || !Files.isRegularFile(schematicFile) || Files.size(schematicFile) == 0L) {
                passed = false;
                details.add("write=failed");
                return new CheckResult(false, checked, details);
            }
            details.add("write=ok:" + Files.size(schematicFile) + "B");

            Object loaded;
            try {
                loaded = schematicClass
                        .getMethod("createFromFile", Path.class, String.class)
                        .invoke(null, tempDir, fileName);
            } catch (NoSuchMethodException ignored) {
                loaded = schematicClass
                        .getMethod("createFromFile", java.io.File.class, String.class)
                        .invoke(null, tempDir.toFile(), fileName);
            }
            checked++;
            if (loaded == null) {
                passed = false;
                details.add("reload=null");
                return new CheckResult(false, checked, details);
            }

            CheckResult loadedCheck = checkLitematicaSchematicContents(loaded, regionName, spec, "reloaded");
            checked += loadedCheck.checked();
            passed &= loadedCheck.passed();
            details.addAll(loadedCheck.details());

            MutableFixtureSpec pastedSpec = MutableFixtureSpec.standard(context.basePos().offset(0, 0, 1));
            Class<?> placementClass = Class.forName(
                    "fi.dy.masa.litematica.schematic.placement.SchematicPlacement",
                    true,
                    loader
            );
            Class<?> replaceBehaviorClass = Class.forName(
                    "fi.dy.masa.litematica.util.ReplaceBehavior",
                    true,
                    loader
            );
            Class<?> pasteLayerBehaviorClass = Class.forName(
                    "fi.dy.masa.litematica.util.PasteLayerBehavior",
                    true,
                    loader
            );
            Class<?> placingUtilsClass = Class.forName(
                    "fi.dy.masa.litematica.util.SchematicPlacingUtils",
                    true,
                    loader
            );
            Object placement = placementClass
                    .getMethod("createTemporary", schematicClass, BlockPos.class)
                    .invoke(null, loaded, pastedSpec.points().getFirst().pos());
            @SuppressWarnings("unchecked")
            Set<Object> touchedChunks = (Set<Object>) placementClass.getMethod("getTouchedChunks").invoke(placement);
            Object replaceAll = Enum.valueOf(replaceBehaviorClass.asSubclass(Enum.class), "ALL");
            Object pasteAll = Enum.valueOf(pasteLayerBehaviorClass.asSubclass(Enum.class), "ALL");
            Method placeWithinChunk = placingUtilsClass.getMethod(
                    "placeToWorldWithinChunk",
                    Level.class,
                    net.minecraft.world.level.ChunkPos.class,
                    placementClass,
                    replaceBehaviorClass,
                    pasteLayerBehaviorClass,
                    boolean.class
            );
            boolean pasteSucceeded = true;
            for (Object touchedChunk : touchedChunks) {
                Object result = placeWithinChunk.invoke(
                        null,
                        context.serverLevel(),
                        touchedChunk,
                        placement,
                        replaceAll,
                        pasteAll,
                        false
                );
                pasteSucceeded &= Boolean.TRUE.equals(result);
            }
            checked++;
            passed &= pasteSucceeded && !touchedChunks.isEmpty();
            details.add("paste-path=TaskPasteSchematicPerChunkDirect:" + (pasteSucceeded ? "ok" : "failed")
                    + ":chunks=" + touchedChunks.size());

            CheckResult pastedCheck = checkSpec(context.serverLevel(), pastedSpec);
            checked += pastedCheck.checked();
            passed &= pastedCheck.passed();
            details.addAll(prefixDetails("pasted.", pastedCheck.details()));

            MutableFixtureSpec previewSpec = MutableFixtureSpec.standard(context.basePos().offset(0, 0, 2));
            Class<?> schematicWorldHandlerClass = Class.forName(
                    "fi.dy.masa.litematica.world.SchematicWorldHandler",
                    true,
                    loader
            );
            Object schematicWorldObject = schematicWorldHandlerClass.getMethod("getSchematicWorld").invoke(null);
            if (!(schematicWorldObject instanceof Level schematicWorld)) {
                passed = false;
                checked++;
                details.add("preview-world=missing");
            } else {
                Object previewPlacement = placementClass
                        .getMethod("createTemporary", schematicClass, BlockPos.class)
                        .invoke(null, loaded, previewSpec.points().getFirst().pos());
                @SuppressWarnings("unchecked")
                Set<net.minecraft.world.level.ChunkPos> previewChunks =
                        (Set<net.minecraft.world.level.ChunkPos>) placementClass
                                .getMethod("getTouchedChunks")
                                .invoke(previewPlacement);
                boolean previewSucceeded = true;
                String previewPath;
                try {
                    Class<?> protoChunkClass = Class.forName(
                            "fi.dy.masa.litematica.world.ProtoChunkSchematic",
                            true,
                            loader
                    );
                    Class<?> chunkSchematicClass = Class.forName(
                            "fi.dy.masa.litematica.world.ChunkSchematic",
                            true,
                            loader
                    );
                    Class<?> worldPlacingUtilsClass = Class.forName(
                            "fi.dy.masa.litematica.util.WorldPlacingUtils",
                            true,
                            loader
                    );
                    Method placeToProtoChunk = worldPlacingUtilsClass.getMethod(
                            "placeToProtoChunk",
                            protoChunkClass,
                            net.minecraft.world.level.ChunkPos.class,
                            placementClass
                    );
                    for (net.minecraft.world.level.ChunkPos previewChunkPos : previewChunks) {
                        Object wrappedChunk = schematicWorld.getChunkAt(previewChunkPos.getWorldPosition());
                        Object protoChunk = protoChunkClass.getConstructor(chunkSchematicClass).newInstance(wrappedChunk);
                        Object result = placeToProtoChunk.invoke(
                                null,
                                protoChunk,
                                previewChunkPos,
                                previewPlacement
                        );
                        previewSucceeded &= result != null;
                    }
                    previewPath = "PlacementManagerTaskRebuild";
                } catch (ClassNotFoundException ignored) {
                    for (net.minecraft.world.level.ChunkPos previewChunkPos : previewChunks) {
                        Object result = placeWithinChunk.invoke(
                                null,
                                schematicWorld,
                                previewChunkPos,
                                previewPlacement,
                                replaceAll,
                                pasteAll,
                                false
                        );
                        previewSucceeded &= Boolean.TRUE.equals(result);
                    }
                    previewPath = "SchematicPlacingUtils";
                }
                checked++;
                passed &= previewSucceeded && !previewChunks.isEmpty();
                details.add("preview-path=" + previewPath + ":" + (previewSucceeded ? "ok" : "failed")
                        + ":chunks=" + previewChunks.size());

                CheckResult previewCheck = checkLitematicaPreviewContents(schematicWorld, previewSpec);
                checked += previewCheck.checked();
                passed &= previewCheck.passed();
                details.addAll(previewCheck.details());

                CheckResult previewModelCheck = checkLitematicaPreviewModels(schematicWorld, previewSpec);
                checked += previewModelCheck.checked();
                passed &= previewModelCheck.passed();
                details.addAll(previewModelCheck.details());
            }
        } catch (Throwable throwable) {
            LOGGER.error("Litematica schematic round-trip fixture failed", throwable);
            Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
            passed = false;
            details.add("round-trip=exception:" + cause.getClass().getSimpleName() + ":" + cause.getMessage());
        } finally {
            cleanupRegion(context);
            if (schematicFile != null) {
                try {
                    Files.deleteIfExists(schematicFile);
                } catch (Exception exception) {
                    passed = false;
                    details.add("temp-file-cleanup=failed:" + exception.getClass().getSimpleName());
                }
            }
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir);
                } catch (Exception exception) {
                    passed = false;
                    details.add("temp-dir-cleanup=failed:" + exception.getClass().getSimpleName());
                }
            }
        }
        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkLitematicaPreviewContents(Level schematicWorld,
                                                               MutableFixtureSpec spec) {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;

        for (MutableFixturePoint point : spec.points()) {
            BlockState state = schematicWorld.getBlockState(point.pos());
            String actualSlots = describeSlots(EnhancedSlabCreateCompat.getRenderSlots(schematicWorld, point.pos()));
            boolean pointPassed = LitematicaEnhancedSlabCompat.handles(state)
                    && point.expectedSlots().equals(actualSlots);
            checked++;
            passed &= pointPassed;
            details.add("preview." + point.name() + "="
                    + (pointPassed ? "ok" : "failed")
                    + ":state=" + blockId(state)
                    + ":slots=" + actualSlots);
        }
        return new CheckResult(passed, checked, details);
    }

    /**
     * Exercises the same position-aware collectParts path used by Litematica's
     * schematic renderer. The storage-only assertions above cannot detect a
     * preview that retained its slot data but rendered the container fallback
     * model instead of the stored slab materials.
     */
    private static CheckResult checkLitematicaPreviewModels(Level schematicWorld,
                                                             MutableFixtureSpec spec) {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;

        try {
            Class<?> helperClass = Class.forName(
                    "org.onenonly.bitsandbalance.fabric.client.renderer.EnhancedSlabTerrainRenderHelper"
            );
            Method rememberContext = null;
            for (Method method : helperClass.getMethods()) {
                if ("rememberCreateModelContext".equals(method.getName()) && method.getParameterCount() == 3) {
                    rememberContext = method;
                    break;
                }
            }
            if (rememberContext == null) {
                details.add("preview-models=skipped:legacy-renderer");
                return new CheckResult(true, 0, details);
            }

            Object modelManager = Minecraft.getInstance().getModelManager();
            Method getStateModelSet = modelManager.getClass().getMethod("getBlockStateModelSet");
            Object stateModelSet = getStateModelSet.invoke(modelManager);
            Method getModel = stateModelSet.getClass().getMethod("get", BlockState.class);
            Class<?> chunkCacheClass = Class.forName(
                    "fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic"
            );
            Object renderLevel = chunkCacheClass.getConstructors()[0].newInstance(
                    schematicWorld,
                    Minecraft.getInstance().level,
                    spec.points().getFirst().pos(),
                    2
            );

            for (MutableFixturePoint point : spec.points()) {
                BlockState state = schematicWorld.getBlockState(point.pos());
                rememberContext.invoke(null, renderLevel, point.pos(), state);
                Object model = getModel.invoke(stateModelSet, state);
                ArrayList<Object> parts = new ArrayList<>();
                Method collectParts = model.getClass().getMethod("collectParts", RandomSource.class, List.class);
                collectParts.invoke(model, RandomSource.create(state.getSeed(point.pos())), parts);

                Set<String> sprites = new java.util.LinkedHashSet<>();
                Set<String> atlases = new java.util.LinkedHashSet<>();
                PreviewQuadStats quadStats = collectPreviewModelSprites(parts, sprites, atlases);
                PreviewTessellationStats tessellation = tessellateLitematicaPreview(
                        helperClass,
                        renderLevel,
                        state,
                        point.pos(),
                        model
                );
                int quadCount = quadStats.quadCount();
                boolean pointPassed = quadCount > 0
                        && quadStats.invalidUvCount() == 0
                        && quadStats.invalidAtlasCount() == 0
                        && quadStats.duplicateQuadCount() == 0
                        && tessellation.rendered()
                        && tessellation.quadCount() > 0
                        && tessellation.invalidColorCount() == 0;
                Set<String> expectedRenderStates = new LinkedHashSet<>();
                for (BlockState slot : point.slots()) {
                    if (slot != null) {
                        expectedRenderStates.add(blockId(slot));
                    }
                }
                pointPassed &= tessellation.renderStates().containsAll(expectedRenderStates);
                String[] expectedSlots = point.expectedSlots().split(",");
                for (int slotIndex = 0; slotIndex < expectedSlots.length; slotIndex++) {
                    if (state.getBlock() instanceof VerticalSlabBlock
                            && slotIndex == 1
                            && state.hasProperty(VerticalSlabBlock.DOUBLE)
                            && !state.getValue(VerticalSlabBlock.DOUBLE)) {
                        continue;
                    }
                    String slot = expectedSlots[slotIndex];
                    String expectedTexture = expectedPreviewTexture(slot);
                    if (expectedTexture != null
                            && sprites.stream().noneMatch(sprite -> sprite.endsWith(expectedTexture))) {
                        pointPassed = false;
                    }
                }

                checked++;
                passed &= pointPassed;
                details.add("preview-model." + point.name() + "="
                        + (pointPassed ? "ok" : "failed")
                        + ":parts=" + parts.size()
                        + ":quads=" + quadCount
                        + ":invalid-uvs=" + quadStats.invalidUvCount()
                        + ":invalid-atlases=" + quadStats.invalidAtlasCount()
                        + ":duplicate-quads=" + quadStats.duplicateQuadCount()
                        + ":tessellated-quads=" + tessellation.quadCount()
                        + ":invalid-colors=" + tessellation.invalidColorCount()
                        + ":render-states=" + String.join(",", tessellation.renderStates())
                        + ":atlases=" + String.join(",", atlases)
                        + ":sprites=" + String.join(",", sprites));
            }
        } catch (NoSuchMethodException ignored) {
            details.add("preview-models=skipped:legacy-model-api");
        } catch (Throwable throwable) {
            Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
            checked++;
            passed = false;
            details.add("preview-models=failed:" + cause.getClass().getSimpleName() + ":" + cause.getMessage());
        }

        return new CheckResult(passed, checked, details);
    }

    /**
     * Runs Litematica's real preview tessellator. This catches compatibility
     * failures that are invisible when merely inspecting the collected model
     * parts, including flattening several material parts under the custom
     * container block state.
     */
    private static PreviewTessellationStats tessellateLitematicaPreview(Class<?> helperClass,
                                                                         Object renderLevel,
                                                                         BlockState state,
                                                                         BlockPos pos,
                                                                         Object model) throws ReflectiveOperationException {
        Method takeRenderStates = helperClass.getMethod("takeLitematicaPreviewRenderStates");
        takeRenderStates.invoke(null);

        ClassLoader loader = StonecutterRuntimeFixtureRunner.class.getClassLoader();
        Class<?> rendererClass = Class.forName(
                "fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic",
                true,
                loader
        );
        Class<?> outputClass = Class.forName(
                "fi.dy.masa.litematica.render.schematic.IBlockOutputSchematic",
                true,
                loader
        );
        Object renderer = rendererClass.getConstructor().newInstance();
        int[] quadCount = {0};
        int[] invalidColorCount = {0};
        Object output = Proxy.newProxyInstance(
                loader,
                new Class<?>[]{outputClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("put") && args != null && args.length == 5) {
                        quadCount[0]++;
                        Object quadInstance = args[4];
                        Method getColor = quadInstance.getClass().getMethod("getColor", int.class);
                        for (int vertex = 0; vertex < 4; vertex++) {
                            int color = ((Number) getColor.invoke(quadInstance, vertex)).intValue();
                            if ((color >>> 24) != 0xFF) {
                                invalidColorCount[0]++;
                            }
                        }
                        return null;
                    }
                    if (method.getName().equals("toString")) {
                        return "BitsAndBalanceLitematicaFixtureOutput";
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == (args == null || args.length == 0 ? null : args[0]);
                    }
                    return null;
                }
        );

        Method tessellate = null;
        for (Method candidate : rendererClass.getMethods()) {
            if (candidate.getName().equals("tessellateBlock") && candidate.getParameterCount() == 7) {
                tessellate = candidate;
                break;
            }
        }
        if (tessellate == null) {
            throw new NoSuchMethodException(rendererClass.getName() + ".tessellateBlock");
        }
        Object result = tessellate.invoke(
                renderer,
                renderLevel,
                state,
                pos,
                Vec3.ZERO,
                model,
                state.getSeed(pos),
                output
        );

        @SuppressWarnings("unchecked")
        List<BlockState> materialStates = (List<BlockState>) takeRenderStates.invoke(null);
        LinkedHashSet<String> renderStates = new LinkedHashSet<>();
        for (BlockState materialState : materialStates) {
            if (materialState != null) {
                renderStates.add(blockId(materialState));
            }
        }
        return new PreviewTessellationStats(
                Boolean.TRUE.equals(result),
                quadCount[0],
                invalidColorCount[0],
                renderStates
        );
    }

    private static PreviewQuadStats collectPreviewModelSprites(List<?> parts,
                                                                Set<String> sprites,
                                                                Set<String> atlases) throws ReflectiveOperationException {
        int quadCount = 0;
        int invalidUvCount = 0;
        int invalidAtlasCount = 0;
        int duplicateQuadCount = 0;
        for (Object part : parts) {
            Set<Object> seenQuads = Collections.newSetFromMap(new IdentityHashMap<>());
            Method getQuads = part.getClass().getMethod("getQuads", Direction.class);
            getQuads.setAccessible(true);
            for (Direction direction : Direction.values()) {
                PreviewQuadStats stats = collectPreviewQuadSprites(
                        getQuads.invoke(part, direction), sprites, atlases, seenQuads
                );
                quadCount += stats.quadCount();
                invalidUvCount += stats.invalidUvCount();
                invalidAtlasCount += stats.invalidAtlasCount();
                duplicateQuadCount += stats.duplicateQuadCount();
            }
            PreviewQuadStats stats = collectPreviewQuadSprites(
                    getQuads.invoke(part, new Object[]{null}), sprites, atlases, seenQuads
            );
            quadCount += stats.quadCount();
            invalidUvCount += stats.invalidUvCount();
            invalidAtlasCount += stats.invalidAtlasCount();
            duplicateQuadCount += stats.duplicateQuadCount();
        }
        return new PreviewQuadStats(quadCount, invalidUvCount, invalidAtlasCount, duplicateQuadCount);
    }

    private static PreviewQuadStats collectPreviewQuadSprites(Object quadsObject,
                                                               Set<String> sprites,
                                                               Set<String> atlases,
                                                               Set<Object> seenQuads) throws ReflectiveOperationException {
        if (!(quadsObject instanceof List<?> quads)) {
            return new PreviewQuadStats(0, 0, 0, 0);
        }
        int invalidUvCount = 0;
        int invalidAtlasCount = 0;
        int duplicateQuadCount = 0;
        for (Object quad : quads) {
            if (!seenQuads.add(quad)) {
                duplicateQuadCount++;
            }
            Method materialInfoMethod = quad.getClass().getMethod("materialInfo");
            materialInfoMethod.setAccessible(true);
            Object materialInfo = materialInfoMethod.invoke(quad);
            Method spriteMethod = materialInfo.getClass().getMethod("sprite");
            spriteMethod.setAccessible(true);
            Object sprite = spriteMethod.invoke(materialInfo);
            Method contentsMethod = sprite.getClass().getMethod("contents");
            contentsMethod.setAccessible(true);
            Object contents = contentsMethod.invoke(sprite);
            Method nameMethod = contents.getClass().getMethod("name");
            nameMethod.setAccessible(true);
            Object name = nameMethod.invoke(contents);
            sprites.add(String.valueOf(name));

            Object atlasLocation = sprite.getClass().getMethod("atlasLocation").invoke(sprite);
            String atlas = String.valueOf(atlasLocation);
            atlases.add(atlas);
            if (!"minecraft:textures/atlas/blocks.png".equals(atlas)) {
                invalidAtlasCount++;
            }

            Method packedUvMethod = quad.getClass().getMethod("packedUV", int.class);
            packedUvMethod.setAccessible(true);
            float minU = ((Number) sprite.getClass().getMethod("getU0").invoke(sprite)).floatValue();
            float maxU = ((Number) sprite.getClass().getMethod("getU1").invoke(sprite)).floatValue();
            float minV = ((Number) sprite.getClass().getMethod("getV0").invoke(sprite)).floatValue();
            float maxV = ((Number) sprite.getClass().getMethod("getV1").invoke(sprite)).floatValue();
            float epsilon = 0.00001F;
            for (int vertex = 0; vertex < 4; vertex++) {
                long packedUv = ((Number) packedUvMethod.invoke(quad, vertex)).longValue();
                float atlasU = Float.intBitsToFloat((int) (packedUv >>> 32));
                float atlasV = Float.intBitsToFloat((int) packedUv);
                if (!Float.isFinite(atlasU)
                        || !Float.isFinite(atlasV)
                        || atlasU < minU - epsilon
                        || atlasU > maxU + epsilon
                        || atlasV < minV - epsilon
                        || atlasV > maxV + epsilon) {
                    invalidUvCount++;
                }
            }
        }
        return new PreviewQuadStats(quads.size(), invalidUvCount, invalidAtlasCount, duplicateQuadCount);
    }

    private record PreviewQuadStats(int quadCount,
                                    int invalidUvCount,
                                    int invalidAtlasCount,
                                    int duplicateQuadCount) {
    }

    private record PreviewTessellationStats(boolean rendered,
                                            int quadCount,
                                            int invalidColorCount,
                                            Set<String> renderStates) {
    }

    private static String expectedPreviewTexture(String materialId) {
        return switch (materialId) {
            case "minecraft:oak_slab" -> "minecraft:block/oak_planks";
            case "minecraft:spruce_slab" -> "minecraft:block/spruce_planks";
            case "minecraft:stone_brick_slab" -> "minecraft:block/stone_bricks";
            case "minecraft:cut_copper_slab" -> "minecraft:block/cut_copper";
            case "minecraft:exposed_cut_copper_slab" -> "minecraft:block/exposed_cut_copper";
            default -> null;
        };
    }

    private static CheckResult checkLitematicaSchematicContents(Object schematic,
                                                                 String regionName,
                                                                 MutableFixtureSpec spec,
                                                                 String phase) throws ReflectiveOperationException {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;

        Object container = schematic.getClass().getMethod("getSubRegionContainer", String.class)
                .invoke(schematic, regionName);
        Object rawMap = schematic.getClass().getMethod("getBlockEntityMapForRegion", String.class)
                .invoke(schematic, regionName);
        if (container == null || !(rawMap instanceof Map<?, ?> blockEntityMap)) {
            return new CheckResult(false, 1, List.of(phase + "=missing-region-data"));
        }

        Method getState = container.getClass().getMethod("get", int.class, int.class, int.class);
        for (int i = 0; i < spec.points().size(); i++) {
            MutableFixturePoint point = spec.points().get(i);
            BlockPos relativePos = new BlockPos(i, 0, 0);
            BlockState state = (BlockState) getState.invoke(container, i, 0, 0);
            Object storedTag = blockEntityMap.get(relativePos);
            String actualSlots = describeSlots(LitematicaEnhancedSlabCompat.slotsFromTag(storedTag));
            boolean pointPassed = LitematicaEnhancedSlabCompat.handles(state)
                    && point.expectedSlots().equals(actualSlots);
            checked++;
            passed &= pointPassed;
            details.add(phase + "." + point.name() + "="
                    + (pointPassed ? "ok" : "failed")
                    + ":state=" + blockId(state)
                    + ":tag=" + (storedTag == null ? "null" : storedTag.getClass().getSimpleName())
                    + ":slots=" + actualSlots);
        }
        return new CheckResult(passed, checked, details);
    }

    private static FixtureResult runMutableAll(MutableFixtureContext context, FixtureRequest request) {
        cleanupRegion(context);
        MutableFixtureSpec spec = MutableFixtureSpec.standard(context.basePos());
        List<String> details = context.details();
        int checked = 0;
        boolean passed = true;

        seedSpec(context, spec);
        details.add("seeded=" + spec.points().size());

        CheckResult storage = checkSpec(context.serverLevel(), spec);
        checked += storage.checked();
        passed &= storage.passed();
        details.addAll(storage.details());

        CheckResult pick = checkPickBlock(context, spec);
        checked += pick.checked();
        passed &= pick.passed();
        details.addAll(pick.details());

        CheckResult particles = checkParticleMaterials(context, spec.particlePoint());
        checked += particles.checked();
        passed &= particles.passed();
        details.addAll(particles.details());

        CheckResult destroy = checkDestroyClearsStorage(context, spec.destroyPoint());
        checked += destroy.checked();
        passed &= destroy.passed();
        details.addAll(destroy.details());

        CheckResult piston = checkPistonStorageMoveSimulation(context, spec.pistonSource(), spec.pistonDest());
        checked += piston.checked();
        passed &= piston.passed();
        details.addAll(piston.details());

        CheckResult snapshot = checkSnapshot(context, spec);
        checked += snapshot.checked();
        passed &= snapshot.passed();
        details.addAll(snapshot.details());

        CheckResult collapse = checkSameMaterialCollapse(context);
        checked += collapse.checked();
        passed &= collapse.passed();
        details.addAll(collapse.details());

        CheckResult mixedMaterial = checkMixedMaterialOptimizerPreservesSavedSlots(context);
        checked += mixedMaterial.checked();
        passed &= mixedMaterial.passed();
        details.addAll(mixedMaterial.details());

        CheckResult placement = checkMixedMaterialPlacementPath(context);
        checked += placement.checked();
        passed &= placement.passed();
        details.addAll(placement.details());

        CheckResult waterPickup = checkWaterBucketPickupTransition(context);
        checked += waterPickup.checked();
        passed &= waterPickup.passed();
        details.addAll(waterPickup.details());

        CheckResult stepFluidGeometry = checkStepFluidGeometryRefresh(context);
        checked += stepFluidGeometry.checked();
        passed &= stepFluidGeometry.passed();
        details.addAll(stepFluidGeometry.details());

        CheckResult stackedStepFluidWall = checkStackedStepFluidWall(context);
        checked += stackedStepFluidWall.checked();
        passed &= stackedStepFluidWall.passed();
        details.addAll(stackedStepFluidWall.details());

        return new FixtureResult(request.target(), passed, checked, 0, details);
    }

    private static FixtureResult seedMutable(MutableFixtureContext context, FixtureRequest request) {
        cleanupRegion(context);
        MutableFixtureSpec spec = MutableFixtureSpec.standard(context.basePos());
        seedSpec(context, spec);
        CheckResult check = checkSpec(context.serverLevel(), spec);
        CheckResult save = markReloadPersistenceScheduled();
        List<String> details = context.details();
        details.add("seeded=" + spec.points().size());
        details.addAll(check.details());
        details.addAll(save.details());
        return new FixtureResult(request.target(), check.passed() && save.passed(), check.checked() + save.checked(), 0, details);
    }

    private static FixtureResult verifyMutable(MutableFixtureContext context, FixtureRequest request) {
        MutableFixtureSpec spec = MutableFixtureSpec.standard(context.basePos());
        CheckResult check = checkReloadSpec(context.serverLevel(), spec, request.target());
        List<String> details = context.details();
        details.add("verify=" + spec.points().size());
        details.addAll(check.details());
        return new FixtureResult(request.target(), check.passed(), check.checked(), 0, details);
    }

    private static FixtureResult cleanupMutable(MutableFixtureContext context, FixtureRequest request) {
        cleanupRegion(context);
        int regionSize = MUTABLE_REGION_LENGTH * MUTABLE_REGION_DEPTH;
        context.details().add("cleanup-region-size=" + regionSize);
        return new FixtureResult(request.target(), true, regionSize, 0, context.details());
    }

    private static FixtureResult runPortableCopyFixture(MutableFixtureContext context, FixtureRequest request) {
        MinecraftServer server = context.serverLevel().getServer();
        if (server == null) {
            return new FixtureResult(request.target(), false, 0, 0,
                    withDetail(context.details(), "portable-copy.server-thread=no-server"));
        }
        if (server.isSameThread()) {
            return runPortableCopyFixtureOnServer(context, request);
        }
        try {
            return callServer(server, () -> runPortableCopyFixtureOnServer(context, request));
        } catch (Exception exception) {
            return new FixtureResult(request.target(), false, 0, 0,
                    withDetail(context.details(), "portable-copy.server-thread="
                            + exception.getClass().getSimpleName()));
        }
    }

    private static FixtureResult runPortableCopyFixtureOnServer(MutableFixtureContext context, FixtureRequest request) {
        cleanupRegion(context);
        MutableFixtureSpec sourceSpec = MutableFixtureSpec.standard(context.basePos());
        seedSpec(context, sourceSpec);
        context.details().addAll(prefixDetails("portable.migration.", legacyMigrationDiagnostics(request.target())));

        int checked = 0;
        boolean passed = true;
        for (MutableFixturePoint source : sourceSpec.points()) {
            Block block = source.containerState().getBlock();
            if (block != CommonBlocks.MIXED_SLAB
                    && block != CommonBlocks.QUAD_STEP
                    && block != CommonBlocks.QUAD_VERTICAL_STEP) {
                continue;
            }
            CompoundTag sourceTag = EnhancedSlabPortableNbt.capture(context.serverLevel(), source.pos());
            String sourceSlots = describeSlots(
                    EnhancedSlabMaterialSavedData.get(context.serverLevel(), source.pos().asLong()));
            boolean sourcePassed = sourceTag != null
                    && !sourceTag.isEmpty()
                    && source.expectedSlots().equals(sourceSlots);
            checked++;
            passed &= sourcePassed;
            context.details().add("portable-source." + source.name() + "="
                    + (sourcePassed ? "ok" : "failed")
                    + ":tag=" + (sourceTag == null ? "null" : "present")
                    + ":stateHasBlockEntity=" + context.serverLevel().getBlockState(source.pos()).hasBlockEntity()
                    + ":blockClass=" + context.serverLevel().getBlockState(source.pos()).getBlock().getClass().getSimpleName()
                    + ":blockEntity=" + blockEntityName(context.serverLevel().getBlockEntity(source.pos()))
                    + ":slots=" + sourceSlots);
        }

        for (MutableFixturePoint source : sourceSpec.points()) {
            Block block = source.containerState().getBlock();
            if (block != CommonBlocks.MIXED_SLAB
                    && block != CommonBlocks.QUAD_STEP
                    && block != CommonBlocks.QUAD_VERTICAL_STEP) {
                continue;
            }

            CompoundTag portableTag = EnhancedSlabPortableNbt.capture(context.serverLevel(), source.pos());
            BlockPos destination = source.pos().offset(0, 0, 1);
            context.serverLevel().setBlock(destination, source.containerState(), Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(context.serverLevel(), destination.asLong());

            boolean applied = portableTag != null
                    && !portableTag.isEmpty()
                    && EnhancedSlabPortableNbt.apply(
                    context.serverLevel(), destination, source.containerState(), portableTag);
            String copiedSlots = describeSlots(
                    EnhancedSlabMaterialSavedData.get(context.serverLevel(), destination.asLong()));
            boolean copyPassed = applied && source.expectedSlots().equals(copiedSlots);
            checked++;
            passed &= copyPassed;
            context.details().add("portable-copy." + source.name() + "="
                    + (copyPassed ? "ok" : "failed")
                    + ":tag=" + (portableTag == null ? "null" : "present")
                    + ":slots=" + copiedSlots);

            BlockEntity destinationBlockEntity = context.serverLevel().getBlockEntity(destination);
            EnhancedSlabMaterialSavedData.remember(context.serverLevel(), destination.asLong());
            EnhancedSlabMaterialSavedData.restoreFromBlockEntity(
                    context.serverLevel(), destinationBlockEntity);
            String rebuiltSlots = describeSlots(
                    EnhancedSlabMaterialSavedData.get(context.serverLevel(), destination.asLong()));
            boolean rebuildPassed = source.expectedSlots().equals(rebuiltSlots);
            checked++;
            passed &= rebuildPassed;
            context.details().add("portable-reindex." + source.name() + "="
                    + (rebuildPassed ? "ok" : "failed")
                    + ":slots=" + rebuiltSlots);
        }

        cleanupRegion(context);
        return new FixtureResult(request.target(), passed, checked, 0, context.details());
    }

    private static FixtureResult runLegacyMigrationFixture(MutableFixtureContext context, FixtureRequest request) {
        if ("26.1-neoforge".equals(request.target()) || "26.2-neoforge".equals(request.target())) {
            return runFixedBlockEntityMigrationFixture(context, request);
        }
        if (!"1.21.1-fabric".equals(request.target()) && !"1.21.1-neoforge".equals(request.target())) {
            context.details().add("legacy-migration-target-skipped=" + request.target());
            return new FixtureResult(request.target(), true, 0, 1, context.details());
        }

        cleanupRegion(context);

        List<LegacyMigrationPoint> points = legacyMigrationPoints(context.basePos());
        for (LegacyMigrationPoint point : points) {
            context.serverLevel().setBlock(point.fixture().pos(), point.fixture().containerState(), Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(context.serverLevel(), point.fixture().pos().asLong());
            EnhancedSlabMaterialSavedData.removeClient(context.serverLevel(), point.fixture().pos().asLong());
            context.serverLevel().removeBlockEntity(point.fixture().pos());
        }

        context.details().addAll(prefixDetails("legacyMigration.before.", legacyMigrationDiagnostics(request.target())));
        boolean promoted = true;
        for (LegacyMigrationPoint point : points) {
            promoted &= promoteLegacyPendingBlockEntityTag(context, request, point);
        }
        drainLegacyMigrationQueue(context.serverLevel().getServer(), request.target(), context.details());
        context.details().addAll(prefixDetails("legacyMigration.after.", legacyMigrationDiagnostics(request.target())));

        List<MutableFixturePoint> fixtures = points.stream().map(LegacyMigrationPoint::fixture).toList();
        CheckResult storage = checkPoints(context.serverLevel(), fixtures);
        boolean blockEntitiesMatch = true;
        for (MutableFixturePoint point : fixtures) {
            BlockEntity blockEntity = context.serverLevel().getBlockEntity(point.pos());
            boolean expectsPortableBlockEntity = point.containerState().getBlock() instanceof EntityBlock;
            boolean blockEntityMatches = expectsPortableBlockEntity ? blockEntity != null : blockEntity == null;
            blockEntitiesMatch &= blockEntityMatches;
            context.details().add(point.name() + ".blockEntity=" + blockEntityName(blockEntity)
                    + ":expected=" + (expectsPortableBlockEntity ? "portable" : "none")
                    + ":" + (blockEntityMatches ? "ok" : "failed"));
        }
        context.details().addAll(storage.details());
        boolean passed = promoted && blockEntitiesMatch && storage.passed();
        return new FixtureResult(request.target(), passed, storage.checked() + points.size() + 1, 0, context.details());
    }

    private static FixtureResult runFixedBlockEntityMigrationFixture(MutableFixtureContext context, FixtureRequest request) {
        cleanupRegion(context);
        List<FixedLegacyMigrationPoint> points = fixedLegacyMigrationPoints(context.basePos());
        if (points.isEmpty()) {
            context.details().add("fixed-legacy-migration=dynamic-registry-unavailable");
            return new FixtureResult(request.target(), false, 0, 1, context.details());
        }

        for (FixedLegacyMigrationPoint point : points) {
            MutableFixturePoint fixture = point.migration().fixture();
            context.serverLevel().setBlock(fixture.pos(), fixture.containerState(), Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(context.serverLevel(), fixture.pos().asLong());
            EnhancedSlabMaterialSavedData.removeClient(context.serverLevel(), fixture.pos().asLong());
            context.serverLevel().removeBlockEntity(fixture.pos());
        }

        context.details().addAll(prefixDetails("legacyMigration.before.", legacyMigrationDiagnostics(request.target())));
        boolean promoted = true;
        for (FixedLegacyMigrationPoint point : points) {
            promoted &= promoteLegacyPendingBlockEntityTag(context, request, point.migration());
        }
        drainLegacyMigrationQueue(context.serverLevel().getServer(), request.target(), context.details());
        context.details().addAll(prefixDetails("legacyMigration.after.", legacyMigrationDiagnostics(request.target())));

        boolean passed = promoted;
        int checked = 1;
        for (FixedLegacyMigrationPoint point : points) {
            MutableFixturePoint fixture = point.migration().fixture();
            BlockState actual = context.serverLevel().getBlockState(fixture.pos());
            BlockEntity blockEntity = context.serverLevel().getBlockEntity(fixture.pos());
            BlockState[] stored = EnhancedSlabMaterialSavedData.get(context.serverLevel(), fixture.pos().asLong());
            boolean stateMatches = actual.equals(point.expectedState());
            boolean noBlockEntity = blockEntity == null;
            boolean noStoredMaterial = !hasAnySlot(stored);
            passed &= stateMatches && noBlockEntity && noStoredMaterial;
            checked += 3;
            context.details().add(fixture.name() + ".state=" + (stateMatches ? "exact" : "expected:" + point.expectedState() + " actual:" + actual));
            context.details().add(fixture.name() + ".blockEntity=" + blockEntityName(blockEntity));
            context.details().add(fixture.name() + ".savedMaterial=" + (noStoredMaterial ? "none" : describeSlots(stored)));
        }
        return new FixtureResult(request.target(), passed, checked, 0, context.details());
    }

    private static boolean promoteLegacyPendingBlockEntityTag(MutableFixtureContext context,
                                                              FixtureRequest request,
                                                              LegacyMigrationPoint point) {
        MutableFixturePoint fixture = point.fixture();
        CompoundTag tag = legacyTag(point);
        LevelChunk chunk = context.serverLevel().getChunkAt(fixture.pos());
        try {
            Map<BlockPos, CompoundTag> pendingTags = pendingBlockEntityTags(chunk);
            if (pendingTags == null) {
                context.details().add(fixture.name() + ".legacy-pending-tag=queue-unavailable:null");
            } else {
                pendingTags.put(fixture.pos(), tag.copy());
                context.details().add(fixture.name() + ".legacy-pending-tag=queued");
            }
        } catch (Throwable throwable) {
            context.details().add(fixture.name() + ".legacy-pending-tag=queue-unavailable:" + throwable.getClass().getSimpleName());
        }

        try {
            Method method = LevelChunk.class.getDeclaredMethod("promotePendingBlockEntity", BlockPos.class, CompoundTag.class);
            method.setAccessible(true);
            Object promoted = method.invoke(chunk, fixture.pos(), tag);
            BlockEntity promotedBlockEntity = promoted instanceof BlockEntity blockEntity ? blockEntity : null;
            context.details().add(fixture.name() + ".legacy-promote-return=" + blockEntityName(promotedBlockEntity));
            if (promotedBlockEntity != null) {
                migratePromotedLegacyBlockEntity(context, request, fixture, promotedBlockEntity);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.details().add(fixture.name() + ".legacy-promote-failed=" + exception.getClass().getSimpleName());
            return false;
        }
    }

    private static void migratePromotedLegacyBlockEntity(MutableFixtureContext context,
                                                         FixtureRequest request,
                                                         MutableFixturePoint fixture,
                                                         BlockEntity blockEntity) {
        String className = switch (request.target()) {
            case "1.21.1-fabric" -> "org.onenonly.bitsandbalance.fabric.mechanics.FabricEnhancedSlabLegacyMigration";
            case "1.21.1-neoforge" -> "org.onenonly.bitsandbalance.mechanics.NeoForgeEnhancedSlabLegacyMigration";
            case "26.1-neoforge", "26.2-neoforge" -> "org.onenonly.bitsandbalance.mechanics.NeoForgeEnhancedSlabFixedBlockEntityMigration";
            default -> null;
        };
        if (className == null) {
            context.details().add(fixture.name() + ".legacy-promoted-live-migration=skipped:" + request.target());
            return;
        }

        try {
            Class<?> migrationClass = Class.forName(className);
            boolean fixedMigration = "26.1-neoforge".equals(request.target()) || "26.2-neoforge".equals(request.target());
            Method method = fixedMigration
                    ? migrationClass.getMethod("migrateRejectedBlockEntity", ServerLevel.class, BlockEntity.class)
                    : migrationClass.getMethod("migrateRejectedBlockEntity", ServerLevel.class, BlockEntity.class, BlockState.class);
            Object migrated = fixedMigration
                    ? method.invoke(null, context.serverLevel(), blockEntity)
                    : method.invoke(null, context.serverLevel(), blockEntity, context.serverLevel().getBlockState(fixture.pos()));
            context.details().add(fixture.name() + ".legacy-promoted-live-migration=" + migrated);
            if (!fixedMigration && Boolean.FALSE.equals(migrated)) {
                Method liveMethod = migrationClass.getDeclaredMethod("migrateLiveBlockEntity", ServerLevel.class, BlockEntity.class);
                liveMethod.setAccessible(true);
                liveMethod.invoke(null, context.serverLevel(), blockEntity);
                context.details().add(fixture.name() + ".legacy-promoted-direct-live-migration=invoked");
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.details().add(fixture.name() + ".legacy-promoted-live-migration=failed:" + exception.getClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<BlockPos, CompoundTag> pendingBlockEntityTags(LevelChunk chunk) {
        Class<?> type = chunk.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("pendingBlockEntities");
                field.setAccessible(true);
                Object value = field.get(chunk);
                return value instanceof Map<?, ?> map ? (Map<BlockPos, CompoundTag>) map : null;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException | RuntimeException exception) {
                return null;
            }
        }
        return null;
    }

    private static void drainLegacyMigrationQueue(MinecraftServer server, String target, List<String> details) {
        String className = switch (target) {
            case "1.21.1-fabric" -> "org.onenonly.bitsandbalance.fabric.mechanics.FabricEnhancedSlabLegacyMigration";
            case "1.21.1-neoforge" -> "org.onenonly.bitsandbalance.mechanics.NeoForgeEnhancedSlabLegacyMigration";
            case "26.1-neoforge", "26.2-neoforge" -> "org.onenonly.bitsandbalance.mechanics.NeoForgeEnhancedSlabFixedBlockEntityMigration";
            default -> null;
        };
        if (className == null) {
            details.add("legacy-drain=skipped:" + target);
            return;
        }

        try {
            Class<?> migrationClass = Class.forName(className);
            Method method = migrationClass.getDeclaredMethod("drainQueue", MinecraftServer.class);
            method.setAccessible(true);
            method.invoke(null, server);
            details.add("legacy-drain=invoked");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            details.add("legacy-drain=failed:" + exception.getClass().getSimpleName());
        }
    }

    private static List<LegacyMigrationPoint> legacyMigrationPoints(BlockPos basePos) {
        BlockState oak = Blocks.OAK_SLAB.defaultBlockState();
        BlockState spruce = Blocks.SPRUCE_SLAB.defaultBlockState();
        BlockState cutCopper = Blocks.CUT_COPPER_SLAB.defaultBlockState();
        BlockState stoneBrick = Blocks.STONE_BRICK_SLAB.defaultBlockState();

        return List.of(
                legacyPoint("legacy-pending-mixed-slab", LEGACY_ID_MIXED_SLAB, basePos,
                        CommonBlocks.MIXED_SLAB.defaultBlockState(), oak, spruce),
                legacyPoint("legacy-pending-vertical-slab", LEGACY_ID_VERTICAL_SLAB, basePos.offset(1, 0, 0),
                        CommonBlocks.VERTICAL_SLAB.defaultBlockState().setValue(VerticalSlabBlock.DOUBLE, true), stoneBrick, oak),
                legacyPoint("legacy-pending-step", LEGACY_ID_STEP, basePos.offset(2, 0, 0),
                        CommonBlocks.STEP.defaultBlockState(), cutCopper),
                legacyPoint("legacy-pending-vertical-step", LEGACY_ID_VERTICAL_STEP, basePos.offset(3, 0, 0),
                        CommonBlocks.VERTICAL_STEP.defaultBlockState(), spruce),
                legacyPoint("legacy-pending-quad-step", LEGACY_ID_QUAD_STEP, basePos.offset(4, 0, 0),
                        CommonBlocks.QUAD_STEP.defaultBlockState(), spruce, oak, cutCopper, stoneBrick),
                legacyPoint("legacy-pending-quad-vertical-step", LEGACY_ID_QUAD_VERTICAL_STEP, basePos.offset(5, 0, 0),
                        CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState(), stoneBrick, stoneBrick, oak, spruce)
        );
    }

    private static List<FixedLegacyMigrationPoint> fixedLegacyMigrationPoints(BlockPos basePos) {
        Block oakStep = StepDynamicRegistry.getStepForSlab(Blocks.OAK_SLAB);
        Block spruceStep = StepDynamicRegistry.getStepForSlab(Blocks.SPRUCE_SLAB);
        Block oakVertical = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.OAK_SLAB);
        Block spruceVertical = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.SPRUCE_SLAB);
        Block oakVerticalStep = oakVertical == null ? null : VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(oakVertical);
        Block spruceVerticalStep = spruceVertical == null ? null : VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(spruceVertical);
        if (oakStep == null || spruceStep == null || oakVertical == null || spruceVertical == null
                || oakVerticalStep == null || spruceVerticalStep == null) {
            return List.of();
        }

        BlockState sourceStep = oakStep.defaultBlockState()
                .setValue(StepBlock.FACING, Direction.SOUTH)
                .setValue(StepBlock.TOP, true)
                .setValue(StepBlock.WATERLOGGED, false);
        BlockState expectedStep = spruceStep.defaultBlockState()
                .setValue(StepBlock.FACING, Direction.SOUTH)
                .setValue(StepBlock.TOP, true)
                .setValue(StepBlock.WATERLOGGED, false);
        BlockState sourceVertical = oakVertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.WEST)
                .setValue(VerticalSlabBlock.DOUBLE, false)
                .setValue(VerticalSlabBlock.WATERLOGGED, false);
        BlockState expectedVertical = spruceVertical.defaultBlockState()
                .setValue(VerticalSlabBlock.FACING, Direction.WEST)
                .setValue(VerticalSlabBlock.DOUBLE, false)
                .setValue(VerticalSlabBlock.WATERLOGGED, false);
        BlockState sourceVerticalStep = oakVerticalStep.defaultBlockState()
                .setValue(VerticalStepBlock.FACING, Direction.NORTH)
                .setValue(VerticalStepBlock.SIDE, Direction.EAST)
                .setValue(VerticalStepBlock.WATERLOGGED, false);
        BlockState expectedVerticalStep = spruceVerticalStep.defaultBlockState()
                .setValue(VerticalStepBlock.FACING, Direction.NORTH)
                .setValue(VerticalStepBlock.SIDE, Direction.EAST)
                .setValue(VerticalStepBlock.WATERLOGGED, false);
        BlockState spruceSlab = Blocks.SPRUCE_SLAB.defaultBlockState();

        return List.of(
                fixedLegacyPoint("legacy-fixed-step", LEGACY_ID_STEP, basePos, sourceStep, expectedStep, spruceSlab),
                fixedLegacyPoint("legacy-fixed-vertical-slab", LEGACY_ID_VERTICAL_SLAB, basePos.offset(1, 0, 0), sourceVertical, expectedVertical, spruceSlab),
                fixedLegacyPoint("legacy-fixed-vertical-step", LEGACY_ID_VERTICAL_STEP, basePos.offset(2, 0, 0), sourceVerticalStep, expectedVerticalStep, spruceSlab)
        );
    }

    private static FixedLegacyMigrationPoint fixedLegacyPoint(String name,
                                                               String legacyId,
                                                               BlockPos pos,
                                                               BlockState sourceState,
                                                               BlockState expectedState,
                                                               BlockState material) {
        LegacyMigrationPoint migration = legacyPoint(name, legacyId, pos, sourceState, material, material);
        return new FixedLegacyMigrationPoint(migration, expectedState);
    }

    private static LegacyMigrationPoint legacyPoint(String name,
                                                    String legacyId,
                                                    BlockPos pos,
                                                    BlockState containerState,
                                                    BlockState... slots) {
        BlockState[] normalizedSlots = normalizeLegacySlots(slots);
        return new LegacyMigrationPoint(legacyId, new MutableFixturePoint(name, pos, containerState, normalizedSlots, describeSlots(normalizedSlots)));
    }

    private static BlockState[] normalizeLegacySlots(BlockState[] slots) {
        BlockState[] normalizedSlots = new BlockState[4];
        System.arraycopy(slots, 0, normalizedSlots, 0, Math.min(slots.length, normalizedSlots.length));
        return normalizedSlots;
    }

    private static CompoundTag legacyTag(LegacyMigrationPoint point) {
        MutableFixturePoint fixture = point.fixture();
        CompoundTag tag = new CompoundTag();
        tag.putString("id", point.legacyId());
        tag.putInt("x", fixture.pos().getX());
        tag.putInt("y", fixture.pos().getY());
        tag.putInt("z", fixture.pos().getZ());
        switch (point.legacyId()) {
            case LEGACY_ID_MIXED_SLAB -> {
                putSlot(tag, "BottomSlab", fixture.slots()[0]);
                putSlot(tag, "TopSlab", fixture.slots()[1]);
            }
            case LEGACY_ID_VERTICAL_SLAB -> {
                putSlot(tag, "FacingSlab", fixture.slots()[0]);
                putSlot(tag, "OppositeSlab", fixture.slots()[1]);
            }
            case LEGACY_ID_STEP, LEGACY_ID_VERTICAL_STEP -> putSlot(tag, "Slab", fixture.slots()[0]);
            case LEGACY_ID_QUAD_STEP, LEGACY_ID_QUAD_VERTICAL_STEP -> {
                putSlot(tag, "Slot0", fixture.slots()[0]);
                putSlot(tag, "Slot1", fixture.slots()[1]);
                putSlot(tag, "Slot2", fixture.slots()[2]);
                putSlot(tag, "Slot3", fixture.slots()[3]);
            }
            default -> {
            }
        }
        return tag;
    }

    private static void putSlot(CompoundTag tag, String key, @Nullable BlockState state) {
        if (state != null) {
            tag.putString(key, String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        }
    }

    private static List<String> prefixDetails(String prefix, List<String> details) {
        List<String> prefixed = new ArrayList<>(details.size());
        for (String detail : details) {
            prefixed.add(prefix + detail);
        }
        return prefixed;
    }

    private static void seedSpec(MutableFixtureContext context, MutableFixtureSpec spec) {
        for (MutableFixturePoint point : spec.points()) {
            context.serverLevel().setBlock(point.pos(), point.containerState(), Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(context.serverLevel(), point.pos().asLong(), point.slots());
            EnhancedSlabMaterialSavedData.rememberClient(context.serverLevel(), point.pos().asLong(), point.slots());
            seedLegacyBlockEntity(context, point);
        }
    }

    private static void seedLegacyBlockEntity(MutableFixtureContext context, MutableFixturePoint point) {
        BlockEntity blockEntity = context.serverLevel().getBlockEntity(point.pos());
        if (blockEntity instanceof MixedSlabBlockEntity mixed && point.slots().length >= 2
                && point.slots()[0] != null && point.slots()[1] != null) {
            mixed.setSlabs(point.slots()[0], point.slots()[1]);
        } else if (blockEntity instanceof VerticalSlabBlockEntity vertical && point.slots().length >= 2
                && point.slots()[0] != null && point.slots()[1] != null) {
            vertical.setSlabs(point.slots()[0], point.slots()[1]);
        } else if (blockEntity instanceof QuadStepBlockEntity quad) {
            quad.setAllSlabs(point.slots());
        } else if (blockEntity instanceof QuadVerticalStepBlockEntity quadVertical) {
            quadVertical.setAllSlabs(point.slots());
        } else if (blockEntity instanceof StepBlockEntity step && point.slots().length >= 1
                && point.slots()[0] != null) {
            step.setSlabState(point.slots()[0]);
        } else if (blockEntity instanceof VerticalStepBlockEntity verticalStep && point.slots().length >= 1
                && point.slots()[0] != null) {
            verticalStep.setSlabState(point.slots()[0]);
        }
    }

    private static CheckResult checkSpec(ServerLevel serverLevel, MutableFixtureSpec spec) {
        return checkPoints(serverLevel, spec.points());
    }

    private static CheckResult checkReloadSpec(ServerLevel serverLevel, MutableFixtureSpec spec, String target) {
        List<MutableFixturePoint> points = new ArrayList<>();
        List<String> details = new ArrayList<>();
        for (MutableFixturePoint point : spec.points()) {
            if ("1.21.1-neoforge".equals(target) && "mutable-mixed-slab".equals(point.name())) {
                details.add(point.name() + "=reload-skipped:legacy-be-seed-collapses-artificial-mixed-slab");
                continue;
            }
            points.add(point);
        }

        CheckResult check = checkPoints(serverLevel, points);
        details.addAll(check.details());
        return new CheckResult(check.passed(), check.checked(), details);
    }

    private static CheckResult checkPoints(ServerLevel serverLevel, List<MutableFixturePoint> points) {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;
        for (MutableFixturePoint point : points) {
            checked++;
            BlockState actualState = serverLevel.getBlockState(point.pos());
            if (!actualState.is(point.containerState().getBlock())) {
                passed = false;
                details.add(point.name() + "=wrong-block expected:" + blockId(point.containerState()) + " actual:" + blockId(actualState));
                continue;
            }

            String actualSlots = describeSlots(EnhancedSlabMaterialSavedData.get(serverLevel, point.pos().asLong()));
            if (!actualSlots.equals(point.expectedSlots())) {
                passed = false;
                details.add(point.name() + "=slot-mismatch expected:" + point.expectedSlots() + " actual:" + actualSlots);
            } else {
                details.add(point.name() + "=" + actualSlots);
            }
        }
        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkPickBlock(MutableFixtureContext context, MutableFixtureSpec spec) {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;

        for (MutableFixturePoint point : spec.points()) {
            PickBlockHelperCompat.PickTarget pickTarget = PickBlockHelperCompat.resolve(context.serverLevel(), context.player(), point.pos());
            checked++;
            if (pickTarget == null || pickTarget.stack().isEmpty()) {
                passed = false;
                details.add(point.name() + ".pick=empty");
                continue;
            }
            ItemStack stack = pickTarget.stack();
            details.add(point.name() + ".pick=" + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }

        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkDestroyClearsStorage(MutableFixtureContext context, MutableFixturePoint point) {
        List<String> details = new ArrayList<>();
        boolean destroyed = context.serverPlayer() == null
                ? context.serverLevel().destroyBlock(point.pos(), false)
                : context.serverPlayer().gameMode.destroyBlock(point.pos());
        BlockState[] remaining = EnhancedSlabMaterialSavedData.get(context.serverLevel(), point.pos().asLong());
        boolean cleared = destroyed && !hasAnySlot(remaining);
        details.add("destroy." + point.name() + "=" + (cleared ? "cleared" : "remaining:" + describeSlots(remaining)));
        return new CheckResult(cleared, 1, details);
    }

    private static CheckResult checkPistonStorageMoveSimulation(MutableFixtureContext context,
                                                               MutableFixturePoint source,
                                                               MutableFixturePoint dest) {
        List<String> details = new ArrayList<>();
        context.serverLevel().setBlock(source.pos(), source.containerState(), Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(context.serverLevel(), source.pos().asLong(), source.slots());
        EnhancedSlabPistonData data = EnhancedSlabPistonHooks.capture(context.serverLevel(), source.pos());
        if (data == null) {
            details.add("piston-storage-capture=missing");
            return new CheckResult(false, 1, details);
        }
        EnhancedSlabPistonHooks.stage(context.serverLevel(), source.pos(), dest.pos(), data);
        context.serverLevel().setBlock(source.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        context.serverLevel().setBlock(dest.pos(), dest.containerState(), Block.UPDATE_ALL);
        EnhancedSlabPistonHooks.applyDeferred(context.serverLevel(), dest.pos(), dest.containerState(), Direction.SOUTH, null);

        boolean sourceCleared = !hasAnySlot(EnhancedSlabMaterialSavedData.get(context.serverLevel(), source.pos().asLong()));
        String actualDest = describeSlots(EnhancedSlabMaterialSavedData.get(context.serverLevel(), dest.pos().asLong()));
        boolean destMatches = actualDest.equals(source.expectedSlots());
        details.add("piston-storage-source=" + (sourceCleared ? "cleared" : "not-cleared"));
        details.add("piston-storage-dest=" + actualDest);
        return new CheckResult(sourceCleared && destMatches, 2, details);
    }

    private static FixtureResult runLivePistonFixture(MutableFixtureContext context, FixtureRequest request) {
        CheckResult result = runCheckOnServerThread(
                context,
                "piston-live",
                () -> checkLivePistonMove(context)
        );
        context.details().addAll(result.details());
        return new FixtureResult(request.target(), result.passed(), result.checked(), 0, context.details());
    }

    private static CheckResult checkLivePistonMove(MutableFixtureContext context) {
        ServerLevel level = context.serverLevel();
        BlockPos powerPos = context.basePos();
        BlockPos pistonPos = context.basePos().relative(Direction.EAST);
        BlockPos sourcePos = pistonPos.relative(Direction.EAST);
        BlockPos destPos = sourcePos.relative(Direction.EAST);
        BlockState pistonState = Blocks.PISTON.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST)
                .setValue(BlockStateProperties.EXTENDED, false);
        BlockState movedState = CommonBlocks.MIXED_SLAB.defaultBlockState();
        BlockState bottom = Blocks.OAK_SLAB.defaultBlockState();
        BlockState top = Blocks.SPRUCE_SLAB.defaultBlockState();
        String expectedMaterials = describeSlots(new BlockState[]{bottom, top, null, null});
        List<String> details = new ArrayList<>();

        cleanupRegion(context);
        try {
            level.setBlock(pistonPos, pistonState, Block.UPDATE_CLIENTS);
            level.setBlock(sourcePos, movedState, Block.UPDATE_CLIENTS);
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong(), bottom, top);
            // Power without notifying neighbors so this explicit block event is the only activation.
            level.setBlock(powerPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);

            boolean triggered = level.getBlockState(pistonPos).triggerEvent(
                    level,
                    pistonPos,
                    0,
                    Direction.EAST.get3DDataValue()
            );
            BlockEntity movingEntity = level.getBlockEntity(destPos);
            boolean createdMovingEntity = movingEntity instanceof PistonMovingBlockEntity;
            if (movingEntity instanceof PistonMovingBlockEntity piston) {
                piston.finalTick();
            }

            BlockState finalDestState = level.getBlockState(destPos);
            String actualMaterials = describeSlots(EnhancedSlabMaterialSavedData.get(level, destPos.asLong()));
            boolean destStateMoved = finalDestState.getBlock() == CommonBlocks.MIXED_SLAB;
            boolean destMaterialsMoved = expectedMaterials.equals(actualMaterials);
            boolean sourceStateMoved = level.getBlockState(sourcePos).getBlock() != CommonBlocks.MIXED_SLAB;
            boolean sourceMaterialsCleared = !hasAnySlot(
                    EnhancedSlabMaterialSavedData.get(level, sourcePos.asLong())
            );

            details.add("piston-live.triggered=" + triggered);
            details.add("piston-live.moving-entity=" + createdMovingEntity);
            details.add("piston-live.dest-state=" + blockId(finalDestState));
            details.add("piston-live.dest-materials=" + actualMaterials);
            details.add("piston-live.source-state=" + blockId(level.getBlockState(sourcePos)));
            details.add("piston-live.source-materials=" + (sourceMaterialsCleared ? "cleared" : "remaining"));
            return new CheckResult(
                    triggered
                            && createdMovingEntity
                            && destStateMoved
                            && destMaterialsMoved
                            && sourceStateMoved
                            && sourceMaterialsCleared,
                    6,
                    details
            );
        } finally {
            cleanupRegion(context);
        }
    }

    private static CheckResult checkParticleMaterials(MutableFixtureContext context, MutableFixturePoint point) {
        if (context.player() == null) {
            return new CheckResult(false, 1, List.of("particle-state." + point.name() + "=no-player"));
        }
        BlockState resolved = EnhancedSlabLandingParticleHelper.resolveLandingParticleState(
                context.player(),
                context.serverLevel().getBlockState(point.pos()),
                point.pos()
        );
        int materialId = resolved == null ? 0 : Block.getId(resolved);
        List<String> details = List.of("particle-state." + point.name() + "=" + (resolved == null ? "null" : blockId(resolved)));
        return new CheckResult(materialId != 0 && resolved != context.serverLevel().getBlockState(point.pos()), 1, details);
    }

    private static CheckResult checkSnapshot(MutableFixtureContext context, MutableFixtureSpec spec) {
        String dimensionId = EnhancedSlabMaterialSavedData.dimensionId(context.serverLevel());
        List<EnhancedSlabMaterialSavedData.MaterialUpdate> snapshot =
                EnhancedSlabMaterialSavedData.get(context.serverLevel()).snapshotAroundChunk(
                        dimensionId,
                        context.basePos().getX() >> 4,
                        context.basePos().getZ() >> 4,
                        1
                );
        boolean found = false;
        for (EnhancedSlabMaterialSavedData.MaterialUpdate update : snapshot) {
            if (update.pos() == spec.snapshotPoint().pos().asLong()) {
                found = describeSlots(update.slabs()).equals(spec.snapshotPoint().expectedSlots());
                break;
            }
        }
        return new CheckResult(found, 1, List.of("snapshot." + spec.snapshotPoint().name() + "=" + (found ? "present" : "missing")));
    }

    private static CheckResult checkSameMaterialCollapse(MutableFixtureContext context) {
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;

        BlockState oak = Blocks.OAK_SLAB.defaultBlockState();
        Block verticalOakSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.OAK_SLAB);
        Block oakStep = StepDynamicRegistry.getStepForSlab(Blocks.OAK_SLAB);

        CollapseCase mixed = new CollapseCase(
                "same-material-mixed-slab",
                context.basePos().offset(6, 0, 0),
                CommonBlocks.MIXED_SLAB.defaultBlockState(),
                new BlockState[]{oak, oak, null, null},
                Blocks.OAK_SLAB
        );
        CollapseCase verticalSlab = new CollapseCase(
                "same-material-vertical-slab",
                context.basePos().offset(7, 0, 0),
                CommonBlocks.VERTICAL_SLAB.defaultBlockState(),
                new BlockState[]{oak, null, null, null},
                verticalOakSlab
        );
        CollapseCase step = new CollapseCase(
                "same-material-step",
                context.basePos().offset(8, 0, 0),
                CommonBlocks.QUAD_STEP.defaultBlockState(),
                new BlockState[]{oak, null, null, null},
                oakStep
        );

        for (CollapseCase collapse : List.of(mixed, verticalSlab, step)) {
            checked++;
            if (collapse.expectedBlock() == null) {
                passed = false;
                details.add(collapse.name() + "=missing-fixed-block");
                continue;
            }
            context.serverLevel().setBlock(collapse.pos(), collapse.containerState(), Block.UPDATE_ALL);
            EnhancedSlabMaterialSavedData.remember(context.serverLevel(), collapse.pos().asLong(), collapse.slots());
            boolean optimized = EnhancedSlabStorageOptimizer.optimizeAt(context.serverLevel(), collapse.pos());
            BlockState actualState = context.serverLevel().getBlockState(collapse.pos());
            boolean storageCleared = !hasAnySlot(EnhancedSlabMaterialSavedData.get(context.serverLevel(), collapse.pos().asLong()));
            boolean actualMatches = actualState.is(collapse.expectedBlock());
            if (!optimized || !actualMatches || !storageCleared) {
                passed = false;
                details.add(collapse.name() + "=fail optimized:" + optimized
                        + " actual:" + blockId(actualState)
                        + " expected:" + BuiltInRegistries.BLOCK.getKey(collapse.expectedBlock())
                        + " storageCleared:" + storageCleared);
            } else {
                details.add(collapse.name() + "=" + blockId(actualState));
            }
        }

        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkMixedMaterialOptimizerPreservesSavedSlots(MutableFixtureContext context) {
        BlockPos pos = context.basePos().offset(9, 0, 0);
        BlockState oak = Blocks.OAK_SLAB.defaultBlockState();
        BlockState spruce = Blocks.SPRUCE_SLAB.defaultBlockState();
        BlockState[] slots = new BlockState[]{oak, spruce, null, null};

        context.serverLevel().setBlock(pos, CommonBlocks.MIXED_SLAB.defaultBlockState(), Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(context.serverLevel(), pos.asLong(), slots);

        boolean optimized = EnhancedSlabStorageOptimizer.optimizeAt(context.serverLevel(), pos);
        BlockState actualState = context.serverLevel().getBlockState(pos);
        String actualSlots = describeSlots(EnhancedSlabMaterialSavedData.get(context.serverLevel(), pos.asLong()));
        String expectedSlots = describeSlots(slots);
        boolean preserved = !optimized && actualState.is(CommonBlocks.MIXED_SLAB) && actualSlots.equals(expectedSlots);

        String detail = "mixed-material-mixed-slab=" + (preserved ? actualSlots : "fail optimized:" + optimized
                + " actual:" + blockId(actualState)
                + " expectedBlock:" + BuiltInRegistries.BLOCK.getKey(CommonBlocks.MIXED_SLAB)
                + " slots:" + actualSlots
                + " expectedSlots:" + expectedSlots);
        return new CheckResult(preserved, 1, List.of(detail));
    }

    private static CheckResult checkMixedMaterialPlacementPath(MutableFixtureContext context) {
        ServerPlayer player = context.serverPlayer();
        if (player == null) {
            return new CheckResult(false, 1, List.of("mixed-material-placement=no-server-player"));
        }

        BlockPos pos = context.basePos().offset(10, 0, 0);
        BlockState oakBottom = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        context.serverLevel().setBlock(pos, oakBottom, Block.UPDATE_ALL);
        EnhancedSlabMaterialSavedData.remember(context.serverLevel(), pos.asLong());

        ItemStack heldBefore = player.getItemInHand(InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.SPRUCE_SLAB));
        InteractionResult result;
        try {
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0),
                    Direction.UP,
                    pos,
                    false
            );
            result = Blocks.SPRUCE_SLAB.asItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, heldBefore);
        }

        BlockState actualState = context.serverLevel().getBlockState(pos);
        String actualSlots = describeSlots(EnhancedSlabMaterialSavedData.get(context.serverLevel(), pos.asLong()));
        String expectedSlots = describeSlots(new BlockState[]{
                oakBottom,
                Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP),
                null,
                null
        });
        boolean passed = result.consumesAction() && actualState.is(CommonBlocks.MIXED_SLAB) && actualSlots.equals(expectedSlots);
        String detail = "mixed-material-placement=" + (passed ? actualSlots : "fail result:" + result
                + " actual:" + blockId(actualState)
                + " slots:" + actualSlots
                + " expectedSlots:" + expectedSlots);
        return new CheckResult(passed, 1, List.of(detail));
    }

    private static FixtureResult runCascadingFixture(MutableFixtureContext context, FixtureRequest request) {
        CheckResult check = runCheckOnServerThread(context, "cascading", () -> {
            CascadingMiningFixture.Result result = CascadingMiningFixture.check(
                    context.serverLevel(), context.serverPlayer(), context.basePos().above(4));
            return new CheckResult(result.passed(), result.checked(), result.details());
        });
        List<String> details = context.details();
        details.addAll(check.details());
        return new FixtureResult(request.target(), check.passed(), check.checked(), 0, details);
    }

    private static FixtureResult runVerticalSlabPlacementFixture(MutableFixtureContext context, FixtureRequest request) {
        CheckResult check = runCheckOnServerThread(context, "vertical-slab-placement", () -> checkVerticalSlabPlacement(context));
        List<String> details = context.details();
        details.addAll(check.details());
        return new FixtureResult(request.target(), check.passed(), check.checked(), 0, details);
    }

    private static CheckResult checkVerticalSlabPlacement(MutableFixtureContext context) {
        ServerPlayer player = context.serverPlayer();
        Block oakVertical = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.OAK_SLAB);
        Block spruceVertical = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.SPRUCE_SLAB);
        if (player == null || oakVertical == null || spruceVertical == null) {
            return new CheckResult(false, 1, List.of("vertical-slab-placement=missing-player-or-vertical-slab"));
        }

        ServerLevel level = context.serverLevel();
        BlockPos support = context.basePos();
        BlockPos target = support.above();
        BlockPos stacked = target.above();
        ItemStack heldBefore = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean wasShiftDown = player.isShiftKeyDown();
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;
        Direction[] existingFacings = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        double[][] groundHits = {{0.10D, 0.75D}, {0.90D, 0.25D}, {0.75D, 0.10D}, {0.25D, 0.90D}};

        try {
            player.setShiftKeyDown(false);
            for (int i = 0; i < existingFacings.length + 3; i++) {
                clearFixtureBlock(level, stacked);
                clearFixtureBlock(level, target);
                level.setBlock(support, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

                Direction existingFacing = i < existingFacings.length ? existingFacings[i] : Direction.NORTH;
                Block placingBlock = i == 4 ? spruceVertical : oakVertical;
                level.setBlock(target, oakVertical.defaultBlockState()
                        .setValue(VerticalSlabBlock.FACING, existingFacing)
                        .setValue(VerticalSlabBlock.DOUBLE, false), Block.UPDATE_ALL);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(placingBlock, 64));

                boolean hitExistingSlab = i >= 5;
                boolean stackOnOccupiedHalf = i == 6;
                double hitX = i < 4 ? groundHits[i][0] : (i == 4 ? 0.10D : 0.50D);
                double hitZ = i < 4 ? groundHits[i][1] : (stackOnOccupiedHalf ? 0.25D : 0.75D);
                BlockPos clickedPos = hitExistingSlab ? target : support;
                BlockHitResult hit = new BlockHitResult(
                        new Vec3(support.getX() + hitX, clickedPos.getY() + 1.0D, support.getZ() + hitZ),
                        Direction.UP, clickedPos, false);
                InteractionResult result = placingBlock.asItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

                BlockState actual = level.getBlockState(target);
                BlockState above = level.getBlockState(stacked);
                BlockState[] savedMaterials = i == 4 ? EnhancedSlabMaterialSavedData.get(level, target.asLong()) : null;
                boolean casePassed = result.consumesAction()
                        && actual.hasProperty(VerticalSlabBlock.DOUBLE)
                        && actual.is(stackOnOccupiedHalf ? oakVertical : (i == 4 ? CommonBlocks.VERTICAL_SLAB : oakVertical))
                        && actual.getValue(VerticalSlabBlock.FACING) == existingFacing
                        && actual.getValue(VerticalSlabBlock.DOUBLE) == !stackOnOccupiedHalf
                        && (i != 4 || (materialSlotCount(savedMaterials) == 2
                        && slotsContainMaterial(savedMaterials, Blocks.OAK_SLAB.defaultBlockState())
                        && slotsContainMaterial(savedMaterials, Blocks.SPRUCE_SLAB.defaultBlockState())))
                        && (!stackOnOccupiedHalf || (above.is(oakVertical)
                        && above.hasProperty(VerticalSlabBlock.DOUBLE)
                        && !above.getValue(VerticalSlabBlock.DOUBLE)));
                checked++;
                passed &= casePassed;
                String label = i < 4 ? "ground-" + existingFacing.getSerializedName()
                        : (i == 4 ? "ground-mixed" : (stackOnOccupiedHalf ? "top-stack" : "top-empty-half"));
                details.add(label + "=" + (casePassed ? "ok" : "fail result:" + result
                        + ",target:" + blockId(actual) + ",double:" + (actual.hasProperty(VerticalSlabBlock.DOUBLE)
                        ? actual.getValue(VerticalSlabBlock.DOUBLE) : "missing")
                        + ",above:" + blockId(above) + ",materials:" + describeSlots(savedMaterials)));
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, heldBefore);
            player.setShiftKeyDown(wasShiftDown);
            clearFixtureBlock(level, stacked);
            clearFixtureBlock(level, target);
            clearFixtureBlock(level, support);
        }

        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkWaterBucketPickupTransition(MutableFixtureContext context) {
        BlockPos pos = context.basePos().offset(11, 0, 0);
        BlockState wet = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true);
        context.serverLevel().setBlock(pos, wet, Block.UPDATE_ALL);

        BlockState before = context.serverLevel().getBlockState(pos);
        context.serverLevel().setBlock(
                pos,
                before.setValue(BlockStateProperties.WATERLOGGED, false),
                Block.UPDATE_ALL
        );
        BlockState after = context.serverLevel().getBlockState(pos);

        boolean pickupLeavesDry = before.getValue(BlockStateProperties.WATERLOGGED)
                && !after.getValue(BlockStateProperties.WATERLOGGED);

        BlockState door = Blocks.OAK_DOOR.defaultBlockState();
        boolean addedProperty = door.hasProperty(BlockStateProperties.WATERLOGGED);
        boolean existingWaterSurvivesConfigDisable = false;
        boolean unsupportedWaterloggedBlockLeavesSource = false;
        boolean shapeUpdatesPreserveWater = false;
        if (addedProperty) {
            BlockState wetDoor = door.setValue(BlockStateProperties.WATERLOGGED, true);
            boolean previousEnabled = AutoWaterloggingRuntime.enabled;
            try {
                AutoWaterloggingRuntime.enabled = false;
                existingWaterSurvivesConfigDisable = AutoWaterloggingRuntime.isAddedWaterloggingActive(wetDoor)
                        && wetDoor.getFluidState().getType() == Fluids.WATER;
            } finally {
                AutoWaterloggingRuntime.enabled = previousEnabled;
            }
            unsupportedWaterloggedBlockLeavesSource = AutoWaterloggingBlockSupport
                    .preserveWaterloggedUpdateShape(wetDoor, Blocks.AIR.defaultBlockState())
                    .is(Blocks.WATER);
            shapeUpdatesPreserveWater = AutoWaterloggingBlockSupport
                    .preserveWaterloggedUpdateShape(
                            wetDoor,
                            wetDoor.setValue(BlockStateProperties.WATERLOGGED, false)
                    )
                    .getValue(BlockStateProperties.WATERLOGGED);
        }

        boolean passed = pickupLeavesDry
                && existingWaterSurvivesConfigDisable
                && unsupportedWaterloggedBlockLeavesSource
                && shapeUpdatesPreserveWater;
        return new CheckResult(
                passed,
                4,
                List.of(
                        "water-bucket-pickup-transition=" + (pickupLeavesDry ? "dry" : "still-waterlogged"),
                        "waterlogged-added-block-config-disable="
                                + (existingWaterSurvivesConfigDisable ? "preserved" : "lost"),
                        "waterlogged-added-block-support-loss="
                                + (unsupportedWaterloggedBlockLeavesSource ? "water" : "air"),
                        "waterlogged-added-block-shape-update="
                                + (shapeUpdatesPreserveWater ? "preserved" : "lost")
                )
        );
    }

    private static CheckResult checkStepFluidGeometryRefresh(MutableFixtureContext context) {
        ServerLevel level = context.serverLevel();
        BlockState oakSlab = Blocks.OAK_SLAB.defaultBlockState();
        List<String> details = new ArrayList<>();
        boolean passed = true;
        int checked = 0;

        boolean quadStepDynamic = CommonBlocks.QUAD_STEP.hasDynamicShape();
        boolean quadVerticalStepDynamic = CommonBlocks.QUAD_VERTICAL_STEP.hasDynamicShape();
        passed &= quadStepDynamic && quadVerticalStepDynamic;
        checked += 2;
        details.add("quad-step-dynamic-shape=" + (quadStepDynamic ? "pass" : "fail"));
        details.add("quad-vertical-step-dynamic-shape=" + (quadVerticalStepDynamic ? "pass" : "fail"));

        BlockPos horizontalPos = context.basePos().offset(7, 0, 1);
        BlockPos horizontalWaterPos = horizontalPos.east();
        BlockState horizontalState = CommonBlocks.QUAD_STEP.defaultBlockState()
                .setValue(QuadStepBlock.WATERLOGGED, true)
                .setValue(QuadStepBlock.AXIS, Direction.Axis.X);
        EnhancedSlabMaterialSavedData.remember(
                level, horizontalPos.asLong(), new BlockState[]{oakSlab, null, null, null});
        level.setBlock(horizontalPos, horizontalState, Block.UPDATE_ALL);
        level.setBlock(horizontalWaterPos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity horizontalEntity = level.getBlockEntity(horizontalPos);
        if (horizontalEntity instanceof QuadStepBlockEntity quadStep) {
            quadStep.setAllSlabs(new BlockState[]{oakSlab, null, null, null});
        }
        boolean horizontalContainerPresent = level.getBlockState(horizontalPos).is(CommonBlocks.QUAD_STEP);
        level.getFluidTicks().clearArea(BoundingBox.fromCorners(
                horizontalPos.offset(-1, -1, -1), horizontalPos.offset(1, 1, 1)));
        if (horizontalEntity instanceof QuadStepBlockEntity quadStep) {
            quadStep.setSlabAt(2, oakSlab);
        } else if (horizontalContainerPresent) {
            EnhancedSlabMaterialSavedData.remember(
                    level, horizontalPos.asLong(), new BlockState[]{oakSlab, null, oakSlab, null});
        }
        if (horizontalContainerPresent) {
            VoxelShape expected = Shapes.or(
                    QuadStepBlock.shapeForIndex(Direction.Axis.X, 0),
                    QuadStepBlock.shapeForIndex(Direction.Axis.X, 2));
            VoxelShape actual = level.getBlockState(horizontalPos)
                    .getCollisionShape(level, horizontalPos, CollisionContext.empty());
            boolean collisionUpdated = fixtureShapesEqual(expected, actual);
            boolean ownerTicked = level.getFluidTicks().hasScheduledTick(horizontalPos, Fluids.WATER);
            boolean neighborTicked = level.getFluidTicks().hasScheduledTick(horizontalWaterPos, Fluids.WATER);
            passed &= collisionUpdated && ownerTicked && neighborTicked;
            checked += 3;
            details.add("quad-step-fluid-collision-refresh=" + (collisionUpdated ? "pass" : "fail"));
            details.add("quad-step-owner-water-tick=" + (ownerTicked ? "pass" : "fail"));
            details.add("quad-step-neighbor-water-tick=" + (neighborTicked ? "pass" : "fail"));
        } else {
            passed = false;
            checked += 3;
            details.add("quad-step-fluid-refresh=missing-block-entity");
        }

        BlockPos verticalPos = context.basePos().offset(9, 0, 1);
        BlockPos verticalWaterPos = verticalPos.east();
        BlockState verticalState = CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState()
                .setValue(QuadVerticalStepBlock.WATERLOGGED, true);
        EnhancedSlabMaterialSavedData.remember(
                level, verticalPos.asLong(), new BlockState[]{oakSlab, null, null, null});
        level.setBlock(verticalPos, verticalState, Block.UPDATE_ALL);
        level.setBlock(verticalWaterPos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity verticalEntity = level.getBlockEntity(verticalPos);
        if (verticalEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            quadVerticalStep.setAllSlabs(new BlockState[]{oakSlab, null, null, null});
        }
        boolean verticalContainerPresent = level.getBlockState(verticalPos).is(CommonBlocks.QUAD_VERTICAL_STEP);
        level.getFluidTicks().clearArea(BoundingBox.fromCorners(
                verticalPos.offset(-1, -1, -1), verticalPos.offset(1, 1, 1)));
        if (verticalEntity instanceof QuadVerticalStepBlockEntity quadVerticalStep) {
            quadVerticalStep.setSlabAt(1, oakSlab);
        } else if (verticalContainerPresent) {
            EnhancedSlabMaterialSavedData.remember(
                    level, verticalPos.asLong(), new BlockState[]{oakSlab, oakSlab, null, null});
        }
        if (verticalContainerPresent) {
            VoxelShape expected = Shapes.or(
                    QuadVerticalStepBlock.shapeForIndex(0),
                    QuadVerticalStepBlock.shapeForIndex(1));
            VoxelShape actual = level.getBlockState(verticalPos)
                    .getCollisionShape(level, verticalPos, CollisionContext.empty());
            boolean collisionUpdated = fixtureShapesEqual(expected, actual);
            boolean ownerTicked = level.getFluidTicks().hasScheduledTick(verticalPos, Fluids.WATER);
            boolean neighborTicked = level.getFluidTicks().hasScheduledTick(verticalWaterPos, Fluids.WATER);
            passed &= collisionUpdated && ownerTicked && neighborTicked;
            checked += 3;
            details.add("quad-vertical-step-fluid-collision-refresh=" + (collisionUpdated ? "pass" : "fail"));
            details.add("quad-vertical-step-owner-water-tick=" + (ownerTicked ? "pass" : "fail"));
            details.add("quad-vertical-step-neighbor-water-tick=" + (neighborTicked ? "pass" : "fail"));
        } else {
            passed = false;
            checked += 3;
            details.add("quad-vertical-step-fluid-refresh=missing-block-entity");
        }

        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkStackedStepFluidWall(MutableFixtureContext context) {
        ServerLevel level = context.serverLevel();
        BlockState oakSlab = Blocks.OAK_SLAB.defaultBlockState();
        Direction.Axis[] axes = {
                Direction.Axis.X, Direction.Axis.X, Direction.Axis.Z, Direction.Axis.Z
        };
        Direction[] openSides = {
                Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
        };
        BlockState[][] slotCases = {
                {oakSlab, null, oakSlab, null},
                {null, oakSlab, null, oakSlab},
                {oakSlab, null, oakSlab, null},
                {null, oakSlab, null, oakSlab}
        };

        List<String> details = new ArrayList<>();
        boolean passed = true;
        for (int caseIndex = 0; caseIndex < axes.length; caseIndex++) {
            BlockPos targetPos = context.basePos().offset(32 + (caseIndex * 4), 0, 0);
            Direction openSide = openSides[caseIndex];
            BlockPos outputPos = targetPos.relative(openSide);
            BlockState[] slots = slotCases[caseIndex];

            EnhancedSlabMaterialSavedData.remember(level, targetPos.asLong(), slots);
            BlockState wetColumn = CommonBlocks.QUAD_STEP.defaultBlockState()
                    .setValue(QuadStepBlock.WATERLOGGED, true)
                    .setValue(QuadStepBlock.AXIS, axes[caseIndex]);
            level.setBlock(targetPos, wetColumn, Block.UPDATE_ALL);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity instanceof QuadStepBlockEntity quadStep) {
                quadStep.setAllSlabs(slots);
            }

            level.setBlock(targetPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighborPos = targetPos.relative(direction);
                level.setBlock(
                        neighborPos,
                        direction == openSide ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(),
                        Block.UPDATE_ALL);
            }

            bitsandbalance$tickWater(level, targetPos);
            boolean blocked = level.getFluidState(outputPos).isEmpty();
            passed &= blocked;
            details.add("stacked-step-fluid-wall-"
                    + axes[caseIndex].getName() + "-" + openSide.getName()
                    + "=" + (blocked ? "blocked" : "leaked"));

            BlockState current = level.getBlockState(targetPos);
            if (current.is(CommonBlocks.QUAD_STEP) && current.hasProperty(QuadStepBlock.WATERLOGGED)) {
                level.setBlock(targetPos, current.setValue(QuadStepBlock.WATERLOGGED, false), Block.UPDATE_ALL);
            }
            EnhancedSlabMaterialSavedData.remember(level, targetPos.asLong());
            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(targetPos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                level.setBlock(targetPos.relative(direction), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return new CheckResult(passed, axes.length, details);
    }

    private static void bitsandbalance$tickWater(ServerLevel level, BlockPos pos) {
        //? if mc == 1.21.1
        Fluids.WATER.tick(level, pos, level.getFluidState(pos));
        //? else
        Fluids.WATER.tick(level, pos, level.getBlockState(pos), level.getFluidState(pos));
        //? endif
    }

    private static FixtureResult runSableStepFixture(MutableFixtureContext context, FixtureRequest request) {
        if (!"1.21.1-neoforge".equals(request.target())) {
            context.details().add("sable-steps-target-skipped=" + request.target());
            return new FixtureResult(request.target(), true, 0, 1, context.details());
        }
        if (context.serverPlayer() == null) {
            context.details().add("sable-steps=no-server-player");
            return new FixtureResult(request.target(), false, 0, 0, context.details());
        }

        cleanupRegion(context);

        SableFixtureApi api = SableFixtureApi.create(context.details());
        if (api == null) {
            return new FixtureResult(request.target(), false, 0, 0, context.details());
        }

        int checked = 0;
        int skipped = 0;
        boolean passed = true;

        boolean placementGeometryPassed = VerticalStepPlacementGeometry.validatesBoundaryFallbackAndConnectivity();
        checked++;
        passed &= placementGeometryPassed;
        context.details().add("sable-vertical-step-adjacent-placement-geometry="
                + (placementGeometryPassed ? "pass" : "fail"));

        if ("fresh-sable-clip-step-split".equalsIgnoreCase(request.scenario().trim())) {
            CheckResult clipSplit = checkSableClipStepSplitDelayed(context, api);
            checked += clipSplit.checked();
            passed &= clipSplit.passed();
            context.details().addAll(clipSplit.details());
            return new FixtureResult(request.target(), passed, checked, skipped, context.details());
        }

        CheckResult vanillaSplit = checkSableVanillaBridgeSplitDelayed(context, api);
        checked += vanillaSplit.checked();
        passed &= vanillaSplit.passed();
        context.details().addAll(vanillaSplit.details());
        if ("fresh-sable-recursive-split".equalsIgnoreCase(request.scenario().trim())) {
            return new FixtureResult(request.target(), passed, checked, skipped, context.details());
        }

        SimPhysicsAssemblerFixtureApi physicsAssemblerApi = SimPhysicsAssemblerFixtureApi.create(context.details());
        if (physicsAssemblerApi == null) {
            skipped++;
        } else {
            List<SablePhysicsMaterialCase> selectedCases = selectedSablePhysicsMaterialCases(
                    request,
                    context.basePos(),
                    context.details()
            );
            if (selectedCases.isEmpty()) {
                return new FixtureResult(request.target(), false, checked, skipped, context.details());
            }
            context.details().add("sable-steps-scope=" + (selectedCases.size() == 1
                    ? "single-case-fresh-world-lifecycle"
                    : "legacy-full-material-list"));
            for (SablePhysicsMaterialCase materialCase : selectedCases) {
                CheckResult result = checkPhysicsAssemblerMaterialCaseDelayed(
                        context,
                        api,
                        physicsAssemblerApi,
                        materialCase
                );
                checked += result.checked();
                passed &= result.passed();
                context.details().addAll(result.details());
                if (result.details().stream().anyMatch(detail -> detail.contains("lifecycle-timeout-unsafe-to-continue"))) {
                    context.details().add("physics-material-cases-aborted=unstable-sable-split");
                    break;
                }
            }

            // The lifecycle matrix above validates assembler conversion and post-break bodies.
            // Exercise real item placement and targeted breaking on live plot blocks separately so
            // those destructive interactions cannot perturb the mass/collision lifecycle oracle.
            List<SableStepCase> liveInteractionCases = sableStepCases(
                    "live-plot",
                    context.basePos().offset(224, 0, 0),
                    context.details()
            ).stream().filter(stepCase -> stepCase.name().endsWith("-stored")).toList();
            for (SableStepCase stepCase : liveInteractionCases) {
                CheckResult interaction = runCheckOnServerThread(
                        context,
                        stepCase.name(),
                        () -> checkSableStepCase(context, api, stepCase)
                );
                checked += interaction.checked();
                passed &= interaction.passed();
                context.details().addAll(interaction.details());
            }
        }

        return new FixtureResult(request.target(), passed, checked, skipped, context.details());
    }

    /**
     * Recreates the clip's cut topology without depending on a player's view ray:
     * a two-Step vertical slab is attached to a larger enhanced-Step body through one removable
     * bridge quarter. The fixture compares horizontal world transforms and orientation so normal
     * gravity during Sable's split cannot hide or imitate the recorded lateral pose jump.
     */
    private static CheckResult checkSableClipStepSplitDelayed(MutableFixtureContext context,
                                                               SableFixtureApi api) {
        MinecraftServer server = context.serverLevel().getServer();
        if (server == null) {
            return new CheckResult(false, 1, List.of("sable-clip-step-split.server-thread=no-server"));
        }

        CompletableFuture<CheckResult> future = new CompletableFuture<>();
        server.execute(() -> {
            ServerLevel level = context.serverLevel();
            List<String> details = new ArrayList<>();
            boolean wasPaused = false;
            try {
                BlockState oak = normalizedSlab(Blocks.OAK_SLAB);
                BlockState spruce = normalizedSlab(Blocks.SPRUCE_SLAB);
                BlockState birch = normalizedSlab(Blocks.BIRCH_SLAB);
                BlockState quadState = CommonBlocks.QUAD_STEP.defaultBlockState()
                        .setValue(QuadStepBlock.WATERLOGGED, false)
                        .setValue(QuadStepBlock.AXIS, Direction.Axis.X);
                BlockState[] detachedSlots = new BlockState[]{null, spruce, null, spruce};
                // Match the reported two-Step bridge exactly: the two disconnected quarters share
                // one owner position, and mining the west quarter must leave the east quarter in
                // the retained body while the west body splits immediately without a relog.
                BlockState[] bridgeSlots = new BlockState[]{oak, oak, null, null};
                BlockState[] retainedSlots = new BlockState[]{oak, oak, oak, oak};
                BlockState[] ballastSlots = new BlockState[]{birch, birch, birch, birch};

                BlockPos detachedWorldPos = context.basePos().offset(288, 24, 0);
                BlockPos bridgeWorldPos = detachedWorldPos.east();
                BlockPos retainedWorldPos = detachedWorldPos.east(2);
                BlockPos ballastWorldPos = detachedWorldPos.east(3);
                // Sable unloads untracked bodies. Keep the QuickPlay player beside this isolated
                // fixture so disappearance means a real split/removal failure, not normal
                // distance-based sublevel unloading.
                context.serverPlayer().connection.teleport(
                        detachedWorldPos.getX() + 0.5D,
                        detachedWorldPos.getY() + 2.0D,
                        detachedWorldPos.getZ() + 3.5D,
                        context.serverPlayer().getYRot(),
                        context.serverPlayer().getXRot()
                );
                for (int x = -2; x <= 5; x++) {
                    BlockPos clearPos = detachedWorldPos.offset(x, 0, 0);
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    // Keep both split bodies in the fixture volume long enough to inspect their
                    // completed heat maps and poses. Without a floor, six seconds of gravity can
                    // carry the bodies below the world before the poll discovers them.
                    level.setBlock(clearPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    clearFixtureMaterial(level, clearPos);
                }

                level.setBlock(detachedWorldPos, quadState, Block.UPDATE_ALL);
                level.setBlock(bridgeWorldPos, quadState, Block.UPDATE_ALL);
                level.setBlock(retainedWorldPos, quadState, Block.UPDATE_ALL);
                level.setBlock(ballastWorldPos, quadState, Block.UPDATE_ALL);
                EnhancedSlabMaterialSavedData.remember(level, detachedWorldPos.asLong(), detachedSlots);
                EnhancedSlabMaterialSavedData.remember(level, bridgeWorldPos.asLong(), bridgeSlots);
                EnhancedSlabMaterialSavedData.remember(level, retainedWorldPos.asLong(), retainedSlots);
                EnhancedSlabMaterialSavedData.remember(level, ballastWorldPos.asLong(), ballastSlots);

                SableGatherResult gather = api.gatherShapeAware(level, retainedWorldPos, 64);
                if (gather == null || !"SUCCESS".equals(gather.state()) || gather.blocks().size() != 4) {
                    details.add("sable-clip-step-split.gather=" + (gather == null
                            ? "null"
                            : gather.state() + ":blocks=" + gather.blocks().size()));
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }

                Set<Object> existing = api.subLevelIdentitySnapshot(level);
                Object original = api.assemble(level, retainedWorldPos, gather.blocks(), gather.boundingBox());
                SableMaterialLocation detached = api.findActiveMaterialLocation(
                        level, existing, CommonBlocks.QUAD_STEP, detachedSlots);
                SableMaterialLocation bridge = api.findActiveMaterialLocation(
                        level, existing, CommonBlocks.QUAD_STEP, bridgeSlots);
                SableMaterialLocation retained = api.findActiveMaterialLocation(
                        level, existing, CommonBlocks.QUAD_STEP, retainedSlots);
                if (original == null || detached == null || bridge == null || retained == null
                        || detached.subLevel() != original || bridge.subLevel() != original
                        || retained.subLevel() != original) {
                    details.add("sable-clip-step-split.assembled-layout=invalid");
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }

                wasPaused = api.physicsPaused(level);
                scheduleSableClipTrackedMutation(
                        server,
                        level,
                        api,
                        context.serverPlayer(),
                        original,
                        existing,
                        bridge.targetPos(),
                        detachedSlots,
                        retainedSlots,
                        ballastSlots,
                        wasPaused,
                        details,
                        future,
                        level.getGameTime() + 80L
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                try {
                    api.setPhysicsPaused(level, wasPaused);
                } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                }
                details.add("sable-clip-step-split.setup=" + exception.getClass().getSimpleName());
                future.complete(new CheckResult(false, 1, details));
            }
        });

        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new CheckResult(false, 1, List.of("sable-clip-step-split=interrupted"));
        } catch (Exception exception) {
            return new CheckResult(false, 1, List.of("sable-clip-step-split="
                    + exception.getClass().getSimpleName()));
        }
    }

    private static void scheduleSableClipTrackedMutation(MinecraftServer server,
                                                          ServerLevel level,
                                                          SableFixtureApi api,
                                                          ServerPlayer player,
                                                          Object original,
                                                          Set<Object> existing,
                                                          BlockPos bridgePos,
                                                          BlockState[] detachedSlots,
                                                          BlockState[] retainedSlots,
                                                          BlockState[] ballastSlots,
                                                          boolean wasPaused,
                                                          List<String> details,
                                                          CompletableFuture<CheckResult> future,
                                                          long trackingTimeoutGameTime) {
        CompletableFuture.delayedExecutor(50L, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
            try {
                if (SableCompat.isPlayerTrackingPlotPosition(level, bridgePos, player.getUUID())) {
                    details.add("sable-clip-step-split.player-tracking=pass");
                    int massRebuildBefore = SableCompat.enhancedSlabMassTrackerRebuildCount();
                    int historyReuseBefore = SableCompat.enhancedSlabMergedHistoryReuseCount();

                    BlockState[] bridgeBeforeSlots = EnhancedSlabMaterialSavedData.get(level, bridgePos.asLong());
                    BlockState bridgeState = SableCompat.plotBlockState(level, bridgePos);
                    int targetedIndex = bridgeState != null && bridgeState.hasProperty(QuadStepBlock.AXIS)
                            ? QuadStepBlock.bitsandbalance$getTargetedIndex(
                            player, bridgePos, bridgeState.getValue(QuadStepBlock.AXIS))
                            : -1;
                    if (bridgeBeforeSlots == null || targetedIndex < 0
                            || targetedIndex >= bridgeBeforeSlots.length
                            || bridgeBeforeSlots[targetedIndex] == null) {
                        api.setPhysicsPaused(level, wasPaused);
                        details.add("sable-clip-step-split.target-lock=invalid:index=" + targetedIndex
                                + ",slots=" + describeSlots(bridgeBeforeSlots));
                        future.complete(new CheckResult(false, 1, details));
                        return;
                    }

                    BlockState remainingBridgeMaterial = null;
                    for (int index = 0; index < bridgeBeforeSlots.length; index++) {
                        if (index != targetedIndex && bridgeBeforeSlots[index] != null) {
                            remainingBridgeMaterial = bridgeBeforeSlots[index];
                            break;
                        }
                    }
                    // A one-quarter QuadStep normalizes to FixedStepBlock storage, whose material
                    // occupies slot zero; the original quarter is preserved by FACING/TOP.
                    BlockState[] bridgeAfterSlots = new BlockState[]{remainingBridgeMaterial, null, null, null};
                    player.setShiftKeyDown(false);
                    player.gameMode.handleBlockBreakAction(
                            bridgePos,
                            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                            Direction.UP,
                            //? if mc == 1.21.1
                            level.getMaxBuildHeight(),
                            //? else
                            level.getMaxY(),
                            //? endif
                            0
                    );
                    BlockState[] afterStart = EnhancedSlabMaterialSavedData.get(level, bridgePos.asLong());
                    if (sameMaterialSlots(bridgeBeforeSlots, afterStart)) {
                        player.gameMode.destroyBlock(bridgePos);
                    }
                    BlockState[] actualAfterSlots = EnhancedSlabMaterialSavedData.get(level, bridgePos.asLong());
                    BlockState bridgeAfterState = SableCompat.plotBlockState(level, bridgePos);
                    if (!sameMaterialSlots(bridgeAfterSlots, actualAfterSlots)) {
                        api.setPhysicsPaused(level, wasPaused);
                        details.add("sable-clip-step-split.target-lock=fail:index=" + targetedIndex
                                + ",before=" + describeSlots(bridgeBeforeSlots)
                                + ",actual=" + describeSlots(actualAfterSlots)
                                + ",expected=" + describeSlots(bridgeAfterSlots));
                        future.complete(new CheckResult(false, 1, details));
                        return;
                    }
                    details.add("sable-clip-step-split.target-lock=pass:index=" + targetedIndex
                            + ",crouching:false,removedSlots:1");
                    details.add("sable-clip-step-split.partial-removal=pass:real-player-path");
                    scheduleSableClipStepSplitPoll(
                            server, level, api, original, existing, detachedSlots,
                            bridgeAfterState == null ? CommonBlocks.QUAD_STEP : bridgeAfterState.getBlock(), bridgeAfterSlots,
                            retainedSlots, ballastSlots, massRebuildBefore, historyReuseBefore,
                            wasPaused, details, future, null, null, 0,
                            level.getGameTime() + 20L, level.getGameTime() + 120L);
                    return;
                }
                if (level.getGameTime() >= trackingTimeoutGameTime || api.isRemoved(original)) {
                    api.setPhysicsPaused(level, wasPaused);
                    details.add("sable-clip-step-split.player-tracking=timeout:originalRemoved="
                            + api.isRemoved(original));
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                try {
                    api.setPhysicsPaused(level, wasPaused);
                } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                }
                details.add("sable-clip-step-split.player-tracking="
                        + exception.getClass().getSimpleName());
                future.complete(new CheckResult(false, 1, details));
                return;
            }
            scheduleSableClipTrackedMutation(
                    server, level, api, player, original, existing, bridgePos,
                    detachedSlots, retainedSlots, ballastSlots, wasPaused,
                    details, future, trackingTimeoutGameTime);
        }));
    }

    private static void scheduleSableClipStepSplitPoll(MinecraftServer server,
                                                        ServerLevel level,
                                                        SableFixtureApi api,
                                                        Object original,
                                                        Set<Object> existing,
                                                        BlockState[] detachedSlots,
                                                        Block bridgeAfterBlock,
                                                        BlockState[] bridgeAfterSlots,
                                                        BlockState[] retainedSlots,
                                                        BlockState[] ballastSlots,
                                                        int massRebuildBefore,
                                                        int historyReuseBefore,
                                                        boolean wasPaused,
                                                        List<String> details,
                                                        CompletableFuture<CheckResult> future,
                                                        @Nullable Object stableDetachedBody,
                                                        @Nullable Object stableRetainedBody,
                                                        int stableTicks,
                                                        long minimumCompletionGameTime,
                                                        long timeoutGameTime) {
        CompletableFuture.delayedExecutor(50L, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
            try {
                int massRebuildDelta = SableCompat.enhancedSlabMassTrackerRebuildCount()
                        - massRebuildBefore;
                int historyReuseDelta = SableCompat.enhancedSlabMergedHistoryReuseCount()
                        - historyReuseBefore;
                SableMaterialLocation detached = api.findActiveMaterialLocation(
                        level, existing, CommonBlocks.QUAD_STEP, detachedSlots);
                SableMaterialLocation bridge = api.findActiveMaterialLocation(
                        level, existing, bridgeAfterBlock, bridgeAfterSlots);
                SableMaterialLocation retained = api.findActiveMaterialLocation(
                        level, existing, CommonBlocks.QUAD_STEP, retainedSlots);
                SableMaterialLocation ballast = api.findActiveMaterialLocation(
                        level, existing, CommonBlocks.QUAD_STEP, ballastSlots);

                boolean completeLayout = detached != null && bridge != null && retained != null && ballast != null;
                boolean twoBodies = completeLayout
                        && detached.subLevel() != retained.subLevel()
                        && bridge.subLevel() == retained.subLevel()
                        && ballast.subLevel() == retained.subLevel();
                boolean bodiesRegistered = twoBodies
                        && api.containsSubLevelIdentity(level, detached.subLevel())
                        && api.containsSubLevelIdentity(level, retained.subLevel())
                        && !api.isRemoved(detached.subLevel())
                        && !api.isRemoved(retained.subLevel());
                SableHeatMapStatus detachedHeatMap = bodiesRegistered
                        ? api.heatMapStatus(detached.subLevel())
                        : new SableHeatMapStatus(false, "body-missing");
                SableHeatMapStatus retainedHeatMap = bodiesRegistered
                        ? api.heatMapStatus(retained.subLevel())
                        : new SableHeatMapStatus(false, "body-missing");
                double detachedMass = bodiesRegistered
                        ? api.subLevelMass(detached.subLevel(), "getSelfMassTracker")
                        : 0.0D;
                double retainedMass = bodiesRegistered
                        ? api.subLevelMass(retained.subLevel(), "getSelfMassTracker")
                        : 0.0D;
                boolean detachedPhysics = bodiesRegistered && api.hasPhysicsHandle(level, detached.subLevel());
                boolean retainedPhysics = bodiesRegistered && api.hasPhysicsHandle(level, retained.subLevel());
                boolean lifecycleStable = level.getGameTime() >= minimumCompletionGameTime
                        && massRebuildDelta > 0
                        && bodiesRegistered
                        && detachedHeatMap.stable()
                        && retainedHeatMap.stable()
                        && detachedMass > 0.0D
                        && retainedMass > 0.0D
                        && detachedPhysics
                        && retainedPhysics;

                Object nextDetachedBody = lifecycleStable ? detached.subLevel() : null;
                Object nextRetainedBody = lifecycleStable ? retained.subLevel() : null;
                int nextStableTicks = lifecycleStable
                        ? (stableDetachedBody == nextDetachedBody && stableRetainedBody == nextRetainedBody
                        ? stableTicks + 1
                        : 1)
                        : 0;
                if (nextStableTicks >= 5) {
                    // Initialized bodies must use the history-bearing MergedMassTracker path. One
                    // freshly assembled split successor may legitimately have no prior history.
                    int uninitializedFallbacks = massRebuildDelta - historyReuseDelta;
                    boolean historySafe = historyReuseDelta > 0
                            && uninitializedFallbacks >= 0
                            && uninitializedFallbacks <= 1;
                    boolean passed = historySafe && lifecycleStable;
                    details.add("sable-clip-step-split=" + (passed ? "pass" : "fail")
                            + ":stableTicks:" + nextStableTicks
                            + ",materialOwners:4,bodies:2"
                            + ",detachedMass:" + detachedMass
                            + ",retainedMass:" + retainedMass
                            + ",detachedPhysics:" + detachedPhysics
                            + ",retainedPhysics:" + retainedPhysics
                            + ",massRebuildDelta:" + massRebuildDelta
                            + ",historyReuseDelta:" + historyReuseDelta
                            + ",uninitializedFallbacks:" + uninitializedFallbacks);
                    api.setPhysicsPaused(level, wasPaused);
                    future.complete(new CheckResult(passed, 16, details));
                    return;
                }

                if (level.getGameTime() >= timeoutGameTime) {
                    api.setPhysicsPaused(level, wasPaused);
                    details.add("sable-clip-step-split=timeout:massRebuildDelta=" + massRebuildDelta
                            + ",historyReuseDelta=" + historyReuseDelta
                            + ",originalRemoved=" + api.isRemoved(original)
                            + ",materials=" + (detached != null ? 1 : 0)
                            + (bridge != null ? 1 : 0)
                            + (retained != null ? 1 : 0)
                            + (ballast != null ? 1 : 0)
                            + ",twoBodies=" + twoBodies
                            + ",registered=" + bodiesRegistered
                            + ",detachedHeatMap=" + detachedHeatMap.description()
                            + ",retainedHeatMap=" + retainedHeatMap.description()
                            + ",detachedMass=" + detachedMass
                            + ",retainedMass=" + retainedMass
                            + ",successorReconciliation="
                            + SableCompat.enhancedSlabSuccessorReconciliationDiagnostics()
                            + ",candidateBodies=" + api.describeCandidateEnhancedSlabBodies(level, existing));
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }
                scheduleSableClipStepSplitPoll(
                        server, level, api, original, existing, detachedSlots, bridgeAfterBlock, bridgeAfterSlots,
                        retainedSlots, ballastSlots, massRebuildBefore, historyReuseBefore,
                        wasPaused, details, future, nextDetachedBody, nextRetainedBody,
                        nextStableTicks, minimumCompletionGameTime, timeoutGameTime);
                return;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                try {
                    api.setPhysicsPaused(level, wasPaused);
                } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                }
                details.add("sable-clip-step-split.poll=" + exception.getClass().getSimpleName());
                future.complete(new CheckResult(false, 1, details));
                return;
            }
        }));
    }

    private static CheckResult checkSableVanillaBridgeSplitDelayed(MutableFixtureContext context,
                                                                    SableFixtureApi api) {
        MinecraftServer server = context.serverLevel().getServer();
        if (server == null) {
            return new CheckResult(false, 1, List.of("sable-vanilla-bridge-split.server-thread=no-server"));
        }

        CompletableFuture<CheckResult> future = new CompletableFuture<>();
        server.execute(() -> {
            ServerLevel level = context.serverLevel();
            BlockPos bridgePos = context.basePos().offset(256, 8, 0);
            BlockPos oakPos = bridgePos.west();
            BlockPos sprucePos = bridgePos.east();
            BlockPos secondBridgePos = bridgePos.east(2);
            BlockPos birchPos = bridgePos.east(3);
            List<String> details = new ArrayList<>();
            try {
                Set<Object> existing = api.subLevelIdentitySnapshot(level);
                for (int x = -4; x <= 4; x++) {
                    level.setBlock(bridgePos.offset(x, 0, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                level.setBlock(oakPos, Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(bridgePos, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(sprucePos, Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(secondBridgePos, Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(birchPos, Blocks.BIRCH_PLANKS.defaultBlockState(), Block.UPDATE_ALL);

                SableGatherResult gather = api.gather(level, sprucePos, 64);
                if (gather == null || !"SUCCESS".equals(gather.state()) || gather.blocks().size() != 5) {
                    details.add("sable-vanilla-bridge-split.gather=" + (gather == null
                            ? "null"
                            : gather.state() + ":blocks=" + gather.blocks().size()));
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }

                Object original = api.assemble(level, sprucePos, gather.blocks(), gather.boundingBox());
                BlockPos plotCenter = api.centerBlock(original);
                if (plotCenter == null
                        || !api.plotBlockState(original, plotCenter.west(2)).is(Blocks.OAK_PLANKS)
                        || !api.plotBlockState(original, plotCenter.west()).is(Blocks.GOLD_BLOCK)
                        || !api.plotBlockState(original, plotCenter).is(Blocks.SPRUCE_PLANKS)
                        || !api.plotBlockState(original, plotCenter.east()).is(Blocks.IRON_BLOCK)
                        || !api.plotBlockState(original, plotCenter.east(2)).is(Blocks.BIRCH_PLANKS)) {
                    details.add("sable-vanilla-bridge-split.assembled-layout=invalid");
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }

                boolean removed = context.serverPlayer().gameMode.destroyBlock(plotCenter.west());
                if (!removed) {
                    details.add("sable-vanilla-bridge-split.bridge-removal=failed");
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }

                details.add("sable-vanilla-bridge-split.bridge-removal=pass");
                scheduleSableVanillaBridgeSplitPoll(
                        server,
                        level,
                        api,
                        context.serverPlayer(),
                        existing,
                        details,
                        future,
                        1,
                        level.getGameTime() + 120L
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                details.add("sable-vanilla-bridge-split.setup=" + exception.getClass().getSimpleName());
                future.complete(new CheckResult(false, 1, details));
            }
        });

        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new CheckResult(false, 1, List.of("sable-vanilla-bridge-split=interrupted"));
        } catch (Exception exception) {
            return new CheckResult(false, 1, List.of("sable-vanilla-bridge-split="
                    + exception.getClass().getSimpleName()));
        }
    }

    private static void scheduleSableVanillaBridgeSplitPoll(MinecraftServer server,
                                                             ServerLevel level,
                                                             SableFixtureApi api,
                                                             ServerPlayer player,
                                                             Set<Object> existing,
                                                             List<String> details,
                                                             CompletableFuture<CheckResult> future,
                                                             int stage,
                                                             long timeoutGameTime) {
        CompletableFuture.delayedExecutor(50L, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
            try {
                SableMaterialLocation oak = api.findActiveBlockLocation(
                        level,
                        existing,
                        Blocks.OAK_PLANKS
                );
                SableMaterialLocation spruce = api.findActiveBlockLocation(
                        level,
                        existing,
                        Blocks.SPRUCE_PLANKS
                );
                SableMaterialLocation birch = api.findActiveBlockLocation(
                        level,
                        existing,
                        Blocks.BIRCH_PLANKS
                );
                boolean firstSplit = oak != null && spruce != null && birch != null
                        && oak.subLevel() != spruce.subLevel()
                        && spruce.subLevel() == birch.subLevel();
                if (stage == 1 && firstSplit
                        && api.heatMapStatus(oak.subLevel()).stable()
                        && api.heatMapStatus(spruce.subLevel()).stable()) {
                    BlockPos secondBridge = api.findBlockInPlot(spruce.subLevel(), Blocks.IRON_BLOCK);
                    boolean removed = secondBridge != null && player.gameMode.destroyBlock(secondBridge);
                    details.add("sable-vanilla-bridge-split.second-bridge-removal="
                            + (removed ? "pass" : "fail"));
                    if (!removed) {
                        future.complete(new CheckResult(false, 2, details));
                        return;
                    }
                    scheduleSableVanillaBridgeSplitPoll(
                            server,
                            level,
                            api,
                            player,
                            existing,
                            details,
                            future,
                            2,
                            level.getGameTime() + 120L
                    );
                    return;
                }

                boolean recursivelyDistinct = oak != null && spruce != null && birch != null
                        && oak.subLevel() != spruce.subLevel()
                        && oak.subLevel() != birch.subLevel()
                        && spruce.subLevel() != birch.subLevel();
                if (stage == 2 && recursivelyDistinct
                        && api.heatMapStatus(oak.subLevel()).stable()
                        && api.heatMapStatus(spruce.subLevel()).stable()
                        && api.heatMapStatus(birch.subLevel()).stable()) {
                    boolean oakPhysics = api.hasPhysicsHandle(level, oak.subLevel());
                    boolean sprucePhysics = api.hasPhysicsHandle(level, spruce.subLevel());
                    boolean birchPhysics = api.hasPhysicsHandle(level, birch.subLevel());
                    double oakMass = api.subLevelMass(oak.subLevel(), "getMassTracker");
                    double spruceMass = api.subLevelMass(spruce.subLevel(), "getMassTracker");
                    double birchMass = api.subLevelMass(birch.subLevel(), "getMassTracker");
                    boolean passed = oakPhysics && sprucePhysics && birchPhysics
                            && oakMass > 0.0D && spruceMass > 0.0D && birchMass > 0.0D;
                    details.add("sable-vanilla-bridge-split=" + (passed ? "pass" : "fail")
                            + ":recursivelyDistinct:true,oakPhysics:" + oakPhysics
                            + ",sprucePhysics:" + sprucePhysics
                            + ",birchPhysics:" + birchPhysics
                            + ",oakMass:" + oakMass
                            + ",spruceMass:" + spruceMass
                            + ",birchMass:" + birchMass);
                    future.complete(new CheckResult(passed, 8, details));
                    return;
                }

                if (level.getGameTime() >= timeoutGameTime) {
                    details.add("sable-vanilla-bridge-split=timeout:stage=" + stage + ",oak=" + (oak != null)
                            + ",spruce=" + (spruce != null)
                            + ",birch=" + (birch != null)
                            + ",firstSplit=" + firstSplit
                            + ",recursivelyDistinct=" + recursivelyDistinct);
                    future.complete(new CheckResult(false, 1, details));
                    return;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                details.add("sable-vanilla-bridge-split.poll=" + exception.getClass().getSimpleName());
                future.complete(new CheckResult(false, 1, details));
                return;
            }
            scheduleSableVanillaBridgeSplitPoll(
                    server,
                    level,
                    api,
                    player,
                    existing,
                    details,
                    future,
                    stage,
                    timeoutGameTime
            );
        }));
    }

    private static List<SablePhysicsMaterialCase> selectedSablePhysicsMaterialCases(FixtureRequest request,
                                                                                    BlockPos basePos,
                                                                                    List<String> details) {
        String scenario = request.scenario().trim().toLowerCase(Locale.ROOT);
        String prefix = "fresh-sable-";
        if (scenario.equals("default") || scenario.equals("all") || scenario.equals("fresh-sable-all")) {
            details.add("sable-steps-selected-case=all");
            return sablePhysicsMaterialCases(basePos);
        }
        if (scenario.equals("fresh-sable-step-matrix")) {
            details.add("sable-steps-selected-case=step-matrix");
            return sablePhysicsMaterialCases(basePos).stream()
                    .filter(materialCase -> materialCase.expectedSlabEquivalents() > 0.0D)
                    .toList();
        }
        if (scenario.equals("fresh-sable-horizontal-step-matrix")) {
            details.add("sable-steps-selected-case=horizontal-step-matrix");
            return sablePhysicsMaterialCases(basePos).stream()
                    .filter(materialCase -> materialCase.expectedSlabEquivalents() > 0.0D)
                    .filter(materialCase -> !materialCase.name().contains("vertical"))
                    .toList();
        }
        if (scenario.equals("fresh-sable-vertical-step-matrix")) {
            details.add("sable-steps-selected-case=vertical-step-matrix");
            return sablePhysicsMaterialCases(basePos).stream()
                    .filter(materialCase -> materialCase.expectedSlabEquivalents() > 0.0D)
                    .filter(materialCase -> materialCase.name().contains("vertical"))
                    .toList();
        }
        if (!scenario.startsWith(prefix) || scenario.length() == prefix.length()) {
            details.add("sable-steps-scenario-required=fresh-sable-<case>");
            details.add("sable-steps-cases=vanilla-stone-control,mixed-slab,mixed-vertical-slab,single-step,quad-step-one,quad-step-two,quad-step-three,quad-step-four,single-vertical-step,quad-vertical-step-one,quad-vertical-step-two,quad-vertical-step-three,quad-vertical-step-four");
            return List.of();
        }

        String selector = scenario.substring(prefix.length());
        String expectedName = selector.endsWith("-after-assembler-break")
                ? selector
                : selector + "-after-assembler-break";
        List<SablePhysicsMaterialCase> selected = sablePhysicsMaterialCases(basePos).stream()
                .filter(materialCase -> materialCase.name().equals(expectedName))
                .toList();
        if (selected.size() != 1) {
            details.add("sable-steps-unknown-case=" + selector);
            details.add("sable-steps-cases=vanilla-stone-control,mixed-slab,mixed-vertical-slab,single-step,quad-step-one,quad-step-two,quad-step-three,quad-step-four,single-vertical-step,quad-vertical-step-one,quad-vertical-step-two,quad-vertical-step-three,quad-vertical-step-four");
            return List.of();
        }
        details.add("sable-steps-selected-case=" + selected.getFirst().name());
        return selected;
    }

    private static CheckResult runCheckOnServerThread(MutableFixtureContext context,
                                                      String name,
                                                      FixtureCheckTask task) {
        MinecraftServer server = context.serverLevel().getServer();
        if (server == null) {
            return new CheckResult(false, 1, List.of(name + ".server-thread=no-server"));
        }

        if (server.isSameThread()) {
            return runFixtureCheck(name, task);
        }

        CompletableFuture<CheckResult> future = new CompletableFuture<>();
        server.execute(() -> future.complete(runFixtureCheck(name, task)));
        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new CheckResult(false, 1, List.of(name + ".server-thread=interrupted"));
        } catch (Exception exception) {
            return new CheckResult(false, 1, List.of(name + ".server-thread=" + exception.getClass().getSimpleName()));
        }
    }

    private static CheckResult runFixtureCheck(String name, FixtureCheckTask task) {
        try {
            return task.run();
        } catch (Exception exception) {
            return new CheckResult(false, 1, List.of(name + ".server-thread-exception=" + exception.getClass().getSimpleName()));
        }
    }

    private static List<SableStepCase> sableStepCases(String prefix, BlockPos basePos, List<String> details) {
        List<SableStepCase> cases = new ArrayList<>();

        Block oakStep = StepDynamicRegistry.getStepForSlab(Blocks.OAK_SLAB);
        Block spruceStep = StepDynamicRegistry.getStepForSlab(Blocks.SPRUCE_SLAB);
        if (oakStep == null || spruceStep == null) {
            details.add("sable-steps.missing-step-dynamic-blocks oak=" + blockId(oakStep) + " spruce=" + blockId(spruceStep));
        } else {
            BlockState oakMaterial = normalizedSlab(Blocks.OAK_SLAB);
            BlockState spruceMaterial = normalizedSlab(Blocks.SPRUCE_SLAB);
            cases.add(new SableStepCase(
                    prefix + "-step-stored",
                    basePos,
                    stepFixtureState(oakStep),
                    singleSlot(oakMaterial),
                    spruceStep,
                    oakMaterial,
                    spruceMaterial,
                    CommonBlocks.QUAD_STEP,
                    Direction.UP,
                    new Vec3(0.25D, 1.0D, 0.25D)
            ));
            cases.add(new SableStepCase(
                    prefix + "-step-bare",
                    basePos.offset(3, 0, 0),
                    stepFixtureState(oakStep),
                    null,
                    spruceStep,
                    oakMaterial,
                    spruceMaterial,
                    CommonBlocks.QUAD_STEP,
                    Direction.UP,
                    new Vec3(0.25D, 1.0D, 0.25D)
            ));
        }

        Block oakVerticalSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.OAK_SLAB);
        Block spruceVerticalSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.SPRUCE_SLAB);
        Block oakVerticalStep = oakVerticalSlab == null ? null : VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(oakVerticalSlab);
        Block spruceVerticalStep = spruceVerticalSlab == null ? null : VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(spruceVerticalSlab);
        if (oakVerticalStep == null || spruceVerticalStep == null) {
            details.add("sable-steps.missing-vertical-step-dynamic-blocks oak=" + blockId(oakVerticalStep)
                    + " spruce=" + blockId(spruceVerticalStep)
                    + " oakVerticalSlab=" + blockId(oakVerticalSlab)
                    + " spruceVerticalSlab=" + blockId(spruceVerticalSlab));
        } else {
            BlockState oakMaterial = normalizedSlab(Blocks.OAK_SLAB);
            BlockState spruceMaterial = normalizedSlab(Blocks.SPRUCE_SLAB);
            cases.add(new SableStepCase(
                    prefix + "-vertical-step-stored",
                    basePos.offset(6, 0, 0),
                    verticalStepFixtureState(oakVerticalStep),
                    singleSlot(oakMaterial),
                    spruceVerticalStep,
                    oakMaterial,
                    spruceMaterial,
                    CommonBlocks.QUAD_VERTICAL_STEP,
                    Direction.UP,
                    new Vec3(0.25D, 0.5D, 0.25D)
            ));
            cases.add(new SableStepCase(
                    prefix + "-vertical-step-bare",
                    basePos.offset(9, 0, 0),
                    verticalStepFixtureState(oakVerticalStep),
                    null,
                    spruceVerticalStep,
                    oakMaterial,
                    spruceMaterial,
                    CommonBlocks.QUAD_VERTICAL_STEP,
                    Direction.UP,
                    new Vec3(0.25D, 0.5D, 0.25D)
            ));
        }

        return cases;
    }

    private static List<SablePhysicsMaterialCase> sablePhysicsMaterialCases(BlockPos basePos) {
        BlockState oak = normalizedSlab(Blocks.OAK_SLAB);
        BlockState spruce = normalizedSlab(Blocks.SPRUCE_SLAB);
        BlockState stoneBrick = normalizedSlab(Blocks.STONE_BRICK_SLAB);
        BlockState brick = normalizedSlab(Blocks.BRICK_SLAB);
        Block oakStep = StepDynamicRegistry.getStepForSlab(Blocks.OAK_SLAB);
        BlockState singleStep = oakStep == null
                ? CommonBlocks.STEP.defaultBlockState()
                : stepFixtureState(oakStep);
        Block oakVerticalSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(Blocks.OAK_SLAB);
        Block oakVerticalStep = oakVerticalSlab == null
                ? null
                : VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(oakVerticalSlab);
        BlockState singleVerticalStep = oakVerticalStep == null
                ? CommonBlocks.VERTICAL_STEP.defaultBlockState()
                : verticalStepFixtureState(oakVerticalStep);
        return List.of(
                new SablePhysicsMaterialCase(
                        "vanilla-stone-control-after-assembler-break",
                        basePos.offset(192, 0, 0),
                        Blocks.STONE.defaultBlockState(),
                        null,
                        -1.0D
                ),
                new SablePhysicsMaterialCase(
                        "mixed-slab-after-assembler-break",
                        basePos,
                        CommonBlocks.MIXED_SLAB.defaultBlockState(),
                        new BlockState[]{oak, spruce},
                        -1.0D
                ),
                new SablePhysicsMaterialCase(
                        "mixed-vertical-slab-after-assembler-break",
                        basePos.offset(16, 0, 0),
                        CommonBlocks.VERTICAL_SLAB.defaultBlockState().setValue(VerticalSlabBlock.DOUBLE, true),
                        new BlockState[]{oak, spruce},
                        -1.0D
                ),
                new SablePhysicsMaterialCase(
                        "single-step-after-assembler-break",
                        basePos.offset(32, 0, 0),
                        singleStep,
                        new BlockState[]{oak},
                        0.5D
                ),
                new SablePhysicsMaterialCase(
                        "quad-step-one-after-assembler-break",
                        basePos.offset(48, 0, 0),
                        CommonBlocks.QUAD_STEP.defaultBlockState(),
                        new BlockState[]{oak, null, null, null},
                        0.5D
                ),
                new SablePhysicsMaterialCase(
                        "quad-step-two-after-assembler-break",
                        basePos.offset(64, 0, 0),
                        CommonBlocks.QUAD_STEP.defaultBlockState(),
                        new BlockState[]{oak, spruce, null, null},
                        1.0D
                ),
                new SablePhysicsMaterialCase(
                        "quad-step-three-after-assembler-break",
                        basePos.offset(80, 0, 0),
                        CommonBlocks.QUAD_STEP.defaultBlockState(),
                        new BlockState[]{oak, spruce, stoneBrick, null},
                        1.5D
                ),
                new SablePhysicsMaterialCase(
                        "quad-step-four-after-assembler-break",
                        basePos.offset(96, 0, 0),
                        CommonBlocks.QUAD_STEP.defaultBlockState(),
                        new BlockState[]{oak, spruce, stoneBrick, brick},
                        2.0D
                ),
                new SablePhysicsMaterialCase(
                        "single-vertical-step-after-assembler-break",
                        basePos.offset(112, 0, 0),
                        singleVerticalStep,
                        new BlockState[]{oak},
                        0.5D
                ),
                new SablePhysicsMaterialCase(
                        "quad-vertical-step-one-after-assembler-break",
                        basePos.offset(128, 0, 0),
                        CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState(),
                        new BlockState[]{oak, null, null, null},
                        0.5D
                ),
                new SablePhysicsMaterialCase(
                        "quad-vertical-step-two-after-assembler-break",
                        basePos.offset(144, 0, 0),
                        CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState(),
                        new BlockState[]{oak, spruce, null, null},
                        1.0D
                ),
                new SablePhysicsMaterialCase(
                        "quad-vertical-step-three-after-assembler-break",
                        basePos.offset(160, 0, 0),
                        CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState(),
                        new BlockState[]{oak, spruce, stoneBrick, null},
                        1.5D
                ),
                new SablePhysicsMaterialCase(
                        "quad-vertical-step-four-after-assembler-break",
                        basePos.offset(176, 0, 0),
                        CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState(),
                        new BlockState[]{oak, spruce, stoneBrick, brick},
                        2.0D
                )
        );
    }

    private static CheckResult checkSableStepCase(MutableFixtureContext context,
                                                  SableFixtureApi api,
                                                  SableStepCase stepCase) {
        ServerLevel level = context.serverLevel();
        BlockPos sourcePos = stepCase.sourcePos();
        BlockPos terrainPos = sourcePos.east();
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;
        Object subLevel = null;
        BlockPos targetPos = null;

        level.setBlock(sourcePos, stepCase.sourceState(), Block.UPDATE_ALL);
        if (stepCase.sourceSlots() == null) {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong());
            EnhancedSlabMaterialSavedData.removeClient(level, sourcePos.asLong());
        } else {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong(), stepCase.sourceSlots());
            EnhancedSlabMaterialSavedData.rememberClient(level, sourcePos.asLong(), stepCase.sourceSlots());
        }
        level.setBlock(terrainPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        details.add(stepCase.name() + ".command=/sable assemble connected " + sourcePos.toShortString() + " 64");
        try {
            LOGGER.info("BNB_FIXTURE sable-case-start name={} source={} state={}",
                    stepCase.name(), sourcePos.toShortString(), blockId(stepCase.sourceState()));

            boolean shapeConnectsTerrain = SableCompat.isEnhancedSlabAssemblyConnected(
                    level,
                    sourcePos,
                    stepCase.sourceState(),
                    terrainPos,
                    Blocks.STONE.defaultBlockState()
            );
            checked++;
            details.add(stepCase.name() + ".shape-connects-terrain=" + shapeConnectsTerrain);
            if (shapeConnectsTerrain) {
                passed = false;
            }

            SableGatherResult shapeGather = api.gatherShapeAware(level, sourcePos, 64);
            checked++;
            if (shapeGather == null) {
                passed = false;
                details.add(stepCase.name() + ".shape-gather=null");
            } else {
                details.add(stepCase.name() + ".shape-gather=" + shapeGather.state() + ":blocks=" + shapeGather.blocks().size()
                        + ":checked=" + shapeGather.checkedBlocks());
                if (!"SUCCESS".equals(shapeGather.state())
                        || shapeGather.blocks().size() != 1
                        || !shapeGather.blocks().contains(sourcePos)) {
                    passed = false;
                    details.add(stepCase.name() + ".shape-gather-blocks=unexpected:" + shapeGather.blocks());
                }
            }

            SableGatherResult gather = api.gather(level, sourcePos, 64);
            checked++;
            if (gather == null) {
                details.add(stepCase.name() + ".gather=null");
                return new CheckResult(false, checked, details);
            }
            details.add(stepCase.name() + ".gather=" + gather.state() + ":blocks=" + gather.blocks().size()
                    + ":checked=" + gather.checkedBlocks());
            if (!"SUCCESS".equals(gather.state())) {
                return new CheckResult(false, checked, details);
            }

            checked++;
            boolean gatheredOnlyStep = gather.blocks().size() == 1 && gather.blocks().contains(sourcePos);
            if (!gatheredOnlyStep) {
                passed = false;
                details.add(stepCase.name() + ".gather-blocks=unexpected:" + gather.blocks());
            }

            LOGGER.info("BNB_FIXTURE sable-case-assemble name={} source={} blocks={} bounds={}",
                    stepCase.name(), sourcePos.toShortString(), gather.blocks(), gather.boundingBox());
            subLevel = api.assemble(level, sourcePos, gather.blocks(), gather.boundingBox());
            checked++;
            if (subLevel == null) {
                details.add(stepCase.name() + ".assemble=null");
                return new CheckResult(false, checked, details);
            }
            LOGGER.info("BNB_FIXTURE sable-case-assembled name={} subLevel={} bounds={}",
                    stepCase.name(), api.describeSubLevel(subLevel), api.describeSubLevelBounds(subLevel));

            targetPos = api.centerBlock(subLevel);
            checked++;
            if (targetPos == null) {
                details.add(stepCase.name() + ".sublevel-center=null");
                return new CheckResult(false, checked, details);
            }
            details.add(stepCase.name() + ".target=" + targetPos.toShortString());
            LOGGER.info("BNB_FIXTURE sable-case-target name={} target={} state={} plot={} normal={} be={} plotBe={} bounds={}",
                    stepCase.name(),
                    targetPos.toShortString(),
                    blockId(sableAwareBlockState(level, targetPos)),
                    blockId(SableCompat.plotBlockState(level, targetPos)),
                    blockId(level.getBlockState(targetPos)),
                    blockEntityName(level.getBlockEntity(targetPos)),
                    blockEntityName(SableCompat.plotBlockEntity(level, targetPos)),
                    api.describeSubLevelBounds(subLevel));

            checked++;
            BlockState sourceAfter = level.getBlockState(sourcePos);
            if (!sourceAfter.isAir()) {
                passed = false;
                details.add(stepCase.name() + ".source-after=" + blockId(sourceAfter));
            }

            checked++;
            BlockState terrainAfter = level.getBlockState(terrainPos);
            if (!terrainAfter.is(Blocks.STONE)) {
                passed = false;
                details.add(stepCase.name() + ".terrain-after=" + blockId(terrainAfter));
            }

            checked++;
            BlockState movedState = sableAwareBlockState(level, targetPos);
            if (!movedState.is(stepCase.sourceState().getBlock())) {
                passed = false;
                details.add(stepCase.name() + ".moved-block=wrong expected:" + blockId(stepCase.sourceState())
                        + " actual:" + blockId(movedState)
                        + " plot:" + blockId(SableCompat.plotBlockState(level, targetPos))
                        + " normal:" + blockId(level.getBlockState(targetPos)));
            }

            checked++;
            BlockState[] movedSlots = EnhancedSlabMaterialSavedData.get(level, targetPos.asLong());
            if (stepCase.sourceSlots() == null) {
                if (!slotsContainMaterial(movedSlots, stepCase.expectedExistingMaterial())) {
                    passed = false;
                    details.add(stepCase.name() + ".moved-slots=missing-synthesized-existing:" + describeSlots(movedSlots));
                } else {
                    details.add(stepCase.name() + ".moved-slots=synthesized:" + describeSlots(movedSlots));
                }
            } else if (!slotsContainMaterial(movedSlots, stepCase.expectedExistingMaterial())) {
                passed = false;
                details.add(stepCase.name() + ".moved-slots=missing-existing:" + describeSlots(movedSlots));
            } else {
                details.add(stepCase.name() + ".moved-slots=" + describeSlots(movedSlots));
            }

            CheckResult placement = checkSableStepPlacement(context, api, subLevel, stepCase, targetPos);
            checked += placement.checked();
            passed &= placement.passed();
            details.addAll(placement.details());
            LOGGER.info("BNB_FIXTURE sable-case-placement name={} target={} result={} bounds={}",
                    stepCase.name(), targetPos.toShortString(), placement.details(), api.describeSubLevelBounds(subLevel));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            checked++;
            passed = false;
            details.add(stepCase.name() + ".exception=" + exception.getClass().getSimpleName());
            LOGGER.error("BNB_FIXTURE sable-case-exception name={} details={}", stepCase.name(), details, exception);
        } finally {
            if (subLevel != null) {
                LOGGER.info("BNB_FIXTURE sable-case-cleanup name={} before={}", stepCase.name(), api.describeSubLevelBounds(subLevel));
                api.remove(level, subLevel, details, stepCase.name());
                LOGGER.info("BNB_FIXTURE sable-case-cleanup name={} after={}", stepCase.name(), api.describeSubLevelBounds(subLevel));
            }
            clearFixtureBlock(level, sourcePos);
            clearFixtureBlock(level, terrainPos);
            if (targetPos != null) {
                clearFixtureMaterial(level, targetPos);
            }
        }

        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkSimAssemblerStepCase(MutableFixtureContext context,
                                                         SableFixtureApi sableApi,
                                                         SimAssemblerFixtureApi simApi,
                                                         SableStepCase stepCase) {
        ServerLevel level = context.serverLevel();
        BlockPos sourcePos = stepCase.sourcePos();
        BlockPos terrainPos = sourcePos.east();
        BlockPos assemblerPos = sourcePos.west();
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;
        Object subLevel = null;
        BlockPos targetPos = null;

        clearFixtureBlock(level, assemblerPos);
        level.setBlock(sourcePos, stepCase.sourceState(), Block.UPDATE_ALL);
        if (stepCase.sourceSlots() == null) {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong());
            EnhancedSlabMaterialSavedData.removeClient(level, sourcePos.asLong());
        } else {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong(), stepCase.sourceSlots());
            EnhancedSlabMaterialSavedData.rememberClient(level, sourcePos.asLong(), stepCase.sourceSlots());
        }
        level.setBlock(terrainPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        details.add(stepCase.name() + ".helper=SimAssemblyHelper.assembleFromSingleBlock "
                + assemblerPos.toShortString() + " " + sourcePos.toShortString());
        try {
            LOGGER.info("BNB_FIXTURE sim-assembler-case-start name={} assembler={} source={} state={}",
                    stepCase.name(), assemblerPos.toShortString(), sourcePos.toShortString(), blockId(stepCase.sourceState()));

            boolean shapeConnectsTerrain = SableCompat.isEnhancedSlabAssemblyConnected(
                    level,
                    sourcePos,
                    stepCase.sourceState(),
                    terrainPos,
                    Blocks.STONE.defaultBlockState()
            );
            checked++;
            details.add(stepCase.name() + ".shape-connects-terrain=" + shapeConnectsTerrain);
            if (shapeConnectsTerrain) {
                passed = false;
            }

            LOGGER.info("BNB_FIXTURE sim-assembler-case-assemble name={} source={}",
                    stepCase.name(), sourcePos.toShortString());
            SimAssemblyResult assembly = simApi.assemble(level, assemblerPos, sourcePos);
            checked++;
            if (assembly == null || assembly.subLevel() == null) {
                details.add(stepCase.name() + ".assemble=null");
                return new CheckResult(false, checked, details);
            }
            subLevel = assembly.subLevel();
            details.add(stepCase.name() + ".offset="
                    + (assembly.offset() == null ? "null" : assembly.offset().toShortString()));
            LOGGER.info("BNB_FIXTURE sim-assembler-case-assembled name={} subLevel={} offset={} bounds={}",
                    stepCase.name(),
                    sableApi.describeSubLevel(subLevel),
                    assembly.offset() == null ? "null" : assembly.offset().toShortString(),
                    sableApi.describeSubLevelBounds(subLevel));

            targetPos = sableApi.centerBlock(subLevel);
            checked++;
            if (targetPos == null) {
                details.add(stepCase.name() + ".sublevel-center=null");
                return new CheckResult(false, checked, details);
            }
            details.add(stepCase.name() + ".target=" + targetPos.toShortString());
            LOGGER.info("BNB_FIXTURE sim-assembler-case-target name={} target={} state={} plot={} normal={} bounds={}",
                    stepCase.name(),
                    targetPos.toShortString(),
                    blockId(sableAwareBlockState(level, targetPos)),
                    blockId(SableCompat.plotBlockState(level, targetPos)),
                    blockId(level.getBlockState(targetPos)),
                    sableApi.describeSubLevelBounds(subLevel));

            checked++;
            BlockState sourceAfter = level.getBlockState(sourcePos);
            if (!sourceAfter.isAir()) {
                passed = false;
                details.add(stepCase.name() + ".source-after=" + blockId(sourceAfter));
            }

            checked++;
            BlockState terrainAfter = level.getBlockState(terrainPos);
            if (!terrainAfter.is(Blocks.STONE)) {
                passed = false;
                details.add(stepCase.name() + ".terrain-after=" + blockId(terrainAfter));
            }

            checked++;
            BlockState movedState = sableAwareBlockState(level, targetPos);
            if (!movedState.is(stepCase.sourceState().getBlock())) {
                passed = false;
                details.add(stepCase.name() + ".moved-block=wrong expected:" + blockId(stepCase.sourceState())
                        + " actual:" + blockId(movedState)
                        + " plot:" + blockId(SableCompat.plotBlockState(level, targetPos))
                        + " normal:" + blockId(level.getBlockState(targetPos)));
            }

            checked++;
            BlockState[] movedSlots = EnhancedSlabMaterialSavedData.get(level, targetPos.asLong());
            if (stepCase.sourceSlots() == null) {
                if (!slotsContainMaterial(movedSlots, stepCase.expectedExistingMaterial())) {
                    passed = false;
                    details.add(stepCase.name() + ".moved-slots=missing-synthesized-existing:" + describeSlots(movedSlots));
                } else {
                    details.add(stepCase.name() + ".moved-slots=synthesized:" + describeSlots(movedSlots));
                }
            } else if (!slotsContainMaterial(movedSlots, stepCase.expectedExistingMaterial())) {
                passed = false;
                details.add(stepCase.name() + ".moved-slots=missing-existing:" + describeSlots(movedSlots));
            } else {
                details.add(stepCase.name() + ".moved-slots=" + describeSlots(movedSlots));
            }

            CheckResult placement = checkSableStepPlacement(context, sableApi, subLevel, stepCase, targetPos);
            checked += placement.checked();
            passed &= placement.passed();
            details.addAll(placement.details());
            LOGGER.info("BNB_FIXTURE sim-assembler-case-placement name={} target={} result={} bounds={}",
                    stepCase.name(), targetPos.toShortString(), placement.details(), sableApi.describeSubLevelBounds(subLevel));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            checked++;
            passed = false;
            details.add(stepCase.name() + ".exception=" + exception.getClass().getSimpleName());
            LOGGER.error("BNB_FIXTURE sim-assembler-case-exception name={} details={}", stepCase.name(), details, exception);
        } finally {
            if (subLevel != null) {
                LOGGER.info("BNB_FIXTURE sim-assembler-case-cleanup name={} before={}",
                        stepCase.name(), sableApi.describeSubLevelBounds(subLevel));
                sableApi.remove(level, subLevel, details, stepCase.name());
                LOGGER.info("BNB_FIXTURE sim-assembler-case-cleanup name={} after={}",
                        stepCase.name(), sableApi.describeSubLevelBounds(subLevel));
            }
            clearFixtureBlock(level, assemblerPos);
            clearFixtureBlock(level, sourcePos);
            clearFixtureBlock(level, terrainPos);
            if (targetPos != null) {
                clearFixtureMaterial(level, targetPos);
            }
        }

        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkPhysicsAssemblerStepCase(MutableFixtureContext context,
                                                             SableFixtureApi sableApi,
                                                             SimPhysicsAssemblerFixtureApi physicsAssemblerApi,
                                                             SableStepCase stepCase) {
        ServerLevel level = context.serverLevel();
        BlockPos sourcePos = stepCase.sourcePos();
        Direction assemblerDirection = fixtureAssemblerDirection(stepCase.sourceState());
        BlockPos terrainPos = sourcePos.relative(assemblerDirection.getOpposite());
        BlockPos assemblerPos = sourcePos.relative(assemblerDirection);
        List<String> details = new ArrayList<>();
        int checked = 0;
        boolean passed = true;
        Object subLevel = null;
        BlockPos stepTargetPos = null;
        Set<Object> existingSubLevels = Collections.emptySet();

        clearFixtureBlock(level, assemblerPos);
        clearFixtureBlock(level, terrainPos);
        level.setBlock(sourcePos, stepCase.sourceState(), Block.UPDATE_ALL);
        if (stepCase.sourceSlots() == null) {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong());
            EnhancedSlabMaterialSavedData.removeClient(level, sourcePos.asLong());
        } else {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong(), stepCase.sourceSlots());
            EnhancedSlabMaterialSavedData.rememberClient(level, sourcePos.asLong(), stepCase.sourceSlots());
        }
        level.setBlock(terrainPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);

        BlockState assemblerState = physicsAssemblerApi.assemblerState(assemblerDirection);
        level.setBlock(assemblerPos, assemblerState, Block.UPDATE_ALL);
        BlockEntity assemblerEntity = level.getBlockEntity(assemblerPos);
        details.add(stepCase.name() + ".real-assembler=" + assemblerPos.toShortString()
                + " source=" + sourcePos.toShortString()
                + " direction=" + assemblerDirection.getSerializedName());

        try {
            existingSubLevels = sableApi.subLevelIdentitySnapshot(level);
            LOGGER.info("BNB_FIXTURE physics-assembler-case-start name={} assembler={} source={} state={}",
                    stepCase.name(), assemblerPos.toShortString(), sourcePos.toShortString(), blockId(stepCase.sourceState()));

            checked++;
            if (assemblerEntity == null) {
                details.add(stepCase.name() + ".real-assembler-be=missing");
                return new CheckResult(false, checked, details);
            }

            boolean shapeConnectsAssembler = SableCompat.isEnhancedSlabAssemblyConnected(
                    level,
                    sourcePos,
                    stepCase.sourceState(),
                    assemblerPos,
                    assemblerState
            );
            checked++;
            details.add(stepCase.name() + ".shape-connects-assembler=" + shapeConnectsAssembler);
            if (!shapeConnectsAssembler) {
                passed = false;
            }

            boolean shapeConnectsTerrain = SableCompat.isEnhancedSlabAssemblyConnected(
                    level,
                    sourcePos,
                    stepCase.sourceState(),
                    terrainPos,
                    Blocks.STONE.defaultBlockState()
            );
            checked++;
            details.add(stepCase.name() + ".shape-connects-terrain=" + shapeConnectsTerrain);
            if (shapeConnectsTerrain) {
                passed = false;
            }

            physicsAssemblerApi.assembleOrDisassemble(assemblerEntity);
            subLevel = sableApi.findNewSubLevelContainingBlocks(
                    level,
                    existingSubLevels,
                    stepCase.sourceState().getBlock(),
                    physicsAssemblerApi.assemblerBlock()
            );
            checked++;
            if (subLevel == null) {
                details.add(stepCase.name() + ".real-assembler-sublevel=null"
                        + " sourceAfter:" + blockId(level.getBlockState(sourcePos))
                        + " assemblerAfter:" + blockId(level.getBlockState(assemblerPos)));
                return new CheckResult(false, checked, details);
            }
            LOGGER.info("BNB_FIXTURE physics-assembler-case-assembled name={} subLevel={} bounds={}",
                    stepCase.name(), sableApi.describeSubLevel(subLevel), sableApi.describeSubLevelBounds(subLevel));

            checked++;
            BlockState sourceAfter = level.getBlockState(sourcePos);
            if (!sourceAfter.isAir()) {
                passed = false;
                details.add(stepCase.name() + ".source-after=" + blockId(sourceAfter));
            }

            checked++;
            BlockState assemblerAfter = level.getBlockState(assemblerPos);
            if (!assemblerAfter.isAir()) {
                passed = false;
                details.add(stepCase.name() + ".assembler-after=" + blockId(assemblerAfter));
            }

            checked++;
            BlockState terrainAfter = level.getBlockState(terrainPos);
            if (!terrainAfter.is(Blocks.STONE)) {
                passed = false;
                details.add(stepCase.name() + ".terrain-after=" + blockId(terrainAfter));
            }

            stepTargetPos = sableApi.findBlockInPlot(subLevel, stepCase.sourceState().getBlock());
            checked++;
            if (stepTargetPos == null) {
                passed = false;
                details.add(stepCase.name() + ".sublevel-step=missing");
            } else {
                details.add(stepCase.name() + ".sublevel-step=" + stepTargetPos.toShortString());
            }

            BlockPos assemblerTargetPos = sableApi.findBlockInPlot(subLevel, physicsAssemblerApi.assemblerBlock());
            checked++;
            if (assemblerTargetPos == null) {
                passed = false;
                details.add(stepCase.name() + ".sublevel-assembler=missing");
            } else {
                details.add(stepCase.name() + ".sublevel-assembler=" + assemblerTargetPos.toShortString());
            }

            if (stepTargetPos != null) {
                checked++;
                BlockState[] movedSlots = EnhancedSlabMaterialSavedData.get(level, stepTargetPos.asLong());
                if (!slotsContainMaterial(movedSlots, stepCase.expectedExistingMaterial())) {
                    passed = false;
                    details.add(stepCase.name() + ".moved-slots=missing-existing:" + describeSlots(movedSlots));
                } else {
                    details.add(stepCase.name() + ".moved-slots=" + describeSlots(movedSlots));
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            checked++;
            passed = false;
            details.add(stepCase.name() + ".exception=" + exception.getClass().getSimpleName());
            LOGGER.error("BNB_FIXTURE physics-assembler-case-exception name={} details={}", stepCase.name(), details, exception);
        } finally {
            if (subLevel != null) {
                LOGGER.info("BNB_FIXTURE physics-assembler-case-cleanup name={} before={}",
                        stepCase.name(), sableApi.describeSubLevelBounds(subLevel));
                sableApi.remove(level, subLevel, details, stepCase.name());
                LOGGER.info("BNB_FIXTURE physics-assembler-case-cleanup name={} after={}",
                        stepCase.name(), sableApi.describeSubLevelBounds(subLevel));
            }
            clearFixtureBlock(level, assemblerPos);
            clearFixtureBlock(level, sourcePos);
            clearFixtureBlock(level, terrainPos);
            if (stepTargetPos != null) {
                clearFixtureMaterial(level, stepTargetPos);
            }
        }

        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkPhysicsAssemblerMaterialCaseDelayed(MutableFixtureContext context,
                                                                        SableFixtureApi sableApi,
                                                                        SimPhysicsAssemblerFixtureApi physicsAssemblerApi,
                                                                        SablePhysicsMaterialCase materialCase) {
        MinecraftServer server = context.serverLevel().getServer();
        if (server == null) {
            return new CheckResult(false, 1, List.of(materialCase.name() + ".server-thread=no-server"));
        }

        CompletableFuture<CheckResult> future = new CompletableFuture<>();
        server.execute(() -> {
            PhysicsMaterialProbe probe = preparePhysicsAssemblerMaterialCase(
                    context,
                    sableApi,
                    physicsAssemblerApi,
                    materialCase
            );
            if (!probe.readyForAssembly()) {
                cleanupPhysicsMaterialProbe(probe);
                long settleTarget = probe.level().getGameTime() + 10L;
                probe.details().add(materialCase.name() + ".support-setup-cleanup-settle-target-game-time="
                        + settleTarget);
                schedulePhysicsMaterialCleanupSettlement(server, probe, future, settleTarget);
                return;
            }

            long assemblyTarget = probe.level().getGameTime() + 20L;
            probe.details().add(materialCase.name() + ".support-settle-target-game-time=" + assemblyTarget);
            schedulePhysicsSupportSettlement(
                    server,
                    probe,
                    physicsAssemblerApi,
                    future,
                    assemblyTarget
            );
        });

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new CheckResult(false, 1, List.of(materialCase.name() + ".delayed-check=interrupted"));
        } catch (Exception exception) {
            return new CheckResult(false, 1, List.of(materialCase.name() + ".delayed-check="
                    + exception.getClass().getSimpleName()));
        }
    }

    private static void schedulePhysicsSupportSettlement(MinecraftServer server,
                                                         PhysicsMaterialProbe probe,
                                                         SimPhysicsAssemblerFixtureApi physicsAssemblerApi,
                                                         CompletableFuture<CheckResult> future,
                                                         long targetGameTime) {
        CompletableFuture.delayedExecutor(50L, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
            if (probe.level().getGameTime() < targetGameTime) {
                schedulePhysicsSupportSettlement(server, probe, physicsAssemblerApi, future, targetGameTime);
                return;
            }

            verifyPhysicsTerrainSupport(probe, "settled-before-assembly");
            probe.details().add(probe.materialCase().name() + ".support-settled-game-time="
                    + probe.level().getGameTime());
            assemblePhysicsAssemblerMaterialCase(probe, physicsAssemblerApi);
            if (!probe.readyForInitialStability()) {
                cleanupPhysicsMaterialProbe(probe);
                long settleTarget = probe.level().getGameTime() + 10L;
                probe.details().add(probe.materialCase().name()
                        + ".early-cleanup-settle-target-game-time=" + settleTarget);
                schedulePhysicsMaterialCleanupSettlement(server, probe, future, settleTarget);
                return;
            }

            long startGameTime = probe.level().getGameTime();
            probe.details().add(probe.materialCase().name()
                    + ".initial-calm-timeout-game-time=" + (startGameTime + 200L));
            schedulePhysicsMaterialInitialStabilityPoll(server, probe, future, startGameTime + 200L);
        }));
    }

    private static void schedulePhysicsMaterialInitialStabilityPoll(MinecraftServer server,
                                                                    PhysicsMaterialProbe probe,
                                                                    CompletableFuture<CheckResult> future,
                                                                    long timeoutGameTime) {
        CompletableFuture.delayedExecutor(50L, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
            long gameTime = probe.level().getGameTime();
            try {
                boolean registered = probe.sableApi().containsSubLevelIdentity(
                        probe.level(),
                        probe.initialSubLevel()
                );
                boolean removed = probe.sableApi().isRemoved(probe.initialSubLevel());
                SableHeatMapStatus heatMap = probe.sableApi().heatMapStatus(probe.initialSubLevel());
                if (registered && !removed && heatMap.stable()) {
                    SableBodyMotion motion = probe.sableApi().bodyMotion(
                            probe.level(),
                            probe.initialSubLevel(),
                            probe.targetPos()
                    );
                    boolean calm = motion.finite()
                            && motion.orderedBounds()
                            && Math.abs(motion.velocityY()) <= 0.10D
                            && Math.hypot(motion.velocityX(), motion.velocityZ()) <= 0.15D;
                    if (calm && probe.initialCalmSince() == 0L) {
                        probe.initialCalmSince(gameTime);
                    } else if (calm && gameTime - probe.initialCalmSince() >= 10L) {
                        probe.details().add(probe.materialCase().name() + ".initial-calm-ticks="
                                + (gameTime - probe.initialCalmSince()) + ":game-time:" + gameTime
                                + ",motion:" + motion.description());
                        breakPhysicsAssemblerAfterInitialStability(probe);
                        if (!probe.readyForDelayedCheck()) {
                            cleanupPhysicsMaterialProbe(probe);
                            long settleTarget = probe.level().getGameTime() + 10L;
                            probe.details().add(probe.materialCase().name()
                                    + ".break-failure-cleanup-settle-target-game-time=" + settleTarget);
                            schedulePhysicsMaterialCleanupSettlement(server, probe, future, settleTarget);
                            return;
                        }

                        long delayedStart = probe.level().getGameTime();
                        probe.delayedCheckStartGameTime(delayedStart);
                        probe.details().add(probe.materialCase().name()
                                + ".post-break-landing-timeout-game-time=" + (delayedStart + 120L));
                        schedulePhysicsMaterialStabilityPoll(server, probe, future, delayedStart + 120L);
                        return;
                    } else if (!calm) {
                        probe.initialCalmSince(0L);
                    }
                } else {
                    probe.initialCalmSince(0L);
                }

                if (gameTime >= timeoutGameTime) {
                    probe.failedCheck(probe.materialCase().name()
                            + ".initial-calm-timeout=registered:" + registered
                            + ",removed:" + removed + ",heatmap:" + heatMap.description());
                    cleanupPhysicsMaterialProbe(probe);
                    long settleTarget = probe.level().getGameTime() + 10L;
                    probe.details().add(probe.materialCase().name()
                            + ".initial-timeout-cleanup-settle-target-game-time=" + settleTarget);
                    schedulePhysicsMaterialCleanupSettlement(server, probe, future, settleTarget);
                    return;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                probe.failedCheck(probe.materialCase().name() + ".initial-stability-poll="
                        + exception.getClass().getSimpleName());
                cleanupPhysicsMaterialProbe(probe);
                long settleTarget = probe.level().getGameTime() + 10L;
                probe.details().add(probe.materialCase().name()
                        + ".initial-poll-error-cleanup-settle-target-game-time=" + settleTarget);
                schedulePhysicsMaterialCleanupSettlement(server, probe, future, settleTarget);
                return;
            }
            schedulePhysicsMaterialInitialStabilityPoll(server, probe, future, timeoutGameTime);
        }));
    }

    private static void schedulePhysicsMaterialStabilityPoll(MinecraftServer server,
                                                             PhysicsMaterialProbe probe,
                                                             CompletableFuture<CheckResult> future,
                                                             long timeoutGameTime) {
        CompletableFuture.delayedExecutor(50L, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
            long gameTime = probe.level().getGameTime();
            try {
                SableMaterialLocation candidate = probe.sableApi().findActiveMaterialLocation(
                        probe.level(),
                        probe.existingSubLevels(),
                        probe.materialCase().sourceState().getBlock(),
                        probe.materialCase().sourceSlots()
                );
                boolean stable = candidate != null
                        && probe.sableApi().heatMapStatus(candidate.subLevel()).stable()
                        && probe.sableApi().heatMapStatus(probe.initialSubLevel()).stable();
                // Give bodies two full seconds to settle whether they landed before or after the
                // assembler was removed. Heat-map stability is independent from physical sleep.
                if (stable && gameTime - probe.delayedCheckStartGameTime() >= 40L) {
                    if (probe.sameStableCandidate(candidate)) {
                        if (gameTime - probe.stableCandidateSince() >= 5L) {
                            SableBodyMotion motion = probe.sableApi().bodyMotion(
                                    probe.level(),
                                    candidate.subLevel(),
                                    candidate.targetPos()
                            );
                            SableTerrainContact contact = sampleLiveTerrainContact(probe.level(), motion);
                            if (contact.inContact() && probe.terrainContactSince() == 0L) {
                                probe.terrainContactSince(gameTime);
                            } else if (!contact.inContact()) {
                                probe.terrainContactSince(0L);
                            }
                            boolean sustainedContact = contact.inContact()
                                    && gameTime - probe.terrainContactSince() >= 5L;
                            boolean bouncedAtContact = contact.inContact() && motion.velocityY() > 0.02D;
                            if (motion.finite()
                                    && motion.orderedBounds()
                                    && (sustainedContact || bouncedAtContact)) {
                                probe.details().add(probe.materialCase().name()
                                        + ".post-break-contact="
                                        + (bouncedAtContact ? "bounce" : "sustained-5-ticks")
                                        + ",motion:" + motion.description());
                                finishPhysicsAssemblerMaterialCase(probe, candidate);
                                cleanupPhysicsMaterialProbe(probe);
                                long settleTarget = probe.level().getGameTime() + 10L;
                                probe.details().add(probe.materialCase().name()
                                        + ".cleanup-settle-target-game-time=" + settleTarget);
                                schedulePhysicsMaterialCleanupSettlement(server, probe, future, settleTarget);
                                return;
                            }
                        }
                    } else {
                        probe.stableCandidate(candidate, gameTime);
                    }
                } else {
                    probe.clearStableCandidate();
                }

                if (gameTime >= timeoutGameTime) {
                    appendPhysicsMaterialTransitionStatus(probe, candidate, "timeout");
                    appendPhysicsMaterialReconciliationDiagnostics(probe, "timeout");
                    appendPhysicsBodyTimeoutDiagnostics(probe, candidate);
                    probe.failedCheck(probe.materialCase().name()
                            + ".lifecycle-timeout-unsafe-to-continue=120-ticks-without-live-terrain-contact");
                    future.complete(probe.result());
                    return;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                probe.failedCheck(probe.materialCase().name() + ".stability-poll="
                        + exception.getClass().getSimpleName());
                probe.fail(probe.materialCase().name() + ".lifecycle-timeout-unsafe-to-continue=poll-error");
                future.complete(probe.result());
                return;
            }
            schedulePhysicsMaterialStabilityPoll(server, probe, future, timeoutGameTime);
        }));
    }

    private static void appendPhysicsBodyTimeoutDiagnostics(PhysicsMaterialProbe probe,
                                                            @Nullable SableMaterialLocation candidate)
            throws ReflectiveOperationException {
        Object diagnosticSubLevel = candidate == null ? probe.initialSubLevel() : candidate.subLevel();
        if (diagnosticSubLevel == null) {
            probe.details().add(probe.materialCase().name() + ".timeout-body-motion=sublevel-missing");
            return;
        }
        BlockPos diagnosticTargetPos = candidate == null ? probe.targetPos() : candidate.targetPos();
        SableBodyMotion motion = probe.sableApi().bodyMotion(
                probe.level(),
                diagnosticSubLevel,
                diagnosticTargetPos
        );
        SableTerrainContact contact = sampleLiveTerrainContact(probe.level(), motion);
        probe.details().add(probe.materialCase().name() + ".timeout-body-motion=" + motion.description()
                + ",orderedBounds:" + motion.orderedBounds()
                + ",finite:" + motion.finite()
                + ",terrainPos:" + contact.terrainPos().toShortString()
                + ",terrainState:" + blockId(contact.terrainState())
                + ",terrainTop:" + contact.terrainTop()
                + ",verticalGap:" + contact.verticalGap()
                + ",scannedColumns:" + contact.scannedColumns()
                + ",collidableTerrain:" + contact.collidableTerrain()
                + ",boundsContact:" + contact.inContact());
    }

    private static void schedulePhysicsMaterialCleanupSettlement(MinecraftServer server,
                                                                 PhysicsMaterialProbe probe,
                                                                 CompletableFuture<CheckResult> future,
                                                                 long targetGameTime) {
        CompletableFuture.delayedExecutor(50L, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
            if (probe.level().getGameTime() < targetGameTime) {
                schedulePhysicsMaterialCleanupSettlement(server, probe, future, targetGameTime);
                return;
            }
            probe.details().add(probe.materialCase().name() + ".cleanup-settled-game-time="
                    + probe.level().getGameTime());
            future.complete(probe.result());
        }));
    }

    private static PhysicsMaterialProbe preparePhysicsAssemblerMaterialCase(MutableFixtureContext context,
                                                                            SableFixtureApi sableApi,
                                                                            SimPhysicsAssemblerFixtureApi physicsAssemblerApi,
                                                                            SablePhysicsMaterialCase materialCase) {
        ServerLevel level = context.serverLevel();
        Direction assemblerDirection = fixtureAssemblerDirection(materialCase.sourceState());
        // Each delayed lifecycle case retains its physics body so Sable cannot tick a removed
        // plot later. Keep their terrain searches spatially isolated instead of stacking every
        // body around the QuickPlay player and letting later cases land on earlier fixtures.
        BlockPos requestedSupport = materialCase.sourcePos();
        if (context.serverPlayer() != null) {
            context.serverPlayer().connection.teleport(
                    requestedSupport.getX() + 0.5D,
                    requestedSupport.getY(),
                    requestedSupport.getZ() + 0.5D,
                    context.serverPlayer().getYRot(),
                    context.serverPlayer().getXRot()
            );
        }
        PhysicsTerrainSupport support = findPhysicsTerrainSupport(
                level,
                requestedSupport,
                assemblerDirection
        );
        if (support == null) {
            BlockPos requestedSource = materialCase.sourcePos();
            PhysicsMaterialProbe probe = new PhysicsMaterialProbe(
                    level,
                    sableApi,
                    materialCase,
                    physicsAssemblerApi.assemblerBlock(),
                    requestedSource.relative(assemblerDirection),
                    requestedSource,
                    requestedSource.below(2),
                    Map.of()
            );
            probe.fail(materialCase.name()
                    + ".terrain-support=missing-flat-two-block-surface-within-32-blocks-of:"
                    + requestedSource.getX() + "," + requestedSource.getZ());
            return probe;
        }
        BlockPos anchorPos = support.center();
        BlockPos sourcePos = anchorPos.above(2);
        BlockPos assemblerPos = sourcePos.relative(assemblerDirection);
        PhysicsMaterialProbe probe = new PhysicsMaterialProbe(
                level,
                sableApi,
                materialCase,
                physicsAssemblerApi.assemblerBlock(),
                assemblerPos,
                sourcePos,
                anchorPos,
                support.states()
        );

        clearFixtureBlock(level, assemblerPos);
        clearFixtureBlock(level, sourcePos);
        probe.fixtureBlocksPrepared(true);
        probe.details().add(materialCase.name() + ".terrain-support-selected=center:"
                + anchorPos.toShortString() + ",source:" + sourcePos.toShortString());
        verifyPhysicsTerrainSupport(probe, "prepared-before-settle");
        probe.readyForAssembly(probe.passed());
        return probe;
    }

    private static void assemblePhysicsAssemblerMaterialCase(PhysicsMaterialProbe probe,
                                                              SimPhysicsAssemblerFixtureApi physicsAssemblerApi) {
        ServerLevel level = probe.level();
        SableFixtureApi sableApi = probe.sableApi();
        SablePhysicsMaterialCase materialCase = probe.materialCase();
        BlockPos sourcePos = probe.sourcePos();
        BlockPos assemblerPos = probe.assemblerPos();
        Direction assemblerDirection = fixtureAssemblerDirection(materialCase.sourceState());
        int physicsRefreshBefore = SableCompat.physicsColliderRefreshCount();
        int rapierColliderBefore = SableCompat.positionAwareRapierColliderCount();

        level.setBlock(sourcePos, materialCase.sourceState(), Block.UPDATE_ALL);
        if (materialCase.enhancedMaterialCase()) {
            EnhancedSlabMaterialSavedData.remember(level, sourcePos.asLong(), materialCase.sourceSlots());
        } else {
            clearFixtureMaterial(level, sourcePos);
        }
        BlockState assemblerState = physicsAssemblerApi.assemblerState(assemblerDirection);
        level.setBlock(assemblerPos, assemblerState, Block.UPDATE_ALL);
        BlockEntity assemblerEntity = level.getBlockEntity(assemblerPos);

        try {
            Set<Object> existingSubLevels = sableApi.subLevelIdentitySnapshot(level);
            probe.existingSubLevels(existingSubLevels);
            probe.checked();
            if (assemblerEntity == null) {
                probe.fail(materialCase.name() + ".assembler-be=missing");
                return;
            }

            physicsAssemblerApi.assembleOrDisassemble(assemblerEntity);
            LOGGER.info("BNB_FIXTURE physics-material-checkpoint name={} phase=initial-assembly-call-returned gameTime={} existingSublevels={}",
                    materialCase.name(), level.getGameTime(), existingSubLevels.size());
            probe.subLevel(sableApi.findNewSubLevelContainingBlock(
                    level,
                    existingSubLevels,
                    physicsAssemblerApi.assemblerBlock()
            ));
            probe.checked();
            if (probe.subLevel() == null) {
                probe.fail(materialCase.name() + ".sublevel=missing-with-assembler");
                return;
            }
            probe.subLevelType(probe.subLevel().getClass().getName());
            probe.details().add(materialCase.name() + ".sublevel-initial=" + probe.subLevelType());
            verifyPhysicsTerrainSupport(probe, "initial");
            logPhysicsMaterialProbeCheckpoint(probe, "initial-sublevel-found");

            int physicsRefreshAfter = SableCompat.physicsColliderRefreshCount();
            if (materialCase.enhancedMaterialCase()) {
                probe.checked();
                if (physicsRefreshAfter <= physicsRefreshBefore) {
                    probe.fail(materialCase.name() + ".physics-refresh=missing");
                } else {
                    probe.details().add(materialCase.name() + ".physics-refresh="
                            + (physicsRefreshAfter - physicsRefreshBefore));
                }
            } else {
                probe.details().add(materialCase.name() + ".physics-refresh-control-delta="
                        + (physicsRefreshAfter - physicsRefreshBefore) + " (informational)");
            }

            if (materialCase.sourceState().getBlock() instanceof QuadStepBlock
                    || materialCase.sourceState().getBlock() instanceof QuadVerticalStepBlock) {
                int rapierColliderDelta = SableCompat.positionAwareRapierColliderCount() - rapierColliderBefore;
                int rapierColliderHandle = SableCompat.lastPositionAwareRapierColliderHandle();
                int rapierColliderBoxes = SableCompat.lastPositionAwareRapierColliderBoxCount();
                probe.checked(3);
                if (rapierColliderDelta <= 0) {
                    probe.fail(materialCase.name() + ".rapier-position-collider=missing");
                }
                if (rapierColliderHandle < 0) {
                    probe.fail(materialCase.name() + ".rapier-position-collider-handle=" + rapierColliderHandle);
                }
                if (rapierColliderBoxes <= 0) {
                    probe.fail(materialCase.name() + ".rapier-position-collider-boxes=" + rapierColliderBoxes);
                }
                probe.details().add(materialCase.name() + ".rapier-position-collider=delta:"
                        + rapierColliderDelta + ",handle:" + rapierColliderHandle + ",boxes:" + rapierColliderBoxes);
            }

            probe.targetPos(sableApi.findBlockInPlot(probe.subLevel(), materialCase.sourceState().getBlock()));
            probe.assemblerTargetPos(sableApi.findBlockInPlot(probe.subLevel(), physicsAssemblerApi.assemblerBlock()));
            probe.assemblerSubLevel(probe.subLevel());
            probe.checked(2);
            if (probe.assemblerTargetPos() == null) {
                probe.fail(materialCase.name() + ".plot-assembler=missing-at-initial-assembly");
                return;
            }
            if (probe.targetPos() == null) {
                probe.fail(materialCase.name() + ".plot-target=removed-at-initial-assembly");
            } else {
                probe.details().add(materialCase.name() + ".plot-target-initial=" + probe.targetPos().toShortString());
            }

            probe.plotAccessor(sableApi.embeddedPlotAccessor(probe.subLevel()));
            probe.checked();
            if (probe.plotAccessor() == null) {
                probe.fail(materialCase.name() + ".plot-accessor=missing");
                return;
            }

            BlockPos plotCenter = sableApi.centerBlock(probe.subLevel());
            probe.checked();
            if (plotCenter == null) {
                probe.fail(materialCase.name() + ".plot-center=missing");
                return;
            }
            if (probe.targetPos() != null) {
                probe.accessorPos(probe.targetPos().subtract(plotCenter));
                verifyPhysicsMaterialTarget(probe, "initial");
                if (materialCase.sourceState().getBlock() instanceof StepBlock
                        || materialCase.sourceState().getBlock() instanceof VerticalStepBlock
                        || materialCase.sourceState().getBlock() instanceof QuadStepBlock
                        || materialCase.sourceState().getBlock() instanceof QuadVerticalStepBlock) {
                    BlockState movedTargetState = probe.plotAccessor().getBlockState(probe.accessorPos());
                    SableVoxelNeighborhoodStatus neighborhood = sableApi.targetVoxelNeighborhoodState(
                            level,
                            probe.targetPos(),
                            probe.plotAccessor(),
                            probe.accessorPos(),
                            movedTargetState
                    );
                    probe.checked();
                    if (!neighborhood.nonEmpty()) {
                        probe.fail(materialCase.name() + ".target-neighborhood-state="
                                + neighborhood.description());
                    } else {
                        probe.details().add(materialCase.name() + ".target-neighborhood-state="
                                + neighborhood.description());
                    }
                }
            }

            if (materialCase.expectedSlabEquivalents() > 0.0D && probe.targetPos() != null) {
                BlockPos assemblerAccessorPos = probe.assemblerTargetPos().subtract(plotCenter);
                BlockState movedAssemblerState = probe.plotAccessor().getBlockState(assemblerAccessorPos);
                double assemblerMass = sableApi.blockMass(
                        probe.plotAccessor(),
                        assemblerAccessorPos,
                        movedAssemblerState
                );
                verifySubLevelMass(probe, "initial", probe.actualSourceMass() + assemblerMass);
            }

            probe.readyForInitialStability(true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            probe.failedCheck(materialCase.name() + ".exception=" + exception.getClass().getSimpleName());
            LOGGER.error("BNB_FIXTURE physics-material-case-exception name={} details={}",
                    materialCase.name(), probe.details(), exception);
        }

    }

    private static void breakPhysicsAssemblerAfterInitialStability(PhysicsMaterialProbe probe)
            throws ReflectiveOperationException {
        SableFixtureApi sableApi = probe.sableApi();
        SablePhysicsMaterialCase materialCase = probe.materialCase();

        probe.initialMotion(sableApi.bodyMotion(probe.level(), probe.subLevel(), probe.targetPos()));
        probe.details().add(materialCase.name() + ".body-motion-before-break="
                + probe.initialMotion().description());
        verifyPhysicsTerrainSupport(probe, "before-break");
        verifyPhysicsMaterialTarget(probe, "before-break");

        probe.physicsRefreshBeforeBreak(SableCompat.physicsColliderRefreshCount());
        probe.massRebuildBeforeBreak(SableCompat.enhancedSlabMassTrackerRebuildCount());
        int directWakeAttemptsBefore = SableCompat.enhancedSlabDirectWakeAttemptCount();
        int directWakeSuccessesBefore = SableCompat.enhancedSlabDirectWakeSuccessCount();
        logPhysicsMaterialProbeCheckpoint(probe, "before-assembler-break-stable");

        SableMaterialLocation activeAssembler = sableApi.findActiveBlockLocation(
                probe.level(),
                probe.existingSubLevels(),
                probe.assemblerBlock()
        );
        probe.checked();
        if (activeAssembler == null) {
            probe.fail(materialCase.name() + ".assembler-current-location=missing-after-heatmap-stability");
            return;
        }
        probe.assemblerSubLevel(activeAssembler.subLevel());
        probe.assemblerTargetPos(activeAssembler.targetPos());
        probe.details().add(materialCase.name() + ".assembler-current-location=identity:"
                + Integer.toHexString(System.identityHashCode(activeAssembler.subLevel()))
                + ",initial:" + (activeAssembler.subLevel() == probe.initialSubLevel())
                + ",pos:" + activeAssembler.targetPos().toShortString()
                + ",bounds:" + sableApi.describeSubLevelBounds(activeAssembler.subLevel()));

        boolean assemblerBroken = SableCompat.setPlotBlockState(
                probe.level(),
                probe.assemblerTargetPos(),
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
        );
        probe.checked();
        if (!assemblerBroken) {
            probe.fail(materialCase.name() + ".assembler-break=failed");
            return;
        }
        int directWakeAttempts = SableCompat.enhancedSlabDirectWakeAttemptCount() - directWakeAttemptsBefore;
        int directWakeSuccesses = SableCompat.enhancedSlabDirectWakeSuccessCount() - directWakeSuccessesBefore;
        probe.details().add(materialCase.name() + ".post-break-direct-wake=attempts:"
                + directWakeAttempts + ",successes:" + directWakeSuccesses);
        if (materialCase.enhancedMaterialCase()
                && probe.assemblerSubLevel() == probe.initialSubLevel()) {
            probe.checked(2);
            if (directWakeAttempts <= 0) {
                probe.fail(materialCase.name() + ".post-break-direct-wake-handler=not-fired");
            }
            if (directWakeSuccesses <= 0) {
                probe.fail(materialCase.name() + ".post-break-direct-wake-call=not-completed");
            }
        } else if (materialCase.enhancedMaterialCase()) {
            probe.details().add(materialCase.name()
                    + ".post-break-direct-wake=split-assembler-in-separate-body; retained-target-wake-validated-by-motion");
        }
        logPhysicsMaterialProbeCheckpoint(probe, "after-assembler-break-call-returned");

        probe.checked();
        if (!sableApi.plotBlockState(probe.assemblerSubLevel(), probe.assemblerTargetPos()).isAir()) {
            probe.fail(materialCase.name() + ".assembler-still-present-immediate");
            return;
        }

        if (probe.targetPos() != null) {
            verifyPhysicsMaterialTarget(probe, "immediate-after-break");
            if (materialCase.expectedSlabEquivalents() > 0.0D) {
                verifySubLevelMass(probe, "immediate-after-break", probe.actualSourceMass());
            }
        }
        probe.readyForDelayedCheck(true);
    }

    private static void logPhysicsMaterialProbeCheckpoint(PhysicsMaterialProbe probe, String phase) {
        Object subLevel = probe.subLevel();
        if (subLevel == null) {
            LOGGER.info("BNB_FIXTURE physics-material-checkpoint name={} phase={} gameTime={} sublevel=missing details={}",
                    probe.materialCase().name(), phase, probe.level().getGameTime(), probe.details());
            return;
        }
        try {
            boolean registered = probe.sableApi().containsSubLevelIdentity(probe.level(), subLevel);
            boolean removed = probe.sableApi().isRemoved(subLevel);
            SableHeatMapStatus heatMap = probe.sableApi().heatMapStatus(subLevel);
            BlockState targetState = probe.targetPos() == null
                    ? Blocks.AIR.defaultBlockState()
                    : probe.sableApi().plotBlockState(subLevel, probe.targetPos());
            BlockState assemblerState = probe.assemblerTargetPos() == null
                    ? Blocks.AIR.defaultBlockState()
                    : probe.sableApi().plotBlockState(probe.assemblerSubLevel(), probe.assemblerTargetPos());
            LOGGER.info("BNB_FIXTURE physics-material-checkpoint name={} phase={} gameTime={} identity={} registered={} removed={} heatmap={} targetPos={} targetState={} assemblerIdentity={} assemblerPos={} assemblerState={} bounds={} details={}",
                    probe.materialCase().name(),
                    phase,
                    probe.level().getGameTime(),
                    Integer.toHexString(System.identityHashCode(subLevel)),
                    registered,
                    removed,
                    heatMap.description(),
                    probe.targetPos() == null ? "missing" : probe.targetPos().toShortString(),
                    blockId(targetState),
                    probe.assemblerSubLevel() == null
                            ? "missing"
                            : Integer.toHexString(System.identityHashCode(probe.assemblerSubLevel())),
                    probe.assemblerTargetPos() == null ? "missing" : probe.assemblerTargetPos().toShortString(),
                    blockId(assemblerState),
                    probe.sableApi().describeSubLevelBounds(subLevel),
                    probe.details());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            LOGGER.warn("BNB_FIXTURE physics-material-checkpoint name={} phase={} gameTime={} diagnostics={}",
                    probe.materialCase().name(), phase, probe.level().getGameTime(),
                    exception.getClass().getSimpleName());
        }
    }

    private static void finishPhysicsAssemblerMaterialCase(PhysicsMaterialProbe probe,
                                                           SableMaterialLocation successor)
            throws ReflectiveOperationException {
        SablePhysicsMaterialCase materialCase = probe.materialCase();
        SableFixtureApi sableApi = probe.sableApi();
        Object subLevel = probe.subLevel();

        long elapsedGameTime = probe.level().getGameTime() - probe.delayedCheckStartGameTime();
        probe.checked();
        if (elapsedGameTime < 40L) {
            probe.fail(materialCase.name() + ".delayed-check-elapsed-ticks=" + elapsedGameTime);
        } else {
            probe.details().add(materialCase.name() + ".delayed-check-elapsed-ticks=" + elapsedGameTime);
        }

        appendPhysicsMaterialTransitionStatus(probe, successor, "stable");
        probe.checked();
        Object initialSubLevel = probe.initialSubLevel();
        subLevel = successor.subLevel();
        BlockPos delayedTargetPos = successor.targetPos();
        probe.subLevel(subLevel);
        probe.targetPos(delayedTargetPos);
        probe.plotAccessor(sableApi.embeddedPlotAccessor(subLevel));
        String delayedType = subLevel.getClass().getName();
        boolean registered = sableApi.containsSubLevelIdentity(probe.level(), subLevel);
        boolean removed = sableApi.isRemoved(subLevel);
        probe.checked(2);
        if (!registered || removed) {
            probe.fail(materialCase.name() + ".successor-survival=registered:" + registered + ",removed:" + removed);
        }
        probe.details().add(materialCase.name() + ".successor="
                + (subLevel == initialSubLevel ? "same" : "replacement")
                + ",type:" + delayedType
                + ",target:" + delayedTargetPos.toShortString());

        BlockPos plotCenter = sableApi.centerBlock(subLevel);
        if (plotCenter == null || probe.plotAccessor() == null) {
            probe.failedCheck(materialCase.name() + ".delayed-plot-center=missing");
        } else {
            probe.accessorPos(delayedTargetPos.subtract(plotCenter));
            verifyPhysicsMaterialTarget(probe, "delayed-after-break");
            if (materialCase.expectedSlabEquivalents() > 0.0D) {
                verifySubLevelMass(probe, "delayed-after-break", probe.actualSourceMass());
            }
        }

        BlockPos delayedAssembler = sableApi.findBlockInPlot(subLevel, probe.assemblerBlock());
        probe.checked();
        if (delayedAssembler != null) {
            probe.fail(materialCase.name() + ".assembler-reappeared-after-stability=" + delayedAssembler.toShortString());
        }

        verifyPhysicsTerrainSupport(probe, "delayed-after-break");
        SableBodyMotion delayedMotion = sableApi.bodyMotion(probe.level(), subLevel, delayedTargetPos);
        probe.checked(3);
        SableTerrainContact contact = sampleLiveTerrainContact(probe.level(), delayedMotion);
        boolean finiteMotion = delayedMotion.finite();
        if (!finiteMotion) {
            probe.fail(materialCase.name() + ".delayed-body-motion=non-finite:"
                    + delayedMotion.description());
        }
        if (!delayedMotion.orderedBounds()) {
            probe.fail(materialCase.name() + ".delayed-body-bounds=unordered:" + delayedMotion.description());
        }
        if (!contact.collidableTerrain()) {
            probe.fail(materialCase.name() + ".delayed-body-terrain=non-collidable-at:"
                    + contact.terrainPos().toShortString() + ",state:" + blockId(contact.terrainState()));
        }
        if (!contact.inContact()) {
            probe.fail(materialCase.name() + ".delayed-body-bounds=outside-live-terrain-range terrainTop:"
                    + contact.terrainTop() + ",terrainPos:" + contact.terrainPos().toShortString()
                    + ",minY:" + delayedMotion.minY() + ",verticalGap:" + contact.verticalGap()
                    + ",scannedColumns:" + contact.scannedColumns()
                    + ",motion:" + delayedMotion.description());
        } else {
            probe.details().add(materialCase.name() + ".delayed-body-motion=resting-on-live-terrain terrainTop:"
                    + contact.terrainTop() + ",terrainPos:" + contact.terrainPos().toShortString()
                    + ",state:" + blockId(contact.terrainState()) + ",verticalGap:" + contact.verticalGap()
                    + ",scannedColumns:" + contact.scannedColumns()
                    + ",motion:" + delayedMotion.description());
        }

        int refreshDelta = SableCompat.physicsColliderRefreshCount() - probe.physicsRefreshBeforeBreak();
        probe.details().add(materialCase.name() + ".post-break-physics-refresh=" + refreshDelta
                + " (informational; assembler removal need not invoke the enhanced-block refresh hook)");
        appendPhysicsMaterialReconciliationDiagnostics(probe, "post-break");

        probe.details().add(materialCase.name() + ".after-break="
                + (probe.passed() ? "survived-initial-and-delayed-checks" : "failed")
                + ":bounds=" + sableApi.describeSubLevelBounds(subLevel));
        LOGGER.info("BNB_FIXTURE physics-material-delayed-case name={} target={} passed={} details={}",
                materialCase.name(),
                probe.targetPos() == null ? "missing" : probe.targetPos().toShortString(),
                probe.passed(),
                probe.details());
    }

    private static void appendPhysicsMaterialReconciliationDiagnostics(PhysicsMaterialProbe probe,
                                                                        String phase) {
        int massRebuildDelta = SableCompat.enhancedSlabMassTrackerRebuildCount()
                - probe.massRebuildBeforeBreak();
        probe.details().add(probe.materialCase().name() + "." + phase
                + "-retained-mass-rebuilds=" + massRebuildDelta);
    }

    private static SableTerrainContact sampleLiveTerrainContact(ServerLevel level, SableBodyMotion motion) {
        int minX = (int) Math.floor(motion.minX());
        int maxX = (int) Math.floor(Math.max(motion.minX(), motion.maxX() - 1.0E-7D));
        int minZ = (int) Math.floor(motion.minZ());
        int maxZ = (int) Math.floor(Math.max(motion.minZ(), motion.maxZ() - 1.0E-7D));
        SableTerrainContact closest = null;
        SableTerrainContact closestContact = null;
        int scannedColumns = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                scannedColumns++;
                int firstAirY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos terrainPos = new BlockPos(x, firstAirY - 1, z);
                BlockState terrainState = level.getBlockState(terrainPos);
                VoxelShape collision = terrainState.getCollisionShape(
                        level,
                        terrainPos,
                        CollisionContext.empty()
                );
                if (collision.isEmpty()) {
                    continue;
                }

                for (AABB terrainBox : collision.toAabbs()) {
                    double terrainMinX = terrainPos.getX() + terrainBox.minX;
                    double terrainMaxX = terrainPos.getX() + terrainBox.maxX;
                    double terrainMinZ = terrainPos.getZ() + terrainBox.minZ;
                    double terrainMaxZ = terrainPos.getZ() + terrainBox.maxZ;
                    double colliderBottom = motion.colliderGeometry().lowestYOverFootprint(
                            terrainMinX,
                            terrainMaxX,
                            terrainMinZ,
                            terrainMaxZ
                    );
                    if (!Double.isFinite(colliderBottom)) {
                        continue;
                    }

                    double terrainTop = terrainPos.getY() + terrainBox.maxY;
                    double verticalGap = colliderBottom - terrainTop;
                    boolean inContact = verticalGap >= -0.15D && verticalGap <= 0.35D;
                    SableTerrainContact candidate = new SableTerrainContact(
                            terrainPos,
                            terrainState,
                            terrainTop,
                            verticalGap,
                            true,
                            inContact,
                            scannedColumns
                    );
                    if (closest == null || Math.abs(verticalGap) < Math.abs(closest.verticalGap())) {
                        closest = candidate;
                    }
                    if (inContact && (closestContact == null
                            || Math.abs(verticalGap) < Math.abs(closestContact.verticalGap()))) {
                        closestContact = candidate;
                    }
                }
            }
        }

        SableTerrainContact selected = closestContact == null ? closest : closestContact;
        if (selected != null) {
            return new SableTerrainContact(
                    selected.terrainPos(),
                    selected.terrainState(),
                    selected.terrainTop(),
                    selected.verticalGap(),
                    true,
                    selected.inContact(),
                    scannedColumns
            );
        }

        int centerX = (int) Math.floor((motion.minX() + motion.maxX()) * 0.5D);
        int centerZ = (int) Math.floor((motion.minZ() + motion.maxZ()) * 0.5D);
        int firstAirY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
        BlockPos terrainPos = new BlockPos(centerX, firstAirY - 1, centerZ);
        return new SableTerrainContact(
                terrainPos,
                level.getBlockState(terrainPos),
                Double.NaN,
                Double.NaN,
                false,
                false,
                scannedColumns
        );
    }

    private static void appendPhysicsMaterialTransitionStatus(PhysicsMaterialProbe probe,
                                                              @Nullable SableMaterialLocation candidate,
                                                              String phase) throws ReflectiveOperationException {
        Object original = probe.initialSubLevel();
        boolean originalRegistered = probe.sableApi().containsSubLevelIdentity(probe.level(), original);
        boolean originalRemoved = probe.sableApi().isRemoved(original);
        SableHeatMapStatus originalHeatMap = probe.sableApi().heatMapStatus(original);
        probe.details().add(probe.materialCase().name() + "." + phase + "-original=registered:"
                + originalRegistered + ",removed:" + originalRemoved + ",heatmap:" + originalHeatMap.description());
        if (candidate == null) {
            probe.details().add(probe.materialCase().name() + "." + phase + "-successor=missing");
            return;
        }
        boolean candidateRegistered = probe.sableApi().containsSubLevelIdentity(probe.level(), candidate.subLevel());
        boolean candidateRemoved = probe.sableApi().isRemoved(candidate.subLevel());
        SableHeatMapStatus candidateHeatMap = probe.sableApi().heatMapStatus(candidate.subLevel());
        probe.details().add(probe.materialCase().name() + "." + phase + "-successor=registered:"
                + candidateRegistered + ",removed:" + candidateRemoved + ",target:"
                + candidate.targetPos().toShortString() + ",heatmap:" + candidateHeatMap.description());
    }

    private static void verifyPhysicsMaterialTarget(PhysicsMaterialProbe probe, String phase)
            throws ReflectiveOperationException {
        SablePhysicsMaterialCase materialCase = probe.materialCase();
        BlockGetter plotAccessor = probe.plotAccessor();
        BlockPos accessorPos = probe.accessorPos();
        BlockState movedState = probe.sableApi().plotBlockState(probe.subLevel(), probe.targetPos());
        VoxelShape collision = movedState.getCollisionShape(plotAccessor, accessorPos, CollisionContext.empty());
        probe.checked(2);
        if (!movedState.is(materialCase.sourceState().getBlock())) {
            probe.fail(materialCase.name() + "." + phase + "-state=" + blockId(movedState));
        }
        if (collision.isEmpty()) {
            probe.fail(materialCase.name() + "." + phase + "-collision=empty");
        }

        if (materialCase.enhancedMaterialCase()) {
            BlockState[] slots = SableCompat.enhancedSlabMaterials(plotAccessor, accessorPos);
            probe.checked();
            if (!sameMaterialSlots(materialCase.sourceSlots(), slots)) {
                probe.fail(materialCase.name() + "." + phase + "-render-data=" + describeSlots(slots));
            } else {
                probe.details().add(materialCase.name() + "." + phase + "-render-data=" + describeSlots(slots));
            }
        } else {
            probe.details().add(materialCase.name() + "." + phase
                    + "-render-data=not-applicable-vanilla-control");
        }

        int occupiedSlots = materialSlotCount(materialCase.sourceSlots());
        boolean quadStep = movedState.getBlock() instanceof QuadStepBlock
                || movedState.getBlock() instanceof QuadVerticalStepBlock;
        if (quadStep && occupiedSlots >= 2) {
            VoxelShape expectedEntityCollision = expectedPhysicsEntityCollisionShape(
                    movedState,
                    materialCase.sourceSlots()
            );
            VoxelShape actualEntityCollision = probe.sableApi().subLevelEntityCollisionShape(
                    probe.level(),
                    probe.targetPos(),
                    movedState
            );
            boolean dynamicShape = movedState.getBlock().hasDynamicShape();
            boolean entityCollisionMatches = !actualEntityCollision.isEmpty()
                    && fixtureShapesEqual(expectedEntityCollision, actualEntityCollision);
            probe.checked(2);
            if (!dynamicShape) {
                probe.fail(materialCase.name() + "." + phase + "-entity-collision=state-shape-cache-enabled");
            }
            if (!entityCollisionMatches) {
                probe.fail(materialCase.name() + "." + phase + "-entity-collision=actual:"
                        + actualEntityCollision.toAabbs() + ",expected:" + expectedEntityCollision.toAabbs());
            } else {
                probe.details().add(materialCase.name() + "." + phase + "-entity-collision=slots:"
                        + occupiedSlots + ",boxes:" + actualEntityCollision.toAabbs());
            }
        }

        if (materialCase.expectedSlabEquivalents() > 0.0D) {
            double actualMass = probe.sableApi().blockMass(plotAccessor, accessorPos, movedState);
            double expectedMass = 0.0D;
            for (BlockState slot : materialCase.sourceSlots()) {
                if (slot != null) {
                    expectedMass += probe.sableApi().blockMass(plotAccessor, accessorPos, slot) * 0.5D;
                }
            }
            boolean massMatches = actualMass > 0.0D
                    && expectedMass > 0.0D
                    && Math.abs(actualMass - expectedMass) <= Math.max(1.0E-6D, expectedMass * 1.0E-6D);
            probe.checked();
            if (!massMatches) {
                probe.fail(materialCase.name() + "." + phase + "-mass=actual:" + actualMass
                        + ",expected:" + expectedMass
                        + ",slab-equivalents:" + materialCase.expectedSlabEquivalents());
            } else {
                probe.details().add(materialCase.name() + "." + phase + "-mass=" + actualMass
                        + ",expected:" + expectedMass);
            }
            probe.actualSourceMass(actualMass);
        }
    }

    /**
     * Independent geometry oracle for Sable's two-argument entity-collision query. Quad occupancy
     * lives outside the owner BlockState, so this must prove every occupied quarter survives the
     * state-shape cache boundary without calling the production occupied-shape helpers.
     */
    private static VoxelShape expectedPhysicsEntityCollisionShape(BlockState ownerState,
                                                                  @Nullable BlockState[] slots) {
        VoxelShape shape = Shapes.empty();
        if (slots == null) {
            return shape;
        }

        boolean horizontal = ownerState.getBlock() instanceof QuadStepBlock;
        boolean vertical = ownerState.getBlock() instanceof QuadVerticalStepBlock;
        if (!horizontal && !vertical) {
            return shape;
        }

        Direction.Axis axis = horizontal && ownerState.hasProperty(QuadStepBlock.AXIS)
                ? ownerState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        for (int index = 0; index < Math.min(4, slots.length); index++) {
            if (slots[index] == null) {
                continue;
            }

            boolean positiveHorizontal = index == 1 || index == 3;
            boolean positiveVertical = index >= 2;
            VoxelShape quarter;
            if (horizontal && axis == Direction.Axis.Z) {
                quarter = Block.box(
                        0.0D,
                        positiveVertical ? 8.0D : 0.0D,
                        positiveHorizontal ? 8.0D : 0.0D,
                        16.0D,
                        positiveVertical ? 16.0D : 8.0D,
                        positiveHorizontal ? 16.0D : 8.0D
                );
            } else if (horizontal) {
                quarter = Block.box(
                        positiveHorizontal ? 8.0D : 0.0D,
                        positiveVertical ? 8.0D : 0.0D,
                        0.0D,
                        positiveHorizontal ? 16.0D : 8.0D,
                        positiveVertical ? 16.0D : 8.0D,
                        16.0D
                );
            } else {
                quarter = Block.box(
                        positiveHorizontal ? 8.0D : 0.0D,
                        0.0D,
                        positiveVertical ? 8.0D : 0.0D,
                        positiveHorizontal ? 16.0D : 8.0D,
                        16.0D,
                        positiveVertical ? 16.0D : 8.0D
                );
            }
            shape = Shapes.or(shape, quarter);
        }
        return shape;
    }

    private static void verifySubLevelMass(PhysicsMaterialProbe probe, String phase, double expectedMass)
            throws ReflectiveOperationException {
        double selfMass = probe.sableApi().subLevelMass(probe.subLevel(), "getSelfMassTracker");
        double mergedMass = probe.sableApi().subLevelMass(probe.subLevel(), "getMassTracker");
        boolean selfMassMatches = selfMass > 0.0D
                && expectedMass > 0.0D
                && Math.abs(selfMass - expectedMass) <= Math.max(1.0E-6D, expectedMass * 1.0E-6D);
        boolean mergedMassMatches = mergedMass > 0.0D
                && expectedMass > 0.0D
                && Math.abs(mergedMass - expectedMass) <= Math.max(1.0E-6D, expectedMass * 1.0E-6D);
        probe.checked(2);
        if (!selfMassMatches) {
            probe.fail(probe.materialCase().name() + "." + phase + "-sublevel-self-mass=actual:" + selfMass
                    + ",expected:" + expectedMass);
        } else {
            probe.details().add(probe.materialCase().name() + "." + phase + "-sublevel-self-mass=" + selfMass
                    + ",expected:" + expectedMass);
        }
        if (!mergedMassMatches) {
            probe.fail(probe.materialCase().name() + "." + phase + "-sublevel-merged-mass=actual:" + mergedMass
                    + ",expected:" + expectedMass);
        } else {
            probe.details().add(probe.materialCase().name() + "." + phase + "-sublevel-merged-mass=" + mergedMass
                    + ",expected:" + expectedMass);
        }

        BlockState sourceState = probe.materialCase().sourceState();
        boolean stepFamily = sourceState.getBlock() instanceof StepBlock
                || sourceState.getBlock() instanceof VerticalStepBlock
                || sourceState.getBlock() instanceof QuadStepBlock
                || sourceState.getBlock() instanceof QuadVerticalStepBlock;
        if (!phase.equals("initial") && stepFamily) {
            SableExpectedMassProperties expectedProperties = expectedStepMassProperties(probe);
            if (!phase.equals("immediate-after-break")) {
                expectedProperties = withSableLoneStepBuoyancyCenter(
                        probe.sableApi(),
                        probe.subLevel(),
                        sourceState,
                        expectedProperties
                );
            }
            double[] actualCenter = probe.sableApi().subLevelCenter(probe.subLevel(), "getSelfMassTracker");
            probe.checked(2);
            if (expectedProperties == null) {
                probe.fail(probe.materialCase().name() + "." + phase + "-sublevel-center=expected-unavailable");
                probe.fail(probe.materialCase().name() + "." + phase + "-sublevel-inertia=expected-unavailable");
            } else {
                Vec3 localCenter = expectedProperties.center();
                double expectedX = probe.targetPos().getX() + localCenter.x;
                double expectedY = probe.targetPos().getY() + localCenter.y;
                double expectedZ = probe.targetPos().getZ() + localCenter.z;
                double tolerance = 1.0E-5D;
                boolean centerMatches = Math.abs(actualCenter[0] - expectedX) <= tolerance
                        && Math.abs(actualCenter[1] - expectedY) <= tolerance
                        && Math.abs(actualCenter[2] - expectedZ) <= tolerance;
                if (!centerMatches) {
                    probe.fail(probe.materialCase().name() + "." + phase + "-sublevel-center=actual:"
                            + actualCenter[0] + "," + actualCenter[1] + "," + actualCenter[2]
                            + ",expected:" + expectedX + "," + expectedY + "," + expectedZ);
                } else {
                    probe.details().add(probe.materialCase().name() + "." + phase + "-sublevel-center="
                            + actualCenter[0] + "," + actualCenter[1] + "," + actualCenter[2]
                            + ",expected:" + expectedX + "," + expectedY + "," + expectedZ);
                }

                double[] actualInertia = probe.sableApi().subLevelInertia(
                        probe.subLevel(),
                        "getSelfMassTracker"
                );
                double[] expectedInertia = expectedProperties.inertia();
                double largestExpected = 0.0D;
                for (double component : expectedInertia) {
                    largestExpected = Math.max(largestExpected, Math.abs(component));
                }
                double inertiaTolerance = Math.max(1.0E-7D, largestExpected * 1.0E-6D);
                boolean inertiaMatches = actualInertia.length == expectedInertia.length;
                for (int index = 0; inertiaMatches && index < expectedInertia.length; index++) {
                    inertiaMatches = Double.isFinite(actualInertia[index])
                            && Math.abs(actualInertia[index] - expectedInertia[index]) <= inertiaTolerance;
                }
                if (!inertiaMatches) {
                    probe.fail(probe.materialCase().name() + "." + phase + "-sublevel-inertia=actual:"
                            + describeMatrix(actualInertia) + ",expected:" + describeMatrix(expectedInertia)
                            + ",tolerance:" + inertiaTolerance);
                } else {
                    probe.details().add(probe.materialCase().name() + "." + phase + "-sublevel-inertia="
                            + describeMatrix(actualInertia) + ",expected:" + describeMatrix(expectedInertia));
                }
            }
        }
    }

    /**
     * Independent mass-properties oracle for the fixed quarter geometry used by the runtime cases.
     * The fixture deliberately does not call the production Sable center/inertia helpers here.
     */
    private static @Nullable SableExpectedMassProperties expectedStepMassProperties(PhysicsMaterialProbe probe)
            throws ReflectiveOperationException {
        return expectedStepMassProperties(
                probe.sableApi(),
                probe.plotAccessor(),
                probe.accessorPos(),
                probe.materialCase().sourceState(),
                probe.materialCase().sourceSlots()
        );
    }

    /** Mirrors the one-voxel Step center used to neutralize Sable's block-centered buoyancy arm. */
    private static @Nullable SableExpectedMassProperties withSableLoneStepBuoyancyCenter(
            SableFixtureApi sableApi,
            Object subLevel,
            BlockState ownerState,
            @Nullable SableExpectedMassProperties expected) throws ReflectiveOperationException {
        if (expected != null
                && (ownerState.getBlock() instanceof StepBlock
                || ownerState.getBlock() instanceof VerticalStepBlock)
                && sableApi.isSingleVoxelBody(subLevel)) {
            return new SableExpectedMassProperties(
                    expected.mass(),
                    new Vec3(0.5D, 0.5D, 0.5D),
                    expected.inertia()
            );
        }
        return expected;
    }

    private static @Nullable SableExpectedMassProperties expectedStepMassProperties(SableFixtureApi sableApi,
                                                                                     BlockGetter plotAccessor,
                                                                                     BlockPos accessorPos,
                                                                                     BlockState ownerState,
                                                                                     @Nullable BlockState[] slots)
            throws ReflectiveOperationException {
        boolean horizontalQuad = ownerState.getBlock() instanceof QuadStepBlock;
        boolean verticalQuad = ownerState.getBlock() instanceof QuadVerticalStepBlock;
        boolean horizontalSingle = ownerState.getBlock() instanceof StepBlock;
        boolean verticalSingle = ownerState.getBlock() instanceof VerticalStepBlock;
        if ((!horizontalQuad && !verticalQuad && !horizontalSingle && !verticalSingle) || slots == null) {
            return null;
        }

        Direction.Axis axis = horizontalQuad && ownerState.hasProperty(QuadStepBlock.AXIS)
                ? ownerState.getValue(QuadStepBlock.AXIS)
                : Direction.Axis.X;
        List<SableExpectedMassElement> elements = new ArrayList<>();
        int maximumSlots = horizontalQuad || verticalQuad ? Math.min(4, slots.length) : Math.min(1, slots.length);
        for (int index = 0; index < maximumSlots; index++) {
            BlockState sourceSlab = slots[index];
            if (sourceSlab == null) {
                continue;
            }
            double slotMass = sableApi.blockMass(plotAccessor, accessorPos, sourceSlab) * 0.5D;
            if (!(slotMass > 0.0D) || !Double.isFinite(slotMass)) {
                continue;
            }

            if (horizontalQuad) {
                boolean positiveHorizontal = index == 1 || index == 3;
                elements.add(new SableExpectedMassElement(
                        slotMass,
                        axis == Direction.Axis.X ? (positiveHorizontal ? 0.75D : 0.25D) : 0.5D,
                        index >= 2 ? 0.75D : 0.25D,
                        axis == Direction.Axis.Z ? (positiveHorizontal ? 0.75D : 0.25D) : 0.5D,
                        axis == Direction.Axis.X ? 0.5D : 1.0D,
                        0.5D,
                        axis == Direction.Axis.Z ? 0.5D : 1.0D
                ));
            } else if (verticalQuad) {
                elements.add(new SableExpectedMassElement(
                        slotMass,
                        index == 1 || index == 3 ? 0.75D : 0.25D,
                        0.5D,
                        index >= 2 ? 0.75D : 0.25D,
                        0.5D,
                        1.0D,
                        0.5D
                ));
            } else if (horizontalSingle) {
                Direction facing = ownerState.getValue(StepBlock.FACING);
                boolean top = ownerState.getValue(StepBlock.TOP);
                boolean alongX = facing.getAxis() == Direction.Axis.X;
                elements.add(new SableExpectedMassElement(
                        slotMass,
                        alongX ? (facing == Direction.EAST ? 0.75D : 0.25D) : 0.5D,
                        top ? 0.75D : 0.25D,
                        alongX ? 0.5D : (facing == Direction.SOUTH ? 0.75D : 0.25D),
                        alongX ? 0.5D : 1.0D,
                        0.5D,
                        alongX ? 1.0D : 0.5D
                ));
            } else {
                Direction facing = ownerState.getValue(VerticalStepBlock.FACING);
                Direction side = ownerState.getValue(VerticalStepBlock.SIDE);
                elements.add(new SableExpectedMassElement(
                        slotMass,
                        side == Direction.EAST ? 0.75D : 0.25D,
                        0.5D,
                        facing == Direction.SOUTH ? 0.75D : 0.25D,
                        0.5D,
                        1.0D,
                        0.5D
                ));
            }
        }

        double weightedX = 0.0D;
        double weightedY = 0.0D;
        double weightedZ = 0.0D;
        double totalMass = 0.0D;
        for (SableExpectedMassElement element : elements) {
            weightedX += element.centerX() * element.mass();
            weightedY += element.centerY() * element.mass();
            weightedZ += element.centerZ() * element.mass();
            totalMass += element.mass();
        }
        if (!(totalMass > 0.0D)) {
            return null;
        }

        Vec3 center = new Vec3(weightedX / totalMass, weightedY / totalMass, weightedZ / totalMass);
        double[] inertia = new double[9];
        for (SableExpectedMassElement element : elements) {
            double mass = element.mass();
            double deltaX = element.centerX() - center.x;
            double deltaY = element.centerY() - center.y;
            double deltaZ = element.centerZ() - center.z;
            double intrinsicX = mass * (element.sizeY() * element.sizeY()
                    + element.sizeZ() * element.sizeZ()) / 12.0D;
            double intrinsicY = mass * (element.sizeX() * element.sizeX()
                    + element.sizeZ() * element.sizeZ()) / 12.0D;
            double intrinsicZ = mass * (element.sizeX() * element.sizeX()
                    + element.sizeY() * element.sizeY()) / 12.0D;
            inertia[0] += intrinsicX + mass * (deltaY * deltaY + deltaZ * deltaZ);
            inertia[4] += intrinsicY + mass * (deltaX * deltaX + deltaZ * deltaZ);
            inertia[8] += intrinsicZ + mass * (deltaX * deltaX + deltaY * deltaY);
            inertia[1] -= mass * deltaX * deltaY;
            inertia[3] -= mass * deltaX * deltaY;
            inertia[2] -= mass * deltaX * deltaZ;
            inertia[6] -= mass * deltaX * deltaZ;
            inertia[5] -= mass * deltaY * deltaZ;
            inertia[7] -= mass * deltaY * deltaZ;
        }
        return new SableExpectedMassProperties(totalMass, center, inertia);
    }

    /** Independent position-aware oracle for a live plot Step before or after partial breaking. */
    private static CheckResult checkSableLiveStepMassProperties(SableFixtureApi sableApi,
                                                                 ServerLevel level,
                                                                 Object subLevel,
                                                                 BlockPos targetPos,
                                                                 BlockState ownerState,
                                                                 @Nullable BlockState[] slots,
                                                                 String detailPrefix,
                                                                 String phase) {
        List<String> details = new ArrayList<>();
        try {
            BlockGetter plotAccessor = sableApi.embeddedPlotAccessor(subLevel);
            BlockPos plotCenter = sableApi.centerBlock(subLevel);
            if (plotAccessor == null || plotCenter == null) {
                return new CheckResult(false, 7, List.of(detailPrefix + "." + phase
                        + "-mass-properties=plot-accessor-unavailable"));
            }
            BlockPos accessorPos = targetPos.subtract(plotCenter);
            SableExpectedMassProperties expected = expectedStepMassProperties(
                    sableApi,
                    plotAccessor,
                    accessorPos,
                    ownerState,
                    slots
            );
            expected = withSableLoneStepBuoyancyCenter(sableApi, subLevel, ownerState, expected);
            if (expected == null) {
                return new CheckResult(false, 7, List.of(detailPrefix + "." + phase
                        + "-mass-properties=expected-unavailable state:" + blockId(ownerState)
                        + ",slots:" + describeSlots(slots)));
            }

            double actualMass = sableApi.subLevelMass(subLevel, "getSelfMassTracker");
            double mergedMass = sableApi.subLevelMass(subLevel, "getMassTracker");
            double[] actualCenter = sableApi.subLevelCenter(subLevel, "getSelfMassTracker");
            double[] mergedCenter = sableApi.subLevelCenter(subLevel, "getMassTracker");
            double[] actualInertia = sableApi.subLevelInertia(subLevel, "getSelfMassTracker");
            double[] mergedInertia = sableApi.subLevelInertia(subLevel, "getMassTracker");
            boolean bodyAlive = sableApi.containsSubLevelIdentity(level, subLevel)
                    && !sableApi.isRemoved(subLevel)
                    && sableApi.hasPhysicsHandle(level, subLevel);
            Vec3 localCenter = expected.center();
            double[] expectedCenter = {
                    targetPos.getX() + localCenter.x,
                    targetPos.getY() + localCenter.y,
                    targetPos.getZ() + localCenter.z
            };
            double massTolerance = Math.max(1.0E-7D, expected.mass() * 1.0E-6D);
            double centerTolerance = 1.0E-5D;
            double largestExpectedInertia = 0.0D;
            for (double component : expected.inertia()) {
                largestExpectedInertia = Math.max(largestExpectedInertia, Math.abs(component));
            }
            double inertiaTolerance = Math.max(1.0E-7D, largestExpectedInertia * 1.0E-6D);
            boolean massMatches = Double.isFinite(actualMass)
                    && Math.abs(actualMass - expected.mass()) <= massTolerance
                    && Double.isFinite(mergedMass)
                    && Math.abs(mergedMass - expected.mass()) <= massTolerance;
            boolean centerMatches = actualCenter.length == 3;
            for (int index = 0; centerMatches && index < 3; index++) {
                centerMatches = Double.isFinite(actualCenter[index])
                        && Math.abs(actualCenter[index] - expectedCenter[index]) <= centerTolerance
                        && mergedCenter.length == 3
                        && Double.isFinite(mergedCenter[index])
                        && Math.abs(mergedCenter[index] - expectedCenter[index]) <= centerTolerance;
            }
            boolean inertiaMatches = actualInertia.length == expected.inertia().length;
            for (int index = 0; inertiaMatches && index < expected.inertia().length; index++) {
                inertiaMatches = Double.isFinite(actualInertia[index])
                        && Math.abs(actualInertia[index] - expected.inertia()[index]) <= inertiaTolerance
                        && mergedInertia.length == expected.inertia().length
                        && Double.isFinite(mergedInertia[index])
                        && Math.abs(mergedInertia[index] - expected.inertia()[index]) <= inertiaTolerance;
            }

            details.add(detailPrefix + "." + phase + "-body-alive=" + bodyAlive);
            details.add(detailPrefix + "." + phase + "-mass=self:" + actualMass + ",merged:" + mergedMass
                    + ",expected:" + expected.mass());
            details.add(detailPrefix + "." + phase + "-center="
                    + actualCenter[0] + "," + actualCenter[1] + "," + actualCenter[2]
                    + ",merged:" + mergedCenter[0] + "," + mergedCenter[1] + "," + mergedCenter[2]
                    + ",expected:" + expectedCenter[0] + "," + expectedCenter[1] + "," + expectedCenter[2]);
            details.add(detailPrefix + "." + phase + "-inertia=" + describeMatrix(actualInertia)
                    + ",merged:" + describeMatrix(mergedInertia)
                    + ",expected:" + describeMatrix(expected.inertia()));
            return new CheckResult(bodyAlive && massMatches && centerMatches && inertiaMatches, 7, details);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return new CheckResult(false, 7, List.of(detailPrefix + "." + phase
                    + "-mass-properties=exception:" + exception.getClass().getSimpleName()));
        }
    }

    private static String describeMatrix(double[] matrix) {
        if (matrix == null || matrix.length != 9) {
            return matrix == null ? "null" : "length:" + matrix.length;
        }
        return String.format(
                Locale.ROOT,
                "[[%.9f,%.9f,%.9f],[%.9f,%.9f,%.9f],[%.9f,%.9f,%.9f]]",
                matrix[0], matrix[1], matrix[2],
                matrix[3], matrix[4], matrix[5],
                matrix[6], matrix[7], matrix[8]
        );
    }

    private static void cleanupPhysicsMaterialProbe(PhysicsMaterialProbe probe) {
        // Sable may still have deferred HeatMapManager work queued for a stable assembled body.
        // Removing that sublevel from the fixture can therefore make a later split tick inside an
        // already removed plot. These lifecycle cases run in a disposable world, so retain their
        // assembled bodies and clear only the ordinary source-world fixture positions.
        probe.details().add(probe.materialCase().name() + ".created-sublevels-retained=disposable-world");
        if (probe.fixtureBlocksPrepared()) {
            clearFixtureBlock(probe.level(), probe.assemblerPos());
            clearFixtureBlock(probe.level(), probe.sourcePos());
        }
    }

    private static @Nullable PhysicsTerrainSupport findPhysicsTerrainSupport(ServerLevel level,
                                                                              BlockPos requested,
                                                                              Direction assemblerDirection) {
        for (int radius = 0; radius <= 32; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    PhysicsTerrainSupport support = physicsTerrainSupportAt(
                            level,
                            requested.getX() + dx,
                            requested.getZ() + dz,
                            assemblerDirection
                    );
                    if (support != null) {
                        return support;
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable PhysicsTerrainSupport physicsTerrainSupportAt(ServerLevel level,
                                                                           int centerX,
                                                                           int centerZ,
                                                                           Direction assemblerDirection) {
        Integer surfaceY = null;
        Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        int[][] supportOffsets = {
                {0, 0},
                {assemblerDirection.getStepX(), assemblerDirection.getStepZ()}
        };
        for (int[] offset : supportOffsets) {
            int x = centerX + offset[0];
            int z = centerZ + offset[1];
            Integer candidateSurfaceY = findTopCollidableSurfaceY(level, x, z);
            if (candidateSurfaceY == null) {
                return null;
            }
            if (surfaceY == null) {
                surfaceY = candidateSurfaceY;
            } else if (!candidateSurfaceY.equals(surfaceY)) {
                return null;
            }

            BlockPos surfacePos = new BlockPos(x, candidateSurfaceY, z);
            BlockState surfaceState = level.getBlockState(surfacePos);
            states.put(surfacePos.immutable(), surfaceState);
        }

        if (surfaceY == null) {
            return null;
        }
        BlockPos center = new BlockPos(centerX, surfaceY, centerZ);
        BlockPos sourcePos = center.above(2);
        BlockPos assemblerPos = sourcePos.relative(assemblerDirection);
        if (Math.abs(assemblerPos.getX() - centerX) > 1
                || Math.abs(assemblerPos.getZ() - centerZ) > 1
                || !level.getBlockState(sourcePos).isAir()
                || !level.getBlockState(assemblerPos).isAir()) {
            return null;
        }
        return new PhysicsTerrainSupport(center, Map.copyOf(states));
    }

    private static @Nullable Integer findTopCollidableSurfaceY(ServerLevel level, int x, int z) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int y = height + 2; y >= height - 8; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(candidate);
            if (!state.getCollisionShape(level, candidate, CollisionContext.empty()).isEmpty()
                    && level.getBlockState(candidate.above()).isAir()
                    && level.getBlockState(candidate.above(2)).isAir()) {
                return y;
            }
        }
        return null;
    }

    private static void verifyPhysicsTerrainSupport(PhysicsMaterialProbe probe, String phase) {
        int unchangedBlocks = 0;
        int collidableBlocks = 0;
        for (Map.Entry<BlockPos, BlockState> entry : probe.supportStates().entrySet()) {
            BlockState actual = probe.level().getBlockState(entry.getKey());
            if (actual.equals(entry.getValue())) {
                unchangedBlocks++;
            }
            if (!actual.getCollisionShape(probe.level(), entry.getKey(), CollisionContext.empty()).isEmpty()) {
                collidableBlocks++;
            }
        }
        probe.checked();
        int expectedBlocks = 2;
        if (probe.supportStates().size() != expectedBlocks
                || unchangedBlocks != expectedBlocks
                || collidableBlocks != expectedBlocks) {
            probe.fail(probe.materialCase().name() + "." + phase
                    + "-terrain-support=states:" + unchangedBlocks + "/" + expectedBlocks + ",collision:"
                    + collidableBlocks + "/" + expectedBlocks);
        } else {
            probe.details().add(probe.materialCase().name() + "." + phase
                    + "-terrain-support=states:" + expectedBlocks + "/" + expectedBlocks
                    + ",collision:" + expectedBlocks + "/" + expectedBlocks);
        }
    }

    private static Direction fixtureAssemblerDirection(BlockState sourceState) {
        if (sourceState.hasProperty(VerticalStepBlock.SIDE)) {
            return sourceState.getValue(VerticalStepBlock.SIDE);
        }
        if (sourceState.hasProperty(StepBlock.FACING)) {
            return sourceState.getValue(StepBlock.FACING);
        }
        return Direction.WEST;
    }

    private static CheckResult checkSableStepPlacement(MutableFixtureContext context,
                                                       SableFixtureApi sableApi,
                                                       Object subLevel,
                                                       SableStepCase stepCase,
                                                       BlockPos targetPos) {
        ServerPlayer player = context.serverPlayer();
        if (player == null) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".placement=no-server-player"));
        }

        ItemStack heldBefore = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean wasShiftDown = player.isShiftKeyDown();
        InteractionResult result;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(stepCase.placementBlock(), 64));
        player.setShiftKeyDown(false);
        try {
            BlockHitResult hit = new BlockHitResult(
                    new Vec3(
                            targetPos.getX() + stepCase.hitOffset().x,
                            targetPos.getY() + stepCase.hitOffset().y,
                            targetPos.getZ() + stepCase.hitOffset().z
                    ),
                    stepCase.placementFace(),
                    targetPos,
                    false
            );
            result = player.gameMode.useItemOn(
                    player,
                    context.serverLevel(),
                    player.getItemInHand(InteractionHand.MAIN_HAND),
                    InteractionHand.MAIN_HAND,
                    hit
            );
        } finally {
            player.setShiftKeyDown(wasShiftDown);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldBefore);
        }

        BlockState plotState = SableCompat.plotBlockState(context.serverLevel(), targetPos);
        BlockState normalState = context.serverLevel().getBlockState(targetPos);
        BlockState actualState = plotState == null ? normalState : plotState;
        BlockState[] actualSlots = EnhancedSlabMaterialSavedData.get(context.serverLevel(), targetPos.asLong());
        boolean passed = result.consumesAction()
                && actualState.is(stepCase.expectedQuadBlock())
                && materialSlotCount(actualSlots) == 2
                && slotsContainMaterial(actualSlots, stepCase.expectedExistingMaterial())
                && slotsContainMaterial(actualSlots, stepCase.expectedPlacedMaterial());
        String detail = stepCase.name() + ".placement=" + (passed ? describeSlots(actualSlots)
                : "fail result:" + result
                + " actual:" + blockId(actualState)
                + " plot:" + blockId(plotState)
                + " normal:" + blockId(normalState)
                + " blockEntity:" + blockEntityName(context.serverLevel().getBlockEntity(targetPos))
                + " expected:" + blockId(stepCase.expectedQuadBlock())
                + " slots:" + describeSlots(actualSlots)
                + " expectedExisting:" + blockId(stepCase.expectedExistingMaterial())
                + " expectedPlaced:" + blockId(stepCase.expectedPlacedMaterial()));

        CheckResult beforeBreakMass = passed
                ? checkSableLiveStepMassProperties(
                        sableApi,
                        context.serverLevel(),
                        subLevel,
                        targetPos,
                        actualState,
                        actualSlots,
                        stepCase.name(),
                        "before-partial-break"
                )
                : new CheckResult(false, 0, List.of());

        CheckResult partialBreak = checkSableStepPartialBreak(
                context,
                sableApi,
                subLevel,
                stepCase,
                targetPos
        );
        List<String> details = new ArrayList<>();
        details.add(detail);
        details.addAll(beforeBreakMass.details());
        details.addAll(partialBreak.details());
        CheckResult adjacentPlacement = new CheckResult(true, 0, List.of());
        if (partialBreak.passed() && stepCase.expectedQuadBlock() instanceof QuadVerticalStepBlock) {
            adjacentPlacement = checkSableVerticalStepAdjacentPlacement(context, stepCase, targetPos);
            details.addAll(adjacentPlacement.details());
        }
        return new CheckResult(
                passed && beforeBreakMass.passed() && partialBreak.passed() && adjacentPlacement.passed(),
                1 + beforeBreakMass.checked() + partialBreak.checked() + adjacentPlacement.checked(),
                details
        );
    }

    private static CheckResult checkSableVerticalStepAdjacentPlacement(MutableFixtureContext context,
                                                                        SableStepCase stepCase,
                                                                        BlockPos sourcePos) {
        ServerPlayer player = context.serverPlayer();
        ServerLevel level = context.serverLevel();
        if (player == null) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".adjacent-placement=no-server-player"));
        }

        BlockState sourceState = SableCompat.plotBlockState(level, sourcePos);
        BlockState[] sourceSlots = EnhancedSlabMaterialSavedData.get(level, sourcePos.asLong());
        int attachmentIndex = -1;
        for (int index : new int[]{1, 3}) {
            if (materialSlot(sourceSlots, index) != null) {
                attachmentIndex = index;
                break;
            }
        }
        if (sourceState == null || attachmentIndex < 0) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".adjacent-placement=no-east-anchor slots:"
                    + describeSlots(sourceSlots)));
        }

        Direction expectedFacing = QuadVerticalStepBlock.facingForIndex(attachmentIndex);
        Direction expectedSide = Direction.WEST;
        Direction clickedFace = Direction.EAST;
        BlockPos adjacentPos = sourcePos.relative(clickedFace);
        Vec3 hitLocation = new Vec3(
                sourcePos.getX() + 1.0D,
                sourcePos.getY() + 0.5D,
                sourcePos.getZ() + (expectedFacing == Direction.SOUTH ? 0.75D : 0.25D)
        );
        ItemStack heldBefore = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean wasShiftDown = player.isShiftKeyDown();
        InteractionResult result;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(stepCase.placementBlock(), 64));
        player.setShiftKeyDown(false);
        try {
            BlockHitResult hit = new BlockHitResult(hitLocation, clickedFace, sourcePos, false);
            result = player.gameMode.useItemOn(
                    player,
                    level,
                    player.getItemInHand(InteractionHand.MAIN_HAND),
                    InteractionHand.MAIN_HAND,
                    hit
            );
        } finally {
            player.setShiftKeyDown(wasShiftDown);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldBefore);
        }

        BlockState plotState = SableCompat.plotBlockState(level, adjacentPos);
        BlockState normalState = level.getBlockState(adjacentPos);
        BlockState[] adjacentSlots = EnhancedSlabMaterialSavedData.get(level, adjacentPos.asLong());
        boolean connectedGeometry = plotState != null
                && plotState.getBlock() instanceof VerticalStepBlock
                && plotState.hasProperty(VerticalStepBlock.FACING)
                && plotState.getValue(VerticalStepBlock.FACING) == expectedFacing
                && plotState.hasProperty(VerticalStepBlock.SIDE)
                && plotState.getValue(VerticalStepBlock.SIDE) == expectedSide
                && !plotState.getCollisionShape(level, adjacentPos, CollisionContext.empty()).isEmpty();
        boolean passed = result.consumesAction()
                && connectedGeometry
                && materialSlotCount(adjacentSlots) == 1
                && slotsContainMaterial(adjacentSlots, stepCase.expectedPlacedMaterial());
        String detail = stepCase.name() + ".adjacent-placement=" + (passed
                ? "source-index:" + attachmentIndex + ",target:" + adjacentPos.toShortString()
                + ",facing:" + expectedFacing.getSerializedName() + ",side:" + expectedSide.getSerializedName()
                : "fail result:" + result
                + " sourceIndex:" + attachmentIndex
                + " target:" + adjacentPos.toShortString()
                + " plot:" + blockId(plotState)
                + " normal:" + blockId(normalState)
                + " slots:" + describeSlots(adjacentSlots)
                + " expectedFacing:" + expectedFacing.getSerializedName()
                + " expectedSide:" + expectedSide.getSerializedName()
                + " connectedGeometry:" + connectedGeometry);
        return new CheckResult(passed, 1, List.of(detail));
    }

    private static CheckResult checkSableStepPartialBreak(MutableFixtureContext context,
                                                          SableFixtureApi sableApi,
                                                          Object subLevel,
                                                          SableStepCase stepCase,
                                                          BlockPos targetPos) {
        ServerPlayer player = context.serverPlayer();
        if (player == null) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".partial-break=no-server-player"));
        }

        boolean wasShiftDown = player.isShiftKeyDown();
        player.setShiftKeyDown(false);
        BlockState[] beforeSlots = EnhancedSlabMaterialSavedData.get(context.serverLevel(), targetPos.asLong());
        int expectedRemainingIndex = -1;
        BlockState expectedRemainingMaterial = null;
        Direction.Axis expectedHorizontalAxis = Direction.Axis.X;
        boolean horizontalQuad = stepCase.expectedQuadBlock() instanceof QuadStepBlock;
        boolean verticalQuad = stepCase.expectedQuadBlock() instanceof QuadVerticalStepBlock;
        if (horizontalQuad) {
            BlockState beforePlotState = SableCompat.plotBlockState(context.serverLevel(), targetPos);
            expectedHorizontalAxis = beforePlotState != null && beforePlotState.hasProperty(QuadStepBlock.AXIS)
                    ? beforePlotState.getValue(QuadStepBlock.AXIS)
                    : Direction.Axis.X;
            int targetedIndex = fixtureResolveStepIndex(
                    beforeSlots,
                    QuadStepBlock.bitsandbalance$getTargetedIndex(player, targetPos, expectedHorizontalAxis)
            );
            BlockState targetedMaterial = materialSlot(beforeSlots, targetedIndex);
            for (int i = 0; i < 4; i++) {
                BlockState material = materialSlot(beforeSlots, i);
                if (material != null && !material.equals(targetedMaterial)) {
                    expectedRemainingIndex = i;
                    expectedRemainingMaterial = material;
                    break;
                }
            }
        } else if (verticalQuad) {
            int targetedIndex = fixtureResolveVerticalStepIndex(
                    beforeSlots,
                    QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, targetPos)
            );
            BlockState targetedMaterial = materialSlot(beforeSlots, targetedIndex);
            for (int i = 0; i < 4; i++) {
                BlockState material = materialSlot(beforeSlots, i);
                if (material != null && !material.equals(targetedMaterial)) {
                    expectedRemainingIndex = i;
                    expectedRemainingMaterial = material;
                    break;
                }
            }
        }

        boolean destroyed;
        try {
            destroyed = performSableStepBreakAction(context, targetPos, beforeSlots);
        } finally {
            player.setShiftKeyDown(wasShiftDown);
        }
        BlockState plotState = SableCompat.plotBlockState(context.serverLevel(), targetPos);
        BlockState normalState = context.serverLevel().getBlockState(targetPos);
        BlockState actualState = plotState == null ? normalState : plotState;
        BlockState[] actualSlots = EnhancedSlabMaterialSavedData.get(context.serverLevel(), targetPos.asLong());
        boolean hasExisting = slotsContainMaterial(actualSlots, stepCase.expectedExistingMaterial());
        boolean hasPlaced = slotsContainMaterial(actualSlots, stepCase.expectedPlacedMaterial());
        boolean horizontalGeometry = !horizontalQuad
                || expectedRemainingIndex >= 0
                && plotState != null
                && actualState.hasProperty(StepBlock.FACING)
                && actualState.getValue(StepBlock.FACING) == QuadStepBlock.sideForIndex(
                        expectedHorizontalAxis,
                        expectedRemainingIndex
                )
                && actualState.hasProperty(StepBlock.TOP)
                && actualState.getValue(StepBlock.TOP) == QuadStepBlock.isTop(expectedRemainingIndex)
                && fixtureShapesEqual(
                        QuadStepBlock.shapeForIndex(expectedHorizontalAxis, expectedRemainingIndex),
                        actualState.getCollisionShape(context.serverLevel(), targetPos, CollisionContext.empty())
                );
        boolean verticalGeometry = !verticalQuad
                || expectedRemainingIndex >= 0
                && plotState != null
                && actualState.hasProperty(VerticalStepBlock.FACING)
                && actualState.getValue(VerticalStepBlock.FACING) == QuadVerticalStepBlock.facingForIndex(expectedRemainingIndex)
                && actualState.hasProperty(VerticalStepBlock.SIDE)
                && actualState.getValue(VerticalStepBlock.SIDE) == QuadVerticalStepBlock.sideForIndex(expectedRemainingIndex)
                && fixtureShapesEqual(
                        QuadVerticalStepBlock.shapeForIndex(expectedRemainingIndex),
                        actualState.getCollisionShape(context.serverLevel(), targetPos, CollisionContext.empty())
                );
        boolean passed = destroyed
                && !actualState.isAir()
                && materialSlotCount(actualSlots) == 1
                && expectedRemainingMaterial != null
                && expectedRemainingMaterial.equals(materialSlot(actualSlots, 0))
                && horizontalGeometry
                && verticalGeometry;
        String detail = stepCase.name() + ".partial-break=" + (passed ? describeSlots(actualSlots)
                : "fail destroyed:" + destroyed
                + " actual:" + blockId(actualState)
                + " plot:" + blockId(plotState)
                + " normal:" + blockId(normalState)
                + " slots:" + describeSlots(actualSlots)
                + " hasExisting:" + hasExisting
                + " hasPlaced:" + hasPlaced
                + " expectedRemainingIndex:" + expectedRemainingIndex
                + " expectedRemainingMaterial:" + blockId(expectedRemainingMaterial)
                + " horizontalGeometry:" + horizontalGeometry
                + " verticalGeometry:" + verticalGeometry);

        if (!passed) {
            return new CheckResult(passed, 1, List.of(detail));
        }

        CheckResult afterBreakMass = checkSableLiveStepMassProperties(
                sableApi,
                context.serverLevel(),
                subLevel,
                targetPos,
                actualState,
                actualSlots,
                stepCase.name(),
                "after-single-quarter-break"
        );

        CheckResult uniform = horizontalQuad
                ? checkSableUniformHorizontalStepPartialBreak(
                        context,
                        stepCase,
                        targetPos,
                        stepCase.expectedExistingMaterial(),
                        expectedHorizontalAxis
                )
                : checkSableUniformVerticalStepPartialBreak(
                        context,
                        stepCase,
                        targetPos,
                        stepCase.expectedExistingMaterial()
                );
        List<String> details = new ArrayList<>();
        details.add(detail);
        details.addAll(afterBreakMass.details());
        details.addAll(uniform.details());
        CheckResult singleQuarterMass = horizontalQuad && uniform.passed()
                ? checkSableHorizontalSingleQuarterMassMatrix(
                        context,
                        sableApi,
                        subLevel,
                        stepCase,
                        targetPos,
                        stepCase.expectedExistingMaterial()
                )
                : new CheckResult(true, 0, List.of());
        details.addAll(singleQuarterMass.details());
        CheckResult sequential = new CheckResult(true, 0, List.of());
        if (uniform.passed() && singleQuarterMass.passed()) {
            if (horizontalQuad) {
                CheckResult xSequence = checkSableSequentialStepPartialBreaks(
                        context, sableApi, subLevel, stepCase, targetPos,
                        stepCase.expectedExistingMaterial(), Direction.Axis.X
                );
                CheckResult zSequence = xSequence.passed()
                        ? checkSableSequentialStepPartialBreaks(
                        context, sableApi, subLevel, stepCase, targetPos,
                        stepCase.expectedExistingMaterial(), Direction.Axis.Z
                )
                        : new CheckResult(true, 0, List.of());
                List<String> sequenceDetails = new ArrayList<>(xSequence.details());
                sequenceDetails.addAll(zSequence.details());
                sequential = new CheckResult(
                        xSequence.passed() && zSequence.passed(),
                        xSequence.checked() + zSequence.checked(),
                        sequenceDetails
                );
            } else {
                sequential = checkSableSequentialStepPartialBreaks(
                        context, sableApi, subLevel, stepCase, targetPos,
                        stepCase.expectedExistingMaterial(), expectedHorizontalAxis
                );
            }
        }
        details.addAll(sequential.details());
        return new CheckResult(
                afterBreakMass.passed() && uniform.passed() && singleQuarterMass.passed() && sequential.passed(),
                1 + afterBreakMass.checked() + uniform.checked() + singleQuarterMass.checked() + sequential.checked(),
                details
        );
    }

    /**
     * Proves that successive KneeSlab removals preserve every survivor's original quadrant and
     * world-space position while Sable republishes the body's changing mass properties.
     */
    private static CheckResult checkSableSequentialStepPartialBreaks(MutableFixtureContext context,
                                                                      SableFixtureApi sableApi,
                                                                      Object subLevel,
                                                                      SableStepCase stepCase,
                                                                      BlockPos targetPos,
                                                                      @Nullable BlockState material,
                                                                      Direction.Axis horizontalAxis) {
        if (material == null) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".sequential-break=no-material"));
        }

        ServerLevel level = context.serverLevel();
        ServerPlayer player = context.serverPlayer();
        boolean horizontal = stepCase.expectedQuadBlock() instanceof QuadStepBlock;
        String geometryName = horizontal
                ? "horizontal-" + horizontalAxis.name().toLowerCase(Locale.ROOT)
                : "vertical";
        BlockState quadState = horizontal
                ? CommonBlocks.QUAD_STEP.defaultBlockState()
                .setValue(QuadStepBlock.WATERLOGGED, false)
                .setValue(QuadStepBlock.AXIS, horizontalAxis)
                : CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState()
                .setValue(QuadVerticalStepBlock.WATERLOGGED, false);
        BlockState[] expectedSlots = new BlockState[]{material, material, material, material};
        boolean restored = SableCompat.setPlotBlockState(level, targetPos, quadState, Block.UPDATE_ALL)
                && SableCompat.rememberPlotEnhancedSlabMaterials(
                level,
                targetPos,
                quadState,
                expectedSlots,
                Block.UPDATE_ALL
        );
        if (!restored) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".sequential-break=plot-restore-failed"));
        }

        List<String> details = new ArrayList<>();
        boolean passed = true;
        int checked = 0;
        for (int expectedCount = 3; expectedCount >= 1; expectedCount--) {
            int preferredIndex = horizontal
                    ? QuadStepBlock.bitsandbalance$getTargetedIndex(player, targetPos, horizontalAxis)
                    : QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, targetPos);
            int targetedIndex = horizontal
                    ? fixtureResolveStepIndex(expectedSlots, preferredIndex)
                    : fixtureResolveVerticalStepIndex(expectedSlots, preferredIndex);
            if (targetedIndex < 0 || materialSlot(expectedSlots, targetedIndex) == null) {
                checked++;
                passed = false;
                details.add(stepCase.name() + ".sequential-" + geometryName + "-"
                        + (expectedCount + 1) + "-to-" + expectedCount
                        + "=target-missing preferred:" + preferredIndex + ",slots:" + describeSlots(expectedSlots));
                break;
            }

            SablePoseGeometry beforeGeometry;
            try {
                beforeGeometry = sableApi.nativePoseGeometry(
                        level,
                        subLevel,
                        targetPos,
                        fixtureStepSlotShapes(horizontal, horizontalAxis, expectedSlots)
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                checked++;
                passed = false;
                details.add(stepCase.name() + ".sequential-" + geometryName + "-"
                        + (expectedCount + 1) + "-to-" + expectedCount
                        + "=before-pose:" + exception.getClass().getSimpleName());
                break;
            }

            BlockState[] beforeSlots = expectedSlots.clone();
            boolean wasShiftDown = player.isShiftKeyDown();
            boolean destroyed;
            try {
                player.setShiftKeyDown(true);
                destroyed = performSableStepBreakAction(context, targetPos, beforeSlots);
            } finally {
                player.setShiftKeyDown(wasShiftDown);
            }
            expectedSlots[targetedIndex] = null;

            BlockState plotState = SableCompat.plotBlockState(level, targetPos);
            BlockState[] storedSlots = EnhancedSlabMaterialSavedData.get(level, targetPos.asLong());
            BlockState[] effectiveSlots = fixtureEffectiveStepSlots(
                    plotState,
                    storedSlots,
                    horizontal,
                    horizontalAxis
            );
            boolean exactMask = fixtureSlotMasksEqual(expectedSlots, effectiveSlots);
            VoxelShape expectedShape = Shapes.empty();
            for (int index = 0; index < 4; index++) {
                if (expectedSlots[index] != null) {
                    expectedShape = Shapes.or(
                            expectedShape,
                            horizontal
                                    ? QuadStepBlock.shapeForIndex(horizontalAxis, index)
                                    : QuadVerticalStepBlock.shapeForIndex(index)
                    );
                }
            }
            VoxelShape actualShape = plotState == null
                    ? Shapes.empty()
                    : plotState.getCollisionShape(level, targetPos, CollisionContext.empty());
            boolean stateAndShape = plotState != null
                    && (expectedCount == 1
                    ? horizontal
                    ? plotState.getBlock() instanceof StepBlock
                    : plotState.getBlock() instanceof VerticalStepBlock
                    : horizontal
                    ? plotState.is(CommonBlocks.QUAD_STEP)
                    : plotState.is(CommonBlocks.QUAD_VERTICAL_STEP))
                    && fixtureShapesEqual(expectedShape, actualShape);

            SablePoseGeometry afterGeometry = null;
            boolean poseAndWorldGeometry = false;
            String poseDetail;
            try {
                afterGeometry = sableApi.nativePoseGeometry(
                        level,
                        subLevel,
                        targetPos,
                        fixtureStepSlotShapes(horizontal, horizontalAxis, expectedSlots)
                );
                double orientationDelta = fixtureOrientationDelta(beforeGeometry.pose(), afterGeometry.pose());
                double positionCompensationDelta = fixturePositionCompensationDelta(
                        beforeGeometry.pose(),
                        afterGeometry.pose()
                );
                double localPivotDelta = fixtureLocalPivotDelta(beforeGeometry.pose(), afterGeometry.pose());
                double nativePositionDelta = fixtureNativePositionDelta(beforeGeometry.pose(), afterGeometry.pose());
                double worldDelta = fixtureSurvivorWorldDelta(beforeGeometry, afterGeometry, expectedSlots);
                poseAndWorldGeometry = orientationDelta <= 1.0E-6D
                        // Sable's plot grid is millions of blocks from the playable body. Its
                        // native pose round-trip therefore carries a few microblocks of absolute
                        // coordinate noise even when every surviving vertex is stationary.
                        && positionCompensationDelta <= SABLE_POSE_POSITION_TOLERANCE
                        && localPivotDelta > 1.0E-6D
                        && nativePositionDelta > 1.0E-6D
                        && worldDelta <= SABLE_POSE_POSITION_TOLERANCE;
                poseDetail = "orientationDelta:" + orientationDelta
                        + ",positionCompensationDelta:" + positionCompensationDelta
                        + ",localPivotDelta:" + localPivotDelta
                        + ",nativePositionDelta:" + nativePositionDelta
                        + ",worldDelta:" + worldDelta;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                poseDetail = "exception:" + exception.getClass().getSimpleName();
            }

            CheckResult mass = destroyed && exactMask && stateAndShape
                    ? checkSableLiveStepMassProperties(
                    sableApi,
                    level,
                    subLevel,
                    targetPos,
                    plotState,
                    storedSlots,
                    stepCase.name(),
                    "sequential-" + geometryName + "-" + (expectedCount + 1) + "-to-" + expectedCount
            )
                    : new CheckResult(false, 0, List.of());
            boolean stagePassed = destroyed
                    && exactMask
                    && stateAndShape
                    && poseAndWorldGeometry
                    && mass.passed();
            passed &= stagePassed;
            checked += 1 + mass.checked();
            details.add(stepCase.name() + ".sequential-" + geometryName + "-"
                    + (expectedCount + 1) + "-to-" + expectedCount
                    + "=" + (stagePassed ? "pass" : "fail")
                    + ",target:" + targetedIndex
                    + ",expected:" + describeSlots(expectedSlots)
                    + ",effective:" + describeSlots(effectiveSlots)
                    + ",stored:" + describeSlots(storedSlots)
                    + ",exactMask:" + exactMask
                    + ",stateAndShape:" + stateAndShape
                    + "," + poseDetail);
            details.addAll(mass.details());
            if (!stagePassed) {
                break;
            }
        }
        return new CheckResult(passed, checked, details);
    }

    private static VoxelShape[] fixtureStepSlotShapes(boolean horizontal,
                                                       Direction.Axis horizontalAxis,
                                                       @Nullable BlockState[] slots) {
        VoxelShape[] shapes = new VoxelShape[4];
        for (int index = 0; index < shapes.length; index++) {
            if (materialSlot(slots, index) != null) {
                shapes[index] = horizontal
                        ? QuadStepBlock.shapeForIndex(horizontalAxis, index)
                        : QuadVerticalStepBlock.shapeForIndex(index);
            }
        }
        return shapes;
    }

    private static BlockState[] fixtureEffectiveStepSlots(@Nullable BlockState state,
                                                           @Nullable BlockState[] storedSlots,
                                                           boolean horizontal,
                                                           Direction.Axis horizontalAxis) {
        BlockState[] effective = new BlockState[4];
        if (state == null) {
            return effective;
        }
        if ((horizontal && state.getBlock() instanceof QuadStepBlock)
                || (!horizontal && state.getBlock() instanceof QuadVerticalStepBlock)) {
            if (storedSlots != null) {
                System.arraycopy(storedSlots, 0, effective, 0, Math.min(4, storedSlots.length));
            }
            return effective;
        }

        BlockState material = null;
        if (storedSlots != null) {
            for (BlockState candidate : storedSlots) {
                if (candidate != null) {
                    material = candidate;
                    break;
                }
            }
        }
        if (material == null) {
            return effective;
        }
        for (int index = 0; index < 4; index++) {
            boolean matches = horizontal
                    ? state.getBlock() instanceof StepBlock
                    && state.hasProperty(StepBlock.FACING)
                    && state.getValue(StepBlock.FACING) == QuadStepBlock.sideForIndex(horizontalAxis, index)
                    && state.hasProperty(StepBlock.TOP)
                    && state.getValue(StepBlock.TOP) == QuadStepBlock.isTop(index)
                    : state.getBlock() instanceof VerticalStepBlock
                    && state.hasProperty(VerticalStepBlock.FACING)
                    && state.getValue(VerticalStepBlock.FACING) == QuadVerticalStepBlock.facingForIndex(index)
                    && state.hasProperty(VerticalStepBlock.SIDE)
                    && state.getValue(VerticalStepBlock.SIDE) == QuadVerticalStepBlock.sideForIndex(index);
            if (matches) {
                effective[index] = material;
                break;
            }
        }
        return effective;
    }

    private static boolean fixtureSlotMasksEqual(@Nullable BlockState[] expected,
                                                  @Nullable BlockState[] actual) {
        for (int index = 0; index < 4; index++) {
            BlockState expectedSlot = materialSlot(expected, index);
            BlockState actualSlot = materialSlot(actual, index);
            if (expectedSlot == null ? actualSlot != null : !expectedSlot.equals(actualSlot)) {
                return false;
            }
        }
        return true;
    }

    private static double fixtureOrientationDelta(SableRigidPose before, SableRigidPose after) {
        double quaternionDot = Math.abs(before.qx() * after.qx()
                + before.qy() * after.qy()
                + before.qz() * after.qz()
                + before.qw() * after.qw());
        return Math.abs(1.0D - quaternionDot);
    }

    private static double fixturePositionCompensationDelta(SableRigidPose before, SableRigidPose after) {
        Vec3 localPivotDelta = new Vec3(
                (after.rotationX() - before.rotationX()) * before.scaleX(),
                (after.rotationY() - before.rotationY()) * before.scaleY(),
                (after.rotationZ() - before.rotationZ()) * before.scaleZ()
        );
        Vec3 expectedPositionDelta = fixtureRotateByQuaternion(localPivotDelta, before);
        Vec3 actualPositionDelta = new Vec3(
                after.x() - before.x(),
                after.y() - before.y(),
                after.z() - before.z()
        );
        return expectedPositionDelta.distanceTo(actualPositionDelta);
    }

    private static double fixtureLocalPivotDelta(SableRigidPose before, SableRigidPose after) {
        return new Vec3(
                after.rotationX() - before.rotationX(),
                after.rotationY() - before.rotationY(),
                after.rotationZ() - before.rotationZ()
        ).length();
    }

    private static double fixtureNativePositionDelta(SableRigidPose before, SableRigidPose after) {
        return new Vec3(
                after.x() - before.x(),
                after.y() - before.y(),
                after.z() - before.z()
        ).length();
    }

    private static Vec3 fixtureRotateByQuaternion(Vec3 value, SableRigidPose pose) {
        double tx = 2.0D * (pose.qy() * value.z - pose.qz() * value.y);
        double ty = 2.0D * (pose.qz() * value.x - pose.qx() * value.z);
        double tz = 2.0D * (pose.qx() * value.y - pose.qy() * value.x);
        return new Vec3(
                value.x + pose.qw() * tx + pose.qy() * tz - pose.qz() * ty,
                value.y + pose.qw() * ty + pose.qz() * tx - pose.qx() * tz,
                value.z + pose.qw() * tz + pose.qx() * ty - pose.qy() * tx
        );
    }

    private static double fixtureSurvivorWorldDelta(SablePoseGeometry before,
                                                     SablePoseGeometry after,
                                                     @Nullable BlockState[] survivingSlots) {
        double maximum = 0.0D;
        for (int index = 0; index < 4; index++) {
            if (materialSlot(survivingSlots, index) == null) {
                continue;
            }
            List<Vec3> beforeVertices = before.slotVertices().get(index);
            List<Vec3> afterVertices = after.slotVertices().get(index);
            if (beforeVertices.size() != afterVertices.size()) {
                return Double.POSITIVE_INFINITY;
            }
            for (int vertex = 0; vertex < beforeVertices.size(); vertex++) {
                maximum = Math.max(maximum, beforeVertices.get(vertex).distanceTo(afterVertices.get(vertex)));
            }
        }
        return maximum;
    }

    /**
     * Independently verifies count-one restoration and mass properties for both horizontal Step
     * axes. Expected centers and inertia tensors come directly from a half-by-half-by-one cuboid,
     * not from the production Sable compatibility helpers.
     */
    private static CheckResult checkSableHorizontalSingleQuarterMassMatrix(MutableFixtureContext context,
                                                                            SableFixtureApi sableApi,
                                                                            Object subLevel,
                                                                            SableStepCase stepCase,
                                                                            BlockPos targetPos,
                                                                            @Nullable BlockState material) {
        if (material == null) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".single-quarter-mass=no-material"));
        }

        ServerLevel level = context.serverLevel();
        ServerPlayer player = context.serverPlayer();
        List<String> details = new ArrayList<>();
        boolean passed = true;
        int checked = 0;
        for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
            String axisName = axis == Direction.Axis.X ? "x" : "z";
            BlockState quadState = CommonBlocks.QUAD_STEP.defaultBlockState()
                    .setValue(QuadStepBlock.WATERLOGGED, false)
                    .setValue(QuadStepBlock.AXIS, axis);
            BlockState[] fullSlots = new BlockState[]{material, material, material, material};
            boolean restored = SableCompat.setPlotBlockState(level, targetPos, quadState, Block.UPDATE_ALL)
                    && SableCompat.rememberPlotEnhancedSlabMaterials(
                            level,
                            targetPos,
                            quadState,
                            fullSlots,
                            Block.UPDATE_ALL
                    );
            if (!restored) {
                checked++;
                passed = false;
                details.add(stepCase.name() + ".single-quarter-" + axisName + "=plot-restore-failed");
                continue;
            }

            boolean wasShiftDown = player.isShiftKeyDown();
            int targetedIndex = -1;
            int remainingIndex = -1;
            boolean destroyed = false;
            try {
                player.setShiftKeyDown(true);
                targetedIndex = QuadStepBlock.bitsandbalance$getTargetedIndex(player, targetPos, axis);
                remainingIndex = targetedIndex ^ 2;
                BlockState[] twoSlots = new BlockState[4];
                twoSlots[targetedIndex] = material;
                twoSlots[remainingIndex] = material;
                restored = SableCompat.rememberPlotEnhancedSlabMaterials(
                        level,
                        targetPos,
                        quadState,
                        twoSlots,
                        Block.UPDATE_ALL
                );
                destroyed = restored && performSableStepBreakAction(context, targetPos, twoSlots);
            } finally {
                player.setShiftKeyDown(wasShiftDown);
            }

            BlockState plotState = SableCompat.plotBlockState(level, targetPos);
            BlockState[] remainingSlots = EnhancedSlabMaterialSavedData.get(level, targetPos.asLong());
            boolean stateMatches = destroyed
                    && remainingIndex >= 0
                    && plotState != null
                    && plotState.getBlock() instanceof StepBlock
                    && plotState.hasProperty(StepBlock.FACING)
                    && plotState.getValue(StepBlock.FACING) == QuadStepBlock.sideForIndex(axis, remainingIndex)
                    && plotState.hasProperty(StepBlock.TOP)
                    && plotState.getValue(StepBlock.TOP) == QuadStepBlock.isTop(remainingIndex)
                    && materialSlotCount(remainingSlots) == 1
                    && material.equals(materialSlot(remainingSlots, 0))
                    && fixtureShapesEqual(
                            QuadStepBlock.shapeForIndex(axis, remainingIndex),
                            plotState.getCollisionShape(level, targetPos, CollisionContext.empty())
                    );

            boolean massMatches = false;
            String massDetail;
            try {
                BlockGetter plotAccessor = sableApi.embeddedPlotAccessor(subLevel);
                BlockPos plotCenter = sableApi.centerBlock(subLevel);
                if (plotAccessor == null || plotCenter == null) {
                    massDetail = "plot-accessor-or-center-missing";
                } else {
                    BlockPos accessorPos = targetPos.subtract(plotCenter);
                    double expectedMass = sableApi.blockMass(plotAccessor, accessorPos, material) * 0.5D;
                    double selfMass = sableApi.subLevelMass(subLevel, "getSelfMassTracker");
                    double mergedMass = sableApi.subLevelMass(subLevel, "getMassTracker");
                    double[] center = sableApi.subLevelCenter(subLevel, "getSelfMassTracker");
                    double[] mergedCenter = sableApi.subLevelCenter(subLevel, "getMassTracker");
                    double[] inertia = sableApi.subLevelInertia(subLevel, "getSelfMassTracker");
                    double[] mergedInertia = sableApi.subLevelInertia(subLevel, "getMassTracker");
                    boolean bodyAlive = sableApi.containsSubLevelIdentity(level, subLevel)
                            && !sableApi.isRemoved(subLevel)
                            && sableApi.hasPhysicsHandle(level, subLevel);
                    // A completed 2 -> 1 conversion leaves a one-voxel Step body. Its Sable mass
                    // center intentionally follows the voxel-centered native buoyancy sampler.
                    double expectedX = targetPos.getX() + 0.5D;
                    double expectedY = targetPos.getY() + 0.5D;
                    double expectedZ = targetPos.getZ() + 0.5D;
                    double[] expectedInertia = axis == Direction.Axis.X
                            ? new double[]{5.0D * expectedMass / 48.0D, 0.0D, 0.0D,
                            0.0D, 5.0D * expectedMass / 48.0D, 0.0D,
                            0.0D, 0.0D, expectedMass / 24.0D}
                            : new double[]{expectedMass / 24.0D, 0.0D, 0.0D,
                            0.0D, 5.0D * expectedMass / 48.0D, 0.0D,
                            0.0D, 0.0D, 5.0D * expectedMass / 48.0D};
                    double massTolerance = Math.max(1.0E-6D, expectedMass * 1.0E-6D);
                    double inertiaTolerance = Math.max(1.0E-7D, expectedMass * 1.0E-6D);
                    massMatches = bodyAlive
                            && Math.abs(selfMass - expectedMass) <= massTolerance
                            && Math.abs(mergedMass - expectedMass) <= massTolerance
                            && center.length == 3
                            && mergedCenter.length == 3
                            && Math.abs(center[0] - expectedX) <= 1.0E-5D
                            && Math.abs(center[1] - expectedY) <= 1.0E-5D
                            && Math.abs(center[2] - expectedZ) <= 1.0E-5D
                            && Math.abs(mergedCenter[0] - expectedX) <= 1.0E-5D
                            && Math.abs(mergedCenter[1] - expectedY) <= 1.0E-5D
                            && Math.abs(mergedCenter[2] - expectedZ) <= 1.0E-5D
                            && inertia.length == expectedInertia.length
                            && mergedInertia.length == expectedInertia.length;
                    for (int index = 0; massMatches && index < expectedInertia.length; index++) {
                        massMatches = Double.isFinite(inertia[index])
                                && Math.abs(inertia[index] - expectedInertia[index]) <= inertiaTolerance
                                && Double.isFinite(mergedInertia[index])
                                && Math.abs(mergedInertia[index] - expectedInertia[index]) <= inertiaTolerance;
                    }
                    massDetail = "bodyAlive:" + bodyAlive + ",self:" + selfMass + ",merged:" + mergedMass
                            + ",expectedMass:" + expectedMass
                            + ",center:" + center[0] + "," + center[1] + "," + center[2]
                            + ",mergedCenter:" + mergedCenter[0] + "," + mergedCenter[1] + "," + mergedCenter[2]
                            + ",expectedCenter:" + expectedX + "," + expectedY + "," + expectedZ
                            + ",inertia:" + describeMatrix(inertia)
                            + ",mergedInertia:" + describeMatrix(mergedInertia)
                            + ",expectedInertia:" + describeMatrix(expectedInertia);
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                massDetail = "exception:" + exception.getClass().getSimpleName();
            }

            checked += 2;
            boolean axisPassed = restored && stateMatches && massMatches;
            passed &= axisPassed;
            details.add(stepCase.name() + ".single-quarter-" + axisName + "="
                    + (axisPassed ? "pass" : "fail")
                    + ",target:" + targetedIndex
                    + ",remaining:" + remainingIndex
                    + ",plot:" + blockId(plotState)
                    + ",slots:" + describeSlots(remainingSlots)
                    + ",stateMatches:" + stateMatches
                    + ",mass:" + massDetail);
        }
        return new CheckResult(passed, checked, details);
    }

    private static CheckResult checkSableUniformHorizontalStepPartialBreak(MutableFixtureContext context,
                                                                           SableStepCase stepCase,
                                                                           BlockPos targetPos,
                                                                           @Nullable BlockState material,
                                                                           Direction.Axis axis) {
        if (material == null) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".uniform-partial-break=no-material"));
        }

        ServerLevel level = context.serverLevel();
        ServerPlayer player = context.serverPlayer();
        BlockState quadState = CommonBlocks.QUAD_STEP.defaultBlockState()
                .setValue(QuadStepBlock.WATERLOGGED, false)
                .setValue(QuadStepBlock.AXIS, axis);
        BlockState[] uniformSlots = new BlockState[]{material, material, material, material};
        boolean restored = SableCompat.setPlotBlockState(level, targetPos, quadState, Block.UPDATE_ALL)
                && SableCompat.rememberPlotEnhancedSlabMaterials(
                        level,
                        targetPos,
                        quadState,
                        uniformSlots,
                        Block.UPDATE_ALL
                );
        if (!restored) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".uniform-partial-break=plot-restore-failed"));
        }

        boolean wasShiftDown = player.isShiftKeyDown();
        int targetedIndex;
        boolean destroyed;
        try {
            player.setShiftKeyDown(true);
            targetedIndex = QuadStepBlock.bitsandbalance$getTargetedIndex(player, targetPos, axis);
            destroyed = performSableStepBreakAction(context, targetPos, uniformSlots);
        } finally {
            player.setShiftKeyDown(wasShiftDown);
        }

        BlockState plotState = SableCompat.plotBlockState(level, targetPos);
        BlockState[] remaining = EnhancedSlabMaterialSavedData.get(level, targetPos.asLong());
        VoxelShape expectedShape = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            if (i != targetedIndex) {
                expectedShape = Shapes.or(expectedShape, QuadStepBlock.shapeForIndex(axis, i));
            }
        }
        VoxelShape actualShape = plotState == null
                ? Shapes.empty()
                : plotState.getCollisionShape(level, targetPos, CollisionContext.empty());
        boolean remainingSlotsMatch = targetedIndex >= 0;
        for (int i = 0; i < 4 && remainingSlotsMatch; i++) {
            BlockState remainingMaterial = materialSlot(remaining, i);
            remainingSlotsMatch = i == targetedIndex
                    ? remainingMaterial == null
                    : material.equals(remainingMaterial);
        }
        boolean passed = destroyed
                && plotState != null
                && plotState.is(CommonBlocks.QUAD_STEP)
                && targetedIndex >= 0
                && materialSlotCount(remaining) == 3
                && remainingSlotsMatch
                && fixtureShapesEqual(expectedShape, actualShape);
        String detail = stepCase.name() + ".uniform-partial-break=" + (passed
                ? "target:" + targetedIndex + ",slots:" + describeSlots(remaining)
                : "fail destroyed:" + destroyed
                + " target:" + targetedIndex
                + " plot:" + blockId(plotState)
                + " slots:" + describeSlots(remaining)
                + " remainingSlotsMatch:" + remainingSlotsMatch
                + " expectedShape:" + expectedShape.toAabbs()
                + " actualShape:" + actualShape.toAabbs());
        return new CheckResult(passed, 1, List.of(detail));
    }

    private static CheckResult checkSableUniformVerticalStepPartialBreak(MutableFixtureContext context,
                                                                         SableStepCase stepCase,
                                                                         BlockPos targetPos,
                                                                         @Nullable BlockState material) {
        if (material == null) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".uniform-partial-break=no-material"));
        }

        ServerLevel level = context.serverLevel();
        ServerPlayer player = context.serverPlayer();
        BlockState quadState = CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState()
                .setValue(QuadVerticalStepBlock.WATERLOGGED, false);
        BlockState[] uniformSlots = new BlockState[]{material, material, material, material};
        boolean restored = SableCompat.setPlotBlockState(level, targetPos, quadState, Block.UPDATE_ALL)
                && SableCompat.rememberPlotEnhancedSlabMaterials(
                        level,
                        targetPos,
                        quadState,
                        uniformSlots,
                        Block.UPDATE_ALL
                );
        if (!restored) {
            return new CheckResult(false, 1, List.of(stepCase.name() + ".uniform-partial-break=plot-restore-failed"));
        }

        boolean wasShiftDown = player.isShiftKeyDown();
        int targetedIndex;
        boolean destroyed;
        try {
            player.setShiftKeyDown(true);
            targetedIndex = QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, targetPos);
            destroyed = performSableStepBreakAction(context, targetPos, uniformSlots);
        } finally {
            player.setShiftKeyDown(wasShiftDown);
        }

        BlockState plotState = SableCompat.plotBlockState(level, targetPos);
        BlockState[] remaining = EnhancedSlabMaterialSavedData.get(level, targetPos.asLong());
        VoxelShape expectedShape = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            if (i != targetedIndex) {
                expectedShape = Shapes.or(expectedShape, QuadVerticalStepBlock.shapeForIndex(i));
            }
        }
        VoxelShape actualShape = plotState == null
                ? Shapes.empty()
                : plotState.getCollisionShape(level, targetPos, CollisionContext.empty());
        boolean remainingSlotsMatch = targetedIndex >= 0;
        for (int i = 0; i < 4 && remainingSlotsMatch; i++) {
            BlockState remainingMaterial = materialSlot(remaining, i);
            remainingSlotsMatch = i == targetedIndex
                    ? remainingMaterial == null
                    : material.equals(remainingMaterial);
        }
        boolean passed = destroyed
                && plotState != null
                && plotState.is(CommonBlocks.QUAD_VERTICAL_STEP)
                && targetedIndex >= 0
                && materialSlotCount(remaining) == 3
                && remainingSlotsMatch
                && fixtureShapesEqual(expectedShape, actualShape);
        String detail = stepCase.name() + ".uniform-partial-break=" + (passed
                ? "target:" + targetedIndex + ",slots:" + describeSlots(remaining)
                : "fail destroyed:" + destroyed
                + " target:" + targetedIndex
                + " plot:" + blockId(plotState)
                + " slots:" + describeSlots(remaining)
                + " remainingSlotsMatch:" + remainingSlotsMatch
                + " expectedShape:" + expectedShape.toAabbs()
                + " actualShape:" + actualShape.toAabbs());
        return new CheckResult(passed, 1, List.of(detail));
    }

    private static boolean performSableStepBreakAction(MutableFixtureContext context,
                                                       BlockPos targetPos,
                                                       @Nullable BlockState[] beforeSlots) {
        ServerPlayer player = context.serverPlayer();
        ServerLevel level = context.serverLevel();
        int beforeCount = materialSlotCount(beforeSlots);
        sendSableStepBreakAction(player, level, targetPos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        int afterStartCount = materialSlotCount(EnhancedSlabMaterialSavedData.get(level, targetPos.asLong()));
        if (afterStartCount < beforeCount) {
            return true;
        }

        boolean shiftAtStart = player.isShiftKeyDown();
        boolean destroyed;
        try {
            // Prove that the START-selected KneeSlab mode is retained until actual destruction.
            // A moving Sable body can make mining span multiple ticks; re-reading crouch state at
            // completion would otherwise remove a different material group or quadrant.
            player.setShiftKeyDown(!shiftAtStart);
            destroyed = player.gameMode.destroyBlock(targetPos);
        } finally {
            player.setShiftKeyDown(shiftAtStart);
        }
        int afterDestroyCount = materialSlotCount(EnhancedSlabMaterialSavedData.get(level, targetPos.asLong()));
        if (!destroyed || afterDestroyCount >= beforeCount) {
            sendSableStepBreakAction(player, level, targetPos, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK);
        }
        return destroyed && afterDestroyCount < beforeCount;
    }

    private static void sendSableStepBreakAction(ServerPlayer player,
                                                 ServerLevel level,
                                                 BlockPos targetPos,
                                                 ServerboundPlayerActionPacket.Action action) {
        try {
            Method handler = player.gameMode.getClass().getMethod(
                    "handleBlockBreakAction",
                    BlockPos.class,
                    ServerboundPlayerActionPacket.Action.class,
                    Direction.class,
                    int.class,
                    int.class
            );
            int maxBuildHeight = 320;
            for (String methodName : List.of("getMaxY", "getMaxBuildHeight")) {
                try {
                    Object value = level.getClass().getMethod(methodName).invoke(level);
                    if (value instanceof Number number) {
                        maxBuildHeight = number.intValue();
                        break;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            handler.invoke(player.gameMode, targetPos, action, Direction.UP, maxBuildHeight, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to exercise the server block-action path", exception);
        }
    }

    private static boolean fixtureShapesEqual(VoxelShape expected, VoxelShape actual) {
        List<AABB> expectedBoxes = expected.toAabbs();
        List<AABB> actualBoxes = actual.toAabbs();
        if (expectedBoxes.size() != actualBoxes.size()) {
            return false;
        }
        double epsilon = 1.0E-7D;
        for (int i = 0; i < expectedBoxes.size(); i++) {
            AABB expectedBox = expectedBoxes.get(i);
            AABB actualBox = actualBoxes.get(i);
            if (Math.abs(expectedBox.minX - actualBox.minX) > epsilon
                    || Math.abs(expectedBox.minY - actualBox.minY) > epsilon
                    || Math.abs(expectedBox.minZ - actualBox.minZ) > epsilon
                    || Math.abs(expectedBox.maxX - actualBox.maxX) > epsilon
                    || Math.abs(expectedBox.maxY - actualBox.maxY) > epsilon
                    || Math.abs(expectedBox.maxZ - actualBox.maxZ) > epsilon) {
                return false;
            }
        }
        return true;
    }

    private static BlockState stepFixtureState(Block block) {
        return block.defaultBlockState()
                .setValue(StepBlock.FACING, Direction.NORTH)
                .setValue(StepBlock.TOP, false)
                .setValue(StepBlock.WATERLOGGED, false);
    }

    private static BlockState verticalStepFixtureState(Block block) {
        return block.defaultBlockState()
                .setValue(VerticalStepBlock.FACING, Direction.NORTH)
                .setValue(VerticalStepBlock.SIDE, Direction.EAST)
                .setValue(VerticalStepBlock.WATERLOGGED, false);
    }

    private static BlockState normalizedSlab(Block block) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(SlabBlock.TYPE)) {
            state = state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            state = state.setValue(SlabBlock.WATERLOGGED, false);
        }
        return state;
    }

    private static BlockState[] singleSlot(BlockState state) {
        return new BlockState[]{state, null, null, null};
    }

    private static boolean slotsContainMaterial(@Nullable BlockState[] slots, BlockState expected) {
        if (slots == null || expected == null) {
            return false;
        }
        for (BlockState slot : slots) {
            if (slot != null && slot.is(expected.getBlock())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameMaterialSlots(@Nullable BlockState[] expected, @Nullable BlockState[] actual) {
        int length = Math.max(expected == null ? 0 : expected.length, actual == null ? 0 : actual.length);
        for (int i = 0; i < length; i++) {
            BlockState expectedSlot = expected != null && i < expected.length ? expected[i] : null;
            BlockState actualSlot = actual != null && i < actual.length ? actual[i] : null;
            if (expectedSlot == null ? actualSlot != null : !expectedSlot.equals(actualSlot)) {
                return false;
            }
        }
        return true;
    }

    private static int materialSlotCount(@Nullable BlockState[] slots) {
        int count = 0;
        if (slots != null) {
            for (BlockState slot : slots) {
                if (slot != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static @Nullable BlockState materialSlot(@Nullable BlockState[] slots, int index) {
        return slots != null && index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static int fixtureResolveStepIndex(@Nullable BlockState[] slots, int preferredIndex) {
        if (materialSlot(slots, preferredIndex) != null) {
            return preferredIndex;
        }
        boolean positive = preferredIndex >= 0 && preferredIndex < 4 && QuadStepBlock.isPositive(preferredIndex);
        boolean top = preferredIndex >= 0 && preferredIndex < 4 && QuadStepBlock.isTop(preferredIndex);
        for (int candidate : new int[]{
                QuadStepBlock.toIndex(positive, !top),
                QuadStepBlock.toIndex(!positive, top),
                QuadStepBlock.toIndex(!positive, !top)
        }) {
            if (materialSlot(slots, candidate) != null) {
                return candidate;
            }
        }
        return fixtureFirstOccupiedSlot(slots);
    }

    private static int fixtureResolveVerticalStepIndex(@Nullable BlockState[] slots, int preferredIndex) {
        if (materialSlot(slots, preferredIndex) != null) {
            return preferredIndex;
        }
        boolean east = preferredIndex >= 0 && preferredIndex < 4 && QuadVerticalStepBlock.isEast(preferredIndex);
        boolean south = preferredIndex >= 0 && preferredIndex < 4 && QuadVerticalStepBlock.isSouth(preferredIndex);
        for (int candidate : new int[]{
                QuadVerticalStepBlock.toIndex(east, !south),
                QuadVerticalStepBlock.toIndex(!east, south),
                QuadVerticalStepBlock.toIndex(!east, !south)
        }) {
            if (materialSlot(slots, candidate) != null) {
                return candidate;
            }
        }
        return fixtureFirstOccupiedSlot(slots);
    }

    private static int fixtureFirstOccupiedSlot(@Nullable BlockState[] slots) {
        if (slots != null) {
            for (int i = 0; i < Math.min(4, slots.length); i++) {
                if (slots[i] != null) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static CheckResult markReloadPersistenceScheduled() {
        return new CheckResult(true, 1, List.of("reload-save=server-shutdown"));
    }

    private static void cleanupRegion(MutableFixtureContext context) {
        ServerLevel level = context.serverLevel();
        for (int x = 0; x < MUTABLE_REGION_LENGTH; x++) {
            for (int z = 0; z < MUTABLE_REGION_DEPTH; z++) {
                BlockPos pos = context.basePos().offset(x, 0, z);
                clearFixtureBlock(level, pos);
            }
        }
    }

    private static void clearFixtureMaterial(ServerLevel level, BlockPos pos) {
        EnhancedSlabMaterialSavedData.remember(level, pos.asLong());
        EnhancedSlabMaterialSavedData.removeClient(level, pos.asLong());
    }

    private static void clearFixtureBlock(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        clearFixtureMaterial(level, pos);
    }

    private static MinecraftServer singleplayerServer(Minecraft client) {
        try {
            Method method = client.getClass().getMethod("getSingleplayerServer");
            Object value = method.invoke(client);
            return value instanceof MinecraftServer server ? server : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static BlockPos findNearbyAir(ServerLevel level, BlockPos preferred) {
        for (int dy = 0; dy <= 4; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = preferred.offset(dx, dy, dz);
                    if (level.getBlockState(pos).isAir()) {
                        return pos;
                    }
                }
            }
        }
        return preferred;
    }

    private static void restoreBlock(MinecraftServer server, ServerLevel serverLevel, ClientLevel clientLevel, BlockPos pos, BlockState serverState, BlockState clientState) {
        if (serverState != null) {
            try {
                callServer(server, () -> {
                    serverLevel.setBlock(pos, serverState, Block.UPDATE_ALL);
                    return null;
                });
            } catch (Exception ignored) {
            }
        }
        if (clientState != null) {
            clientLevel.setBlock(pos, clientState, Block.UPDATE_ALL);
        }
    }

    private static <T> T callServer(MinecraftServer server, Callable<T> callable) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future.get(5, TimeUnit.SECONDS);
    }

    private static @Nullable ServerPlayer serverPlayer(MinecraftServer server, @Nullable Player clientPlayer) {
        if (server == null || clientPlayer == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(clientPlayer.getUUID());
    }

    private static boolean hasAnySlot(BlockState[] slots) {
        if (slots == null) {
            return false;
        }
        for (BlockState slot : slots) {
            if (slot != null) {
                return true;
            }
        }
        return false;
    }

    private static String describeSlots(BlockState[] slots) {
        List<String> ids = new ArrayList<>();
        if (slots != null) {
            for (BlockState slot : slots) {
                ids.add(slot == null ? "_" : String.valueOf(BuiltInRegistries.BLOCK.getKey(slot.getBlock())));
            }
        }
        return String.join(",", ids);
    }

    private static boolean enabled() {
        boolean enabled = enabledValue(System.getProperty(ENABLE_PROPERTY)) || enabledValue(System.getenv(ENABLE_ENV));
        if (!enabled && !loggedDisabled) {
            loggedDisabled = true;
        }
        return enabled;
    }

    private static boolean exitAfterRun() {
        String property = System.getProperty(EXIT_PROPERTY);
        if (property != null) {
            return truthy(property);
        }
        String env = System.getenv(EXIT_ENV);
        return env == null || truthy(env);
    }

    private static String targetName() {
        String property = System.getProperty(TARGET_PROPERTY);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String env = System.getenv(TARGET_ENV);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return "unknown";
    }

    private static FixtureRequest request() {
        return new FixtureRequest(
                runnerName(),
                targetName(),
                optionalProperty(SCENARIO_PROPERTY, "default"),
                optionalProperty(PHASE_PROPERTY, "default"),
                optionalProperty(BASE_POS_PROPERTY, "unset")
        );
    }

    private static String runnerName() {
        String property = System.getProperty(ENABLE_PROPERTY);
        if (property != null && !property.isBlank()) {
            return normalizeRunnerName(property);
        }
        String env = System.getenv(ENABLE_ENV);
        if (env != null && !env.isBlank()) {
            return normalizeRunnerName(env);
        }
        return "disabled";
    }

    private static String normalizeRunnerName(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return truthy(normalized) ? RUNNER_LEDGER : normalized;
    }

    private static String optionalProperty(String name, String fallback) {
        String property = System.getProperty(name);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        return fallback;
    }

    private static boolean allowMutation() {
        return truthy(System.getProperty(ALLOW_MUTATION_PROPERTY));
    }

    private static boolean truthy(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "y", "on" -> true;
            default -> false;
        };
    }

    private static boolean enabledValue(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "", "0", "false", "no", "n", "off" -> false;
            default -> true;
        };
    }

    private static void stopClient(Minecraft client) {
        try {
            Method method = client.getClass().getMethod("stop");
            method.invoke(client);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("BNB_FIXTURE unable to stop client cleanly after fixture run", exception);
        }
    }

    private static @Nullable BlockPos parseBasePos(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("unset")) {
            return null;
        }
        String[] parts = value.trim().split("[,\\s]+");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isScratchScenario(FixtureRequest request) {
        String scenario = request.scenario().toLowerCase(Locale.ROOT);
        return scenario.contains("scratch") || scenario.contains("mutable") || scenario.contains("fresh");
    }

    private static boolean isScratchName(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("scratch") || normalized.contains("mutable") || normalized.contains("fresh");
    }

    private static String levelName(MinecraftServer server) {
        try {
            Method getWorldData = server.getClass().getMethod("getWorldData");
            Object worldData = getWorldData.invoke(server);
            if (worldData != null) {
                Method getLevelName = worldData.getClass().getMethod("getLevelName");
                Object levelName = getLevelName.invoke(worldData);
                if (levelName instanceof String name && !name.isBlank()) {
                    return name;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return "unknown";
    }

    private static Path worldPath(MinecraftServer server) {
        try {
            return server.getWorldPath(LevelResource.ROOT);
        } catch (RuntimeException exception) {
            return Path.of("unknown");
        }
    }

    private static List<String> withDetail(List<String> details, String detail) {
        List<String> copy = new ArrayList<>(details);
        copy.add(detail);
        return copy;
    }

    private static String blockId(@Nullable BlockState state) {
        return state == null ? "null" : String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static String blockId(@Nullable Block block) {
        return block == null ? "null" : String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static BlockState chunkBlockState(ServerLevel level, BlockPos pos) {
        try {
            LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            return chunk.getBlockState(pos);
        } catch (RuntimeException exception) {
            return level.getBlockState(pos);
        }
    }

    private static BlockState sableAwareBlockState(ServerLevel level, BlockPos pos) {
        BlockState plotState = SableCompat.plotBlockState(level, pos);
        return plotState == null ? chunkBlockState(level, pos) : plotState;
    }

    private static final class SableFixtureApi {
        private final Method gatherConnectedBlocks;
        private final Method assembleBlocks;
        private final Method resultBlocks;
        private final Method resultCheckedBlocks;
        private final Method resultBoundingBox;
        private final Method resultAssemblyState;
        private final Method getContainer;
        private final Method removeSubLevel;
        private final Object removalReason;

        private SableFixtureApi(Method gatherConnectedBlocks,
                                Method assembleBlocks,
                                Method resultBlocks,
                                Method resultCheckedBlocks,
                                Method resultBoundingBox,
                                Method resultAssemblyState,
                                Method getContainer,
                                Method removeSubLevel,
                                Object removalReason) {
            this.gatherConnectedBlocks = gatherConnectedBlocks;
            this.assembleBlocks = assembleBlocks;
            this.resultBlocks = resultBlocks;
            this.resultCheckedBlocks = resultCheckedBlocks;
            this.resultBoundingBox = resultBoundingBox;
            this.resultAssemblyState = resultAssemblyState;
            this.getContainer = getContainer;
            this.removeSubLevel = removeSubLevel;
            this.removalReason = removalReason;
        }

        private static @Nullable SableFixtureApi create(List<String> details) {
            try {
                Class<?> helperClass = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper");
                Class<?> frontierPredicateClass = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper$FrontierPredicate");
                Class<?> boundsClass = sableClass(
                        "dev.ryanhcode.sable.companion.math.BoundingBox3ic",
                        "dev.ryanhcode.sable.companion.math.BoundingBox3i"
                );
                Class<?> resultClass = Class.forName("dev.ryanhcode.sable.api.SubLevelAssemblyHelper$GatherResult");
                Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
                Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
                Class<?> removalReasonClass = Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason");

                Method gatherConnectedBlocks = helperClass.getMethod(
                        "gatherConnectedBlocks",
                        BlockPos.class,
                        ServerLevel.class,
                        int.class,
                        frontierPredicateClass
                );
                Method assembleBlocks = helperClass.getMethod(
                        "assembleBlocks",
                        ServerLevel.class,
                        BlockPos.class,
                        Iterable.class,
                        boundsClass
                );
                Method getContainer = sableGetContainerMethod(containerClass);
                Method removeSubLevel = containerClass.getMethod("removeSubLevel", subLevelClass, removalReasonClass);
                Object removalReason = sableRemovalReason(removalReasonClass);

                details.add("sable-api=available");
                return new SableFixtureApi(
                        gatherConnectedBlocks,
                        assembleBlocks,
                        resultClass.getMethod("blocks"),
                        resultClass.getMethod("checkedBlocks"),
                        resultClass.getMethod("boundingBox"),
                        resultClass.getMethod("assemblyState"),
                        getContainer,
                        removeSubLevel,
                        removalReason
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                details.add("sable-api=unavailable:" + exception.getClass().getSimpleName());
                return null;
            }
        }

        private SableGatherResult gather(ServerLevel level, BlockPos sourcePos, int capacity) throws ReflectiveOperationException {
            Object result = gatherConnectedBlocks.invoke(null, sourcePos, level, capacity, null);
            return describeGatherResult(result);
        }

        private SableGatherResult gatherShapeAware(ServerLevel level, BlockPos sourcePos, int capacity) throws ReflectiveOperationException {
            Object result = SableCompat.gatherConnectedBlocksShapeAware(sourcePos, level, capacity, null);
            return describeGatherResult(result);
        }

        private SableGatherResult describeGatherResult(@Nullable Object result) throws ReflectiveOperationException {
            if (result == null) {
                return null;
            }

            Object rawBlocks = resultBlocks.invoke(result);
            Set<BlockPos> blocks = new LinkedHashSet<>();
            if (rawBlocks instanceof Iterable<?> iterable) {
                for (Object value : iterable) {
                    if (value instanceof BlockPos blockPos) {
                        blocks.add(blockPos.immutable());
                    }
                }
            }

            Object rawChecked = resultCheckedBlocks.invoke(result);
            int checkedBlocks = rawChecked instanceof Number number ? number.intValue() : 0;
            Object boundingBox = resultBoundingBox.invoke(result);
            Object state = resultAssemblyState.invoke(result);
            return new SableGatherResult(blocks, checkedBlocks, boundingBox, String.valueOf(state));
        }

        private Object assemble(ServerLevel level,
                                BlockPos sourcePos,
                                Iterable<BlockPos> blocks,
                                Object boundingBox) throws ReflectiveOperationException {
            return assembleBlocks.invoke(null, level, sourcePos, blocks, boundingBox);
        }

        private @Nullable BlockPos centerBlock(Object subLevel) throws ReflectiveOperationException {
            Method getPlot = subLevel.getClass().getMethod("getPlot");
            Object plot = getPlot.invoke(subLevel);
            if (plot == null) {
                return null;
            }
            Method getCenterBlock = plot.getClass().getMethod("getCenterBlock");
            Object center = getCenterBlock.invoke(plot);
            return center instanceof BlockPos blockPos ? blockPos.immutable() : null;
        }

        private @Nullable BlockGetter embeddedPlotAccessor(Object subLevel) throws ReflectiveOperationException {
            Method getPlot = subLevel.getClass().getMethod("getPlot");
            Object plot = getPlot.invoke(subLevel);
            if (plot == null) {
                return null;
            }
            Method getEmbeddedLevelAccessor = plot.getClass().getMethod("getEmbeddedLevelAccessor");
            Object accessor = getEmbeddedLevelAccessor.invoke(plot);
            return accessor instanceof BlockGetter blockGetter ? blockGetter : null;
        }

        private boolean isSingleVoxelBody(Object subLevel) throws ReflectiveOperationException {
            Method getPlot = subLevel.getClass().getMethod("getPlot");
            Object plot = getPlot.invoke(subLevel);
            if (plot == null) {
                return false;
            }
            Object bounds = plot.getClass().getMethod("getBoundingBox").invoke(plot);
            return bounds != null
                    && invokeInt(bounds, "minX") == invokeInt(bounds, "maxX")
                    && invokeInt(bounds, "minY") == invokeInt(bounds, "maxY")
                    && invokeInt(bounds, "minZ") == invokeInt(bounds, "maxZ");
        }

        private Set<Object> subLevelIdentitySnapshot(ServerLevel level) throws ReflectiveOperationException {
            Set<Object> snapshot = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object subLevel : allSubLevels(level)) {
                snapshot.add(subLevel);
            }
            return snapshot;
        }

        private @Nullable Object findNewSubLevelContainingBlocks(ServerLevel level,
                                                                 Set<Object> existingSubLevels,
                                                                 Block firstBlock,
                                                                 Block secondBlock) throws ReflectiveOperationException {
            for (Object subLevel : allSubLevels(level)) {
                if (subLevel == null || existingSubLevels.contains(subLevel) || isRemoved(subLevel)) {
                    continue;
                }
                if (findBlockInPlot(subLevel, firstBlock) != null && findBlockInPlot(subLevel, secondBlock) != null) {
                    return subLevel;
                }
            }
            return null;
        }

        private @Nullable Object findNewSubLevelContainingBlock(ServerLevel level,
                                                                Set<Object> existingSubLevels,
                                                                Block block) throws ReflectiveOperationException {
            for (Object subLevel : allSubLevels(level)) {
                if (subLevel == null || existingSubLevels.contains(subLevel) || isRemoved(subLevel)) {
                    continue;
                }
                if (findBlockInPlot(subLevel, block) != null) {
                    return subLevel;
                }
            }
            return null;
        }

        private @Nullable SableMaterialLocation findActiveMaterialLocation(ServerLevel level,
                                                                           Set<Object> excludedSubLevels,
                                                                           Block expectedBlock,
                                                                           BlockState[] expectedSlots)
                throws ReflectiveOperationException {
            for (Object candidate : allSubLevels(level)) {
                if (candidate == null || excludedSubLevels.contains(candidate) || isRemoved(candidate)) {
                    continue;
                }
                for (BlockPos targetPos : findBlocksInPlot(candidate, expectedBlock)) {
                    BlockState[] slots = EnhancedSlabMaterialSavedData.get(level, targetPos.asLong());
                    if (sameMaterialSlots(expectedSlots, slots)) {
                        return new SableMaterialLocation(candidate, targetPos);
                    }
                }
            }
            return null;
        }

        private @Nullable SableMaterialLocation findActiveBlockLocation(ServerLevel level,
                                                                         Set<Object> excludedSubLevels,
                                                                         Block expectedBlock)
                throws ReflectiveOperationException {
            for (Object candidate : allSubLevels(level)) {
                if (candidate == null || excludedSubLevels.contains(candidate) || isRemoved(candidate)) {
                    continue;
                }
                BlockPos targetPos = findBlockInPlot(candidate, expectedBlock);
                if (targetPos != null) {
                    return new SableMaterialLocation(candidate, targetPos);
                }
            }
            return null;
        }

        private String describeCandidateEnhancedSlabBodies(ServerLevel level,
                                                            Set<Object> excludedSubLevels)
                throws ReflectiveOperationException {
            List<String> descriptions = new ArrayList<>();
            for (Object candidate : allSubLevels(level)) {
                if (candidate == null || excludedSubLevels.contains(candidate)) {
                    continue;
                }
                List<BlockPos> positions = findBlocksInPlot(candidate, CommonBlocks.QUAD_STEP);
                List<String> slots = new ArrayList<>();
                for (BlockPos pos : positions) {
                    slots.add(pos.toShortString() + "="
                            + describeSlots(EnhancedSlabMaterialSavedData.get(level, pos.asLong())));
                }
                descriptions.add("id:" + System.identityHashCode(candidate)
                        + "/removed:" + isRemoved(candidate)
                        + "/quadSteps:" + positions.size()
                        + "/selfMass:" + subLevelMass(candidate, "getSelfMassTracker")
                        + "/slots:" + slots);
            }
            return descriptions.isEmpty() ? "none" : String.join("|", descriptions);
        }

        private List<Object> allSubLevels(ServerLevel level) throws ReflectiveOperationException {
            Object container = getContainer.invoke(null, level);
            Method getAllSubLevels = container.getClass().getMethod("getAllSubLevels");
            Object rawSubLevels = getAllSubLevels.invoke(container);
            List<Object> subLevels = new ArrayList<>();
            if (rawSubLevels instanceof Iterable<?> iterable) {
                for (Object subLevel : iterable) {
                    if (subLevel != null) {
                        subLevels.add(subLevel);
                    }
                }
            }
            return subLevels;
        }

        private boolean isRemoved(Object subLevel) throws ReflectiveOperationException {
            Method isRemoved = subLevel.getClass().getMethod("isRemoved");
            Object removed = isRemoved.invoke(subLevel);
            return removed instanceof Boolean value && value;
        }

        private boolean containsSubLevelIdentity(ServerLevel level, Object expected) throws ReflectiveOperationException {
            for (Object subLevel : allSubLevels(level)) {
                if (subLevel == expected) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasPhysicsHandle(ServerLevel level, Object subLevel) throws ReflectiveOperationException {
            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem"
            );
            Class<?> serverSubLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            Object physicsSystem = physicsSystemClass.getMethod("get", Level.class).invoke(null, level);
            return physicsSystem != null
                    && physicsSystemClass.getMethod("getPhysicsHandle", serverSubLevelClass)
                    .invoke(physicsSystem, subLevel) != null;
        }

        private double blockMass(BlockGetter level, BlockPos pos, BlockState state) throws ReflectiveOperationException {
            Class<?> helperClass = Class.forName(
                    "dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper"
            );
            Method getMass = helperClass.getMethod("getMass", BlockGetter.class, BlockPos.class, BlockState.class);
            Object mass = getMass.invoke(null, level, pos, state);
            if (mass instanceof Number number) {
                return number.doubleValue();
            }
            throw new NoSuchMethodException("PhysicsBlockPropertyHelper.getMass returned "
                    + (mass == null ? "null" : mass.getClass().getName()));
        }

        /**
         * Reads the same low-byte neighborhood classification Rapier packs beside its collider id.
         * Older and newer supported Sable lines expose the accelerator as an implementation field;
         * if that implementation detail changes, the public solidity helper remains a useful required
         * assertion that the position-backed Step shape is not being classified as empty.
         */
        private SableVoxelNeighborhoodStatus targetVoxelNeighborhoodState(ServerLevel level,
                                                                           BlockPos targetPos,
                                                                           BlockGetter plotAccessor,
                                                                           BlockPos accessorPos,
                                                                           BlockState targetState) {
            String getStateFailure = "unavailable";
            try {
                Class<?> physicsSystemClass = Class.forName(
                        "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem"
                );
                Object physicsSystem = physicsSystemClass.getMethod("get", Level.class).invoke(null, level);
                Object pipeline = physicsSystem == null
                        ? null
                        : physicsSystemClass.getMethod("getPipeline").invoke(physicsSystem);
                Object accelerator = sableFieldByNameOrType(
                        pipeline,
                        "accelerator",
                        "dev.ryanhcode.sable.util.LevelAccelerator"
                );
                if (accelerator == null) {
                    getStateFailure = "accelerator-missing";
                } else {
                    Class<?> neighborhoodClass = Class.forName(
                            "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState"
                    );
                    Method getState = null;
                    for (Method method : neighborhoodClass.getMethods()) {
                        if (method.getName().equals("getState") && method.getParameterCount() == 3
                                && method.getParameterTypes()[0].isInstance(accelerator)
                                && method.getParameterTypes()[1].isAssignableFrom(BlockPos.class)) {
                            getState = method;
                            break;
                        }
                    }
                    if (getState == null) {
                        getStateFailure = "method-missing";
                    } else {
                        Object state = getState.invoke(null, accelerator, targetPos, null);
                        String classification = state == null ? "null" : String.valueOf(state);
                        return new SableVoxelNeighborhoodStatus(
                                state != null && !"EMPTY".equalsIgnoreCase(classification),
                                "getState:" + classification
                        );
                    }
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                getStateFailure = exception.getClass().getSimpleName();
            }

            try {
                Class<?> neighborhoodClass = Class.forName(
                        "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState"
                );
                Method isSolid = null;
                for (Method method : neighborhoodClass.getMethods()) {
                    if (method.getName().equals("isSolid") && method.getParameterCount() == 3) {
                        isSolid = method;
                        break;
                    }
                }
                if (isSolid == null) {
                    return new SableVoxelNeighborhoodStatus(
                            false,
                            "unavailable:getState:" + getStateFailure + ",isSolid:method-missing"
                    );
                }
                Object result = isSolid.invoke(null, plotAccessor, accessorPos, targetState);
                boolean solid = result instanceof Boolean value && value;
                return new SableVoxelNeighborhoodStatus(
                        solid,
                        "isSolid:" + solid + ",getState-fallback:" + getStateFailure
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                return new SableVoxelNeighborhoodStatus(
                        false,
                        "unavailable:getState:" + getStateFailure + ",isSolid:"
                                + exception.getClass().getSimpleName()
                );
            }
        }

        private static @Nullable Object sableFieldByNameOrType(@Nullable Object target,
                                                                String fieldName,
                                                                String typeName)
                throws ReflectiveOperationException {
            if (target == null) {
                return null;
            }

            Field typeMatch = null;
            for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (field.getName().equals(fieldName)) {
                        field.setAccessible(true);
                        return field.get(target);
                    }
                    if (typeMatch == null && field.getType().getName().equals(typeName)) {
                        typeMatch = field;
                    }
                }
            }
            if (typeMatch == null) {
                return null;
            }
            typeMatch.setAccessible(true);
            return typeMatch.get(target);
        }

        private double subLevelMass(Object subLevel, String trackerMethodName) throws ReflectiveOperationException {
            Method getMassTracker = subLevel.getClass().getMethod(trackerMethodName);
            Object tracker = getMassTracker.invoke(subLevel);
            if (tracker == null) {
                return 0.0D;
            }
            Object mass = tracker.getClass().getMethod("getMass").invoke(tracker);
            if (mass instanceof Number number) {
                return number.doubleValue();
            }
            throw new NoSuchMethodException("MassTracker.getMass returned "
                    + (mass == null ? "null" : mass.getClass().getName()));
        }

        private Object massTracker(Object subLevel) throws ReflectiveOperationException {
            return subLevel.getClass().getMethod("getMassTracker").invoke(subLevel);
        }

        private boolean physicsPaused(ServerLevel level) throws ReflectiveOperationException {
            Object physicsSystem = physicsSystem(level);
            Object paused = physicsSystem.getClass().getMethod("getPaused").invoke(physicsSystem);
            return paused instanceof Boolean value && value;
        }

        private void setPhysicsPaused(ServerLevel level, boolean paused) throws ReflectiveOperationException {
            Object physicsSystem = physicsSystem(level);
            physicsSystem.getClass().getMethod("setPaused", boolean.class).invoke(physicsSystem, paused);
        }

        private double bodyAngularSpeed(ServerLevel level, Object subLevel) throws ReflectiveOperationException {
            Object physicsSystem = physicsSystem(level);
            Class<?> serverSubLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            Object handle = physicsSystem.getClass().getMethod("getPhysicsHandle", serverSubLevelClass)
                    .invoke(physicsSystem, subLevel);
            if (handle == null) {
                return Double.POSITIVE_INFINITY;
            }
            Object velocity = handle.getClass().getMethod("getAngularVelocity").invoke(handle);
            double x = reflectedDouble(velocity, "x");
            double y = reflectedDouble(velocity, "y");
            double z = reflectedDouble(velocity, "z");
            return Math.sqrt(x * x + y * y + z * z);
        }

        private Object physicsSystem(ServerLevel level) throws ReflectiveOperationException {
            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem"
            );
            Object physicsSystem = physicsSystemClass.getMethod("get", Level.class).invoke(null, level);
            if (physicsSystem == null) {
                throw new IllegalStateException("Sable SubLevelPhysicsSystem is unavailable");
            }
            return physicsSystem;
        }

        private double[] subLevelCenter(Object subLevel, String trackerMethodName) throws ReflectiveOperationException {
            Method getMassTracker = subLevel.getClass().getMethod(trackerMethodName);
            Object tracker = getMassTracker.invoke(subLevel);
            if (tracker == null) {
                throw new NoSuchMethodException(trackerMethodName + " returned null");
            }
            Object center = tracker.getClass().getMethod("getCenterOfMass").invoke(tracker);
            if (center == null) {
                throw new NoSuchMethodException("MassTracker.getCenterOfMass returned null");
            }
            return new double[]{
                    ((Number) center.getClass().getMethod("x").invoke(center)).doubleValue(),
                    ((Number) center.getClass().getMethod("y").invoke(center)).doubleValue(),
                    ((Number) center.getClass().getMethod("z").invoke(center)).doubleValue()
            };
        }

        private double[] subLevelInertia(Object subLevel, String trackerMethodName)
                throws ReflectiveOperationException {
            Method getMassTracker = subLevel.getClass().getMethod(trackerMethodName);
            Object tracker = getMassTracker.invoke(subLevel);
            if (tracker == null) {
                throw new NoSuchMethodException(trackerMethodName + " returned null");
            }
            Object inertia = tracker.getClass().getMethod("getInertiaTensor").invoke(tracker);
            if (inertia == null) {
                throw new NoSuchMethodException("MassTracker.getInertiaTensor returned null");
            }
            return new double[]{
                    matrixComponent(inertia, "m00"),
                    matrixComponent(inertia, "m01"),
                    matrixComponent(inertia, "m02"),
                    matrixComponent(inertia, "m10"),
                    matrixComponent(inertia, "m11"),
                    matrixComponent(inertia, "m12"),
                    matrixComponent(inertia, "m20"),
                    matrixComponent(inertia, "m21"),
                    matrixComponent(inertia, "m22")
            };
        }

        private static double matrixComponent(Object matrix, String methodName)
                throws ReflectiveOperationException {
            Object component = matrix.getClass().getMethod(methodName).invoke(matrix);
            if (component instanceof Number number) {
                return number.doubleValue();
            }
            throw new NoSuchMethodException(matrix.getClass().getName() + "." + methodName
                    + " returned " + (component == null ? "null" : component.getClass().getName()));
        }

        private SableHeatMapStatus heatMapStatus(Object subLevel) throws ReflectiveOperationException {
            if (subLevel == null) {
                return new SableHeatMapStatus(false, "sublevel-null");
            }
            Object manager = subLevel.getClass().getMethod("getHeatMapManager").invoke(subLevel);
            if (manager == null) {
                return new SableHeatMapStatus(false, "manager-null");
            }
            boolean splitComplete = booleanField(manager, "splitComplete");
            boolean initialized = booleanField(manager, "initialized");
            Object state = fieldValue(manager, "state");
            int floodfill = collectionSize(fieldValue(manager, "floodfill"));
            int removed = collectionSize(fieldValue(manager, "removed"));
            int newStarts = collectionSize(fieldValue(manager, "newStarts"));
            int splits = collectionSize(fieldValue(manager, "subLevelSplits"));
            boolean stable = initialized
                    && splitComplete
                    && floodfill == 0
                    && removed == 0
                    && newStarts == 0
                    && splits == 0;
            return new SableHeatMapStatus(
                    stable,
                    "stable:" + stable
                            + ",initialized:" + initialized
                            + ",splitComplete:" + splitComplete
                            + ",state:" + state
                            + ",floodfill:" + floodfill
                            + ",removed:" + removed
                            + ",newStarts:" + newStarts
                            + ",splits:" + splits
            );
        }

        private static boolean booleanField(Object target, String name) throws ReflectiveOperationException {
            Object value = fieldValue(target, name);
            return value instanceof Boolean bool && bool;
        }

        private static Object fieldValue(Object target, String name) throws ReflectiveOperationException {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        }

        private static int collectionSize(Object value) {
            if (value instanceof java.util.Collection<?> collection) {
                return collection.size();
            }
            if (value instanceof java.util.Map<?, ?> map) {
                return map.size();
            }
            try {
                Object size = value == null ? null : value.getClass().getMethod("size").invoke(value);
                return size instanceof Number number ? number.intValue() : -1;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return -1;
            }
        }

        private @Nullable BlockPos findBlockInPlot(Object subLevel, Block block) throws ReflectiveOperationException {
            List<BlockPos> matches = findBlocksInPlot(subLevel, block);
            return matches.isEmpty() ? null : matches.getFirst();
        }

        private List<BlockPos> findBlocksInPlot(Object subLevel, Block block) throws ReflectiveOperationException {
            if (subLevel == null || block == null) {
                return List.of();
            }

            Method getPlot = subLevel.getClass().getMethod("getPlot");
            Object plot = getPlot.invoke(subLevel);
            if (plot == null) {
                return List.of();
            }

            Method getLoadedChunks = plot.getClass().getMethod("getLoadedChunks");
            Object rawChunks = getLoadedChunks.invoke(plot);
            if (!(rawChunks instanceof Iterable<?> chunks)) {
                return List.of();
            }

            List<BlockPos> matches = new ArrayList<>();
            for (Object holder : chunks) {
                if (holder == null) {
                    continue;
                }
                Method getBoundingBox = holder.getClass().getMethod("getBoundingBox");
                Object bounds = getBoundingBox.invoke(holder);
                if (bounds == null || "EMPTY".equals(String.valueOf(bounds))) {
                    continue;
                }
                Method getChunk = holder.getClass().getMethod("getChunk");
                Object chunkObject = getChunk.invoke(holder);
                if (!(chunkObject instanceof LevelChunk chunk)) {
                    continue;
                }

                int minX = invokeInt(bounds, "minX");
                int minY = invokeInt(bounds, "minY");
                int minZ = invokeInt(bounds, "minZ");
                int maxX = invokeInt(bounds, "maxX");
                int maxY = invokeInt(bounds, "maxY");
                int maxZ = invokeInt(bounds, "maxZ");
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos plotPos = new BlockPos(
                                    chunk.getPos().getMinBlockX() + x,
                                    y,
                                    chunk.getPos().getMinBlockZ() + z
                            );
                            if (chunk.getBlockState(plotPos).is(block)) {
                                matches.add(plotPos.immutable());
                            }
                        }
                    }
                }
            }
            return List.copyOf(matches);
        }

        private BlockState plotBlockState(Object subLevel, BlockPos plotPos) throws ReflectiveOperationException {
            if (subLevel == null || plotPos == null) {
                return Blocks.AIR.defaultBlockState();
            }
            Method getPlot = subLevel.getClass().getMethod("getPlot");
            Object plot = getPlot.invoke(subLevel);
            if (plot == null) {
                return Blocks.AIR.defaultBlockState();
            }
            Object rawChunks = plot.getClass().getMethod("getLoadedChunks").invoke(plot);
            if (!(rawChunks instanceof Iterable<?> chunks)) {
                return Blocks.AIR.defaultBlockState();
            }
            int expectedChunkX = SectionPos.blockToSectionCoord(plotPos.getX());
            int expectedChunkZ = SectionPos.blockToSectionCoord(plotPos.getZ());
            for (Object holder : chunks) {
                if (holder == null) {
                    continue;
                }
                Object chunkObject = holder.getClass().getMethod("getChunk").invoke(holder);
                if (chunkObject instanceof LevelChunk chunk
                        && SectionPos.blockToSectionCoord(chunk.getPos().getMinBlockX()) == expectedChunkX
                        && SectionPos.blockToSectionCoord(chunk.getPos().getMinBlockZ()) == expectedChunkZ) {
                    return chunk.getBlockState(plotPos);
                }
            }
            return Blocks.AIR.defaultBlockState();
        }

        /**
         * Invokes the exact non-scaffolding shape query used by Sable's entity-vs-sublevel solver.
         * Both supported Sable releases construct this accelerator from Entity.level(), so the
         * fixture mirrors that parent-level accessor and its plot-grid BlockPos exactly.
         */
        private VoxelShape subLevelEntityCollisionShape(Level entityLevel,
                                                        BlockPos plotPos,
                                                        BlockState state)
                throws ReflectiveOperationException {
            Class<?> acceleratorClass = Class.forName("dev.ryanhcode.sable.util.LevelAccelerator");
            Object accelerator = acceleratorClass.getConstructor(Level.class).newInstance(entityLevel);
            Class<?> collisionClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision"
            );
            Method shapeQuery = null;
            for (Method method : collisionClass.getDeclaredMethods()) {
                if (method.getName().equals("getSubLevelEntityCollisionShape")
                        && method.getParameterCount() == 7) {
                    shapeQuery = method;
                    break;
                }
            }
            if (shapeQuery == null) {
                throw new NoSuchMethodException("Sable entity collision shape query is unavailable");
            }
            shapeQuery.setAccessible(true);
            Object result = shapeQuery.invoke(null, null, null, null, state, accelerator, plotPos, null);
            if (!(result instanceof VoxelShape shape)) {
                throw new IllegalStateException("Sable entity collision query did not return a VoxelShape");
            }
            return shape;
        }

        private String describeSubLevel(@Nullable Object subLevel) {
            return subLevel == null ? "null" : String.valueOf(subLevel);
        }

        private String describeSubLevelBounds(@Nullable Object subLevel) {
            if (subLevel == null) {
                return "subLevel=null";
            }

            try {
                Method getPlot = subLevel.getClass().getMethod("getPlot");
                Object plot = getPlot.invoke(subLevel);
                Object plotBounds = null;
                if (plot != null) {
                    Method getBoundingBox = plot.getClass().getMethod("getBoundingBox");
                    plotBounds = getBoundingBox.invoke(plot);
                }
                Method boundingBox = subLevel.getClass().getMethod("boundingBox");
                Object globalBounds = boundingBox.invoke(subLevel);
                Method isRemoved = subLevel.getClass().getMethod("isRemoved");
                Object removed = isRemoved.invoke(subLevel);
                return "removed=" + removed + " plot=" + plotBounds + " global=" + globalBounds;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                return "unavailable:" + exception.getClass().getSimpleName();
            }
        }

        private SableBodyMotion bodyMotion(ServerLevel level,
                                           Object subLevel,
                                           BlockPos targetPos) throws ReflectiveOperationException {
            Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
            Object position = pose.getClass().getMethod("position").invoke(pose);

            BlockGetter plotAccessor = embeddedPlotAccessor(subLevel);
            BlockPos plotCenter = centerBlock(subLevel);
            if (plotAccessor == null || plotCenter == null) {
                throw new IllegalStateException("Sable embedded plot accessor is unavailable");
            }
            BlockPos accessorPos = targetPos.subtract(plotCenter);
            BlockState targetState = plotBlockState(subLevel, targetPos);
            VoxelShape collision = targetState.getCollisionShape(
                    plotAccessor,
                    accessorPos,
                    CollisionContext.empty()
            );
            if (collision.isEmpty()) {
                throw new IllegalStateException("Sable target collision shape is empty");
            }

            Class<?> poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
            Method transformPosition = poseClass.getMethod("transformPosition", Vec3.class);
            Method transformPositionInverse = poseClass.getMethod("transformPositionInverse", Vec3.class);
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            List<AABB> localCollisionBoxes = new ArrayList<>();
            List<SableTransformedColliderBox> transformedColliderBoxes = new ArrayList<>();
            for (AABB box : collision.toAabbs()) {
                AABB localBox = new AABB(
                        targetPos.getX() + box.minX,
                        targetPos.getY() + box.minY,
                        targetPos.getZ() + box.minZ,
                        targetPos.getX() + box.maxX,
                        targetPos.getY() + box.maxY,
                        targetPos.getZ() + box.maxZ
                );
                localCollisionBoxes.add(localBox);
                List<Vec3> transformedVertices = new ArrayList<>(8);
                for (double x : new double[]{box.minX, box.maxX}) {
                    for (double y : new double[]{box.minY, box.maxY}) {
                        for (double z : new double[]{box.minZ, box.maxZ}) {
                            Vec3 transformed = (Vec3) transformPosition.invoke(
                                    pose,
                                    new Vec3(targetPos.getX() + x, targetPos.getY() + y, targetPos.getZ() + z)
                            );
                            transformedVertices.add(transformed);
                            minX = Math.min(minX, transformed.x);
                            minY = Math.min(minY, transformed.y);
                            minZ = Math.min(minZ, transformed.z);
                            maxX = Math.max(maxX, transformed.x);
                            maxY = Math.max(maxY, transformed.y);
                            maxZ = Math.max(maxZ, transformed.z);
                        }
                    }
                }
                transformedColliderBoxes.add(new SableTransformedColliderBox(List.copyOf(transformedVertices)));
            }

            Vec3 inverseOrigin = (Vec3) transformPositionInverse.invoke(pose, Vec3.ZERO);
            Vec3 inverseX = ((Vec3) transformPositionInverse.invoke(pose, new Vec3(1.0D, 0.0D, 0.0D)))
                    .subtract(inverseOrigin);
            Vec3 inverseY = ((Vec3) transformPositionInverse.invoke(pose, new Vec3(0.0D, 1.0D, 0.0D)))
                    .subtract(inverseOrigin);
            Vec3 inverseZ = ((Vec3) transformPositionInverse.invoke(pose, new Vec3(0.0D, 0.0D, 1.0D)))
                    .subtract(inverseOrigin);
            SableColliderGeometry colliderGeometry = new SableColliderGeometry(
                    List.copyOf(localCollisionBoxes),
                    List.copyOf(transformedColliderBoxes),
                    inverseOrigin,
                    inverseX,
                    inverseY,
                    inverseZ
            );

            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem"
            );
            Class<?> serverSubLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            Object physicsSystem = physicsSystemClass.getMethod("get", Level.class).invoke(null, level);
            if (physicsSystem == null) {
                throw new IllegalStateException("Sable SubLevelPhysicsSystem is unavailable");
            }
            Object handle = physicsSystemClass.getMethod("getPhysicsHandle", serverSubLevelClass)
                    .invoke(physicsSystem, subLevel);
            if (handle == null) {
                throw new IllegalStateException("Sable rigid body handle is unavailable");
            }
            Object velocity = handle.getClass().getMethod("getLinearVelocity").invoke(handle);

            return new SableBodyMotion(
                    reflectedDouble(position, "x"),
                    reflectedDouble(position, "y"),
                    reflectedDouble(position, "z"),
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    reflectedDouble(velocity, "x"),
                    reflectedDouble(velocity, "y"),
                    reflectedDouble(velocity, "z"),
                    colliderGeometry
            );
        }

        private SablePoseGeometry nativePoseGeometry(ServerLevel level,
                                                     Object subLevel,
                                                     BlockPos targetPos,
                                                     VoxelShape[] slotShapes) throws ReflectiveOperationException {
            Class<?> physicsSystemClass = Class.forName(
                    "dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem"
            );
            Object physicsSystem = physicsSystemClass.getMethod("get", Level.class).invoke(null, level);
            if (physicsSystem == null) {
                throw new IllegalStateException("Sable SubLevelPhysicsSystem is unavailable");
            }
            Object pipeline = physicsSystemClass.getMethod("getPipeline").invoke(physicsSystem);
            if (pipeline == null) {
                throw new IllegalStateException("Sable physics pipeline is unavailable");
            }

            Class<?> poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3d");
            Class<?> poseInterface = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
            Object logicalPose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
            // readPose only overwrites native position/orientation. Seed the destination from the
            // full logical pose so rotationPoint and scale continue to describe plot geometry.
            Object pose = poseClass.getConstructor(poseInterface).newInstance(logicalPose);
            Method readPose = null;
            for (Method method : pipeline.getClass().getMethods()) {
                if (method.getName().equals("readPose")
                        && method.getParameterCount() == 2
                        && method.getParameterTypes()[0].isInstance(subLevel)
                        && method.getParameterTypes()[1].isInstance(pose)) {
                    readPose = method;
                    break;
                }
            }
            if (readPose == null) {
                throw new NoSuchMethodException(pipeline.getClass().getName() + ".readPose");
            }
            readPose.invoke(pipeline, subLevel, pose);

            Object position = pose.getClass().getMethod("position").invoke(pose);
            Object orientation = pose.getClass().getMethod("orientation").invoke(pose);
            Object rotationPoint = pose.getClass().getMethod("rotationPoint").invoke(pose);
            Object scale = pose.getClass().getMethod("scale").invoke(pose);
            SableRigidPose rigidPose = new SableRigidPose(
                    reflectedDouble(position, "x"),
                    reflectedDouble(position, "y"),
                    reflectedDouble(position, "z"),
                    reflectedDouble(orientation, "x"),
                    reflectedDouble(orientation, "y"),
                    reflectedDouble(orientation, "z"),
                    reflectedDouble(orientation, "w"),
                    reflectedDouble(rotationPoint, "x"),
                    reflectedDouble(rotationPoint, "y"),
                    reflectedDouble(rotationPoint, "z"),
                    reflectedDouble(scale, "x"),
                    reflectedDouble(scale, "y"),
                    reflectedDouble(scale, "z")
            );

            Method transformPosition = pose.getClass().getMethod("transformPosition", Vec3.class);
            List<List<Vec3>> slotVertices = new ArrayList<>(4);
            for (int index = 0; index < 4; index++) {
                VoxelShape shape = slotShapes != null && index < slotShapes.length
                        ? slotShapes[index]
                        : null;
                List<Vec3> vertices = new ArrayList<>();
                if (shape != null) {
                    for (AABB box : shape.toAabbs()) {
                        for (double x : new double[]{box.minX, box.maxX}) {
                            for (double y : new double[]{box.minY, box.maxY}) {
                                for (double z : new double[]{box.minZ, box.maxZ}) {
                                    vertices.add((Vec3) transformPosition.invoke(
                                            pose,
                                            new Vec3(
                                                    targetPos.getX() + x,
                                                    targetPos.getY() + y,
                                                    targetPos.getZ() + z
                                            )
                                    ));
                                }
                            }
                        }
                    }
                }
                slotVertices.add(List.copyOf(vertices));
            }
            return new SablePoseGeometry(rigidPose, List.copyOf(slotVertices));
        }

        private static double reflectedDouble(Object target, String methodName) throws ReflectiveOperationException {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            throw new NoSuchMethodException(target.getClass().getName() + "." + methodName
                    + " returned " + (value == null ? "null" : value.getClass().getName()));
        }

        private void remove(ServerLevel level, Object subLevel, List<String> details, String name) {
            try {
                if (subLevel == null || isRemoved(subLevel)) {
                    details.add(name + ".sublevel-remove=already-removed");
                    return;
                }
                Object container = getContainer.invoke(null, level);
                if (container == null) {
                    details.add(name + ".sublevel-remove=no-container");
                    return;
                }
                removeSubLevel.invoke(container, subLevel, removalReason);
                details.add(name + ".sublevel-remove=removed");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                details.add(name + ".sublevel-remove=failed:" + exception.getClass().getSimpleName());
            }
        }

        private static Class<?> sableClass(String primary, String fallback) throws ClassNotFoundException {
            try {
                return Class.forName(primary);
            } catch (ClassNotFoundException ignored) {
                return Class.forName(fallback);
            }
        }

        private static Method sableGetContainerMethod(Class<?> containerClass) throws NoSuchMethodException {
            try {
                return containerClass.getMethod("getContainer", ServerLevel.class);
            } catch (NoSuchMethodException ignored) {
                return containerClass.getMethod("getContainer", net.minecraft.world.level.Level.class);
            }
        }

        private static int invokeInt(Object target, String methodName) throws ReflectiveOperationException {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            if (value instanceof Number number) {
                return number.intValue();
            }
            throw new NoSuchMethodException(methodName);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Object sableRemovalReason(Class<?> removalReasonClass) {
            try {
                return Enum.valueOf(removalReasonClass.asSubclass(Enum.class), "REMOVED");
            } catch (IllegalArgumentException exception) {
                Object[] constants = removalReasonClass.getEnumConstants();
                return constants == null || constants.length == 0 ? null : constants[0];
            }
        }
    }

    private static final class SimAssemblerFixtureApi {
        private final Method assembleFromSingleBlock;
        private final Method resultSubLevel;
        private final Method resultOffset;

        private SimAssemblerFixtureApi(Method assembleFromSingleBlock,
                                       Method resultSubLevel,
                                       Method resultOffset) {
            this.assembleFromSingleBlock = assembleFromSingleBlock;
            this.resultSubLevel = resultSubLevel;
            this.resultOffset = resultOffset;
        }

        private static @Nullable SimAssemblerFixtureApi create(List<String> details) {
            try {
                Class<?> helperClass = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper");
                Class<?> resultClass = Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper$AssemblyResult");
                Method assembleFromSingleBlock = helperClass.getMethod(
                        "assembleFromSingleBlock",
                        Level.class,
                        BlockPos.class,
                        BlockPos.class,
                        boolean.class,
                        boolean.class
                );

                details.add("sim-assembler-api=available");
                return new SimAssemblerFixtureApi(
                        assembleFromSingleBlock,
                        resultClass.getMethod("subLevel"),
                        resultClass.getMethod("offset")
                );
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                details.add("sim-assembler-api=unavailable:" + exception.getClass().getSimpleName());
                return null;
            }
        }

        private @Nullable SimAssemblyResult assemble(Level level,
                                                     BlockPos assemblerPos,
                                                     BlockPos sourcePos) throws ReflectiveOperationException {
            Object result = assembleFromSingleBlock.invoke(null, level, assemblerPos, sourcePos, true, true);
            if (result == null) {
                return null;
            }

            Object subLevel = resultSubLevel.invoke(result);
            Object offset = resultOffset.invoke(result);
            return subLevel == null ? null : new SimAssemblyResult(
                    subLevel,
                    offset instanceof BlockPos blockPos ? blockPos.immutable() : null
            );
        }
    }

    private static final class SimPhysicsAssemblerFixtureApi {
        private final Block assemblerBlock;
        private final Method assembleOrDisassemble;

        private SimPhysicsAssemblerFixtureApi(Block assemblerBlock, Method assembleOrDisassemble) {
            this.assemblerBlock = assemblerBlock;
            this.assembleOrDisassemble = assembleOrDisassemble;
        }

        private static @Nullable SimPhysicsAssemblerFixtureApi create(List<String> details) {
            try {
                Class<?> blocksClass = Class.forName("dev.simulated_team.simulated.index.SimBlocks");
                Field physicsAssemblerField = blocksClass.getField("PHYSICS_ASSEMBLER");
                Object blockEntry = physicsAssemblerField.get(null);
                Method get = blockEntry.getClass().getMethod("get");
                Object blockObject = get.invoke(blockEntry);
                if (!(blockObject instanceof Block block)) {
                    details.add("sim-physics-assembler-api=unavailable:no-block");
                    return null;
                }

                Class<?> blockEntityClass = Class.forName("dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity");
                Method assembleOrDisassemble = blockEntityClass.getMethod("assembleOrDisassemble");

                details.add("sim-physics-assembler-api=available");
                return new SimPhysicsAssemblerFixtureApi(block, assembleOrDisassemble);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                details.add("sim-physics-assembler-api=unavailable:" + exception.getClass().getSimpleName());
                return null;
            }
        }

        private Block assemblerBlock() {
            return assemblerBlock;
        }

        private BlockState assemblerState(Direction facing) {
            BlockState state = assemblerBlock.defaultBlockState();
            state = setPropertyValueByName(state, "face", "wall");
            state = setPropertyValueByName(state, "facing", facing.toString());
            return state;
        }

        private void assembleOrDisassemble(BlockEntity assemblerEntity) throws ReflectiveOperationException {
            assembleOrDisassemble.invoke(assemblerEntity);
        }

        private static BlockState setPropertyValueByName(BlockState state, String propertyName, String valueName) {
            for (Property<?> property : state.getProperties()) {
                if (!property.getName().equals(propertyName)) {
                    continue;
                }
                for (Comparable<?> value : property.getPossibleValues()) {
                    if (String.valueOf(value).equalsIgnoreCase(valueName)) {
                        return setRawPropertyValue(state, property, value);
                    }
                }
            }
            return state;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static BlockState setRawPropertyValue(BlockState state, Property property, Comparable value) {
            return state.setValue(property, value);
        }
    }

    private static String blockEntityName(@Nullable BlockEntity blockEntity) {
        return blockEntity == null ? "none" : blockEntity.getClass().getSimpleName();
    }

    private record SimAssemblyResult(Object subLevel, @Nullable BlockPos offset) {
    }

    private record SableStepCase(String name,
                                 BlockPos sourcePos,
                                 BlockState sourceState,
                                 @Nullable BlockState[] sourceSlots,
                                 Block placementBlock,
                                 BlockState expectedExistingMaterial,
                                 BlockState expectedPlacedMaterial,
                                 Block expectedQuadBlock,
                                 Direction placementFace,
                                 Vec3 hitOffset) {
    }

    private static final class PhysicsMaterialProbe {
        private final ServerLevel level;
        private final SableFixtureApi sableApi;
        private final SablePhysicsMaterialCase materialCase;
        private final Block assemblerBlock;
        private final BlockPos assemblerPos;
        private final BlockPos sourcePos;
        private final BlockPos anchorPos;
        private final Map<BlockPos, BlockState> supportStates;
        private final List<String> details = new ArrayList<>();
        private Object subLevel;
        private Object initialSubLevel;
        private Set<Object> existingSubLevels = Collections.emptySet();
        private BlockGetter plotAccessor;
        private BlockPos targetPos;
        private final Set<Long> materialPositions = new LinkedHashSet<>();
        private Object assemblerSubLevel;
        private BlockPos assemblerTargetPos;
        private BlockPos accessorPos;
        private String subLevelType;
        private int physicsRefreshBeforeBreak;
        private int massRebuildBeforeBreak;
        private double actualSourceMass;
        private SableBodyMotion initialMotion;
        private long initialCalmSince;
        private long delayedCheckStartGameTime;
        private Object stableCandidateSubLevel;
        private BlockPos stableCandidateTargetPos;
        private long stableCandidateSince;
        private long terrainContactSince;
        private int checked;
        private boolean passed = true;
        private boolean fixtureBlocksPrepared;
        private boolean readyForAssembly;
        private boolean readyForInitialStability;
        private boolean readyForDelayedCheck;

        private PhysicsMaterialProbe(ServerLevel level,
                                     SableFixtureApi sableApi,
                                     SablePhysicsMaterialCase materialCase,
                                     Block assemblerBlock,
                                     BlockPos assemblerPos,
                                     BlockPos sourcePos,
                                     BlockPos anchorPos,
                                     Map<BlockPos, BlockState> supportStates) {
            this.level = level;
            this.sableApi = sableApi;
            this.materialCase = materialCase;
            this.assemblerBlock = assemblerBlock;
            this.assemblerPos = assemblerPos;
            this.sourcePos = sourcePos;
            this.anchorPos = anchorPos;
            this.supportStates = supportStates;
        }

        private ServerLevel level() { return level; }
        private SableFixtureApi sableApi() { return sableApi; }
        private SablePhysicsMaterialCase materialCase() { return materialCase; }
        private Block assemblerBlock() { return assemblerBlock; }
        private BlockPos assemblerPos() { return assemblerPos; }
        private BlockPos sourcePos() { return sourcePos; }
        private BlockPos anchorPos() { return anchorPos; }
        private Map<BlockPos, BlockState> supportStates() { return supportStates; }
        private List<String> details() { return details; }
        private Object subLevel() { return subLevel; }
        private void subLevel(Object value) {
            subLevel = value;
            if (initialSubLevel == null) {
                initialSubLevel = value;
            }
        }
        private Object initialSubLevel() { return initialSubLevel; }
        private Set<Object> existingSubLevels() { return existingSubLevels; }
        private void existingSubLevels(Set<Object> value) { existingSubLevels = value; }
        private BlockGetter plotAccessor() { return plotAccessor; }
        private void plotAccessor(BlockGetter value) { plotAccessor = value; }
        private BlockPos targetPos() { return targetPos; }
        private void targetPos(BlockPos value) {
            targetPos = value;
            if (value != null) {
                materialPositions.add(value.asLong());
            }
        }
        private Set<Long> materialPositions() { return materialPositions; }
        private Object assemblerSubLevel() { return assemblerSubLevel; }
        private void assemblerSubLevel(Object value) { assemblerSubLevel = value; }
        private BlockPos assemblerTargetPos() { return assemblerTargetPos; }
        private void assemblerTargetPos(BlockPos value) { assemblerTargetPos = value; }
        private BlockPos accessorPos() { return accessorPos; }
        private void accessorPos(BlockPos value) { accessorPos = value; }
        private String subLevelType() { return subLevelType; }
        private void subLevelType(String value) { subLevelType = value; }
        private int physicsRefreshBeforeBreak() { return physicsRefreshBeforeBreak; }
        private void physicsRefreshBeforeBreak(int value) { physicsRefreshBeforeBreak = value; }
        private int massRebuildBeforeBreak() { return massRebuildBeforeBreak; }
        private void massRebuildBeforeBreak(int value) { massRebuildBeforeBreak = value; }
        private double actualSourceMass() { return actualSourceMass; }
        private void actualSourceMass(double value) { actualSourceMass = value; }
        private SableBodyMotion initialMotion() { return initialMotion; }
        private void initialMotion(SableBodyMotion value) { initialMotion = value; }
        private long initialCalmSince() { return initialCalmSince; }
        private void initialCalmSince(long value) { initialCalmSince = value; }
        private long delayedCheckStartGameTime() { return delayedCheckStartGameTime; }
        private void delayedCheckStartGameTime(long value) { delayedCheckStartGameTime = value; }
        private boolean sameStableCandidate(SableMaterialLocation candidate) {
            return candidate != null
                    && stableCandidateSubLevel == candidate.subLevel()
                    && candidate.targetPos().equals(stableCandidateTargetPos);
        }
        private long stableCandidateSince() { return stableCandidateSince; }
        private void stableCandidate(SableMaterialLocation candidate, long gameTime) {
            stableCandidateSubLevel = candidate.subLevel();
            stableCandidateTargetPos = candidate.targetPos();
            stableCandidateSince = gameTime;
            terrainContactSince = 0L;
        }
        private long terrainContactSince() { return terrainContactSince; }
        private void terrainContactSince(long value) { terrainContactSince = value; }
        private void clearStableCandidate() {
            stableCandidateSubLevel = null;
            stableCandidateTargetPos = null;
            stableCandidateSince = 0L;
            terrainContactSince = 0L;
        }
        private boolean passed() { return passed; }
        private boolean fixtureBlocksPrepared() { return fixtureBlocksPrepared; }
        private void fixtureBlocksPrepared(boolean value) { fixtureBlocksPrepared = value; }
        private boolean readyForAssembly() { return readyForAssembly; }
        private void readyForAssembly(boolean value) { readyForAssembly = value; }
        private boolean readyForInitialStability() { return readyForInitialStability; }
        private void readyForInitialStability(boolean value) { readyForInitialStability = value; }
        private boolean readyForDelayedCheck() { return readyForDelayedCheck; }
        private void readyForDelayedCheck(boolean value) { readyForDelayedCheck = value; }
        private void checked() { checked++; }
        private void checked(int count) { checked += count; }
        private void fail(String detail) { passed = false; details.add(detail); }
        private void failedCheck(String detail) { checked++; fail(detail); }
        private CheckResult result() { return new CheckResult(passed, checked, details); }
    }

    private record SablePhysicsMaterialCase(String name,
                                            BlockPos sourcePos,
                                            BlockState sourceState,
                                            @Nullable BlockState[] sourceSlots,
                                            double expectedSlabEquivalents) {
        private boolean enhancedMaterialCase() {
            return sourceSlots != null;
        }
    }

    private record PhysicsTerrainSupport(BlockPos center, Map<BlockPos, BlockState> states) {
    }

    private record SableMaterialLocation(Object subLevel, BlockPos targetPos) {
    }

    private record SableExpectedMassElement(double mass,
                                            double centerX,
                                            double centerY,
                                            double centerZ,
                                            double sizeX,
                                            double sizeY,
                                            double sizeZ) {
    }

    private record SableExpectedMassProperties(double mass, Vec3 center, double[] inertia) {
    }

    private record SableTerrainContact(BlockPos terrainPos,
                                       BlockState terrainState,
                                        double terrainTop,
                                        double verticalGap,
                                        boolean collidableTerrain,
                                        boolean inContact,
                                        int scannedColumns) {
    }

    private record SableBodyMotion(double poseX,
                                   double poseY,
                                   double poseZ,
                                   double minX,
                                   double minY,
                                   double minZ,
                                   double maxX,
                                   double maxY,
                                   double maxZ,
                                   double velocityX,
                                   double velocityY,
                                   double velocityZ,
                                   SableColliderGeometry colliderGeometry) {
        private boolean finite() {
            return Double.isFinite(poseX)
                    && Double.isFinite(poseY)
                    && Double.isFinite(poseZ)
                    && Double.isFinite(minX)
                    && Double.isFinite(minY)
                    && Double.isFinite(minZ)
                    && Double.isFinite(maxX)
                    && Double.isFinite(maxY)
                    && Double.isFinite(maxZ)
                    && Double.isFinite(velocityX)
                    && Double.isFinite(velocityY)
                    && Double.isFinite(velocityZ)
                    && colliderGeometry != null;
        }

        private boolean orderedBounds() {
            return minX <= maxX && minY <= maxY && minZ <= maxZ;
        }

        private String description() {
            return String.format(
                    Locale.ROOT,
                    "pose:(%.4f,%.4f,%.4f),globalBounds:[(%.4f,%.4f,%.4f)-(%.4f,%.4f,%.4f)],velocity:(%.4f,%.4f,%.4f)",
                    poseX,
                    poseY,
                    poseZ,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    velocityX,
                    velocityY,
                    velocityZ
            );
        }
    }

    private record SableRigidPose(double x,
                                  double y,
                                  double z,
                                  double qx,
                                  double qy,
                                  double qz,
                                  double qw,
                                  double rotationX,
                                  double rotationY,
                                  double rotationZ,
                                  double scaleX,
                                  double scaleY,
                                  double scaleZ) {
    }

    private record SablePoseGeometry(SableRigidPose pose,
                                     List<List<Vec3>> slotVertices) {
    }

    private record SableHorizontalPoint(double x, double z) {
    }

    private record SableTransformedColliderBox(List<Vec3> vertices) {
        private void addFootprintCandidates(double minX,
                                            double maxX,
                                            double minZ,
                                            double maxZ,
                                            List<SableHorizontalPoint> candidates) {
            for (Vec3 vertex : vertices) {
                if (within(vertex.x, minX, maxX) && within(vertex.z, minZ, maxZ)) {
                    candidates.add(new SableHorizontalPoint(vertex.x, vertex.z));
                }
            }

            for (int i = 0; i < vertices.size(); i++) {
                for (int axisBit : new int[]{1, 2, 4}) {
                    int j = i ^ axisBit;
                    if (j <= i || j >= vertices.size()) {
                        continue;
                    }
                    Vec3 start = vertices.get(i);
                    Vec3 end = vertices.get(j);
                    addXBoundaryIntersection(start, end, minX, minZ, maxZ, candidates);
                    addXBoundaryIntersection(start, end, maxX, minZ, maxZ, candidates);
                    addZBoundaryIntersection(start, end, minZ, minX, maxX, candidates);
                    addZBoundaryIntersection(start, end, maxZ, minX, maxX, candidates);
                }
            }
        }

        private static void addXBoundaryIntersection(Vec3 start,
                                                     Vec3 end,
                                                     double boundaryX,
                                                     double minZ,
                                                     double maxZ,
                                                     List<SableHorizontalPoint> candidates) {
            double deltaX = end.x - start.x;
            if (Math.abs(deltaX) <= 1.0E-10D) {
                return;
            }
            double t = (boundaryX - start.x) / deltaX;
            if (t < -1.0E-9D || t > 1.0D + 1.0E-9D) {
                return;
            }
            double z = start.z + (end.z - start.z) * t;
            if (within(z, minZ, maxZ)) {
                candidates.add(new SableHorizontalPoint(boundaryX, z));
            }
        }

        private static void addZBoundaryIntersection(Vec3 start,
                                                     Vec3 end,
                                                     double boundaryZ,
                                                     double minX,
                                                     double maxX,
                                                     List<SableHorizontalPoint> candidates) {
            double deltaZ = end.z - start.z;
            if (Math.abs(deltaZ) <= 1.0E-10D) {
                return;
            }
            double t = (boundaryZ - start.z) / deltaZ;
            if (t < -1.0E-9D || t > 1.0D + 1.0E-9D) {
                return;
            }
            double x = start.x + (end.x - start.x) * t;
            if (within(x, minX, maxX)) {
                candidates.add(new SableHorizontalPoint(x, boundaryZ));
            }
        }

        private static boolean within(double value, double min, double max) {
            return value >= min - 1.0E-9D && value <= max + 1.0E-9D;
        }
    }

    private record SableColliderGeometry(List<AABB> localBoxes,
                                         List<SableTransformedColliderBox> transformedBoxes,
                                         Vec3 inverseOrigin,
                                         Vec3 inverseX,
                                         Vec3 inverseY,
                                         Vec3 inverseZ) {
        private double lowestYOverFootprint(double minX, double maxX, double minZ, double maxZ) {
            List<SableHorizontalPoint> candidates = new ArrayList<>();
            candidates.add(new SableHorizontalPoint(minX, minZ));
            candidates.add(new SableHorizontalPoint(minX, maxZ));
            candidates.add(new SableHorizontalPoint(maxX, minZ));
            candidates.add(new SableHorizontalPoint(maxX, maxZ));
            for (SableTransformedColliderBox box : transformedBoxes) {
                box.addFootprintCandidates(minX, maxX, minZ, maxZ, candidates);
            }

            double lowestY = Double.POSITIVE_INFINITY;
            for (SableHorizontalPoint candidate : candidates) {
                double candidateY = lowestYAt(candidate.x(), candidate.z());
                if (Double.isFinite(candidateY)) {
                    lowestY = Math.min(lowestY, candidateY);
                }
            }
            return lowestY;
        }

        private double lowestYAt(double worldX, double worldZ) {
            Vec3 localRayOrigin = inverseOrigin
                    .add(inverseX.scale(worldX))
                    .add(inverseZ.scale(worldZ));
            double lowestY = Double.POSITIVE_INFINITY;
            for (AABB box : localBoxes) {
                double entryY = rayBoxEntry(localRayOrigin, inverseY, box);
                if (Double.isFinite(entryY)) {
                    lowestY = Math.min(lowestY, entryY);
                }
            }
            return lowestY;
        }

        private static double rayBoxEntry(Vec3 origin, Vec3 direction, AABB box) {
            double entry = Double.NEGATIVE_INFINITY;
            double exit = Double.POSITIVE_INFINITY;
            double[] origins = {origin.x, origin.y, origin.z};
            double[] directions = {direction.x, direction.y, direction.z};
            double[] minimums = {box.minX, box.minY, box.minZ};
            double[] maximums = {box.maxX, box.maxY, box.maxZ};
            for (int axis = 0; axis < 3; axis++) {
                double axisDirection = directions[axis];
                if (Math.abs(axisDirection) <= 1.0E-12D) {
                    if (origins[axis] < minimums[axis] - 1.0E-8D
                            || origins[axis] > maximums[axis] + 1.0E-8D) {
                        return Double.NaN;
                    }
                    continue;
                }
                double first = (minimums[axis] - origins[axis]) / axisDirection;
                double second = (maximums[axis] - origins[axis]) / axisDirection;
                entry = Math.max(entry, Math.min(first, second));
                exit = Math.min(exit, Math.max(first, second));
                if (entry > exit + 1.0E-8D) {
                    return Double.NaN;
                }
            }
            return entry;
        }
    }

    private record SableHeatMapStatus(boolean stable, String description) {
    }

    private record SableVoxelNeighborhoodStatus(boolean nonEmpty, String description) {
    }

    private record SableGatherResult(Set<BlockPos> blocks, int checkedBlocks, Object boundingBox, String state) {
    }

    private record FixtureRequest(String runner, String target, String scenario, String phase, String basePos) {
    }

    private record FixtureResult(String target, boolean passed, int checked, int skipped, List<String> details) {
    }

    private record CheckResult(boolean passed, int checked, List<String> details) {
    }

    @FunctionalInterface
    private interface FixtureCheckTask {
        CheckResult run() throws Exception;
    }

    private record MutableFixtureContext(Minecraft client,
                                         ServerLevel serverLevel,
                                         @Nullable Player player,
                                         @Nullable ServerPlayer serverPlayer,
                                         BlockPos basePos,
                                         List<String> details) {
    }

    private record MutableFixtureSpec(List<MutableFixturePoint> points,
                                      MutableFixturePoint destroyPoint,
                                      MutableFixturePoint pistonSource,
                                      MutableFixturePoint pistonDest,
                                      MutableFixturePoint particlePoint,
                                      MutableFixturePoint snapshotPoint) {
        private static MutableFixtureSpec standard(BlockPos basePos) {
            BlockState oak = Blocks.OAK_SLAB.defaultBlockState();
            BlockState spruce = Blocks.SPRUCE_SLAB.defaultBlockState();
            BlockState stoneBrick = Blocks.STONE_BRICK_SLAB.defaultBlockState();
            BlockState cutCopper = Blocks.CUT_COPPER_SLAB.defaultBlockState();
            BlockState exposedCopper = Blocks.EXPOSED_CUT_COPPER_SLAB.defaultBlockState();

            MutableFixturePoint mixed = point(
                    "mutable-mixed-slab",
                    basePos,
                    CommonBlocks.MIXED_SLAB.defaultBlockState(),
                    oak,
                    spruce,
                    null,
                    null
            );
            MutableFixturePoint verticalSlab = point(
                    "mutable-vertical-slab",
                    basePos.offset(1, 0, 0),
                    CommonBlocks.VERTICAL_SLAB.defaultBlockState().setValue(VerticalSlabBlock.DOUBLE, true),
                    stoneBrick,
                    oak,
                    null,
                    null
            );
            MutableFixturePoint quadStep = point(
                    "mutable-quad-step",
                    basePos.offset(2, 0, 0),
                    CommonBlocks.QUAD_STEP.defaultBlockState(),
                    spruce,
                    oak,
                    cutCopper,
                    stoneBrick
            );
            MutableFixturePoint quadVerticalStep = point(
                    "mutable-quad-vertical-step",
                    basePos.offset(3, 0, 0),
                    CommonBlocks.QUAD_VERTICAL_STEP.defaultBlockState(),
                    stoneBrick,
                    stoneBrick,
                    oak,
                    spruce
            );
            MutableFixturePoint step = point(
                    "mutable-step",
                    basePos.offset(4, 0, 0),
                    CommonBlocks.STEP.defaultBlockState(),
                    exposedCopper,
                    null,
                    null,
                    null
            );
            MutableFixturePoint verticalStep = point(
                    "mutable-vertical-step",
                    basePos.offset(5, 0, 0),
                    CommonBlocks.VERTICAL_STEP.defaultBlockState(),
                    spruce,
                    null,
                    null,
                    null
            );
            MutableFixturePoint pistonDest = point(
                    "mutable-piston-destination",
                    basePos.offset(2, 0, 1),
                    CommonBlocks.QUAD_STEP.defaultBlockState(),
                    spruce,
                    oak,
                    cutCopper,
                    stoneBrick
            );

            return new MutableFixtureSpec(
                    List.of(mixed, verticalSlab, quadStep, quadVerticalStep, step, verticalStep),
                    mixed,
                    quadStep,
                    pistonDest,
                    quadStep,
                    quadVerticalStep
            );
        }

        private static MutableFixturePoint point(String name,
                                                 BlockPos pos,
                                                 BlockState containerState,
                                                 @Nullable BlockState slot0,
                                                 @Nullable BlockState slot1,
                                                 @Nullable BlockState slot2,
                                                 @Nullable BlockState slot3) {
            BlockState[] slots = new BlockState[]{slot0, slot1, slot2, slot3};
            return new MutableFixturePoint(name, pos, containerState, slots, describeSlots(slots));
        }
    }

    private record MutableFixturePoint(String name,
                                       BlockPos pos,
                                       BlockState containerState,
                                       BlockState[] slots,
                                       String expectedSlots) {
    }

    private record LegacyMigrationPoint(String legacyId, MutableFixturePoint fixture) {
    }

    private record FixedLegacyMigrationPoint(LegacyMigrationPoint migration, BlockState expectedState) {
    }

    private record CollapseCase(String name,
                                BlockPos pos,
                                BlockState containerState,
                                BlockState[] slots,
                                @Nullable Block expectedBlock) {
    }

    private record FixturePoint(String name, BlockPos pos, String expectedSlots) {
        private static List<FixturePoint> forTarget(String target) {
            return switch (target) {
                case "1.21.1-fabric" -> List.of(
                        point("vinery-dirt-path-quads", 2957, 72, -1895, "vinery:dirt_path_slab,vinery:dirt_path_slab,vinery:dirt_path_slab,vinery:dirt_path_slab"),
                        point("vinery-grass-variants", 2958, 69, -1868, "vinery:grass_slab,_,_,_"),
                        point("mixed-oak-spruce", 2959, 74, -1895, "minecraft:oak_slab,minecraft:spruce_slab,_,_"),
                        point("architects-palette-quad", 3016, 87, -1957, "architects_palette:myonite_brick_slab,architects_palette:olivestone_brick_slab,architects_palette:olivestone_tile_slab,architects_palette:abyssaline_tile_slab")
                );
                case "1.21.1-neoforge" -> List.of(
                        point("architects-quad", -1, 63, -291, "architects_palette:olivestone_tile_slab,architects_palette:myonite_slab,architects_palette:myonite_slab,architects_palette:olivestone_tile_slab"),
                        point("architects-single-slot", 0, 63, -291, "architects_palette:olivestone_tile_slab,_,_,_"),
                        point("oak-spruce-mixed", 13, 65, -289, "minecraft:spruce_slab,minecraft:oak_slab,_,_"),
                        point("full-quad-oak-spruce", -28, 71, -354, "minecraft:oak_slab,minecraft:spruce_slab,minecraft:spruce_slab,minecraft:oak_slab")
                );
                case "1.21.11-fabric" -> List.of(
                        point("oak-copper-mix", 1537, 68, 552, "minecraft:oak_slab,minecraft:oxidized_cut_copper_slab,_,_"),
                        point("full-oak-copper-quad", 1534, 68, 543, "minecraft:oak_slab,minecraft:oxidized_cut_copper_slab,minecraft:oak_slab,minecraft:oxidized_cut_copper_slab"),
                        point("stone-brick-quad", 1507, 66, 554, "minecraft:stone_brick_slab,minecraft:stone_brick_slab,minecraft:stone_brick_slab,minecraft:stone_brick_slab")
                );
                case "26.1-fabric", "26.2-fabric", "26.3-fabric" -> List.of(
                        point("spruce-oak-quad-step", -1823, 64, -596, "minecraft:spruce_slab,minecraft:oak_slab,minecraft:oak_slab,minecraft:spruce_slab"),
                        point("spruce-oak-full-quad-step", -1822, 64, -597, "minecraft:spruce_slab,minecraft:spruce_slab,minecraft:oak_slab,minecraft:oak_slab"),
                        point("copper-spruce-quad-step", -1829, 64, -587, "minecraft:exposed_cut_copper_slab,minecraft:spruce_slab,_,minecraft:cut_copper_slab"),
                        point("stone-brick-quad-vertical-step", -1826, 64, -584, "minecraft:stone_brick_slab,minecraft:stone_brick_slab,minecraft:stone_brick_slab,minecraft:stone_brick_slab")
                );
                default -> List.of();
            };
        }

        private static FixturePoint point(String name, int x, int y, int z, String expectedSlots) {
            return new FixturePoint(name, new BlockPos(x, y, z), expectedSlots);
        }
    }
}
