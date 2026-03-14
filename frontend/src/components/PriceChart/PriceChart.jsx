import { useState, useEffect, useMemo } from 'react';
import {
    ResponsiveContainer,
    AreaChart,
    Area,
    XAxis,
    YAxis,
    Tooltip,
    ReferenceLine,
} from 'recharts';
import './PriceChart.css';

/**
 * PriceChart Component - Visualizes price movement over time.
 * 
 * Shows the mid-price as an area chart, making it easy to see
 * trends and volatility at a glance.
 * 
 * Analogy: Like a heart rate monitor for the market - showing the
 * pulse of prices over time. Flat = calm, spiky = volatile.
 */
export default function PriceChart({ trades = [], midPrice }) {
    const [priceHistory, setPriceHistory] = useState([]);

    // Update price history when we get new trades
    useEffect(() => {
        if (midPrice) {
            setPriceHistory((prev) => {
                const newPoint = {
                    time: new Date().toLocaleTimeString('en-US', {
                        hour12: false,
                        minute: '2-digit',
                        second: '2-digit',
                    }),
                    price: parseFloat(midPrice),
                    timestamp: Date.now(),
                };

                // Keep last 60 data points (about 30 seconds at 500ms updates)
                const updated = [...prev, newPoint].slice(-60);
                return updated;
            });
        }
    }, [midPrice]);

    // Calculate price range for the chart
    const { minPrice, maxPrice, avgPrice, priceChange } = useMemo(() => {
        if (priceHistory.length === 0) {
            return { minPrice: 99, maxPrice: 101, avgPrice: 100, priceChange: 0 };
        }

        const prices = priceHistory.map((p) => p.price);
        const min = Math.min(...prices);
        const max = Math.max(...prices);
        const avg = prices.reduce((a, b) => a + b, 0) / prices.length;
        const change = prices.length > 1
            ? ((prices[prices.length - 1] - prices[0]) / prices[0]) * 100
            : 0;

        // Add padding to the range
        const padding = (max - min) * 0.1 || 0.5;
        return {
            minPrice: min - padding,
            maxPrice: max + padding,
            avgPrice: avg,
            priceChange: change,
        };
    }, [priceHistory]);

    const currentPrice = priceHistory.length > 0
        ? priceHistory[priceHistory.length - 1].price
        : midPrice || 100;

    const isUp = priceChange >= 0;

    return (
        <div className="price-chart card">
            <div className="card-header">
                <div className="price-info">
                    <h3 className="card-title">Price Chart</h3>
                    <div className="current-price-display">
                        <span className="current-price mono">${currentPrice.toFixed(2)}</span>
                        <span className={`price-change ${isUp ? 'up' : 'down'}`}>
                            {isUp ? '▲' : '▼'} {Math.abs(priceChange).toFixed(2)}%
                        </span>
                    </div>
                </div>
            </div>

            <div className="card-body chart-body">
                {priceHistory.length < 2 ? (
                    <div className="chart-placeholder">
                        <span className="animate-pulse">Waiting for price data...</span>
                        <span className="chart-hint">Start the simulation to see prices move</span>
                    </div>
                ) : (
                    <ResponsiveContainer width="100%" height={200}>
                        <AreaChart
                            data={priceHistory}
                            margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
                        >
                            <defs>
                                <linearGradient id="priceGradientUp" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stopColor="#10b981" stopOpacity={0.4} />
                                    <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                                </linearGradient>
                                <linearGradient id="priceGradientDown" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stopColor="#ef4444" stopOpacity={0.4} />
                                    <stop offset="100%" stopColor="#ef4444" stopOpacity={0} />
                                </linearGradient>
                            </defs>

                            <XAxis
                                dataKey="time"
                                stroke="#64748b"
                                fontSize={10}
                                tickLine={false}
                                axisLine={false}
                            />
                            <YAxis
                                domain={[minPrice, maxPrice]}
                                stroke="#64748b"
                                fontSize={10}
                                tickLine={false}
                                axisLine={false}
                                tickFormatter={(v) => `$${v.toFixed(2)}`}
                                width={55}
                            />

                            <Tooltip
                                contentStyle={{
                                    background: '#1a2332',
                                    border: '1px solid rgba(255,255,255,0.1)',
                                    borderRadius: '8px',
                                    fontSize: '12px',
                                }}
                                labelStyle={{ color: '#94a3b8' }}
                                formatter={(value) => [`$${value.toFixed(2)}`, 'Price']}
                            />

                            <ReferenceLine
                                y={avgPrice}
                                stroke="#64748b"
                                strokeDasharray="3 3"
                                strokeOpacity={0.5}
                            />

                            <Area
                                type="monotone"
                                dataKey="price"
                                stroke={isUp ? '#10b981' : '#ef4444'}
                                strokeWidth={2}
                                fill={isUp ? 'url(#priceGradientUp)' : 'url(#priceGradientDown)'}
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                )}
            </div>

            {/* Educational Tooltip */}
            <div className="chart-tooltip">
                <div className="tooltip-icon">?</div>
                <div className="tooltip-content">
                    <strong>Price Chart Explained:</strong>
                    <p>This shows the mid-price over time - the average of best bid and ask.</p>
                    <ul>
                        <li><span className="price-up">Green</span>: Price is up from start</li>
                        <li><span className="price-down">Red</span>: Price is down from start</li>
                        <li>Dashed line: Average price in this window</li>
                    </ul>
                    <p>Watch how your trades move the price!</p>
                </div>
            </div>
        </div>
    );
}
