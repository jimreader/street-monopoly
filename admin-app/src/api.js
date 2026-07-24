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

  // Events
  getEvents: () => request('/events'),
  getEvent: (id) => request(`/events/${id}`),
  createEvent: (data) => request('/events', { method: 'POST', body: JSON.stringify(data) }),
  updateEvent: (id, data) => request(`/events/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  inviteEventPlayer: (eventId, data) => request(`/events/${eventId}/invite`, { method: 'POST', body: JSON.stringify(data) }),
  getEventPlayers: (eventId) => request(`/events/${eventId}/players`),
  getEventGames: (eventId) => request(`/events/${eventId}/games`),
  getEventAdminView: (eventId) => request(`/events/${eventId}/admin-view`),
  getEventChallenges: (eventId) => request(`/events/${eventId}/challenges`),
  createEventChallenge: (eventId, data) => request(`/events/${eventId}/challenges`, { method: 'POST', body: JSON.stringify(data) }),
  updateEventChallenge: (eventId, challengeId, data) => request(`/events/${eventId}/challenges/${challengeId}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteEventChallenge: (eventId, challengeId) => request(`/events/${eventId}/challenges/${challengeId}`, { method: 'DELETE' }),
  getEventChallengeSubmissions: (eventId, challengeId) => request(`/events/${eventId}/challenges/${challengeId}/submissions`),
  reviewEventChallengeSubmission: (eventId, challengeId, submissionId, data) =>
    request(`/events/${eventId}/challenges/${challengeId}/submissions/${submissionId}/review`, { method: 'POST', body: JSON.stringify(data) }),
  deleteEvent: (eventId) => request(`/events/${eventId}`, { method: 'DELETE' }),
  deleteEventPlayer: (eventId, eventPlayerId) => request(`/events/${eventId}/players/${eventPlayerId}`, { method: 'DELETE' }),
  resetEventPlayerDevice: (eventId, eventPlayerId) =>
    request(`/events/${eventId}/players/${eventPlayerId}/reset-device`, { method: 'POST' }),

  // Games (read/inspect generated games)
  getGame: (id) => request(`/games/${id}`),
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
