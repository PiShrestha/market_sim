import { useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = 'http://localhost:8080/ws';

/**
 * Custom hook for WebSocket connection to market data streams.
 * 
 * @param {string} symbol - The trading symbol to subscribe to
 * @returns {Object} WebSocket data and controls
 */
export function useWebSocket(symbol) {
    const [connected, setConnected] = useState(false);
    const [orderBook, setOrderBook] = useState(null);
    const [trades, setTrades] = useState([]);
    const [metrics, setMetrics] = useState(null);
    const clientRef = useRef(null);

    const connect = useCallback(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            setConnected(true);
            console.log('WebSocket connected');

            // Subscribe to order book updates
            client.subscribe(`/topic/orderbook/${symbol}`, (message) => {
                const data = JSON.parse(message.body);
                setOrderBook(data);
            });

            // Subscribe to trade updates
            client.subscribe(`/topic/trades/${symbol}`, (message) => {
                const trade = JSON.parse(message.body);
                setTrades((prev) => [trade, ...prev.slice(0, 49)]); // Keep last 50
            });

            // Subscribe to metrics
            client.subscribe(`/topic/metrics/${symbol}`, (message) => {
                const data = JSON.parse(message.body);
                setMetrics(data);
            });
        };

        client.onDisconnect = () => {
            setConnected(false);
            console.log('WebSocket disconnected');
        };

        client.onStompError = (frame) => {
            console.error('STOMP error:', frame.headers.message);
        };

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, [symbol]);

    useEffect(() => {
        const cleanup = connect();
        return cleanup;
    }, [connect]);

    return {
        connected,
        orderBook,
        trades,
        metrics,
        reconnect: connect,
    };
}

/**
 * Custom hook for order book data with polling fallback.
 */
export function useOrderBook(symbol) {
    const [orderBook, setOrderBook] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const refresh = useCallback(async () => {
        try {
            const response = await fetch(`http://localhost:8080/api/orderbook/${symbol}?depth=10`);
            if (!response.ok) throw new Error('Failed to fetch order book');
            const data = await response.json();
            setOrderBook(data);
            setError(null);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [symbol]);

    useEffect(() => {
        refresh();
        // Poll every 500ms as fallback
        const interval = setInterval(refresh, 500);
        return () => clearInterval(interval);
    }, [refresh]);

    return { orderBook, loading, error, refresh };
}

/**
 * Custom hook for trade history.
 */
export function useTrades(symbol, limit = 50) {
    const [trades, setTrades] = useState([]);
    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {
        try {
            const response = await fetch(`http://localhost:8080/api/trades/${symbol}?limit=${limit}`);
            if (!response.ok) throw new Error('Failed to fetch trades');
            const data = await response.json();
            setTrades(data);
        } catch (err) {
            console.error('Error fetching trades:', err);
        } finally {
            setLoading(false);
        }
    }, [symbol, limit]);

    useEffect(() => {
        refresh();
        const interval = setInterval(refresh, 1000);
        return () => clearInterval(interval);
    }, [refresh]);

    return { trades, loading, refresh };
}
