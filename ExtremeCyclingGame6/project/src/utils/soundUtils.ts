// Web Audio API based sound system
export type SoundType = 'jump' | 'hit' | 'powerUp' | 'gameOver' | 'background';

let audioContext: AudioContext | null = null;
let masterGain: GainNode | null = null;

const initAudioContext = () => {
  if (!audioContext) {
    audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    masterGain = audioContext.createGain();
    masterGain.connect(audioContext.destination);
    masterGain.gain.value = 0.3; // Master volume
  }
};

const createTone = (frequency: number, duration: number, type: OscillatorType = 'sine') => {
  if (!audioContext || !masterGain) return;
  
  const oscillator = audioContext.createOscillator();
  const gainNode = audioContext.createGain();
  
  oscillator.connect(gainNode);
  gainNode.connect(masterGain);
  
  oscillator.frequency.value = frequency;
  oscillator.type = type;
  
  gainNode.gain.value = 0.1;
  gainNode.gain.exponentialRampToValueAtTime(0.001, audioContext.currentTime + duration);
  
  oscillator.start();
  oscillator.stop(audioContext.currentTime + duration);
};

const createNoise = (duration: number, filterFreq: number = 1000) => {
  if (!audioContext || !masterGain) return;
  
  const bufferSize = audioContext.sampleRate * duration;
  const buffer = audioContext.createBuffer(1, bufferSize, audioContext.sampleRate);
  const data = buffer.getChannelData(0);
  
  for (let i = 0; i < bufferSize; i++) {
    data[i] = Math.random() * 2 - 1;
  }
  
  const noise = audioContext.createBufferSource();
  noise.buffer = buffer;
  
  const filter = audioContext.createBiquadFilter();
  filter.type = 'lowpass';
  filter.frequency.value = filterFreq;
  
  const gainNode = audioContext.createGain();
  gainNode.gain.value = 0.1;
  gainNode.gain.exponentialRampToValueAtTime(0.001, audioContext.currentTime + duration);
  
  noise.connect(filter);
  filter.connect(gainNode);
  gainNode.connect(masterGain);
  
  noise.start();
  noise.stop(audioContext.currentTime + duration);
};

export const playSound = (type: SoundType) => {
  initAudioContext();
  
  switch (type) {
    case 'jump':
      createTone(440, 0.1, 'square');
      setTimeout(() => createTone(554, 0.1, 'square'), 50);
      break;
      
    case 'hit':
      createNoise(0.2, 200);
      createTone(150, 0.3, 'sawtooth');
      break;
      
    case 'powerUp':
      createTone(523, 0.1, 'sine');
      setTimeout(() => createTone(659, 0.1, 'sine'), 100);
      setTimeout(() => createTone(784, 0.1, 'sine'), 200);
      setTimeout(() => createTone(1047, 0.2, 'sine'), 300);
      break;
      
    case 'gameOver':
      createTone(440, 0.5, 'sawtooth');
      setTimeout(() => createTone(415, 0.5, 'sawtooth'), 200);
      setTimeout(() => createTone(392, 0.5, 'sawtooth'), 400);
      setTimeout(() => createTone(349, 1.0, 'sawtooth'), 600);
      break;
      
    case 'background':
      // Background music would be more complex
      // For now, we'll skip it to keep the implementation simple
      break;
  }
};

export const setMasterVolume = (volume: number) => {
  initAudioContext();
  if (masterGain) {
    masterGain.gain.value = Math.max(0, Math.min(1, volume));
  }
};

export const playBackgroundMusic = () => {
  // Implementation for background music would go here
  // This would involve creating a loop of harmonious tones
  // For simplicity, we'll leave this as a placeholder
};

export const stopBackgroundMusic = () => {
  // Implementation to stop background music
};