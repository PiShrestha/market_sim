package com.marketsim.engine;

import com.marketsim.domain.agent.MarketMaker;
import com.marketsim.domain.agent.TraderAgent;
import com.marketsim.domain.order.Order;
import com.marketsim.domain.orderbook.OrderBook;
import com.marketsim.service.MarketDataService;
import com.marketsim.service.OrderService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Simulation Engine - Orchestrates the Entire Market.
 * 
 * This engine:
 * 1. Manages the simulation lifecycle (start, stop, pause)
 * 2. Runs the market maker and trader agents
 * 3. Generates market activity on a timer
 * 4. Tracks metrics and publishes updates
 * 
 * Analogy: Think of this as the "game master" running the market simulation.
 * It controls the clock, activates the AI traders, and keeps everything in
 * sync.
 */
@Slf4j
@Service
public class SimulationEngine {

    private static final String DEFAULT_SYMBOL = "SIMU";

    private final OrderService orderService;
    private final MarketDataService marketDataService;
    private final MatchingEngine matchingEngine;

    @Getter
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Getter
    private BigDecimal referencePrice = new BigDecimal("100.00");

    private MarketMaker marketMaker;
    private final List<TraderAgent> traderAgents = new ArrayList<>();

    private long tickCount = 0;

    public SimulationEngine(OrderService orderService, MarketDataService marketDataService,
            MatchingEngine matchingEngine) {
        this.orderService = orderService;
        this.marketDataService = marketDataService;
        this.matchingEngine = matchingEngine;

        initializeAgents();
    }

    /**
     * Initialize the default agents.
     */
    private void initializeAgents() {
        // Create a market maker
        marketMaker = new MarketMaker(
                new BigDecimal("100000"), // Initial capital
                0.1, // Risk aversion
                new BigDecimal("0.10"), // Target spread ($0.10)
                1000, // Max inventory
                10 // Quote size
        );

        log.info("Simulation engine initialized with market maker");
    }

    /**
     * Start the simulation.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("Simulation started");

            // Ensure order book exists
            orderService.getOrCreateOrderBook(DEFAULT_SYMBOL);

            // Seed the book with initial market maker quotes
            seedOrderBook();
        }
    }

    /**
     * Stop the simulation.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Simulation stopped");
        }
    }

    /**
     * Seed the order book with initial market maker quotes.
     */
    private void seedOrderBook() {
        TraderAgent.MarketData initialData = new TraderAgent.MarketData(
                referencePrice, referencePrice, 0.01, 0, 0.0);

        OrderBook orderBook = orderService.getOrCreateOrderBook(DEFAULT_SYMBOL);
        MarketMaker.QuoteResult quotes = marketMaker.calculateQuotes(orderBook, initialData);

        if (quotes != null) {
            // Submit initial bid
            Order bid = Order.builder()
                    .traderId(marketMaker.getId())
                    .symbol(DEFAULT_SYMBOL)
                    .side(com.marketsim.domain.order.OrderSide.BUY)
                    .type(com.marketsim.domain.order.OrderType.LIMIT)
                    .price(quotes.bid())
                    .quantity(quotes.size())
                    .build();
            orderService.submitOrder(bid);

            // Submit initial ask
            Order ask = Order.builder()
                    .traderId(marketMaker.getId())
                    .symbol(DEFAULT_SYMBOL)
                    .side(com.marketsim.domain.order.OrderSide.SELL)
                    .type(com.marketsim.domain.order.OrderType.LIMIT)
                    .price(quotes.ask())
                    .quantity(quotes.size())
                    .build();
            orderService.submitOrder(ask);

            log.info("Seeded order book with MM quotes: bid={}, ask={}", quotes.bid(), quotes.ask());
        }
    }

    /**
     * Main simulation tick - runs periodically when simulation is active.
     */
    @Scheduled(fixedRate = 500) // Every 500ms
    public void tick() {
        if (!running.get()) {
            return;
        }

        tickCount++;

        OrderBook orderBook = orderService.getOrCreateOrderBook(DEFAULT_SYMBOL);

        // Get current market data
        TraderAgent.MarketData marketData = buildMarketData();

        // Market maker updates quotes every few ticks
        if (tickCount % 2 == 0) {
            updateMarketMakerQuotes(orderBook, marketData);
        }

        // Publish metrics
        if (tickCount % 4 == 0) {
            publishMetrics();
        }
    }

    /**
     * Update market maker quotes.
     */
    private void updateMarketMakerQuotes(OrderBook orderBook, TraderAgent.MarketData marketData) {
        MarketMaker.QuoteResult quotes = marketMaker.calculateQuotes(orderBook, marketData);

        if (quotes == null) {
            return;
        }

        // Check if we need to update quotes (if price moved significantly)
        BigDecimal currentMid = orderBook.getMidPrice();
        if (currentMid != null && marketMaker.getLastBid() != null) {
            BigDecimal priceDiff = currentMid.subtract(
                    marketMaker.getLastBid().add(marketMaker.getLastAsk()).divide(BigDecimal.valueOf(2))).abs();

            // Only update if price moved more than 1 cent
            if (priceDiff.compareTo(new BigDecimal("0.01")) < 0) {
                return;
            }
        }

        // Submit new quotes
        Optional<Order> order = marketMaker.generateOrder(orderBook, marketData);
        order.ifPresent(orderService::submitOrder);
    }

    /**
     * Build market data from current state.
     */
    private TraderAgent.MarketData buildMarketData() {
        BigDecimal lastPrice = marketDataService.getLastPrice();
        if (lastPrice == null) {
            lastPrice = referencePrice;
        }

        return new TraderAgent.MarketData(
                lastPrice,
                marketDataService.getVWAP(DEFAULT_SYMBOL),
                marketDataService.getVolatility(DEFAULT_SYMBOL),
                marketDataService.getRecentVolume(DEFAULT_SYMBOL),
                marketDataService.getPriceChange(DEFAULT_SYMBOL, 10));
    }

    /**
     * Publish market metrics via WebSocket.
     */
    private void publishMetrics() {
        OrderBook orderBook = orderService.getOrCreateOrderBook(DEFAULT_SYMBOL);

        java.util.Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("symbol", DEFAULT_SYMBOL);
        metrics.put("midPrice", orderBook.getMidPrice());
        metrics.put("spread", orderBook.getSpread());
        metrics.put("bidDepth", orderBook.getTotalBidQuantity());
        metrics.put("askDepth", orderBook.getTotalAskQuantity());
        metrics.put("volatility", marketDataService.getVolatility(DEFAULT_SYMBOL));
        metrics.put("tickCount", tickCount);

        marketDataService.publishMetrics(DEFAULT_SYMBOL, metrics);
    }

    /**
     * Get current simulation status.
     */
    public SimulationStatus getStatus() {
        OrderBook orderBook = orderService.getOrCreateOrderBook(DEFAULT_SYMBOL);

        return new SimulationStatus(
                running.get(),
                tickCount,
                orderBook.getMidPrice(),
                orderBook.getSpread(),
                marketDataService.getVolatility(DEFAULT_SYMBOL),
                marketMaker.getPosition());
    }

    /**
     * Update reference price (for scenario testing).
     */
    public void setReferencePrice(BigDecimal price) {
        this.referencePrice = price;
    }

    /**
     * Status DTO for simulation state.
     */
    public record SimulationStatus(
            boolean running,
            long tickCount,
            BigDecimal midPrice,
            BigDecimal spread,
            double volatility,
            int marketMakerInventory) {
    }
}
