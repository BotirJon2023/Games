import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react';
import { Weather, WeatherType } from '../types/game';

interface WeatherContextType {
  currentWeather: Weather;
  changeWeather: (type?: WeatherType) => void;
}

const WeatherContext = createContext<WeatherContextType | undefined>(undefined);

export const useWeather = () => {
  const context = useContext(WeatherContext);
  if (!context) {
    throw new Error('useWeather must be used within a WeatherProvider');
  }
  return context;
};

interface WeatherProviderProps {
  children: ReactNode;
}

// Weather impact presets
const weatherImpacts: Record<WeatherType, Weather['impact']> = {
  sunny: { speedMultiplier: 1.0, handlingMultiplier: 1.0 },
  cloudy: { speedMultiplier: 0.9, handlingMultiplier: 0.95 },
  rainy: { speedMultiplier: 0.8, handlingMultiplier: 0.7 },
  stormy: { speedMultiplier: 0.6, handlingMultiplier: 0.5 }
};

const initialWeather: Weather = {
  type: 'sunny',
  windSpeed: 5,
  windDirection: 45,
  visibility: 100,
  impact: weatherImpacts.sunny
};

export const WeatherProvider: React.FC<WeatherProviderProps> = ({ children }) => {
  const [currentWeather, setCurrentWeather] = useState<Weather>(initialWeather);

  // Randomly change weather occasionally
  useEffect(() => {
    const weatherChangeInterval = setInterval(() => {
      // 20% chance to change weather when this interval runs
      if (Math.random() < 0.2) {
        changeWeather();
      }
    }, 60000); // Check every minute

    return () => clearInterval(weatherChangeInterval);
  }, []);

  const changeWeather = (type?: WeatherType) => {
    // If no specific type is provided, choose randomly
    const weatherTypes: WeatherType[] = ['sunny', 'cloudy', 'rainy', 'stormy'];
    const randomType = type || weatherTypes[Math.floor(Math.random() * weatherTypes.length)];
    
    // Generate random wind conditions
    const windSpeed = Math.floor(Math.random() * 20) + 1; // 1-20
    const windDirection = Math.floor(Math.random() * 360); // 0-359 degrees
    
    // Calculate visibility based on weather type
    let visibility = 100;
    if (randomType === 'cloudy') visibility = 80;
    if (randomType === 'rainy') visibility = 60;
    if (randomType === 'stormy') visibility = 40;
    
    setCurrentWeather({
      type: randomType,
      windSpeed,
      windDirection,
      visibility,
      impact: weatherImpacts[randomType]
    });
  };

  return (
    <WeatherContext.Provider value={{ currentWeather, changeWeather }}>
      {children}
    </WeatherContext.Provider>
  );
};