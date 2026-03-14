import { motion, AnimatePresence } from 'framer-motion';
import './TradeTape.css';

/**
 * TradeTape Component - Shows the flow of executed trades.
 * 
 * Also known as "Time and Sales" - this displays each trade as it happens,
 * showing price, quantity, and who initiated it (buyer or seller).
 * 
 * Analogy: Like a receipt printer at a store - each trade prints out
 * as it happens, showing exactly what was bought/sold and at what price.
 */
export default function TradeTape({ trades = [] }) {
    const formatTime = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('en-US', {
            hour12: false,
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
        });
    };

    const formatPrice = (price) => {
        return typeof price === 'number' ? price.toFixed(2) : parseFloat(price).toFixed(2);
    };

    return (
        <div className="trade-tape card">
            <div className="card-header">
                <h3 className="card-title">Trade Flow</h3>
                <span className="trade-count">{trades.length} trades</span>
            </div>

            <div className="card-body trade-tape-body">
                <div className="trade-tape-header">
                    <span>TIME</span>
                    <span>PRICE</span>
                    <span>QTY</span>
                    <span>SIDE</span>
                </div>

                <div className="trade-list">
                    <AnimatePresence initial={false}>
                        {trades.slice(0, 30).map((trade, index) => (
                            <motion.div
                                key={trade.id || index}
                                initial={{ opacity: 0, y: -20, height: 0 }}
                                animate={{ opacity: 1, y: 0, height: 'auto' }}
                                exit={{ opacity: 0 }}
                                transition={{ duration: 0.2 }}
                                className={`trade-row ${trade.aggressor === 'BUY' ? 'trade-buy' : 'trade-sell'}`}
                            >
                                <span className="trade-time mono">{formatTime(trade.timestamp)}</span>
                                <span className="trade-price mono">{formatPrice(trade.price)}</span>
                                <span className="trade-qty mono">{trade.quantity}</span>
                                <span className={`trade-side ${trade.aggressor === 'BUY' ? 'side-buy' : 'side-sell'}`}>
                                    {trade.aggressor === 'BUY' ? '↑ BUY' : '↓ SELL'}
                                </span>
                            </motion.div>
                        ))}
                    </AnimatePresence>

                    {trades.length === 0 && (
                        <div className="no-trades">
                            <span>No trades yet</span>
                            <span className="no-trades-hint">Submit an order to see trades</span>
                        </div>
                    )}
                </div>

                {/* Educational Tooltip */}
                <div className="trade-tape-tooltip">
                    <div className="tooltip-icon">?</div>
                    <div className="tooltip-content">
                        <strong>Trade Tape Explained:</strong>
                        <p>Each row is an executed trade - when a buy and sell order matched.</p>
                        <ul>
                            <li><span className="price-up">↑ BUY</span>: Buyer was aggressive (hit the ask)</li>
                            <li><span className="price-down">↓ SELL</span>: Seller was aggressive (hit the bid)</li>
                        </ul>
                        <p>Large trades often signal institutional activity!</p>
                    </div>
                </div>
            </div>
        </div>
    );
}
