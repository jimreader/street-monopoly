import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { api } from './api.js';

const STATUS_LABELS = {
  owned: 'Owned',
  unvisited: 'Not visited',
  'visited_rent_paid': 'Rent paid',
  'visited_insufficient_funds': 'Visited',
};

const STATUS_BADGE_CLASS = {
  owned: 'badge-owned',
  unvisited: 'badge-unvisited',
  'visited_rent_paid': 'badge-visited-rent',
  'visited_insufficient_funds': 'badge-visited-no-funds',
};

function RentIncomeList({ rentCollections }) {
  if (!rentCollections || rentCollections.length === 0) return null;
  const totalRent = rentCollections.reduce((s, r) => s + parseFloat(r.amount), 0);

  return (
    <div style={{
      background: 'var(--surface)', border: '1.5px solid var(--border)',
      borderRadius: 'var(--radius-lg)', overflow: 'hidden'
    }}>
      <div style={{
        padding: '12px 16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        borderBottom: '1px solid var(--border)', background: 'var(--bg-warm)'
      }}>
        <span style={{ fontWeight: 700, fontSize: 14 }}>Rent Income</span>
        <span style={{ fontWeight: 700, fontSize: 14, color: 'var(--monopoly-green)' }}>+£{totalRent.toFixed(0)}</span>
      </div>
      {rentCollections.map((rc, i) => (
        <div key={i} style={{
          display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px',
          borderBottom: i < rentCollections.length - 1 ? '1px solid var(--border)' : 'none',
          fontSize: 13
        }}>
          <span style={{
            width: 10, height: 10, borderRadius: 3, flexShrink: 0,
            background: `var(--${rc.streetColour})`
          }} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontWeight: 600 }}>{rc.streetName}</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
              Paid by {rc.paidByPlayerName}
              {rc.collectedAt && (
                <span style={{ marginLeft: 6, color: 'var(--text-dim)' }}>
                  {new Date(rc.collectedAt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}
                </span>
              )}
            </div>
          </div>
          <span style={{ fontWeight: 700, color: 'var(--monopoly-green)', flexShrink: 0 }}>
            +£{parseFloat(rc.amount).toFixed(0)}
          </span>
        </div>
      ))}
    </div>
  );
}

