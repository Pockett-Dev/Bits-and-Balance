package org.onenonly.bitsandbalance.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import com.mojang.math.Quadrant;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Fix: dynamically-registered slab/step item variants have no generated item model JSON.
 *
 * We resolve their item model by temporarily swapping the stack to the source slab item,
 * preserving any attached stack data.
 */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverVerticalSlabItemMixin {

    @Unique
    private static final Object bitsandbalance$VERTICAL_SLAB_MODEL_IDENTITY_MARKER = new Object();

    @Unique
    private static final Object bitsandbalance$STEP_MODEL_IDENTITY_MARKER = new Object();

    @Unique
    private static final Object bitsandbalance$VERTICAL_STEP_MODEL_IDENTITY_MARKER = new Object();

        @Unique
        private static final Identifier bitsandbalance$VERTICAL_SLAB_TEMPLATE_MODEL_RESOURCE =
            Identifier.fromNamespaceAndPath("bitsandbalance", "models/item/vertical_slab.json");

        @Unique
        private static final Identifier bitsandbalance$STEP_TEMPLATE_MODEL_RESOURCE =
            Identifier.fromNamespaceAndPath("bitsandbalance", "models/item/step.json");

        @Unique
        private static final Identifier bitsandbalance$VERTICAL_STEP_TEMPLATE_MODEL_RESOURCE =
            Identifier.fromNamespaceAndPath("bitsandbalance", "models/item/vertical_step.json");

        @Unique
        private static volatile EnumMap<ItemDisplayContext, ItemTransform> bitsandbalance$verticalSlabTemplateDisplayTransforms;

        @Unique
        private static volatile EnumMap<ItemDisplayContext, ItemTransform> bitsandbalance$stepTemplateDisplayTransforms;

        @Unique
        private static volatile EnumMap<ItemDisplayContext, ItemTransform> bitsandbalance$verticalStepTemplateDisplayTransforms;

        @Unique
        private static volatile long bitsandbalance$verticalSlabTemplateDisplayTransformsLastLoadMs;

        @Unique
        private static volatile long bitsandbalance$stepTemplateDisplayTransformsLastLoadMs;

        @Unique
        private static volatile long bitsandbalance$verticalStepTemplateDisplayTransformsLastLoadMs;

        @Unique
        private static final FaceBakery bitsandbalance$FACE_BAKERY = new FaceBakery();

        @Unique
        private static final ModelState bitsandbalance$NO_MODEL_TRANSFORM = BlockModelRotation.IDENTITY;

        @Unique
        private static final ModelBaker.PartCache bitsandbalance$PART_CACHE = new ModelBaker.PartCache() {
            @Override
            public Vector3fc vector(Vector3fc in) {
                // FaceBakery may call through this to reduce allocations; we can safely return an immutable copy.
                return new Vector3f(in);
            }
        };

        @Unique
        private static final Vector3fc bitsandbalance$VERTICAL_SLAB_FROM = new Vector3f(0.0f, 0.0f, 8.0f);

        @Unique
        private static final Vector3fc bitsandbalance$VERTICAL_SLAB_TO = new Vector3f(16.0f, 16.0f, 16.0f);

        @Unique
        private static final Vector3fc bitsandbalance$STEP_FROM = new Vector3f(8.0f, 0.0f, 0.0f);

        @Unique
        private static final Vector3fc bitsandbalance$STEP_TO = new Vector3f(16.0f, 8.0f, 16.0f);

        @Unique
        private static final Vector3fc bitsandbalance$VERTICAL_STEP_FROM = new Vector3f(8.0f, 0.0f, 0.0f);

        @Unique
        private static final Vector3fc bitsandbalance$VERTICAL_STEP_TO = new Vector3f(16.0f, 16.0f, 8.0f);

    @Unique
    private static final int bitsandbalance$ENHANCED_BLOCK_NONE = 0;

    @Unique
    private static final int bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB = 1;

    @Unique
    private static final int bitsandbalance$ENHANCED_BLOCK_STEP = 2;

    @Unique
    private static final int bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP = 3;

    @Unique
    private static int bitsandbalance$getEnhancedBlockType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return bitsandbalance$ENHANCED_BLOCK_NONE;
        Item item = stack.getItem();
        if (!(item instanceof BlockItem blockItem)) return bitsandbalance$ENHANCED_BLOCK_NONE;
        Block block = blockItem.getBlock();
        if (block instanceof FixedStepBlock) return bitsandbalance$ENHANCED_BLOCK_STEP;
        if (block instanceof FixedVerticalStepBlock) return bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP;
        if (block instanceof FixedVerticalSlabBlock) return bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB;
        if (StepDynamicRegistry.getSlabForStep(block) != null) return bitsandbalance$ENHANCED_BLOCK_STEP;
        if (VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block) != null) return bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP;
        if (VerticalSlabDynamicRegistry.getSlabForVertical(block) != null) return bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB;
        return bitsandbalance$ENHANCED_BLOCK_NONE;
    }

    @ModifyVariable(
            method = "appendItemLayers(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2,
            require = 0
    )
    private ItemStack bitsandbalance$renderEnhancedItemsFromCopy(ItemStack stack) {
        return bitsandbalance$copyWithModelOverride(stack);
    }

    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private ItemStack bitsandbalance$renderEnhancedItemsFromCopyByName(ItemStack stack) {
        return bitsandbalance$copyWithModelOverride(stack);
    }

    @Inject(
            method = "appendItemLayers(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("HEAD"),
            require = 0
    )
    private void bitsandbalance$ensureVerticalSlabItemModel(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed,
            CallbackInfo ci
    ) {
        bitsandbalance$maybeOverrideModel(renderState, stack);
    }

    // Fallback target (less signature-fragile across mappings)
    @Inject(method = "appendItemLayers", at = @At("HEAD"), require = 0)
    private void bitsandbalance$ensureVerticalSlabItemModelByName(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed,
            CallbackInfo ci
    ) {
        bitsandbalance$maybeOverrideModel(renderState, stack);
    }

        @Inject(
            method = "appendItemLayers(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("RETURN"),
            require = 0
        )
        private void bitsandbalance$rotateVerticalSlabItemQuads(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed,
            CallbackInfo ci
        ) {
        bitsandbalance$maybeRotateRenderState(renderState, stack, displayContext);
        }

        // Fallback target (less signature-fragile across mappings)
        @Inject(method = "appendItemLayers", at = @At("RETURN"), require = 0)
        private void bitsandbalance$rotateVerticalSlabItemQuadsByName(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed,
            CallbackInfo ci
        ) {
        bitsandbalance$maybeRotateRenderState(renderState, stack, displayContext);
        }

    @Unique
    private static void bitsandbalance$maybeOverrideModel(ItemStackRenderState renderState, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        Identifier desiredModelId = bitsandbalance$modelIdForEnhancedSlabStack(stack);
        if (desiredModelId == null) return;

        // If the item has a proper definition JSON, let that take precedence.
        if (bitsandbalance$hasItemDefinitionJson(stack)) {
            return;
        }

        // If the stack already has some other explicit model override, respect it.
        Identifier existing = null;
        try {
            existing = stack.get(DataComponents.ITEM_MODEL);
        } catch (Throwable ignored) {
        }

        Identifier ownItemId = null;
        try {
            ownItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        } catch (Throwable ignored) {
        }

        if (existing != null && (ownItemId == null || (!existing.equals(ownItemId) && !existing.equals(desiredModelId)))) {
            return;
        }

        try {
            stack.set(DataComponents.ITEM_MODEL, desiredModelId);
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static ItemStack bitsandbalance$copyWithModelOverride(ItemStack original) {
        if (original == null || original.isEmpty()) return original;

        Identifier desiredModelId = bitsandbalance$modelIdForEnhancedSlabStack(original);
        if (desiredModelId == null) return original;

        if (bitsandbalance$hasItemDefinitionJson(original)) {
            return original;
        }

        Identifier existing = null;
        try {
            existing = original.get(DataComponents.ITEM_MODEL);
        } catch (Throwable ignored) {
        }

        Identifier ownItemId = null;
        try {
            ownItemId = BuiltInRegistries.ITEM.getKey(original.getItem());
        } catch (Throwable ignored) {
        }

        if (existing != null && (ownItemId == null || (!existing.equals(ownItemId) && !existing.equals(desiredModelId)))) {
            return original;
        }

        try {
            ItemStack copy = original.copy();
            copy.set(DataComponents.ITEM_MODEL, desiredModelId);
            return copy;
        } catch (Throwable ignored) {
            return original;
        }
    }

    @Unique
    private static void bitsandbalance$maybeRotateRenderState(ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext injectedDisplayContext) {
        if (renderState == null || stack == null || stack.isEmpty()) return;

        // Only rotate when this stack is a vertical slab item, AND we are using the fallback model override.
        Identifier desiredModelId = bitsandbalance$modelIdForEnhancedSlabStack(stack);
        if (desiredModelId == null) return;
        if (bitsandbalance$hasItemDefinitionJson(stack)) return;

        Identifier existing = null;
        try {
            existing = stack.get(DataComponents.ITEM_MODEL);
        } catch (Throwable ignored) {
        }

        Identifier ownItemId = null;
        try {
            ownItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        } catch (Throwable ignored) {
        }

        if (existing != null && (ownItemId == null || (!existing.equals(ownItemId) && !existing.equals(desiredModelId)))) {
            return;
        }

        // Determine the block type so we use the correct shape/identity for rendering.
        int blockType = bitsandbalance$getEnhancedBlockType(stack);
        if (blockType == bitsandbalance$ENHANCED_BLOCK_NONE) blockType = bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB;

        // Ensure GUI caching never conflates enhanced items with the source slab item.
        // This must run after appendItemLayers has populated/cleared the render state.
        Object identityMarker = switch (blockType) {
            case bitsandbalance$ENHANCED_BLOCK_STEP -> bitsandbalance$STEP_MODEL_IDENTITY_MARKER;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP -> bitsandbalance$VERTICAL_STEP_MODEL_IDENTITY_MARKER;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB -> bitsandbalance$VERTICAL_SLAB_MODEL_IDENTITY_MARKER;
            default -> bitsandbalance$VERTICAL_SLAB_MODEL_IDENTITY_MARKER;
        };
        try {
            renderState.appendModelIdentityElement(identityMarker);
        } catch (Throwable ignored) {
        }

        ItemStackRenderStateAccessor access;
        try {
            access = (ItemStackRenderStateAccessor) renderState;
        } catch (Throwable t) {
            return;
        }

        int count;
        ItemStackRenderState.LayerRenderState[] layers;
        ItemDisplayContext displayContext;
        try {
            count = access.bitsandbalance$getActiveLayerCount();
            layers = access.bitsandbalance$getLayers();
            displayContext = injectedDisplayContext != null ? injectedDisplayContext : access.bitsandbalance$getDisplayContext();
        } catch (Throwable t) {
            return;
        }
        if (layers == null || count <= 0) return;

        ItemTransform forcedTransform = bitsandbalance$enhancedDisplayTransform(displayContext, blockType);

        int limit = Math.min(count, layers.length);
        for (int i = 0; i < limit; i++) {
            ItemStackRenderState.LayerRenderState layer = layers[i];
            if (layer == null) continue;

            if (forcedTransform != null) {
                try {
                    ((ItemStackRenderStateLayerAccessor) (Object) layer).bitsandbalance$setTransform(forcedTransform);
                } catch (Throwable ignored) {
                }
            }

            List<BakedQuad> quads;
            try {
                quads = ((ItemStackRenderStateLayerAccessor) (Object) layer).bitsandbalance$getQuads();
            } catch (Throwable t) {
                continue;
            }
            if (quads == null || quads.isEmpty()) continue;

            // Rebuild a prism using the slab model's sprites per face.
            // The shape depends on the enhanced block type (vertical slab / step / vertical step).
            List<BakedQuad> rebuilt = switch (blockType) {
                case bitsandbalance$ENHANCED_BLOCK_STEP -> bitsandbalance$buildStepPrismFromLayer(quads);
                case bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP -> bitsandbalance$buildVerticalStepPrismFromLayer(quads);
                case bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB -> bitsandbalance$buildVerticalSlabPrismFromLayer(quads);
                default -> bitsandbalance$buildVerticalSlabPrismFromLayer(quads);
            };
            if (rebuilt == null || rebuilt.isEmpty()) continue;

            try {
                ((ItemStackRenderStateLayerAccessor) (Object) layer).bitsandbalance$setQuads(rebuilt);
            } catch (Throwable t) {
                try {
                    quads.clear();
                    quads.addAll(rebuilt);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Unique
    private record bitsandbalance$FaceData(TextureAtlasSprite sprite, int tintIndex, boolean shade, int lightEmission) {
    }

    @Unique
    private static List<BakedQuad> bitsandbalance$buildVerticalSlabPrismFromLayer(List<BakedQuad> sourceQuads) {
        if (sourceQuads == null || sourceQuads.isEmpty()) return null;

        EnumMap<Direction, bitsandbalance$FaceData> byDir = new EnumMap<>(Direction.class);
        bitsandbalance$FaceData any = null;

        for (BakedQuad q : sourceQuads) {
            if (q == null) continue;
            Direction d = q.direction();
            if (d == null) continue;
            TextureAtlasSprite sprite = q.sprite();
            if (sprite == null) continue;

            bitsandbalance$FaceData fd = new bitsandbalance$FaceData(sprite, q.tintIndex(), q.shade(), q.lightEmission());
            if (any == null) any = fd;
            byDir.putIfAbsent(d, fd);
        }

        if (any == null) return null;

        bitsandbalance$FaceData north = byDir.getOrDefault(Direction.NORTH, any);
        bitsandbalance$FaceData south = byDir.getOrDefault(Direction.SOUTH, any);
        bitsandbalance$FaceData west = byDir.getOrDefault(Direction.WEST, any);
        bitsandbalance$FaceData east = byDir.getOrDefault(Direction.EAST, any);
        bitsandbalance$FaceData up = byDir.getOrDefault(Direction.UP, any);
        bitsandbalance$FaceData down = byDir.getOrDefault(Direction.DOWN, any);

        List<BakedQuad> out = new ArrayList<>(6);

        // UVs match assets/bitsandbalance/models/item/vertical_slab.json
        out.add(bitsandbalance$bakeFaceQuad(Direction.NORTH, 0, 0, 16, 16, north));
        out.add(bitsandbalance$bakeFaceQuad(Direction.SOUTH, 0, 0, 16, 16, south));
        out.add(bitsandbalance$bakeFaceQuad(Direction.WEST, 0, 0, 8, 16, west));
        out.add(bitsandbalance$bakeFaceQuad(Direction.EAST, 8, 0, 16, 16, east));
        out.add(bitsandbalance$bakeFaceQuad(Direction.UP, 0, 8, 16, 16, up));
        out.add(bitsandbalance$bakeFaceQuad(Direction.DOWN, 0, 0, 16, 8, down));

        return out;
    }

    @Unique
    private static BakedQuad bitsandbalance$bakeFaceQuad(Direction direction, float u0, float v0, float u1, float v1, bitsandbalance$FaceData face) {
        return bitsandbalance$bakeFaceQuad(direction, u0, v0, u1, v1, face, bitsandbalance$VERTICAL_SLAB_FROM, bitsandbalance$VERTICAL_SLAB_TO);
    }

    @Unique
    private static BakedQuad bitsandbalance$bakeFaceQuad(Direction direction, float u0, float v0, float u1, float v1,
                                                          bitsandbalance$FaceData face, Vector3fc from, Vector3fc to) {
        // Delegate to vanilla FaceBakery so UV orientation & wrapping matches JSON model baking.
        BlockElementFace.UVs uvs = new BlockElementFace.UVs(u0, v0, u1, v1);
        BlockElementFace elementFace = new BlockElementFace(null, face.tintIndex(), "#texture", uvs, Quadrant.R0);
        return FaceBakery.bakeQuad(
                bitsandbalance$PART_CACHE,
                from,
                to,
                elementFace,
                face.sprite(),
                direction,
                bitsandbalance$NO_MODEL_TRANSFORM,
                null,
                face.shade(),
                face.lightEmission()
        );
    }

    @Unique
    private static EnumMap<Direction, bitsandbalance$FaceData> bitsandbalance$extractFaceData(List<BakedQuad> sourceQuads) {
        EnumMap<Direction, bitsandbalance$FaceData> byDir = new EnumMap<>(Direction.class);
        bitsandbalance$FaceData any = null;
        for (BakedQuad q : sourceQuads) {
            if (q == null) continue;
            Direction d = q.direction();
            if (d == null) continue;
            TextureAtlasSprite sprite = q.sprite();
            if (sprite == null) continue;
            bitsandbalance$FaceData fd = new bitsandbalance$FaceData(sprite, q.tintIndex(), q.shade(), q.lightEmission());
            if (any == null) any = fd;
            byDir.putIfAbsent(d, fd);
        }
        // Store fallback as a convention: if 'any' is non-null, at least one face was found.
        if (any != null) {
            for (Direction d : Direction.values()) {
                byDir.putIfAbsent(d, any);
            }
        }
        return byDir;
    }

    /** Step prism: [8,0,0] → [16,8,16] — horizontal quarter-block. */
    @Unique
    private static List<BakedQuad> bitsandbalance$buildStepPrismFromLayer(List<BakedQuad> sourceQuads) {
        if (sourceQuads == null || sourceQuads.isEmpty()) return null;
        EnumMap<Direction, bitsandbalance$FaceData> byDir = bitsandbalance$extractFaceData(sourceQuads);
        if (byDir.isEmpty()) return null;

        List<BakedQuad> out = new ArrayList<>(6);
        // UVs match assets/bitsandbalance/models/item/step.json
        out.add(bitsandbalance$bakeFaceQuad(Direction.NORTH, 0, 8, 8, 16, byDir.get(Direction.NORTH), bitsandbalance$STEP_FROM, bitsandbalance$STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.SOUTH, 8, 8, 16, 16, byDir.get(Direction.SOUTH), bitsandbalance$STEP_FROM, bitsandbalance$STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.WEST,  0, 8, 16, 16, byDir.get(Direction.WEST),  bitsandbalance$STEP_FROM, bitsandbalance$STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.EAST,  0, 8, 16, 16, byDir.get(Direction.EAST),  bitsandbalance$STEP_FROM, bitsandbalance$STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.UP,    8, 0, 16, 16, byDir.get(Direction.UP),    bitsandbalance$STEP_FROM, bitsandbalance$STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.DOWN,  8, 0, 16, 16, byDir.get(Direction.DOWN),  bitsandbalance$STEP_FROM, bitsandbalance$STEP_TO));
        return out;
    }

    /** Vertical step prism: [8,0,0] → [16,16,8] — vertical quarter-block. */
    @Unique
    private static List<BakedQuad> bitsandbalance$buildVerticalStepPrismFromLayer(List<BakedQuad> sourceQuads) {
        if (sourceQuads == null || sourceQuads.isEmpty()) return null;
        EnumMap<Direction, bitsandbalance$FaceData> byDir = bitsandbalance$extractFaceData(sourceQuads);
        if (byDir.isEmpty()) return null;

        List<BakedQuad> out = new ArrayList<>(6);
        // UVs match assets/bitsandbalance/models/item/vertical_step.json
        out.add(bitsandbalance$bakeFaceQuad(Direction.NORTH, 8, 0, 16, 16, byDir.get(Direction.NORTH), bitsandbalance$VERTICAL_STEP_FROM, bitsandbalance$VERTICAL_STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.SOUTH, 8, 0, 16, 16, byDir.get(Direction.SOUTH), bitsandbalance$VERTICAL_STEP_FROM, bitsandbalance$VERTICAL_STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.WEST,  0, 0,  8, 16, byDir.get(Direction.WEST),  bitsandbalance$VERTICAL_STEP_FROM, bitsandbalance$VERTICAL_STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.EAST,  0, 0,  8, 16, byDir.get(Direction.EAST),  bitsandbalance$VERTICAL_STEP_FROM, bitsandbalance$VERTICAL_STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.UP,    8, 0, 16,  8, byDir.get(Direction.UP),    bitsandbalance$VERTICAL_STEP_FROM, bitsandbalance$VERTICAL_STEP_TO));
        out.add(bitsandbalance$bakeFaceQuad(Direction.DOWN,  8, 0, 16,  8, byDir.get(Direction.DOWN),  bitsandbalance$VERTICAL_STEP_FROM, bitsandbalance$VERTICAL_STEP_TO));
        return out;
    }

    /** Rotates a quad around the X axis by {@code steps} × 90° CCW, keeping a slab in-bounds. */
    @Unique
    private static BakedQuad bitsandbalance$rotateQuadX(BakedQuad quad, int steps) {
        steps = steps & 3;
        if (steps == 0) return quad;

        Vector3fc[] rp = new Vector3fc[4];
        float minZ = Float.POSITIVE_INFINITY;
        for (int v = 0; v < 4; v++) {
            Vector3fc pos = quad.position(v);
            float x = pos.x();
            float y = pos.y();
            float z = pos.z();

            float ry = y;
            float rz = z;
            // Rotate around origin in the YZ plane.
            for (int i = 0; i < steps; i++) {
                float ny = -rz;
                float nz = ry;
                ry = ny;
                rz = nz;
            }

            // Translate so we remain in [0..1]. For the common case (steps=1): y' = -z, z' = y.
            if (steps == 1) {
                ry += 1.0f;
            } else if (steps == 2) {
                ry += 1.0f;
                rz += 1.0f;
            } else if (steps == 3) {
                rz += 1.0f;
            }

            if (rz < minZ) minZ = rz;
            rp[v] = new Vector3f(x, ry, rz);
        }

        // For steps=3 (-90°), a GUI-specific slab item model may be authored as a *top* slab.
        // That rotates into the *front* half (z in [0, 0.5]). The vertical slab template is
        // defined in the back half (z in [0.5, 1.0]), so shift forward results back by +0.5.
        if (steps == 3 && minZ < 0.5f) {
            for (int v = 0; v < 4; v++) {
                Vector3fc p = rp[v];
                rp[v] = new Vector3f(p.x(), p.y(), p.z() + 0.5f);
            }
        }

        Direction dir = quad.direction();
        Direction rotatedDir = bitsandbalance$rotateDirX(dir, steps);

        return new BakedQuad(
                rp[0], rp[1], rp[2], rp[3],
                quad.packedUV0(), quad.packedUV1(),
                quad.packedUV2(), quad.packedUV3(),
                quad.tintIndex(),
                rotatedDir,
                quad.sprite(), quad.shade(),
                quad.lightEmission()
        );
    }

    @Unique
    private static Direction bitsandbalance$rotateDirX(Direction dir, int steps) {
        if (dir == null) return null;
        steps = steps & 3;
        if (steps == 0) return dir;

        Direction out = dir;
        for (int i = 0; i < steps; i++) {
            out = switch (out) {
                case UP -> Direction.SOUTH;
                case SOUTH -> Direction.DOWN;
                case DOWN -> Direction.NORTH;
                case NORTH -> Direction.UP;
                default -> out;
            };
        }
        return out;
    }

    @Unique
    private static Identifier bitsandbalance$modelIdForEnhancedSlabStack(ItemStack original) {
        Item item = original.getItem();
        if (!(item instanceof BlockItem blockItem)) return null;

        Block block = blockItem.getBlock();
        Block slabBlock = null;
        if (block instanceof FixedVerticalSlabBlock fixed) {
            slabBlock = fixed.getSourceSlab();
        } else if (block instanceof FixedStepBlock fixedStep) {
            slabBlock = fixedStep.getSourceSlab();
        } else if (block instanceof FixedVerticalStepBlock fixedVerticalStep) {
            Block sourceVerticalSlab = fixedVerticalStep.getSourceVerticalSlab();
            slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
            if (slabBlock == null) {
                slabBlock = sourceVerticalSlab;
            }
        } else {
            slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(block);
            if (slabBlock == null) {
                slabBlock = StepDynamicRegistry.getSlabForStep(block);
            }
            if (slabBlock == null) {
                Block sourceVerticalSlab = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(block);
                slabBlock = sourceVerticalSlab == null
                        ? null
                        : VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
                if (slabBlock == null) {
                    slabBlock = sourceVerticalSlab;
                }
            }
        }
        if (slabBlock == null) return null;

        Item slabItem = slabBlock.asItem();
        if (slabItem == null || slabItem == Items.AIR || slabItem == item) return null;

        return BuiltInRegistries.ITEM.getKey(slabItem);
    }

    @Unique
    private static ItemTransform bitsandbalance$enhancedDisplayTransform(ItemDisplayContext displayContext, int blockType) {
        if (displayContext == null) return null;

        ItemTransform fromTemplate = bitsandbalance$templateDisplayTransform(displayContext, blockType);
        if (fromTemplate != null) return fromTemplate;

        return switch (displayContext) {
            case THIRD_PERSON_RIGHT_HAND -> new ItemTransform(
                    new Vector3f(75.0f, 45.0f, 0.0f),
                    new Vector3f(-1.25f / 16.0f, -0.25f / 16.0f, 1.5f / 16.0f),
                    new Vector3f(0.375f, 0.375f, 0.375f)
            );
            case THIRD_PERSON_LEFT_HAND -> new ItemTransform(
                    new Vector3f(75.0f, 45.0f, 0.0f),
                    new Vector3f(0.0f / 16.0f, 2.5f / 16.0f, 0.0f / 16.0f),
                    new Vector3f(0.375f, 0.375f, 0.375f)
            );
            case FIRST_PERSON_RIGHT_HAND -> new ItemTransform(
                    new Vector3f(0.0f, 45.0f, 0.0f),
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Vector3f(0.4f, 0.4f, 0.4f)
            );
            case FIRST_PERSON_LEFT_HAND -> new ItemTransform(
                    new Vector3f(0.0f, -135.0f, 0.0f),
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Vector3f(0.4f, 0.4f, 0.4f)
            );
            case GROUND -> new ItemTransform(
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Vector3f(-0.75f / 16.0f, 3.0f / 16.0f, 0.0f / 16.0f),
                    new Vector3f(0.25f, 0.25f, 0.25f)
            );
            case GUI -> new ItemTransform(
                    new Vector3f(30.0f, -135.0f, 0.0f),
                    new Vector3f(1.75f / 16.0f, 2.5f / 16.0f, 0.0f / 16.0f),
                    new Vector3f(0.625f, 0.625f, 0.625f)
            );
            case HEAD -> new ItemTransform(
                    new Vector3f(103.0f, 0.0f, 0.0f),
                    new Vector3f(0.0f / 16.0f, 8.0f / 16.0f, 1.75f / 16.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f)
            );
            case FIXED -> new ItemTransform(
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Vector3f(0.5f, 0.5f, 0.5f)
            );
            case ON_SHELF -> new ItemTransform(
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    new Vector3f(-3.75f / 16.0f, 0.0f / 16.0f, 1.0f / 16.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f)
            );
            default -> null;
        };
    }

    @Unique
    private static ItemTransform bitsandbalance$templateDisplayTransform(ItemDisplayContext displayContext, int blockType) {
        if (displayContext == null) return null;

        long now = System.currentTimeMillis();
        EnumMap<ItemDisplayContext, ItemTransform> map = bitsandbalance$getTemplateTransformCache(blockType);
        long lastLoadMs = bitsandbalance$getTemplateTransformsLastLoadMs(blockType);
        if (map == null || (now - lastLoadMs) > 5_000L) {
            EnumMap<ItemDisplayContext, ItemTransform> loaded = bitsandbalance$loadTemplateTransforms(blockType);
            bitsandbalance$setTemplateTransformsLastLoadMs(blockType, now);
            if (loaded != null) {
                bitsandbalance$setTemplateTransformCache(blockType, loaded);
                map = loaded;
            }
        }

        return map == null ? null : map.get(displayContext);
    }

    @Unique
    private static EnumMap<ItemDisplayContext, ItemTransform> bitsandbalance$getTemplateTransformCache(int blockType) {
        return switch (blockType) {
            case bitsandbalance$ENHANCED_BLOCK_STEP -> bitsandbalance$stepTemplateDisplayTransforms;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP -> bitsandbalance$verticalStepTemplateDisplayTransforms;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB -> bitsandbalance$verticalSlabTemplateDisplayTransforms;
            default -> bitsandbalance$verticalSlabTemplateDisplayTransforms;
        };
    }

    @Unique
    private static void bitsandbalance$setTemplateTransformCache(int blockType, EnumMap<ItemDisplayContext, ItemTransform> map) {
        switch (blockType) {
            case bitsandbalance$ENHANCED_BLOCK_STEP -> bitsandbalance$stepTemplateDisplayTransforms = map;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP -> bitsandbalance$verticalStepTemplateDisplayTransforms = map;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB, bitsandbalance$ENHANCED_BLOCK_NONE -> bitsandbalance$verticalSlabTemplateDisplayTransforms = map;
            default -> bitsandbalance$verticalSlabTemplateDisplayTransforms = map;
        }
    }

    @Unique
    private static long bitsandbalance$getTemplateTransformsLastLoadMs(int blockType) {
        return switch (blockType) {
            case bitsandbalance$ENHANCED_BLOCK_STEP -> bitsandbalance$stepTemplateDisplayTransformsLastLoadMs;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP -> bitsandbalance$verticalStepTemplateDisplayTransformsLastLoadMs;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB -> bitsandbalance$verticalSlabTemplateDisplayTransformsLastLoadMs;
            default -> bitsandbalance$verticalSlabTemplateDisplayTransformsLastLoadMs;
        };
    }

    @Unique
    private static void bitsandbalance$setTemplateTransformsLastLoadMs(int blockType, long value) {
        switch (blockType) {
            case bitsandbalance$ENHANCED_BLOCK_STEP -> bitsandbalance$stepTemplateDisplayTransformsLastLoadMs = value;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP -> bitsandbalance$verticalStepTemplateDisplayTransformsLastLoadMs = value;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB, bitsandbalance$ENHANCED_BLOCK_NONE -> bitsandbalance$verticalSlabTemplateDisplayTransformsLastLoadMs = value;
            default -> bitsandbalance$verticalSlabTemplateDisplayTransformsLastLoadMs = value;
        }
    }

    @Unique
    private static Identifier bitsandbalance$templateResourceForBlockType(int blockType) {
        return switch (blockType) {
            case bitsandbalance$ENHANCED_BLOCK_STEP -> bitsandbalance$STEP_TEMPLATE_MODEL_RESOURCE;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_STEP -> bitsandbalance$VERTICAL_STEP_TEMPLATE_MODEL_RESOURCE;
            case bitsandbalance$ENHANCED_BLOCK_VERTICAL_SLAB -> bitsandbalance$VERTICAL_SLAB_TEMPLATE_MODEL_RESOURCE;
            default -> bitsandbalance$VERTICAL_SLAB_TEMPLATE_MODEL_RESOURCE;
        };
    }

    @Unique
    private static EnumMap<ItemDisplayContext, ItemTransform> bitsandbalance$loadTemplateTransforms(int blockType) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            ResourceManager rm = mc.getResourceManager();
            if (rm == null) return null;

            var opt = rm.getResource(bitsandbalance$templateResourceForBlockType(blockType));
            if (opt.isEmpty()) return null;

            StringBuilder sb = new StringBuilder();
            try (var in = opt.get().open();
                 var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            JsonElement parsed = JsonParser.parseString(sb.toString());
            if (!(parsed instanceof JsonObject root)) return null;
            JsonObject display = root.getAsJsonObject("display");
            if (display == null) return null;

            EnumMap<ItemDisplayContext, ItemTransform> out = new EnumMap<>(ItemDisplayContext.class);
            bitsandbalance$tryReadDisplayTransform(display, "thirdperson_righthand", ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, out);
            bitsandbalance$tryReadDisplayTransform(display, "thirdperson_lefthand", ItemDisplayContext.THIRD_PERSON_LEFT_HAND, out);
            bitsandbalance$tryReadDisplayTransform(display, "firstperson_righthand", ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, out);
            bitsandbalance$tryReadDisplayTransform(display, "firstperson_lefthand", ItemDisplayContext.FIRST_PERSON_LEFT_HAND, out);
            bitsandbalance$tryReadDisplayTransform(display, "ground", ItemDisplayContext.GROUND, out);
            bitsandbalance$tryReadDisplayTransform(display, "gui", ItemDisplayContext.GUI, out);
            bitsandbalance$tryReadDisplayTransform(display, "head", ItemDisplayContext.HEAD, out);
            bitsandbalance$tryReadDisplayTransform(display, "fixed", ItemDisplayContext.FIXED, out);
            bitsandbalance$tryReadDisplayTransform(display, "on_shelf", ItemDisplayContext.ON_SHELF, out);
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static void bitsandbalance$tryReadDisplayTransform(
            JsonObject displayRoot,
            String key,
            ItemDisplayContext displayContext,
            EnumMap<ItemDisplayContext, ItemTransform> out
    ) {
        try {
            if (displayRoot == null || key == null || displayContext == null || out == null) return;
            JsonObject obj = displayRoot.getAsJsonObject(key);
            if (obj == null) return;

            Vector3f rotationDeg = bitsandbalance$readVec3(obj, "rotation", 0.0f, 0.0f, 0.0f);
            Vector3f translationPx = bitsandbalance$readVec3(obj, "translation", 0.0f, 0.0f, 0.0f);
            Vector3f scale = bitsandbalance$readVec3(obj, "scale", 1.0f, 1.0f, 1.0f);

            // Model JSON translation is in pixels; ItemTransform expects 1 unit = 1 block.
            Vector3f translation = new Vector3f(
                    translationPx.x / 16.0f,
                    translationPx.y / 16.0f,
                    translationPx.z / 16.0f
            );

            out.put(displayContext, new ItemTransform(rotationDeg, translation, scale));
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static Vector3f bitsandbalance$readVec3(JsonObject obj, String key, float dx, float dy, float dz) {
        try {
            if (obj == null || key == null) return new Vector3f(dx, dy, dz);
            JsonArray arr = obj.getAsJsonArray(key);
            if (arr == null || arr.size() < 3) return new Vector3f(dx, dy, dz);
            float x = arr.get(0).getAsFloat();
            float y = arr.get(1).getAsFloat();
            float z = arr.get(2).getAsFloat();
            return new Vector3f(x, y, z);
        } catch (Throwable ignored) {
            return new Vector3f(dx, dy, dz);
        }
    }

    @Unique
    private static boolean bitsandbalance$hasItemDefinitionJson(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (bitsandbalance$modelIdForEnhancedSlabStack(stack) != null) return false;

        Identifier ownItemId;
        try {
            ownItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        } catch (Throwable t) {
            return false;
        }
        if (ownItemId == null) return false;

        Identifier resourceId;
        try {
            resourceId = Identifier.fromNamespaceAndPath(
                    ownItemId.getNamespace(),
                    "items/" + ownItemId.getPath() + ".json"
            );
        } catch (Throwable t) {
            return false;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return false;
            ResourceManager rm = mc.getResourceManager();
            if (rm == null) return false;

            List<?> stackResources = rm.getResourceStack(resourceId);
            return stackResources != null && !stackResources.isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
