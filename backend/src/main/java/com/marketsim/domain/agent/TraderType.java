package com.marketsim.domain.agent;

/**
 * Types of traders based on their behavior and strategy.
 * 
 * Each type represents a different market participant with distinct psychology.
 */
public enum TraderType {

    /**
     * Market Maker: Provides liquidity by continuously quoting bid/ask.
     * Earns the spread but takes inventory risk.
     * 
     * Analogy: Like a used car dealer who always has cars to sell
     * and always buys cars - makes money on the markup.
     */
    MARKET_MAKER,

    /**
     * Momentum Trader: Follows trends, buys rising prices, sells falling.
     * "The trend is your friend" mentality.
     * 
     * Analogy: Like a surfer riding waves - jump on when momentum builds.
     */
    MOMENTUM,

    /**
     * Mean Reversion Trader: Bets prices will return to average.
     * Buys dips, sells rallies.
     * 
     * Analogy: Like a pendulum - if it swings too far one way,
     * it will swing back.
     */
    MEAN_REVERSION,

    /**
     * Noise Trader: Makes random trades, represents uninformed participants.
     * Adds liquidity and unpredictability to the market.
     * 
     * Analogy: Like someone throwing darts blindfolded.
     */
    NOISE,

    /**
     * Fundamental Trader: Trades based on perceived "true value".
     * Provides long-term price anchoring.
     * 
     * Analogy: Like a home appraiser who knows what a house is "really" worth.
     */
    FUNDAMENTAL
}
