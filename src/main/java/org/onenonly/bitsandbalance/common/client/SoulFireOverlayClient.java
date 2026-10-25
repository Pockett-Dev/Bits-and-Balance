package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
//? if mc == 1.21.1
import net.minecraft.resources.ResourceLocation;
//? else
import net.minecraft.resources.Identifier;
//? endif
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.tags.BlockTags;
//? endif
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
//? endif
import net.minecraft.world.phys.AABB;
//? if mc != 26.1 && mc != 26.2 && mc != 26.3
import net.minecraft.world.phys.Vec3;
//? endif

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
//? if mc == 26.1 || mc == 26.2 || mc == 26.3
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import org.onenonly.bitsandbalance.common.entity.SoulFireBlockHelper;
import org.onenonly.bitsandbalance.common.entity.SoulFireBurnAccess;
//? endif

/**
 * Client-only helper for rendering blue (soul) fire when the player is burning
 * due to soul-fire-related blocks.
 */
public final class SoulFireOverlayClient {
    private SoulFireOverlayClient() {
    }

    //? if mc == 1.21.1
    private static final ResourceLocation FIRE_SPRITE_0 = ResourceLocation.parse("minecraft:block/fire_0");
    private static final ResourceLocation FIRE_SPRITE_1 = ResourceLocation.parse("minecraft:block/fire_1");
    private static final ResourceLocation SOUL_FIRE_SPRITE_0 = ResourceLocation.parse("minecraft:block/soul_fire_0");
    private static final ResourceLocation SOUL_FIRE_SPRITE_1 = ResourceLocation.parse("minecraft:block/soul_fire_1");
    //? else
    private static final Identifier FIRE_SPRITE_0 = Identifier.parse("minecraft:block/fire_0");
    private static final Identifier FIRE_SPRITE_1 = Identifier.parse("minecraft:block/fire_1");
    private static final Identifier SOUL_FIRE_SPRITE_0 = Identifier.parse("minecraft:block/soul_fire_0");
    private static final Identifier SOUL_FIRE_SPRITE_1 = Identifier.parse("minecraft:block/soul_fire_1");
    //? endif

    // >0 means we have confirmed the current burn came from soul fire.
    // We keep this latched until the player is no longer on fire.
    private static int soulBurnTicks;

    // Client-side on-fire state can briefly flicker due to sync/timing; avoid clearing immediately.
    private static int playerNotOnFireTicks;

    // Per-entity latch (for third-person / other entities).
    private static final Int2BooleanOpenHashMap soulBurnEntity = new Int2BooleanOpenHashMap();
    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
    // Keep the last soul-fire classification for a couple of client ticks so the flame renderer
    // does not fall back to orange on the final burn frame.
    private static final Int2LongOpenHashMap soulBurnEntityGraceUntilTick = new Int2LongOpenHashMap();

    static {
        soulBurnEntityGraceUntilTick.defaultReturnValue(Long.MIN_VALUE);
    }
    //? endif

