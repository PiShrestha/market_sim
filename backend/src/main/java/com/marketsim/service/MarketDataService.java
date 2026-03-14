package com.marketsim.service;

import com.marketsim.domain.orderbook.OrderBookSnapshot;
import com.marketsim.domain.trade.Trade;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing and publishing market data.
 * Handles price history, statistics, and WebSocket broadcasts.
 */
@Slf4j
@Service
public class MarketDataService {

    private final SimpMessagingTemplate messagingTemplate;

    // Price history by symbol
    private final Map<String, List<BigDecimal>> priceHistory = new HashMap<>();

    // Trade volume by symbol
    private final Map<String, Integer> tradedVolume = new HashMap<>();

    @Getter
    private BigDecimal lastPrice;

    public MarketDataService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Record trades and update market data.
     */
    public void recordTrades(String symbol, List<Trade> trades) {
        if (trades.isEmpty()) {
            return;
        }

        List<BigDecimal> history = priceHistory.computeIfAbsent(symbol, k -> new ArrayList<>());

        for (Trade trade : trades) {
            history.add(trade.getPrice());
            tradedVolume.merge(symbol, trade.getQuantity(), Integer::sum);
            lastPrice = trade.getPrice();
        }

        // Keep only last 1000 prices
        if (history.size() > 1000) {
            history.subList(0, history.size() - 1000).clear();
        }
    }

    /**
     * Calculate volatility from recent prices.
     * Uses simple standard deviation of returns.
     */
    public double getVolatility(String symbol) {
        List<BigDecimal> history = priceHistory.get(symbol);
        if (history == null || history.size() < 2) {
            return 0.01; // Default volatility
        }

        // Calculate log returns
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < history.size() && i < 50; i++) {
            double ret = Math.log(history.get(i).doubleValue() / history.get(i - 1).doubleValue());
            returns.add(ret);
        }

        if (returns.isEmpty()) {
            return 0.01;
        }

        // Calculate standard deviation
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance);
    }

    /**
     * Calculate VWAP (Volume Weighted Average Price).
     */
    public BigDecimal getVWAP(String symbol) {
        List<BigDecimal> history = priceHistory.get(symbol);
        if (history == null || history.isEmpty()) {
            return null;
        }

        // Simple average for now (would need volume per price for true VWAP)
        return history.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get recent price change percentage.
     */
    public double getPriceChange(String symbol, int periods) {
        List<BigDecimal> history = priceHistory.get(symbol);
        if (history == null || history.size() < periods + 1) {
            return 0.0;
        }

        BigDecimal current = history.get(history.size() - 1);
        BigDecimal previous = history.get(history.size() - 1 - periods);

        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Get recent volume.
     */
    public int getRecentVolume(String symbol) {
        return tradedVolume.getOrDefault(symbol, 0);
    }

    /**
     * Publish order book update via WebSocket.
     */
    public void publishOrderBookUpdate(OrderBookSnapshot snapshot) {
        if (snapshot != null) {
            messagingTemplate.convertAndSend("/topic/orderbook/" + snapshot.getSymbol(), snapshot);
        }
    }

    /**
     * Publish trade via WebSocket.
     */
    public void publishTrade(Trade trade) {
        messagingTemplate.convertAndSend("/topic/trades/" + trade.getSymbol(), trade);
    }

    /**
     * Publish market metrics via WebSocket.
     */
    public void publishMetrics(String symbol, Map<String, Object> metrics) {
        messagingTemplate.convertAndSend("/topic/metrics/" + symbol, metrics);
    }
}
