import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api.js';

export function EventDetailPage() {
  const { id } = useParams();
  const [view, setView] = useState(null);
  const [players, setPlayers] = useState([]);
  const [games, setGames] = useState([]);
  const [challenges, setChallenges] = useState([]);
  const [tab, setTab] = useState('players');
  const [showInvite, setShowInvite] = useState(false);
  const [inviteForm, setInviteForm] = useState({ name: '', email: '' });
  const [showChallengeModal, setShowChallengeModal] = useState(false);
  const [challengeForm, setChallengeForm] = useState({ description: '', prizeAmount: '', durationMinutes: '' });
  const [editingChallenge, setEditingChallenge] = useState(null);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewChallenge, setReviewChallenge] = useState(null);
  const [challengeSubmissions, setChallengeSubmissions] = useState([]);
  const [reviewingSubmissionId, setReviewingSubmissionId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [deletingEvent, setDeletingEvent] = useState(false);
  const [deletingPlayerId, setDeletingPlayerId] = useState(null);
  const [maps, setMaps] = useState([]);
  const [showEditEvent, setShowEditEvent] = useState(false);
  const [savingEvent, setSavingEvent] = useState(false);
  const [eventLogoUploading, setEventLogoUploading] = useState(false);
  const [editEventForm, setEditEventForm] = useState({
    name: '',
    logoImageUrl: '',
    gameMapId: '',
    startTime: '',
    endTime: '',
    startingBalance: '',
    proximityMetres: '',
    maxPlayersPerGame: ''
  });

  useEffect(() => { loadData(); }, [id]);
  useEffect(() => {
    api.getMaps().then(setMaps).catch(e => setError(e.message));
  }, []);

  useEffect(() => {
    const hasPendingInviteEmail = players.some(p => p.inviteEmailStatus === 'pending');
    if (view?.status === 'active' || hasPendingInviteEmail) {
      const interval = setInterval(loadData, hasPendingInviteEmail ? 4000 : 10000);
      return () => clearInterval(interval);
    }
  }, [view?.status, players]);

  async function loadData() {
    try {
      const [eventView, eventPlayers, eventGames, eventChallenges] = await Promise.all([
        api.getEventAdminView(id),
        api.getEventPlayers(id),
        api.getEventGames(id),
        api.getEventChallenges(id)
      ]);
      setView(eventView);
      setPlayers(eventPlayers);
      setGames(eventGames);
      setChallenges(eventChallenges);
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

  async function handleSaveChallenge(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    const payload = {
      description: challengeForm.description.trim(),
      prizeAmount: Number(challengeForm.prizeAmount),
      durationMinutes: Number(challengeForm.durationMinutes)
    };

    try {
      if (editingChallenge) {
        await api.updateEventChallenge(id, editingChallenge.id, payload);
        setSuccess('Challenge updated.');
      } else {
        await api.createEventChallenge(id, payload);
        setSuccess('Challenge added.');
      }
      setShowChallengeModal(false);
      setEditingChallenge(null);
      setChallengeForm({ description: '', prizeAmount: '', durationMinutes: '' });
      await loadData();
    } catch (e) {
      setError(e.message);
    }
  }

  function openNewChallengeModal() {
    setEditingChallenge(null);
    setChallengeForm({ description: '', prizeAmount: '', durationMinutes: '' });
    setShowChallengeModal(true);
  }

  function openEditChallengeModal(challenge) {
    setEditingChallenge(challenge);
    setChallengeForm({
      description: challenge.description,
      prizeAmount: String(challenge.prizeAmount),
      durationMinutes: String(challenge.durationMinutes)
    });
    setShowChallengeModal(true);
  }

  async function handleDeleteChallenge(challenge) {
    const confirmed = window.confirm('Delete this challenge?');
    if (!confirmed) return;

    setError('');
    setSuccess('');
    try {
      await api.deleteEventChallenge(id, challenge.id);
      setSuccess('Challenge deleted.');
      await loadData();
    } catch (e) {
      setError(e.message);
    }
  }

  async function openReviewModal(challenge) {
    setError('');
    setSuccess('');
    try {
      const submissions = await api.getEventChallengeSubmissions(id, challenge.id);
      setReviewChallenge(challenge);
      setChallengeSubmissions(submissions);
      setShowReviewModal(true);
    } catch (e) {
      setError(e.message);
    }
  }

  async function handleReviewSubmission(submission, reviewStatus) {
    setError('');
    setSuccess('');
    setReviewingSubmissionId(submission.submissionId);
    try {
      await api.reviewEventChallengeSubmission(id, reviewChallenge.id, submission.submissionId, {
        reviewStatus,
        reviewNotes: ''
      });
      const submissions = await api.getEventChallengeSubmissions(id, reviewChallenge.id);
      setChallengeSubmissions(submissions);
      setSuccess(`Submission marked as ${reviewStatus}.`);
      await loadData();
    } catch (e) {
      setError(e.message);
    } finally {
      setReviewingSubmissionId(null);
    }
  }

  function formatDate(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  function renderInviteEmailStatus(status) {
    if (status === 'sent') {
      return <span className="status-pill status-pill--success">Sent</span>;
    }
    if (status === 'failed') {
      return <span className="status-pill status-pill--danger">Failed</span>;
    }
    return <span className="status-pill status-pill--warning">Sending…</span>;
  }

  function toInputDateTime(iso) {
    if (!iso) return '';
    const normalized = String(iso).trim().replace(' ', 'T');
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(normalized)) {
      return normalized.slice(0, 16);
    }
    const d = new Date(normalized);
    if (Number.isNaN(d.getTime())) return '';
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function toLocalDateTimePayload(value) {
    if (!value) return value;
    return value.length === 16 ? `${value}:00` : value;
  }

  function openEditEventModal() {
    api.getEvent(id).then((event) => {
      setEditEventForm({
        name: event.name || '',
        logoImageUrl: event.logoImageUrl || '',
        gameMapId: event.gameMapId || '',
        startTime: toInputDateTime(event.startTime),
        endTime: toInputDateTime(event.endTime),
        startingBalance: String(event.startingBalance ?? ''),
        proximityMetres: String(event.proximityMetres ?? ''),
        maxPlayersPerGame: String(event.maxPlayersPerGame ?? '')
      });
      setShowEditEvent(true);
    }).catch(e => setError(e.message));
  }

  async function handleEventLogoSelected(file) {
    if (!file) return;
    setEventLogoUploading(true);
    setError('');
    try {
      const uploaded = await api.uploadImage(file);
      setEditEventForm(f => ({ ...f, logoImageUrl: uploaded.url }));
    } catch (e) {
      setError(e.message);
    } finally {
      setEventLogoUploading(false);
    }
  }

  async function handleSaveEvent(e) {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSavingEvent(true);
    try {
      await api.updateEvent(id, {
        name: editEventForm.name,
        logoImageUrl: editEventForm.logoImageUrl || null,
        gameMapId: editEventForm.gameMapId,
        startTime: toLocalDateTimePayload(editEventForm.startTime),
        endTime: toLocalDateTimePayload(editEventForm.endTime),
        startingBalance: Number(editEventForm.startingBalance),
        proximityMetres: Number(editEventForm.proximityMetres),
        maxPlayersPerGame: Number(editEventForm.maxPlayersPerGame),
      });
      setShowEditEvent(false);
      setSuccess('Event updated.');
      await loadData();
    } catch (e) {
      setError(e.message);
    } finally {
      setSavingEvent(false);
    }
  }

  if (!view) return <div className="empty-state-card">Loading...</div>;

  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <Link to="/events" className="muted">← Events</Link>
          <div className="section-eyebrow" style={{ marginTop: 8 }}>Events</div>
          <h1 className="page-title">{view.eventName}</h1>
          <div className="card-meta" style={{ marginTop: 6 }}>
            <span className={`badge badge-${view.status}`}>{view.status}</span>
            <span>🗺️ {view.mapName}</span>
            <span>💰 £{parseFloat(view.startingBalance).toFixed(0)}</span>
            <span>📍 {view.proximityMetres}m</span>
            <span>👥 max {view.maxPlayersPerGame}/game</span>
          </div>
        </div>
        <div className="page-actions">
          {view.status === 'pending' && (
            <button className="btn btn-secondary" onClick={openEditEventModal}>Edit Event</button>
          )}
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

      <div className="card-meta" style={{ marginBottom: 24 }}>
        <span>Start: {formatDate(view.startTime)}</span>
        <span>End: {formatDate(view.endTime)}</span>
      </div>

      {error && <div className="error-msg">{error}</div>}
      {success && <div className="success-msg">{success}</div>}

      <div className="tabs">
        <button className={`tab ${tab === 'players' ? 'active' : ''}`} onClick={() => setTab('players')}>Players ({players.length})</button>
        <button className={`tab ${tab === 'challenges' ? 'active' : ''}`} onClick={() => setTab('challenges')}>Challenges ({challenges.length})</button>
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
            <div className="empty-state-card">
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
                    <th>Invite email</th>
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
                      <td>{renderInviteEmailStatus(ep.inviteEmailStatus)}</td>
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

      {tab === 'challenges' && (
        <div>
          {view.status === 'pending' && (
            <div style={{ marginBottom: 16 }}>
              <button className="btn btn-primary btn-sm" onClick={openNewChallengeModal}>+ Add Challenge</button>
            </div>
          )}

          {challenges.length === 0 ? (
            <div className="empty-state-card">
              <div className="empty-state-icon">📸</div>
              <p>No challenges configured yet.</p>
            </div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Challenge</th>
                    <th>Prize</th>
                    <th>Duration</th>
                    <th>Status</th>
                    <th>Schedule</th>
                    <th>Submissions</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {challenges.map(ch => (
                    <tr key={ch.id}>
                      <td style={{ maxWidth: 360 }}>{ch.description}</td>
                      <td>£{parseFloat(ch.prizeAmount || 0).toFixed(0)}</td>
                      <td>{ch.durationMinutes}m</td>
                      <td><span className={`badge badge-${ch.status}`}>{ch.status}</span></td>
                      <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                        {ch.scheduledStartAt ? `${formatDate(ch.scheduledStartAt)} - ${formatDate(ch.scheduledEndAt)}` : 'Randomized on event start'}
                      </td>
                      <td style={{ fontSize: 12 }}>
                        {ch.submittedCount} total · {ch.pendingReviewCount} pending review · {ch.accomplishedCount} accomplished
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                          <button className="btn btn-secondary btn-sm" onClick={() => openReviewModal(ch)}>
                            Review
                          </button>
                          {view.status === 'pending' && ch.status === 'pending' && (
                            <>
                              <button className="btn btn-secondary btn-sm" onClick={() => openEditChallengeModal(ch)}>
                                Edit
                              </button>
                              <button className="btn btn-danger btn-sm" onClick={() => handleDeleteChallenge(ch)}>
                                Delete
                              </button>
                            </>
                          )}
                        </div>
                      </td>
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
            <div className="empty-state-card">
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
                  <Link className="btn btn-primary btn-sm" to={`/games/${game.id}`}>Open Game Dashboard</Link>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'leaderboard' && (
        <div className="card">
          {!view.leaderboard || view.leaderboard.length === 0 ? (
            <div className="empty-state-card">
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
            <div className="modal-header">
              <div>
                <div className="modal-kicker">Invitation</div>
                <h2 className="modal-title">Invite Player</h2>
                <p className="modal-lead">The player will be sent an email link to join this event.</p>
              </div>
            </div>
            <form className="modal-body" onSubmit={handleInvite}>
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

      {showEditEvent && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowEditEvent(false)}>
          <div className="modal">
            <div className="modal-header">
              <div>
                <div className="modal-kicker">Events</div>
                <h2 className="modal-title">Edit Event</h2>
                <p className="modal-lead">Update event details before it starts.</p>
              </div>
            </div>
            <form className="modal-body" onSubmit={handleSaveEvent}>
              <div className="form-group">
                <label className="form-label">Event Name</label>
                <input className="form-input" value={editEventForm.name}
                  onChange={e => setEditEventForm(f => ({ ...f, name: e.target.value }))}
                  required autoFocus />
              </div>

              <div className="form-group">
                <label className="form-label">Event Logo (optional)</label>
                <div className="split" style={{ gap: 8 }}>
                  <label className="btn btn-secondary btn-sm" style={{ cursor: eventLogoUploading ? 'wait' : 'pointer' }}>
                    {eventLogoUploading ? 'Uploading...' : 'Upload Logo'}
                    <input
                      type="file"
                      accept="image/*"
                      style={{ display: 'none' }}
                      disabled={eventLogoUploading}
                      onChange={(e) => handleEventLogoSelected(e.target.files?.[0])}
                    />
                  </label>
                  {editEventForm.logoImageUrl && (
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditEventForm(f => ({ ...f, logoImageUrl: '' }))}>
                      Remove
                    </button>
                  )}
                </div>
                {editEventForm.logoImageUrl && (
                  <div className="preview-panel" style={{ marginTop: 10 }}>
                    <img src={editEventForm.logoImageUrl} alt="Event logo preview" style={{ maxHeight: 72, width: 'auto', objectFit: 'contain' }} />
                  </div>
                )}
              </div>

              <div className="form-group">
                <label className="form-label">Event Map</label>
                <select className="form-select" value={editEventForm.gameMapId}
                  onChange={e => setEditEventForm(f => ({ ...f, gameMapId: e.target.value }))} required>
                  <option value="">Select a map...</option>
                  {maps.map(m => (
                    <option key={m.id} value={m.id}>{m.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Start Time</label>
                  <input className="form-input" type="datetime-local" value={editEventForm.startTime}
                    onChange={e => setEditEventForm(f => ({ ...f, startTime: e.target.value }))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">End Time</label>
                  <input className="form-input" type="datetime-local" value={editEventForm.endTime}
                    onChange={e => setEditEventForm(f => ({ ...f, endTime: e.target.value }))} required />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Starting Budget (£)</label>
                  <input className="form-input" type="number" min="1" value={editEventForm.startingBalance}
                    onChange={e => setEditEventForm(f => ({ ...f, startingBalance: e.target.value }))} required />
                </div>
                <div className="form-group">
                  <label className="form-label">GPS Proximity (metres)</label>
                  <input className="form-input" type="number" min="1" max="1000" value={editEventForm.proximityMetres}
                    onChange={e => setEditEventForm(f => ({ ...f, proximityMetres: e.target.value }))} required />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Max Players Per Game</label>
                <input className="form-input" type="number" min="1" max="200" value={editEventForm.maxPlayersPerGame}
                  onChange={e => setEditEventForm(f => ({ ...f, maxPlayersPerGame: e.target.value }))} required />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowEditEvent(false)} disabled={savingEvent}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={savingEvent || eventLogoUploading}>
                  {savingEvent ? 'Saving...' : 'Save Event'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showChallengeModal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowChallengeModal(false)}>
          <div className="modal">
            <div className="modal-header">
              <div>
                <div className="modal-kicker">Challenges</div>
                <h2 className="modal-title">{editingChallenge ? 'Edit Challenge' : 'Add Challenge'}</h2>
                <p className="modal-lead">Challenge timings are randomized automatically when the event starts.</p>
              </div>
            </div>
            <form className="modal-body" onSubmit={handleSaveChallenge}>
              <div className="form-group">
                <label className="form-label">Challenge</label>
                <input
                  className="form-input"
                  value={challengeForm.description}
                  onChange={e => setChallengeForm(f => ({ ...f, description: e.target.value }))}
                  placeholder="e.g. Send a photo where your feet are off the ground"
                  required
                  autoFocus
                />
              </div>
              <div className="form-group">
                <label className="form-label">Prize Amount (£)</label>
                <input
                  className="form-input"
                  type="number"
                  min="1"
                  step="1"
                  value={challengeForm.prizeAmount}
                  onChange={e => setChallengeForm(f => ({ ...f, prizeAmount: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Duration (minutes)</label>
                <input
                  className="form-input"
                  type="number"
                  min="1"
                  step="1"
                  value={challengeForm.durationMinutes}
                  onChange={e => setChallengeForm(f => ({ ...f, durationMinutes: e.target.value }))}
                  required
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowChallengeModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editingChallenge ? 'Save Changes' : 'Add Challenge'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showReviewModal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setShowReviewModal(false)}>
          <div className="modal modal--wide" role="dialog" aria-modal="true" aria-labelledby="review-challenges-title">
            <div className="modal-header">
              <div>
                <div className="modal-kicker">Review</div>
                <h2 className="modal-title" id="review-challenges-title">Review Challenge Submissions</h2>
                <p className="modal-lead">{reviewChallenge?.description}</p>
              </div>
            </div>

            <div className="modal-body">
              {challengeSubmissions.length === 0 ? (
                <div className="empty-state-card">
                  <p>No submissions yet.</p>
                </div>
              ) : (
                <div style={{ maxHeight: '60vh', overflowY: 'auto', paddingRight: 4 }}>
                  {challengeSubmissions.map(sub => (
                    <div key={sub.submissionId} className="card" style={{ marginBottom: 12 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'start' }}>
                      <div>
                        <div style={{ fontWeight: 700 }}>{sub.playerName}</div>
                        <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>{sub.playerEmail}</div>
                        <div style={{ color: 'var(--text-muted)', fontSize: 12, marginTop: 6 }}>
                          Submitted: {formatDate(sub.submittedAt)}
                        </div>
                        <div style={{ marginTop: 6 }}>
                          <span className={`badge badge-${sub.reviewStatus === 'accomplished' ? 'active' : sub.reviewStatus === 'failed' ? 'completed' : 'pending'}`}>
                            {sub.reviewStatus}
                          </span>
                        </div>
                      </div>
                      <a className="btn btn-secondary btn-sm" href={sub.photoUrl} target="_blank" rel="noreferrer">Open Photo</a>
                      </div>

                      {sub.photoUrl && (
                        <img src={sub.photoUrl} alt="Challenge submission" style={{ width: '100%', borderRadius: 8, marginTop: 12, maxHeight: 280, objectFit: 'cover' }} />
                      )}

                      {sub.reviewStatus === 'pending' && view.status === 'active' && (
                        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => handleReviewSubmission(sub, 'accomplished')}
                            disabled={reviewingSubmissionId === sub.submissionId}
                          >
                            Mark Accomplished (+£{parseFloat(reviewChallenge?.prizeAmount || 0).toFixed(0)})
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleReviewSubmission(sub, 'failed')}
                            disabled={reviewingSubmissionId === sub.submissionId}
                          >
                            Mark Failed
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setShowReviewModal(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
