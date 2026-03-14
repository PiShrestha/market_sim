package com.marketsim.domain.order;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a trading order in the market.
 * 
 * An order is an instruction to buy or sell a specific quantity
 * of an asset at a specified price (limit) or at the best available price
 * (market).
 * 
 * Analogy: Think of an order like a shopping list item with conditions:
 * "Buy 10 apples, but only if they cost $1 or less each"
 */
@Data
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    @EqualsAndHashCode.Include
    @Builder.Default
    private UUID id = UUID.randomUUID();

    /** The trader who submitted this order */
    private String traderId;

    /** The asset symbol (e.g., "SIMU") */
    private String symbol;

    /** Buy or Sell */
    private OrderSide side;

    /** Limit or Market */
    private OrderType type;

    /** Price per share (null for market orders) */
    private BigDecimal price;

    /** Total quantity to trade */
    private int quantity;

    /** Quantity already filled */
    @Builder.Default
    private int filledQuantity = 0;

    /** Current order status */
    @Builder.Default
    private OrderStatus status = OrderStatus.NEW;

    /** When the order was created */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Get the remaining unfilled quantity.
     */
    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    /**
     * Check if order is fully filled.
     */
    public boolean isFilled() {
        return filledQuantity >= quantity;
    }

    /**
     * Fill a portion of the order.
     * 
     * @param fillQuantity Amount to fill
     * @return The actual amount filled (may be less if order is nearly complete)
     */
    public int fill(int fillQuantity) {
        int actualFill = Math.min(fillQuantity, getRemainingQuantity());
        filledQuantity += actualFill;

        if (isFilled()) {
            status = OrderStatus.FILLED;
        } else if (filledQuantity > 0) {
            status = OrderStatus.PARTIALLY_FILLED;
        }

        return actualFill;
    }

    /**
     * Cancel this order.
     */
    public void cancel() {
        if (status != OrderStatus.FILLED) {
            status = OrderStatus.CANCELLED;
        }
    }

    /**
     * Check if this order can match against another order at a given price.
     */
    public boolean canMatchAt(BigDecimal matchPrice) {
        if (type == OrderType.MARKET) {
            return true; // Market orders match at any price
        }

        if (side == OrderSide.BUY) {
            // Buy order matches if match price <= limit price
            return matchPrice.compareTo(price) <= 0;
        } else {
            // Sell order matches if match price >= limit price
            return matchPrice.compareTo(price) >= 0;
        }
    }
}
