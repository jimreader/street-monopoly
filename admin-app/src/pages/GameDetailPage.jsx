import React, { useState, useEffect, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { api } from '../api.js';

// Distinct colours for player pins — matched to table dots
const PLAYER_COLOURS = [
  '#D32F2F', '#0D47A1', '#E65100', '#7B1FA2',
  '#00838F', '#C62828', '#33691E', '#4527A0',
  '#BF360C', '#006064', '#1B5E20', '#AD1457'
];

function createPlayerIcon(colour, initial) {
  return L.divIcon({
    className: '',
    html: `<div style="
      width:32px;height:32px;border-radius:50%;
      background:${colour};border:3px solid #FFF;
      box-shadow:0 2px 8px rgba(0,0,0,0.3);
      display:flex;align-items:center;justify-content:center;
      font-family:-apple-system,sans-serif;font-weight:700;
      font-size:13px;color:#FFF;
    ">${initial}</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
    popupAnchor: [0, -18],
  });
}

function PlayerLocationMap({ playerLocations, allPlayers }) {
  // Build a colour index keyed by playerId matching table order
  const colourMap = useMemo(() => {
    const map = {};
    allPlayers.forEach((gp, idx) => {
      map[gp.playerId] = PLAYER_COLOURS[idx % PLAYER_COLOURS.length];
    });
    return map;
  }, [allPlayers]);

  // Calculate map center from player locations
  const center = useMemo(() => {
    if (playerLocations.length === 0) return [52.92, -1.47];
    const avgLat = playerLocations.reduce((s, l) => s + l.latitude, 0) / playerLocations.length;
    const avgLng = playerLocations.reduce((s, l) => s + l.longitude, 0) / playerLocations.length;
    return [avgLat, avgLng];
  }, [playerLocations]);

  return (
    <div style={{ borderRadius: 'var(--radius-lg)', overflow: 'hidden', border: '1.5px solid var(--border)', marginBottom: 16, boxShadow: 'var(--shadow-md)' }}>
      <MapContainer center={center} zoom={14} style={{ height: 300, width: '100%' }} scrollWheelZoom={true}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {playerLocations.map(loc => {
          const colour = colourMap[loc.playerId] || '#D32F2F';
          const initial = (loc.playerName || '?').charAt(0).toUpperCase();
          return (
            <Marker
              key={loc.playerId}
              position={[loc.latitude, loc.longitude]}
              icon={createPlayerIcon(colour, initial)}
            >
              <Popup>
                <strong>{loc.playerName}</strong><br />
                Last seen: {loc.streetName}<br />
                {loc.visitedAt && (
                  <span style={{ fontSize: 12, color: '#666' }}>
                    {new Date(loc.visitedAt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                )}
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
}

export function GameDetailPage() {
  const { id } = useParams();
  const [view, setView] = useState(null);
  const [players, setPlayers] = useState([]);
  const [tab, setTab] = useState('overview');
  const [showInvite, setShowInvite] = useState(false);
  const [inviteForm, setInviteForm] = useState({ name: '', email: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => { loadData(); }, [id]);

  useEffect(() => {
    // Auto-refresh for active games
    if (view?.status === 'active') {
      const interval = setInterval(loadData, 10000);
      return () => clearInterval(interval);
    }
  }, [view?.status]);

  async function loadData() {
    try {
      const [adminView, gamePlayers] = await Promise.all([
        api.getAdminView(id),
        api.getGamePlayers(id)
      ]);
      setView(adminView);
      setPlayers(gamePlayers);
    } catch (e) { setError(e.message); }
  }

  async function handleInvite(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await api.invitePlayer(id, inviteForm);
      setSuccess(`Invitation sent to ${inviteForm.email}`);
      setInviteForm({ name: '', email: '' });
      setShowInvite(false);
      loadData();
    } catch (e) { setError(e.message); }
  }

  function formatDate(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  if (!view) return <div className="empty-state">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <div>
          <Link to="/games" style={{ fontSize: 13, color: 'var(--text-muted)', textDecoration: 'none' }}>← Games</Link>
          <h1 className="page-title">{view.gameName}</h1>
          <div className="card-meta" style={{ marginTop: 4 }}>
            <span className={`badge badge-${view.status}`}>{view.status}</span>
            <span>🗺️ {view.mapName}</span>
            <span>💰 £{parseFloat(view.startingBalance).toFixed(0)}</span>
            <span>📍 {view.proximityMetres}m</span>
          </div>
        </div>
        {(view.status === 'pending' || view.status === 'active') && (
          <button className="btn btn-primary" onClick={() => setShowInvite(true)}>+ Invite Player</button>
        )}
      </div>

      <div style={{ display: 'flex', gap: 16, marginBottom: 24, fontSize: 13, color: 'var(--text-muted)' }}>
        <span>Start: {formatDate(view.startTime)}</span>
        <span>End: {formatDate(view.endTime)}</span>
      </div>

      {error && <div className="error-msg">{error}</div>}
      {success && <div style={{ color: 'var(--monopoly-green)', background: 'var(--monopoly-green-light)', padding: '10px 14px', borderRadius: 'var(--radius)', fontSize: 14, marginBottom: 16 }}>{success}</div>}

      <div className="tabs">
        <button className={`tab ${tab === 'overview' ? 'active' : ''}`} onClick={() => setTab('overview')}>Overview</button>
        <button className={`tab ${tab === 'streets' ? 'active' : ''}`} onClick={() => setTab('streets')}>Streets ({view.streets.length})</button>
        <button className={`tab ${tab === 'players' ? 'active' : ''}`} onClick={() => setTab('players')}>Players ({players.length})</button>
        <button className={`tab ${tab === 'leaderboard' ? 'active' : ''}`} onClick={() => setTab('leaderboard')}>Leaderboard</button>
      </div>

      {tab === 'overview' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
          <div className="card">
            <h3 className="card-title" style={{ fontSize: 18 }}>Leaderboard</h3>
            {view.leaderboard.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>No players yet</p>
            ) : (
              view.leaderboard.slice(0, 5).map(entry => (
                <div key={entry.playerId} className="leaderboard-row" style={{ padding: '10px 0' }}>
                  <span className={`leaderboard-rank ${entry.rank === 1 ? 'gold' : entry.rank === 2 ? 'silver' : entry.rank === 3 ? 'bronze' : ''}`}>
                    {entry.rank}
                  </span>
                  <span className="leaderboard-name">{entry.playerName}</span>
                  <span className="leaderboard-balance" style={{ color: 'var(--monopoly-green)' }}>
                    £{(view.status === 'completed' && entry.finalBalance != null ? entry.finalBalance : entry.balance).toFixed(0)}
                  </span>
                  <span className="leaderboard-streets">{entry.streetsOwned} owned</span>
                </div>
              ))
            )}
          </div>

          <div className="card">
            <h3 className="card-title" style={{ fontSize: 18 }}>Street Ownership</h3>
            {view.streets.filter(s => s.ownerName).length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>No streets purchased yet</p>
            ) : (
              view.streets.filter(s => s.ownerName).map(s => (
                <div key={s.streetId} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
                  <span className="colour-dot" style={{ backgroundColor: `var(--${s.colour})` }} />
                  <span style={{ flex: 1, fontSize: 14 }}>{s.name}</span>
                  <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>{s.ownerName}</span>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {tab === 'streets' && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Colour</th>
                <th>Street</th>
                <th>Price</th>
                <th>Rent</th>
                <th>Owner</th>
                <th>Visitors</th>
              </tr>
            </thead>
            <tbody>
              {view.streets.map(s => (
                <tr key={s.streetId}>
                  <td><span className="colour-dot" style={{ backgroundColor: `var(--${s.colour})` }} /></td>
                  <td style={{ fontWeight: 500 }}>{s.name}</td>
                  <td>£{parseFloat(s.price).toFixed(0)}</td>
                  <td>£{parseFloat(s.rentalPrice).toFixed(0)}</td>
                  <td style={{ color: s.ownerName ? 'var(--monopoly-green)' : 'var(--text-dim)' }}>
                    {s.ownerName || 'Available'}
                  </td>
                  <td>
                    {(!s.visitors || s.visitors.length === 0) ? (
                      <span style={{ color: 'var(--text-dim)', fontSize: 12 }}>—</span>
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                        {s.visitors.map((v, i) => (
                          <span key={i} style={{ fontSize: 12, display: 'inline-flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
                            <span style={{
                              width: 7, height: 7, borderRadius: '50%', flexShrink: 0,
                              background: v.visitType === 'purchased' ? 'var(--monopoly-green)'
                                : v.visitType === 'rent_paid' ? 'var(--monopoly-red)'
                                : 'var(--monopoly-gold)'
                            }} />
                            <span style={{ color: 'var(--text-muted)' }}>{v.playerName}</span>
                            <span style={{ color: 'var(--text-dim)', fontSize: 11 }}>
                              {v.visitType === 'purchased' ? '(bought)' : v.visitType === 'rent_paid' ? '(rent)' : '(no funds)'}
                            </span>
                            {v.visitedAt && (
                              <span style={{ color: 'var(--text-dim)', fontSize: 10 }}>
                                {new Date(v.visitedAt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}
                              </span>
                            )}
                          </span>
                        ))}
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'players' && (
        <div>
          {(view.status === 'pending' || view.status === 'active') && (
            <div style={{ marginBottom: 16 }}>
              <button className="btn btn-primary btn-sm" onClick={() => setShowInvite(true)}>+ Invite Player</button>
            </div>
          )}
          {players.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">👥</div>
              <p>No players invited yet.</p>
            </div>
          ) : (
            <>
              {/* Player location map */}
              {view.playerLocations && view.playerLocations.length > 0 && (
                <PlayerLocationMap
                  playerLocations={view.playerLocations}
                  allPlayers={players}
                />
              )}

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th></th>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Balance</th>
                      <th>Last seen</th>
                    </tr>
                  </thead>
                  <tbody>
                    {players.map((gp, idx) => {
                      const colour = PLAYER_COLOURS[idx % PLAYER_COLOURS.length];
                      const lastLoc = view.playerLocations?.find(l => l.playerId === gp.playerId);
                      return (
                        <tr key={gp.id}>
                          <td>
                            <span style={{
                              width: 12, height: 12, borderRadius: '50%', display: 'inline-block',
                              background: colour, border: '2px solid rgba(0,0,0,0.1)'
                            }} />
                          </td>
                          <td style={{ fontWeight: 500 }}>{gp.player?.name || '—'}</td>
                          <td style={{ color: 'var(--text-muted)' }}>{gp.player?.email || '—'}</td>
                          <td>£{parseFloat(gp.balance).toFixed(0)}</td>
                          <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                            {lastLoc ? (
                              <>
                                {lastLoc.streetName}
                                {lastLoc.visitedAt && (
                                  <span style={{ color: 'var(--text-dim)', marginLeft: 4 }}>
                                    {new Date(lastLoc.visitedAt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}
                                  </span>
                                )}
                              </>
                            ) : (
                              <span style={{ color: 'var(--text-dim)' }}>Not checked in</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}

      {tab === 'leaderboard' && (
        <div className="card">
          {view.leaderboard.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">🏆</div>
              <p>No players in this game yet.</p>
            </div>
          ) : (
            view.leaderboard.map(entry => (
              <div key={entry.playerId} className="leaderboard-row">
                <span className={`leaderboard-rank ${entry.rank === 1 ? 'gold' : entry.rank === 2 ? 'silver' : entry.rank === 3 ? 'bronze' : ''}`}>
                  {entry.rank}
                </span>
                <span className="leaderboard-name">{entry.playerName}</span>
                <span className="leaderboard-streets">{entry.streetsOwned} streets</span>
                <span className="leaderboard-balance" style={{
                  color: (view.status === 'completed' && entry.finalBalance != null ? entry.finalBalance : entry.balance) >= 0
                    ? 'var(--monopoly-green)' : 'var(--danger)'
                }}>
                  £{(view.status === 'completed' && entry.finalBalance != null
                    ? parseFloat(entry.finalBalance) : parseFloat(entry.balance)).toFixed(0)}
                  {view.status === 'completed' && entry.finalBalance != null && (
                    <span style={{ fontSize: 11, color: 'var(--text-dim)', marginLeft: 6 }}>final</span>
                  )}
                </span>
              </div>
            ))
          )}
        </div>
      )}

      {showInvite && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowInvite(false)}>
          <div className="modal">
            <h2 className="modal-title">Invite Player</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: 14, marginBottom: 20 }}>
              The player will be sent an email with a link to join the game directly.
            </p>
            <form onSubmit={handleInvite}>
              <div className="form-group">
                <label className="form-label">Player Name</label>
                <input className="form-input" value={inviteForm.name}
                  onChange={e => setInviteForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="e.g. Alice" required autoFocus />
              </div>
              <div className="form-group">
                <label className="form-label">Email Address</label>
                <input className="form-input" type="email" value={inviteForm.email}
                  onChange={e => setInviteForm(f => ({ ...f, email: e.target.value }))}
                  placeholder="alice@example.com" required />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowInvite(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Add Player</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
