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

export function GameScreen() {
  const { joinToken } = useParams();
  const [game, setGame] = useState(null);
  const [error, setError] = useState('');
  const [gpsPos, setGpsPos] = useState(null);
  const [gpsError, setGpsError] = useState(false);
  const [checkingIn, setCheckingIn] = useState(null);
  const [toast, setToast] = useState(null);
  const [filter, setFilter] = useState('all');
  const [countdown, setCountdown] = useState('');
  const watchRef = useRef(null);
  const toastTimer = useRef(null);

  // Load game data
  const loadGame = useCallback(async () => {
    try {
      const data = await api.getGameView(joinToken);
      setGame(data);
    } catch (e) {
      setError(e.message);
    }
  }, [joinToken]);

  useEffect(() => { loadGame(); }, [loadGame]);

  // Poll for updates
  useEffect(() => {
    const interval = setInterval(loadGame, 10000);
    return () => clearInterval(interval);
  }, [loadGame]);

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
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [game?.status, game?.startTime]);

  function showToast(message, type = 'info') {
    setToast({ message, type });
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 4000);
  }

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

  if (error) {
    return (
      <div className="center-screen">
        <div className="hero-icon">😵</div>
        <h1 className="hero-title">Oops</h1>
        <p className="hero-sub">{error}</p>
      </div>
    );
  }

  if (!game) {
    return (
      <div className="center-screen">
        <div className="hero-icon" style={{ animation: 'pulse 2s infinite' }}>🏙️</div>
        <p className="hero-sub">Loading game...</p>
      </div>
    );
  }

  // PENDING — show countdown
  if (game.status === 'pending') {
    return (
      <div className="countdown-screen">
        <div style={{ fontSize: 64, marginBottom: 24 }}>⏳</div>
        <p className="countdown-label">Game starts in</p>
        <div className="countdown-timer">{countdown}</div>
        <p className="countdown-date">
          {new Date(game.startTime).toLocaleString('en-GB', {
            weekday: 'long', day: 'numeric', month: 'long',
            hour: '2-digit', minute: '2-digit'
          })}
        </p>
        <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 24, marginTop: 32 }}>
          {game.gameName}
        </h2>
      </div>
    );
  }

  // COMPLETED — show final results
  if (game.status === 'completed') {
    const owned = game.streets.filter(s => s.ownedByPlayer).length;
    const visited = game.streets.filter(s => s.visitStatus !== 'unvisited').length;
    const finalBal = game.finalBalance != null ? parseFloat(game.finalBalance) : parseFloat(game.balance);

    return (
      <div className="game-over-screen">
        <div className="game-over-icon">🏁</div>
        <h1 className="game-over-title">Game Over</h1>
        <p style={{ color: 'var(--text-muted)', marginBottom: 24 }}>{game.gameName}</p>

        <p className="final-balance-label">Your Final Balance</p>
        <div className={`final-balance-value ${finalBal >= 0 ? 'balance-positive' : 'balance-negative'}`}>
          £{finalBal.toFixed(0)}
        </div>

        <div style={{ display: 'flex', gap: 24, marginBottom: 24 }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--monopoly-green)' }}>{owned}</div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Streets owned</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{visited}</div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Streets visited</div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--danger)' }}>{game.streets.length - visited}</div>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Unvisited</div>
          </div>
        </div>

        <p className="final-note">
          Unvisited street rental values have been deducted from your balance. Thanks for playing!
        </p>
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

  return (
    <div>
      <div className="player-header">
        <div className="player-header-top">
          <div>
            <div className="game-name">{game.gameName}</div>
            <div className="gps-status">
              <span className={`gps-dot ${gpsPos && !gpsError ? 'active' : 'inactive'}`} />
              {gpsPos && !gpsError
                ? `GPS active (±${Math.round(gpsPos.accuracy)}m)`
                : 'GPS unavailable'}
            </div>
          </div>
          <div className="balance-display">
            <div className="balance-label">Balance</div>
            <div className={`balance-value ${balance >= 0 ? 'balance-positive' : 'balance-negative'}`}>
              £{balance.toFixed(0)}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 12, marginTop: 10, fontSize: 12, color: 'var(--text-muted)' }}>
          <span>🏠 {ownedCount} owned</span>
          <span>👣 {visitedCount}/{game.streets.length} visited</span>
          <span style={{ marginLeft: 'auto' }}>
            Ends {new Date(game.endTime).toLocaleString('en-GB', { hour: '2-digit', minute: '2-digit', day: 'numeric', month: 'short' })}
          </span>
        </div>
      </div>

      <div className="player-content">
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
            const canCheckIn = near && street.visitStatus === 'unvisited' && !street.ownedByPlayer;
            const isLoading = checkingIn === street.streetId;
            const statusLabel = STATUS_LABELS[street.visitStatus] || street.visitStatus;
            const badgeClass = STATUS_BADGE_CLASS[street.visitStatus] || 'badge-unvisited';

            return (
              <div key={street.streetId}
                className={`street-card ${street.ownedByPlayer ? 'owned' : ''} ${street.visitStatus !== 'unvisited' && !street.ownedByPlayer ? 'visited' : ''}`}>
                <div className="street-colour-bar" style={{ backgroundColor: `var(--${street.colour})` }} />
                <div className="street-card-body">
                  <div className="street-card-top">
                    <div>
                      <div className="street-name">{street.name}</div>
                      <div className="street-prices">
                        <span>Buy: £{parseFloat(street.price).toFixed(0)}</span>
                        <span>Rent: £{parseFloat(street.rentalPrice).toFixed(0)}</span>
                      </div>
                    </div>
                    <span className={`street-status-badge ${badgeClass}`}>
                      {statusLabel}
                    </span>
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
                            📍 {Math.round(calculateDistance(gpsPos.lat, gpsPos.lng, street.latitude, street.longitude))}m away
                            — need to be within {game.proximityMetres}m
                          </div>
                        )
                      )}
                    </>
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
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          {toast.message}
        </div>
      )}
    </div>
  );
}
