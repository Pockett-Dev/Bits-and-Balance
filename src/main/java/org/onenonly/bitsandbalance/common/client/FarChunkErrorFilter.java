package org.onenonly.bitsandbalance.common.client;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ReusableMessageFactory;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Client log suppression controlled solely by a list of literal message fragments.
 */
public class FarChunkErrorFilter extends AbstractFilter {
    private static final String ERROR_PATTERN = "Detected setBlock in a far chunk";
    private static final String RECIPE_PATTERN = "Tried to load unrecognized recipe:";
    private static volatile List<String> blockedMessages = defaultBlockedMessages();
    private static final FarChunkErrorFilter INSTANCE = new FarChunkErrorFilter();
    private static Configuration installedConfiguration;

    public static List<String> defaultBlockedMessages() {
        return List.of(ERROR_PATTERN, RECIPE_PATTERN);
    }

    /** Seed the former hardcoded filters once, while retaining custom entries. */
    public static List<String> migrateBlockedMessages(Collection<? extends String> messages, int version,
                                                       boolean legacyFarChunkEnabled, boolean legacyRecipeEnabled) {
        List<String> migrated = new ArrayList<>();
        if (version < 1) migrated.addAll(defaultBlockedMessages());
        if (messages != null) migrated.addAll(messages);
        if (version < 2) {
            if (!legacyFarChunkEnabled) migrated.removeIf(ERROR_PATTERN::equals);
            if (!legacyRecipeEnabled) migrated.removeIf(RECIPE_PATTERN::equals);
        }
        return migrated.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim).distinct().toList();
    }

    public static boolean suppressesRemovedRecipeWarning(String message, Object recipeKey) {
        return "Tried to load unrecognized recipe: {} removed now.".equals(message)
                && match(new ParameterizedMessage(message, recipeKey).getFormattedMessage()) == Result.DENY;
    }

    public static void setBlockedMessages(Collection<? extends String> messages) {
        blockedMessages = messages.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim).distinct().toList();
    }

    private static boolean mightFilter() {
        return !blockedMessages.isEmpty();
    }

    private static Result match(String message) {
        if (message == null) return Result.NEUTRAL;
        for (String blocked : blockedMessages) {
            if (message.contains(blocked)) return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    /**
     * Install once per Log4j configuration, including child loggers and their own appenders.
     */
    public static synchronized void install() {
        Configuration configuration = ((LoggerContext) LogManager.getContext(false)).getConfiguration();
        if (installedConfiguration == configuration) return;
        if (installedConfiguration != null) installedConfiguration.removeFilter(INSTANCE);
        INSTANCE.start();
        configuration.addFilter(INSTANCE);
        installedConfiguration = configuration;
    }

    @Override
    public Result filter(LogEvent event) {
        if (event == null || !mightFilter()) return Result.NEUTRAL;
        Message msg = event.getMessage();
        return match(msg == null ? null : msg.getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        if (!mightFilter() || msg == null) return Result.NEUTRAL;
        if (params == null || params.length == 0) return match(msg);
        Message formatted = logger == null ? new ParameterizedMessage(msg, params)
                : logger.getMessageFactory().newMessage(msg, params);
        try {
            return match(formatted.getFormattedMessage());
        } finally {
            ReusableMessageFactory.release(formatted);
        }
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        if (!mightFilter()) return Result.NEUTRAL;
        return match(msg == null ? null : msg.toString());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (!mightFilter()) return Result.NEUTRAL;
        return match(msg == null ? null : msg.getFormattedMessage());
    }
}
