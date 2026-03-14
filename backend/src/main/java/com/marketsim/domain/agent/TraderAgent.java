package com.marketsim.domain.agent;

import com.marketsim.domain.order.Order;
import com.marketsim.domain.orderbook.OrderBook;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Base class for all trader agents in the simulation.
 * 
 * A trader agent represents an automated market participant with
 * a specific strategy/behavior pattern. This allows us to simulate
 * realistic market dynamics without needing human participants.
 */
@Getter
@Setter
public abstract class TraderAgent {

    protected final String id;
    protected final TraderType type;
    protected BigDecimal capital;
    protected int position; // Positive = long, negative = short
    protected boolean active;

    protected TraderAgent(TraderType type, BigDecimal initialCapital) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.capital = initialCapital;
        this.position = 0;
        this.active = true;
    }

    /**
     * Generate an order decision based on current market state.
     * 
     * @param orderBook  Current order book state
     * @param marketData Additional market data (prices, volumes, etc.)
     * @return Optional order to submit, empty if no action
     */
    public abstract Optional<Order> generateOrder(OrderBook orderBook, MarketData marketData);

    /**
     * Update agent state after a trade involving this agent.
     */
    public void onTrade(BigDecimal price, int quantity, boolean isBuyer) {
        BigDecimal tradeValue = price.multiply(BigDecimal.valueOf(quantity));

        if (isBuyer) {
            capital = capital.subtract(tradeValue);
            position += quantity;
        } else {
            capital = capital.add(tradeValue);
            position -= quantity;
        }
    }

    /**
     * Calculate the agent's current profit/loss.
     */
    public BigDecimal getPnL(BigDecimal currentPrice) {
        return capital
                .add(currentPrice.multiply(BigDecimal.valueOf(position)))
                .subtract(getInitialCapital());
    }

    protected abstract BigDecimal getInitialCapital();

    /**
     * Container for market data passed to agents.
     */
    @Getter
    public static class MarketData {
        private final BigDecimal lastPrice;
        private final BigDecimal vwap; // Volume-weighted average price
        private final double volatility;
        private final int recentVolume;
        private final double priceChange; // Percent change

        public MarketData(BigDecimal lastPrice, BigDecimal vwap, double volatility,
                int recentVolume, double priceChange) {
            this.lastPrice = lastPrice;
            this.vwap = vwap;
            this.volatility = volatility;
            this.recentVolume = recentVolume;
            this.priceChange = priceChange;
        }
    }
}
