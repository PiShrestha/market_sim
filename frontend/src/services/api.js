import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Order API calls
 */
export const orderApi = {
  /**
   * Submit a new order
   */
  submit: async (symbol, order) => {
    const response = await api.post(`/orders/${symbol}`, order);
    return response.data;
  },

  /**
   * Cancel an order
   */
  cancel: async (symbol, orderId) => {
    const response = await api.delete(`/orders/${symbol}/${orderId}`);
    return response.data;
  },

  /**
   * Get order by ID
   */
  get: async (symbol, orderId) => {
    const response = await api.get(`/orders/${symbol}/${orderId}`);
    return response.data;
  },
};

/**
 * Order Book API calls
 */
export const orderBookApi = {
  /**
   * Get current order book
   */
  get: async (symbol, depth = 10) => {
    const response = await api.get(`/orderbook/${symbol}`, { params: { depth } });
    return response.data;
  },
};

/**
 * Trade API calls
 */
export const tradeApi = {
  /**
   * Get recent trades
   */
  getRecent: async (symbol, limit = 50) => {
    const response = await api.get(`/trades/${symbol}`, { params: { limit } });
    return response.data;
  },
};

/**
 * Simulation API calls
 */
export const simulationApi = {
  /**
   * Start simulation
   */
  start: async () => {
    const response = await api.post('/simulation/start');
    return response.data;
  },

  /**
   * Stop simulation
   */
  stop: async () => {
    const response = await api.post('/simulation/stop');
    return response.data;
  },

  /**
   * Get simulation status
   */
  getStatus: async () => {
    const response = await api.get('/simulation/status');
    return response.data;
  },
};

export default api;
