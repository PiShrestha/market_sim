import { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import OrderBook from './components/OrderBook/OrderBook';
import TradeTape from './components/TradeTape/TradeTape';
import OrderForm from './components/OrderForm/OrderForm';
import PriceChart from './components/PriceChart/PriceChart';
import SimulationControls from './components/SimulationControls/SimulationControls';
import { useOrderBook, useTrades } from './hooks/useMarketData';
import { simulationApi } from './services/api';
import './App.css';

const SYMBOL = 'SIMU';

/**
 * Main Application Component
 * 
 * The Market Simulation Platform - an educational tool for understanding
 * how markets work, who sets prices, and why you can always buy/sell.
 */
function App() {
  const [simStatus, setSimStatus] = useState(null);
  const { orderBook, refresh: refreshOrderBook } = useOrderBook(SYMBOL);
  const { trades, refresh: refreshTrades } = useTrades(SYMBOL);

  // Fetch initial simulation status
  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const status = await simulationApi.getStatus();
        setSimStatus(status);
      } catch (error) {
        console.error('Failed to fetch simulation status:', error);
      }
    };
    fetchStatus();

    // Poll status every 2 seconds
    const interval = setInterval(fetchStatus, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleOrderSubmit = useCallback(() => {
    // Refresh data after order submission
    refreshOrderBook();
    refreshTrades();
  }, [refreshOrderBook, refreshTrades]);

  return (
    <div className="app">
      {/* Header */}
      <header className="app-header">
        <div className="logo">
          <span className="logo-icon">📊</span>
          <h1 className="logo-text">MarketSim</h1>
        </div>
        <p className="tagline">
          Understand how markets really work
        </p>
      </header>

      {/* Simulation Controls */}
      <section className="controls-section">
        <SimulationControls
          status={simStatus}
          onStatusChange={setSimStatus}
        />
      </section>

      {/* Main Dashboard Grid */}
      <main className="dashboard">
        {/* Left Column - Order Book */}
        <motion.div
          className="panel panel-orderbook"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.1 }}
        >
          <OrderBook orderBook={orderBook} />
        </motion.div>

        {/* Center Column - Price Chart & Trade Tape */}
        <div className="center-column">
          <motion.div
            className="panel panel-chart"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            <PriceChart
              trades={trades}
              midPrice={orderBook?.midPrice}
            />
          </motion.div>

          <motion.div
            className="panel panel-trades"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
          >
            <TradeTape trades={trades} />
          </motion.div>
        </div>

        {/* Right Column - Order Form */}
        <motion.div
          className="panel panel-form"
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.4 }}
        >
          <OrderForm symbol={SYMBOL} onOrderSubmit={handleOrderSubmit} />
        </motion.div>
      </main>

      {/* Educational Footer */}
      <footer className="app-footer">
        <div className="concepts-bar">
          <div className="concept">
            <span className="concept-icon">📈</span>
            <div className="concept-text">
              <strong>Price Discovery</strong>
              <span>Where buyers meet sellers</span>
            </div>
          </div>
          <div className="concept">
            <span className="concept-icon">💧</span>
            <div className="concept-text">
              <strong>Liquidity</strong>
              <span>How easily you can trade</span>
            </div>
          </div>
          <div className="concept">
            <span className="concept-icon">⚖️</span>
            <div className="concept-text">
              <strong>Market Makers</strong>
              <span>Always ready to trade</span>
            </div>
          </div>
          <div className="concept">
            <span className="concept-icon">📊</span>
            <div className="concept-text">
              <strong>Order Book</strong>
              <span>All pending orders</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
