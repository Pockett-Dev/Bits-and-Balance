package org.onenonly.bitsandbalance.fabric.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;
import org.onenonly.bitsandbalance.fabric.client.FabricChatMentions;
import org.onenonly.bitsandbalance.fabric.client.ItemShareClient;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mixin to add chat heads to messages in the ChatListener.
 */
@Mixin(ChatListener.class)
public class ChatListenerMixin {
    private static final ThreadLocal<PlayerInfo> CURRENT_SENDER = new ThreadLocal<>();

    private static final Pattern PLAYER_CHAT_PATTERN = Pattern.compile("^<([^>]+)>");
    private static final Pattern JOIN_LEAVE_PATTERN = Pattern.compile("^([\\w]+) (joined|left) the game");
    private static final Pattern DEATH_PATTERN = Pattern.compile("^([\\w]+) (was|died|fell|drowned|burned|suffocated|blew|hit|shot|killed|slain|pummeled|withered|starved|squashed|experienced|went|walked|tried|discovered|got|froze|didn't|swam|doomed)");
    private static final Pattern ADVANCEMENT_PATTERN = Pattern.compile("^([\\w]+) has (made|completed|reached)");

    /**
     * Capture sender info for the upcoming showMessageToPlayer() call.
     * We use require=0 so this won't hard-fail if mappings/signatures change.
     */
    @Inject(method = "handlePlayerChatMessage", at = @At("HEAD"), require = 0)
    private void bitsandbalance$captureSender(PlayerChatMessage playerChatMessage, GameProfile gameProfile, ChatType.Bound bound, CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            ClientPacketListener connection = mc.getConnection();
            if (connection == null) return;

            // Prefer UUID-based lookup for robustness.
            var senderId = playerChatMessage.sender();
            CURRENT_SENDER.set(connection.getPlayerInfo(senderId));
        } catch (Throwable t) {
            // Keep silent; we'll fall back to heuristic placement.
        }
    }

    @Inject(method = "showMessageToPlayer", at = @At("RETURN"), require = 0)
    private void bitsandbalance$clearSender(CallbackInfoReturnable<?> cir) {
        CURRENT_SENDER.remove();
    }

    /**
    	 * Player chat path varies by loader/runtime patch set in 26.1.
    	 * Support both the legacy signed addMessage overload and the current plain Component overload.
     */
    @ModifyVariable(method = {"showMessageToPlayer", "method_44943"}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private Component bitsandbalance$addChatHeadToPlayerMessage(Component message) {
        return maybeAddChatHead(message, true);
    }

    @ModifyVariable(method = {"handleSystemMessage", "method_45745"}, at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private Component bitsandbalance$addChatHeadToSystemMessage(Component message) {
        // System messages include join/leave, advancements, etc. Mentions should only trigger
        // when a user sends a chat message.
        return maybeAddChatHead(message, false);
    }

    private static Component maybeAddChatHead(Component message, boolean processMentions) {
        if (message == null) return message;
        
        Component mentionProcessed = message;
        if (processMentions) {
            // Only process mentions for actual player chat messages.
            Component processed = FabricChatMentions.processChatMessage(message, CURRENT_SENDER.get());
            if (processed != null) mentionProcessed = processed;
        }
        
        // Then, add chat heads if enabled
        if (!FabricClientConfig.enableChatHeads) return mentionProcessed;
        if (containsPlayerSprite(mentionProcessed)) return mentionProcessed;

        PlayerInfo senderInfo = CURRENT_SENDER.get();
        Component processed = processMessage(mentionProcessed, senderInfo);
        ItemShareClient.rememberSharedItemMessage(processed);
        return processed;
    }

    private static Component processMessage(Component message, PlayerInfo senderInfo) {
        String messageText = message.getString();

        PlayerInfo playerInfo = senderInfo;
        Set<String> preferredNames = new HashSet<>();
        if (playerInfo != null) {
            preferredNames.add(cleanName(playerInfo.getProfile().name()));
            if (playerInfo.getTabListDisplayName() != null)
                preferredNames.add(cleanName(playerInfo.getTabListDisplayName().getString()));
        }

        String detectedName = null;
        if (preferredNames.isEmpty()) {
            detectedName = detectPlayerName(messageText);
            if (detectedName != null) {
                PlayerInfo pi = getPlayerInfo(detectedName);
                if (pi != null) {
                    playerInfo = pi;
                    preferredNames.add(cleanName(pi.getProfile().name()));
                    if (pi.getTabListDisplayName() != null)
                        preferredNames.add(cleanName(pi.getTabListDisplayName().getString()));
                }
            }
        }

        if (playerInfo == null || preferredNames.isEmpty()) {
            return message;
        }

        MutableComponent chatHead = Component.object(
                new PlayerSprite(
                        ResolvableProfile.createResolved(playerInfo.getProfile()),
                        playerInfo.showHat()
                )
        ).withStyle(ChatFormatting.WHITE);

        // Common chat decoration is "<name> message".
        // Insert head before the leading '<' so it renders outside the brackets.
        if (messageText.startsWith("<")) {
            return Component.empty().append(chatHead).append(Component.literal(" ")).append(message);
        }

        Component inserted = insertHeadBeforeNameComponent(message, chatHead, preferredNames);
        if (inserted != message) return inserted;

        // Fallback: if we couldn't find a dedicated name component, just prefix.
        return Component.empty().append(chatHead).append(Component.literal(" ")).append(message);
    }

    private static String cleanName(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").trim();
    }

    private static Component insertHeadBeforeNameComponent(Component root, MutableComponent head, Set<String> names) {
        return replaceFirstInTree(root,
                c -> isLikelyNameComponent(c, names),
                c -> Component.empty().append(head.copy()).append(Component.literal(" ")).append(c));
    }

    private static boolean isLikelyNameComponent(Component c, Set<String> names) {
        if (c == null) return false;
        String text = cleanName(c.getString());
        if (text.isEmpty()) return false;

        boolean matchesText = names.contains(text);
        ClickEvent ce = c.getStyle().getClickEvent();
        if (ce instanceof ClickEvent.SuggestCommand suggest) {
            String cmd = suggest.command();
            if (cmd != null) {
                for (String n : names) {
                    if (!n.isBlank() && cmd.contains(n)) return true;
                }
            }
        }
        if (ce instanceof ClickEvent.RunCommand run) {
            String cmd = run.command();
            if (cmd != null) {
                for (String n : names) {
                    if (!n.isBlank() && cmd.contains(n)) return true;
                }
            }
        }

        // If there is a click event, prefer exact matches.
        if (ce != null) return matchesText;

        // Otherwise accept exact matches (some modded chat removes click events).
        return matchesText;
    }

    private static Component replaceFirstInTree(
            Component root,
            java.util.function.Predicate<Component> match,
            java.util.function.Function<Component, Component> replace
    ) {
        if (root == null) return null;
        if (match.test(root)) return replace.apply(root);
        if (root.getSiblings().isEmpty()) return root;

        boolean changed = false;
        MutableComponent copy = root.copy();
        List<Component> newSiblings = copy.getSiblings();
        newSiblings.clear();

        for (Component sibling : root.getSiblings()) {
            if (!changed) {
                Component replaced = replaceFirstInTree(sibling, match, replace);
                changed = replaced != sibling;
                newSiblings.add(replaced);
            } else {
                newSiblings.add(sibling);
            }
        }

        return changed ? copy : root;
    }

    private static boolean containsPlayerSprite(Component component) {
        if (component == null) return false;

        var contents = component.getContents();
        if (contents instanceof ObjectContents objectContents && objectContents.contents() instanceof PlayerSprite) {
            return true;
        }

        for (Component sibling : component.getSiblings()) {
            if (containsPlayerSprite(sibling)) return true;
        }

        return false;
    }

    private static String detectPlayerName(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return null;
        }

        String cleanText = messageText.replaceAll("§.", "");

        // Fast-path the common vanilla formats.
        Matcher chatMatcher = PLAYER_CHAT_PATTERN.matcher(cleanText);
        if (chatMatcher.find()) return chatMatcher.group(1);
        Matcher joinLeaveMatcher = JOIN_LEAVE_PATTERN.matcher(cleanText);
        if (joinLeaveMatcher.find()) return joinLeaveMatcher.group(1);
        Matcher deathMatcher = DEATH_PATTERN.matcher(cleanText);
        if (deathMatcher.find()) return deathMatcher.group(1);
        Matcher advancementMatcher = ADVANCEMENT_PATTERN.matcher(cleanText);
        if (advancementMatcher.find()) return advancementMatcher.group(1);

        // Generic fallback: scan for any online player's name inside the rendered message.
        // This works for modded chat formats as long as the name appears somewhere.
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return null;

        List<String> candidates = collectCandidateNames(connection);
        return scanForName(cleanText, candidates);
    }

    private static List<String> collectCandidateNames(ClientPacketListener connection) {
        Set<String> names = new HashSet<>();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (info == null) continue;
            String profileName = info.getProfile().name();
            if (profileName != null && !profileName.isBlank()) names.add(profileName.replaceAll("§.", ""));
            if (info.getTabListDisplayName() != null) {
                String display = info.getTabListDisplayName().getString().replaceAll("§.", "");
                if (!display.isBlank()) names.add(display);
            }
        }
        ArrayList<String> list = new ArrayList<>(names);
        list.sort(Comparator.comparingInt(String::length).reversed());
        return list;
    }

    private static String scanForName(String message, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        for (String name : candidates) {
            if (name == null || name.isBlank()) continue;
            int from = 0;
            while (true) {
                int idx = message.indexOf(name, from);
                if (idx < 0) break;
                int end = idx + name.length();
                if (isWordBoundary(message, idx - 1) && isWordBoundary(message, end)) {
                    return name;
                }
                from = idx + 1;
            }
        }
        return null;
    }

    private static boolean isWordBoundary(String s, int index) {
        if (index < 0 || index >= s.length()) return true;
        int cp = s.codePointAt(index);
        return !isWordCharacter(cp);
    }

    private static boolean isWordCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_';
    }

    private static PlayerInfo getPlayerInfo(String playerName) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            return null;
        }

        return connection.getPlayerInfo(playerName);
    }
}
