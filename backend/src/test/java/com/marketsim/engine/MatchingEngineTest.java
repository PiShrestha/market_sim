package com.marketsim.engine;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.order.OrderSide;
import com.marketsim.domain.order.OrderType;
import com.marketsim.domain.orderbook.OrderBook;
import com.marketsim.domain.trade.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MatchingEngine.
 * Validates price-time priority matching and trade execution.
 */
class MatchingEngineTest {

    private MatchingEngine matchingEngine;
    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        matchingEngine = new MatchingEngine();
        orderBook = new OrderBook("TEST");
    }

    @Test
    @DisplayName("Limit order with no match goes to book")
    void limitOrderNoMatch() {
        Order sellOrder = Order.builder()
                .traderId("seller1")
                .symbol("TEST")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();

        List<Trade> trades = matchingEngine.processOrder(sellOrder, orderBook);

        assertTrue(trades.isEmpty(), "No trades should occur");
        assertEquals(new BigDecimal("100.00"), orderBook.getBestAsk());
        assertEquals(10, orderBook.getTotalAskQuantity());
    }

    @Test
    @DisplayName("Matching buy order executes trade at resting price")
    void matchingBuyOrder() {
        // First, add a sell order to the book
        Order sellOrder = Order.builder()
                .traderId("seller1")
                .symbol("TEST")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();
        matchingEngine.processOrder(sellOrder, orderBook);

        // Now submit a matching buy order
        Order buyOrder = Order.builder()
                .traderId("buyer1")
                .symbol("TEST")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(5)
                .build();

        List<Trade> trades = matchingEngine.processOrder(buyOrder, orderBook);

        assertEquals(1, trades.size(), "One trade should occur");
        Trade trade = trades.get(0);
        assertEquals(new BigDecimal("100.00"), trade.getPrice());
        assertEquals(5, trade.getQuantity());
        assertEquals("buyer1", trade.getBuyerId());
        assertEquals("seller1", trade.getSellerId());
        assertEquals(Trade.AggressorSide.BUY, trade.getAggressor());

        // Verify remaining quantity in book
        assertEquals(5, orderBook.getTotalAskQuantity());
    }

    @Test
    @DisplayName("Price priority - best price matches first")
    void pricePriority() {
        // Add two sell orders at different prices
        Order sell1 = Order.builder()
                .traderId("seller1")
                .symbol("TEST")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("101.00"))
                .quantity(10)
                .build();
        Order sell2 = Order.builder()
                .traderId("seller2")
                .symbol("TEST")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();

        matchingEngine.processOrder(sell1, orderBook);
        matchingEngine.processOrder(sell2, orderBook);

        // Buy order should match with the better (lower) price first
        Order buyOrder = Order.builder()
                .traderId("buyer1")
                .symbol("TEST")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("101.00"))
                .quantity(5)
                .build();

        List<Trade> trades = matchingEngine.processOrder(buyOrder, orderBook);

        assertEquals(1, trades.size());
        assertEquals(new BigDecimal("100.00"), trades.get(0).getPrice());
        assertEquals("seller2", trades.get(0).getSellerId());
    }

    @Test
    @DisplayName("Time priority - first order at same price matches first")
    void timePriority() {
        // Add two sell orders at the same price
        Order sell1 = Order.builder()
                .traderId("seller1")
                .symbol("TEST")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(5)
                .build();
        Order sell2 = Order.builder()
                .traderId("seller2")
                .symbol("TEST")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(5)
                .build();

        matchingEngine.processOrder(sell1, orderBook);
        matchingEngine.processOrder(sell2, orderBook);

        // Buy order should match with the first seller
        Order buyOrder = Order.builder()
                .traderId("buyer1")
                .symbol("TEST")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(5)
                .build();

        List<Trade> trades = matchingEngine.processOrder(buyOrder, orderBook);

        assertEquals(1, trades.size());
        assertEquals("seller1", trades.get(0).getSellerId());
    }

    @Test
    @DisplayName("Market order matches any price")
    void marketOrder() {
        Order sellOrder = Order.builder()
                .traderId("seller1")
                .symbol("TEST")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(10)
                .build();
        matchingEngine.processOrder(sellOrder, orderBook);

        // Market buy order (no price limit)
        Order marketBuy = Order.builder()
                .traderId("buyer1")
                .symbol("TEST")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(5)
                .build();

        List<Trade> trades = matchingEngine.processOrder(marketBuy, orderBook);

        assertEquals(1, trades.size());
        assertEquals(5, trades.get(0).getQuantity());
    }

    @Test
    @DisplayName("Partial fills create multiple trades")
    void partialFills() {
        // Add multiple sell orders
        matchingEngine.processOrder(Order.builder()
                .traderId("seller1").symbol("TEST").side(OrderSide.SELL)
                .type(OrderType.LIMIT).price(new BigDecimal("100.00")).quantity(3).build(), orderBook);

        matchingEngine.processOrder(Order.builder()
                .traderId("seller2").symbol("TEST").side(OrderSide.SELL)
                .type(OrderType.LIMIT).price(new BigDecimal("100.50")).quantity(5).build(), orderBook);

        // Large buy order that spans multiple levels
        Order largeBuy = Order.builder()
                .traderId("buyer1")
                .symbol("TEST")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("101.00"))
                .quantity(6)
                .build();

        List<Trade> trades = matchingEngine.processOrder(largeBuy, orderBook);

        assertEquals(2, trades.size());
        assertEquals(3, trades.get(0).getQuantity()); // From seller1
        assertEquals(3, trades.get(1).getQuantity()); // From seller2
    }

    @Test
    @DisplayName("Mid price calculation is correct")
    void midPriceCalculation() {
        orderBook.addOrder(Order.builder()
                .traderId("buyer1").symbol("TEST").side(OrderSide.BUY)
                .type(OrderType.LIMIT).price(new BigDecimal("99.00")).quantity(10).build());

        orderBook.addOrder(Order.builder()
                .traderId("seller1").symbol("TEST").side(OrderSide.SELL)
                .type(OrderType.LIMIT).price(new BigDecimal("101.00")).quantity(10).build());

        assertEquals(new BigDecimal("99.00"), orderBook.getBestBid());
        assertEquals(new BigDecimal("101.00"), orderBook.getBestAsk());
        assertEquals(new BigDecimal("100.0000"), orderBook.getMidPrice());
        assertEquals(new BigDecimal("2.00"), orderBook.getSpread());
    }
}
