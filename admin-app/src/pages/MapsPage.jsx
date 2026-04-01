import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

export function MapsPage() {
  const [maps, setMaps] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newPostcode, setNewPostcode] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { loadMaps(); }, []);

  async function loadMaps() {
    try { setMaps(await api.getMaps()); } catch (e) { setError(e.message); }
  }

  async function handleCreate(e) {
    e.preventDefault();
    try {
      await api.createMap({ name: newName, postcodeArea: newPostcode.toUpperCase().trim() });
      setNewName('');
      setNewPostcode('');
      setShowCreate(false);
      loadMaps();
    } catch (e) { setError(e.message); }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this map and all its streets?')) return;
    try { await api.deleteMap(id); loadMaps(); } catch (e) { setError(e.message); }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Game Maps</h1>
          <p className="page-subtitle">Create maps with streets for your games</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ New Map</button>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {maps.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🗺️</div>
          <p>No maps yet. Create your first game map to get started.</p>
        </div>
      ) : (
        <div className="card-grid">
          {maps.map(m => (
            <div key={m.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                <Link to={`/maps/${m.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                  <h3 className="card-title">{m.name}</h3>
                </Link>
                <button className="btn btn-danger btn-sm btn-icon" onClick={() => handleDelete(m.id)}>✕</button>
              </div>
              <div className="card-meta">
                {m.postcodeArea && <span>📮 {m.postcodeArea}</span>}
                <span>📍 {m.streets?.length || 0} streets</span>
                {m.streets?.length > 0 && (
                  <span style={{ display: 'flex', gap: '3px' }}>
                    {[...new Set(m.streets.map(s => s.colour))].map(c => (
                      <span key={c} className="colour-dot" style={{ backgroundColor: `var(--${c})`, width: 10, height: 10 }} />
                    ))}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setShowCreate(false)}>
          <div className="modal">
            <h2 className="modal-title">New Game Map</h2>
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label className="form-label">Map Name</label>
                <input className="form-input" value={newName} onChange={e => setNewName(e.target.value)}
                  placeholder="e.g. Derby City Centre" required autoFocus />
              </div>
              <div className="form-group">
                <label className="form-label">Postcode Area</label>
                <input className="form-input" value={newPostcode} onChange={e => setNewPostcode(e.target.value)}
                  placeholder="e.g. DE22 or DE73" style={{ textTransform: 'uppercase' }} />
                <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
                  The UK postcode area this map is based in. Used to centre the map when adding streets.
                </p>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreate(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create Map</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
