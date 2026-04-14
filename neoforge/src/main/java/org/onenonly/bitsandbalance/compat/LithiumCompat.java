package org.onenonly.bitsandbalance.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Compatibility layer for Lithium mod's sleeping block entity system.
 * Allows our custom brewing to work alongside Lithium's performance optimizations.
 */
public class LithiumCompat {
    private static final Logger LOG = LogUtils.getLogger();
    
    private static boolean checkedForLithium = false;
    private static boolean lithiumPresent = false;
    
    // Reflected methods for Lithium's SleepingBlockEntity interface
    private static Method wakeUpNowMethod = null;
    private static Method startSleepingMethod = null;
    private static Method isSleepingMethod = null;
    
    /**
     * Checks if Lithium is loaded and caches the result.
     */
    public static boolean isLithiumPresent() {
        if (!checkedForLithium) {
            checkedForLithium = true;
            try {
                // Try to load Lithium's SleepingBlockEntity interface
                Class<?> sleepingBlockEntityClass = Class.forName("net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity");
                
                // Try to get the methods we need
                wakeUpNowMethod = sleepingBlockEntityClass.getDeclaredMethod("wakeUpNow");
                startSleepingMethod = sleepingBlockEntityClass.getDeclaredMethod("lithium$startSleeping");
                isSleepingMethod = sleepingBlockEntityClass.getDeclaredMethod("isSleeping");
                
                lithiumPresent = true;
                LOG.info("[BitsAndBalance] Lithium detected - enabling sleeping block entity compatibility");
            } catch (ClassNotFoundException e) {
                LOG.debug("[BitsAndBalance] Lithium not found - using standard block entity ticking");
                lithiumPresent = false;
            } catch (NoSuchMethodException e) {
                LOG.warn("[BitsAndBalance] Lithium found but API mismatch - disabling integration");
                lithiumPresent = false;
            } catch (Throwable t) {
                LOG.error("[BitsAndBalance] Error checking for Lithium", t);
                lithiumPresent = false;
            }
        }
        return lithiumPresent;
    }
    
    /**
     * Wakes up a block entity if it's sleeping (Lithium optimization).
     * Call this when starting custom brewing to ensure the block entity ticks.
     */
    public static void wakeUp(BlockEntity blockEntity) {
        if (!isLithiumPresent()) return;
        
        try {
            if (wakeUpNowMethod != null) {
                wakeUpNowMethod.invoke(blockEntity);
            }
        } catch (Throwable t) {
            LOG.error("[BitsAndBalance] Error waking up block entity", t);
        }
    }
    
    /**
     * Allows a block entity to enter sleep mode if appropriate (Lithium optimization).
     * Call this when custom brewing finishes and the block entity can idle.
     */
    public static void allowSleep(BlockEntity blockEntity) {
        if (!isLithiumPresent()) return;
        
        try {
            if (startSleepingMethod != null) {
                startSleepingMethod.invoke(blockEntity);
            }
        } catch (Throwable t) {
            LOG.error("[BitsAndBalance] Error putting block entity to sleep", t);
        }
    }
    
    /**
     * Checks if a block entity is currently sleeping (Lithium optimization).
     */
    public static boolean isSleeping(BlockEntity blockEntity) {
        if (!isLithiumPresent()) return false;
        
        try {
            if (isSleepingMethod != null) {
                Object result = isSleepingMethod.invoke(blockEntity);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Throwable t) {
            LOG.error("[BitsAndBalance] Error checking sleep state", t);
        }
        return false;
    }
    
    /**
     * Gets a debug string describing the Lithium compatibility state.
     */
    public static String getStatusString() {
        if (!checkedForLithium) {
            return "Not checked";
        }
        if (lithiumPresent) {
            return "Active (methods: " + 
                   (wakeUpNowMethod != null ? "wake" : "") + 
                   (startSleepingMethod != null ? ",sleep" : "") + 
                   (isSleepingMethod != null ? ",check" : "") + ")";
        }
        return "Not present";
    }
}

