
const BASE = import.meta.env.DEV ? '/api' : 'http://localhost:8080';

export async function getQueue() {
  const res = await fetch(`${BASE}/queue`);
  if (!res.ok) throw new Error('Failed to fetch queue');
  return res.json();
}

export async function getBaristas() {
  const res = await fetch(`${BASE}/baristas`);
  if (!res.ok) throw new Error('Failed to fetch baristas');
  return res.json();
}

export async function getMetrics() {
  const res = await fetch(`${BASE}/metrics`);
  if (!res.ok) throw new Error('Failed to fetch metrics');
  return res.json();
}

export async function startSimulation() {
  const res = await fetch(`${BASE}/simulate/start`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to start simulation');
  return res.json();
}

export async function stopSimulation() {
  const res = await fetch(`${BASE}/simulate/stop`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to stop simulation');
  return res.json();
}

export async function startTestSimulation() {
  const res = await fetch(`${BASE}/simulate/test`, { method: 'POST' });
  if (!res.ok) throw new Error('Failed to run test simulation');
  return res.json();
}

export async function getSimulationStatus() {
  const res = await fetch(`${BASE}/simulate/status`);
  if (!res.ok) throw new Error('Failed to fetch status');
  return res.json();
}

export async function getAllOrders() {
  const res = await fetch(`${BASE}/orders/all`);
  if (!res.ok) throw new Error('Failed to fetch all orders');
  return res.json();
}

export async function createOrder({ customerName, drinkType, loyaltyStatus }) {
  const res = await fetch(`${BASE}/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ customerName, drinkType, loyaltyStatus }),
  });
  if (!res.ok) throw new Error('Failed to create order');
  return res.json();
}
