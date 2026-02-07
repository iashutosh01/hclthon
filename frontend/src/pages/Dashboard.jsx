import React, { useState, useEffect, useCallback } from 'react';
import QueueTable from '../components/QueueTable';
import BaristaPanel from '../components/BaristaPanel';
import MetricsDashboard from '../components/MetricsDashboard';
import AllOrdersPanel from '../components/AllOrdersPanel';
import OrderAnalytics from '../components/OrderAnalytics';
import * as api from '../services/api';

const POLL_INTERVAL = 3000;


export default function Dashboard() {
  const [queue, setQueue] = useState([]);
  const [baristas, setBaristas] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState(null);
  const [manualOrder, setManualOrder] = useState({ customerName: '', drinkType: 'LATTE', loyaltyStatus: 'REGULAR' });
  const [showAllOrders, setShowAllOrders] = useState(false);
  const [testResult, setTestResult] = useState(null);
  const [testRunning, setTestRunning] = useState(false);
  const [orders, setOrders] = useState([]);
  const [ordersError, setOrdersError] = useState(null);

  const fetchAll = useCallback(async () => {
    try {
      const [q, b, m, s] = await Promise.all([
        api.getQueue(),
        api.getBaristas(),
        api.getMetrics(),
        api.getSimulationStatus(),
      ]);
      setQueue(q);
      setBaristas(b);
      setMetrics(m);
      setRunning(s.running);
      setError(null);
    } catch (e) {
      setError(e.message || 'Failed to fetch');
    }
  }, []);

  useEffect(() => {
    fetchAll();
    const id = setInterval(fetchAll, POLL_INTERVAL);
    return () => clearInterval(id);
  }, [fetchAll]);

  const fetchOrders = useCallback(async () => {
    try {
      const data = await api.getAllOrders();
      setOrders(data);
      setOrdersError(null);
    } catch (e) {
      setOrdersError(e.message || 'Failed to fetch orders');
    }
  }, []);

  useEffect(() => {
    fetchOrders();
    const id = setInterval(fetchOrders, POLL_INTERVAL);
    return () => clearInterval(id);
  }, [fetchOrders]);

  const handleStart = async () => {
    try {
      await api.startSimulation();
      setRunning(true);
      fetchAll();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleStop = async () => {
    try {
      await api.stopSimulation();
      setRunning(false);
      fetchAll();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleStartTestSimulation = async () => {
    setTestRunning(true);
    setTestResult(null);
    try {
      const result = await api.startTestSimulation();
      setTestResult(result);
      setError(null);
    } catch (e) {
      setError(e.message || 'Test simulation failed');
    } finally {
      setTestRunning(false);
    }
  };

  const handleCreateOrder = async (e) => {
    e.preventDefault();
    try {
      await api.createOrder(manualOrder);
      setManualOrder({ customerName: '', drinkType: 'LATTE', loyaltyStatus: 'REGULAR' });
      fetchAll();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="dashboard">
      <div className="dashboard-layout">
        <aside className="sidebar">
          <div className="sidebar-header">
            <h2>Barista!</h2>
            <span className={`status ${running ? 'on' : 'off'}`}>
              {running ? '● Running ' : '○ Stopped'}
            </span>
          </div>

          <div className="sidebar-section">
            <button className="primary" onClick={handleStart} disabled={running}>
              Start Simulation
            </button>
            <button className="secondary" onClick={handleStop} disabled={!running}>
              Stop Simulation
            </button>
            <button type="button" className="ghost" onClick={() => setShowAllOrders(true)}>
              View Order Details
            </button>
            <button type="button" className="ghost" onClick={handleStartTestSimulation} disabled={testRunning}>
              {testRunning ? 'Running Test...' : 'Start Test Simulation'}
            </button>
          </div>

          {error && <p className="error">{error}</p>}

          <div className="sidebar-section">
            <OrderAnalytics orders={orders} error={ordersError} />
          </div>
        </aside>

        <div className="content">
          <header className="header">
            <h1>Dashboard</h1>
          </header>

          <section className="manual-order">
            <h3>Manual Order:)</h3>
            <form onSubmit={handleCreateOrder}>
              <input
                type="text"
                placeholder="Customer name"
                value={manualOrder.customerName}
                onChange={(e) => setManualOrder({ ...manualOrder, customerName: e.target.value })}
              />
              <select
                value={manualOrder.drinkType}
                onChange={(e) => setManualOrder({ ...manualOrder, drinkType: e.target.value })}
              >
                <option value="COLD_BREW">Cold Brew (1 min)</option>
                <option value="ESPRESSO">Espresso (2 min)</option>
                <option value="AMERICANO">Americano (2 min)</option>
                <option value="CAPPUCCINO">Cappuccino (4 min)</option>
                <option value="LATTE">Latte (4 min)</option>
                <option value="MOCHA">Mocha (6 min)</option>
              </select>
              <select
                value={manualOrder.loyaltyStatus}
                onChange={(e) => setManualOrder({ ...manualOrder, loyaltyStatus: e.target.value })}
              >
                <option value="REGULAR">Regular</option>
                <option value="GOLD">Gold</option>
              </select>
              <button type="submit">Add Order</button>
            </form>
          </section>

          <AllOrdersPanel isOpen={showAllOrders} onClose={() => setShowAllOrders(false)} />

          {testResult && (
            <section className="test-simulation-result">
              <h3>Monte Carlo Test Results </h3>
              <div className="test-summary">{testResult.summary}</div>
              <div className="test-metrics">
                <div className="metric">Avg Wait: {testResult.avgWaitTimeMinutes?.toFixed(2)} min</div>
                <div className="metric">Timeout Rate: {(testResult.avgTimeoutRate * 100)?.toFixed(2)}%</div>
                <div className="metric">Workload Balance: {testResult.avgWorkloadBalancePercentage?.toFixed(1)}%</div>
                <div className="metric">Alerts to Manager: {testResult.totalAlertsSentToManager}</div>
                <div className="metric">Orders &gt;10 min: {testResult.totalOrdersExceeded10Min}</div>
                <div className="metric">Fairness Violations: {testResult.totalFairnessViolations}</div>
              </div>
              {testResult.results?.length > 0 && (
                <details className="test-details">
                  <summary>Per-run details (first 5 runs)</summary>
                  <table className="test-runs-table">
                    <thead>
                      <tr>
                        <th>Run</th>
                        <th>Avg Wait</th>
                        <th>Timeout %</th>
                        <th>Alerts</th>
                        <th>Barista 1</th>
                        <th>Barista 2</th>
                        <th>Barista 3</th>
                      </tr>
                    </thead>
                    <tbody>
                      {testResult.results.slice(0, 5).map((r) => (
                        <tr key={r.testCaseIndex}>
                          <td>{r.testCaseIndex}</td>
                          <td>{r.avgWaitTimeMinutes?.toFixed(2)} min</td>
                          <td>{(r.timeoutRate * 100)?.toFixed(2)}%</td>
                          <td>{r.alertsSentToManager}</td>
                          <td>{r.perBarista?.['Barista 1'] ? JSON.stringify(r.perBarista['Barista 1']) : '-'}</td>
                          <td>{r.perBarista?.['Barista 2'] ? JSON.stringify(r.perBarista['Barista 2']) : '-'}</td>
                          <td>{r.perBarista?.['Barista 3'] ? JSON.stringify(r.perBarista['Barista 3']) : '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </details>
              )}
            </section>
          )}

          <section className="main-content">
            <div className="left">
              <QueueTable queue={queue} />
              <BaristaPanel baristas={baristas} />
            </div>
            <div className="right">
              <MetricsDashboard metrics={metrics} />
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
