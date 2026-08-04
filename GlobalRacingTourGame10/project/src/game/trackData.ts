import type { Track, Vec2, SceneryType } from './types';

export type TrackId = 'monaco' | 'tokyo' | 'sahara' | 'alps' | 'amazon';

export const TRACKS: Track[] = [
  {
    id: 'monaco',
    name: 'Azure Coast',
    city: 'Monte Carlo',
    country: 'Monaco',
    flag: 'MC',
    description: 'Glamorous harbor circuit with tight corners and shimmering sea views.',
    laps: 3,
    difficulty: 2,
    halfWidth: 58,
    controlPoints: [
      { x: 300, y: 200 }, { x: 700, y: 180 }, { x: 1180, y: 240 },
      { x: 1500, y: 420 }, { x: 1640, y: 720 }, { x: 1500, y: 1020 },
      { x: 1180, y: 1180 }, { x: 820, y: 1180 }, { x: 560, y: 1040 },
      { x: 480, y: 760 }, { x: 300, y: 620 }, { x: 180, y: 420 },
    ],
    theme: {
      ground: '#2a4a52', groundDark: '#1f3a42',
      road: '#3a3a42', roadDark: '#2e2e36',
      curbA: '#ef4444', curbB: '#f8fafc',
      accent: '#38bdf8',
      skyTop: '#0c4a6e', skyBottom: '#7dd3fc',
    },
    sceneryTypes: ['building', 'palm', 'lighthouse', 'mountain'],
    accent: '#38bdf8',
  },
  {
    id: 'tokyo',
    name: 'Neon Grid',
    city: 'Tokyo',
    country: 'Japan',
    flag: 'JP',
    description: 'A nighttime sprint through glowing skyscrapers and neon-lit boulevards.',
    laps: 3,
    difficulty: 3,
    halfWidth: 56,
    controlPoints: [
      { x: 220, y: 540 }, { x: 380, y: 320 }, { x: 700, y: 260 },
      { x: 1080, y: 360 }, { x: 1380, y: 600 }, { x: 1320, y: 920 },
      { x: 1060, y: 1080 }, { x: 720, y: 1000 }, { x: 460, y: 980 },
      { x: 240, y: 880 },
    ],
    theme: {
      ground: '#1e1b2e', groundDark: '#161322',
      road: '#26243a', roadDark: '#1c1a2c',
      curbA: '#ec4899', curbB: '#22d3ee',
      accent: '#f472b6',
      skyTop: '#1e1b4b', skyBottom: '#7c3aed',
    },
    sceneryTypes: ['building', 'ferris', 'building', 'flag'],
    accent: '#f472b6',
  },
  {
    id: 'sahara',
    name: 'Desert Mirage',
    city: 'Sahara',
    country: 'Egypt',
    flag: 'EG',
    description: 'Blistering dunes and ancient pyramids along a high-speed desert loop.',
    laps: 3,
    difficulty: 1,
    halfWidth: 64,
    controlPoints: [
      { x: 260, y: 260 }, { x: 680, y: 200 }, { x: 1180, y: 280 },
      { x: 1500, y: 520 }, { x: 1600, y: 880 }, { x: 1360, y: 1160 },
      { x: 920, y: 1180 }, { x: 540, y: 1080 }, { x: 260, y: 820 },
      { x: 180, y: 500 },
    ],
    theme: {
      ground: '#caa45a', groundDark: '#b88c44',
      road: '#7a6a4a', roadDark: '#665740',
      curbA: '#fbbf24', curbB: '#1e293b',
      accent: '#f97316',
      skyTop: '#fb923c', skyBottom: '#fde68a',
    },
    sceneryTypes: ['pyramid', 'cactus', 'sanddune', 'rock', 'palm'],
    accent: '#f97316',
  },
  {
    id: 'alps',
    name: 'Alpine Rally',
    city: 'Bern',
    country: 'Switzerland',
    flag: 'CH',
    description: 'Snow-capped switchbacks through an evergreen mountain pass.',
    laps: 3,
    difficulty: 3,
    halfWidth: 52,
    controlPoints: [
      { x: 280, y: 620 }, { x: 460, y: 380 }, { x: 760, y: 280 },
      { x: 980, y: 460 }, { x: 880, y: 720 }, { x: 620, y: 760 },
      { x: 720, y: 1020 }, { x: 1080, y: 1060 }, { x: 1400, y: 860 },
      { x: 1460, y: 520 }, { x: 1240, y: 280 }, { x: 880, y: 220 },
    ],
    theme: {
      ground: '#d6dde0', groundDark: '#bcc6cc',
      road: '#4a4a52', roadDark: '#3a3a42',
      curbA: '#ef4444', curbB: '#f8fafc',
      accent: '#38bdf8',
      skyTop: '#7dd3fc', skyBottom: '#e0f2fe',
    },
    sceneryTypes: ['snowtree', 'mountain', 'snowtree', 'rock', 'tent'],
    accent: '#38bdf8',
  },
  {
    id: 'amazon',
    name: 'Jungle Run',
    city: 'Manaus',
    country: 'Brazil',
    flag: 'BR',
    description: 'A wild tropical rally weaving through dense rainforest canopy.',
    laps: 3,
    difficulty: 2,
    halfWidth: 58,
    controlPoints: [
      { x: 300, y: 300 }, { x: 680, y: 240 }, { x: 1040, y: 340 },
      { x: 1320, y: 560 }, { x: 1280, y: 880 }, { x: 1020, y: 760 },
      { x: 820, y: 1000 }, { x: 520, y: 1080 }, { x: 260, y: 860 },
      { x: 220, y: 540 },
    ],
    theme: {
      ground: '#2d5a2d', groundDark: '#234923',
      road: '#5a5238', roadDark: '#48402c',
      curbA: '#fbbf24', curbB: '#1e293b',
      accent: '#84cc16',
      skyTop: '#065f46', skyBottom: '#a7f3d0',
    },
    sceneryTypes: ['tree', 'palm', 'rock', 'tent', 'flag'],
    accent: '#84cc16',
  },
];

