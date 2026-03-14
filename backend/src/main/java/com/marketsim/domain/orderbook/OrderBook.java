package com.marketsim.domain.orderbook;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.order.OrderSide;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The Order Book - The Heart of Any Market.
 * 
 * An order book is a real-time list of all buy and sell orders for an asset,
 * organized by price. It's the mechanism through which price discovery happens.
 * 
 * Analogy: Think of the order book like a public bulletin board at an auction
 * house:
 * - Left side (bids): "WANTED: Shares at $99"
 * - Right side (asks): "FOR SALE: Shares at $101"
 * - The gap in the middle is the "spread" - the negotiation zone
 * 
 * Structure:
 * - Bids (buy orders): Sorted highest to lowest (best bid = highest price)
 * - Asks (sell orders): Sorted lowest to highest (best ask = lowest price)
 */
@Slf4j
@Getter
public class OrderBook {

    private final String symbol;

    // TreeMap with descending order for bids (highest price first)
    private final NavigableMap<BigDecimal, PriceLevel> bids;

    // TreeMap with ascending order for asks (lowest price first)
    private final NavigableMap<BigDecimal, PriceLevel> asks;

    // Quick lookup of orders by ID
    private final Map<UUID, Order> ordersById;

    // Thread safety for concurrent access
    private final ReentrantReadWriteLock lock;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        // Bids: descending (highest price = best bid comes first)
        this.bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        // Asks: ascending (lowest price = best ask comes first)
        this.asks = new ConcurrentSkipListMap<>();
        this.ordersById = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Add an order to the book.
     * 
     * @param order The order to add
     */
    public void addOrder(Order order) {
        lock.writeLock().lock();
        try {
            NavigableMap<BigDecimal, PriceLevel> book = getBookForSide(order.getSide());

            book.computeIfAbsent(
                    order.getPrice(),
                    price -> new PriceLevel(price, order.getSide())).addOrder(order);

            ordersById.put(order.getId(), order);
            log.debug("Added {} order {} at {} for {} shares",
                    order.getSide(), order.getId(), order.getPrice(), order.getQuantity());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove an order from the book.
     * 
     * @param orderId The order ID to remove
     * @return The removed order, or null if not found
     */
    public Order removeOrder(UUID orderId) {
        lock.writeLock().lock();
        try {
            Order order = ordersById.remove(orderId);
            if (order == null) {
                return null;
            }

            NavigableMap<BigDecimal, PriceLevel> book = getBookForSide(order.getSide());
            PriceLevel level = book.get(order.getPrice());

            if (level != null) {
                level.removeOrder(order);
                if (level.isEmpty()) {
                    book.remove(order.getPrice());
                }
            }

            order.cancel();
            log.debug("Removed order {}", orderId);
            return order;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get the best bid (highest buy price).
     * 
     * @return The best bid price, or null if no bids
     */
    public BigDecimal getBestBid() {
        lock.readLock().lock();
        try {
            Map.Entry<BigDecimal, PriceLevel> entry = bids.firstEntry();
            return entry != null ? entry.getKey() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the best ask (lowest sell price).
     * 
     * @return The best ask price, or null if no asks
     */
    public BigDecimal getBestAsk() {
        lock.readLock().lock();
        try {
            Map.Entry<BigDecimal, PriceLevel> entry = asks.firstEntry();
            return entry != null ? entry.getKey() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the mid-price (average of best bid and best ask).
     * This is often considered the "fair" price.
     * 
     * @return The mid-price, or null if book is one-sided
     */
    public BigDecimal getMidPrice() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();

        if (bestBid == null || bestAsk == null) {
            return bestBid != null ? bestBid : bestAsk;
        }

        return bestBid.add(bestAsk).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    /**
     * Get the spread (difference between best ask and best bid).
     * 
     * @return The spread, or null if book is one-sided
     */
    public BigDecimal getSpread() {
        BigDecimal bestBid = getBestBid();
        BigDecimal bestAsk = getBestAsk();

        if (bestBid == null || bestAsk == null) {
            return null;
        }

        return bestAsk.subtract(bestBid);
    }

    /**
     * Get the best price level for a side.
     * 
     * @param side The order side
     * @return The best price level, or null if empty
     */
    public PriceLevel getBestLevel(OrderSide side) {
        lock.readLock().lock();
        try {
            NavigableMap<BigDecimal, PriceLevel> book = getBookForSide(side);
            Map.Entry<BigDecimal, PriceLevel> entry = book.firstEntry();
            return entry != null ? entry.getValue() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a price level after all orders are filled.
     */
    public void removeEmptyLevel(BigDecimal price, OrderSide side) {
        NavigableMap<BigDecimal, PriceLevel> book = getBookForSide(side);
        PriceLevel level = book.get(price);
        if (level != null && level.isEmpty()) {
            book.remove(price);
        }
    }

    /**
     * Get an order by ID.
     */
    public Order getOrder(UUID orderId) {
        lock.readLock().lock();
        try {
            return ordersById.get(orderId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get a snapshot of the order book state.
     */
    public OrderBookSnapshot getSnapshot(int depth) {
        lock.readLock().lock();
        try {
            List<OrderBookSnapshot.PriceLevelSnapshot> bidLevels = new ArrayList<>();
            List<OrderBookSnapshot.PriceLevelSnapshot> askLevels = new ArrayList<>();

            int count = 0;
            for (Map.Entry<BigDecimal, PriceLevel> entry : bids.entrySet()) {
                if (count++ >= depth)
                    break;
                PriceLevel level = entry.getValue();
                bidLevels.add(new OrderBookSnapshot.PriceLevelSnapshot(
                        level.getPrice(), level.getTotalQuantity(), level.getOrderCount()));
            }

            count = 0;
            for (Map.Entry<BigDecimal, PriceLevel> entry : asks.entrySet()) {
                if (count++ >= depth)
                    break;
                PriceLevel level = entry.getValue();
                askLevels.add(new OrderBookSnapshot.PriceLevelSnapshot(
                        level.getPrice(), level.getTotalQuantity(), level.getOrderCount()));
            }

            return new OrderBookSnapshot(
                    symbol, getBestBid(), getBestAsk(), getMidPrice(), getSpread(),
                    bidLevels, askLevels);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the book (bids or asks) for a given side.
     */
    public NavigableMap<BigDecimal, PriceLevel> getBookForSide(OrderSide side) {
        return side == OrderSide.BUY ? bids : asks;
    }

    /**
     * Get the opposite book for matching.
     */
    public NavigableMap<BigDecimal, PriceLevel> getOppositeBook(OrderSide side) {
        return side == OrderSide.BUY ? asks : bids;
    }

    /**
     * Get total quantity at all bid levels.
     */
    public int getTotalBidQuantity() {
        lock.readLock().lock();
        try {
            return bids.values().stream().mapToInt(PriceLevel::getTotalQuantity).sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get total quantity at all ask levels.
     */
    public int getTotalAskQuantity() {
        lock.readLock().lock();
        try {
            return asks.values().stream().mapToInt(PriceLevel::getTotalQuantity).sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }
}
