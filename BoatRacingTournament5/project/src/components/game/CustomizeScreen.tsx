import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { ArrowLeft, Check } from 'lucide-react';
import { useBoatSelection } from '../../context/BoatSelectionContext';
import { getBoatIcon } from '../../data/boats';

interface CustomizeScreenProps {
  onBack: () => void;
}

const CustomizeScreen: React.FC<CustomizeScreenProps> = ({ onBack }) => {
  const { selectedBoat, selectBoat } = useBoatSelection();
  
  const [boatColor, setBoatColor] = useState(selectedBoat?.color || '#3b82f6');
  const [boatName, setBoatName] = useState(selectedBoat?.name || '');
  
  const colorOptions = [
    '#ef4444', // red
    '#f59e0b', // amber
    '#10b981', // green
    '#3b82f6', // blue
    '#8b5cf6', // purple
    '#ec4899', // pink
    '#0891b2', // cyan
    '#64748b', // slate
  ];
  
  const handleSaveCustomization = () => {
    if (selectedBoat) {
      const updatedBoat = {
        ...selectedBoat,
        name: boatName || selectedBoat.name,
        color: boatColor
      };
      selectBoat(updatedBoat);
    }
    onBack();
  };
  
  return (
    <div className="max-w-4xl mx-auto">
      <motion.div 
        className="card p-8"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
      >
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-blue-800">Customize Your Boat</h2>
          <button 
            onClick={onBack}
            className="p-2 rounded-full hover:bg-blue-100 transition-colors"
          >
            <ArrowLeft size={20} className="text-blue-600" />
          </button>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Boat Preview */}
          <div>
            <div className="bg-blue-50 rounded-xl p-8 flex flex-col items-center">
              <motion.div 
                className="h-48 w-48 flex items-center justify-center mb-6"
                animate={{ rotate: [0, 5, 0, -5, 0] }}
                transition={{ repeat: Infinity, duration: 5 }}
              >
                {selectedBoat && React.createElement(getBoatIcon(selectedBoat.image), { 
                  size: 120, 
                  style: { color: boatColor },
                  className: "boat-shadow"
                })}
              </motion.div>
              
              <div className="text-center">
                <h3 className="text-xl font-bold">{boatName || selectedBoat?.name}</h3>
                <p className="text-sm text-gray-500">Customized Boat</p>
              </div>
            </div>
            
            {selectedBoat && (
              <div className="mt-6">
                <h3 className="font-medium mb-2">Boat Stats</h3>
                <div className="space-y-3">
                  <div>
                    <div className="flex justify-between mb-1 text-sm">
                      <span>Speed</span>
                      <span>{selectedBoat.stats.speed}/10</span>
                    </div>
                    <div className="stat-bar">
                      <div 
                        className="stat-bar-fill"
                        style={{ width: `${selectedBoat.stats.speed * 10}%`, backgroundColor: boatColor }}
                      />
                    </div>
                  </div>
                  
                  <div>
                    <div className="flex justify-between mb-1 text-sm">
                      <span>Acceleration</span>
                      <span>{selectedBoat.stats.acceleration}/10</span>
                    </div>
                    <div className="stat-bar">
                      <div 
                        className="stat-bar-fill"
                        style={{ width: `${selectedBoat.stats.acceleration * 10}%`, backgroundColor: boatColor }}
                      />
                    </div>
                  </div>
                  
                  <div>
                    <div className="flex justify-between mb-1 text-sm">
                      <span>Handling</span>
                      <span>{selectedBoat.stats.handling}/10</span>
                    </div>
                    <div className="stat-bar">
                      <div 
                        className="stat-bar-fill"
                        style={{ width: `${selectedBoat.stats.handling * 10}%`, backgroundColor: boatColor }}
                      />
                    </div>
                  </div>
                  
                  <div>
                    <div className="flex justify-between mb-1 text-sm">
                      <span>Durability</span>
                      <span>{selectedBoat.stats.durability}/10</span>
                    </div>
                    <div className="stat-bar">
                      <div 
                        className="stat-bar-fill"
                        style={{ width: `${selectedBoat.stats.durability * 10}%`, backgroundColor: boatColor }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
          
          {/* Customization Options */}
          <div>
            <div className="mb-6">
              <h3 className="font-medium mb-3">Boat Name</h3>
              <input
                type="text"
                value={boatName}
                onChange={(e) => setBoatName(e.target.value)}
                placeholder={selectedBoat?.name}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            
            <div className="mb-8">
              <h3 className="font-medium mb-3">Boat Color</h3>
              <div className="grid grid-cols-4 gap-3">
                {colorOptions.map((color) => (
                  <button
                    key={color}
                    className={`w-12 h-12 rounded-full flex items-center justify-center ${
                      boatColor === color ? 'ring-2 ring-offset-2 ring-blue-500' : ''
                    }`}
                    style={{ backgroundColor: color }}
                    onClick={() => setBoatColor(color)}
                  >
                    {boatColor === color && (
                      <Check size={20} className="text-white" />
                    )}
                  </button>
                ))}
              </div>
            </div>
            
            <div className="mb-6">
              <h3 className="font-medium mb-3">Boat Decorations</h3>
              <div className="bg-gray-100 rounded-lg p-4 text-center">
                <p className="text-gray-500">Coming soon! Unlock more decorations by winning races.</p>
              </div>
            </div>
            
            <div className="space-y-3">
              <button 
                onClick={handleSaveCustomization}
                className="btn btn-primary w-full flex items-center justify-center"
              >
                <Check size={18} className="mr-2" />
                Save Customization
              </button>
              
              <button 
                onClick={onBack}
                className="btn bg-gray-200 text-gray-700 hover:bg-gray-300 w-full"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default CustomizeScreen;