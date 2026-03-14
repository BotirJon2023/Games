function App() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-sky-300 to-sky-100 overflow-hidden relative">
      <div className="absolute inset-0 flex items-center justify-center">
        <svg viewBox="0 0 120 300" width="300" height="750" className="walking-character">
          <defs>
            <linearGradient id="skinGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#f4c4a0" />
              <stop offset="50%" stopColor="#e8b896" />
              <stop offset="100%" stopColor="#d4a080" />
            </linearGradient>
            <linearGradient id="hairGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#8b6f47" />
              <stop offset="100%" stopColor="#5a4a2c" />
            </linearGradient>
            <linearGradient id="topGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#ff6b9d" />
              <stop offset="50%" stopColor="#ff5087" />
              <stop offset="100%" stopColor="#e63b70" />
            </linearGradient>
            <linearGradient id="pantsGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="#2d5a7b" />
              <stop offset="50%" stopColor="#1f4256" />
              <stop offset="100%" stopColor="#162d3d" />
            </linearGradient>
          </defs>

          <g className="walking-group">
            {/* Hair */}
            <ellipse cx="60" cy="25" rx="28" ry="32" fill="url(#hairGradient)" className="hair-element" />

            {/* Head */}
            <circle cx="60" cy="35" r="22" fill="url(#skinGradient)" />

            {/* Ears */}
            <ellipse cx="38" cy="38" rx="8" ry="12" fill="url(#skinGradient)" />
            <ellipse cx="82" cy="38" rx="8" ry="12" fill="url(#skinGradient)" />

            {/* Neck */}
            <rect x="52" y="52" width="16" height="12" fill="url(#skinGradient)" rx="3" />

            {/* Torso/Top */}
            <path d="M 40 65 Q 35 75 36 95 L 84 95 Q 85 75 80 65 Z" fill="url(#topGradient)" />

            {/* Chest detail */}
            <ellipse cx="60" cy="80" rx="16" ry="14" fill="rgba(255,80,135,0.3)" />

            {/* Arms - Left (back, more transparent) */}
            <g className="arm-left-group">
              <ellipse cx="32" cy="72" rx="9" ry="18" fill="url(#skinGradient)" className="arm-segment" />
              <ellipse cx="28" cy="95" rx="8" ry="16" fill="url(#skinGradient)" className="arm-segment" />
              <circle cx="26" cy="112" r="6" fill="url(#skinGradient)" />
            </g>

            {/* Arms - Right (front, more visible) */}
            <g className="arm-right-group">
              <ellipse cx="88" cy="72" rx="9" ry="18" fill="url(#skinGradient)" className="arm-segment" />
              <ellipse cx="92" cy="95" rx="8" ry="16" fill="url(#skinGradient)" className="arm-segment" />
              <circle cx="94" cy="112" r="6" fill="url(#skinGradient)" />
            </g>

            {/* Hips/Waist */}
            <path d="M 38 95 Q 35 105 38 120 L 82 120 Q 85 105 82 95 Z" fill="url(#pantsGradient)" />

            {/* Left Leg (back) */}
            <g className="leg-left-group">
              <rect x="42" y="120" width="12" height="48" fill="url(#pantsGradient)" rx="6" className="thigh-left" />
              <rect x="43" y="168" width="10" height="45" fill="url(#skinGradient)" rx="5" className="calf-left" />
              <ellipse cx="48" cy="215" rx="8" ry="6" fill="#3d2817" />
            </g>

            {/* Right Leg (front) */}
            <g className="leg-right-group">
              <rect x="66" y="120" width="12" height="48" fill="url(#pantsGradient)" rx="6" className="thigh-right" />
              <rect x="67" y="168" width="10" height="45" fill="url(#skinGradient)" rx="5" className="calf-right" />
              <ellipse cx="72" cy="215" rx="8" ry="6" fill="#3d2817" />
            </g>

            {/* Eyes */}
            <circle cx="54" cy="32" r="2" fill="#333" />
            <circle cx="66" cy="32" r="2" fill="#333" />

            {/* Blush */}
            <circle cx="45" cy="42" r="3" fill="rgba(255,100,120,0.4)" />
            <circle cx="75" cy="42" r="3" fill="rgba(255,100,120,0.4)" />

            {/* Smile */}
            <path d="M 54 48 Q 60 52 66 48" stroke="#333" strokeWidth="1.5" fill="none" strokeLinecap="round" />
          </g>
        </svg>
      </div>

      <div className="ground"></div>
    </div>
  );
}

export default App;
