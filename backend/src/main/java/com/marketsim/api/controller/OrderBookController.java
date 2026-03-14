package com.marketsim.api.controller;

import com.marketsim.api.dto.OrderBookResponse;
import com.marketsim.api.dto.TradeResponse;
import com.marketsim.domain.orderbook.OrderBookSnapshot;
import com.marketsim.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for order book and market data.
 * 
 * Endpoints:
 * - GET /api/orderbook/{symbol} - Get current order book state
 * - GET /api/trades/{symbol} - Get recent trades
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
public class OrderBookController {

    private final OrderService orderService;

    /**
     * Get the current order book for a symbol.
     * 
     * @param symbol The trading symbol
     * @param depth  Number of price levels to include (default: 10)
     */
    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<OrderBookResponse> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int depth) {

        OrderBookSnapshot snapshot = orderService.getOrderBook(symbol, depth);
        if (snapshot == null) {
            // Return empty order book if none exists
            return ResponseEntity.ok(OrderBookResponse.builder()
                    .symbol(symbol)
                    .bids(List.of())
                    .asks(List.of())
                    .build());
        }

        return ResponseEntity.ok(mapToResponse(snapshot));
    }

    /**
     * Get recent trades for a symbol.
     * 
     * @param symbol The trading symbol
     * @param limit  Maximum number of trades to return (default: 50)
     */
    @GetMapping("/trades/{symbol}")
    public ResponseEntity<List<TradeResponse>> getRecentTrades(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "50") int limit) {

        return ResponseEntity.ok(
                orderService.getRecentTrades(symbol, limit).stream()
                        .map(TradeResponse::fromTrade)
                        .toList());
    }

    private OrderBookResponse mapToResponse(OrderBookSnapshot snapshot) {
        List<OrderBookResponse.PriceLevelDto> bidDtos = snapshot.getBids().stream()
                .map(level -> OrderBookResponse.PriceLevelDto.builder()
                        .price(level.getPrice())
                        .quantity(level.getQuantity())
                        .orderCount(level.getOrderCount())
                        .build())
                .toList();

        List<OrderBookResponse.PriceLevelDto> askDtos = snapshot.getAsks().stream()
                .map(level -> OrderBookResponse.PriceLevelDto.builder()
                        .price(level.getPrice())
                        .quantity(level.getQuantity())
                        .orderCount(level.getOrderCount())
                        .build())
                .toList();

        return OrderBookResponse.builder()
                .symbol(snapshot.getSymbol())
                .bestBid(snapshot.getBestBid())
                .bestAsk(snapshot.getBestAsk())
                .midPrice(snapshot.getMidPrice())
                .spread(snapshot.getSpread())
                .bids(bidDtos)
                .asks(askDtos)
                .build();
    }
}
