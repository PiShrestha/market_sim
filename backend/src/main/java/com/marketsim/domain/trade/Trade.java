package com.marketsim.domain.trade;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an executed trade between two orders.
 * 
 * A trade is created when a buy order and sell order match at a price.
 * It's the permanent record of a transaction.
 * 
 * Analogy: A trade is like a receipt - it proves a transaction happened,
 * recording who bought, who sold, at what price, and how much.
 */
@Data
@Builder
public class Trade {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    /** The symbol that was traded */
    private String symbol;

    /** The buy order that was filled */
    private UUID buyOrderId;

    /** The sell order that was filled */
    private UUID sellOrderId;

    /** The buyer's trader ID */
    private String buyerId;

    /** The seller's trader ID */
    private String sellerId;

    /** The execution price */
    private BigDecimal price;

    /** The quantity traded */
    private int quantity;

    /**
     * Which side initiated the trade (the "aggressor").
     * If a new buy order hit a resting sell, the buy side is the aggressor.
     * This matters for analytics - aggressive orders indicate urgency.
     */
    private AggressorSide aggressor;

    /** When the trade occurred */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Get the total value of this trade.
     */
    public BigDecimal getValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Indicates which side initiated the trade.
     */
    public enum AggressorSide {
        BUY, // Buyer crossed the spread to hit the ask
        SELL // Seller crossed the spread to hit the bid
    }
}