export function buildTrack(track: Track): Track {
  const N = 480;
  const samples: Vec2[] = [];
  const normals: Vec2[] = [];
  const cumDist: number[] = [];
  const cps = track.controlPoints;
  const cpCount = cps.length;

  for (let i = 0; i < N; i++) {
    const t = i / N;
    const ft = t * cpCount;
    const i0 = Math.floor(ft) % cpCount;
    const i1 = (i0 + 1) % cpCount;
    const i2 = (i0 + 2) % cpCount;
    const i3 = (i0 + 3) % cpCount;
    const f = ft - Math.floor(ft);
    const p0 = cps[i0];
    const p1 = cps[i1];
    const p2 = cps[i2];
    const p3 = cps[i3];
    const pt = catmullRom(p0, p1, p2, p3, f);
    samples.push(pt);
    if (i === 0) cumDist.push(0);
    else cumDist.push(cumDist[i - 1] + dist(samples[i - 1], samples[i]));
  }
  const totalLength = cumDist[N - 1] + dist(samples[N - 1], samples[0]);

  for (let i = 0; i < N; i++) {
    const next = (i + 1) % N;
    const dx = samples[next].x - samples[i].x;
    const dy = samples[next].y - samples[i].y;
    const len = Math.hypot(dx, dy) || 1;
    normals.push({ x: -dy / len, y: dx / len });
  }

  const leftEdge: Vec2[] = [];
  const rightEdge: Vec2[] = [];
  for (let i = 0; i < N; i++) {
    leftEdge.push({ x: samples[i].x + normals[i].x * track.halfWidth, y: samples[i].y + normals[i].y * track.halfWidth });
    rightEdge.push({ x: samples[i].x - normals[i].x * track.halfWidth, y: samples[i].y - normals[i].y * track.halfWidth });
  }

  const checkpoints: number[] = [];
  const numChecks = 8;
  for (let c = 0; c < numChecks; c++) {
    checkpoints.push(Math.floor((c / numChecks) * N));
  }

  const scenery = buildScenery(track, samples, normals);

  return { ...track, samples, normals, cumDist, totalLength, leftEdge, rightEdge, checkpoints, scenery };
}

function catmullRom(p0: Vec2, p1: Vec2, p2: Vec2, p3: Vec2, t: number): Vec2 {
  const t2 = t * t;
  const t3 = t2 * t;
  const x = 0.5 * (2 * p1.x + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
  const y = 0.5 * (2 * p1.y + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
  return { x, y };
}

function dist(a: Vec2, b: Vec2): number {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

function buildScenery(track: Track, samples: Vec2[], normals: Vec2[]): ReturnType<typeof Object> {
  const items: import('./types').SceneryItem[] = [];
  const N = samples.length;
  const types = track.sceneryTypes;

  for (let i = 0; i < N; i += 3) {
    if (Math.random() < 0.72) {
      const side = Math.random() < 0.5 ? -1 : 1;
      const offset = track.halfWidth + 18 + Math.random() * 110;
      const jitter = (Math.random() - 0.5) * 14;
      const nx = normals[i].x * side * offset + normals[i].y * jitter;
      const ny = normals[i].y * side * offset - normals[i].x * jitter;
      const type = types[Math.floor(Math.random() * types.length)];
      items.push({
        pos: { x: samples[i].x + nx, y: samples[i].y + ny },
        type,
        scale: 0.7 + Math.random() * 0.7,
        side,
      });
    }
  }
  return items;
}
