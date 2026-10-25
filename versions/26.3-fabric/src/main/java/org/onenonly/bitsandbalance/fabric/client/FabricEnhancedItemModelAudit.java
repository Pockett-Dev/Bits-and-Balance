package org.onenonly.bitsandbalance.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3fc;
import org.onenonly.bitsandbalance.mixin.ItemStackRenderStateAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Development-only runtime check for the 26.3 ItemQuads item-model path. */
public final class FabricEnhancedItemModelAudit {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-item-model-audit");
    private static final float EPSILON = 0.02F;
    private static boolean completed;
    private static int readyTicks;
    private static Field quadsField;

    private FabricEnhancedItemModelAudit() {
    }

    public static void tick(Minecraft client) {
        boolean requested = Boolean.getBoolean("bitsandbalance.itemModelAudit")
                || "client-render-smoke".equals(System.getProperty("bitsandbalance.fixtureRunner"));
        if (completed || !requested) {
            return;
        }
        if (client == null || client.level == null || client.player == null) {
            readyTicks = 0;
            return;
        }
        if (++readyTicks < 10) {
            return;
        }

        completed = true;
        List<String> details = new ArrayList<>();
        boolean passed = true;
        passed &= check(client, "vertical-oak-slab", "bitsandbalance:vertical_oak_slab", 1.0F, 1.0F, 0.5F, details);
        passed &= check(client, "oak-step", "bitsandbalance:oak_step", 0.5F, 0.5F, 1.0F, details);
        passed &= check(client, "vertical-oak-step", "bitsandbalance:vertical_oak_step", 0.5F, 1.0F, 0.5F, details);

        String joined = String.join(";", details);
        if (passed) {
            LOGGER.info("BNB_ITEM_MODEL_FIXTURE PASS details={}", joined);
        } else {
            LOGGER.error("BNB_ITEM_MODEL_FIXTURE FAIL details={}", joined);
        }
    }

    private static boolean check(
            Minecraft client,
            String name,
            String itemId,
            float expectedX,
            float expectedY,
            float expectedZ,
            List<String> details
    ) {
        try {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
            if (item == null || item == Items.AIR) {
                details.add(name + "=missing-item");
                return false;
            }

            ItemStackRenderState renderState = new ItemStackRenderState();
            client.getItemModelResolver().updateForTopItem(
                    renderState,
                    new ItemStack(item),
                    ItemDisplayContext.GUI,
                    client.level,
                    client.player,
                    0
            );

            ItemStackRenderStateAccessor access = (ItemStackRenderStateAccessor) renderState;
            ItemStackRenderState.LayerRenderState[] layers = access.bitsandbalance$getLayers();
            int count = Math.min(access.bitsandbalance$getActiveLayerCount(), layers == null ? 0 : layers.length);
            List<BakedQuad> quads = new ArrayList<>();
            for (int layerIndex = 0; layerIndex < count; layerIndex++) {
                ItemStackRenderState.LayerRenderState layer = layers[layerIndex];
                if (layer == null) {
                    continue;
                }
                Object stored = quadsField(layer).get(layer);
                if (stored instanceof ItemQuads itemQuads) {
                    quads.addAll(itemQuads.all());
                }
            }
            if (quads.isEmpty()) {
                details.add(name + "=empty-quads");
                return false;
            }

            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (BakedQuad quad : quads) {
                for (int vertex = 0; vertex < 4; vertex++) {
                    Vector3fc pos = quad.position(vertex);
                    minX = Math.min(minX, pos.x());
                    minY = Math.min(minY, pos.y());
                    minZ = Math.min(minZ, pos.z());
                    maxX = Math.max(maxX, pos.x());
                    maxY = Math.max(maxY, pos.y());
                    maxZ = Math.max(maxZ, pos.z());
                }
            }

            float spanX = maxX - minX;
            float spanY = maxY - minY;
            float spanZ = maxZ - minZ;
            float scale = Math.max(spanX, Math.max(spanY, spanZ));
            float normalizedX = spanX / scale;
            float normalizedY = spanY / scale;
            float normalizedZ = spanZ / scale;
            boolean matches = close(normalizedX, expectedX)
                    && close(normalizedY, expectedY)
                    && close(normalizedZ, expectedZ);
            details.add(name + "=" + (matches ? "ok" : "wrong-shape")
                    + ",quads=" + quads.size()
                    + ",normalized=" + normalizedX + "/" + normalizedY + "/" + normalizedZ);
            return matches;
        } catch (Throwable throwable) {
            details.add(name + "=exception:" + throwable.getClass().getSimpleName() + ":" + throwable.getMessage());
            return false;
        }
    }

    private static Field quadsField(ItemStackRenderState.LayerRenderState layer) throws NoSuchFieldException {
        Field field = quadsField;
        if (field == null) {
            field = layer.getClass().getDeclaredField("quads");
            field.setAccessible(true);
            quadsField = field;
        }
        return field;
    }

    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) <= EPSILON;
    }
}
