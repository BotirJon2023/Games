import React from 'react';

const Menu: React.FC = () => {
  return (
    <div className="fixed top-0 left-0 w-full h-screen flex flex-col items-center justify-center bg-black bg-opacity-30 pointer-events-none">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-yellow-400 mb-8">MOTORSPORT RACING</h1>
        <p className="text-2xl text-white mb-4">Championship Edition</p>
        <p className="text-xl text-gray-300">Press SPACE to Start</p>
      </div>
    </div>
  );
};

export default Menu;
