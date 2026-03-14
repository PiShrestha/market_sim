package com.marketsim.domain.order;

/**
 * Lifecycle status of an order.
 * 
 * Analogy: Like tracking a package delivery
 * - NEW: Package just created
 * - PARTIALLY_FILLED: Some items delivered, more coming
 * - FILLED: All items delivered
 * - CANCELLED: Delivery cancelled
 */
public enum OrderStatus {
    NEW, // Order just submitted, not yet matched
    PARTIALLY_FILLED, // Some quantity has been executed
    FILLED, // Entire quantity has been executed
    CANCELLED // Order was cancelled before full execution
}
