import React, { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { Flag, Clock, AlertTriangle } from 'lucide-react';
import { useBoatSelection } from '../../context/BoatSelectionContext';
import { useTournament } from '../../context/TournamentContext';
import { useWeather } from '../../context/WeatherContext';
import { getRandomTrack } from '../../data/tracks';
import { getBoatIcon } from '../../data/boats';
import { RaceStatus, RaceResult, BoatAnimationState, Obstacle } from '../../types/game';

interface RaceScreenProps {
  onRaceFinish: () => void;
}

// Generates AI opponents
const generateOpponents = (count: number): { id: string; name: string; boatId: string; color: string; }[] => {
  const opponents = [];
  const names = ['Captain Alex', 'Sea Wolf', 'Marina', 'Admiral Bob', 'Ocean Master', 'Wave Rider'];
  const boatIds = ['speedster', 'cruiser', 'agile', 'balanced', 'performer', 'titan'];
  const colors = ['#ef4444', '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#64748b'];
  
  for (let i = 0; i < count; i++) {
    opponents.push({
      id: `opponent-${i}`,
      name: names[i % names.length],
      boatId: boatIds[i % boatIds.length],
      color: colors[i % colors.length]
    });
  }
  return opponents;
};

const RaceScreen: React.FC<RaceScreenProps> = ({ onRaceFinish }) => {
  const { selectedBoat } = useBoatSelection();
  const { tournamentState, addRaceResult } = useTournament();
  const { currentWeather } = useWeather();
  
  const [raceStatus, setRaceStatus] = useState<RaceStatus>('preparing');
  const [countdown, setCountdown] = useState(3);
  const [raceTime, setRaceTime] = useState(0);
  const [raceTrack, setRaceTrack] = useState(getRandomTrack());
  const [opponents] = useState(generateOpponents(5));
  const [results, setResults] = useState<RaceResult[]>([]);
  
  // Boat positions and animations
  const [playerBoatState, setPlayerBoatState] = useState<BoatAnimationState>({
    position: { x: 0, y: 100 },
    rotation: 0,
    speed: 0,
    wobble: 0
  });

  const [opponentStates, setOpponentStates] = useState<Record<string, BoatAnimationState>>({});
  
  const [obstacles, setObstacles] = useState<Obstacle[]>(raceTrack.obstacles);
  const [collisions, setCollisions] = useState<string[]>([]);
  
  // Initialize race
  useEffect(() => {
    // Set initial positions for all boats
    const initialOpponentStates: Record<string, BoatAnimationState> = {};
    
    opponents.forEach((opponent, index) => {
      initialOpponentStates[opponent.id] = {
        position: { x: 0, y: 50 + (index + 1) * 30 },
        rotation: 0,
        speed: 0,
        wobble: 0
      };
    });
    
    setOpponentStates(initialOpponentStates);
    
    // Start the race preparation
    const prepTimer = setTimeout(() => {
      setRaceStatus('countdown');
    }, 3000);
    
    return () => clearTimeout(prepTimer);
  }, [opponents]);
  
  // Handle countdown
  useEffect(() => {
    if (raceStatus !== 'countdown') return;
    
    if (countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else {
      setRaceStatus('racing');
    }
  }, [countdown, raceStatus]);
  
  // Handle race timing
  useEffect(() => {
    if (raceStatus !== 'racing') return;
    
    const timer = setInterval(() => {
      setRaceTime(prev => prev + 0.1);
    }, 100);
    
    return () => clearInterval(timer);
  }, [raceStatus]);
  
  // Simulate boat movements
  const updateBoats = useCallback(() => {
    if (raceStatus !== 'racing') return;
    
    // Calculate player boat movement
    const playerBoatSpeed = selectedBoat?.stats.speed || 5;
    const playerAcceleration = selectedBoat?.stats.acceleration || 5;
    const playerHandling = selectedBoat?.stats.handling || 5;
    
    // Apply weather effects
    const weatherSpeedMultiplier = currentWeather.impact.speedMultiplier;
    const weatherHandlingMultiplier = currentWeather.impact.handlingMultiplier;
    
    // Update player boat
    setPlayerBoatState(prev => {
      // Calculate new speed (acceleration and weather factors)
      let newSpeed = prev.speed;
      if (newSpeed < playerBoatSpeed * weatherSpeedMultiplier) {
        newSpeed += (playerAcceleration / 50) * weatherSpeedMultiplier;
      }
      
      // Add some wobble for visual effect
      const newWobble = Math.sin(raceTime * 5) * (1 / (playerHandling * weatherHandlingMultiplier));
      
      // Calculate new position
      const newX = prev.position.x + newSpeed;
      const newY = prev.position.y + Math.sin(raceTime) * 0.5;
      
      return {
        position: { x: newX, y: newY },
        rotation: newWobble * 5, // Convert wobble to visual rotation
        speed: newSpeed,
        wobble: newWobble
      };
    });
    
    // Update opponent boats
    setOpponentStates(prev => {
      const updated: Record<string, BoatAnimationState> = {};
      
      opponents.forEach((opponent) => {
        const opponentState = prev[opponent.id];
        if (!opponentState) return;
        
        // Each opponent has slightly different behavior
        const opponentFactor = 0.8 + Math.random() * 0.4; // 0.8-1.2 random factor
        const baseSpeed = (4 + Math.random() * 6) * opponentFactor; // 4-10 base speed
        
        // Calculate new speed
        let newSpeed = opponentState.speed;
        if (newSpeed < baseSpeed * weatherSpeedMultiplier) {
          newSpeed += (0.1 + Math.random() * 0.1) * weatherSpeedMultiplier;
        }
        
        // Add wobble for visual effect
        const newWobble = Math.sin(raceTime * 3 + parseInt(opponent.id.split('-')[1])) * 0.2;
        
        // Calculate new position
        const newX = opponentState.position.x + newSpeed;
        const newY = opponentState.position.y + Math.sin(raceTime + parseInt(opponent.id.split('-')[1])) * 0.8;
        
        updated[opponent.id] = {
          position: { x: newX, y: newY },
          rotation: newWobble * 8,
          speed: newSpeed,
          wobble: newWobble
        };
      });
      
      return updated;
    });
    
    // Check for race completion (when boats reach the end of track)
    const trackLength = raceTrack.length;
    
    // Check player's position
    if (playerBoatState.position.x >= trackLength && !results.some(r => r.isPlayer)) {
      // Add player to results if not already added
      const playerPosition = results.length + 1;
      setResults(prev => [...prev, {
        boatId: selectedBoat?.id || '',
        playerName: 'Player',
        position: playerPosition,
        time: raceTime,
        isPlayer: true
      }]);
    }
    
    // Check opponents' positions
    opponents.forEach(opponent => {
      const opponentState = opponentStates[opponent.id];
      if (opponentState && opponentState.position.x >= trackLength && 
          !results.some(r => r.boatId === opponent.id)) {
        // Add opponent to results if not already added
        const opponentPosition = results.length + 1;
        setResults(prev => [...prev, {
          boatId: opponent.id,
          playerName: opponent.name,
          position: opponentPosition,
          time: raceTime,
          isPlayer: false
        }]);
      }
    });
    
    // Check if race is complete (all boats finished)
    if (results.length === opponents.length + 1) {
      setRaceStatus('finished');
      addRaceResult(results);
      
      // End the race after showing results briefly
      const finishTimer = setTimeout(() => {
        onRaceFinish();
      }, 3000);
      
      return () => clearTimeout(finishTimer);
    }
    
    // Check for collisions with obstacles
    const checkCollisions = () => {
      const newCollisions: string[] = [];
      
      // Function to check if a boat hit an obstacle
      const isCollision = (boatX: number, boatY: number, obstacle: Obstacle) => {
        const dx = boatX - obstacle.position.x;
        const dy = boatY - obstacle.position.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        return distance < obstacle.size;
      };
      
      // Check player boat
      obstacles.forEach((obstacle, index) => {
        if (isCollision(playerBoatState.position.x, playerBoatState.position.y, obstacle)) {
          newCollisions.push(`player-${index}`);
          
          // Apply obstacle effects
          if (obstacle.type === 'rock' || obstacle.type === 'buoy' || obstacle.type === 'debris') {
            // Slow down the player's boat
            setPlayerBoatState(prev => ({
              ...prev,
              speed: prev.speed * 0.7,
              wobble: prev.wobble + 0.3
            }));
          } else if (obstacle.type === 'currentBoost') {
            // Speed up the player's boat
            setPlayerBoatState(prev => ({
              ...prev,
              speed: prev.speed * 1.2
            }));
          } else if (obstacle.type === 'currentSlow') {
            // Slow down the player's boat
            setPlayerBoatState(prev => ({
              ...prev,
              speed: prev.speed * 0.8
            }));
          }
        }
      });
      
      // Check opponent boats
      opponents.forEach(opponent => {
        const opponentState = opponentStates[opponent.id];
        if (!opponentState) return;
        
        obstacles.forEach((obstacle, index) => {
          if (isCollision(opponentState.position.x, opponentState.position.y, obstacle)) {
            newCollisions.push(`${opponent.id}-${index}`);
            
            // Apply obstacle effects to opponents
            setOpponentStates(prev => {
              const updated = { ...prev };
              if (obstacle.type === 'rock' || obstacle.type === 'buoy' || obstacle.type === 'debris') {
                updated[opponent.id] = {
                  ...updated[opponent.id],
                  speed: updated[opponent.id].speed * 0.7,
                  wobble: updated[opponent.id].wobble + 0.3
                };
              } else if (obstacle.type === 'currentBoost') {
                updated[opponent.id] = {
                  ...updated[opponent.id],
                  speed: updated[opponent.id].speed * 1.2
                };
              } else if (obstacle.type === 'currentSlow') {
                updated[opponent.id] = {
                  ...updated[opponent.id],
                  speed: updated[opponent.id].speed * 0.8
                };
              }
              return updated;
            });
          }
        });
      });
      
      setCollisions(newCollisions);
    };
    
    checkCollisions();
  }, [
    raceStatus, 
    selectedBoat, 
    playerBoatState, 
    opponentStates, 
    opponents, 
    raceTime, 
    results, 
    raceTrack.length, 
    obstacles, 
    currentWeather, 
    addRaceResult, 
    onRaceFinish
  ]);
  
  // Animation frame loop
  useEffect(() => {
    if (raceStatus !== 'racing') return;
    
    let animationFrameId: number;
    
    const animate = () => {
      updateBoats();
      animationFrameId = requestAnimationFrame(animate);
    };
    
    animationFrameId = requestAnimationFrame(animate);
    
    return () => {
      if (animationFrameId) {
        cancelAnimationFrame(animationFrameId);
      }
    };
  }, [raceStatus, updateBoats]);
  
  // Render race UI
  return (
    <div className="flex flex-col h-full">
      {/* Race information header */}
      <div className="bg-white bg-opacity-80 rounded-lg p-4 mb-4 flex justify-between items-center">
        <div className="flex items-center">
          <div className="mr-6">
            <span className="text-sm text-gray-600 block">Track</span>
            <span className="font-medium">{raceTrack.name}</span>
          </div>
          
          <div className="mr-6">
            <span className="text-sm text-gray-600 block">Length</span>
            <span className="font-medium">{raceTrack.length}m</span>
          </div>
          
          <div>
            <span className="text-sm text-gray-600 block">Weather</span>
            <span className="font-medium capitalize">{currentWeather.type}</span>
          </div>
        </div>
        
        <div className="flex items-center">
          <div className="flex items-center mr-6">
            <Clock size={18} className="mr-1 text-blue-600" />
            <span className="font-mono font-medium">{raceTime.toFixed(1)}s</span>
          </div>
          
          {raceStatus === 'countdown' && (
            <div className="bg-blue-600 text-white font-bold text-xl w-10 h-10 flex items-center justify-center rounded-full">
              {countdown}
            </div>
          )}
          
          {raceStatus === 'preparing' && (
            <div className="flex items-center text-yellow-600">
              <AlertTriangle size={18} className="mr-1" />
              <span className="font-medium">Preparing race...</span>
            </div>
          )}
        </div>
      </div>
      
      {/* Race track */}
      <div 
        className="flex-grow relative overflow-hidden race-track rounded-lg"
        style={{ 
          background: raceTrack.background,
          height: '400px'
        }}
      >
        {/* Finish line */}
        <div 
          className="absolute top-0 bottom-0 border-r-4 border-dashed border-white"
          style={{ left: `${raceTrack.length}px`, height: '100%' }}
        >
          <div className="absolute top-4 -right-6">
            <Flag size={24} className="text-white flag-animation" />
          </div>
        </div>
        
        {/* Weather effects */}
        {currentWeather.type === 'rainy' && <div className="rain absolute inset-0" />}
        {currentWeather.type === 'stormy' && (
          <>
            <div className="rain absolute inset-0\" style={{ opacity: 0.6 }} />
            <div className="absolute inset-0 bg-gray-800 opacity-20" />
          </>
        )}
        
        {/* Player boat */}
        {selectedBoat && (
          <motion.div
            className="boat-container absolute"
            style={{
              left: playerBoatState.position.x,
              top: playerBoatState.position.y,
              zIndex: 10
            }}
          >
            <motion.div 
              className="boat"
              style={{
                rotate: playerBoatState.rotation,
                transition: 'transform 0.1s ease-out'
              }}
            >
              {React.createElement(getBoatIcon(selectedBoat.image), {
                size: 40,
                color: selectedBoat.color,
                className: "boat-shadow"
              })}
            </motion.div>
            <div className="boat-wake" />
          </motion.div>
        )}
        
        {/* Opponent boats */}
        {opponents.map((opponent) => {
          const opponentState = opponentStates[opponent.id];
          if (!opponentState) return null;
          
          return (
            <motion.div
              key={opponent.id}
              className="boat-container absolute"
              style={{
                left: opponentState.position.x,
                top: opponentState.position.y,
                zIndex: 5
              }}
            >
              <motion.div 
                className="boat"
                style={{
                  rotate: opponentState.rotation,
                  transition: 'transform 0.1s ease-out'
                }}
              >
                {React.createElement(getBoatIcon('Sailboat'), {
                  size: 36,
                  color: opponent.color,
                  className: "boat-shadow"
                })}
              </motion.div>
              <div className="boat-wake" />
            </motion.div>
          );
        })}
        
        {/* Obstacles */}
        {obstacles.map((obstacle, index) => {
          let color = 'rgba(255, 255, 255, 0.5)';
          let content = null;
          
          switch (obstacle.type) {
            case 'rock':
              color = 'rgba(120, 120, 120, 0.8)';
              break;
            case 'buoy':
              color = 'rgba(255, 60, 60, 0.8)';
              break;
            case 'debris':
              color = 'rgba(165, 130, 100, 0.8)';
              break;
            case 'currentBoost':
              color = 'rgba(100, 200, 255, 0.4)';
              content = '↑';
              break;
            case 'currentSlow':
              color = 'rgba(30, 90, 150, 0.4)';
              content = '↓';
              break;
          }
          
          return (
            <div
              key={`obstacle-${index}`}
              className="absolute rounded-full flex items-center justify-center text-white font-bold"
              style={{
                left: obstacle.position.x,
                top: obstacle.position.y,
                width: obstacle.size * 2,
                height: obstacle.size * 2,
                backgroundColor: color,
                transform: 'translate(-50%, -50%)'
              }}
            >
              {content}
            </div>
          );
        })}
        
        {/* Collision effects */}
        {collisions.map((collision, index) => (
          <div
            key={`collision-${index}`}
            className="water-splash"
            style={{
              left: collision.includes('player') 
                ? playerBoatState.position.x 
                : opponentStates[collision.split('-')[0]]?.position.x || 0,
              top: collision.includes('player') 
                ? playerBoatState.position.y 
                : opponentStates[collision.split('-')[0]]?.position.y || 0,
              width: '30px',
              height: '30px',
              transform: 'translate(-50%, -50%)'
            }}
          />
        ))}
      </div>
      
      {/* Race positions sidebar */}
      <div className="bg-white bg-opacity-80 rounded-lg p-4 mt-4">
        <h3 className="font-medium mb-2">Race Positions</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
          {results.length > 0 ? (
            // Show finished positions
            results.map((result) => (
              <div 
                key={result.boatId}
                className={`flex items-center p-2 rounded ${
                  result.isPlayer ? 'bg-blue-100' : 'bg-gray-50'
                }`}
              >
                <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center mr-2">
                  {result.position}
                </div>
                <div>
                  <div className="font-medium">{result.isPlayer ? 'You' : result.playerName}</div>
                  <div className="text-sm text-gray-600">{result.time.toFixed(1)}s</div>
                </div>
              </div>
            ))
          ) : (
            // Show current race order based on x position
            [...(selectedBoat ? [{
              id: 'player',
              name: 'You',
              position: playerBoatState.position,
              isPlayer: true
            }] : []), 
            ...opponents.map(o => ({
              id: o.id,
              name: o.name,
              position: opponentStates[o.id]?.position || { x: 0, y: 0 },
              isPlayer: false
            }))].sort((a, b) => b.position.x - a.position.x)
            .map((boat, index) => (
              <div 
                key={boat.id}
                className={`flex items-center p-2 rounded ${
                  boat.isPlayer ? 'bg-blue-100' : 'bg-gray-50'
                }`}
              >
                <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center mr-2">
                  {index + 1}
                </div>
                <div className="font-medium">{boat.name}</div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default RaceScreen;