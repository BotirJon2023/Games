import React, { useState, useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import { ArrowLeft, Save, Trash2, Plus, Map } from 'lucide-react';
import { Obstacle, RaceTrack } from '../../types/game';
import { tracks } from '../../data/tracks';

interface TrackEditorScreenProps {
  onBack: () => void;
}

const TrackEditorScreen: React.FC<TrackEditorScreenProps> = ({ onBack }) => {
  const [selectedTrack, setSelectedTrack] = useState<RaceTrack>(tracks[0]);
  const [editedTrack, setEditedTrack] = useState<RaceTrack>({ ...tracks[0] });
  const [selectedObstacle, setSelectedObstacle] = useState<Obstacle | null>(null);
  const [obstacleType, setObstacleType] = useState<Obstacle['type']>('rock');
  const [obstacleSize, setObstacleSize] = useState<number>(20);
  
  const trackRef = useRef<HTMLDivElement>(null);
  
  // Reset edited track when selection changes
  useEffect(() => {
    setEditedTrack({ ...selectedTrack });
    setSelectedObstacle(null);
  }, [selectedTrack]);
  
  const handleTrackSelect = (track: RaceTrack) => {
    setSelectedTrack(track);
  };
  
  const handleAddObstacle = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!trackRef.current) return;
    
    const rect = trackRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    const newObstacle: Obstacle = {
      type: obstacleType,
      position: { x, y },
      size: obstacleSize
    };
    
    setEditedTrack(prev => ({
      ...prev,
      obstacles: [...prev.obstacles, newObstacle]
    }));
  };
  
  const handleSelectObstacle = (obstacle: Obstacle) => {
    setSelectedObstacle(obstacle);
    setObstacleType(obstacle.type);
    setObstacleSize(obstacle.size);
  };
  
  const handleDeleteObstacle = () => {
    if (!selectedObstacle) return;
    
    setEditedTrack(prev => ({
      ...prev,
      obstacles: prev.obstacles.filter(o => o !== selectedObstacle)
    }));
    
    setSelectedObstacle(null);
  };
  
  const handleUpdateObstacle = () => {
    if (!selectedObstacle) return;
    
    setEditedTrack(prev => ({
      ...prev,
      obstacles: prev.obstacles.map(o => 
        o === selectedObstacle 
          ? { ...o, type: obstacleType, size: obstacleSize }
          : o
      )
    }));
    
    setSelectedObstacle(null);
  };
  
  const handleSaveTrack = () => {
    // In a real app, this would save to a database or local storage
    alert('Track saved! (Note: This is a demo, the track will not persist after reload)');
    onBack();
  };
  
  const getObstacleColor = (type: Obstacle['type']) => {
    switch (type) {
      case 'rock': return 'rgba(120, 120, 120, 0.8)';
      case 'buoy': return 'rgba(255, 60, 60, 0.8)';
      case 'debris': return 'rgba(165, 130, 100, 0.8)';
      case 'currentBoost': return 'rgba(100, 200, 255, 0.4)';
      case 'currentSlow': return 'rgba(30, 90, 150, 0.4)';
    }
  };
  
  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
      {/* Sidebar with controls */}
      <div className="lg:col-span-1">
        <div className="card p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-bold text-blue-800">Track Editor</h2>
            <button 
              onClick={onBack}
              className="p-2 rounded-full hover:bg-blue-100 transition-colors"
            >
              <ArrowLeft size={18} className="text-blue-600" />
            </button>
          </div>
          
          <div className="mb-6">
            <h3 className="font-medium text-sm mb-2">Select Track</h3>
            <div className="space-y-2">
              {tracks.map(track => (
                <button
                  key={track.id}
                  className={`w-full text-left p-2 rounded ${
                    selectedTrack.id === track.id 
                      ? 'bg-blue-100 text-blue-800' 
                      : 'hover:bg-gray-100'
                  }`}
                  onClick={() => handleTrackSelect(track)}
                >
                  <div className="flex items-center">
                    <Map size={16} className="mr-2 text-blue-600" />
                    <div>
                      <div className="font-medium">{track.name}</div>
                      <div className="text-xs text-gray-500 capitalize">{track.difficulty}</div>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>
          
          <div className="mb-6">
            <h3 className="font-medium text-sm mb-2">Obstacle Controls</h3>
            
            <div className="space-y-3">
              <div>
                <label className="block text-xs text-gray-500 mb-1">Obstacle Type</label>
                <select
                  value={obstacleType}
                  onChange={(e) => setObstacleType(e.target.value as Obstacle['type'])}
                  className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="rock">Rock</option>
                  <option value="buoy">Buoy</option>
                  <option value="debris">Debris</option>
                  <option value="currentBoost">Current (Boost)</option>
                  <option value="currentSlow">Current (Slow)</option>
                </select>
              </div>
              
              <div>
                <label className="block text-xs text-gray-500 mb-1">
                  Size: {obstacleSize}
                </label>
                <input
                  type="range"
                  min="10"
                  max="40"
                  value={obstacleSize}
                  onChange={(e) => setObstacleSize(parseInt(e.target.value))}
                  className="w-full"
                />
              </div>
            </div>
            
            <div className="mt-4 space-y-2">
              {selectedObstacle ? (
                <>
                  <button 
                    onClick={handleUpdateObstacle}
                    className="btn btn-primary w-full flex items-center justify-center py-2 text-sm"
                  >
                    <Save size={16} className="mr-2" />
                    Update Obstacle
                  </button>
                  
                  <button 
                    onClick={handleDeleteObstacle}
                    className="btn bg-red-500 text-white hover:bg-red-600 w-full flex items-center justify-center py-2 text-sm"
                  >
                    <Trash2 size={16} className="mr-2" />
                    Delete Obstacle
                  </button>
                </>
              ) : (
                <div className="text-center text-sm text-gray-500 p-2 bg-blue-50 rounded">
                  <Plus size={16} className="inline mr-1" />
                  Click on track to add obstacles
                </div>
              )}
            </div>
          </div>
          
          <button 
            onClick={handleSaveTrack}
            className="btn btn-primary w-full flex items-center justify-center"
          >
            <Save size={18} className="mr-2" />
            Save Track
          </button>
        </div>
      </div>
      
      {/* Track preview */}
      <div className="lg:col-span-3">
        <div className="card p-6">
          <div className="mb-4">
            <h2 className="text-xl font-bold text-blue-800">{editedTrack.name}</h2>
            <p className="text-sm text-gray-500">
              {editedTrack.length}m • {editedTrack.obstacles.length} obstacles • 
              <span className="capitalize"> {editedTrack.difficulty} difficulty</span>
            </p>
          </div>
          
          <div 
            ref={trackRef}
            className="relative overflow-hidden race-track rounded-lg cursor-crosshair"
            style={{ 
              background: editedTrack.background,
              height: '400px'
            }}
            onClick={handleAddObstacle}
          >
            {/* Finish line */}
            <div 
              className="absolute top-0 bottom-0 border-r-4 border-dashed border-white"
              style={{ left: `${editedTrack.length}px`, height: '100%' }}
            />
            
            {/* Obstacles */}
            {editedTrack.obstacles.map((obstacle, index) => (
              <motion.div
                key={`obstacle-${index}`}
                className="absolute rounded-full flex items-center justify-center text-white font-bold cursor-pointer"
                style={{
                  left: obstacle.position.x,
                  top: obstacle.position.y,
                  width: obstacle.size * 2,
                  height: obstacle.size * 2,
                  backgroundColor: getObstacleColor(obstacle.type),
                  transform: 'translate(-50%, -50%)',
                  border: selectedObstacle === obstacle ? '2px solid white' : 'none'
                }}
                onClick={(e) => {
                  e.stopPropagation();
                  handleSelectObstacle(obstacle);
                }}
                whileHover={{ scale: 1.05 }}
              >
                {obstacle.type === 'currentBoost' && '↑'}
                {obstacle.type === 'currentSlow' && '↓'}
              </motion.div>
            ))}
          </div>
          
          <div className="mt-4 bg-blue-50 p-3 rounded">
            <h3 className="font-medium text-sm mb-2">Editor Tips</h3>
            <ul className="text-sm text-gray-600 space-y-1 list-disc pl-5">
              <li>Click on the track to add new obstacles</li>
              <li>Click on existing obstacles to select and edit them</li>
              <li>Different obstacle types affect boat behavior differently</li>
              <li>Use a combination of obstacles to create challenging tracks</li>
              <li>Remember to save your track when finished</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TrackEditorScreen;