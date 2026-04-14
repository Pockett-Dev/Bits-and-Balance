package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PistonBlockEntityMoveRuntime {
    public static volatile boolean enabled = true;

    private static final List<String> DEFAULT_EXACT_BLACKLIST = List.of(
            "minecraft:spawner",
            "minecraft:trial_spawner",
            "minecraft:vault"
    );

    private static final List<String> DEFAULT_NAME_BLACKLIST = List.of(
            "spawner",
            "trial",
            "vault"
    );
        private static final Set<String> DEFAULT_AFFECTED_BLOCK_IDS = Set.copyOf(bitsandbalance$computeDefaultAffectedBlockIds());

    private static volatile Set<String> exactBlacklistBlockIds = Set.copyOf(DEFAULT_EXACT_BLACKLIST);
    private static volatile List<String> blacklistNameContains = List.copyOf(DEFAULT_NAME_BLACKLIST);

    private PistonBlockEntityMoveRuntime() {
    }

        public static void applyConfig(boolean enabledValue,
                       List<String> configuredExactBlacklistBlockIds,
                                   List<String> configuredBlacklistNameContains) {
        enabled = enabledValue;
        exactBlacklistBlockIds = Set.copyOf(bitsandbalance$normalizeIdList(
                configuredExactBlacklistBlockIds == null || configuredExactBlacklistBlockIds.isEmpty()
                        ? DEFAULT_EXACT_BLACKLIST
                        : configuredExactBlacklistBlockIds
        ));
        blacklistNameContains = List.copyOf(bitsandbalance$normalizeKeywordList(
                configuredBlacklistNameContains == null || configuredBlacklistNameContains.isEmpty()
                        ? DEFAULT_NAME_BLACKLIST
                        : configuredBlacklistNameContains
        ));
    }

    public static List<String> defaultExactBlacklistBlockIds() {
        return DEFAULT_EXACT_BLACKLIST;
    }

    public static List<String> defaultBlacklistNameContains() {
        return DEFAULT_NAME_BLACKLIST;
    }

    public static boolean allows(@Nullable BlockState state) {
        if (state == null || !state.hasBlockEntity()) return false;
        if (bitsandbalance$isIntrinsicExclusion(state.getBlock())) return false;

        Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) return false;

        String fullId = key.toString().toLowerCase(Locale.ROOT);
        String namespace = key.getNamespace().toLowerCase(Locale.ROOT);
        String path = key.getPath().toLowerCase(Locale.ROOT);

        if (exactBlacklistBlockIds.contains(fullId)) {
            return false;
        }
        for (String token : blacklistNameContains) {
            if (!token.isEmpty() && (path.contains(token) || fullId.contains(token))) {
                return false;
            }
        }

        if (!"minecraft".equals(namespace)) {
            return true;
        }

        return DEFAULT_AFFECTED_BLOCK_IDS.contains(fullId);
    }

    private static boolean bitsandbalance$isIntrinsicExclusion(Block block) {
        return block instanceof MovingPistonBlock
                || block instanceof MixedSlabBlock
                || block instanceof VerticalSlabBlock
                || block instanceof StepBlock
                || block instanceof VerticalStepBlock
                || block instanceof QuadStepBlock
                || block instanceof QuadVerticalStepBlock;
    }

    private static List<String> bitsandbalance$computeDefaultAffectedBlockIds() {
        ArrayList<String> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState defaultState = block.defaultBlockState();
            if (!defaultState.hasBlockEntity()) continue;
            if (bitsandbalance$isIntrinsicExclusion(block)) continue;

            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null) continue;

            String fullId = key.toString().toLowerCase(Locale.ROOT);
            String path = key.getPath().toLowerCase(Locale.ROOT);
            if (DEFAULT_EXACT_BLACKLIST.contains(fullId)) continue;

            boolean blockedByKeyword = false;
            for (String token : DEFAULT_NAME_BLACKLIST) {
                if (path.contains(token)) {
                    blockedByKeyword = true;
                    break;
                }
            }
            if (blockedByKeyword) continue;

            out.add(fullId);
        }
        out.sort(String::compareTo);
        return out;
    }

    private static List<String> bitsandbalance$normalizeIdList(List<String> ids) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null) continue;
            String cleaned = id.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        return List.copyOf(normalized);
    }

    private static List<String> bitsandbalance$normalizeKeywordList(List<String> keywords) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword == null) continue;
            String cleaned = keyword.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        return List.copyOf(normalized);
    }
}