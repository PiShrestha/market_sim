import { useState } from 'react';
import { motion } from 'framer-motion';
import { orderApi } from '../../services/api';
import './OrderForm.css';

/**
 * OrderForm Component - Interactive trading panel.
 * 
 * Allows users to submit buy/sell orders to see how they affect
 * the market. Includes educational labels explaining each field.
 */
export default function OrderForm({ symbol = 'SIMU', onOrderSubmit }) {
    const [side, setSide] = useState('BUY');
    const [type, setType] = useState('LIMIT');
    const [price, setPrice] = useState('');
    const [quantity, setQuantity] = useState('10');
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setResult(null);

        try {
            const order = {
                traderId: 'user-' + Math.random().toString(36).substr(2, 9),
                side,
                type,
                quantity: parseInt(quantity),
                ...(type === 'LIMIT' && { price: parseFloat(price) }),
            };

            const response = await orderApi.submit(symbol, order);

            const tradeCount = response.trades?.length || 0;
            const message = tradeCount > 0
                ? `Order filled! ${tradeCount} trade(s) executed`
                : `Order placed on the book at $${price}`;

            setResult({ success: true, message });

            if (onOrderSubmit) {
                onOrderSubmit(response);
            }

            // Clear form after successful submission
            if (type === 'LIMIT') setPrice('');
        } catch (error) {
            setResult({
                success: false,
                message: error.response?.data?.message || 'Failed to submit order',
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="order-form card">
            <div className="card-header">
                <h3 className="card-title">Submit Order</h3>
                <span className="symbol-badge mono">{symbol}</span>
            </div>

            <form className="card-body" onSubmit={handleSubmit}>
                {/* Side Selection */}
                <div className="form-group">
                    <label className="input-label">Side</label>
                    <div className="side-toggle">
                        <button
                            type="button"
                            className={`side-btn buy-btn ${side === 'BUY' ? 'active' : ''}`}
                            onClick={() => setSide('BUY')}
                        >
                            BUY
                        </button>
                        <button
                            type="button"
                            className={`side-btn sell-btn ${side === 'SELL' ? 'active' : ''}`}
                            onClick={() => setSide('SELL')}
                        >
                            SELL
                        </button>
                    </div>
                    <span className="form-hint">
                        {side === 'BUY'
                            ? '💡 Buying = Hoping price goes UP'
                            : '💡 Selling = Hoping price goes DOWN'}
                    </span>
                </div>

                {/* Order Type */}
                <div className="form-group">
                    <label className="input-label">Order Type</label>
                    <div className="type-toggle">
                        <button
                            type="button"
                            className={`type-btn ${type === 'LIMIT' ? 'active' : ''}`}
                            onClick={() => setType('LIMIT')}
                        >
                            LIMIT
                        </button>
                        <button
                            type="button"
                            className={`type-btn ${type === 'MARKET' ? 'active' : ''}`}
                            onClick={() => setType('MARKET')}
                        >
                            MARKET
                        </button>
                    </div>
                    <span className="form-hint">
                        {type === 'LIMIT'
                            ? '💡 Limit = Only execute at your price or better'
                            : '💡 Market = Execute NOW at current price'}
                    </span>
                </div>

                {/* Price Input (for LIMIT orders) */}
                {type === 'LIMIT' && (
                    <motion.div
                        className="form-group"
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                    >
                        <label className="input-label">Price</label>
                        <div className="input-wrapper">
                            <span className="input-prefix">$</span>
                            <input
                                type="number"
                                className="input mono"
                                value={price}
                                onChange={(e) => setPrice(e.target.value)}
                                placeholder="100.00"
                                step="0.01"
                                min="0.01"
                                required={type === 'LIMIT'}
                            />
                        </div>
                    </motion.div>
                )}

                {/* Quantity Input */}
                <div className="form-group">
                    <label className="input-label">Quantity</label>
                    <input
                        type="number"
                        className="input mono"
                        value={quantity}
                        onChange={(e) => setQuantity(e.target.value)}
                        placeholder="10"
                        min="1"
                        required
                    />
                    <span className="form-hint">Number of shares to trade</span>
                </div>

                {/* Submit Button */}
                <motion.button
                    type="submit"
                    className={`submit-btn ${side === 'BUY' ? 'btn-buy' : 'btn-sell'}`}
                    disabled={loading || (type === 'LIMIT' && !price)}
                    whileTap={{ scale: 0.98 }}
                    whileHover={{ scale: 1.02 }}
                >
                    {loading ? (
                        <span className="loading-spinner" />
                    ) : (
                        <>
                            {side === 'BUY' ? '🚀 ' : '📉 '}
                            {side} {quantity} @ {type === 'MARKET' ? 'Market' : `$${price || '?'}`}
                        </>
                    )}
                </motion.button>

                {/* Result Message */}
                {result && (
                    <motion.div
                        className={`result-message ${result.success ? 'success' : 'error'}`}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        {result.success ? '✓ ' : '✗ '}
                        {result.message}
                    </motion.div>
                )}
            </form>
        </div>
    );
}
