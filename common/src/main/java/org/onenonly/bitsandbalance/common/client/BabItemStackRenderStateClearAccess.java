package org.onenonly.bitsandbalance.common.client;

/**
 * Marker/accessor mixed into {@code ItemStackRenderState} so other mixins can coordinate
 * one-time identity element injection per {@code clear()} cycle.
 */
public interface BabItemStackRenderStateClearAccess {
    boolean bitsandbalance$needsIdentityInjection();

    void bitsandbalance$setNeedsIdentityInjection(boolean value);
}
