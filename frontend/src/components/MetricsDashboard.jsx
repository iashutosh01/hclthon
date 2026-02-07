import React from 'react';


export default function MetricsDashboard({ metrics }) {
  if (!metrics) {
    return (
      <div className="metrics-dashboard">
        <p>Loading metrics...</p>
      </div>
    );
  }

  return (
    <div className="metrics-dashboard">
      <h3>Metrics </h3>
      <div className="metrics-grid">
        <div className="metric-card">
          <span className="label">Avg Wait Time</span>
          <span className="value">{metrics.avgWaitTimeMinutes?.toFixed(1) ?? 0} min</span>
        </div>
        <div className="metric-card">
          <span className="label">Max Wait Time</span>
          <span className="value">{metrics.maxWaitTimeMinutes?.toFixed(1) ?? 0} min</span>
        </div>
        <div className="metric-card">
          <span className="label">Timeout Rate (≥10 min)</span>
          <span className="value">{(metrics.timeoutRate ?? 0) * 100}%</span>
        </div>
        <div className="metric-card">
          <span className="label">Fairness Violations</span>
          <span className="value">{metrics.fairnessViolations ?? 0}</span>
          <span className="hint">(&gt;3 later served first)</span>
        </div>
        <div className="metric-card">
          <span className="label">Total Completed</span>
          <span className="value">{metrics.totalOrdersCompleted ?? 0}</span>
        </div>
        <div className="metric-card">
          <span className="label">Queue Size</span>
          <span className="value">{metrics.queueSize ?? 0}</span>
        </div>
      </div>
      <p className="recorded">
        Last updated: {metrics.recordedAt
          ? new Date(metrics.recordedAt).toLocaleTimeString()
          : '-'}
      </p>
    </div>
  );
}
