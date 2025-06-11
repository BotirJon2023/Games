import React, { useState } from 'react';
import { Character } from '../types/game';
import { characterClasses } from '../data/characterClasses';
import { Sword, Shield, Zap, Luggage as Dagger } from 'lucide-react';

interface CharacterCreationProps {
  onCharacterCreate: (character: Character) => void;
}

const classIcons = {
  Warrior: Sword,
  Mage: Zap,
  Rogue: Dagger
};

export const CharacterCreation: React.FC<CharacterCreationProps> = ({ onCharacterCreate }) => {
  const [name, setName] = useState('');
  const [selectedClass, setSelectedClass] = useState<string>('');
  const [step, setStep] = useState<'name' | 'class' | 'confirm'>('name');

  const handleNameSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (name.trim()) {
      setStep('class');
    }
  };

  const handleClassSelect = (className: string) => {
    setSelectedClass(className);
    setStep('confirm');
  };

  const handleConfirm = () => {
    const chosenClass = characterClasses.find(c => c.name === selectedClass);
    if (!chosenClass || !name.trim()) return;

    const character: Character = {
      id: Date.now().toString(),
      name: name.trim(),
      class: chosenClass,
      level: 1,
      experience: 0,
      experienceToNext: 100,
      health: chosenClass.baseStats.health,
      maxHealth: chosenClass.baseStats.health,
      mana: chosenClass.baseStats.mana,
      maxMana: chosenClass.baseStats.mana,
      attack: chosenClass.baseStats.attack,
      defense: chosenClass.baseStats.defense,
      speed: chosenClass.baseStats.speed,
      gold: 50,
      inventory: [],
      equipment: {}
    };

    onCharacterCreate(character);
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-purple-900 via-indigo-900 to-black flex items-center justify-center p-4">
      <div className="max-w-2xl w-full">
        <div className="text-center mb-8">
          <h1 className="text-5xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-gold-400 to-amber-300 mb-4">
            Create Your Hero
          </h1>
          <p className="text-gray-300 text-lg">Begin your legendary adventure</p>
        </div>

        {step === 'name' && (
          <div className="bg-black/40 backdrop-blur-lg rounded-2xl p-8 border border-purple-500/20">
            <form onSubmit={handleNameSubmit} className="space-y-6">
              <div>
                <label className="block text-gold-300 text-sm font-medium mb-2">
                  What is your name, hero?
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-800/50 border border-purple-500/30 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-gold-400 focus:ring-2 focus:ring-gold-400/20 transition-all"
                  placeholder="Enter your character's name"
                  maxLength={20}
                  autoFocus
                />
              </div>
              <button
                type="submit"
                disabled={!name.trim()}
                className="w-full py-3 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 disabled:from-gray-600 disabled:to-gray-700 text-white font-semibold rounded-lg transition-all duration-200 transform hover:scale-105 disabled:scale-100 disabled:opacity-50"
              >
                Continue
              </button>
            </form>
          </div>
        )}

        {step === 'class' && (
          <div className="bg-black/40 backdrop-blur-lg rounded-2xl p-8 border border-purple-500/20">
            <h2 className="text-2xl font-bold text-gold-300 mb-6 text-center">Choose Your Class</h2>
            <div className="grid gap-4">
              {characterClasses.map((charClass) => {
                const Icon = classIcons[charClass.name as keyof typeof classIcons];
                return (
                  <div
                    key={charClass.name}
                    onClick={() => handleClassSelect(charClass.name)}
                    className="p-6 bg-gray-800/30 border border-purple-500/20 rounded-xl cursor-pointer hover:border-gold-400 hover:bg-gray-700/30 transition-all duration-200 group"
                  >
                    <div className="flex items-start space-x-4">
                      <div className="p-3 bg-gradient-to-br from-purple-600 to-indigo-600 rounded-lg group-hover:from-gold-500 group-hover:to-amber-500 transition-all">
                        <Icon className="w-6 h-6 text-white" />
                      </div>
                      <div className="flex-1">
                        <h3 className="text-xl font-semibold text-white mb-2">{charClass.name}</h3>
                        <p className="text-gray-300 text-sm mb-4">{charClass.description}</p>
                        <div className="grid grid-cols-2 gap-2 text-xs">
                          <div className="text-green-400">Health: {charClass.baseStats.health}</div>
                          <div className="text-blue-400">Mana: {charClass.baseStats.mana}</div>
                          <div className="text-red-400">Attack: {charClass.baseStats.attack}</div>
                          <div className="text-yellow-400">Defense: {charClass.baseStats.defense}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {step === 'confirm' && (
          <div className="bg-black/40 backdrop-blur-lg rounded-2xl p-8 border border-purple-500/20">
            <h2 className="text-2xl font-bold text-gold-300 mb-6 text-center">Confirm Your Hero</h2>
            <div className="text-center space-y-4">
              <div className="text-3xl font-bold text-white">{name}</div>
              <div className="text-xl text-purple-300">the {selectedClass}</div>
              <div className="flex justify-center space-x-4 mt-6">
                <button
                  onClick={() => setStep('class')}
                  className="px-6 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-all"
                >
                  Back
                </button>
                <button
                  onClick={handleConfirm}
                  className="px-8 py-3 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 text-white font-semibold rounded-lg transition-all duration-200 transform hover:scale-105"
                >
                  Begin Adventure!
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};