import React from 'react';


export default function BaristaPanel({ baristas }) {
  if (!baristas || baristas.length === 0) {
    return (
      <div className="barista-panel">
        <p>No baristas loaded. Start simulation to create baristas.</p>
      </div>
    );
  }

  return (
    <div className="barista-panel">
      <h3>Barista Status</h3>
      <div className="barista-cards">
        {baristas.map((b) => (
          <div
            key={b.id}
            className={`card ${b.available ? 'free' : 'busy'}`}
          >
            <div className="barista-header">
              <strong>{b.name}</strong>
              <span className={`status ${b.available ? 'free' : 'busy'}`}>
                {b.available ? 'Free' : 'Busy'}
              </span>
            </div>
            <div className="workload">
              Workload: {b.currentWorkloadMinutes.toFixed(1)} min | Ratio: {b.workloadRatio.toFixed(2)}x
            </div>
            {b.currentOrders && b.currentOrders.length > 0 && (
              <div className="current-orders">
                <strong>Current orders:</strong>
                {b.currentOrders.map((o) => (
                  <div key={o.id} className="order-item">
                    {o.customerName} - {o.drinkType} ({o.prepTimeMinutes} min)
                    {o.assignmentReason && (
                      <div className="reason">Why: {o.assignmentReason}</div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
