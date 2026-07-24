import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { GameScreen } from './GameScreen.jsx';
import { JoinGamePage } from './JoinGamePage.jsx';
import './styles.css';

function Home() {
  return (
    <div className="app-shell app-shell--player">
      <div className="app-brand-band">
        <img
          src="/logo.svg"
          alt="Road Rush"
          style={{ height: 42, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.2))' }}
        />
      </div>
      <a href="#app-main" className="skip-link">Skip to content</a>
      <main id="app-main" tabIndex={-1} className="center-screen" style={{ minHeight: 'calc(100dvh - 66px)' }}>
        <p className="hero-sub">Use the link from your invitation email, then tap Join on the next screen.</p>
      </main>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <div className="app-shell app-shell--player">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/game/:joinToken" element={<JoinGamePage />} />
          <Route path="/game/:joinToken/join" element={<JoinGamePage />} />
          <Route path="/game/:joinToken/play" element={<GameScreen />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
