package org.onenonly.bitsandbalance.common.client;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.items.EnhancedSlabItemSupport;
import org.onenonly.bitsandbalance.common.registry.DynamicVariantMaterialHelper;

import java.util.EnumMap;
import java.util.Map;

public final class EnhancedSlabFabricRenderUtil {
    private static final int WHITE = 0xFFFFFFFF;
    private static final Map<BlendMode, RenderMaterial> MATERIALS = new EnumMap<>(BlendMode.class);

    private EnhancedSlabFabricRenderUtil() {
    }

    public static @Nullable RenderMaterial materialFor(@Nullable BlockState renderState) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) {
            return null;
        }

        RenderType renderType = renderState == null ? RenderType.solid() : ItemBlockRenderTypes.getChunkRenderType(renderState);
        BlendMode blendMode = BlendMode.fromRenderLayer(renderType);
        synchronized (MATERIALS) {
            return MATERIALS.computeIfAbsent(blendMode, mode -> renderer.materialFinder()
                    .clear()
                    .blendMode(mode)
                    .disableColorIndex(true)
                    .shadeMode(ShadeMode.VANILLA)
                    .find());
        }
    }

    public static boolean emitQuad(RenderContext context,
                                   BakedQuad quad,
                                   RenderMaterial material,
                                   @Nullable BlockAndTintGetter world,
                                   @Nullable BlockPos pos,
                                   @Nullable BlockState tintState,
                                   @Nullable Direction cullFace) {
        if (context == null || quad == null || material == null) {
            return false;
        }

        var emitter = context.getEmitter();
        emitter.fromVanilla(quad, material, cullFace);
        emitter.colorIndex(-1);
        int color = tintColor(world, pos, tintState, quad);
        emitter.color(color, color, color, color);
        emitter.emit();
        return true;
    }

    public static int tintColor(@Nullable BlockAndTintGetter world,
                                @Nullable BlockPos pos,
                                @Nullable BlockState tintState,
                                BakedQuad quad) {
        if (quad == null || quad.getTintIndex() < 0 || tintState == null) {
            return WHITE;
        }

        try {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            int color = blockColors == null ? -1 : blockColors.getColor(tintState, world, pos, quad.getTintIndex());
            return color == -1 ? WHITE : 0xFF000000 | (color & 0xFFFFFF);
        } catch (Throwable ignored) {
            return WHITE;
        }
    }

    public static @Nullable BlockState sourceRenderStateForOwner(@Nullable BlockState ownerState) {
        if (ownerState == null) {
            return null;
        }

        var sourceSlab = EnhancedSlabItemSupport.sourceSlabForBlock(ownerState.getBlock());
        if (sourceSlab != null) {
            return normalizeSlabState(sourceSlab.defaultBlockState());
        }
        return normalizeSlabState(ownerState);
    }

    public static @Nullable BlockState normalizeSlabState(@Nullable BlockState state) {
        BlockState normalized = DynamicVariantMaterialHelper.normalizeSlabState(state);
        if (normalized != null) {
            return normalized;
        }
        if (state == null) {
            return null;
        }

        BlockState result = state;
        if (result.hasProperty(SlabBlock.TYPE)) {
            result = result.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        }
        if (result.hasProperty(SlabBlock.WATERLOGGED)) {
            result = result.setValue(SlabBlock.WATERLOGGED, false);
        }
        return result;
    }
}
