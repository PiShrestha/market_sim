package com.marketsim.domain.orderbook;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.order.OrderSide;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Represents a single price level in the order book.
 * 
 * A price level aggregates all orders at the same price, maintaining
 * FIFO (first-in-first-out) order for time priority.
 * 
 * Analogy: Think of a price level like a checkout line at a specific store.
 * Everyone in line wants the same thing (to buy/sell at this price),
 * and they're served in the order they arrived.
 */
@Getter
public class PriceLevel {

    private final BigDecimal price;
    private final OrderSide side;
    private final Queue<Order> orders;
    private int totalQuantity;

    public PriceLevel(BigDecimal price, OrderSide side) {
        this.price = price;
        this.side = side;
        this.orders = new LinkedList<>();
        this.totalQuantity = 0;
    }

    /**
     * Add an order to this price level.
     * Orders are queued in FIFO order for time priority.
     */
    public void addOrder(Order order) {
        if (!order.getPrice().equals(price)) {
            throw new IllegalArgumentException("Order price doesn't match price level");
        }
        orders.add(order);
        totalQuantity += order.getRemainingQuantity();
    }

    /**
     * Remove an order from this price level.
     */
    public boolean removeOrder(Order order) {
        boolean removed = orders.remove(order);
        if (removed) {
            totalQuantity -= order.getRemainingQuantity();
        }
        return removed;
    }

    /**
     * Get the first order in the queue (highest time priority).
     */
    public Order peekFirstOrder() {
        return orders.peek();
    }

    /**
     * Remove and return the first order in the queue.
     */
    public Order pollFirstOrder() {
        Order order = orders.poll();
        if (order != null) {
            totalQuantity -= order.getRemainingQuantity();
        }
        return order;
    }

    /**
     * Update total quantity when an order is partially filled.
     */
    public void decrementQuantity(int amount) {
        totalQuantity -= amount;
    }

    /**
     * Check if this price level has any orders.
     */
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    /**
     * Get the number of orders at this price level.
     */
    public int getOrderCount() {
        return orders.size();
    }

    /**
     * Get a snapshot of all orders at this price level.
     */
    public List<Order> getOrdersSnapshot() {
        return new ArrayList<>(orders);
    }
}
