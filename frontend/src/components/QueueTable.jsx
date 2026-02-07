import React from 'react';


export default function QueueTable({ queue }) {
  if (!queue || queue.length === 0) {
    return (
      <div className="queue-table empty">
        <p>Queue is empty. Start simulation or add a manual order.</p>
      </div>
    );
  }

  return (
    <div className="queue-table">
      <h3>Waiting Queue (by priority)</h3>
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Customer</th>
            <th>Drink</th>
            <th>Prep (min)</th>
            <th>Wait (min)</th>
            <th>Est. Wait</th>
            <th>Priority</th>
            <th>Loyalty</th>
            <th>Assignment Reason</th>
          </tr>
        </thead>
        <tbody>
          {queue.map((order, idx) => (
            <tr key={order.id} className={order.waitTimeMinutes >= 8 ? 'urgent' : ''}>
              <td>{idx + 1}</td>
              <td>{order.customerName}</td>
              <td>{order.drinkType}</td>
              <td>{order.prepTimeMinutes}</td>
              <td>
                <span className={order.waitTimeMinutes >= 8 ? 'badge urgent' : ''}>
                  {order.waitTimeMinutes} min
                </span>
              </td>
              <td>~{order.estimatedWaitMinutes} min</td>
              <td>{order.priorityScore != null ? order.priorityScore.toFixed(1) : '-'}</td>
              <td>{order.loyaltyStatus}</td>
              <td className="reason">
                {order.assignmentReason || '-'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
