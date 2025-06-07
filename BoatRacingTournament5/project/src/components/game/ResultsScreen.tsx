import React from 'react';
import { motion } from 'framer-motion';
import { Trophy, Clock, ArrowRight } from 'lucide-react';
import { useTournament } from '../../context/TournamentContext';
import { useBoatSelection } from '../../context/BoatSelectionContext';
import { getBoatIcon } from '../../data/boats';

interface ResultsScreenProps {
  onContinue: () => void;
}

const ResultsScreen: React.FC<ResultsScreenProps> = ({ onContinue }) => {
  const { tournamentState } = useTournament();
  const { selectedBoat } = useBoatSelection();
  
  // Get the latest race results based on current stage
  const getLatestResults = () => {
    const { currentStage, currentRaceIndex, races } = tournamentState;
    
    switch (currentStage) {
      case 'qualifying':
        return races.qualifying[currentRaceIndex] || [];
      case 'quarterfinals':
        return races.quarterfinals[currentRaceIndex] || [];
      case 'semifinals':
        return races.semifinals[currentRaceIndex] || [];
      case 'finals':
        return races.finals || [];
      default:
        return [];
    }
  };
  
  const results = getLatestResults();
  
  // Get player result
  const playerResult = results.find(r => r.isPlayer);
  
  // Determine if player qualified (top 3)
  const playerQualified = playerResult ? playerResult.position <= 3 : false;
  
  const getPositionText = (position: number) => {
    if (position === 1) return '1st';
    if (position === 2) return '2nd';
    if (position === 3) return '3rd';
    return `${position}th`;
  };
  
  return (
    <div className="max-w-4xl mx-auto">
      <motion.div 
        className="card p-8 text-center"
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5 }}
      >
        <div className="mb-8">
          <h2 className="text-3xl font-bold text-blue-800 mb-4">Race Results</h2>
          <p className="text-blue-600">
            {playerQualified 
              ? 'Congratulations! You qualified for the next round!' 
              : 'You did not qualify for the next round. Better luck next time!'}
          </p>
        </div>
        
        {playerResult && (
          <motion.div 
            className="flex flex-col items-center mb-8"
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.3, duration: 0.5 }}
          >
            <div className="relative mb-4">
              <div className="h-24 w-24 flex items-center justify-center rounded-full bg-blue-50 mb-2">
                {selectedBoat && React.createElement(getBoatIcon(selectedBoat.image), { 
                  size: 64, 
                  style: { color: selectedBoat?.color },
                  className: "boat-shadow"
                })}
              </div>
              
              {playerResult.position <= 3 && (
                <div className="absolute -top-4 -right-4 bg-yellow-400 rounded-full w-12 h-12 flex items-center justify-center shadow-lg">
                  <Trophy size={24} className="text-white" />
                </div>
              )}
            </div>
            
            <h3 className="text-2xl font-bold mb-1">Your Position</h3>
            <div className="text-4xl font-bold text-blue-600 mb-2">
              {getPositionText(playerResult.position)}
            </div>
            <div className="flex items-center text-gray-600">
              <Clock size={16} className="mr-1" />
              <span>{playerResult.time.toFixed(1)}s</span>
            </div>
          </motion.div>
        )}
        
        <div className="mb-8">
          <h3 className="text-xl font-bold text-blue-800 mb-4">All Results</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {results.slice(0, 6).map((result) => (
              <motion.div 
                key={result.boatId}
                className={`p-4 rounded-lg ${
                  result.isPlayer ? 'bg-blue-100 border-2 border-blue-300' : 'bg-white shadow'
                }`}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 * result.position, duration: 0.3 }}
              >
                <div className="flex items-center mb-2">
                  <div className={`w-8 h-8 rounded-full ${
                    result.position === 1 ? 'bg-yellow-400' : 
                    result.position === 2 ? 'bg-gray-300' : 
                    result.position === 3 ? 'bg-amber-600' : 'bg-gray-100'
                  } flex items-center justify-center mr-3 text-white font-bold`}>
                    {result.position}
                  </div>
                  <div>
                    <div className="font-medium">{result.isPlayer ? 'You' : result.playerName}</div>
                    <div className="text-sm text-gray-600 flex items-center">
                      <Clock size={14} className="mr-1" />
                      {result.time.toFixed(1)}s
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
        
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.8 }}
        >
          <button 
            onClick={onContinue}
            className="btn btn-primary flex items-center justify-center mx-auto"
          >
            Continue <ArrowRight size={18} className="ml-2" />
          </button>
        </motion.div>
      </motion.div>
    </div>
  );
};

export default ResultsScreen;