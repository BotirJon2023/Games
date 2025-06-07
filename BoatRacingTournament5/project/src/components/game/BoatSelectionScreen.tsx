import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Boat } from '../../types/game';
import { boats, getBoatIcon } from '../../data/boats';
import { useBoatSelection } from '../../context/BoatSelectionContext';
import { ArrowRight, Lock } from 'lucide-react';

interface BoatSelectionScreenProps {
  onSelect: () => void;
}

const BoatSelectionScreen: React.FC<BoatSelectionScreenProps> = ({ onSelect }) => {
  const { selectedBoat, selectBoat } = useBoatSelection();
  const [activeBoat, setActiveBoat] = useState<Boat | null>(selectedBoat || boats[0]);

  const handleSelectBoat = (boat: Boat) => {
    if (!boat.unlocked) return;
    setActiveBoat(boat);
  };

  const handleConfirmSelection = () => {
    if (activeBoat) {
      selectBoat(activeBoat);
      onSelect();
    }
  };

  const renderStatBar = (value: number, color: string) => {
    return (
      <div className="stat-bar">
        <motion.div 
          className="stat-bar-fill"
          style={{ backgroundColor: color }}
          initial={{ width: 0 }}
          animate={{ width: `${value * 10}%` }}
          transition={{ duration: 0.5 }}
        />
      </div>
    );
  };

  return (
    <motion.div 
      className="grid grid-cols-1 lg:grid-cols-3 gap-8"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      <div className="lg:col-span-2">
        <div className="card p-6">
          <h2 className="text-xl font-bold text-blue-800 mb-4">Available Boats</h2>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {boats.map(boat => {
              const IconComponent = getBoatIcon(boat.image);
              return (
                <motion.div
                  key={boat.id}
                  className={`relative cursor-pointer rounded-lg p-4 ${
                    activeBoat?.id === boat.id 
                      ? 'bg-blue-100 ring-2 ring-blue-500' 
                      : 'bg-white hover:bg-blue-50'
                  } ${!boat.unlocked ? 'opacity-70' : ''}`}
                  whileHover={{ scale: boat.unlocked ? 1.03 : 1 }}
                  onClick={() => handleSelectBoat(boat)}
                >
                  {!boat.unlocked && (
                    <div className="absolute inset-0 flex flex-col items-center justify-center bg-gray-800 bg-opacity-60 rounded-lg z-10">
                      <Lock className="text-white mb-1\" size={20} />
                      <span className="text-white text-sm">{boat.price} pts</span>
                    </div>
                  )}
                  
                  <div className="flex items-center justify-center h-16 mb-2">
                    <IconComponent size={48} style={{ color: boat.color }} />
                  </div>
                  
                  <h3 className="font-medium text-center mb-1">{boat.name}</h3>
                  
                  <div className="space-y-1 mt-2">
                    <div className="flex justify-between text-xs text-gray-600">
                      <span>Speed</span>
                      <span>{boat.stats.speed}/10</span>
                    </div>
                    {renderStatBar(boat.stats.speed, boat.color)}
                    
                    <div className="flex justify-between text-xs text-gray-600">
                      <span>Handling</span>
                      <span>{boat.stats.handling}/10</span>
                    </div>
                    {renderStatBar(boat.stats.handling, boat.color)}
                  </div>
                </motion.div>
              );
            })}
          </div>
        </div>
      </div>
      
      <div>
        {activeBoat && (
          <motion.div 
            className="card p-6"
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.3 }}
          >
            <h2 className="text-xl font-bold text-blue-800 mb-4">Boat Details</h2>
            
            <div className="flex flex-col items-center mb-6">
              <div className="h-24 w-24 flex items-center justify-center mb-4 rounded-full bg-blue-50">
                {React.createElement(getBoatIcon(activeBoat.image), { 
                  size: 64, 
                  style: { color: activeBoat.color },
                  className: "boat-shadow"
                })}
              </div>
              
              <h3 className="text-2xl font-bold">{activeBoat.name}</h3>
            </div>
            
            <div className="space-y-4 mb-6">
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Speed</span>
                  <span>{activeBoat.stats.speed}/10</span>
                </div>
                {renderStatBar(activeBoat.stats.speed, activeBoat.color)}
              </div>
              
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Acceleration</span>
                  <span>{activeBoat.stats.acceleration}/10</span>
                </div>
                {renderStatBar(activeBoat.stats.acceleration, activeBoat.color)}
              </div>
              
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Handling</span>
                  <span>{activeBoat.stats.handling}/10</span>
                </div>
                {renderStatBar(activeBoat.stats.handling, activeBoat.color)}
              </div>
              
              <div>
                <div className="flex justify-between mb-1 text-sm font-medium">
                  <span>Durability</span>
                  <span>{activeBoat.stats.durability}/10</span>
                </div>
                {renderStatBar(activeBoat.stats.durability, activeBoat.color)}
              </div>
            </div>
            
            <div>
              <p className="text-sm text-gray-600 mb-4">
                {activeBoat.name} is {getBoatDescription(activeBoat)}
              </p>
              
              <button 
                onClick={handleConfirmSelection}
                className="btn btn-primary w-full flex items-center justify-center"
                disabled={!activeBoat.unlocked}
              >
                Select Boat <ArrowRight size={18} className="ml-2" />
              </button>
            </div>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
};

// Helper function to generate boat descriptions
const getBoatDescription = (boat: Boat): string => {
  const { speed, acceleration, handling, durability } = boat.stats;
  
  let description = '';
  
  if (speed >= 8 && acceleration >= 8) {
    description += 'extremely fast with excellent acceleration. ';
  } else if (speed >= 8) {
    description += 'built for high speed racing. ';
  } else if (acceleration >= 8) {
    description += 'quick off the starting line. ';
  } else if (speed <= 5 && acceleration <= 5) {
    description += 'not the fastest boat, but ';
  }
  
  if (handling >= 8) {
    description += 'It handles turns with exceptional precision';
  } else if (handling >= 6) {
    description += 'It maneuvers well through most race conditions';
  } else {
    description += 'It requires skill to navigate through tight turns';
  }
  
  if (durability >= 8) {
    description += ' and can withstand significant impacts.';
  } else if (durability >= 6) {
    description += ' and has decent durability.';
  } else {
    description += ' but is somewhat fragile under pressure.';
  }
  
  return description;
};

export default BoatSelectionScreen;