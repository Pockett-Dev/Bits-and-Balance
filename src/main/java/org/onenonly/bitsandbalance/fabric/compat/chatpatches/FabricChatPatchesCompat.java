package org.onenonly.bitsandbalance.fabric.compat.chatpatches;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
//? if mc != 1.21.1
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.TranslatableContents;
//? if mc == 1.21.11
import net.minecraft.network.chat.contents.objects.ObjectInfo;
//? endif
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
//? else
import org.onenonly.bitsandbalance.fabric.client.FabricChatHeadsRenderer;
//? endif

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Optional bridge used while a player chat message is being rewritten by Chat Patches.
 */
public final class FabricChatPatchesCompat {
    private static final ThreadLocal<PlayerInfo> CURRENT_SENDER = new ThreadLocal<>();
    //? if mc != 1.21.1
    private static final int CHAT_HEAD_SHADOW_COLOR = 0x8C555555;
    private static final int CHAT_PATCHES_MESSAGE_INDEX = 1;
    private static final int CHAT_PATCHES_SENDER_INDEX = 1;
    //? endif

    private FabricChatPatchesCompat() {
    }

    public static void captureSender(PlayerInfo sender) {
        if (sender == null) {
            CURRENT_SENDER.remove();
        } else {
            CURRENT_SENDER.set(sender);
        }
    }

    public static void clearSender() {
        CURRENT_SENDER.remove();
    }

    public static Component restoreChatHead(Component message) {
        if (message == null) {
            return null;
        }

        PlayerInfo sender = CURRENT_SENDER.get();
        //? if mc == 1.21.1
        FabricChatHeadsRenderer.rememberChatHeadOwner(message, sender, false);
        return message;
        //? else
        if (!FabricClientConfig.enableChatHeads || sender == null || containsPlayerSprite(message)) {
            return message;
        }

        Set<String> names = new HashSet<>();
        names.add(cleanName(sender.getProfile().name()));
        if (sender.getTabListDisplayName() != null) {
            names.add(cleanName(sender.getTabListDisplayName().getString()));
        }
        names.removeIf(String::isBlank);
        if (names.isEmpty()) {
            return message;
        }

        MutableComponent head = Component.object(
                new PlayerSprite(
                        ResolvableProfile.createResolved(sender.getProfile()),
                        sender.showHat()
                )
        ).withStyle(style -> {
            style = style.withColor(ChatFormatting.WHITE);
            return FabricClientConfig.chatHeadsShadow
                    ? style.withShadowColor(CHAT_HEAD_SHADOW_COLOR)
                    : style.withoutShadow();
        });

        Component inserted = insertAtChatPatchesSenderSlot(message, names, head);
        if (inserted != message) {
            return inserted;
        }

        inserted = replaceFirstInTree(
                message,
                names,
                head
        );
        if (inserted != message) {
            return inserted;
        }

        // Never prefix the whole rewritten message: Chat Patches keeps its
        // timestamp at the outer left edge, while the head belongs with the sender.
        return message;
        //? endif
    }

    //? if mc != 1.21.1
    private static Component insertAtChatPatchesSenderSlot(
            Component root,
            Set<String> names,
            MutableComponent head
    ) {
        if (root.getSiblings().size() <= CHAT_PATCHES_MESSAGE_INDEX) {
            return root;
        }

        Component message = root.getSiblings().get(CHAT_PATCHES_MESSAGE_INDEX);
        if (message.getSiblings().size() <= CHAT_PATCHES_SENDER_INDEX) {
            return root;
        }

        Component sender = message.getSiblings().get(CHAT_PATCHES_SENDER_INDEX);
        if (!containsKnownPlayerName(sender, names)) {
            return root;
        }

        MutableComponent messageCopy = message.copy();
        messageCopy.getSiblings().set(
                CHAT_PATCHES_SENDER_INDEX,
                withHead(sender, head)
        );

        MutableComponent rootCopy = root.copy();
        rootCopy.getSiblings().set(CHAT_PATCHES_MESSAGE_INDEX, messageCopy);
        return rootCopy;
    }

    private static Component replaceFirstInTree(Component root, Set<String> names, MutableComponent head) {
        if (isLikelyPlayerNameComponent(root, names)) {
            return withHead(root, head);
        }
        if (root.getSiblings().isEmpty()) {
            return root;
        }

        boolean changed = false;
        MutableComponent copy = root.copy();
        List<Component> siblings = copy.getSiblings();
        siblings.clear();

        for (Component sibling : root.getSiblings()) {
            if (!changed) {
                Component replaced = replaceFirstInTree(sibling, names, head);
                changed = replaced != sibling;
                siblings.add(replaced);
            } else {
                siblings.add(sibling);
            }
        }

        return changed ? copy : root;
    }

    private static Component withHead(Component sender, MutableComponent head) {
        return Component.empty()
                .append(head.copy())
                .append(Component.literal(" "))
                .append(sender);
    }

    private static boolean containsKnownPlayerName(Component component, Set<String> names) {
        String text = cleanName(component.getString());
        for (String name : names) {
            int offset = text.indexOf(name);
            while (offset >= 0) {
                int end = offset + name.length();
                boolean startsAtBoundary = offset == 0 || !isPlayerNameCharacter(text.charAt(offset - 1));
                boolean endsAtBoundary = end == text.length() || !isPlayerNameCharacter(text.charAt(end));
                if (startsAtBoundary && endsAtBoundary) {
                    return true;
                }
                offset = text.indexOf(name, offset + 1);
            }
        }
        return false;
    }

    private static boolean isPlayerNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static boolean isLikelyPlayerNameComponent(Component component, Set<String> names) {
        if (component == null) {
            return false;
        }

        String text = cleanName(component.getString());
        for (String name : names) {
            if (text.equals(name)
                    || text.startsWith("<" + name + ">")
                    || text.startsWith("[" + name + "]")
                    || text.startsWith(name + ":")
                    || text.startsWith(name + " ")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPlayerSprite(Component component) {
        if (component == null) {
            return false;
        }

        var contents = component.getContents();
        //? if mc == 1.21.11
        if (contents instanceof ObjectContents(ObjectInfo objectInfo) && objectInfo instanceof PlayerSprite) {
        //? else
        if (contents instanceof ObjectContents objectContents && objectContents.contents() instanceof PlayerSprite) {
        //? endif
            return true;
        }

        if (contents instanceof TranslatableContents translatable) {
            for (Object argument : translatable.getArgs()) {
                if (argument instanceof Component child && containsPlayerSprite(child)) {
                    return true;
                }
            }
        }

        for (Component sibling : component.getSiblings()) {
            if (containsPlayerSprite(sibling)) {
                return true;
            }
        }
        return false;
    }

    private static String cleanName(String text) {
        return text == null ? "" : text.replaceAll("\\u00A7.", "").trim();
    }
    //? endif
}
