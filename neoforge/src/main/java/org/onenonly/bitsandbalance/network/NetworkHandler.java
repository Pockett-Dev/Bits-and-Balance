package org.onenonly.bitsandbalance.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.onenonly.bitsandbalance.BitsAndBalance;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class NetworkHandler {

    private static final java.util.Map<java.util.UUID, Long> LAST_ITEM_SHARE_AT_MS = new java.util.HashMap<>();

    private static final String SIT_X_KEY = "bitsandbalance_sit_x";
    private static final String SIT_Y_KEY = "bitsandbalance_sit_y";
    private static final String SIT_Z_KEY = "bitsandbalance_sit_z";
    private static final String SIT_DIM_KEY = "bitsandbalance_sit_dim";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(BitsAndBalance.MODID).versioned("1.0");
        // Server-side packet handlers
        registrar.playToServer(org.onenonly.bitsandbalance.mechanics.CrawlPayload.TYPE, org.onenonly.bitsandbalance.mechanics.CrawlPayload.STREAM_CODEC, NetworkHandler::handleCrawlToggle);
        registrar.playToServer(org.onenonly.bitsandbalance.mechanics.SneakPayload.TYPE, org.onenonly.bitsandbalance.mechanics.SneakPayload.STREAM_CODEC, NetworkHandler::handleSneakToggle);
        registrar.playToServer(org.onenonly.bitsandbalance.mechanics.ItemSharePayload.TYPE, org.onenonly.bitsandbalance.mechanics.ItemSharePayload.STREAM_CODEC, NetworkHandler::handleItemShare);
        registrar.playToServer(org.onenonly.bitsandbalance.mechanics.DoorKnockPayload.TYPE, org.onenonly.bitsandbalance.mechanics.DoorKnockPayload.STREAM_CODEC, NetworkHandler::handleDoorKnock);
        registrar.playToServer(org.onenonly.bitsandbalance.mechanics.NavigatorCompassSetPayload.TYPE, org.onenonly.bitsandbalance.mechanics.NavigatorCompassSetPayload.STREAM_CODEC, NetworkHandler::handleNavigatorCompassSet);
        registrar.playToServer(org.onenonly.bitsandbalance.mechanics.SitPayload.TYPE, org.onenonly.bitsandbalance.mechanics.SitPayload.STREAM_CODEC, NetworkHandler::handleSitToggle);

        // Register client-side channels on server (empty handlers - actual handling done on client)
        registrar.playToClient(SnowballPowderSnowPayload.TYPE, SnowballPowderSnowPayload.STREAM_CODEC, (payload, ctx) -> {
            // Only process on client side
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handleSnowballPowderSnow(payload, ctx);
            }
        });
        registrar.playToClient(org.onenonly.bitsandbalance.network.SyncSitPayload.TYPE, org.onenonly.bitsandbalance.network.SyncSitPayload.STREAM_CODEC, (payload, ctx) -> {
            // Only process on client side
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handleSitSync(payload, ctx);
            }
        });
        registrar.playToClient(NavigatorCompassOpenGUIPayload.TYPE, NavigatorCompassOpenGUIPayload.STREAM_CODEC, (payload, ctx) -> {
            // Only process on client side
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handleNavigatorCompassOpenGUI(payload, ctx);
            }
        });
        registrar.playToClient(PlayerMentionPayload.TYPE, PlayerMentionPayload.STREAM_CODEC, (payload, ctx) -> {
            // Only process on client side
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handlePlayerMention(payload, ctx);
            }
        });
        registrar.playToClient(BioluminescenceSyncPayload.TYPE, BioluminescenceSyncPayload.STREAM_CODEC, (payload, ctx) -> {
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handleBioluminescenceSync(payload, ctx);
            }
        });

        registrar.playToClient(CandleBundleColorsUpdatePayload.TYPE, CandleBundleColorsUpdatePayload.STREAM_CODEC, (payload, ctx) -> {
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handleCandleBundleColorsUpdate(payload, ctx);
            }
        });
        registrar.playToClient(CandleBundleContentsUpdatePayload.TYPE, CandleBundleContentsUpdatePayload.STREAM_CODEC, (payload, ctx) -> {
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handleCandleBundleContentsUpdate(payload, ctx);
            }
        });
        registrar.playToClient(CandleBundleColorsBulkPayload.TYPE, CandleBundleColorsBulkPayload.STREAM_CODEC, (payload, ctx) -> {
            if (ctx.player().level().isClientSide()) {
                NetworkHandlerClient.handleCandleBundleColorsBulk(payload, ctx);
            }
        });
    }

    private static void handleCrawlToggle(final org.onenonly.bitsandbalance.mechanics.CrawlPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            if (!org.onenonly.bitsandbalance.Config.enableCrawlingMechanic) return;
            boolean crawl = payload.crawling();
            try {
                if (crawl) {
                    sp.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);
                } else {
                    sp.setForcedPose(null);
                }
            } catch (Throwable ignored) {
            }
            try {
                // Persist desired crawl state
                sp.getPersistentData().putBoolean("bitsandbalance_crawling", crawl);
            } catch (Throwable ignored) {
            }
            // When crawling, ensure server-side sneaking is disabled to avoid conflicts
            if (crawl) {
                try {
                    sp.setShiftKeyDown(false);
                } catch (Throwable ignored) {
                }
            }
        });
    }
 
    private static void handleSneakToggle(final org.onenonly.bitsandbalance.mechanics.SneakPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            boolean sneaking = payload.sneaking();

            try {
                sp.setShiftKeyDown(sneaking);
            } catch (Throwable ignored) {
            }
            try {
                sp.getPersistentData().putBoolean("bitsandbalance_sneaking", sneaking);
            } catch (Throwable ignored) {
            }

            // If sneaking, ensure mutually exclusive with crawling.
            if (sneaking) {
                try {
                    sp.setForcedPose(null);
                } catch (Throwable ignored) {
                }
                try {
                    sp.getPersistentData().putBoolean("bitsandbalance_crawling", false);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static void handleSitToggle(final org.onenonly.bitsandbalance.mechanics.SitPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var p = ctx.player();
            if (!(p instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            boolean wantSit = payload.sitting();

            // Basic validation
            boolean ok = org.onenonly.bitsandbalance.mechanics.Validation.testSit(sp);
            java.util.UUID id = sp.getUUID();

            if (!wantSit || !ok) {
                // Clear sit
                org.onenonly.bitsandbalance.BitsAndBalance.SITTING_PLAYERS.remove(id);
                try {
                    sp.getPersistentData().remove(SIT_X_KEY);
                    sp.getPersistentData().remove(SIT_Y_KEY);
                    sp.getPersistentData().remove(SIT_Z_KEY);
                    sp.getPersistentData().remove(SIT_DIM_KEY);
                } catch (Throwable ignored) {}
                try { sp.refreshDimensions(); } catch (Throwable ignored) {}
                // Sync to self and trackers
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new org.onenonly.bitsandbalance.network.SyncSitPayload(id, false));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(sp, new org.onenonly.bitsandbalance.network.SyncSitPayload(id, false));
            } else {
                // Sitting: ensure mutually exclusive with crawling/sneak
                try { sp.setShiftKeyDown(false); } catch (Throwable ignored) {}
                try { sp.setForcedPose(null); } catch (Throwable ignored) {}
                try { sp.getPersistentData().putBoolean("bitsandbalance_crawling", false); } catch (Throwable ignored) {}

                org.onenonly.bitsandbalance.BitsAndBalance.SITTING_PLAYERS.add(id);
                try {
                    // Record an anchor position so any movement/knockback cancels sitting.
                    sp.getPersistentData().putLong(SIT_X_KEY, Double.doubleToRawLongBits(sp.getX()));
                    sp.getPersistentData().putLong(SIT_Y_KEY, Double.doubleToRawLongBits(sp.getY()));
                    sp.getPersistentData().putLong(SIT_Z_KEY, Double.doubleToRawLongBits(sp.getZ()));
                    String dimId;
                    try {
                        dimId = sp.level().dimension().identifier().toString();
                    } catch (Throwable ignored) {
                        dimId = "";
                    }
                    sp.getPersistentData().putString(SIT_DIM_KEY, dimId);
                } catch (Throwable ignored) {
                }
                try { sp.refreshDimensions(); } catch (Throwable ignored) {}

                boolean finalSit = org.onenonly.bitsandbalance.BitsAndBalance.SITTING_PLAYERS.contains(id);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new org.onenonly.bitsandbalance.network.SyncSitPayload(id, finalSit));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(sp, new org.onenonly.bitsandbalance.network.SyncSitPayload(id, finalSit));
            }
        });
    }

        private static void handleItemShare(final org.onenonly.bitsandbalance.mechanics.ItemSharePayload payload, final net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;

            long cooldownMs = (long) (Math.max(0.0D, org.onenonly.bitsandbalance.Config.itemShareCooldownSeconds) * 1000.0D);
            if (cooldownMs > 0L) {
                long now = System.currentTimeMillis();
                java.util.UUID id = sp.getUUID();
                Long last = LAST_ITEM_SHARE_AT_MS.get(id);
                if (last != null) {
                    long elapsed = now - last;
                    if (elapsed >= 0L && elapsed < cooldownMs) {
                        return;
                    }
                }
                LAST_ITEM_SHARE_AT_MS.put(id, now);
            }

            var stack = payload.stack();
            if (stack == null || stack.isEmpty()) return;
            // Build a vanilla-like player chat prefix for Chat Heads compatibility
            net.minecraft.network.chat.Component prefix = net.minecraft.network.chat.Component.literal("<" + sp.getName().getString() + "> ");

            // Build the item marker spaces carrying the SHOW_ITEM hover (anchor for client to draw icon).
            // ItemShareClient renders the icon when it sees a space marker and the *next* character has SHOW_ITEM.
            // We use THREE spaces so the third space becomes a visible gap between the icon and the item text.
            // Use the original stack to preserve all NBT data including enchantments and lore.
            net.minecraft.network.chat.HoverEvent hover = new net.minecraft.network.chat.HoverEvent.ShowItem(stack);
            net.minecraft.network.chat.MutableComponent spaces = net.minecraft.network.chat.Component.literal("   ").withStyle(s -> s.withHoverEvent(hover));

            // Build a bracketed item link with SHOW_ITEM hover
            net.minecraft.network.chat.Component baseName = stack.getHoverName().copy();
            net.minecraft.network.chat.Component itemLink = net.minecraft.network.chat.ComponentUtils.wrapInSquareBrackets(baseName)
                    .withStyle(style -> style.withHoverEvent(hover));

            // Compose: <Player>    [Item Name] (spaces reserve room for the icon; 3rd space is the gap after the icon)
            // This format should be detected by Chat Heads mod as a player message
            net.minecraft.network.chat.MutableComponent msg = net.minecraft.network.chat.Component.empty().copy().append(prefix).append(spaces).append(itemLink);
            try {
                // Send as system message but format it to look like a player message
                // This maintains compatibility while making it appear as if from the player
                var server = sp.level().getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(msg, false);
                } else {
                    sp.sendSystemMessage(msg);
                }
            } catch (Throwable ignored) {
                try {
                    sp.sendSystemMessage(msg);
                } catch (Throwable ignored2) {
                    // Final fallback
                }
            }
        });
        }

    private static void handleDoorKnock(final org.onenonly.bitsandbalance.mechanics.DoorKnockPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            if (!org.onenonly.bitsandbalance.Config.enableDoorKnocking) return;
            
            var doorPos = payload.doorPos();
            var level = sp.level();
            var state = level.getBlockState(doorPos);
            
            // Verify it's still a door block
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)) return;
            
            // Use the existing door knocking logic from DoorKnockingModule
            org.onenonly.bitsandbalance.mechanics.modules.DoorKnockingModule.handleKeybindKnock(sp, doorPos, state);
        });
    }

    private static void handleNavigatorCompassSet(final org.onenonly.bitsandbalance.mechanics.NavigatorCompassSetPayload payload, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                return;
            }
            
            // Check if Navigator Compass is enabled
            if (!org.onenonly.bitsandbalance.Config.enableNavigatorCompass) {
                return;
            }
            
            // Check if player is holding a compass
            var mainHand = serverPlayer.getMainHandItem();
            var offHand = serverPlayer.getOffhandItem();
            boolean holdingCompass = mainHand.is(net.minecraft.world.item.Items.COMPASS) || offHand.is(net.minecraft.world.item.Items.COMPASS);
            
            if (!holdingCompass) {
                // Play error sound instead of chat message
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.5f);
                return;
            }
            
            // Get the compass being held
            var compass = mainHand.is(net.minecraft.world.item.Items.COMPASS) ? mainHand : offHand;

            // Ensure legacy lodestone tracker targets get migrated once.
            org.onenonly.bitsandbalance.mechanics.NavigatorCompassItemData.migrateLegacyLodestoneTargetIfPresent(compass);
            
            // Check if this is a clear command (special coordinates)
            if (payload.x() == Integer.MAX_VALUE && payload.y() == Integer.MAX_VALUE && payload.z() == Integer.MAX_VALUE) {
                org.onenonly.bitsandbalance.mechanics.modules.NavigatorCompassModule.clearCompassTarget(compass);
                // Play positive success sound for clearing
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
            } else {
                // Set the compass target
                net.minecraft.core.BlockPos targetPos = new net.minecraft.core.BlockPos(payload.x(), payload.y(), payload.z());
                org.onenonly.bitsandbalance.mechanics.modules.NavigatorCompassModule.setCompassTarget(compass, (net.minecraft.server.level.ServerLevel) serverPlayer.level(), targetPos);
                // Play positive success sound for setting
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        });
    }



}

