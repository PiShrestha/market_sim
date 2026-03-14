package com.marketsim.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for order book state.
 */
@Data
@Builder
public class OrderBookResponse {

    private String symbol;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private BigDecimal midPrice;
    private BigDecimal spread;
    private int totalBidQuantity;
    private int totalAskQuantity;
    private List<PriceLevelDto> bids;
    private List<PriceLevelDto> asks;

    /**
     * Represents a single price level in the response.
     */
    @Data
    @Builder
    public static class PriceLevelDto {
        private BigDecimal price;
        private int quantity;
        private int orderCount;
    }
}
