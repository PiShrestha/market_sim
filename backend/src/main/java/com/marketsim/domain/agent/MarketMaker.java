package com.marketsim.domain.agent;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.order.OrderSide;
import com.marketsim.domain.order.OrderType;
import com.marketsim.domain.orderbook.OrderBook;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Market Maker Agent - The Liquidity Provider.
 * 
 * Market makers are the unsung heroes of liquid markets. They:
 * 1. Continuously quote bid and ask prices
 * 2. Provide liquidity (you can always trade with them)
 * 3. Earn the spread but take inventory risk
 * 
 * This implementation uses a simplified Avellaneda-Stoikov model:
 * - Adjusts quotes based on inventory (skews prices to reduce risk)
 * - Widens spread when volatility is high
 * - Narrows spread when competition is high
 * 
 * Analogy: Like a currency exchange booth at an airport.
 * They always buy and sell currencies, but at different rates.
 * If they have too many Euros, they'll offer a better rate to sell them.
 */
@Slf4j
@Getter
public class MarketMaker extends TraderAgent {

    // Risk aversion parameter (γ in the model)
    // Higher = more risk-averse = wider spreads
    private final double riskAversion;

    // Base spread the market maker wants to earn
    private final BigDecimal targetSpread;

    // Maximum inventory position (absolute value)
    private final int maxInventory;

    // Quote size (how many shares per quote)
    private final int quoteSize;

    // Last quoted bid and ask prices
    private BigDecimal lastBid;
    private BigDecimal lastAsk;

    private final BigDecimal initialCapital;

    public MarketMaker(BigDecimal initialCapital, double riskAversion,
            BigDecimal targetSpread, int maxInventory, int quoteSize) {
        super(TraderType.MARKET_MAKER, initialCapital);
        this.initialCapital = initialCapital;
        this.riskAversion = riskAversion;
        this.targetSpread = targetSpread;
        this.maxInventory = maxInventory;
        this.quoteSize = quoteSize;
    }

    /**
     * Calculate optimal bid and ask quotes based on current state.
     * 
     * Uses Avellaneda-Stoikov reservation price concept:
     * Reservation Price = Mid Price - inventory × risk_aversion × volatility² ×
     * time
     * 
     * Simplified for our simulation:
     * - Skew quotes based on inventory to reduce position
     * - Widen spread when volatility is high
     */
    public QuoteResult calculateQuotes(OrderBook orderBook, MarketData marketData) {
        BigDecimal midPrice = orderBook.getMidPrice();

        if (midPrice == null) {
            midPrice = marketData.getLastPrice();
        }

        if (midPrice == null) {
            return null; // Can't quote without a reference price
        }

        // Calculate inventory skew
        // If we're long, we want to sell more (lower our ask, raise our bid)
        // If we're short, we want to buy more (lower our bid, raise our ask)
        double inventoryRatio = (double) position / maxInventory;
        BigDecimal inventorySkew = midPrice
                .multiply(BigDecimal.valueOf(inventoryRatio))
                .multiply(BigDecimal.valueOf(riskAversion));

        // Calculate volatility adjustment
        // Higher volatility = wider spread (more risk)
        double volMultiplier = 1.0 + marketData.getVolatility();

        // Calculate half-spread
        BigDecimal halfSpread = targetSpread
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(volMultiplier));

        // Calculate reservation price (adjusted mid)
        BigDecimal reservationPrice = midPrice.subtract(inventorySkew);

        // Calculate bid and ask
        BigDecimal bid = reservationPrice.subtract(halfSpread)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ask = reservationPrice.add(halfSpread)
                .setScale(2, RoundingMode.HALF_UP);

        // Ensure bid < ask (sanity check)
        if (bid.compareTo(ask) >= 0) {
            bid = midPrice.subtract(targetSpread.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
            ask = midPrice.add(targetSpread.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
        }

        this.lastBid = bid;
        this.lastAsk = ask;

        log.debug("MM quotes: bid={}, ask={}, inventory={}, skew={}",
                bid, ask, position, inventorySkew);

        return new QuoteResult(bid, ask, quoteSize);
    }

    @Override
    public Optional<Order> generateOrder(OrderBook orderBook, MarketData marketData) {
        // Market makers submit quote pairs, not single orders
        // This method returns a single order for compatibility,
        // but typically we'd use calculateQuotes() directly

        QuoteResult quotes = calculateQuotes(orderBook, marketData);
        if (quotes == null) {
            return Optional.empty();
        }

        // Alternate between submitting bid and ask
        // In a real implementation, we'd submit both simultaneously
        if (position >= 0) {
            // We're long or neutral, prioritize the ask (selling)
            return Optional.of(Order.builder()
                    .traderId(id)
                    .symbol(orderBook.getSymbol())
                    .side(OrderSide.SELL)
                    .type(OrderType.LIMIT)
                    .price(quotes.ask)
                    .quantity(quoteSize)
                    .build());
        } else {
            // We're short, prioritize the bid (buying)
            return Optional.of(Order.builder()
                    .traderId(id)
                    .symbol(orderBook.getSymbol())
                    .side(OrderSide.BUY)
                    .type(OrderType.LIMIT)
                    .price(quotes.bid)
                    .quantity(quoteSize)
                    .build());
        }
    }

    @Override
    protected BigDecimal getInitialCapital() {
        return initialCapital;
    }

    /**
     * Result of quote calculation.
     */
    public record QuoteResult(BigDecimal bid, BigDecimal ask, int size) {
    }
}
