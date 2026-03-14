package com.marketsim.api.dto;

import com.marketsim.domain.order.OrderSide;
import com.marketsim.domain.order.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for submitting a new order.
 */
@Data
public class OrderRequest {

    @NotNull(message = "Trader ID is required")
    private String traderId;

    @NotNull(message = "Side is required (BUY or SELL)")
    private OrderSide side;

    @NotNull(message = "Order type is required (LIMIT or MARKET)")
    private OrderType type;

    /** Price is required for LIMIT orders, ignored for MARKET orders */
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
