package org.onenonly.bitsandbalance.common.client;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

/**
 * Log4j filter to suppress "Detected setBlock in a far chunk" error spam.
 * <p>
 * This error is often triggered by worldgen features (especially from mods) trying to place blocks
 * slightly outside chunk boundaries during generation. It's harmless noise in most cases.
 */
public class FarChunkErrorFilter extends AbstractFilter {
    private static final String ERROR_PATTERN = "Detected setBlock in a far chunk";
    private static volatile boolean enabled = true;

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Install this filter globally on the root logger.
     */
    public static void install() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addFilter(new FarChunkErrorFilter());
    }

    @Override
    public Result filter(LogEvent event) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        if (event.getLevel() == Level.ERROR) {
            Message msg = event.getMessage();
            if (msg != null) {
                String formattedMessage = msg.getFormattedMessage();
                if (formattedMessage != null && formattedMessage.contains(ERROR_PATTERN)) {
                    return Result.DENY;
                }
            }
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        if (level == Level.ERROR && msg != null && msg.contains(ERROR_PATTERN)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        if (level == Level.ERROR && msg != null && msg.toString().contains(ERROR_PATTERN)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        if (level == Level.ERROR && msg != null) {
            String formattedMessage = msg.getFormattedMessage();
            if (formattedMessage != null && formattedMessage.contains(ERROR_PATTERN)) {
                return Result.DENY;
            }
        }
        return Result.NEUTRAL;
    }
}
