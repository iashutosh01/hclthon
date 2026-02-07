
import React, { useState, useEffect } from 'react';
import * as api from '../services/api';

const STATUS_LABELS = {
  QUEUED: 'Pending',
  PREPARING: 'In Service',
  COMPLETED: 'Completed',
  TIMEOUT: 'Completed',
};

const FILTER_OPTIONS = ['All', 'Pending', 'In Service', 'Completed'];

export default function AllOrdersPanel({ isOpen, onClose }) {
  const [orders, setOrders] = useState([]);
  const [filter, setFilter] = useState('All');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      setError(null);
      api
        .getAllOrders()
        .then((data) => {
          setOrders(data);
          setLoading(false);
        })
        .catch((e) => {
          setError(e.message || 'Failed to fetch orders');
          setLoading(false);
        });
    }
  }, [isOpen]);

  const filteredOrders =
    filter === 'All'
      ? orders
      : orders.filter((o) => STATUS_LABELS[o.status] === filter);

  const displayStatus = (status) => STATUS_LABELS[status] ?? status;

  if (!isOpen) return null;

  return (
    <div className="all-orders-panel-overlay" onClick={onClose}>
      <div className="all-orders-panel" onClick={(e) => e.stopPropagation()}>
        <div className="all-orders-header">
          <h3>All Orders</h3>
          <button type="button" className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="all-orders-filters">
          {FILTER_OPTIONS.map((opt) => (
            <button
              key={opt}
              type="button"
              className={filter === opt ? 'active' : ''}
              onClick={() => setFilter(opt)}
            >
              {opt}
            </button>
          ))}
        </div>

        {loading && <p className="loading">Loading orders...</p>}
        {error && <p className="error">{error}</p>}

        {!loading && !error && (
          <div className="all-orders-table-wrap">
            <table className="all-orders-table">
              <thead>
                <tr>
                  <th>Customer</th>
                  <th>Drink</th>
                  <th>Prep</th>
                  <th>Status</th>
                  <th>Arrival</th>
                  <th>Service Start</th>
                  <th>Completion</th>
                  <th>Barista</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrders.length === 0 ? (
                  <tr>
                    <td colSpan="8">No orders found</td>
                  </tr>
                ) : (
                  filteredOrders.map((o) => (
                    <tr key={o.id}>
                      <td>{o.customerName}</td>
                      <td>{o.drinkType}</td>
                      <td>{o.prepTimeMinutes} min</td>
                      <td>{displayStatus(o.status)}</td>
                      <td>
                        {o.arrivalTime
                          ? new Date(o.arrivalTime).toLocaleTimeString()
                          : '-'}
                      </td>
                      <td>
                        {o.assignmentTime
                          ? new Date(o.assignmentTime).toLocaleTimeString()
                          : '-'}
                      </td>
                      <td>
                        {o.completionTime
                          ? new Date(o.completionTime).toLocaleTimeString()
                          : '-'}
                      </td>
                      <td>{o.baristaName ?? '-'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
