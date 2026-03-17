import { Gamepad2, Bot } from 'lucide-react';

interface MenuProps {
  onSelectMode: (mode: 'pvp' | 'pvc') => void;
}

export function Menu({ onSelectMode }: MenuProps) {
  return (
    <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-b from-blue-400 to-blue-600">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-white mb-4 drop-shadow-lg" style={{ fontFamily: 'monospace' }}>
          MINECRAFT VOLLEYBALL
        </h1>
        <p className="text-xl text-white mb-12 drop-shadow">Select Game Mode</p>

        <div className="flex gap-6 justify-center">
          <button
            onClick={() => onSelectMode('pvp')}
            className="bg-green-600 hover:bg-green-700 text-white px-8 py-6 rounded-lg font-bold text-xl transition-all transform hover:scale-105 shadow-lg flex items-center gap-3"
          >
            <Gamepad2 size={32} />
            <div className="text-left">
              <div>2 PLAYERS</div>
              <div className="text-sm font-normal opacity-90">Player vs Player</div>
            </div>
          </button>

          <button
            onClick={() => onSelectMode('pvc')}
            className="bg-orange-600 hover:bg-orange-700 text-white px-8 py-6 rounded-lg font-bold text-xl transition-all transform hover:scale-105 shadow-lg flex items-center gap-3"
          >
            <Bot size={32} />
            <div className="text-left">
              <div>vs COMPUTER</div>
              <div className="text-sm font-normal opacity-90">Player vs AI</div>
            </div>
          </button>
        </div>

        <div className="mt-12 text-white text-sm space-y-2">
          <div className="font-bold text-lg mb-3">CONTROLS</div>
          <div className="grid grid-cols-2 gap-8 max-w-2xl mx-auto">
            <div className="bg-white/20 rounded-lg p-4">
              <div className="font-bold mb-2">PLAYER 1 (Blue)</div>
              <div>A / D - Move</div>
              <div>W - Jump</div>
              <div>SPACE - Serve</div>
            </div>
            <div className="bg-white/20 rounded-lg p-4">
              <div className="font-bold mb-2">PLAYER 2 (Red)</div>
              <div>← / → - Move</div>
              <div>↑ - Jump</div>
              <div>SPACE - Serve</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
