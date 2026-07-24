import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

export function GamesPage() {
  const [events, setEvents] = useState([]);
  const [error, setError] = useState('');
  const [deletingEventId, setDeletingEventId] = useState(null);

  useEffect(() => {
    api.getEvents().then(setEvents).catch(e => setError(e.message));
  }, []);

  function formatDate(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-GB', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  const pending = events.filter(e => e.status === 'pending');
  const active = events.filter(e => e.status === 'active');
  const completed = events.filter(e => e.status === 'completed');

  async function handleDeleteEvent(event) {
    const canDelete = event.status === 'pending' || event.status === 'completed';
    if (!canDelete) {
      setError('Only pending or completed events can be deleted.');
      return;
    }

    const confirmed = window.confirm(`Delete event "${event.name}"? This is a soft delete and can hide event data from admin/player views.`);
    if (!confirmed) return;

    setError('');
    setDeletingEventId(event.id);
    try {
      await api.deleteEvent(event.id);
      setEvents(prev => prev.filter(e => e.id !== event.id));
    } catch (e) {
      setError(e.message);
    } finally {
      setDeletingEventId(null);
    }
  }

  function EventCard({ event }) {
    const canDelete = event.status === 'pending' || event.status === 'completed';
    const isDeleting = deletingEventId === event.id;

    return (
      <div className="card">
        <Link to={`/events/${event.id}`} style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <h3 className="card-title">{event.name}</h3>
            <span className={`badge badge-${event.status}`}>{event.status}</span>
          </div>
          <div className="card-meta">
            <span>🕐 {formatDate(event.startTime)}</span>
            <span>💰 £{parseFloat(event.startingBalance).toFixed(0)}</span>
            <span>📍 {event.proximityMetres}m</span>
            <span>👥 max {event.maxPlayersPerGame}/game</span>
          </div>
        </Link>

        {canDelete && (
          <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end' }}>
            <button
              className="btn btn-danger btn-sm"
              onClick={() => handleDeleteEvent(event)}
              disabled={isDeleting}
            >
              {isDeleting ? 'Deleting...' : 'Delete Event'}
            </button>
          </div>
        )}
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Events</h1>
          <p className="page-subtitle">Create and manage events that automatically split players into balanced games</p>
        </div>
        <Link to="/events/create" className="btn btn-primary">+ New Event</Link>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {events.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🎪</div>
          <p>No events yet. Create your first event to get started.</p>
        </div>
      ) : (
        <>
          {active.length > 0 && (
            <div className="section">
              <h2 className="section-title">🟢 Active</h2>
              <div className="card-grid">{active.map(e => <EventCard key={e.id} event={e} />)}</div>
            </div>
          )}
          {pending.length > 0 && (
            <div className="section">
              <h2 className="section-title">⏳ Pending</h2>
              <div className="card-grid">{pending.map(e => <EventCard key={e.id} event={e} />)}</div>
            </div>
          )}
          {completed.length > 0 && (
            <div className="section">
              <h2 className="section-title">✅ Completed</h2>
              <div className="card-grid">{completed.map(e => <EventCard key={e.id} event={e} />)}</div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
