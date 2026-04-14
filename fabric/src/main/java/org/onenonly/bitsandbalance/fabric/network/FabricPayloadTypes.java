package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Registers all CustomPacketPayload types/codecs used by the Fabric port. */
public final class FabricPayloadTypes {
    private FabricPayloadTypes() {
    }

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        PayloadTypeRegistry<RegistryFriendlyByteBuf> serverbound = playRegistry(
            new String[]{"playC2S", "serverboundPlay"},
            new String[]{"PLAY_C2S", "SERVERBOUND_PLAY"}
        );
        PayloadTypeRegistry<RegistryFriendlyByteBuf> clientbound = playRegistry(
            new String[]{"playS2C", "clientboundPlay"},
            new String[]{"PLAY_S2C", "CLIENTBOUND_PLAY"}
        );

        // Client -> Server
        serverbound.register(SitTogglePayload.TYPE, SitTogglePayload.STREAM_CODEC);
        serverbound.register(CrawlTogglePayload.TYPE, CrawlTogglePayload.STREAM_CODEC);
        serverbound.register(ItemSharePayload.TYPE, ItemSharePayload.STREAM_CODEC);
        serverbound.register(DoorKnockPayload.TYPE, DoorKnockPayload.STREAM_CODEC);
        serverbound.register(NavigatorCompassSetPayload.TYPE, NavigatorCompassSetPayload.STREAM_CODEC);
        serverbound.register(QuickHarvestPayload.TYPE, QuickHarvestPayload.STREAM_CODEC);

        // Server -> Client
        clientbound.register(SyncSitPayload.TYPE, SyncSitPayload.STREAM_CODEC);
        clientbound.register(BioluminescenceSyncPayload.TYPE, BioluminescenceSyncPayload.STREAM_CODEC);
        clientbound.register(SoulFireBurnSyncPayload.TYPE, SoulFireBurnSyncPayload.STREAM_CODEC);
        clientbound.register(SnowballPowderSnowPayload.TYPE, SnowballPowderSnowPayload.STREAM_CODEC);
        clientbound.register(NavigatorCompassOpenGUIPayload.TYPE, NavigatorCompassOpenGUIPayload.STREAM_CODEC);

        // Candle bundling per-candle colors (Server -> Client)
        clientbound.register(CandleBundleColorsUpdatePayload.TYPE, CandleBundleColorsUpdatePayload.STREAM_CODEC);
        clientbound.register(CandleBundleColorsBulkPayload.TYPE, CandleBundleColorsBulkPayload.STREAM_CODEC);

        // Candle bundling per-slot contents (Server -> Client)
        clientbound.register(CandleBundleContentsUpdatePayload.TYPE, CandleBundleContentsUpdatePayload.STREAM_CODEC);

        // Taboret Table craft request (Client -> Server)
        serverbound.register(TaboretCraftPayload.TYPE, TaboretCraftPayload.STREAM_CODEC);
    }

    @SuppressWarnings("unchecked")
    private static PayloadTypeRegistry<RegistryFriendlyByteBuf> playRegistry(String[] methodNames, String[] fieldNames) {
        for (String methodName : methodNames) {
            try {
                return (PayloadTypeRegistry<RegistryFriendlyByteBuf>) PayloadTypeRegistry.class.getMethod(methodName).invoke(null);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            Class<?> impl = Class.forName("net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl");
            for (String fieldName : fieldNames) {
                try {
                    return (PayloadTypeRegistry<RegistryFriendlyByteBuf>) impl.getField(fieldName).get(null);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        throw new IllegalStateException("Unable to resolve Fabric payload registry for " + String.join("/", methodNames));
    }
}
