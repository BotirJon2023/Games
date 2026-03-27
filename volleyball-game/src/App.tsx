import { useState, lazy, Suspense } from "react";

const VolleyballGame = lazy(() => import("./game/VolleyballGame"));

type GameMode = "menu" | "2player" | "vscomputer";

function Menu({ onSelect }: { onSelect: (mode: "2player" | "vscomputer") => void }) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center relative overflow-hidden"
      style={{
        background: "linear-gradient(180deg, #1a6fb5 0%, #4db8f5 50%, #87CEEB 70%, #f4c877 70%, #e8b65a 85%, #d4934a 100%)"
      }}
    >
      {/* Animated waves */}
      <div className="absolute bottom-0 left-0 right-0 h-40 overflow-hidden pointer-events-none">
        <svg viewBox="0 0 800 160" preserveAspectRatio="none" className="w-full h-full absolute bottom-0" style={{ animation: "wave 3s ease-in-out infinite" }}>
          <path fill="rgba(0,119,204,0.5)" d="M0,80 C200,40 400,120 600,80 C700,60 750,90 800,80 L800,160 L0,160 Z" />
        </svg>
        <svg viewBox="0 0 800 160" preserveAspectRatio="none" className="w-full h-full absolute bottom-0" style={{ animation: "wave 4s ease-in-out infinite reverse" }}>
          <path fill="rgba(0,100,180,0.35)" d="M0,100 C150,60 350,130 550,90 C650,70 720,100 800,90 L800,160 L0,160 Z" />
        </svg>
      </div>

      {/* Sun */}
      <div className="absolute top-8 right-20 w-20 h-20 rounded-full"
        style={{ background: "radial-gradient(circle, #FFE566 40%, rgba(255,200,50,0.4) 70%, transparent 100%)" }}
      />

      {/* Seagulls decoration */}
      <svg className="absolute top-16 left-24 opacity-60" width="60" height="20" viewBox="0 0 60 20">
        <path d="M0 10 Q10 2 20 10 Q30 2 40 10" stroke="#333" strokeWidth="2" fill="none" />
        <path d="M35 12 Q45 4 55 12" stroke="#333" strokeWidth="2" fill="none" />
      </svg>

      {/* Title */}
      <div className="relative z-10 text-center mb-10">
        <div className="text-7xl mb-2">🏐</div>
        <h1 className="text-5xl font-black text-white drop-shadow-lg mb-1" style={{ textShadow: "0 3px 12px rgba(0,0,0,0.4)" }}>
          Beach Volleyball
        </h1>
        <p className="text-xl text-yellow-200 font-semibold drop-shadow">Seaside Edition</p>
        <div className="mt-2 text-2xl">🌊 🏖️ 🌞</div>
      </div>

      {/* Mode selection */}
      <div className="relative z-10 flex flex-col gap-4 items-center">
        <button
          onClick={() => onSelect("2player")}
          className="group relative w-72 py-5 text-xl font-bold text-white rounded-2xl transition-all duration-200 overflow-hidden"
          style={{ background: "linear-gradient(135deg, #e74c3c, #c0392b)", boxShadow: "0 6px 20px rgba(231,76,60,0.5)" }}
        >
          <span className="relative z-10 flex items-center justify-center gap-3">
            <span className="text-2xl">👫</span>
            2 Players
          </span>
          <div className="absolute inset-0 bg-white opacity-0 group-hover:opacity-10 transition-opacity" />
        </button>

        <button
          onClick={() => onSelect("vscomputer")}
          className="group relative w-72 py-5 text-xl font-bold text-white rounded-2xl transition-all duration-200 overflow-hidden"
          style={{ background: "linear-gradient(135deg, #2980b9, #1a5f8a)", boxShadow: "0 6px 20px rgba(41,128,185,0.5)" }}
        >
          <span className="relative z-10 flex items-center justify-center gap-3">
            <span className="text-2xl">🤖</span>
            vs Computer
          </span>
          <div className="absolute inset-0 bg-white opacity-0 group-hover:opacity-10 transition-opacity" />
        </button>

        {/* Controls info */}
        <div className="mt-6 bg-black/30 backdrop-blur rounded-xl p-4 text-white text-sm max-w-sm text-center">
          <p className="font-bold mb-2 text-yellow-300">Controls</p>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="bg-white/10 rounded p-2">
              <p className="font-semibold text-red-300">Player 1 (Left)</p>
              <p>A / D — Move</p>
              <p>W — Jump</p>
            </div>
            <div className="bg-white/10 rounded p-2">
              <p className="font-semibold text-blue-300">Player 2 (Right)</p>
              <p>← / → — Move</p>
              <p>↑ — Jump</p>
            </div>
          </div>
          <p className="mt-2 text-yellow-200">First to 7 points wins!</p>
        </div>
      </div>

      <style>{`
        @keyframes wave {
          0%, 100% { transform: translateX(0); }
          50% { transform: translateX(-30px); }
        }
      `}</style>
    </div>
  );
}

export default function App() {
  const [mode, setMode] = useState<GameMode>("menu");

  return (
    <>
      {mode === "menu" ? (
        <Menu onSelect={(m) => setMode(m)} />
      ) : (
        <div
          className="min-h-screen flex flex-col items-center justify-center py-4"
          style={{
            background: "linear-gradient(135deg, #0d3b6e 0%, #1a6fb5 40%, #2196F3 100%)"
          }}
        >
          <h2 className="text-white font-bold text-2xl mb-4 drop-shadow">
            🏐 Beach Volleyball — {mode === "2player" ? "2 Players" : "vs Computer"}
          </h2>
          <Suspense fallback={<div className="text-white text-xl">Loading game...</div>}>
            <VolleyballGame mode={mode} onBack={() => setMode("menu")} />
          </Suspense>
        </div>
      )}
    </>
  );
}
