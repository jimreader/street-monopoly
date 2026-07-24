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
          setError(e.message || 'Unable to load event details.');
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
    <div className="app-shell app-shell--player">
      <div className="app-brand-band">
        <img
          src="/logo.svg"
          alt="Road Rush"
          style={{ height: 42, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.2))' }}
        />
      </div>

      <main className="center-screen" style={{ minHeight: 'calc(100dvh - 66px)', justifyContent: 'flex-start', paddingTop: 28 }}>
        <div className="section-eyebrow">Invitation</div>
        <h1 className="hero-title" style={{ fontSize: 34 }}>Join This Event</h1>

        <div className="join-warning-card" style={{ marginBottom: 14, width: '100%', maxWidth: 420 }} aria-live="polite">
          {error ? (
            <p className="hero-sub" style={{ color: 'var(--danger)', maxWidth: 360 }}>
              {error}
            </p>
          ) : !summary ? (
            <p className="hero-sub" style={{ maxWidth: 360 }}>
              Loading event details...
            </p>
          ) : (
            <>
              {summary.eventLogoUrl && (
                <img
                  src={summary.eventLogoUrl}
                  alt={`${summary.eventName} logo`}
                  style={{
                    maxHeight: 72,
                    width: 'auto',
                    maxWidth: 'min(80vw, 300px)',
                    objectFit: 'contain',
                    margin: '0 auto 12px',
                    borderRadius: 8
                  }}
                />
              )}
              <p className="hero-sub" style={{ maxWidth: 360, marginBottom: 8 }}>
                <strong>{summary.eventName}</strong>
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
            You can only join once. When you are ready, tap the button below to join this event on this device.
          </p>
          <p className="hero-sub" style={{ maxWidth: 360 }}>
            Keep this same browser page open while you play. Opening the link in another device or browser may stop access.
          </p>
        </div>
        <button className="join-confirm-btn" onClick={handleJoinClick}>Join Event Now</button>
      </main>
    </div>
  );
}
