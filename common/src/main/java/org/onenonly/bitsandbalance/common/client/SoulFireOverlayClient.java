package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import org.onenonly.bitsandbalance.common.entity.SoulFireBlockHelper;
import org.onenonly.bitsandbalance.common.entity.SoulFireBurnAccess;

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
    // Keep the last soul-fire classification for a couple of client ticks so the flame renderer
    // does not fall back to orange on the final burn frame.
    private static final Int2LongOpenHashMap soulBurnEntityGraceUntilTick = new Int2LongOpenHashMap();

    static {
        soulBurnEntityGraceUntilTick.defaultReturnValue(Long.MIN_VALUE);
    }

    public static boolean shouldUseSoulFireOverlayNow(Minecraft mc) {
        if (mc == null) return false;

        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return soulBurnTicks > 0;
        if (!player.isOnFire()) return false;

        if (player instanceof SoulFireBurnAccess access && access.bitsandbalance$isSoulFireBurn()) {
            soulBurnTicks = 1;
            return true;
        }

        // Prefer a live check (reliable for soul fire / soul campfire contact).
        // Fall back to the persisted timer so the overlay can remain blue after stepping away.
        AABB playerBox = SoulFireBlockHelper.fireCheckBox(player.getBoundingBox());
        boolean touchingSoulFire = SoulFireBlockHelper.isTouchingSoulFire(level, playerBox);
        // Important: latch immediately when we observe soul-fire contact while burning.
        // Otherwise, stepping out before our client tick runs can cause the overlay to revert to orange.
        if (touchingSoulFire) {
            soulBurnTicks = 1;
        } else if (SoulFireBlockHelper.isTouchingNormalFire(level, playerBox)) {
            soulBurnTicks = 0;
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

        if (player instanceof SoulFireBurnAccess access && access.bitsandbalance$isSoulFireBurn()) {
            soulBurnTicks = 1;
            return;
        }

        AABB playerBox = SoulFireBlockHelper.fireCheckBox(player.getBoundingBox());
        boolean touchingSoulFire = SoulFireBlockHelper.isTouchingSoulFire(level, playerBox);
        if (touchingSoulFire) {
            // Latch for the entire burn; client-side remaining-fire-ticks can be unreliable.
            soulBurnTicks = 1;
        } else if (SoulFireBlockHelper.isTouchingNormalFire(level, playerBox)) {
            soulBurnTicks = 0;
        }
    }

    public static boolean useSoulFireOverlay() {
        return soulBurnTicks > 0;
    }

    /** Returns true when this entity's current burn should be rendered as soul fire. */
    public static boolean shouldUseSoulFireFlamesNow(Level level, Entity entity) {
        if (level == null || entity == null) return false;
        long gameTime = level.getGameTime();

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && entity == mc.player) {
            boolean localSoul = shouldUseSoulFireOverlayNow(mc);
            if (localSoul) {
                bitsandbalance$latchSoulBurnEntity(entity.getId(), gameTime);
                return true;
            }
        }

        int id;
        try {
            id = entity.getId();
        } catch (Throwable t) {
            return false;
        }

        if (!entity.isOnFire()) {
            return bitsandbalance$hasLingeringSoulBurn(id, gameTime);
        }

        AABB box;
        try {
            box = SoulFireBlockHelper.fireCheckBox(entity.getBoundingBox());
        } catch (Throwable t) {
            return soulBurnEntity.getOrDefault(id, false);
        }

        if (entity instanceof SoulFireBurnAccess access) {
            if (access.bitsandbalance$isSoulFireBurn()) {
                bitsandbalance$latchSoulBurnEntity(id, gameTime);
                return true;
            }

            if (SoulFireBlockHelper.isTouchingNormalFire(level, box)) {
                bitsandbalance$clearSoulBurnEntity(id);
                return false;
            }

            return bitsandbalance$hasLingeringSoulBurn(id, gameTime);
        }

        boolean touchingSoul = SoulFireBlockHelper.isTouchingSoulFire(level, box);
        if (touchingSoul) {
            bitsandbalance$latchSoulBurnEntity(id, gameTime);
            return true;
        }

        // If currently touching normal fire/campfires, treat as normal burn.
        if (SoulFireBlockHelper.isTouchingNormalFire(level, box)) {
            bitsandbalance$clearSoulBurnEntity(id);
            return false;
        }

        return bitsandbalance$hasLingeringSoulBurn(id, gameTime);
    }

    private static void bitsandbalance$latchSoulBurnEntity(int entityId, long gameTime) {
        soulBurnEntity.put(entityId, true);
        soulBurnEntityGraceUntilTick.put(entityId, gameTime + 2L);
    }

    private static boolean bitsandbalance$hasLingeringSoulBurn(int entityId, long gameTime) {
        long untilTick = soulBurnEntityGraceUntilTick.get(entityId);
        if (!soulBurnEntity.getOrDefault(entityId, false)) {
            return false;
        }
        if (gameTime <= untilTick) {
            return true;
        }
        bitsandbalance$clearSoulBurnEntity(entityId);
        return false;
    }

    private static void bitsandbalance$clearSoulBurnEntity(int entityId) {
        soulBurnEntity.remove(entityId);
        soulBurnEntityGraceUntilTick.remove(entityId);
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

}
