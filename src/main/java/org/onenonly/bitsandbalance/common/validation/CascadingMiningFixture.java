package org.onenonly.bitsandbalance.common.validation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Guarded scratch-world fixture for event registration, pacing, cap, and crouch bypass. */
public final class CascadingMiningFixture {
    private static final int ORE_COUNT = 10;

    private CascadingMiningFixture() {}

    public static Result check(ServerLevel level, ServerPlayer player, BlockPos origin) {
        List<String> details = new ArrayList<>();
        if (player == null) return new Result(false, 1, List.of("cascading=missing-player"));

        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
//? if mc == 1.21.1
                ResourceLocation.fromNamespaceAndPath("bitsandbalance", "cascading")
//? else
                Identifier.fromNamespaceAndPath("bitsandbalance", "cascading")
//? endif
        );
        Holder<Enchantment> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(key).orElse(null);
        if (enchantment == null) return new Result(false, 1, List.of("cascading=missing-enchantment"));

        BlockState[] previous = new BlockState[ORE_COUNT];
        for (int x = 0; x < ORE_COUNT; x++) {
            BlockPos pos = origin.offset(x, 0, 0);
            if (!level.hasChunkAt(pos)) level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            previous[x] = level.getBlockState(pos);
            if (!previous[x].isAir()) return new Result(false, 1, List.of("cascading=fixture-space-not-air:" + pos));
        }
        BlockPos nearerOrderTestPos = origin.offset(1, -1, 0);
        if (!level.hasChunkAt(nearerOrderTestPos)) {
            level.getChunk(nearerOrderTestPos.getX() >> 4, nearerOrderTestPos.getZ() >> 4);
        }
        BlockState previousOrderTest = level.getBlockState(nearerOrderTestPos);
        if (!previousOrderTest.isAir()) {
            return new Result(false, 1, List.of("cascading=fixture-space-not-air:" + nearerOrderTestPos));
        }

