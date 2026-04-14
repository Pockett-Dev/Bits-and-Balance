package org.onenonly.bitsandbalance.mechanics;

/**
 * Base interface for all isolated feature modules in Bits and Balance.
 * 
 * Each feature module should:
 * - Be self-contained with all its logic in one class
 * - Check its own config flags before executing
 * - Only subscribe to events it actually needs
 * - Be independently testable and maintainable
 * 
 * This interface provides a standard contract for all feature modules.
 */
public interface FeatureModule {
    
    /**
     * Gets the name of this feature module for logging and debugging.
     * @return A human-readable name for this feature
     */
    String getFeatureName();
    
    /**
     * Checks if this feature module is currently enabled based on configuration.
     * This should be checked before any feature logic executes.
     * @return true if the feature is enabled, false otherwise
     */
    boolean isEnabled();
    
    /**
     * Initializes the feature module. Called during mod setup.
     * Use this for any one-time setup that doesn't depend on events.
     */
    default void initialize() {
        // Default implementation does nothing
        // Override if your feature needs initialization
    }
    
    /**
     * Called when the feature is being disabled or the mod is shutting down.
     * Use this to clean up any resources, timers, or cached data.
     */
    default void cleanup() {
        // Default implementation does nothing
        // Override if your feature needs cleanup
    }
    
    /**
     * Gets the priority of this feature module for initialization order.
     * Lower numbers initialize first.
     * @return Priority value (0 = highest priority, higher numbers = lower priority)
     */
    default int getInitializationPriority() {
        return 1000; // Default medium priority
    }
}
