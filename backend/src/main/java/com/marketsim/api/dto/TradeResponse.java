package com.marketsim.api.dto;

import com.marketsim.domain.trade.Trade;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for trade details.
 */
@Data
@Builder
public class TradeResponse {

    private UUID id;
    private String symbol;
    private UUID buyOrderId;
    private UUID sellOrderId;
    private String buyerId;
    private String sellerId;
    private BigDecimal price;
    private int quantity;
    private BigDecimal value;
    private String aggressor;
    private Instant timestamp;

    public static TradeResponse fromTrade(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .symbol(trade.getSymbol())
                .buyOrderId(trade.getBuyOrderId())
                .sellOrderId(trade.getSellOrderId())
                .buyerId(trade.getBuyerId())
                .sellerId(trade.getSellerId())
                .price(trade.getPrice())
                .quantity(trade.getQuantity())
                .value(trade.getValue())
                .aggressor(trade.getAggressor().name())
                .timestamp(trade.getTimestamp())
                .build();
    }
}
