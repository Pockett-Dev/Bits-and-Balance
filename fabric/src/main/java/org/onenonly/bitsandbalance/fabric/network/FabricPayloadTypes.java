package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

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

        // Client -> Server
        PayloadTypeRegistry.playC2S().register(SitTogglePayload.TYPE, SitTogglePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CrawlTogglePayload.TYPE, CrawlTogglePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ItemSharePayload.TYPE, ItemSharePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DoorKnockPayload.TYPE, DoorKnockPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(NavigatorCompassSetPayload.TYPE, NavigatorCompassSetPayload.STREAM_CODEC);

        // Server -> Client
        PayloadTypeRegistry.playS2C().register(SyncSitPayload.TYPE, SyncSitPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SnowballPowderSnowPayload.TYPE, SnowballPowderSnowPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(NavigatorCompassOpenGUIPayload.TYPE, NavigatorCompassOpenGUIPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BioluminescenceSyncPayload.TYPE, BioluminescenceSyncPayload.STREAM_CODEC);

        // Candle bundling per-candle colors (Server -> Client)
        PayloadTypeRegistry.playS2C().register(CandleBundleColorsUpdatePayload.TYPE, CandleBundleColorsUpdatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CandleBundleColorsBulkPayload.TYPE, CandleBundleColorsBulkPayload.STREAM_CODEC);

        // Candle bundling per-slot contents (Server -> Client)
        PayloadTypeRegistry.playS2C().register(CandleBundleContentsUpdatePayload.TYPE, CandleBundleContentsUpdatePayload.STREAM_CODEC);

        // Taboret Table craft request (Client -> Server)
        PayloadTypeRegistry.playC2S().register(TaboretCraftPayload.TYPE, TaboretCraftPayload.STREAM_CODEC);
    }
}
