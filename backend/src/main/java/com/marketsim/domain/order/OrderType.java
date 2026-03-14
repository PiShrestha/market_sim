package com.marketsim.domain.order;

/**
 * Types of orders that can be submitted to the market.
 * 
 * Analogy:
 * - LIMIT order: "I'll only pay $10 for this item, not a penny more"
 * - MARKET order: "I need this item NOW, whatever the price"
 */
public enum OrderType {
    LIMIT, // Execute only at specified price or better
    MARKET // Execute immediately at best available price
}
