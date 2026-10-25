package org.onenonly.bitsandbalance.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FabricChatMentions {
    private FabricChatMentions() {
    }

    private static final ConcurrentHashMap<UUID, Long> LAST_PING_BY_SENDER = new ConcurrentHashMap<>();

    public static Component processChatMessage(Component message, PlayerInfo senderInfo) {
        if (!FabricMechanicsConfig.enableChatMentions) return message;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return message;

        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return message;

        String playerName = mc.player.getGameProfile().getName();
        String messageText = message.getString();

        String senderName = senderInfo != null ? senderInfo.getProfile().getName() : null;
        UUID senderId = senderInfo != null ? senderInfo.getProfile().getId() : null;

        int contentStart = computeContentStartIndex(messageText, senderName);
        String contentText = contentStart <= 0 ? messageText : messageText.substring(contentStart);

        Set<String> onlineNames = new HashSet<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (info == null) continue;
            String name = info.getProfile().getName();
            if (name != null && !name.isBlank()) onlineNames.add(name);
        }

        if (senderName != null && !senderName.isBlank()) {
            onlineNames.remove(senderName);
        }

        List<String> mentionedNames = findMentionedNames(contentText, onlineNames);
        boolean localMentioned = isMentioned(contentText, playerName);

        if (localMentioned && passesSenderCooldown(senderId)) {
            if (senderName == null || !senderName.equals(playerName) || FabricMechanicsConfig.mentionAllowSelfPing) {
                playMentionSound();
            }
        }

        if (FabricMechanicsConfig.mentionRecipientOnlyHighlight) {
            if (!localMentioned) return message;
            return highlightNames(messageText, List.of(playerName), contentStart);
        }

        if (mentionedNames.isEmpty()) return message;
        return highlightNames(messageText, mentionedNames, contentStart);
    }

    private static int computeContentStartIndex(String messageText, String senderName) {
        if (messageText == null || messageText.isEmpty()) return 0;

        if (senderName != null && !senderName.isBlank()) {
            String token = "<" + senderName + ">";
            int idx = messageText.indexOf(token);
            if (idx >= 0) {
                int end = idx + token.length();
                if (end < messageText.length() && messageText.charAt(end) == ' ') end++;
                return end;
            }

            String token2 = senderName + ":";
            if (messageText.startsWith(token2)) {
                int end = token2.length();
                if (end < messageText.length() && messageText.charAt(end) == ' ') end++;
                return end;
            }
        }

        return 0;
    }

    public static Component processChatMessage(Component message) {
        return processChatMessage(message, null);
    }

    private static boolean passesSenderCooldown(UUID senderId) {
        int cooldownMs = FabricMechanicsConfig.mentionCooldownMsPerSender;
        if (cooldownMs <= 0) return true;
        if (senderId == null) return true;

        long now = System.currentTimeMillis();
        Long last = LAST_PING_BY_SENDER.get(senderId);
        if (last != null && (now - last) < cooldownMs) {
            return false;
        }
        LAST_PING_BY_SENDER.put(senderId, now);
        return true;
    }

    private static boolean isMentioned(String text, String playerName) {
        for (String mode : FabricMechanicsConfig.mentionTriggerModes) {
            if (mode.equalsIgnoreCase("AT_NAME")) {
                if (containsToken(text, "@" + playerName)) {
                    return true;
                }
            } else if (mode.equalsIgnoreCase("PLAIN_NAME")) {
                if (containsToken(text, playerName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> findMentionedNames(String text, Set<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        int maxMentions = FabricMechanicsConfig.mentionMaxMentionsPerMessage;
        if (maxMentions <= 0) return List.of();

        List<String> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> Integer.compare(b.length(), a.length()));

        List<String> mentioned = new ArrayList<>();
        for (String name : sorted) {
            if (mentioned.size() >= maxMentions) break;
            if (name == null || name.isBlank()) continue;

            boolean hit = false;
            for (String mode : FabricMechanicsConfig.mentionTriggerModes) {
                if (mode.equalsIgnoreCase("AT_NAME")) {
                    if (containsToken(text, "@" + name)) {
                        hit = true;
                        break;
                    }
                } else if (mode.equalsIgnoreCase("PLAIN_NAME")) {
                    if (containsToken(text, name)) {
                        hit = true;
                        break;
                    }
                }
            }

            if (hit) {
                mentioned.add(name);
            }
        }

        return mentioned;
    }

    private static boolean containsToken(String text, String token) {
        if (!FabricMechanicsConfig.mentionWordBoundary) {
            if (FabricMechanicsConfig.mentionCaseInsensitive) {
                return text.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
            }
            return text.contains(token);
        }

        Pattern pattern = createSearchPattern(token);
        return pattern.matcher(text).find();
    }

    private static Component highlightNames(String messageText, List<String> names, int contentStart) {
        List<MentionPos> positions = new ArrayList<>();

        for (String name : names) {
            if (name == null || name.isBlank()) continue;

            for (String mode : FabricMechanicsConfig.mentionTriggerModes) {
                String searchText = mode.equalsIgnoreCase("AT_NAME") ? "@" + name : name;
                Pattern pattern = createSearchPattern(searchText);
                Matcher matcher = pattern.matcher(messageText);
                while (matcher.find()) {
                    if (matcher.start() < contentStart) {
                        continue;
                    }
                    positions.add(new MentionPos(matcher.start(), matcher.end()));
                }
            }
        }

        if (positions.isEmpty()) return Component.literal(messageText);

        positions.sort((a, b) -> Integer.compare(a.start, b.start));

        MutableComponent result = Component.empty();
        int lastEnd = 0;

        for (MentionPos pos : positions) {
            if (pos.start < lastEnd) {
                continue;
            }

            if (pos.start > lastEnd) {
                result.append(Component.literal(messageText.substring(lastEnd, pos.start)));
            }

            String mentionText = messageText.substring(pos.start, pos.end);
            result.append(createHighlightedMention(mentionText));

            lastEnd = pos.end;
        }

        if (lastEnd < messageText.length()) {
            result.append(Component.literal(messageText.substring(lastEnd)));
        }

        return result;
    }

    private static Pattern createSearchPattern(String text) {
        String pattern;
        if (FabricMechanicsConfig.mentionWordBoundary) {
            if (text.startsWith("@") && text.length() > 1) {
                String namePart = text.substring(1);
                pattern = "(?<!\\w)@" + Pattern.quote(namePart) + "(?!\\w)";
            } else {
                pattern = "(?<!\\w)" + Pattern.quote(text) + "(?!\\w)";
            }
        } else {
            pattern = Pattern.quote(text);
        }

        int flags = 0;
        if (FabricMechanicsConfig.mentionCaseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        return Pattern.compile(pattern, flags);
    }

    private static Component createHighlightedMention(String text) {
        TextColor color;
        try {
            color = TextColor.fromRgb(Integer.parseInt(FabricMechanicsConfig.mentionHighlightColorHex, 16));
        } catch (NumberFormatException e) {
            color = TextColor.fromRgb(0xFF3B30);
        }

        Style style = Style.EMPTY.withColor(color);

        if (FabricMechanicsConfig.mentionHoverText != null && !FabricMechanicsConfig.mentionHoverText.isEmpty()) {
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(FabricMechanicsConfig.mentionHoverText)));
        }

        return Component.literal(text).withStyle(style);
    }

    private static void playMentionSound() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            SoundEvent soundEvent = getSoundEvent(FabricMechanicsConfig.mentionSoundEvent);
            if (soundEvent == null) return;

            float volume = (float) FabricMechanicsConfig.mentionSoundVolume;
            float pitch = (float) FabricMechanicsConfig.mentionSoundPitch;

            mc.player.playSound(soundEvent, volume, pitch);
        } catch (Exception ignored) {
        }
    }

    private static SoundEvent getSoundEvent(String soundEventStr) {
        try {
            ResourceLocation location = ResourceLocation.parse(soundEventStr);
            return SoundEvent.createVariableRangeEvent(location);
        } catch (Exception e) {
            return null;
        }
    }

    private static record MentionPos(int start, int end) {}
}