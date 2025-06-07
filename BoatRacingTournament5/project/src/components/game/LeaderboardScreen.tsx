import React from 'react';
import { motion } from 'framer-motion';
import { Trophy, Medal, ArrowRight, RotateCw } from 'lucide-react';
import { useTournament } from '../../context/TournamentContext';

interface LeaderboardScreenProps {
  onPlayAgain: () => void;
}

const LeaderboardScreen: React.FC<LeaderboardScreenProps> = ({ onPlayAgain }) => {
  const { tournamentState, resetTournament } = useTournament();
  
  // Calculate final standings from finals
  const finalResults = tournamentState.races.finals || [];
  
  // Generate final rankings with score points
  const leaderboard = finalResults.map((result, index) => ({
    name: result.isPlayer ? 'You' : result.playerName,
    position: result.position,
    time: result.time,
    isPlayer: result.isPlayer,
    points: result.position === 1 ? 100 : 
            result.position === 2 ? 80 : 
            result.position === 3 ? 60 : 
            Math.max(50 - ((result.position - 4) * 10), 10)
  }));
  
  const handlePlayAgain = () => {
    resetTournament();
    onPlayAgain();
  };
  
  return (
    <div className="max-w-4xl mx-auto">
      <motion.div 
        className="card p-8"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-blue-800 mb-2">Tournament Results</h2>
          <p className="text-blue-600">
            {leaderboard.find(r => r.isPlayer)?.position === 1 
              ? 'Congratulations! You are the tournament champion!' 
              : 'Tournament completed! See the final standings below.'}
          </p>
        </div>
        
        {/* Top 3 Podium */}
        <div className="flex flex-col md:flex-row items-end justify-center gap-4 mb-12">
          {leaderboard.length > 0 && (
            <>
              {/* 2nd Place */}
              <motion.div 
                className="flex flex-col items-center"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2, duration: 0.5 }}
              >
                <div className="w-16 h-16 rounded-full bg-gray-200 flex items-center justify-center mb-2">
                  <Medal size={32} className="text-gray-500" />
                </div>
                <div className="text-center">
                  <div className="bg-gray-300 text-center h-28 w-20 rounded-t-lg flex items-center justify-center">
                    <div className={`font-bold ${
                      leaderboard.find(r => r.position === 2)?.isPlayer ? 'text-blue-600' : ''
                    }`}>
                      {leaderboard.find(r => r.position === 2)?.name || ''}
                    </div>
                  </div>
                  <div className="bg-gray-800 text-white font-bold py-1 rounded-b-lg">2nd</div>
                </div>
              </motion.div>
              
              {/* 1st Place */}
              <motion.div 
                className="flex flex-col items-center"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1, duration: 0.5 }}
              >
                <div className="w-20 h-20 rounded-full bg-yellow-100 flex items-center justify-center mb-2">
                  <Trophy size={40} className="text-yellow-500" />
                </div>
                <div className="text-center">
                  <div className="bg-yellow-300 text-center h-36 w-24 rounded-t-lg flex items-center justify-center">
                    <div className={`font-bold ${
                      leaderboard.find(r => r.position === 1)?.isPlayer ? 'text-blue-600' : ''
                    }`}>
                      {leaderboard.find(r => r.position === 1)?.name || ''}
                    </div>
                  </div>
                  <div className="bg-yellow-500 text-white font-bold py-1 rounded-b-lg">1st</div>
                </div>
              </motion.div>
              
              {/* 3rd Place */}
              <motion.div 
                className="flex flex-col items-center"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3, duration: 0.5 }}
              >
                <div className="w-16 h-16 rounded-full bg-amber-100 flex items-center justify-center mb-2">
                  <Medal size={32} className="text-amber-600" />
                </div>
                <div className="text-center">
                  <div className="bg-amber-200 text-center h-24 w-20 rounded-t-lg flex items-center justify-center">
                    <div className={`font-bold ${
                      leaderboard.find(r => r.position === 3)?.isPlayer ? 'text-blue-600' : ''
                    }`}>
                      {leaderboard.find(r => r.position === 3)?.name || ''}
                    </div>
                  </div>
                  <div className="bg-amber-600 text-white font-bold py-1 rounded-b-lg">3rd</div>
                </div>
              </motion.div>
            </>
          )}
        </div>
        
        {/* Full Leaderboard Table */}
        <div className="mb-8">
          <h3 className="text-xl font-bold text-blue-800 mb-4">Final Standings</h3>
          
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-blue-100">
                  <th className="py-3 px-4 text-left">Position</th>
                  <th className="py-3 px-4 text-left">Name</th>
                  <th className="py-3 px-4 text-left">Time</th>
                  <th className="py-3 px-4 text-left">Points</th>
                </tr>
              </thead>
              <tbody>
                {leaderboard.map((result, index) => (
                  <motion.tr 
                    key={index}
                    className={`leaderboard-row ${result.isPlayer ? 'font-medium text-blue-600' : ''}`}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.1 * index, duration: 0.3 }}
                  >
                    <td className="py-3 px-4">
                      <div className="flex items-center">
                        {result.position <= 3 && (
                          <div className={`mr-2 w-6 h-6 rounded-full flex items-center justify-center ${
                            result.position === 1 ? 'bg-yellow-400' : 
                            result.position === 2 ? 'bg-gray-300' : 
                            'bg-amber-600'
                          } text-white text-xs font-bold`}>
                            {result.position}
                          </div>
                        )}
                        {result.position > 3 && (
                          <span className="font-medium">{result.position}</span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      {result.name} {result.isPlayer && '(You)'}
                    </td>
                    <td className="py-3 px-4">{result.time.toFixed(1)}s</td>
                    <td className="py-3 px-4">{result.points} pts</td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <button 
            onClick={handlePlayAgain}
            className="btn btn-primary flex items-center justify-center"
          >
            <RotateCw size={18} className="mr-2" />
            Play Again
          </button>
          
          <button 
            onClick={() => {}}
            className="btn btn-secondary flex items-center justify-center"
          >
            Share Results <ArrowRight size={18} className="ml-2" />
          </button>
        </div>
      </motion.div>
    </div>
  );
};

export default LeaderboardScreen;