export function GameScreen() {
  const { joinToken } = useParams();
  const [game, setGame] = useState(null);
  const [challenges, setChallenges] = useState([]);
  const [challengeAnnouncement, setChallengeAnnouncement] = useState('');
  const [error, setError] = useState('');
  const [gpsPos, setGpsPos] = useState(null);
  const [gpsError, setGpsError] = useState(false);
  const [checkingIn, setCheckingIn] = useState(null);
  const [toast, setToast] = useState(null);
  const [uploadingChallengeId, setUploadingChallengeId] = useState(null);
  const [filter, setFilter] = useState('all');
  const [expandedStreetId, setExpandedStreetId] = useState(null);
  const [countdown, setCountdown] = useState('');
  const [endCountdown, setEndCountdown] = useState('');
  const watchRef = useRef(null);
  const toastTimer = useRef(null);
  const announcementTimer = useRef(null);
  const previousChallengeStatuses = useRef(new Map());
  const challengeStatusesInitialized = useRef(false);

  // Load game data
  const loadGame = useCallback(async () => {
    try {
      const [gameData, challengeData] = await Promise.all([
        api.getGameView(joinToken),
        api.getChallenges(joinToken),
      ]);
      setGame(gameData);
      setChallenges(challengeData || []);
    } catch (e) {
      setError(e.message);
    }
  }, [joinToken]);

  useEffect(() => { loadGame(); }, [loadGame]);

  // Poll for updates
  useEffect(() => {
    if (game?.status === 'completed') return;
    const intervalMs = 10000;
    const interval = setInterval(loadGame, intervalMs);
    return () => clearInterval(interval);
  }, [loadGame, game?.status]);

  // GPS tracking
  useEffect(() => {
    if (!navigator.geolocation) {
      setGpsError(true);
      return;
    }
    watchRef.current = navigator.geolocation.watchPosition(
      (pos) => {
        setGpsPos({ lat: pos.coords.latitude, lng: pos.coords.longitude, accuracy: pos.coords.accuracy });
        setGpsError(false);
      },
      () => setGpsError(true),
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 15000 }
    );
    return () => {
      if (watchRef.current != null) navigator.geolocation.clearWatch(watchRef.current);
    };
  }, []);

  // Countdown timer
  useEffect(() => {
    if (!game || game.status !== 'pending') return;
    const target = new Date(game.startTime).getTime();
    let intervalId;

    function update() {
      const now = Date.now();
      const diff = target - now;
      if (diff <= 0) {
        setCountdown('Starting...');
        loadGame();
        return;
      }
      const d = Math.floor(diff / 86400000);
      const h = Math.floor((diff % 86400000) / 3600000);
      const m = Math.floor((diff % 3600000) / 60000);
      const s = Math.floor((diff % 60000) / 1000);
      if (d > 0) setCountdown(`${d}d ${h}h ${m}m ${s}s`);
      else if (h > 0) setCountdown(`${h}h ${m}m ${s}s`);
      else setCountdown(`${m}m ${s}s`);
    }

    update();
    intervalId = setInterval(update, 1000);
    return () => clearInterval(intervalId);
  }, [game?.status, game?.startTime, loadGame]);

  // Active game end countdown timer
  useEffect(() => {
    if (!game || game.status !== 'active') {
      setEndCountdown('');
      return;
    }

    const target = new Date(game.endTime).getTime();
    let intervalId;

    function updateEndCountdown() {
      const now = Date.now();
      const diff = target - now;

      if (diff <= 0) {
        setEndCountdown('Ending...');
        return;
      }

      const d = Math.floor(diff / 86400000);
      const h = Math.floor((diff % 86400000) / 3600000);
      const m = Math.floor((diff % 3600000) / 60000);
      const s = Math.floor((diff % 60000) / 1000);

      if (d > 0) setEndCountdown(`${d}d ${h}h ${m}m ${s}s`);
      else if (h > 0) setEndCountdown(`${h}h ${m}m ${s}s`);
      else setEndCountdown(`${m}m ${s}s`);
    }

    updateEndCountdown();
    intervalId = setInterval(updateEndCountdown, 1000);
    return () => clearInterval(intervalId);
  }, [game?.status, game?.endTime]);

  function showToast(message, type = 'info') {
    setToast({ message, type });
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 4000);
  }

  function announceNewChallenges(newlyActiveChallenges) {
    if (!newlyActiveChallenges || newlyActiveChallenges.length === 0) return;

    const message = newlyActiveChallenges.length === 1
      ? `New challenge available: ${newlyActiveChallenges[0].description}`
      : `${newlyActiveChallenges.length} new challenges are now available.`;

    setChallengeAnnouncement(message);
    if (announcementTimer.current) clearTimeout(announcementTimer.current);
    announcementTimer.current = setTimeout(() => setChallengeAnnouncement(''), 12000);

    showToast(message, 'success');
    try {
      if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
        navigator.vibrate(200);
      }
    } catch {
      // Ignore unsupported vibration API errors on some browsers/devices.
    }
  }

  useEffect(() => {
    const nextStatuses = new Map(challenges.map(ch => [ch.id, ch.status]));

    if (challengeStatusesInitialized.current) {
      const newlyActive = challenges.filter(ch => {
        const previous = previousChallengeStatuses.current.get(ch.id);
        return ch.status === 'active' && previous !== 'active' && ch.submissionStatus === 'awaiting_submission';
      });
      announceNewChallenges(newlyActive);
    }

    previousChallengeStatuses.current = nextStatuses;
    challengeStatusesInitialized.current = true;
  }, [challenges]);

  useEffect(() => () => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    if (announcementTimer.current) clearTimeout(announcementTimer.current);
  }, []);

  function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) ** 2 +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  function isNearStreet(street) {
    if (!gpsPos || !game) return false;
    const dist = calculateDistance(gpsPos.lat, gpsPos.lng, street.latitude, street.longitude);
    return dist <= game.proximityMetres;
  }

  async function handleCheckIn(street) {
    if (!gpsPos) {
      showToast('GPS location not available', 'error');
      return;
    }
    setCheckingIn(street.streetId);
    try {
      const result = await api.checkIn(joinToken, {
        streetId: street.streetId,
        latitude: gpsPos.lat,
        longitude: gpsPos.lng
      });
      const type = result.outcome === 'purchased' ? 'success'
        : result.outcome === 'rent_paid' ? 'error' : 'info';
      showToast(result.message, type);
      await loadGame();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setCheckingIn(null);
    }
  }

  async function handleChallengePhotoSelected(challenge, file) {
    if (!file) return;
    setUploadingChallengeId(challenge.id);
    try {
      const upload = await api.uploadChallengePhoto(file);
      await api.submitChallenge(joinToken, challenge.id, { photoUrl: upload.url });
      showToast('Challenge photo submitted for review.', 'success');
      await loadGame();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setUploadingChallengeId(null);
    }
  }

  function challengeStatusLabel(challenge) {
    switch (challenge.submissionStatus) {
      case 'awaiting_submission':
        return 'Awaiting your photo';
      case 'submitted_pending_review':
        return 'Submitted - pending review';
      case 'submitted_accomplished':
        return 'Accomplished';
      case 'submitted_failed':
        return 'Failed';
      case 'missed':
        return 'Missed';
      default:
        return 'Not open yet';
    }
  }

  if (error) {
    return (
      <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>
        <div className="app-brand-band">
          <img
            src="/logo.svg"
            alt="Road Rush"
            style={{ height: 42, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.2))' }}
          />
        </div>
        <div className="center-screen" style={{ minHeight: 'calc(100dvh - 66px)' }}>
          <div className="hero-icon">😵</div>
          <h1 className="hero-title">Oops</h1>
          <p className="hero-sub">{error}</p>
        </div>
      </div>
    );
  }

  if (!game) {
    return (
      <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>
        <div className="app-brand-band">
          <img
            src="/logo.svg"
            alt="Road Rush"
            style={{ height: 42, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.2))' }}
          />
        </div>
        <div className="center-screen" style={{ minHeight: 'calc(100dvh - 66px)' }}>
          <img src="/logo.svg" alt="Road Rush" style={{ height: 36, animation: 'pulse 2s infinite' }} />
          <p className="hero-sub">Loading event...</p>
        </div>
      </div>
    );
  }

  // PENDING — show countdown
  if (game.status === 'pending') {
    return (
      <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>
        <div className="app-brand-band">
          <img
            src="/logo.svg"
            alt="Road Rush"
            style={{ height: 42, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.2))' }}
          />
        </div>
        <div className="countdown-screen" style={{ minHeight: 'calc(100dvh - 66px)' }}>
          <div style={{ fontSize: 64, marginBottom: 24 }}>⏳</div>
          <p className="countdown-label">Event starts in</p>
          <div className="countdown-timer">{countdown}</div>
          <p className="countdown-date">
            {new Date(game.startTime).toLocaleString('en-GB', {
              weekday: 'long', day: 'numeric', month: 'long',
              hour: '2-digit', minute: '2-digit'
            })}
          </p>
          <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 24, fontWeight: 700, marginTop: 32 }}>
            {game.eventName}
          </h2>
        </div>
      </div>
    );
  }

  // COMPLETED — show final results
  if (game.status === 'completed') {
    const owned = game.streets.filter(s => s.ownedByPlayer).length;
    const visited = game.streets.filter(s => s.visitStatus !== 'unvisited').length;
    const notVisited = game.streets.length - visited;
    const rentCollectedCount = (game.rentCollections || []).length;
    const rentPaidCount = game.streets.filter(s => s.visitStatus === 'visited_rent_paid').length;
    const challengesCompletedCount = challenges.filter(c => c.submissionStatus === 'submitted_accomplished').length;

    return (
      <div style={{ minHeight: '100dvh', background: 'var(--bg)' }}>
        <div className="app-brand-band">
          <img
            src="/logo.svg"
            alt="Road Rush"
            style={{ height: 42, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.2))' }}
          />
        </div>
        <div className="game-over-screen" style={{ minHeight: 'auto', paddingBottom: 20 }}>
          <div className="game-over-icon">🏁</div>
          <h1 className="game-over-title">Event Complete</h1>
          <p style={{ color: 'var(--text-muted)', marginBottom: 24 }}>{game.eventName}</p>

          <p className="final-note" style={{ marginBottom: 20, fontSize: 18, fontWeight: 700, color: 'var(--text)' }}>
            Please return to Game HQ for the final results announcement.
          </p>

          <div style={{ display: 'flex', gap: 24, marginBottom: 24, flexWrap: 'wrap', justifyContent: 'center' }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--monopoly-green)' }}>{owned}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Streets owned</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700 }}>{visited}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Streets visited</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--text-muted)' }}>{notVisited}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Streets not visited</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--monopoly-green)' }}>{rentCollectedCount}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Times rent was collected</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--monopoly-red)' }}>{rentPaidCount}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Times rent was paid</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--monopoly-green)' }}>{challengesCompletedCount}</div>
              <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Challenges completed</div>
            </div>
          </div>

          <p className="final-note">
            Unvisited street rental values have been deducted from your balance. Thanks for playing!
          </p>
        </div>

        {/* Rent income summary */}
        {game.rentCollections && game.rentCollections.length > 0 && (
          <div style={{ padding: '0 16px 40px' }}>
            <RentIncomeList rentCollections={game.rentCollections} />
          </div>
        )}
      </div>
    );
  }

  // ACTIVE — main game view
  const filteredStreets = game.streets.filter(s => {
    if (filter === 'all') return true;
    if (filter === 'owned') return s.ownedByPlayer;
    if (filter === 'unvisited') return s.visitStatus === 'unvisited';
    if (filter === 'visited') return s.visitStatus !== 'unvisited' && !s.ownedByPlayer;
    return true;
  });

  const balance = parseFloat(game.balance);
  const ownedCount = game.streets.filter(s => s.ownedByPlayer).length;
  const visitedCount = game.streets.filter(s => s.visitStatus !== 'unvisited').length;
  const sortedChallenges = [...challenges].sort((a, b) => {
    const rank = (challenge) => {
      const status = (challenge.status || '').toLowerCase().trim();
      const submissionStatus = (challenge.submissionStatus || '').toLowerCase().trim();

      // Keep actionable items at the top.
      if (status === 'active' && submissionStatus === 'awaiting_submission') return 0;
      if (status === 'active') return 1;

      // Player-complete outcomes belong at the bottom.
      if (
        status === 'completed' ||
        submissionStatus === 'submitted_accomplished' ||
        submissionStatus === 'submitted_failed' ||
        submissionStatus === 'missed'
      ) return 3;

      return 2;
    };
    return rank(a) - rank(b);
  });
  const activeChallenges = challenges.filter(c => c.status === 'active');

  return (
    <div>
      <div className="player-header">
        <div className="player-header-top">
          <div className="player-header-meta-row">
            <img src="/logo.svg" alt="Road Rush" className="header-logo" />
            <div className="balance-display">
              <div className="balance-label">Balance</div>
              <div className={`balance-value ${balance >= 0 ? 'balance-positive' : 'balance-negative'}`}>
                £{balance.toFixed(0)}
              </div>
            </div>
          </div>

          <div className="game-name game-name-full">{game.eventName}</div>

          <div className="gps-status">
            <span className={`gps-dot ${gpsPos && !gpsError ? 'active' : 'inactive'}`} />
            {gpsPos && !gpsError
              ? `GPS active (±${Math.round(gpsPos.accuracy)}m)`
              : 'GPS unavailable'}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 12, marginTop: 10, fontSize: 12, fontWeight: 600, color: 'rgba(255,255,255,0.9)' }}>
          <span>🏠 {ownedCount} owned</span>
          <span>👣 {visitedCount}/{game.streets.length} visited</span>
          <span style={{ marginLeft: 'auto' }}>
            Ends in {endCountdown || '—'}
          </span>
        </div>
      </div>

      <div className="player-content">
        {challengeAnnouncement && (
          <div style={{
            marginBottom: 12,
            background: 'var(--monopoly-green-light)',
            color: 'var(--text)',
            border: '1px solid var(--monopoly-green)',
            borderRadius: 'var(--radius)',
            padding: '10px 12px',
            fontWeight: 700,
            fontSize: 13
          }}>
            {challengeAnnouncement}
          </div>
        )}

        <div style={{ marginBottom: 18 }}>
          <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 10 }}>Challenges</div>
          {challenges.length === 0 ? (
            <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>No challenges are available for this event.</div>
          ) : (
            <div style={{ display: 'grid', gap: 10 }}>
              {sortedChallenges.map(ch => {
                const canSubmit = ch.status === 'active' && ch.submissionStatus === 'awaiting_submission';
                const submitted = ch.submittedPhotoUrl;
                return (
                  <div key={ch.id} style={{
                    background: 'var(--surface)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-lg)',
                    padding: 12
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, alignItems: 'start' }}>
                      <div>
                        <div style={{ fontWeight: 700 }}>{ch.description}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>
                          Prize £{parseFloat(ch.prizeAmount || 0).toFixed(0)} · {ch.durationMinutes} minutes
                        </div>
                        {ch.scheduledEndAt && ch.status === 'active' && (
                          <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 4 }}>
                            Ends {new Date(ch.scheduledEndAt).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        )}
                      </div>
                      <span className={`street-status-badge ${
                        ch.submissionStatus === 'submitted_accomplished' ? 'badge-owned' :
                        ch.submissionStatus === 'submitted_failed' ? 'badge-visited-rent' :
                        ch.submissionStatus === 'awaiting_submission' ? 'badge-unvisited' :
                        'badge-visited-no-funds'
                      }`}>
                        {challengeStatusLabel(ch)}
                      </span>
                    </div>

                    {submitted && (
                      <img
                        src={submitted}
                        alt="Submitted challenge"
                        style={{ width: '100%', borderRadius: 8, marginTop: 10, maxHeight: 180, objectFit: 'cover' }}
                      />
                    )}

                    {canSubmit && (
                      <div style={{ marginTop: 10 }}>
                        <label className="checkin-btn" style={{ display: 'inline-block', cursor: 'pointer', margin: 0 }}>
                          {uploadingChallengeId === ch.id ? 'Uploading...' : 'Submit Photo'}
                          <input
                            type="file"
                            accept="image/*"
                            capture="environment"
                            style={{ display: 'none' }}
                            disabled={uploadingChallengeId === ch.id}
                            onChange={(e) => handleChallengePhotoSelected(ch, e.target.files?.[0])}
                          />
                        </label>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
          {activeChallenges.length > 1 && (
            <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-dim)' }}>
              Multiple active challenges are currently available.
            </div>
          )}
        </div>

        <div className="filter-bar">
          {['all', 'unvisited', 'owned', 'visited'].map(f => (
            <button key={f} className={`filter-chip ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}>
              {f === 'all' ? `All (${game.streets.length})`
                : f === 'unvisited' ? `Unvisited (${game.streets.filter(s => s.visitStatus === 'unvisited').length})`
                : f === 'owned' ? `Owned (${ownedCount})`
                : `Visited (${game.streets.filter(s => s.visitStatus !== 'unvisited' && !s.ownedByPlayer).length})`}
            </button>
          ))}
        </div>

        <div className="street-grid">
          {filteredStreets.map(street => {
            const near = isNearStreet(street);
            const isLoading = checkingIn === street.streetId;
            const statusLabel = STATUS_LABELS[street.visitStatus] || street.visitStatus;
            const badgeClass = STATUS_BADGE_CLASS[street.visitStatus] || 'badge-unvisited';
            const isExpanded = expandedStreetId === street.streetId;

            return (
              <div key={street.streetId}
                className={`street-card ${street.ownedByPlayer ? 'owned' : ''} ${street.visitStatus !== 'unvisited' && !street.ownedByPlayer ? 'visited' : ''}`}>
                <div className="street-colour-bar" style={{ backgroundColor: `var(--${street.colour})` }} />
                <div className="street-card-body">
                  <button
                    type="button"
                    className="street-summary-btn"
                    onClick={() => setExpandedStreetId(prev => prev === street.streetId ? null : street.streetId)}
                  >
                    <div>
                      <div className="street-name">{street.name}</div>
                      <div className="street-prices">
                        <span>Price: £{parseFloat(street.price).toFixed(0)}</span>
                      </div>
                    </div>
                    <span className={`street-status-badge ${badgeClass}`}>
                      {statusLabel}
                    </span>
                  </button>

                  {isExpanded && (
                    <>
                      <div style={{ marginTop: 8, fontSize: 13, color: 'var(--text-muted)', fontWeight: 600 }}>
                        Rent: £{parseFloat(street.rentalPrice).toFixed(0)}
                      </div>

                      {street.imageClueUrl && street.visitStatus === 'unvisited' && (
                        <img src={street.imageClueUrl} alt="Location clue" className="street-clue-img"
                          onError={(e) => e.target.style.display = 'none'} />
                      )}

                      {street.visitStatus === 'unvisited' && !street.ownedByPlayer && (
                        <>
                          {near && gpsPos ? (
                            <button className={`checkin-btn ${isLoading ? 'loading' : ''}`}
                              onClick={() => handleCheckIn(street)} disabled={isLoading}>
                              {isLoading ? '⏳ Checking in...' : '📍 Check In'}
                            </button>
                          ) : (
                            gpsPos && (
                              <div style={{ marginTop: 10, fontSize: 12, color: 'var(--text-dim)', textAlign: 'center' }}>
                                Move closer to this street to unlock check in.
                              </div>
                            )
                          )}
                        </>
                      )}
                    </>
                  )}
                  {!isExpanded && (
                    <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-dim)' }}>
                      Tap to view details
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {filteredStreets.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-muted)' }}>
            No streets match this filter.
          </div>
        )}

        {/* Rent income during active game */}
        {game.rentCollections && game.rentCollections.length > 0 && (
          <div style={{ marginTop: 20 }}>
            <RentIncomeList rentCollections={game.rentCollections} />
          </div>
        )}
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.message}
        </div>
      )}
    </div>
  );
}
