import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { GameScreen } from './GameScreen.jsx';
import './styles.css';

function Home() {
  return (
    <div className="center-screen">
      <div className="hero-icon">🏙️</div>
      <h1 className="hero-title">Street Monopoly</h1>
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
