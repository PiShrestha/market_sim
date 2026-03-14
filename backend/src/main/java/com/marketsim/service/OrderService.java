package com.marketsim.service;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.order.OrderType;
import com.marketsim.domain.orderbook.OrderBook;
import com.marketsim.domain.orderbook.OrderBookSnapshot;
import com.marketsim.domain.trade.Trade;
import com.marketsim.engine.MatchingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing orders and the order book.
 * This is the primary interface between the API layer and the trading engine.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String DEFAULT_SYMBOL = "SIMU";
    private static final int DEFAULT_DEPTH = 10;

    private final MatchingEngine matchingEngine;
    private final MarketDataService marketDataService;

    // Order books by symbol
    private final ConcurrentHashMap<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    // Trade history
    private final List<Trade> tradeHistory = new ArrayList<>();

    /**
     * Get or create an order book for a symbol.
     */
    public OrderBook getOrCreateOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, OrderBook::new);
    }

    /**
     * Submit a new order to the market.
     * 
     * @param order The order to submit
     * @return List of trades generated (empty if order went to book)
     */
    public List<Trade> submitOrder(Order order) {
        if (order.getSymbol() == null) {
            order.setSymbol(DEFAULT_SYMBOL);
        }

        // Validate limit orders have a price
        if (order.getType() == OrderType.LIMIT && order.getPrice() == null) {
            throw new IllegalArgumentException("Limit orders must have a price");
        }

        OrderBook orderBook = getOrCreateOrderBook(order.getSymbol());

        log.info("Processing {} {} order for {} shares at {}",
                order.getSide(), order.getType(), order.getQuantity(), order.getPrice());

        // Process through matching engine
        List<Trade> trades = matchingEngine.processOrder(order, orderBook);

        // Record trades
        if (!trades.isEmpty()) {
            tradeHistory.addAll(trades);
            marketDataService.recordTrades(order.getSymbol(), trades);
        }

        // Notify WebSocket clients
        marketDataService.publishOrderBookUpdate(orderBook.getSnapshot(DEFAULT_DEPTH));

        for (Trade trade : trades) {
            marketDataService.publishTrade(trade);
        }

        return trades;
    }

    /**
     * Cancel an existing order.
     * 
     * @param symbol  The symbol
     * @param orderId The order ID to cancel
     * @return The cancelled order, or null if not found
     */
    public Order cancelOrder(String symbol, UUID orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return null;
        }

        Order cancelled = matchingEngine.cancelOrder(orderId, orderBook);

        if (cancelled != null) {
            marketDataService.publishOrderBookUpdate(orderBook.getSnapshot(DEFAULT_DEPTH));
        }

        return cancelled;
    }

    /**
     * Get the current order book snapshot.
     */
    public OrderBookSnapshot getOrderBook(String symbol, int depth) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return null;
        }
        return orderBook.getSnapshot(depth);
    }

    /**
     * Get recent trades for a symbol.
     */
    public List<Trade> getRecentTrades(String symbol, int limit) {
        return tradeHistory.stream()
                .filter(t -> t.getSymbol().equals(symbol))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    /**
     * Get a specific order by ID.
     */
    public Order getOrder(String symbol, UUID orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return null;
        }
        return orderBook.getOrder(orderId);
    }
}
