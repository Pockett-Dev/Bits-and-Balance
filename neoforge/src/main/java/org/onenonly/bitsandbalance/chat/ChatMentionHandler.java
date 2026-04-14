package org.onenonly.bitsandbalance.chat;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class ChatMentionHandler {
    
    // Online player name cache (lowercase name -> actual name)
    private static final Map<String, String> onlineNameIndexLower = new ConcurrentHashMap<>();
    
    // Cooldown tracking (player UUID -> last mention time)
    private static final Object2LongMap<UUID> lastMentionTime = new Object2LongOpenHashMap<>();
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Initialize the online player cache
        refreshOnlinePlayerCache(event.getServer());
    }
    
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Clear caches when server stops
        onlineNameIndexLower.clear();
        lastMentionTime.clear();
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            addPlayerToCache(player);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removePlayerFromCache(player);
        }
    }
    
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (!Config.enableChatMentions) {
            return;
        }
        
        ServerPlayer sender = event.getPlayer();
        String rawMessage = event.getRawText();
        
        // Check cooldown
        if (!passesCooldown(sender, Config.mentionCooldownMsPerSender)) {
            return;
        }
        
        // Parse mentions
        MentionParser.MentionParseResult parse = MentionParser.findMentions(
            rawMessage,
            Config.mentionTriggerModes,
            Config.mentionCaseInsensitive,
            Config.mentionWordBoundary,
            onlineNameIndexLower.keySet(),
            Config.mentionMaxMentionsPerMessage
        );
        
        
        if (parse.isEmpty()) {
            return; // No mentions found, let vanilla handle it
        }
        
        // Check for self-mention
        if (!Config.mentionAllowSelfPing && parse.isMentioned(sender.getGameProfile().name())) {
            return; // Self-mention not allowed
        }
        
        // Update cooldown
        lastMentionTime.put(sender.getUUID(), System.currentTimeMillis());
        
        // Build base component for general audience
        Component baseMessage = Component.literal(rawMessage);
        
        // Always highlight usernames for everyone
        Component highlightedMessage = MentionRenderer.renderForAll(rawMessage, parse);
        event.setMessage(highlightedMessage);
        
        // Play ping sounds for all mentioned players
        MinecraftServer server = sender.level().getServer();
        for (String mentionedName : parse.mentionedNames()) {
            ServerPlayer mentionedPlayer = server.getPlayerList().getPlayerByName(mentionedName);
            if (mentionedPlayer != null) {
                playPing(mentionedPlayer);
            }
        }
    }
    
    /**
     * Checks if the sender has passed the cooldown period.
     */
    private static boolean passesCooldown(ServerPlayer sender, int cooldownMs) {
        if (cooldownMs <= 0) {
            return true;
        }
        
        UUID senderId = sender.getUUID();
        long lastTime = lastMentionTime.getLong(senderId);
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - lastTime) >= cooldownMs;
    }
    
    /**
     * Formats the sender prefix for chat messages.
     */
    private static Component formatSenderPrefix(ServerPlayer sender) {
        return Component.literal("<" + sender.getGameProfile().name() + "> ");
    }
    
    /**
     * Plays the ping sound for a player.
     */
    private static void playPing(ServerPlayer player) {
        try {
            SoundEvent soundEvent = getSoundEvent(Config.mentionSoundEvent);
            if (soundEvent == null) {
                return;
            }
            
            SoundSource soundSource = getSoundSource(Config.mentionSoundSource);
            float volume = (float) Config.mentionSoundVolume;
            float pitch = (float) Config.mentionSoundPitch;
            
            player.level().playSound(null, player.blockPosition(), soundEvent, soundSource, volume, pitch);
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("Failed to play mention sound: " + e.getMessage());
        }
    }
    
    /**
     * Gets a sound event from a resource location string.
     */
    private static SoundEvent getSoundEvent(String soundEventStr) {
        try {
            Identifier location = Identifier.parse(soundEventStr);
            return SoundEvent.createVariableRangeEvent(location);
        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("Invalid sound event: " + soundEventStr);
            return null;
        }
    }
    
    /**
     * Gets a sound source from a string.
     */
    private static SoundSource getSoundSource(String soundSourceStr) {
        try {
            return SoundSource.valueOf(soundSourceStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            BitsAndBalance.LOGGER.warn("Invalid sound source: " + soundSourceStr + ", using PLAYERS");
            return SoundSource.PLAYERS;
        }
    }
    
    /**
     * Refreshes the online player cache.
     */
    private static void refreshOnlinePlayerCache(MinecraftServer server) {
        onlineNameIndexLower.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            addPlayerToCache(player);
        }
    }
    
    /**
     * Adds a player to the online cache.
     */
    private static void addPlayerToCache(ServerPlayer player) {
        String name = player.getGameProfile().name();
        String nameLower = name.toLowerCase(Locale.ROOT);
        onlineNameIndexLower.put(nameLower, name);
    }
    
    /**
     * Removes a player from the online cache.
     */
    private static void removePlayerFromCache(ServerPlayer player) {
        String name = player.getGameProfile().name();
        String nameLower = name.toLowerCase(Locale.ROOT);
        onlineNameIndexLower.remove(nameLower);
    }
}
