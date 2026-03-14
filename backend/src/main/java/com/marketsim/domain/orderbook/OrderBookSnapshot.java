package com.marketsim.domain.orderbook;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * A snapshot of the order book at a point in time.
 * This is what gets sent to the frontend for visualization.
 */
@Data
@AllArgsConstructor
public class OrderBookSnapshot {

    private String symbol;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private BigDecimal midPrice;
    private BigDecimal spread;
    private List<PriceLevelSnapshot> bids;
    private List<PriceLevelSnapshot> asks;

    /**
     * Snapshot of a single price level.
     */
    @Data
    @AllArgsConstructor
    public static class PriceLevelSnapshot {
        private BigDecimal price;
        private int quantity;
        private int orderCount;
    }
}
