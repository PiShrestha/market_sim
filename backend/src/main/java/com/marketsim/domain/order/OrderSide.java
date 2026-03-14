package com.marketsim.domain.order;

/**
 * Represents the side of an order - whether it's a buy or sell.
 * 
 * Analogy: In a farmer's market, you're either a buyer looking for produce
 * or a seller with goods to offer.
 */
public enum OrderSide {
    BUY, // Want to purchase shares (bid)
    SELL // Want to sell shares (ask/offer)
}
