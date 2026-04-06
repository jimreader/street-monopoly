import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link, useLocation } from 'react-router-dom';
import { Auth0Provider, useAuth0 } from '@auth0/auth0-react';
import { MapsPage } from './pages/MapsPage.jsx';
import { MapDetailPage } from './pages/MapDetailPage.jsx';
import { GamesPage } from './pages/GamesPage.jsx';
import { GameDetailPage } from './pages/GameDetailPage.jsx';
import { CreateGamePage } from './pages/CreateGamePage.jsx';
import { useApiTokenProvider } from './hooks/useApiTokenProvider.js';
import './styles.css';

function Nav() {
  const location = useLocation();
  const { user, logout } = useAuth0();
  const isActive = (path) => location.pathname.startsWith(path);

  return (
    <nav className="nav">
      <Link to="/" className="nav-brand">
        <img src="/logo.svg" alt="Road Rush" style={{ height: 26 }} />
        <span className="brand-badge">ADMIN</span>
      </Link>
      <div className="nav-links">
        <Link to="/maps" className={`nav-link ${isActive('/maps') ? 'active' : ''}`}>Maps</Link>
        <Link to="/games" className={`nav-link ${isActive('/games') ? 'active' : ''}`}>Games</Link>
        <div className="nav-user">
          {user?.picture && <img src={user.picture} alt="" className="nav-avatar" />}
          <span className="nav-username">{user?.name || user?.email}</span>
          <button className="btn btn-secondary btn-sm" onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
            Log out
          </button>
        </div>
      </div>
    </nav>
  );
}

/** Wraps routes that require authentication */
function RequireAuth({ children }) {
  const { isAuthenticated, isLoading, loginWithRedirect } = useAuth0();
  useApiTokenProvider();

  if (isLoading) {
    return (
      <div className="center-screen">
        <div className="loading-spinner" />
        <p style={{ color: 'var(--text-muted)', marginTop: 16 }}>Checking authentication...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    loginWithRedirect();
    return (
      <div className="center-screen">
        <div className="loading-spinner" />
        <p style={{ color: 'var(--text-muted)', marginTop: 16 }}>Redirecting to login...</p>
      </div>
    );
  }

  return children;
}

function AppRoutes() {
  return (
    <RequireAuth>
      <Nav />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<GamesPage />} />
          <Route path="/maps" element={<MapsPage />} />
          <Route path="/maps/:id" element={<MapDetailPage />} />
          <Route path="/games" element={<GamesPage />} />
          <Route path="/games/create" element={<CreateGamePage />} />
          <Route path="/games/:id" element={<GameDetailPage />} />
        </Routes>
      </main>
    </RequireAuth>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/*" element={<AppRoutes />} />
      </Routes>
    </BrowserRouter>
  );
}

const domain = import.meta.env.VITE_AUTH0_DOMAIN;
const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID;
const audience = import.meta.env.VITE_AUTH0_AUDIENCE;

if (!domain || !clientId) {
  console.error(
    'Auth0 configuration missing. Create a .env file with VITE_AUTH0_DOMAIN and VITE_AUTH0_CLIENT_ID. See .env.example.'
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <Auth0Provider
    domain={domain}
    clientId={clientId}
    authorizationParams={{
      redirect_uri: window.location.origin,
      audience: audience,
    }}
  >
    <App />
  </Auth0Provider>
);
