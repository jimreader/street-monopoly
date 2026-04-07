const API_BASE = '/api/player';

/**
 * Generate or retrieve a persistent device token.
 * Uses sessionStorage so it survives page refreshes within the same browser tab/session,
 * but a different device or incognito window will get a different token.
 */
function getDeviceToken() {
  const STORAGE_KEY = 'road-rush-device-token';
  let token = sessionStorage.getItem(STORAGE_KEY);
  if (!token) {
    token = crypto.randomUUID ? crypto.randomUUID() : (
      'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0;
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
      })
    );
    sessionStorage.setItem(STORAGE_KEY, token);
  }
  return token;
}

async function request(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    'X-Device-Token': getDeviceToken(),
    ...options.headers,
  };

  const res = await fetch(`${API_BASE}${url}`, { ...options, headers });

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
