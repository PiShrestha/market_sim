package com.marketsim.engine;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.order.OrderSide;
import com.marketsim.domain.order.OrderType;
import com.marketsim.domain.orderbook.OrderBook;
import com.marketsim.domain.orderbook.PriceLevel;
import com.marketsim.domain.trade.Trade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

/**
 * The Matching Engine - The Brain of the Exchange.
 * 
 * The matching engine is responsible for:
 * 1. Receiving incoming orders
 * 2. Attempting to match them against existing orders in the book
 * 3. Generating trades when matches occur
 * 4. Adding unmatched portions to the order book
 * 
 * It implements Price-Time Priority (FIFO within each price level):
 * - Best price gets matched first
 * - At the same price, earliest order gets matched first
 * 
 * Analogy: Think of the matching engine as a super-efficient auctioneer.
 * When someone shouts "I'll buy at $100!", the auctioneer instantly checks
 * if anyone is willing to sell at $100 or less, starting with whoever
 * offered the lowest price first, then whoever arrived earliest.
 */
@Slf4j
@Service
public class MatchingEngine {

    /**
     * Process an incoming order against the order book.
     * 
     * @param order     The incoming order
     * @param orderBook The order book to match against
     * @return List of trades generated (empty if no matches)
     */
    public List<Trade> processOrder(Order order, OrderBook orderBook) {
        List<Trade> trades = new ArrayList<>();

        orderBook.getLock().writeLock().lock();
        try {
            // Get the opposite side of the book to match against
            NavigableMap<BigDecimal, PriceLevel> oppositeBook = orderBook.getOppositeBook(order.getSide());

            // Keep matching while:
            // 1. Order has remaining quantity
            // 2. There are orders on the opposite side to match
            // 3. Prices are compatible (for limit orders)
            while (order.getRemainingQuantity() > 0 && !oppositeBook.isEmpty()) {
                // Get the best price level on the opposite side
                PriceLevel bestLevel = oppositeBook.firstEntry().getValue();
                BigDecimal matchPrice = bestLevel.getPrice();

                // Check if prices are compatible
                if (!isPriceCompatible(order, matchPrice)) {
                    break; // No match possible
                }

                // Match against orders at this price level
                List<Trade> levelTrades = matchAtPriceLevel(order, bestLevel, orderBook);
                trades.addAll(levelTrades);

                // Remove price level if empty
                if (bestLevel.isEmpty()) {
                    oppositeBook.remove(matchPrice);
                }
            }

            // If order still has remaining quantity and is a limit order, add to book
            if (order.getRemainingQuantity() > 0 && order.getType() == OrderType.LIMIT) {
                orderBook.addOrder(order);
                log.info("Added remaining {} of order {} to book at {}",
                        order.getRemainingQuantity(), order.getId(), order.getPrice());
            }

            return trades;
        } finally {
            orderBook.getLock().writeLock().unlock();
        }
    }

    /**
     * Check if an incoming order's price is compatible with a resting order's
     * price.
     */
    private boolean isPriceCompatible(Order incomingOrder, BigDecimal restingPrice) {
        // Market orders match at any price
        if (incomingOrder.getType() == OrderType.MARKET) {
            return true;
        }

        BigDecimal limitPrice = incomingOrder.getPrice();

        if (incomingOrder.getSide() == OrderSide.BUY) {
            // Buy order: willing to pay up to limitPrice
            // Match if resting ask <= limitPrice
            return restingPrice.compareTo(limitPrice) <= 0;
        } else {
            // Sell order: willing to accept down to limitPrice
            // Match if resting bid >= limitPrice
            return restingPrice.compareTo(limitPrice) >= 0;
        }
    }

    /**
     * Match an incoming order against all orders at a specific price level.
     * Follows time priority (FIFO).
     */
    private List<Trade> matchAtPriceLevel(Order incomingOrder, PriceLevel priceLevel, OrderBook orderBook) {
        List<Trade> trades = new ArrayList<>();

        while (incomingOrder.getRemainingQuantity() > 0 && !priceLevel.isEmpty()) {
            Order restingOrder = priceLevel.peekFirstOrder();

            // Determine trade quantity
            int tradeQuantity = Math.min(
                    incomingOrder.getRemainingQuantity(),
                    restingOrder.getRemainingQuantity());

            // Execute the trade at the resting order's price
            // (Price priority goes to the resting order)
            Trade trade = executeTrade(incomingOrder, restingOrder, priceLevel.getPrice(), tradeQuantity);
            trades.add(trade);

            // Update quantities and remove filled orders
            priceLevel.decrementQuantity(tradeQuantity);

            if (restingOrder.isFilled()) {
                priceLevel.pollFirstOrder();
                orderBook.getOrdersById().remove(restingOrder.getId());
                log.debug("Resting order {} fully filled", restingOrder.getId());
            }
        }

        return trades;
    }

    /**
     * Execute a trade between two orders.
     */
    private Trade executeTrade(Order incomingOrder, Order restingOrder, BigDecimal price, int quantity) {
        // Fill both orders
        incomingOrder.fill(quantity);
        restingOrder.fill(quantity);

        // Determine buyer and seller
        Order buyOrder = incomingOrder.getSide() == OrderSide.BUY ? incomingOrder : restingOrder;
        Order sellOrder = incomingOrder.getSide() == OrderSide.SELL ? incomingOrder : restingOrder;

        // Determine aggressor (the incoming order is always the aggressor)
        Trade.AggressorSide aggressor = incomingOrder.getSide() == OrderSide.BUY
                ? Trade.AggressorSide.BUY
                : Trade.AggressorSide.SELL;

        Trade trade = Trade.builder()
                .symbol(incomingOrder.getSymbol())
                .buyOrderId(buyOrder.getId())
                .sellOrderId(sellOrder.getId())
                .buyerId(buyOrder.getTraderId())
                .sellerId(sellOrder.getTraderId())
                .price(price)
                .quantity(quantity)
                .aggressor(aggressor)
                .build();

        log.info("Trade executed: {} shares at {} (aggressor: {})",
                quantity, price, aggressor);

        return trade;
    }

    /**
     * Cancel an order from the book.
     * 
     * @param orderId   The order ID to cancel
     * @param orderBook The order book
     * @return The cancelled order, or null if not found
     */
    public Order cancelOrder(java.util.UUID orderId, OrderBook orderBook) {
        return orderBook.removeOrder(orderId);
    }
}
