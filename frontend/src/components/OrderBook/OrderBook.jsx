import { useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import './OrderBook.css';

/**
 * OrderBook Component - Visualizes the live order book.
 * 
 * Shows bid (buy) orders on the left and ask (sell) orders on the right,
 * with depth bars indicating the relative quantity at each price level.
 * 
 * Analogy: Like a tug-of-war visualization - green bars (buyers) pull
 * from the left, red bars (sellers) push from the right. The bigger the
 * bar, the more orders at that price.
 */
export default function OrderBook({ orderBook }) {
    // Calculate max quantities for scaling the depth bars
    const { maxBidQty, maxAskQty, maxQty } = useMemo(() => {
        if (!orderBook?.bids?.length && !orderBook?.asks?.length) {
            return { maxBidQty: 1, maxAskQty: 1, maxQty: 1 };
        }

        const maxBid = Math.max(...(orderBook?.bids?.map((b) => b.quantity) || [1]));
        const maxAsk = Math.max(...(orderBook?.asks?.map((a) => a.quantity) || [1]));
        return { maxBidQty: maxBid, maxAskQty: maxAsk, maxQty: Math.max(maxBid, maxAsk) };
    }, [orderBook]);

    if (!orderBook) {
        return (
            <div className="order-book card">
                <div className="card-header">
                    <h3 className="card-title">Order Book</h3>
                </div>
                <div className="card-body order-book-loading">
                    <div className="animate-pulse">Loading order book...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="order-book card">
            <div className="card-header">
                <h3 className="card-title">Order Book</h3>
                <div className="order-book-spread">
                    <span className="spread-label">Spread:</span>
                    <span className="spread-value mono">
                        ${orderBook.spread?.toFixed(2) || '0.00'}
                    </span>
                </div>
            </div>

            <div className="card-body">
                {/* Column Headers */}
                <div className="order-book-header">
                    <span>QTY</span>
                    <span>BID</span>
                    <span>ASK</span>
                    <span>QTY</span>
                </div>

                <div className="order-book-grid">
                    {/* Bid Side (Buy Orders) */}
                    <div className="order-book-side order-book-bids">
                        <AnimatePresence>
                            {orderBook.bids?.slice(0, 10).map((level, i) => (
                                <motion.div
                                    key={level.price}
                                    initial={{ opacity: 0, x: -20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: -20 }}
                                    transition={{ duration: 0.2 }}
                                    className="order-book-row bid-row"
                                >
                                    <div
                                        className="depth-bar bid-bar"
                                        style={{ width: `${(level.quantity / maxQty) * 100}%` }}
                                    />
                                    <span className="qty mono">{level.quantity}</span>
                                    <span className="price mono">{parseFloat(level.price).toFixed(2)}</span>
                                </motion.div>
                            ))}
                        </AnimatePresence>
                    </div>

                    {/* Ask Side (Sell Orders) */}
                    <div className="order-book-side order-book-asks">
                        <AnimatePresence>
                            {orderBook.asks?.slice(0, 10).map((level, i) => (
                                <motion.div
                                    key={level.price}
                                    initial={{ opacity: 0, x: 20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: 20 }}
                                    transition={{ duration: 0.2 }}
                                    className="order-book-row ask-row"
                                >
                                    <span className="price mono">{parseFloat(level.price).toFixed(2)}</span>
                                    <span className="qty mono">{level.quantity}</span>
                                    <div
                                        className="depth-bar ask-bar"
                                        style={{ width: `${(level.quantity / maxQty) * 100}%` }}
                                    />
                                </motion.div>
                            ))}
                        </AnimatePresence>
                    </div>
                </div>

                {/* Mid Price Indicator */}
                <div className="mid-price-indicator">
                    <span className="mid-price-label">Mid Price</span>
                    <span className="mid-price-value mono">
                        ${orderBook.midPrice?.toFixed(2) || '—'}
                    </span>
                </div>
            </div>

            {/* Educational Tooltip */}
            <div className="order-book-tooltip">
                <div className="tooltip-icon">?</div>
                <div className="tooltip-content">
                    <strong>Order Book Explained:</strong>
                    <p>The order book shows all pending buy and sell orders.</p>
                    <ul>
                        <li><span className="price-up">Bids (Green)</span>: Buyers willing to pay this price</li>
                        <li><span className="price-down">Asks (Red)</span>: Sellers willing to accept this price</li>
                        <li><strong>Spread</strong>: The gap between highest bid and lowest ask</li>
                    </ul>
                    <p>Bigger bars = more orders at that price level</p>
                </div>
            </div>
        </div>
    );
}
