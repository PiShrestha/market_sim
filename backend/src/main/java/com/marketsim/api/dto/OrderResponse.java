package com.marketsim.api.dto;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.order.OrderSide;
import com.marketsim.domain.order.OrderStatus;
import com.marketsim.domain.order.OrderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for order details.
 */
@Data
@Builder
public class OrderResponse {

    private UUID id;
    private String traderId;
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private int quantity;
    private int filledQuantity;
    private int remainingQuantity;
    private OrderStatus status;
    private Instant timestamp;

    public static OrderResponse fromOrder(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .traderId(order.getTraderId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .status(order.getStatus())
                .timestamp(order.getTimestamp())
                .build();
    }
}
