package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

/**
 * Client-only helper for rendering blue (soul) fire when the player is burning
 * due to soul-fire-related blocks.
 */
public final class SoulFireOverlayClient {
    private SoulFireOverlayClient() {
    }

    private static final Identifier FIRE_SPRITE_0 = Identifier.parse("minecraft:block/fire_0");
    private static final Identifier FIRE_SPRITE_1 = Identifier.parse("minecraft:block/fire_1");
    private static final Identifier SOUL_FIRE_SPRITE_0 = Identifier.parse("minecraft:block/soul_fire_0");
    private static final Identifier SOUL_FIRE_SPRITE_1 = Identifier.parse("minecraft:block/soul_fire_1");

    // >0 means we have confirmed the current burn came from soul fire.
    // We keep this latched until the player is no longer on fire.
    private static int soulBurnTicks;

    // Client-side on-fire state can briefly flicker due to sync/timing; avoid clearing immediately.
    private static int playerNotOnFireTicks;

    // Per-entity latch (for third-person / other entities).
    private static final Int2BooleanOpenHashMap soulBurnEntity = new Int2BooleanOpenHashMap();

    public static boolean shouldUseSoulFireOverlayNow(Minecraft mc) {
        if (mc == null) return false;

        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return soulBurnTicks > 0;
        if (!player.isOnFire()) return false;

        // Prefer a live check (reliable for soul fire / soul campfire contact).
        // Fall back to the persisted timer so the overlay can remain blue after stepping away.
        boolean touchingSoulFire = isTouchingSoulFire(level, player);
        // Important: latch immediately when we observe soul-fire contact while burning.
        // Otherwise, stepping out before our client tick runs can cause the overlay to revert to orange.
        if (touchingSoulFire) {
            soulBurnTicks = 1;
        }
        return touchingSoulFire || soulBurnTicks > 0;
    }

    public static void tick(Minecraft mc, boolean enabled) {
        if (!enabled) {
            soulBurnTicks = 0;
            playerNotOnFireTicks = 0;
            return;
        }

        if (mc == null) {
            soulBurnTicks = 0;
            playerNotOnFireTicks = 0;
            return;
        }

        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            soulBurnTicks = 0;
            playerNotOnFireTicks = 0;
            return;
        }

        if (!player.isOnFire()) {
            playerNotOnFireTicks++;
            if (playerNotOnFireTicks >= 2) {
                soulBurnTicks = 0;
            }
            return;
        }

        playerNotOnFireTicks = 0;

