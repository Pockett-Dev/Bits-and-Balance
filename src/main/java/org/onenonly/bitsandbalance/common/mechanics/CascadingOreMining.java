package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.WeakHashMap;

/** Server-only, one-block-at-a-time ore mining for the Cascading enchantment. */
public final class CascadingOreMining {
    private static final Map<ServerPlayer, Sequence> ACTIVE = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_BREAK = ThreadLocal.withInitial(() -> false);
    private static final int TICKS_BETWEEN_BLOCKS = 3;
    private static final int MAX_CONNECTED_ORES_TO_SCAN = 8192;
    private static final Set<String> VANILLA_OVERWORLD_ORES = Set.of(
            "coal_ore", "iron_ore", "copper_ore", "gold_ore", "diamond_ore",
            "emerald_ore", "lapis_ore", "redstone_ore");
    public static volatile int configuredMaxLevel = 1;
    public static volatile boolean enabled = true;

    private CascadingOreMining() {}

    public static boolean isInternalBreak() {
        return INTERNAL_BREAK.get();
    }

    /** maxBlocks includes the ore the player broke by hand. */
    public static void start(ServerPlayer player, BlockPos origin, BlockState state, boolean enabled,
                             int maxActiveLevel, int levelOne, int levelTwo, int levelThree) {
        if (!enabled || isInternalBreak() || player.isShiftKeyDown() || player.isCreative() || !isOre(state)) return;
        ItemStack tool = player.getMainHandItem();
        int level = enchantmentLevel(tool);
        if (level < 1) return;
        level = Math.min(level, Math.max(1, Math.min(3, maxActiveLevel)));
        int maxBlocks = switch (level) {
            case 1 -> levelOne;
            case 2 -> levelTwo;
            default -> levelThree;
        };
        maxBlocks = Math.max(1, Math.min(64, maxBlocks));
        if (maxBlocks <= 1) return;

        Sequence sequence = new Sequence((ServerLevel) player.level(), origin.immutable(), state.getBlock(), oreFamily(state), tool,
                maxBlocks - 1);
        ACTIVE.put(player, sequence);
    }

    public static void tickAll(MinecraftServer server) {
        Iterator<Map.Entry<ServerPlayer, Sequence>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerPlayer, Sequence> entry = iterator.next();
            ServerPlayer player = entry.getKey();
            if (player == null || entry.getValue().level.getServer() != server || !advance(player, entry.getValue())) {
                iterator.remove();
            }
        }
    }

    public static void tick(ServerPlayer player) {
        Sequence sequence = ACTIVE.get(player);
        if (sequence != null && !advance(player, sequence)) ACTIVE.remove(player);
    }

    private static boolean advance(ServerPlayer player, Sequence sequence) {
        if (!enabled || player.isRemoved() || !player.isAlive() || player.level() != sequence.level
                || player.isShiftKeyDown() || player.getMainHandItem() != sequence.tool
                || enchantmentLevel(sequence.tool) < 1 || sequence.remaining <= 0) return false;
        // NeoForge's break event fires before the original block is removed. A canceled break must not cascade.
        if (sequence.level.getBlockState(sequence.origin).getBlock() == sequence.originBlock) return false;
        if (!sequence.discovered && !sequence.discoverConnectedOres()) return false;
        if (--sequence.cooldown > 0) return true;
        sequence.cooldown = TICKS_BETWEEN_BLOCKS;

        while (!sequence.pending.isEmpty()) {
            BlockPos pos = sequence.pending.remove();
            if (!sequence.level.hasChunkAt(pos)) continue;
            BlockState state = sequence.level.getBlockState(pos);
            if (!isOre(state) || !sequence.family.equals(oreFamily(state))) continue;
            boolean broken;
            INTERNAL_BREAK.set(true);
            try {
                broken = player.gameMode.destroyBlock(pos);
            } finally {
                INTERNAL_BREAK.set(false);
            }
            if (!broken || sequence.level.getBlockState(pos).getBlock() == state.getBlock()) continue;
            // ServerPlayerGameMode handles drops and tool wear, but the client normally
            // creates the break effect for a hand-mined block. These server-driven
            // breaks need the vanilla event so every observer hears and sees them.
            // 2001 is sound + particles on every maintained version. In 26.3,
            // PARTICLES_DESTROY_BLOCK means particles only (event 2014).
            sequence.level.levelEvent(2001, pos, Block.getId(state));
            sequence.remaining--;
            if (sequence.remaining <= 0) return false;
            return !sequence.pending.isEmpty();
        }
        return false;
    }

    private static int enchantmentLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var holder : enchantments.keySet()) {
            if (holder.unwrapKey().map(key -> {
//? if mc == 1.21.1
                return key.location().toString().equals("bitsandbalance:cascading");
//? else
                return key.identifier().toString().equals("bitsandbalance:cascading");
//? endif
            })
                    .orElse(false)) return enchantments.getLevel(holder);
        }
        return 0;
    }

    private static boolean isOre(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("_ore");
    }

    private static String oreFamily(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        for (String prefix : new String[]{"deepslate_", "andesite_", "diorite_", "granite_", "tuff_"}) {
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                break;
            }
        }
        String namespace = id.getNamespace();
        if (namespace.equals("bitsandbalance") && VANILLA_OVERWORLD_ORES.contains(path)) {
            namespace = "minecraft";
        }
        return namespace + ':' + path;
    }

    private static final class Sequence {
        final ServerLevel level;
        final BlockPos origin;
        final net.minecraft.world.level.block.Block originBlock;
        final String family;
        final ItemStack tool;
        final PriorityQueue<BlockPos> pending;
        boolean discovered;
        int remaining;
        int cooldown = TICKS_BETWEEN_BLOCKS;

        Sequence(ServerLevel level, BlockPos origin, net.minecraft.world.level.block.Block originBlock,
                 String family, ItemStack tool, int remaining) {
            this.level = level;
            this.origin = origin;
            this.originBlock = originBlock;
            this.family = family;
            this.tool = tool;
            this.remaining = remaining;
            this.pending = new PriorityQueue<>(Comparator
                    .comparingLong((BlockPos pos) -> distanceSquared(origin, pos))
                    .thenComparingInt(BlockPos::getY)
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ));
        }

        boolean discoverConnectedOres() {
            discovered = true;
            ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
            Set<BlockPos> seen = new HashSet<>();
            frontier.add(origin);
            seen.add(origin);
            int oreCount = 1;
            while (!frontier.isEmpty()) {
                BlockPos center = frontier.removeFirst();
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos neighbor = center.offset(dx, dy, dz);
                            if (!seen.add(neighbor) || !level.hasChunkAt(neighbor)) continue;
                            BlockState state = level.getBlockState(neighbor);
                            if (!isOre(state) || !family.equals(oreFamily(state))) continue;
                            // The full connected set must be known before the nearest ore is chosen.
                            // Abort an extreme vein rather than mine in an incorrect order or stall the server.
                            if (++oreCount > MAX_CONNECTED_ORES_TO_SCAN) return false;
                            frontier.addLast(neighbor);
                            pending.add(neighbor);
                        }
                    }
                }
            }
            return !pending.isEmpty();
        }
    }

    private static long distanceSquared(BlockPos origin, BlockPos pos) {
        long dx = (long) pos.getX() - origin.getX();
        long dy = (long) pos.getY() - origin.getY();
        long dz = (long) pos.getZ() - origin.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
