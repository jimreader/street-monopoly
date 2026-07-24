import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api.js';

const COLOURS = ['brown', 'light_blue', 'pink', 'orange', 'red', 'yellow', 'green', 'dark_blue'];
const COLOUR_LABELS = {
  brown: 'Brown', light_blue: 'Light Blue', pink: 'Pink', orange: 'Orange',
  red: 'Red', yellow: 'Yellow', green: 'Green', dark_blue: 'Dark Blue'
};

const emptyStreet = {
  name: '', price: '', rentalPrice: '', colour: 'brown',
  latitude: '', longitude: '', imageClueUrl: ''
};

export function MapDetailPage() {
  const { id } = useParams();
  const [map, setMap] = useState(null);
  const [editing, setEditing] = useState(false);
  const [editingStreet, setEditingStreet] = useState(null);
  const [form, setForm] = useState({ ...emptyStreet });
  const [error, setError] = useState('');
  const [gpsLoading, setGpsLoading] = useState(false);
  const [gpsAccuracy, setGpsAccuracy] = useState(null);
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const formRef = useRef(null);
  const fileInputRef = useRef(null);

  useEffect(() => { loadMap(); }, [id]);

  async function loadMap() {
    try { setMap(await api.getMap(id)); } catch (e) { setError(e.message); }
  }

  function openAdd() {
    setEditingStreet(null);
    setForm({ ...emptyStreet });
    setImageFile(null);
    setImagePreview(null);
    setGpsAccuracy(null);
    setEditing(true);
    setTimeout(() => formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100);
  }

  function openEdit(street) {
    setEditingStreet(street);
    setForm({
      name: street.name,
      price: street.price,
      rentalPrice: street.rentalPrice,
      colour: street.colour,
      latitude: street.latitude,
      longitude: street.longitude,
      imageClueUrl: street.imageClueUrl || ''
    });
    setImageFile(null);
    setImagePreview(street.imageClueUrl || null);
    setGpsAccuracy(null);
    setEditing(true);
    setTimeout(() => formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100);
  }

  function cancelEdit() {
    setEditing(false);
    setEditingStreet(null);
    setForm({ ...emptyStreet });
    setImageFile(null);
    setImagePreview(null);
    setGpsAccuracy(null);
    setError('');
  }

  // ---- GPS capture ----
  function captureGPS() {
    if (!navigator.geolocation) {
      setError('GPS is not available on this device.');
      return;
    }
    setGpsLoading(true);
    setError('');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setForm(f => ({
          ...f,
          latitude: pos.coords.latitude.toFixed(6),
          longitude: pos.coords.longitude.toFixed(6)
        }));
        setGpsAccuracy(Math.round(pos.coords.accuracy));
        setGpsLoading(false);
      },
      (err) => {
        setError('Could not get GPS location: ' + err.message);
        setGpsLoading(false);
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
    );
  }

  // ---- Camera / image capture ----
  function handleImageCapture(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setImageFile(file);
    const reader = new FileReader();
    reader.onload = (ev) => setImagePreview(ev.target.result);
    reader.readAsDataURL(file);
  }

  function removeImage() {
    setImageFile(null);
    setImagePreview(null);
    setForm(f => ({ ...f, imageClueUrl: '' }));
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  // ---- Save ----
  async function handleSave(e) {
    e.preventDefault();
    if (!form.latitude || !form.longitude) {
      setError('Please capture your GPS location first.');
      return;
    }
    setError('');
    setSaving(true);

    try {
      let imageUrl = form.imageClueUrl;

      // Upload new image if captured
      if (imageFile) {
        setUploading(true);
        const result = await api.uploadImage(imageFile);
        imageUrl = result.url;
        setUploading(false);
      }

      const data = {
        name: form.name,
        price: parseFloat(form.price),
        rentalPrice: parseFloat(form.rentalPrice),
        colour: form.colour,
        latitude: parseFloat(form.latitude),
        longitude: parseFloat(form.longitude),
        imageClueUrl: imageUrl
      };

      if (editingStreet) {
        await api.updateStreet(editingStreet.id, data);
      } else {
        await api.addStreet(id, data);
      }
      cancelEdit();
      loadMap();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
      setUploading(false);
    }
  }

  async function handleDelete(streetId) {
    if (!confirm('Delete this street?')) return;
    try { await api.deleteStreet(streetId); loadMap(); } catch (e) { setError(e.message); }
  }

  function setField(key, value) { setForm(f => ({ ...f, [key]: value })); }

  if (!map) return <div className="empty-state-card"><div className="loading-spinner" /></div>;

  return (
    <div className="page-shell">
      <div className="page-header">
        <div>
          <Link to="/maps" className="muted">← Back</Link>
          <div className="section-eyebrow" style={{ marginTop: 8 }}>Maps</div>
          <h1 className="page-title">{map.name}</h1>
          <div className="card-meta" style={{ marginTop: 6 }}>
            {map.postcodeArea && <span>📮 {map.postcodeArea}</span>}
            <span>📍 {map.streets?.length || 0} streets</span>
          </div>
        </div>
        <div className="page-actions">
          {!editing && (
            <button className="btn btn-primary" onClick={openAdd}>+ Add</button>
          )}
        </div>
      </div>

      {error && <div className="error-msg">{error}</div>}

      {/* ===== ADD / EDIT FORM ===== */}
      {editing && (
        <div ref={formRef} className="surface-panel" style={{ marginBottom: 20, borderColor: 'rgba(25,95,61,0.35)', boxShadow: '0 16px 42px rgba(25,95,61,0.08)' }}>
          <div className="card-header-row" style={{ marginBottom: 16 }}>
            <h3 className="card-title" style={{ marginBottom: 0 }}>
              {editingStreet ? 'Edit Street' : 'Add Street'}
            </h3>
            <button className="btn btn-secondary btn-sm" onClick={cancelEdit} aria-label="Cancel editing">✕</button>
          </div>

          <form onSubmit={handleSave}>
            {/* GPS Capture */}
            <div className="form-group">
              <label className="form-label">Location</label>
              <button
                type="button"
                className={`gps-btn ${form.latitude ? 'captured' : ''} ${gpsLoading ? 'loading' : ''}`}
                onClick={captureGPS}
                disabled={gpsLoading}
              >
                {gpsLoading ? (
                  <>
                    <span className="loading-spinner" style={{ width: 20, height: 20, borderWidth: 2 }} />
                    Getting location...
                  </>
                ) : form.latitude ? (
                  <>
                    📍 {form.latitude}, {form.longitude}
                    {gpsAccuracy && <span style={{ fontSize: 12, opacity: 0.7 }}>(±{gpsAccuracy}m)</span>}
                    <span style={{ fontSize: 12, opacity: 0.7 }}>Tap to recapture</span>
                  </>
                ) : (
                  <>📍 Tap to capture GPS location</>
                )}
              </button>
            </div>

            {/* Camera / Image Capture */}
            <div className="form-group">
              <label className="form-label">Photo Clue</label>
              {imagePreview ? (
                <div style={{ position: 'relative' }}>
                  <img src={imagePreview} alt="Clue preview" className="camera-preview" />
                  <button type="button" onClick={removeImage} style={{
                    position: 'absolute', top: 16, right: 8,
                    background: 'var(--monopoly-red)', color: '#FFF',
                    border: 'none', borderRadius: '50%', width: 28, height: 28,
                    cursor: 'pointer', fontSize: 14, fontWeight: 700,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    boxShadow: 'var(--shadow-md)'
                  }}>✕</button>
                </div>
              ) : (
                <div className="camera-btn">
                  <span style={{ fontSize: 28 }}>📸</span>
                  <span>Tap to take photo or choose from gallery</span>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    capture="environment"
                    onChange={handleImageCapture}
                  />
                </div>
              )}
            </div>

            {/* Street details */}
            <div className="form-group">
              <label className="form-label">Street Name</label>
              <input className="form-input" value={form.name} onChange={e => setField('name', e.target.value)}
                placeholder="e.g. Park Lane" required />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Buy Price (£)</label>
                <input className="form-input" type="number" inputMode="numeric" step="1" min="1"
                  value={form.price} onChange={e => setField('price', e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Rent (£)</label>
                <input className="form-input" type="number" inputMode="numeric" step="1" min="1"
                  value={form.rentalPrice} onChange={e => setField('rentalPrice', e.target.value)} required />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Colour Group</label>
              <div className="colour-picker-grid">
                {COLOURS.map(c => (
                  <button
                    key={c}
                    type="button"
                    onClick={() => setField('colour', c)}
                    className={`option-tile ${form.colour === c ? 'is-selected' : ''}`}
                    aria-pressed={form.colour === c}
                  >
                    <span style={{
                      width: 24, height: 24, borderRadius: 6,
                      background: `var(--${c})`,
                      border: '2px solid rgba(0,0,0,0.1)',
                      display: 'block'
                    }} />
                    <span style={{ fontSize: 10, fontWeight: 600, color: 'var(--text-muted)' }}>
                      {COLOUR_LABELS[c].replace(' ', '\n')}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            <div className="card-actions" style={{ marginTop: 20 }}>
              <button type="button" className="btn btn-secondary btn-block" onClick={cancelEdit}>Cancel</button>
              <button type="submit" className="btn btn-green btn-block" disabled={saving}>
                {saving ? (uploading ? 'Uploading photo...' : 'Saving...') : (editingStreet ? 'Save Changes' : 'Add Street')}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ===== STREET LIST (mobile card layout) ===== */}
      {(!map.streets || map.streets.length === 0) ? (
        !editing && (
          <div className="empty-state-card">
            <div className="empty-state-icon">🏘️</div>
            <p>No streets yet. Go to a street location and tap Add to get started.</p>
          </div>
        )
      ) : (
        <div className="card-grid">
          {map.streets.map(s => (
            <div key={s.id} className="card" style={{ padding: 0, overflow: 'hidden' }}>
              {/* Colour bar */}
              <div style={{ height: 5, background: `var(--${s.colour})` }} />

              {/* Image clue thumbnail */}
              {s.imageClueUrl && (
                <img src={s.imageClueUrl} alt="Clue"
                  style={{ width: '100%', height: 120, objectFit: 'cover' }}
                  onError={(e) => e.target.style.display = 'none'} />
              )}

              <div style={{ padding: 14 }}>
                <div className="card-header-row" style={{ alignItems: 'start' }}>
                  <div>
                    <div className="card-title" style={{ fontSize: 16, marginBottom: 0 }}>{s.name}</div>
                    <div className="card-meta" style={{ marginTop: 2 }}>
                      Buy £{parseFloat(s.price).toFixed(0)} · Rent £{parseFloat(s.rentalPrice).toFixed(0)}
                    </div>
                  </div>
                  <div className="card-actions" style={{ gap: 4, flexShrink: 0 }}>
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(s)}>Edit</button>
                    <button className="btn btn-danger btn-sm btn-icon" onClick={() => handleDelete(s.id)}>✕</button>
                  </div>
                </div>
                <div className="muted" style={{ fontSize: 11, marginTop: 6 }}>
                  📍 {s.latitude.toFixed(5)}, {s.longitude.toFixed(5)}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
