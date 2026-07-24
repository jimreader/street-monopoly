import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api } from '../api.js';

export function CreateGamePage() {
  const navigate = useNavigate();
  const [maps, setMaps] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    name: '',
    gameMapId: '',
    startTime: '',
    endTime: '',
    startingBalance: '1500',
    proximityMetres: '50',
    maxPlayersPerGame: '8'
  });

  useEffect(() => {
    api.getMaps().then(setMaps).catch(e => setError(e.message));
  }, []);

  function setField(key, value) { setForm(f => ({ ...f, [key]: value })); }

  function toLocalDateTimePayload(value) {
    if (!value) return value;
    return value.length === 16 ? `${value}:00` : value;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const event = await api.createEvent({
        name: form.name,
        gameMapId: form.gameMapId,
        startTime: toLocalDateTimePayload(form.startTime),
        endTime: toLocalDateTimePayload(form.endTime),
        startingBalance: parseFloat(form.startingBalance),
        proximityMetres: parseInt(form.proximityMetres, 10),
        maxPlayersPerGame: parseInt(form.maxPlayersPerGame, 10)
      });
      navigate(`/events/${event.id}`);
    } catch (e) { setError(e.message); }
  }

  const selectedMap = maps.find(m => m.id === form.gameMapId);

  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <Link to="/events" className="muted">← Events</Link>
          <div className="section-eyebrow" style={{ marginTop: 8 }}>Administration</div>
          <h1 className="page-title">Create Event</h1>
        </div>
      </div>

      {error && <div className="error-msg">{error}</div>}

      <div className="card" style={{ maxWidth: 760 }}>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Event Name</label>
            <input className="form-input" value={form.name} onChange={e => setField('name', e.target.value)}
              placeholder="e.g. Saturday Street Dash" required autoFocus />
          </div>

          <div className="form-group">
            <label className="form-label">Event Map</label>
            <select className="form-select" value={form.gameMapId} onChange={e => setField('gameMapId', e.target.value)} required>
              <option value="">Select a map...</option>
              {maps.map(m => (
                <option key={m.id} value={m.id}>{m.name} ({m.streets?.length || 0} streets)</option>
              ))}
            </select>
            {maps.length === 0 && (
              <p className="form-help">
                No maps found. <Link to="/maps">Create a map first</Link>.
              </p>
            )}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Start Time</label>
              <input className="form-input" type="datetime-local" value={form.startTime}
                onChange={e => setField('startTime', e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">End Time</label>
              <input className="form-input" type="datetime-local" value={form.endTime}
                onChange={e => setField('endTime', e.target.value)} required />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Starting Budget (£)</label>
              <input className="form-input" type="number" min="1" value={form.startingBalance}
                onChange={e => setField('startingBalance', e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">GPS Proximity (metres)</label>
              <input className="form-input" type="number" min="1" max="1000" value={form.proximityMetres}
                onChange={e => setField('proximityMetres', e.target.value)} required />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Max Players Per Game</label>
            <input className="form-input" type="number" min="1" max="200" value={form.maxPlayersPerGame}
              onChange={e => setField('maxPlayersPerGame', e.target.value)} required />
            <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
              Players are invited to the event first. At start time, they are split across as many games as needed to keep each game at or below this limit.
            </p>
          </div>

          {selectedMap && selectedMap.streets?.length > 0 && (
            <div className="preview-panel">
              <p className="section-eyebrow" style={{ marginBottom: 8 }}>
                Map Preview - {selectedMap.streets.length} streets
              </p>
              <div className="chip-list">
                {selectedMap.streets.map(s => (
                  <span key={s.id} className="chip">
                    <span className="colour-dot" style={{ backgroundColor: `var(--${s.colour})`, width: 8, height: 8 }} />
                    {s.name}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="modal-actions" style={{ marginTop: 28 }}>
            <Link to="/events" className="btn btn-secondary">Cancel</Link>
            <button type="submit" className="btn btn-primary">Create Event</button>
          </div>
        </form>
      </div>
    </div>
  );
}
