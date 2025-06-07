import React from 'react';
import { motion } from 'framer-motion';
import { Flag, Award, Trophy, ArrowRight } from 'lucide-react';
import { TournamentStage } from '../../types/game';
import { useTournament } from '../../context/TournamentContext';
import { useBoatSelection } from '../../context/BoatSelectionContext';
import { useWeather } from '../../context/WeatherContext';
import { getBoatIcon } from '../../data/boats';
import { tracks } from '../../data/tracks';

interface TournamentScreenProps {
  onStartRace: () => void;
  currentStage: TournamentStage;
  currentRaceIndex: number;
}

const TournamentScreen: React.FC<TournamentScreenProps> = ({ 
  onStartRace,
  currentStage,
  currentRaceIndex
}) => {
  const { tournamentState } = useTournament();
  const { selectedBoat } = useBoatSelection();
  const { currentWeather, changeWeather } = useWeather();
  
  const getTournamentProgress = (): number => {
    let progress = 0;
    
    switch (currentStage) {
      case 'qualifying':
        progress = (currentRaceIndex / 2) * 25;
        break;
      case 'quarterfinals':
        progress = 25 + (currentRaceIndex / 1) * 25;
        break;
      case 'semifinals':
        progress = 50 + (currentRaceIndex / 1) * 25;
        break;
      case 'finals':
        progress = 75 + (currentRaceIndex / 1) * 25;
        break;
    }
    
    return progress;
  };
  
  const getStageTitle = (stage: TournamentStage): string => {
    switch (stage) {
      case 'qualifying': return 'Qualifying Round';
      case 'quarterfinals': return 'Quarterfinals';
      case 'semifinals': return 'Semifinals';
      case 'finals': return 'Finals';
    }
  };
  
  const getCurrentTrack = () => {
    let track;
    
    switch (currentStage) {
      case 'qualifying':
        track = tracks.find(t => t.difficulty === 'easy');
        break;
      case 'quarterfinals':
        track = tracks.find(t => t.difficulty === 'medium');
        break;
      case 'semifinals':
        track = tracks.find(t => t.difficulty === 'medium');
        break;
      case 'finals':
        track = tracks.find(t => t.difficulty === 'hard');
        break;
    }
    
    return track || tracks[0];
  };
  
  const handleStartRace = () => {
    // Randomly change weather before race
    if (Math.random() < 0.3) {
      changeWeather();
    }
    onStartRace();
  };
  
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <div className="lg:col-span-2">
        <div className="card p-6 mb-6">
          <h2 className="text-xl font-bold text-blue-800 mb-4">Tournament Progress</h2>
          
          <div className="relative pt-1 mb-6">
            <div className="flex items-center justify-between mb-2">
              <div>
                <span className="text-xs font-semibold inline-block py-1 px-2 uppercase rounded-full text-blue-600 bg-blue-100">
                  {getStageTitle(currentStage)}
                </span>
              </div>
              <div className="text-right">
                <span className="text-xs font-semibold inline-block text-blue-600">
                  {getTournamentProgress()}%
                </span>
              </div>
            </div>
            <div className="h-2 rounded-full bg-gray-200">
              <motion.div 
                className="h-2 rounded-full bg-blue-600"
                initial={{ width: 0 }}
                animate={{ width: `${getTournamentProgress()}%` }}
                transition={{ duration: 0.5 }}
              />
            </div>
          </div>
          
          <div className="grid grid-cols-4 gap-4 mb-6">
            <div className={`p-4 rounded-lg border-2 ${currentStage === 'qualifying' ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}>
              <div className="flex items-center justify-center mb-2">
                <Flag size={20} className={currentStage === 'qualifying' ? 'text-blue-500' : 'text-gray-400'} />
              </div>
              <h3 className="text-center text-sm font-medium">Qualifying</h3>
              <p className="text-center text-xs text-gray-500">2 Races</p>
            </div>
            
            <div className={`p-4 rounded-lg border-2 ${currentStage === 'quarterfinals' ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}>
              <div className="flex items-center justify-center mb-2">
                <Award size={20} className={currentStage === 'quarterfinals' ? 'text-blue-500' : 'text-gray-400'} />
              </div>
              <h3 className="text-center text-sm font-medium">Quarterfinals</h3>
              <p className="text-center text-xs text-gray-500">1 Race</p>
            </div>
            
            <div className={`p-4 rounded-lg border-2 ${currentStage === 'semifinals' ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}>
              <div className="flex items-center justify-center mb-2">
                <Award size={20} className={currentStage === 'semifinals' ? 'text-blue-500' : 'text-gray-400'} />
              </div>
              <h3 className="text-center text-sm font-medium">Semifinals</h3>
              <p className="text-center text-xs text-gray-500">1 Race</p>
            </div>
            
            <div className={`p-4 rounded-lg border-2 ${currentStage === 'finals' ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}>
              <div className="flex items-center justify-center mb-2">
                <Trophy size={20} className={currentStage === 'finals' ? 'text-blue-500' : 'text-gray-400'} />
              </div>
              <h3 className="text-center text-sm font-medium">Finals</h3>
              <p className="text-center text-xs text-gray-500">1 Race</p>
            </div>
          </div>
          
          <div className="bg-blue-50 rounded-lg p-4 border border-blue-100">
            <h3 className="text-lg font-medium text-blue-800 mb-2">
              {getStageTitle(currentStage)} - Race {currentRaceIndex + 1}
            </h3>
            
            <div className="flex flex-col md:flex-row md:items-center justify-between">
              <div className="mb-4 md:mb-0">
                <p className="text-sm text-blue-600 mb-1">Track: {getCurrentTrack().name}</p>
                <p className="text-sm text-blue-600 mb-1">
                  Weather: <span className="capitalize">{currentWeather.type}</span>
                </p>
                <p className="text-sm text-blue-600">
                  Difficulty: <span className="capitalize">{getCurrentTrack().difficulty}</span>
                </p>
              </div>
              
              <button 
                onClick={handleStartRace}
                className="btn btn-primary flex items-center justify-center"
              >
                Start Race <ArrowRight size={18} className="ml-2" />
              </button>
            </div>
          </div>
        </div>
        
        {/* Past Race Results */}
        {Object.entries(tournamentState.races).some(([stage, races]) => races.length > 0) && (
          <div className="card p-6">
            <h2 className="text-xl font-bold text-blue-800 mb-4">Past Race Results</h2>
            
            <div className="space-y-4">
              {/* Qualifying Results */}
              {tournamentState.races.qualifying.length > 0 && (
                <div>
                  <h3 className="font-medium text-blue-700 mb-2">Qualifying</h3>
                  <div className="space-y-2">
                    {tournamentState.races.qualifying.map((raceResults, index) => (
                      <div key={`qualifying-${index}`} className="bg-gray-50 rounded-lg p-3">
                        <div className="text-sm font-medium mb-2">Race {index + 1}</div>
                        <div className="grid grid-cols-2 gap-2">
                          {raceResults.slice(0, 3).map((result) => (
                            <div 
                              key={result.boatId} 
                              className={`flex items-center p-2 rounded ${result.isPlayer ? 'bg-blue-100' : ''}`}
                            >
                              <div className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center mr-2 text-xs">
                                {result.position}
                              </div>
                              <div className="text-sm">{result.isPlayer ? 'You' : result.playerName}</div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              
              {/* Quarterfinals Results */}
              {tournamentState.races.quarterfinals.length > 0 && (
                <div>
                  <h3 className="font-medium text-blue-700 mb-2">Quarterfinals</h3>
                  <div className="space-y-2">
                    {tournamentState.races.quarterfinals.map((raceResults, index) => (
                      <div key={`quarterfinals-${index}`} className="bg-gray-50 rounded-lg p-3">
                        <div className="grid grid-cols-2 gap-2">
                          {raceResults.slice(0, 3).map((result) => (
                            <div 
                              key={result.boatId} 
                              className={`flex items-center p-2 rounded ${result.isPlayer ? 'bg-blue-100' : ''}`}
                            >
                              <div className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center mr-2 text-xs">
                                {result.position}
                              </div>
                              <div className="text-sm">{result.isPlayer ? 'You' : result.playerName}</div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              
              {/* Semifinals Results */}
              {tournamentState.races.semifinals.length > 0 && (
                <div>
                  <h3 className="font-medium text-blue-700 mb-2">Semifinals</h3>
                  <div className="space-y-2">
                    {tournamentState.races.semifinals.map((raceResults, index) => (
                      <div key={`semifinals-${index}`} className="bg-gray-50 rounded-lg p-3">
                        <div className="grid grid-cols-2 gap-2">
                          {raceResults.slice(0, 3).map((result) => (
                            <div 
                              key={result.boatId} 
                              className={`flex items-center p-2 rounded ${result.isPlayer ? 'bg-blue-100' : ''}`}
                            >
                              <div className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center mr-2 text-xs">
                                {result.position}
                              </div>
                              <div className="text-sm">{result.isPlayer ? 'You' : result.playerName}</div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
      
      <div>
        {/* Your Boat */}
        {selectedBoat && (
          <motion.div 
            className="card p-6 mb-6"
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.3 }}
          >
            <h2 className="text-xl font-bold text-blue-800 mb-4">Your Boat</h2>
            
            <div className="flex flex-col items-center mb-6">
              <div className="h-24 w-24 flex items-center justify-center mb-4 rounded-full bg-blue-50">
                {React.createElement(getBoatIcon(selectedBoat.image), { 
                  size: 64, 
                  style: { color: selectedBoat.color },
                  className: "boat-shadow"
                })}
              </div>
              
              <h3 className="text-2xl font-bold">{selectedBoat.name}</h3>
            </div>
            
            <div className="space-y-4 mb-6">
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Speed</span>
                  <span>{selectedBoat.stats.speed}/10</span>
                </div>
                <div className="stat-bar">
                  <div 
                    className="stat-bar-fill"
                    style={{ 
                      width: `${selectedBoat.stats.speed * 10}%`,
                      backgroundColor: selectedBoat.color
                    }}
                  />
                </div>
              </div>
              
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Acceleration</span>
                  <span>{selectedBoat.stats.acceleration}/10</span>
                </div>
                <div className="stat-bar">
                  <div 
                    className="stat-bar-fill"
                    style={{ 
                      width: `${selectedBoat.stats.acceleration * 10}%`,
                      backgroundColor: selectedBoat.color
                    }}
                  />
                </div>
              </div>
              
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Handling</span>
                  <span>{selectedBoat.stats.handling}/10</span>
                </div>
                <div className="stat-bar">
                  <div 
                    className="stat-bar-fill"
                    style={{ 
                      width: `${selectedBoat.stats.handling * 10}%`,
                      backgroundColor: selectedBoat.color
                    }}
                  />
                </div>
              </div>
              
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Durability</span>
                  <span>{selectedBoat.stats.durability}/10</span>
                </div>
                <div className="stat-bar">
                  <div 
                    className="stat-bar-fill"
                    style={{ 
                      width: `${selectedBoat.stats.durability * 10}%`,
                      backgroundColor: selectedBoat.color
                    }}
                  />
                </div>
              </div>
            </div>
          </motion.div>
        )}
        
        {/* Weather Forecast */}
        <div className="card p-6">
          <h2 className="text-xl font-bold text-blue-800 mb-4">Weather Forecast</h2>
          
          <div className="flex flex-col items-center mb-4">
            <div className="h-16 w-16 flex items-center justify-center mb-2 rounded-full bg-blue-50">
              {currentWeather.type === 'sunny' && (
                <span className="text-3xl">☀️</span>
              )}
              {currentWeather.type === 'cloudy' && (
                <span className="text-3xl">☁️</span>
              )}
              {currentWeather.type === 'rainy' && (
                <span className="text-3xl">🌧️</span>
              )}
              {currentWeather.type === 'stormy' && (
                <span className="text-3xl">⛈️</span>
              )}
            </div>
            
            <h3 className="text-xl font-bold capitalize">{currentWeather.type}</h3>
          </div>
          
          <div className="space-y-4">
            <div>
              <div className="flex justify-between mb-1 text-sm font-medium">
                <span>Wind Speed</span>
                <span>{currentWeather.windSpeed} knots</span>
              </div>
              <div className="stat-bar">
                <div 
                  className="stat-bar-fill bg-blue-400"
                  style={{ width: `${(currentWeather.windSpeed / 20) * 100}%` }}
                />
              </div>
            </div>
            
            <div>
              <div className="flex justify-between mb-1 text-sm font-medium">
                <span>Visibility</span>
                <span>{currentWeather.visibility}%</span>
              </div>
              <div className="stat-bar">
                <div 
                  className="stat-bar-fill bg-blue-400"
                  style={{ width: `${currentWeather.visibility}%` }}
                />
              </div>
            </div>
            
            <div className="bg-blue-50 p-3 rounded-lg mt-4">
              <h4 className="font-medium text-blue-800 mb-1">Weather Effects</h4>
              <p className="text-sm text-blue-600">
                Speed: {(currentWeather.impact.speedMultiplier * 100).toFixed(0)}% of normal
              </p>
              <p className="text-sm text-blue-600">
                Handling: {(currentWeather.impact.handlingMultiplier * 100).toFixed(0)}% of normal
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TournamentScreen;