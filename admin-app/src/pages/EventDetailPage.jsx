import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api.js';

export function EventDetailPage() {
  const { id } = useParams();
  const [view, setView] = useState(null);
  const [players, setPlayers] = useState([]);
  const [games, setGames] = useState([]);
  const [tab, setTab] = useState('players');
  const [showInvite, setShowInvite] = useState(false);
  const [inviteForm, setInviteForm] = useState({ name: '', email: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [deletingEvent, setDeletingEvent] = useState(false);
  const [deletingPlayerId, setDeletingPlayerId] = useState(null);

  useEffect(() => { loadData(); }, [id]);

  useEffect(() => {
    if (view?.status === 'active') {
      const interval = setInterval(loadData, 10000);
      return () => clearInterval(interval);
    }
  }, [view?.status]);

  async function loadData() {
    try {
      const [eventView, eventPlayers, eventGames] = await Promise.all([
        api.getEventAdminView(id),
        api.getEventPlayers(id),
        api.getEventGames(id)
      ]);
      setView(eventView);
      setPlayers(eventPlayers);
      setGames(eventGames);
    } catch (e) {
      setError(e.message);
    }
  }

  async function handleInvite(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    try {
      await api.inviteEventPlayer(id, inviteForm);
      setSuccess(`Invitation sent to ${inviteForm.email}`);
      setInviteForm({ name: '', email: '' });
      setShowInvite(false);
      loadData();
    } catch (e) {
      setError(e.message);
    }
  }

  async function handleDeleteEvent() {
    if (!(view.status === 'pending' || view.status === 'completed')) {
      setError('Only pending or completed events can be deleted.');
      return;
    }

    const confirmed = window.confirm(`Delete event "${view.eventName}"? This is a soft delete and cannot be accessed in normal views.`);
    if (!confirmed) return;

    setError('');
    setSuccess('');
    setDeletingEvent(true);
    try {
      await api.deleteEvent(id);
      window.location.href = '/events';
    } catch (e) {
      setError(e.message);
    } finally {
      setDeletingEvent(false);
    }
  }

  async function handleDeletePlayer(ep) {
    const playerName = ep.player?.name || 'this player';
    const confirmed = window.confirm(`Remove ${playerName} from this event? This is a soft delete.`);
    if (!confirmed) return;

    setError('');
    setSuccess('');
    setDeletingPlayerId(ep.id);
    try {
      await api.deleteEventPlayer(id, ep.id);
      setSuccess(`${playerName} was removed from the event.`);
      await loadData();
    } catch (e) {
      setError(e.message);
    } finally {
      setDeletingPlayerId(null);
    }
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
          <Link to="/events" style={{ fontSize: 13, color: 'var(--text-muted)', textDecoration: 'none' }}>← Events</Link>
          <h1 className="page-title">{view.eventName}</h1>
          <div className="card-meta" style={{ marginTop: 4 }}>
            <span className={`badge badge-${view.status}`}>{view.status}</span>
            <span>🗺️ {view.mapName}</span>
            <span>💰 £{parseFloat(view.startingBalance).toFixed(0)}</span>
            <span>📍 {view.proximityMetres}m</span>
            <span>👥 max {view.maxPlayersPerGame}/game</span>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          {(view.status === 'pending' || view.status === 'active') && (
            <button className="btn btn-primary" onClick={() => setShowInvite(true)}>+ Invite Player</button>
          )}
          {(view.status === 'pending' || view.status === 'completed') && (
            <button className="btn btn-danger" onClick={handleDeleteEvent} disabled={deletingEvent}>
              {deletingEvent ? 'Deleting...' : 'Delete Event'}
            </button>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 16, marginBottom: 24, fontSize: 13, color: 'var(--text-muted)' }}>
        <span>Start: {formatDate(view.startTime)}</span>
        <span>End: {formatDate(view.endTime)}</span>
      </div>

      {error && <div className="error-msg">{error}</div>}
      {success && <div style={{ color: 'var(--monopoly-green)', background: 'var(--monopoly-green-light)', padding: '10px 14px', borderRadius: 'var(--radius)', fontSize: 14, marginBottom: 16 }}>{success}</div>}

      <div className="tabs">
        <button className={`tab ${tab === 'players' ? 'active' : ''}`} onClick={() => setTab('players')}>Players ({players.length})</button>
        <button className={`tab ${tab === 'leaderboard' ? 'active' : ''}`} onClick={() => setTab('leaderboard')}>Leaderboard</button>
        <button className={`tab ${tab === 'games' ? 'active' : ''}`} onClick={() => setTab('games')}>Generated Games ({games.length})</button>
      </div>

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
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Assigned game</th>
                    <th>Joined</th>
                    {(view.status === 'pending' || view.status === 'active') && <th></th>}
                  </tr>
                </thead>
                <tbody>
                  {players.map(ep => (
                    <tr key={ep.id}>
                      <td style={{ fontWeight: 500 }}>{ep.player?.name || '—'}</td>
                      <td style={{ color: 'var(--text-muted)' }}>{ep.player?.email || '—'}</td>
                      <td>
                        {ep.assignedGameId ? (
                          <Link to={`/games/${ep.assignedGameId}`} style={{ textDecoration: 'none' }}>View game</Link>
                        ) : (
                          <span style={{ color: 'var(--text-dim)' }}>Pending assignment</span>
                        )}
                      </td>
                      <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                        {ep.joinedAt ? formatDate(ep.joinedAt) : '—'}
                      </td>
                      {(view.status === 'pending' || view.status === 'active') && (
                        <td>
                          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                            <button
                              className="btn btn-secondary btn-sm"
                              title="Clear the bound device so the player can rejoin from any device"
                              onClick={async () => {
                                try {
                                  await api.resetEventPlayerDevice(id, ep.id);
                                  setSuccess(`Device reset for ${ep.player?.name || 'player'}`);
                                } catch (e) { setError(e.message); }
                              }}
                            >
                              Reset device
                            </button>
                            <button
                              className="btn btn-danger btn-sm"
                              onClick={() => handleDeletePlayer(ep)}
                              disabled={deletingPlayerId === ep.id}
                            >
                              {deletingPlayerId === ep.id ? 'Removing...' : 'Remove'}
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {tab === 'games' && (
        <div>
          {games.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">🎮</div>
              <p>Games will be generated automatically when the event starts.</p>
            </div>
          ) : (
            <div className="card-grid">
              {games.map(game => (
                <div key={game.id} className="card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                    <h3 className="card-title" style={{ marginBottom: 0 }}>{game.name}</h3>
                    <span className={`badge badge-${game.status}`}>{game.status}</span>
                  </div>
                  <div className="card-meta" style={{ marginBottom: 12 }}>
                    <span>🕐 {formatDate(game.startTime)}</span>
                  </div>
                  <Link className="btn btn-secondary btn-sm" to={`/games/${game.id}`}>Open Game Dashboard</Link>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'leaderboard' && (
        <div className="card">
          {!view.leaderboard || view.leaderboard.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">🏆</div>
              <p>No players in this event yet.</p>
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
                    ? parseFloat(entry.finalBalance) : parseFloat(entry.balance || 0)).toFixed(0)}
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
              The player will be sent an email link to join this event.
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
