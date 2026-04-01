const API_BASE = '/api';

let _getAccessToken = null;

/**
 * Called once from the React app to provide the Auth0 getAccessTokenSilently function.
 * This lets the plain-JS api module obtain tokens without being a React component.
 */
export function setTokenProvider(getAccessTokenSilently) {
  _getAccessToken = getAccessTokenSilently;
}

async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };

  // Attach Auth0 bearer token
  if (_getAccessToken) {
    try {
      const token = await _getAccessToken();
      headers['Authorization'] = `Bearer ${token}`;
    } catch (err) {
      console.error('Failed to get access token:', err);
      throw new Error('Authentication required. Please log in again.');
    }
  }

  const res = await fetch(`${API_BASE}${url}`, { ...options, headers });

  if (res.status === 401) {
    throw new Error('Unauthorized — your session may have expired. Please log in again.');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.error || 'Request failed');
  }

  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  // Maps
  getMaps: () => request('/maps'),
  getMap: (id) => request(`/maps/${id}`),
  createMap: (data) => request('/maps', { method: 'POST', body: JSON.stringify(data) }),
  updateMap: (id, data) => request(`/maps/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteMap: (id) => request(`/maps/${id}`, { method: 'DELETE' }),

  // Streets
  addStreet: (mapId, data) => request(`/maps/${mapId}/streets`, { method: 'POST', body: JSON.stringify(data) }),
  updateStreet: (streetId, data) => request(`/maps/streets/${streetId}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteStreet: (streetId) => request(`/maps/streets/${streetId}`, { method: 'DELETE' }),

  // Games
  getGames: () => request('/games'),
  getGame: (id) => request(`/games/${id}`),
  createGame: (data) => request('/games', { method: 'POST', body: JSON.stringify(data) }),
  invitePlayer: (gameId, data) => request(`/games/${gameId}/invite`, { method: 'POST', body: JSON.stringify(data) }),
  getGamePlayers: (gameId) => request(`/games/${gameId}/players`),
  getAdminView: (gameId) => request(`/games/${gameId}/admin-view`),

  // Images
  uploadImage: async (file) => {
    const headers = {};
    if (_getAccessToken) {
      try {
        const token = await _getAccessToken();
        headers['Authorization'] = `Bearer ${token}`;
      } catch (err) {
        throw new Error('Authentication required.');
      }
    }
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${API_BASE}/images/upload`, {
      method: 'POST',
      headers, // no Content-Type — browser sets multipart boundary
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: res.statusText }));
      throw new Error(err.error || 'Upload failed');
    }
    return res.json();
  },
};
