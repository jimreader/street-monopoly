const API_BASE = '/api/player';

async function request(url, options = {}) {
  const res = await fetch(`${API_BASE}${url}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.error || 'Request failed');
  }
  return res.json();
}

export const api = {
  getGameView: (joinToken) => request(`/game/${joinToken}`),
  checkIn: (joinToken, data) => request(`/game/${joinToken}/checkin`, {
    method: 'POST',
    body: JSON.stringify(data)
  }),
};
