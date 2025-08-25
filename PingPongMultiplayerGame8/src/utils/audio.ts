// Web Audio API based sound effects
let audioContext: AudioContext | null = null;

const getAudioContext = (): AudioContext => {
  if (!audioContext) {
    audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
  }
  return audioContext;
};

export const playSound = (
  frequency: number,
  duration: number = 0.1,
  type: OscillatorType = 'sine',
  volume: number = 0.3
): void => {
  try {
    const ctx = getAudioContext();
    
    // Resume context if suspended (required by some browsers)
    if (ctx.state === 'suspended') {
      ctx.resume();
    }
    
    const oscillator = ctx.createOscillator();
    const gainNode = ctx.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);
    
    oscillator.frequency.value = frequency;
    oscillator.type = type;
    
    gainNode.gain.setValueAtTime(volume, ctx.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + duration);
    
    oscillator.start(ctx.currentTime);
    oscillator.stop(ctx.currentTime + duration);
  } catch (error) {
    // Silently fail if Web Audio API is not available
    console.warn('Audio not available:', error);
  }
};

export const playChord = (frequencies: number[], duration: number = 0.2): void => {
  frequencies.forEach((freq, index) => {
    setTimeout(() => playSound(freq, duration, 'sine', 0.1), index * 50);
  });
};

export const playMelody = (notes: { frequency: number; duration: number }[]): void => {
  let delay = 0;
  notes.forEach(note => {
    setTimeout(() => playSound(note.frequency, note.duration), delay);
    delay += note.duration * 1000;
  });
};