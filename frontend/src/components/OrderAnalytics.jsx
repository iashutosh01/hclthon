import React, { useMemo } from 'react';

const formatDrink = (drinkType) => {
  if (!drinkType) return '-';
  return drinkType
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
};

const average = (values) => {
  if (!values.length) return 0;
  const total = values.reduce((sum, v) => sum + v, 0);
  return total / values.length;
};

export default function OrderAnalytics({ orders, error }) {
  const { itemRows, avgWait, avgTotal, waitCount, totalCount } = useMemo(() => {
    const safeOrders = Array.isArray(orders) ? orders : [];
    const itemCounts = new Map();
    const itemWait = new Map();
    const itemTotal = new Map();

    const waitTimes = [];
    const totalTimes = [];

    safeOrders.forEach((order) => {
      const drinkLabel = formatDrink(order.drinkType);
      itemCounts.set(drinkLabel, (itemCounts.get(drinkLabel) || 0) + 1);

      const isCompleted = order.status === 'COMPLETED';
      if (isCompleted && order.assignmentTime && order.arrivalTime) {
        const startMs = new Date(order.assignmentTime).getTime();
        const arrivalMs = new Date(order.arrivalTime).getTime();
        if (!Number.isNaN(startMs) && !Number.isNaN(arrivalMs)) {
          const wait = (startMs - arrivalMs) / 60000;
          waitTimes.push(wait);
          const curr = itemWait.get(drinkLabel) || { sum: 0, count: 0 };
          curr.sum += wait;
          curr.count += 1;
          itemWait.set(drinkLabel, curr);
        }
      }

      if (isCompleted && order.completionTime && order.arrivalTime) {
        const completionMs = new Date(order.completionTime).getTime();
        const arrivalMs = new Date(order.arrivalTime).getTime();
        if (!Number.isNaN(completionMs) && !Number.isNaN(arrivalMs)) {
          const total = (completionMs - arrivalMs) / 60000;
          totalTimes.push(total);
          const curr = itemTotal.get(drinkLabel) || { sum: 0, count: 0 };
          curr.sum += total;
          curr.count += 1;
          itemTotal.set(drinkLabel, curr);
        }
      }
    });

    const itemRowsLocal = Array.from(itemCounts.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([drink, count]) => {
        const wait = itemWait.get(drink);
        const total = itemTotal.get(drink);
        return {
          drink,
          count,
          avgWait: wait && wait.count ? wait.sum / wait.count : null,
          avgTotal: total && total.count ? total.sum / total.count : null,
        };
      });

    return {
      itemRows: itemRowsLocal,
      avgWait: average(waitTimes),
      avgTotal: average(totalTimes),
      waitCount: waitTimes.length,
      totalCount: totalTimes.length,
    };
  }, [orders]);

  return (
    <div className="order-analytics">
      <h3>Order Analytics</h3>
      {error && <p className="error">{error}</p>}

      <div className="analytics-summary">
        <div>
          <span className="label">Average wait time</span>
          <span className="value">{avgWait.toFixed(2)} min</span>
          <span className="hint">Based on {waitCount} completed orders</span>
        </div>
        <div>
          <span className="label">Average total time</span>
          <span className="value">{avgTotal.toFixed(2)} min</span>
          <span className="hint">Based on {totalCount} completed orders</span>
        </div>
      </div>

      {itemRows.length === 0 ? (
        <p className="muted">No orders available.</p>
      ) : (
        <table className="analytics-table">
          <thead>
            <tr>
              <th>Item</th>
              <th>Orders</th>
              <th>Avg wait</th>
              <th>Avg total</th>
            </tr>
          </thead>
          <tbody>
            {itemRows.map((row) => (
              <tr key={row.drink}>
                <td>{row.drink}</td>
                <td>{row.count}</td>
                <td>{row.avgWait === null ? '-' : `${row.avgWait.toFixed(2)} min`}</td>
                <td>{row.avgTotal === null ? '-' : `${row.avgTotal.toFixed(2)} min`}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
