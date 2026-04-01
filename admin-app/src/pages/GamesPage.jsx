import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

export function GamesPage() {
  const [games, setGames] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.getGames().then(setGames).catch(e => setError(e.message));
  }, []);

  function formatDate(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  const pending = games.filter(g => g.status === 'pending');
  const active = games.filter(g => g.status === 'active');
  const completed = games.filter(g => g.status === 'completed');

  function GameCard({ game }) {
    return (
      <Link to={`/games/${game.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <h3 className="card-title">{game.name}</h3>
            <span className={`badge badge-${game.status}`}>{game.status}</span>
          </div>
          <div className="card-meta">
            <span>🕐 {formatDate(game.startTime)}</span>
            <span>💰 £{parseFloat(game.startingBalance).toFixed(0)}</span>
            <span>📍 {game.proximityMetres}m</span>
          </div>
        </div>
      </Link>
    );
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Games</h1>
          <p className="page-subtitle">Create and manage street monopoly games</p>
        </div>
        <Link to="/games/create" className="btn btn-primary">+ New Game</Link>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {games.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🎮</div>
          <p>No games yet. Create your first game to get started.</p>
        </div>
      ) : (
        <>
          {active.length > 0 && (
            <div className="section">
              <h2 className="section-title">🟢 Active</h2>
              <div className="card-grid">{active.map(g => <GameCard key={g.id} game={g} />)}</div>
            </div>
          )}
          {pending.length > 0 && (
            <div className="section">
              <h2 className="section-title">⏳ Pending</h2>
              <div className="card-grid">{pending.map(g => <GameCard key={g.id} game={g} />)}</div>
            </div>
          )}
          {completed.length > 0 && (
            <div className="section">
              <h2 className="section-title">✅ Completed</h2>
              <div className="card-grid">{completed.map(g => <GameCard key={g.id} game={g} />)}</div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