        ItemStack previousTool = player.getItemInHand(InteractionHand.MAIN_HAND);
        GameType previousMode = player.gameMode.getGameModeForPlayer();
        boolean previousCrouch = player.isShiftKeyDown();
        int previousMaxLevel = CascadingOreMining.configuredMaxLevel;
        AABB region = new AABB(origin.getX() - 1, origin.getY() - 1, origin.getZ() - 1,
                origin.getX() + ORE_COUNT + 1, origin.getY() + 2, origin.getZ() + 2);
        Set<UUID> existingEntities = new HashSet<>();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, region)) existingEntities.add(entity.getUUID());

        int checked = 0;
        boolean passed = true;
        try {
            ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
            tool.enchant(enchantment, 1);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setShiftKeyDown(false);

            boolean defaultLevel = enchantment.value().getMaxLevel() == 1;
            checked++;
            passed &= defaultLevel;
            details.add("cascading.defaultLevel=" + enchantment.value().getMaxLevel());
            boolean treasure = enchantment.is(enchantmentTag("treasure"));
            boolean tradeable = enchantment.is(enchantmentTag("tradeable"));
            boolean enchantingTableExcluded = !enchantment.is(enchantmentTag("in_enchanting_table"));
            checked += 3;
            passed &= treasure && tradeable && enchantingTableExcluded;
            details.add("cascading.availability=treasure:" + treasure + ",tradeable:" + tradeable
                    + ",enchantingTableExcluded:" + enchantingTableExcluded);
            LootParams chestParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                    .create(LootContextParamSets.CHEST);
            for (String chest : List.of("abandoned_mineshaft", "stronghold_corridor",
                    "stronghold_crossing", "stronghold_library", "simple_dungeon")) {
                int books = cascadingBooksInSample(level, chestParams, enchantment, chest);
                checked++;
                passed &= books > 0 && books < 100;
                details.add("cascading.loot." + chest + "=" + books + "/1024");
            }

            seed(level, origin);
            boolean originalBroken = player.gameMode.destroyBlock(origin);
            checked++;
            passed &= originalBroken && countRemaining(level, origin) == 9;
            boolean originalWear = tool.getDamageValue() == 1;
            checked++;
            passed &= originalWear;
            CascadingOreMining.tick(player);
            CascadingOreMining.tick(player);
            boolean delayed = countRemaining(level, origin) == 9;
            checked++;
            passed &= delayed;
            CascadingOreMining.tick(player);
            boolean oneAtATime = countRemaining(level, origin) == 8;
            checked++;
            passed &= oneAtATime;
            boolean firstExtraWear = tool.getDamageValue() == 2;
            checked++;
            passed &= firstExtraWear;
            boolean everyExtraWear = true;
            for (int totalBroken = 3; totalBroken <= 8; totalBroken++) {
                for (int tick = 0; tick < 3; tick++) CascadingOreMining.tick(player);
                everyExtraWear &= countRemaining(level, origin) == ORE_COUNT - totalBroken
                        && tool.getDamageValue() == totalBroken;
            }
            checked++;
            passed &= everyExtraWear;
            boolean cap = countRemaining(level, origin) == 2;
            checked++;
            passed &= cap;
            boolean exactWearAtCap = tool.getDamageValue() == 8;
            checked++;
            passed &= exactWearAtCap;
            details.add("cascading.sequence=original:" + originalBroken + ",delayed:" + delayed
                    + ",oneAtATime:" + oneAtATime + ",remainingAfterCap:" + countRemaining(level, origin)
                    + ",wearAfterEight:" + tool.getDamageValue() + ",wearEveryBlock:" + everyExtraWear);

            seed(level, origin);
            level.setBlock(nearerOrderTestPos, Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(), Block.UPDATE_ALL);
            player.gameMode.destroyBlock(origin);
            for (int tick = 0; tick < 3; tick++) CascadingOreMining.tick(player);
            boolean closestFirst = level.getBlockState(origin.east()).isAir()
                    && level.getBlockState(nearerOrderTestPos).is(Blocks.DEEPSLATE_COAL_ORE);
            checked++;
            passed &= closestFirst;
            for (int tick = 0; tick < 3; tick++) CascadingOreMining.tick(player);
            boolean diagonalBeforeFarther = level.getBlockState(nearerOrderTestPos).isAir()
                    && level.getBlockState(origin.offset(2, 0, 0)).is(Blocks.COAL_ORE);
            checked++;
            passed &= diagonalBeforeFarther;
            boolean ascendingAfterBranch = true;
            for (int x = 2; x <= 6; x++) {
                for (int tick = 0; tick < 3; tick++) CascadingOreMining.tick(player);
                ascendingAfterBranch &= level.getBlockState(origin.offset(x, 0, 0)).isAir()
                        && !level.getBlockState(origin.offset(x + 1, 0, 0)).isAir();
            }
            checked++;
            passed &= ascendingAfterBranch;
            details.add("cascading.closestFirst=" + closestFirst
                    + ",diagonalBeforeFarther=" + diagonalBeforeFarther
                    + ",ascendingAfterBranch=" + ascendingAfterBranch);
            level.setBlock(nearerOrderTestPos, previousOrderTest, Block.UPDATE_ALL);

            seed(level, origin);
            player.setShiftKeyDown(true);
            boolean crouchedOriginal = player.gameMode.destroyBlock(origin);
            for (int tick = 0; tick < 25; tick++) CascadingOreMining.tick(player);
            boolean crouchBypass = crouchedOriginal && countRemaining(level, origin) == 9;
            checked++;
            passed &= crouchBypass;
            details.add("cascading.crouchBypass=" + crouchBypass);

            seed(level, origin);
            player.setShiftKeyDown(false);
            player.gameMode.destroyBlock(origin);
            for (int tick = 0; tick < 3; tick++) CascadingOreMining.tick(player);
            player.setShiftKeyDown(true);
            for (int tick = 0; tick < 25; tick++) CascadingOreMining.tick(player);
            boolean crouchStops = countRemaining(level, origin) == 8;
            checked++;
            passed &= crouchStops;
            details.add("cascading.crouchStops=" + crouchStops);

            CascadingOreMining.configuredMaxLevel = 3;
            boolean configurableLevel = enchantment.value().getMaxLevel() == 3;
            checked++;
            passed &= configurableLevel;
            details.add("cascading.configuredMaxLevel=" + enchantment.value().getMaxLevel());

            player.setShiftKeyDown(false);
            tool.enchant(enchantment, 2);
            seed(level, origin);
            player.gameMode.destroyBlock(origin);
            CascadingOreMining.start(player, origin, Blocks.COAL_ORE.defaultBlockState(), true, 3, 8, 4, 6);
            for (int tick = 0; tick < 30; tick++) CascadingOreMining.tick(player);
            boolean levelTwoLimit = countRemaining(level, origin) == 6;
            checked++;
            passed &= levelTwoLimit;
            details.add("cascading.levelTwoCustomLimit=" + levelTwoLimit);

            tool.enchant(enchantment, 3);
            seed(level, origin);
            player.gameMode.destroyBlock(origin);
            CascadingOreMining.start(player, origin, Blocks.COAL_ORE.defaultBlockState(), true, 3, 8, 4, 6);
            for (int tick = 0; tick < 30; tick++) CascadingOreMining.tick(player);
            boolean levelThreeLimit = countRemaining(level, origin) == 4;
            checked++;
            passed &= levelThreeLimit;
            details.add("cascading.levelThreeCustomLimit=" + levelThreeLimit);
        } finally {
            player.setShiftKeyDown(true);
            CascadingOreMining.tick(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, previousTool);
            player.gameMode.changeGameModeForPlayer(previousMode);
            player.setShiftKeyDown(previousCrouch);
            CascadingOreMining.configuredMaxLevel = previousMaxLevel;
            for (int x = 0; x < ORE_COUNT; x++) {
                level.setBlock(origin.offset(x, 0, 0), previous[x], Block.UPDATE_ALL);
            }
            level.setBlock(nearerOrderTestPos, previousOrderTest, Block.UPDATE_ALL);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, region)) {
                if (!existingEntities.contains(entity.getUUID())
                        && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) entity.discard();
            }
        }
        return new Result(passed, checked, details);
    }

    private static void seed(ServerLevel level, BlockPos origin) {
        for (int x = 0; x < ORE_COUNT; x++) {
            level.setBlock(origin.offset(x, 0, 0),
                    (x & 1) == 0 ? Blocks.COAL_ORE.defaultBlockState() : Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(),
                    Block.UPDATE_ALL);
        }
    }

    private static TagKey<Enchantment> enchantmentTag(String path) {
        return TagKey.create(Registries.ENCHANTMENT,
//? if mc == 1.21.1
                ResourceLocation.fromNamespaceAndPath("minecraft", path)
//? else
                Identifier.fromNamespaceAndPath("minecraft", path)
//? endif
        );
    }

    private static int cascadingBooksInSample(ServerLevel level, LootParams params,
                                               Holder<Enchantment> enchantment, String chest) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
//? if mc == 1.21.1
                ResourceLocation.fromNamespaceAndPath("minecraft", "chests/" + chest)
//? else
                Identifier.fromNamespaceAndPath("minecraft", "chests/" + chest)
//? endif
        );
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        int books = 0;
        for (long seed = 0; seed < 1024; seed++) {
            for (ItemStack stack : table.getRandomItems(params, seed)) {
                if (!stack.is(Items.ENCHANTED_BOOK)) continue;
                ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
                        ItemEnchantments.EMPTY);
                if (stored.getLevel(enchantment) == 1) books++;
            }
        }
        return books;
    }

    private static int countRemaining(ServerLevel level, BlockPos origin) {
        int count = 0;
        for (int x = 0; x < ORE_COUNT; x++) {
            Block block = level.getBlockState(origin.offset(x, 0, 0)).getBlock();
            if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) count++;
        }
        return count;
    }

    public record Result(boolean passed, int checked, List<String> details) {}
}
