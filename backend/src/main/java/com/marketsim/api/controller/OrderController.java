package com.marketsim.api.controller;

import com.marketsim.api.dto.OrderRequest;
import com.marketsim.api.dto.OrderResponse;
import com.marketsim.api.dto.TradeResponse;
import com.marketsim.domain.order.Order;
import com.marketsim.domain.trade.Trade;
import com.marketsim.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for order management.
 * 
 * Endpoints:
 * - POST /api/orders/{symbol} - Submit a new order
 * - GET /api/orders/{symbol}/{orderId} - Get order details
 * - DELETE /api/orders/{symbol}/{orderId} - Cancel an order
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
public class OrderController {

    private final OrderService orderService;

    /**
     * Submit a new order.
     * 
     * @param symbol  The trading symbol (e.g., "SIMU")
     * @param request The order details
     * @return The created order and any resulting trades
     */
    @PostMapping("/{symbol}")
    public ResponseEntity<OrderSubmitResponse> submitOrder(
            @PathVariable String symbol,
            @Valid @RequestBody OrderRequest request) {

        Order order = Order.builder()
                .traderId(request.getTraderId())
                .symbol(symbol)
                .side(request.getSide())
                .type(request.getType())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .build();

        List<Trade> trades = orderService.submitOrder(order);

        List<TradeResponse> tradeResponses = trades.stream()
                .map(TradeResponse::fromTrade)
                .toList();

        return ResponseEntity.ok(new OrderSubmitResponse(
                OrderResponse.fromOrder(order),
                tradeResponses));
    }

    /**
     * Get order details by ID.
     */
    @GetMapping("/{symbol}/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String symbol,
            @PathVariable UUID orderId) {

        Order order = orderService.getOrder(symbol, orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(OrderResponse.fromOrder(order));
    }

    /**
     * Cancel an order.
     */
    @DeleteMapping("/{symbol}/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String symbol,
            @PathVariable UUID orderId) {

        Order cancelled = orderService.cancelOrder(symbol, orderId);
        if (cancelled == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(OrderResponse.fromOrder(cancelled));
    }

    /**
     * Response for order submission including any resulting trades.
     */
    public record OrderSubmitResponse(OrderResponse order, List<TradeResponse> trades) {
    }
}
