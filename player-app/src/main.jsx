import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { GameScreen } from './GameScreen.jsx';
import './styles.css';

function Home() {
  return (
    <div className="center-screen">
      <img src="/logo.svg" alt="Road Rush" style={{ height: 44, marginBottom: 20, filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.15))' }} />
      <p className="hero-sub">Use the link from your invitation email to join a game.</p>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/game/:joinToken" element={<GameScreen />} />
      </Routes>
    </BrowserRouter>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
