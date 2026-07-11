import React from 'react';
import { useNavigate, useParams } from 'react-router-dom';

export function JoinGamePage() {
  const { joinToken } = useParams();
  const navigate = useNavigate();

  function handleJoinClick() {
    navigate(`/game/${joinToken}/play`);
  }

  return (
    <div className="center-screen">
      <img
        src="/logo.svg"
        alt="Road Rush"
        style={{ height: 44, marginBottom: 20, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.15))' }}
      />
      <h1 className="hero-title" style={{ fontSize: 34 }}>Join This Game</h1>
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
