import React, { useState, useRef, useEffect } from 'react';
import RacingGame from './components/RacingGame';

function App() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800">
      <RacingGame />
    </div>
  );
}

export default App;
