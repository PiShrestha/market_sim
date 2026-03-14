import { useState } from 'react';
import { motion } from 'framer-motion';
import { simulationApi } from '../../services/api';
import './SimulationControls.css';

/**
 * SimulationControls Component - Start/stop the market simulation.
 * 
 * Shows simulation status and lets users control the market maker
 * and other automated agents.
 */
export default function SimulationControls({ status, onStatusChange }) {
    const [loading, setLoading] = useState(false);

    const handleStart = async () => {
        setLoading(true);
        try {
            const newStatus = await simulationApi.start();
            onStatusChange?.(newStatus);
        } catch (error) {
            console.error('Failed to start simulation:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleStop = async () => {
        setLoading(true);
        try {
            const newStatus = await simulationApi.stop();
            onStatusChange?.(newStatus);
        } catch (error) {
            console.error('Failed to stop simulation:', error);
        } finally {
            setLoading(false);
        }
    };

    const isRunning = status?.running;

    return (
        <div className="simulation-controls">
            <div className="status-indicator">
                <div className={`status-dot ${isRunning ? 'running' : 'stopped'}`} />
                <span className="status-text">
                    {isRunning ? 'Simulation Running' : 'Simulation Stopped'}
                </span>
            </div>

            <div className="control-buttons">
                {!isRunning ? (
                    <motion.button
                        className="btn btn-primary start-btn"
                        onClick={handleStart}
                        disabled={loading}
                        whileTap={{ scale: 0.95 }}
                    >
                        {loading ? '...' : '▶ Start'}
                    </motion.button>
                ) : (
                    <motion.button
                        className="btn stop-btn"
                        onClick={handleStop}
                        disabled={loading}
                        whileTap={{ scale: 0.95 }}
                    >
                        {loading ? '...' : '■ Stop'}
                    </motion.button>
                )}
            </div>

            {status && (
                <div className="status-metrics">
                    <div className="metric">
                        <span className="metric-label">Ticks</span>
                        <span className="metric-value mono">{status.tickCount || 0}</span>
                    </div>
                    {status.volatility !== undefined && (
                        <div className="metric">
                            <span className="metric-label">Volatility</span>
                            <span className="metric-value mono">{(status.volatility * 100).toFixed(2)}%</span>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
