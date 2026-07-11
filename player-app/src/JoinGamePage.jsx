import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from './api.js';

export function JoinGamePage() {
  const { joinToken } = useParams();
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    api.getJoinSummary(joinToken)
      .then((data) => {
        if (active) {
          setSummary(data);
          setError('');
        }
      })
      .catch((e) => {
        if (active) {
          setError(e.message || 'Unable to load game details.');
        }
      });

    return () => {
      active = false;
    };
  }, [joinToken]);

  function handleJoinClick() {
    navigate(`/game/${joinToken}/play`);
  }

  function formatDateTime(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  return (
    <div className="center-screen">
      <img
        src="/logo.svg"
        alt="Road Rush"
        style={{ height: 44, marginBottom: 20, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.15))' }}
      />
      <h1 className="hero-title" style={{ fontSize: 34 }}>Join This Game</h1>

      <div className="join-warning-card" style={{ marginBottom: 14, width: '100%', maxWidth: 420 }}>
        {error ? (
          <p className="hero-sub" style={{ color: 'var(--danger)', maxWidth: 360 }}>
            {error}
          </p>
        ) : !summary ? (
          <p className="hero-sub" style={{ maxWidth: 360 }}>
            Loading game details...
          </p>
        ) : (
          <>
            <p className="hero-sub" style={{ maxWidth: 360, marginBottom: 8 }}>
              <strong>{summary.gameName}</strong>
            </p>
            <p className="hero-sub" style={{ maxWidth: 360, marginBottom: 4 }}>
              Start: {formatDateTime(summary.startTime)}
            </p>
            <p className="hero-sub" style={{ maxWidth: 360 }}>
              End: {formatDateTime(summary.endTime)}
            </p>
          </>
        )}
      </div>

      <div className="join-warning-card">
        <p className="hero-sub" style={{ maxWidth: 360 }}>
          You can only join once. When you are ready, tap the button below to join this game on this device.
        </p>
        <p className="hero-sub" style={{ maxWidth: 360 }}>
          Keep this same browser page open while you play. Opening the link in another device or browser may stop access.
        </p>
      </div>
      <button className="join-confirm-btn" onClick={handleJoinClick}>Join Game Now</button>
    </div>
  );
}
