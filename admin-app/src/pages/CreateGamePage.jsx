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
    proximityMetres: '50'
  });

  useEffect(() => {
    api.getMaps().then(setMaps).catch(e => setError(e.message));
  }, []);

  function setField(key, value) { setForm(f => ({ ...f, [key]: value })); }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const game = await api.createGame({
        name: form.name,
        gameMapId: form.gameMapId,
        startTime: new Date(form.startTime).toISOString(),
        endTime: new Date(form.endTime).toISOString(),
        startingBalance: parseFloat(form.startingBalance),
        proximityMetres: parseInt(form.proximityMetres)
      });
      navigate(`/games/${game.id}`);
    } catch (e) { setError(e.message); }
  }

  const selectedMap = maps.find(m => m.id === form.gameMapId);

  return (
    <div>
      <div className="page-header">
        <div>
          <Link to="/games" style={{ fontSize: 13, color: 'var(--text-muted)', textDecoration: 'none' }}>← Games</Link>
          <h1 className="page-title">Create Game</h1>
        </div>
      </div>

      {error && <div className="error-msg">{error}</div>}

      <div className="card" style={{ maxWidth: 640 }}>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Game Name</label>
            <input className="form-input" value={form.name} onChange={e => setField('name', e.target.value)}
              placeholder="e.g. Saturday Street Dash" required autoFocus />
          </div>

          <div className="form-group">
            <label className="form-label">Game Map</label>
            <select className="form-select" value={form.gameMapId} onChange={e => setField('gameMapId', e.target.value)} required>
              <option value="">Select a map...</option>
              {maps.map(m => (
                <option key={m.id} value={m.id}>{m.name} ({m.streets?.length || 0} streets)</option>
              ))}
            </select>
            {maps.length === 0 && (
              <p style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 6 }}>
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
              <label className="form-label">Starting Balance (£)</label>
              <input className="form-input" type="number" min="1" value={form.startingBalance}
                onChange={e => setField('startingBalance', e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">Proximity (metres)</label>
              <input className="form-input" type="number" min="1" max="1000" value={form.proximityMetres}
                onChange={e => setField('proximityMetres', e.target.value)} required />
              <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
                How close a player must be to a street's GPS location to check in
              </p>
            </div>
          </div>

          {selectedMap && selectedMap.streets?.length > 0 && (
            <div style={{ marginTop: 16, padding: 16, background: 'var(--bg)', borderRadius: 'var(--radius)', border: '1px solid var(--border)' }}>
              <p style={{ fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1, color: 'var(--text-muted)', marginBottom: 8 }}>
                Map Preview — {selectedMap.streets.length} streets
              </p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {selectedMap.streets.map(s => (
                  <span key={s.id} style={{
                    display: 'inline-flex', alignItems: 'center', gap: 5,
                    padding: '4px 10px', borderRadius: 20, fontSize: 12,
                    background: 'var(--surface)', border: '1px solid var(--border)'
                  }}>
                    <span className="colour-dot" style={{ backgroundColor: `var(--${s.colour})`, width: 8, height: 8 }} />
                    {s.name}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="modal-actions" style={{ marginTop: 28 }}>
            <Link to="/games" className="btn btn-secondary">Cancel</Link>
            <button type="submit" className="btn btn-primary">Create Game</button>
          </div>
        </form>
      </div>
    </div>
  );
}