        boolean touchingSoulFire = isTouchingSoulFire(level, player);
        if (touchingSoulFire) {
            // Latch for the entire burn; client-side remaining-fire-ticks can be unreliable.
            soulBurnTicks = 1;
        }
    }

    public static boolean useSoulFireOverlay() {
        return soulBurnTicks > 0;
    }

    /** Returns true when this entity's current burn should be rendered as soul fire. */
    public static boolean shouldUseSoulFireFlamesNow(Level level, Entity entity) {
        if (level == null || entity == null) return false;

        int id;
        try {
            id = entity.getId();
        } catch (Throwable t) {
            return false;
        }

        if (!entity.isOnFire()) {
            soulBurnEntity.remove(id);
            return false;
        }

        AABB box;
        try {
            box = entity.getBoundingBox().inflate(0.001D);
        } catch (Throwable t) {
            return soulBurnEntity.getOrDefault(id, false);
        }

        boolean touchingSoul = isTouchingSoulFire(level, box);
        if (touchingSoul) {
            soulBurnEntity.put(id, true);
            return true;
        }

        // If currently touching normal fire/campfires, treat as normal burn.
        if (isTouchingNormalFire(level, box)) {
            soulBurnEntity.remove(id);
            return false;
        }

        return soulBurnEntity.getOrDefault(id, false);
    }

    public static boolean shouldUseSoulFireCandleFlames(Level level, Vec3 flamePos) {
        if (level == null || flamePos == null) return false;

        BlockPos candlePos = BlockPos.containing(flamePos);
        return level.getBlockState(candlePos.below()).is(BlockTags.SOUL_FIRE_BASE_BLOCKS);
    }

    public static Identifier remapFireOverlay(Identifier overlayTex) {
        if (overlayTex == null) return null;
        if (!"minecraft".equals(overlayTex.getNamespace())) return overlayTex;

        String path = overlayTex.getPath();
        if (path == null) return overlayTex;

        // Some MC versions route overlays through renderTextureOverlay and reference these textures.
        if (path.endsWith("fire_0.png")) {
            return Identifier.parse("minecraft:" + path.replace("fire_0.png", "soul_fire_0.png"));
        }
        if (path.endsWith("fire_1.png")) {
            return Identifier.parse("minecraft:" + path.replace("fire_1.png", "soul_fire_1.png"));
        }

        return overlayTex;
    }

    /**
     * In MC 1.21.x the on-fire screen effect is rendered via {@nette.minecraft.client.renderer.ScreenEffectRenderer}
     * and uses block-atlas sprites {@code minecraft:block/fire_0} + {@code minecraft:block/fire_1}.
     */
    public static TextureAtlasSprite remapFireSprite(TextureAtlasSprite sprite) {
        if (sprite == null) return null;

        Identifier name = null;
        try {
            name = sprite.contents().name();
        } catch (Throwable ignored) {
        }
        if (name == null) return sprite;
        if (!"minecraft".equals(name.getNamespace())) return sprite;

        Identifier target;
        if (FIRE_SPRITE_0.equals(name)) {
            target = SOUL_FIRE_SPRITE_0;
        } else if (FIRE_SPRITE_1.equals(name)) {
            target = SOUL_FIRE_SPRITE_1;
        } else {
            return sprite;
        }

        // Prefer grabbing from the same atlas instance backing the current sprite.
        TextureAtlasSprite sibling = getSpriteFromSameAtlas(sprite, target);
        if (sibling != null) return sibling;

        TextureAtlasSprite remapped = getBlockAtlasSprite(target);
        return remapped != null ? remapped : sprite;
    }

    private static TextureAtlasSprite getSpriteFromSameAtlas(TextureAtlasSprite current, Identifier spriteId) {
        try {
            // Probe zero-arg methods for an atlas-like object or sprite getter.
            for (var method : current.getClass().getMethods()) {
                if (method.getParameterCount() != 0) continue;
                Class<?> returnType = method.getReturnType();
                if (returnType == void.class) continue;

                Object result;
                try {
                    result = method.invoke(current);
                } catch (Throwable ignored) {
                    continue;
                }
                if (result == null) continue;

                // Case A: result has getSprite(ResourceLocation)
                try {
                    var getSprite = result.getClass().getMethod("getSprite", Identifier.class);
                    Object maybeSprite = getSprite.invoke(result, spriteId);
                    if (maybeSprite instanceof TextureAtlasSprite tas) {
                        return tas;
                    }
                } catch (Throwable ignored) {
                }

                // Case B: result is a Function<ResourceLocation, TextureAtlasSprite>
                if (result instanceof java.util.function.Function<?, ?> func) {
                    try {
                        Object maybeSprite = ((java.util.function.Function) func).apply(spriteId);
                        if (maybeSprite instanceof TextureAtlasSprite tas) {
                            return tas;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TextureAtlasSprite getBlockAtlasSprite(Identifier spriteId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;

            Object atlas = null;

            // Attempt: mc.getModelManager().getAtlas(ResourceLocation)
            try {
                Object modelManager = mc.getModelManager();
                if (modelManager != null) {
                    var getAtlas = modelManager.getClass().getMethod("getAtlas", Identifier.class);
                    atlas = getAtlas.invoke(modelManager, TextureAtlas.LOCATION_BLOCKS);
                }
            } catch (Throwable ignored) {
            }

            // Attempt: mc.getTextureAtlas(ResourceLocation) -> (TextureAtlas) OR (Function<ResourceLocation, TextureAtlasSprite>)
            if (atlas == null) {
                try {
                    var getTextureAtlas = mc.getClass().getMethod("getTextureAtlas", Identifier.class);
                    Object result = getTextureAtlas.invoke(mc, TextureAtlas.LOCATION_BLOCKS);
                    if (result instanceof java.util.function.Function func) {
                        Object maybeSprite = func.apply(spriteId);
                        if (maybeSprite instanceof TextureAtlasSprite tas) {
                            return tas;
                        }
                    } else {
                        atlas = result;
                    }
                } catch (Throwable ignored) {
                }
            }

            if (atlas == null) return null;

            try {
                var getSprite = atlas.getClass().getMethod("getSprite", Identifier.class);
                Object maybeSprite = getSprite.invoke(atlas, spriteId);
                if (maybeSprite instanceof TextureAtlasSprite tas) {
                    return tas;
                }
            } catch (Throwable ignored) {
            }

            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isTouchingSoulFire(Level level, Player player) {
        return isTouchingSoulFire(level, player.getBoundingBox().inflate(0.001D));
    }

    private static boolean isTouchingSoulFire(Level level, AABB box) {
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        // Include one block below the feet so standing on (soul) campfires is detected.
        int minY = (int) Math.floor(box.minY) - 1;
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);

                    if (state.is(Blocks.SOUL_FIRE)) {
                        return true;
                    }

                    if (state.is(Blocks.SOUL_CAMPFIRE)) {
                        if (!state.hasProperty(CampfireBlock.LIT) || state.getValue(CampfireBlock.LIT)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean isTouchingNormalFire(Level level, AABB box) {
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY) - 1;
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);

                    if (state.is(Blocks.FIRE)) {
                        return true;
                    }

                    if (state.is(Blocks.CAMPFIRE)) {
                        if (!state.hasProperty(CampfireBlock.LIT) || state.getValue(CampfireBlock.LIT)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