    public static boolean shouldUseSoulFireOverlayNow(Minecraft mc) {
        if (mc == null) return false;

        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return soulBurnTicks > 0;
        if (!player.isOnFire()) return false;

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        if (player instanceof SoulFireBurnAccess access && access.bitsandbalance$isSoulFireBurn()) {
            soulBurnTicks = 1;
            return true;
        }
        //? endif

        // Prefer a live check (reliable for soul fire / soul campfire contact).
        // Fall back to the persisted timer so the overlay can remain blue after stepping away.
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        AABB playerBox = SoulFireBlockHelper.fireCheckBox(player.getBoundingBox());
        boolean touchingSoulFire = SoulFireBlockHelper.isTouchingSoulFire(level, playerBox);
        //? else
        boolean touchingSoulFire = isTouchingSoulFire(level, player);
        //? endif
        // Important: latch immediately when we observe soul-fire contact while burning.
        // Otherwise, stepping out before our client tick runs can cause the overlay to revert to orange.
        if (touchingSoulFire) {
            soulBurnTicks = 1;
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        } else if (SoulFireBlockHelper.isTouchingNormalFire(level, playerBox)) {
            soulBurnTicks = 0;
        //? endif
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

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        if (player instanceof SoulFireBurnAccess access && access.bitsandbalance$isSoulFireBurn()) {
            soulBurnTicks = 1;
            return;
        }

        AABB playerBox = SoulFireBlockHelper.fireCheckBox(player.getBoundingBox());
        boolean touchingSoulFire = SoulFireBlockHelper.isTouchingSoulFire(level, playerBox);
        //? else
        boolean touchingSoulFire = isTouchingSoulFire(level, player);
        //? endif
        if (touchingSoulFire) {
            // Latch for the entire burn; client-side remaining-fire-ticks can be unreliable.
            soulBurnTicks = 1;
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        } else if (SoulFireBlockHelper.isTouchingNormalFire(level, playerBox)) {
            soulBurnTicks = 0;
        //? endif
        }
    }

    public static boolean useSoulFireOverlay() {
        return soulBurnTicks > 0;
    }

    /** Returns true when this entity's current burn should be rendered as soul fire. */
    public static boolean shouldUseSoulFireFlamesNow(Level level, Entity entity) {
        if (level == null || entity == null) return false;
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        long gameTime = level.getGameTime();

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && entity == mc.player) {
            boolean localSoul = shouldUseSoulFireOverlayNow(mc);
            if (localSoul) {
                bitsandbalance$latchSoulBurnEntity(entity.getId(), gameTime);
                return true;
            }
        }
        //? endif

        int id;
        try {
            id = entity.getId();
        } catch (Throwable t) {
            return false;
        }

        if (!entity.isOnFire()) {
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            return bitsandbalance$hasLingeringSoulBurn(id, gameTime);
            //? else
            soulBurnEntity.remove(id);
            return false;
            //? endif
        }

        AABB box;
        try {
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            box = SoulFireBlockHelper.fireCheckBox(entity.getBoundingBox());
            //? else
            box = entity.getBoundingBox().inflate(0.001D);
            //? endif
        } catch (Throwable t) {
            return soulBurnEntity.getOrDefault(id, false);
        }

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
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
        //? endif

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        boolean touchingSoul = SoulFireBlockHelper.isTouchingSoulFire(level, box);
        //? else
        boolean touchingSoul = isTouchingSoulFire(level, box);
        //? endif
        if (touchingSoul) {
            //? if mc == 26.1 || mc == 26.2 || mc == 26.3
            bitsandbalance$latchSoulBurnEntity(id, gameTime);
            //? else
            soulBurnEntity.put(id, true);
            //? endif
            return true;
        }

        // If currently touching normal fire/campfires, treat as normal burn.
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        if (SoulFireBlockHelper.isTouchingNormalFire(level, box)) {
            bitsandbalance$clearSoulBurnEntity(id);
        //? else
        if (isTouchingNormalFire(level, box)) {
            soulBurnEntity.remove(id);
        //? endif
            return false;
        }

        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        return bitsandbalance$hasLingeringSoulBurn(id, gameTime);
        //? else
        return soulBurnEntity.getOrDefault(id, false);
        //? endif
    }

    //? if mc == 26.1 || mc == 26.2 || mc == 26.3
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
    //? endif

    //? if mc != 26.1 && mc != 26.2 && mc != 26.3
    public static boolean shouldUseSoulFireCandleFlames(Level level, Vec3 flamePos) {
        if (level == null || flamePos == null) return false;

        BlockPos candlePos = BlockPos.containing(flamePos);
        return level.getBlockState(candlePos.below()).is(BlockTags.SOUL_FIRE_BASE_BLOCKS);
    }
    //? endif

    //? if mc == 1.21.1
    public static ResourceLocation remapFireOverlay(ResourceLocation overlayTex) {
    //? else
    public static Identifier remapFireOverlay(Identifier overlayTex) {
    //? endif
        if (overlayTex == null) return null;
        if (!"minecraft".equals(overlayTex.getNamespace())) return overlayTex;

        String path = overlayTex.getPath();
        if (path == null) return overlayTex;

        // Some MC versions route overlays through renderTextureOverlay and reference these textures.
        if (path.endsWith("fire_0.png")) {
            //? if mc == 1.21.1
            return ResourceLocation.parse("minecraft:" + path.replace("fire_0.png", "soul_fire_0.png"));
            //? else
            return Identifier.parse("minecraft:" + path.replace("fire_0.png", "soul_fire_0.png"));
            //? endif
        }
        if (path.endsWith("fire_1.png")) {
            //? if mc == 1.21.1
            return ResourceLocation.parse("minecraft:" + path.replace("fire_1.png", "soul_fire_1.png"));
            //? else
            return Identifier.parse("minecraft:" + path.replace("fire_1.png", "soul_fire_1.png"));
            //? endif
        }

        return overlayTex;
    }

    /**
     * In MC 1.21.x the on-fire screen effect is rendered via {@nette.minecraft.client.renderer.ScreenEffectRenderer}
     * and uses block-atlas sprites {@code minecraft:block/fire_0} + {@code minecraft:block/fire_1}.
     */
    public static TextureAtlasSprite remapFireSprite(TextureAtlasSprite sprite) {
        if (sprite == null) return null;

        //? if mc == 1.21.1
        ResourceLocation name = null;
        //? else
        Identifier name = null;
        //? endif
        try {
            name = sprite.contents().name();
        } catch (Throwable ignored) {
        }
        if (name == null) return sprite;
        if (!"minecraft".equals(name.getNamespace())) return sprite;

        //? if mc == 1.21.1
        ResourceLocation target;
        //? else
        Identifier target;
        //? endif
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

    //? if mc == 1.21.1
    private static TextureAtlasSprite getSpriteFromSameAtlas(TextureAtlasSprite current, ResourceLocation spriteId) {
    //? else
    private static TextureAtlasSprite getSpriteFromSameAtlas(TextureAtlasSprite current, Identifier spriteId) {
    //? endif
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
                    //? if mc == 1.21.1
                    var getSprite = result.getClass().getMethod("getSprite", ResourceLocation.class);
                    //? else
                    var getSprite = result.getClass().getMethod("getSprite", Identifier.class);
                    //? endif
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
    //? if mc == 1.21.1
    private static TextureAtlasSprite getBlockAtlasSprite(ResourceLocation spriteId) {
    //? else
    private static TextureAtlasSprite getBlockAtlasSprite(Identifier spriteId) {
    //? endif
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;

            Object atlas = null;

            // Attempt: mc.getModelManager().getAtlas(ResourceLocation)
            try {
                Object modelManager = mc.getModelManager();
                if (modelManager != null) {
                    //? if mc == 1.21.1
                    var getAtlas = modelManager.getClass().getMethod("getAtlas", ResourceLocation.class);
                    //? else
                    var getAtlas = modelManager.getClass().getMethod("getAtlas", Identifier.class);
                    //? endif
                    atlas = getAtlas.invoke(modelManager, TextureAtlas.LOCATION_BLOCKS);
                }
            } catch (Throwable ignored) {
            }

            // Attempt: mc.getTextureAtlas(ResourceLocation) -> (TextureAtlas) OR (Function<ResourceLocation, TextureAtlasSprite>)
            if (atlas == null) {
                try {
                    //? if mc == 1.21.1
                    var getTextureAtlas = mc.getClass().getMethod("getTextureAtlas", ResourceLocation.class);
                    //? else
                    var getTextureAtlas = mc.getClass().getMethod("getTextureAtlas", Identifier.class);
                    //? endif
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
                //? if mc == 1.21.1
                var getSprite = atlas.getClass().getMethod("getSprite", ResourceLocation.class);
                //? else
                var getSprite = atlas.getClass().getMethod("getSprite", Identifier.class);
                //? endif
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

    //? if mc != 26.1 && mc != 26.2 && mc != 26.3
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
    //? endif

}
