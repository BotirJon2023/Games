import * as THREE from 'three';
import * as CANNON from 'cannon-es';
import { createClient } from '@supabase/supabase-js';

// Supabase Configuration
const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL;
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY;
const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// Game Constants
const LEVELS = [
  { id: 1, name: 'ROOKIE COURT', targetScore: 10, rounds: 3, timePerShot: 30, wind: 0, distance: 5 },
  { id: 2, name: 'STREET BALL', targetScore: 15, rounds: 4, timePerShot: 25, wind: 0.1, distance: 6 },
  { id: 3, name: 'CAMPUS ARENA', targetScore: 20, rounds: 5, timePerShot: 22, wind: 0.15, distance: 6.5 },
  { id: 4, name: 'CITY LEAGUE', targetScore: 25, rounds: 5, timePerShot: 20, wind: 0.2, distance: 7 },
  { id: 5, name: 'STATE FINALS', targetScore: 30, rounds: 6, timePerShot: 18, wind: 0.25, distance: 7.5 },
  { id: 6, name: 'NATIONAL CUP', targetScore: 35, rounds: 6, timePerShot: 16, wind: 0.3, distance: 8 },
  { id: 7, name: 'WORLD TOURNAMENT', targetScore: 40, rounds: 7, timePerShot: 14, wind: 0.35, distance: 8.5 },
  { id: 8, name: 'OLYMPIC STAGE', targetScore: 50, rounds: 8, timePerShot: 12, wind: 0.4, distance: 9 },
  { id: 9, name: 'LEGEND ARENA', targetScore: 60, rounds: 8, timePerShot: 10, wind: 0.45, distance: 9.5 },
  { id: 10, name: 'ULTIMATE CHAMPION', targetScore: 75, rounds: 10, timePerShot: 8, wind: 0.5, distance: 10 }
];

const DIFFICULTY_SETTINGS = {
  easy: { aiAccuracy: 0.3, aiReactionTime: 2000, aiPowerVariance: 15 },
  medium: { aiAccuracy: 0.5, aiReactionTime: 1200, aiPowerVariance: 8 },
  hard: { aiAccuracy: 0.75, aiReactionTime: 600, aiPowerVariance: 3 }
};

// Game State
class GameState {
  constructor() {
    this.mode = 'vsComputer';
    this.difficulty = 'medium';
    this.currentLevel = 1;
    this.unlockedLevels = 1;
    this.scores = { player1: 0, player2: 0 };
    this.currentRound = 1;
    this.totalRounds = 5;
    this.currentPlayer = 1;
    this.shotClock = 30;
    this.isPlaying = false;
    this.isPaused = false;
    this.power = 50;
    this.angle = 45;
    this.wind = 0;
    this.ballInFlight = false;
  }

  reset() {
    this.scores = { player1: 0, player2: 0 };
    this.currentRound = 1;
    this.currentPlayer = 1;
    this.shotClock = LEVELS[this.currentLevel - 1].timePerShot;
    this.power = 50;
    this.angle = 45;
    this.ballInFlight = false;
  }
}

// 3D Scene Manager
class BasketballScene {
  constructor(canvas) {
    this.canvas = canvas;
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 100);
    this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    this.clock = new THREE.Clock();
    this.ball = null;
    this.hoop = null;
    this.backboard = null;
    this.court = null;
    this.lights = [];
    this.particles = [];
    this.ballTrail = [];

    this.init();
  }

  init() {
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.renderer.toneMappingExposure = 1.2;

    this.createEnvironment();
    this.createCourt();
    this.createHoop();
    this.createBall();
    this.setupCamera();
    this.setupLights();

    window.addEventListener('resize', () => this.onResize());
  }

  createEnvironment() {
    // Skybox / Background
    const skyGeometry = new THREE.SphereGeometry(100, 32, 32);
    const skyMaterial = new THREE.ShaderMaterial({
      uniforms: {
        topColor: { value: new THREE.Color(0x0a0e17) },
        bottomColor: { value: new THREE.Color(0x1a1f35) },
        offset: { value: 20 },
        exponent: { value: 0.6 }
      },
      vertexShader: `
        varying vec3 vWorldPosition;
        void main() {
          vec4 worldPosition = modelMatrix * vec4(position, 1.0);
          vWorldPosition = worldPosition.xyz;
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform vec3 topColor;
        uniform vec3 bottomColor;
        uniform float offset;
        uniform float exponent;
        varying vec3 vWorldPosition;
        void main() {
          float h = normalize(vWorldPosition + offset).y;
          gl_FragColor = vec4(mix(bottomColor, topColor, max(pow(max(h, 0.0), exponent), 0.0)), 1.0);
        }
      `,
      side: THREE.BackSide
    });
    const sky = new THREE.Mesh(skyGeometry, skyMaterial);
    this.scene.add(sky);

    // Fog
    this.scene.fog = new THREE.Fog(0x0a0e17, 30, 80);
  }

  createCourt() {
    // Main court floor
    const courtGeometry = new THREE.PlaneGeometry(30, 20);
    const courtMaterial = new THREE.MeshStandardMaterial({
      color: 0xcd853f,
      roughness: 0.8,
      metalness: 0.1
    });
    this.court = new THREE.Mesh(courtGeometry, courtMaterial);
    this.court.rotation.x = -Math.PI / 2;
    this.court.receiveShadow = true;
    this.scene.add(this.court);

    // Court lines
    const lineGeometry = new THREE.BufferGeometry();
    const lineVertices = [];
    const lineColor = new THREE.Color(0xffffff);

    // Three-point arc
    for (let i = 0; i <= 32; i++) {
      const angle = (i / 32) * Math.PI;
      const radius = 6.75;
      lineVertices.push(
        Math.cos(angle) * radius, 0.02, Math.sin(angle) * radius - 4.5
      );
      if (i < 32) {
        lineVertices.push(
          Math.cos((i + 1) / 32 * Math.PI) * radius, 0.02, Math.sin((i + 1) / 32 * Math.PI) * radius - 4.5
        );
      }
    }

    // Free throw lane
    const laneWidth = 4.9;
    const laneDepth = 5.8;
    lineVertices.push(
      -laneWidth/2, 0.02, -laneDepth - 4.5,
      laneWidth/2, 0.02, -laneDepth - 4.5,
      laneWidth/2, 0.02, -laneDepth - 4.5,
      laneWidth/2, 0.02, -4.5,
      -laneWidth/2, 0.02, -4.5,
      laneWidth/2, 0.02, -4.5,
      -laneWidth/2, 0.02, -4.5,
      -laneWidth/2, 0.02, -laneDepth - 4.5
    );

    const linePositions = new Float32Array(lineVertices);
    lineGeometry.setAttribute('position', new THREE.BufferAttribute(linePositions, 3));

    const lineMaterial = new THREE.LineBasicMaterial({ color: 0xffffff, linewidth: 2 });
    const lines = new THREE.LineSegments(lineGeometry, lineMaterial);
    this.scene.add(lines);

    // Key area (painted)
    const keyGeometry = new THREE.PlaneGeometry(laneWidth, laneDepth);
    const keyMaterial = new THREE.MeshStandardMaterial({
      color: 0x1a3a5c,
      roughness: 0.7,
      metalness: 0.1
    });
    const key = new THREE.Mesh(keyGeometry, keyMaterial);
    key.rotation.x = -Math.PI / 2;
    key.position.set(0, 0.01, -laneDepth/2 - 4.5);
    key.receiveShadow = true;
    this.scene.add(key);

    // Center court circle
    const centerCircleGeometry = new THREE.RingGeometry(1.8, 2, 32);
    const centerCircleMaterial = new THREE.MeshBasicMaterial({
      color: 0xffffff,
      side: THREE.DoubleSide
    });
    const centerCircle = new THREE.Mesh(centerCircleGeometry, centerCircleMaterial);
    centerCircle.rotation.x = -Math.PI / 2;
    centerCircle.position.y = 0.02;
    this.scene.add(centerCircle);

    // Stands/Bleachers
    this.createStands();
  }

  createStands() {
    const standGeometry = new THREE.BoxGeometry(25, 8, 3);
    const standMaterial = new THREE.MeshStandardMaterial({
      color: 0x2c3e50,
      roughness: 0.9,
      metalness: 0.1
    });

    // Left stands
    const leftStands = new THREE.Mesh(standGeometry, standMaterial);
    leftStands.position.set(-18, 4, -2);
    leftStands.rotation.y = Math.PI / 12;
    leftStands.castShadow = true;
    leftStands.receiveShadow = true;
    this.scene.add(leftStands);

    // Right stands
    const rightStands = new THREE.Mesh(standGeometry, standMaterial);
    rightStands.position.set(18, 4, -2);
    rightStands.rotation.y = -Math.PI / 12;
    rightStands.castShadow = true;
    rightStands.receiveShadow = true;
    this.scene.add(rightStands);

    // Crowd particles (simplified)
    const crowdGeometry = new THREE.BufferGeometry();
    const crowdCount = 200;
    const crowdPositions = new Float32Array(crowdCount * 3);
    const crowdColors = new Float32Array(crowdCount * 3);

    for (let i = 0; i < crowdCount; i++) {
      const side = Math.random() > 0.5 ? 1 : -1;
      crowdPositions[i * 3] = side * (15 + Math.random() * 6) + Math.random() * 2;
      crowdPositions[i * 3 + 1] = 2 + Math.random() * 6;
      crowdPositions[i * 3 + 2] = -4 + Math.random() * 4;

      const colorChoice = Math.random();
      if (colorChoice < 0.3) {
        crowdColors[i * 3] = 1; crowdColors[i * 3 + 1] = 0.42; crowdColors[i * 3 + 2] = 0.21; // Orange
      } else if (colorChoice < 0.6) {
        crowdColors[i * 3] = 0; crowdColors[i * 3 + 1] = 0.71; crowdColors[i * 3 + 2] = 0.85; // Cyan
      } else {
        crowdColors[i * 3] = 0.9; crowdColors[i * 3 + 1] = 0.9; crowdColors[i * 3 + 2] = 0.9; // White
      }
    }

    crowdGeometry.setAttribute('position', new THREE.BufferAttribute(crowdPositions, 3));
    crowdGeometry.setAttribute('color', new THREE.BufferAttribute(crowdColors, 3));

    const crowdMaterial = new THREE.PointsMaterial({
      size: 0.3,
      vertexColors: true,
      transparent: true,
      opacity: 0.8
    });

    const crowd = new THREE.Points(crowdGeometry, crowdMaterial);
    this.scene.add(crowd);
  }

  createHoop() {
    const hoopGroup = new THREE.Group();

    // Backboard
    const backboardGeometry = new THREE.BoxGeometry(1.8, 1.2, 0.05);
    const backboardMaterial = new THREE.MeshPhysicalMaterial({
      color: 0xffffff,
      transparent: true,
      opacity: 0.7,
      roughness: 0.1,
      metalness: 0,
      clearcoat: 1
    });
    this.backboard = new THREE.Mesh(backboardGeometry, backboardMaterial);
    this.backboard.position.set(0, 3.9, -4.5);
    this.backboard.castShadow = true;
    hoopGroup.add(this.backboard);

    // Backboard frame
    const frameGeometry = new THREE.BoxGeometry(1.9, 1.3, 0.08);
    const frameMaterial = new THREE.MeshStandardMaterial({
      color: 0xff6b35,
      roughness: 0.3,
      metalness: 0.7
    });
    const frame = new THREE.Mesh(frameGeometry, frameMaterial);
    frame.position.copy(this.backboard.position);
    frame.position.z -= 0.02;
    hoopGroup.add(frame);

    // Rim
    const rimGeometry = new THREE.TorusGeometry(0.23, 0.02, 16, 32);
    const rimMaterial = new THREE.MeshStandardMaterial({
      color: 0xff6b35,
      roughness: 0.3,
      metalness: 0.8
    });
    const rim = new THREE.Mesh(rimGeometry, rimMaterial);
    rim.rotation.x = Math.PI / 2;
    rim.position.set(0, 3.05, -4.2);
    rim.castShadow = true;
    this.hoop = rim;
    hoopGroup.add(rim);

    // Net
    const netGeometry = new THREE.CylinderGeometry(0.22, 0.15, 0.4, 12, 5, true);
    const netMaterial = new THREE.MeshBasicMaterial({
      color: 0xffffff,
      wireframe: true,
      transparent: true,
      opacity: 0.8
    });
    const net = new THREE.Mesh(netGeometry, netMaterial);
    net.position.set(0, 2.85, -4.2);
    hoopGroup.add(net);

    // Pole
    const poleGeometry = new THREE.CylinderGeometry(0.1, 0.1, 4.5, 16);
    const poleMaterial = new THREE.MeshStandardMaterial({
      color: 0x333333,
      roughness: 0.4,
      metalness: 0.6
    });
    const pole = new THREE.Mesh(poleGeometry, poleMaterial);
    pole.position.set(0, 2.25, -4.7);
    pole.castShadow = true;
    hoopGroup.add(pole);

    // Support arm
    const armGeometry = new THREE.BoxGeometry(0.1, 0.1, 0.3);
    const arm = new THREE.Mesh(armGeometry, poleMaterial);
    arm.position.set(0, 3.5, -4.55);
    hoopGroup.add(arm);

    this.scene.add(hoopGroup);
  }

  createBall() {
    // Basketball geometry
    const ballGeometry = new THREE.SphereGeometry(0.12, 32, 32);
    const ballMaterial = new THREE.MeshStandardMaterial({
      color: 0xff6b35,
      roughness: 0.6,
      metalness: 0.1
    });
    this.ball = new THREE.Mesh(ballGeometry, ballMaterial);
    this.ball.castShadow = true;
    this.ball.position.set(0, 0.2, 5);

    // Add basketball lines
    const seamCurves = [
      new THREE.EllipseCurve(0, 0, 0.12, 0.12, 0, Math.PI * 2, false, 0),
      new THREE.EllipseCurve(0, 0, 0.001, 0.12, 0, Math.PI * 2, false, 0),
      new THREE.EllipseCurve(0, 0, 0.12, 0.001, 0, Math.PI * 2, false, 0)
    ];

    const seamMaterial = new THREE.LineBasicMaterial({ color: 0x1a1a1a });
    seamCurves.forEach((curve, idx) => {
      const points = curve.getPoints(64);
      const geometry = new THREE.BufferGeometry().setFromPoints(
        points.map(p => new THREE.Vector3(p.x, 0, p.y))
      );
      const line = new THREE.Line(geometry, seamMaterial);
      if (idx === 1) line.rotation.z = Math.PI / 2;
      this.ball.add(line);
    });

    this.scene.add(this.ball);
  }

  setupCamera() {
    this.camera.position.set(0, 3, 10);
    this.camera.lookAt(0, 2, -4);
  }

  setupLights() {
    // Ambient light
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.4);
    this.scene.add(ambientLight);

    // Main spotlight (arena light)
    const mainLight = new THREE.SpotLight(0xffffff, 2);
    mainLight.position.set(0, 15, 0);
    mainLight.angle = Math.PI / 4;
    mainLight.penumbra = 0.3;
    mainLight.decay = 2;
    mainLight.distance = 50;
    mainLight.castShadow = true;
    mainLight.shadow.mapSize.width = 2048;
    mainLight.shadow.mapSize.height = 2048;
    mainLight.shadow.camera.near = 5;
    mainLight.shadow.camera.far = 50;
    this.scene.add(mainLight);
    this.lights.push(mainLight);

    // Fill lights
    const fillLight1 = new THREE.PointLight(0xff6b35, 0.5);
    fillLight1.position.set(-10, 8, 5);
    this.scene.add(fillLight1);

    const fillLight2 = new THREE.PointLight(0x00b4d8, 0.5);
    fillLight2.position.set(10, 8, 5);
    this.scene.add(fillLight2);

    // Back light for rim effect
    const backLight = new THREE.PointLight(0xffffff, 1);
    backLight.position.set(0, 5, -8);
    this.scene.add(backLight);
  }

  updateBallTrail() {
    if (this.ballInFlight) {
      this.ballTrail.push(this.ball.position.clone());
      if (this.ballTrail.length > 30) {
        this.ballTrail.shift();
      }
    }
  }

  clearTrail() {
    this.ballTrail = [];
  }

  resetBall(player) {
    const level = LEVELS[gameState.currentLevel - 1];
    const zPos = player === 1 ? level.distance : -level.distance - 1;
    this.ball.position.set(0, 0.2, zPos);
    this.ball.velocity = new THREE.Vector3(0, 0, 0);
    this.clearTrail();
  }

  createScoreParticles(position, color = 0x2ecc71) {
    const particleCount = 50;
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(particleCount * 3);
    const velocities = [];

    for (let i = 0; i < particleCount; i++) {
      positions[i * 3] = position.x + (Math.random() - 0.5) * 0.5;
      positions[i * 3 + 1] = position.y + (Math.random() - 0.5) * 0.5;
      positions[i * 3 + 2] = position.z + (Math.random() - 0.5) * 0.5;
      velocities.push({
        x: (Math.random() - 0.5) * 0.2,
        y: Math.random() * 0.3 + 0.1,
        z: (Math.random() - 0.5) * 0.2
      });
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

    const material = new THREE.PointsMaterial({
      color: color,
      size: 0.1,
      transparent: true,
      opacity: 1
    });

    const particles = new THREE.Points(geometry, material);
    particles.userData = { velocities, life: 1 };
    this.scene.add(particles);
    this.particles.push(particles);
  }

  updateParticles(delta) {
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const particle = this.particles[i];
      const positions = particle.geometry.attributes.position.array;
      const velocities = particle.userData.velocities;

      particle.userData.life -= delta;

      for (let j = 0; j < velocities.length; j++) {
        positions[j * 3] += velocities[j].x * delta * 60;
        positions[j * 3 + 1] += velocities[j].y * delta * 60;
        positions[j * 3 + 2] += velocities[j].z * delta * 60;
        velocities[j].y -= delta * 0.5; // gravity
      }

      particle.geometry.attributes.position.needsUpdate = true;
      particle.material.opacity = particle.userData.life;

      if (particle.userData.life <= 0) {
        this.scene.remove(particle);
        particle.geometry.dispose();
        particle.material.dispose();
        this.particles.splice(i, 1);
      }
    }
  }

  onResize() {
    this.camera.aspect = window.innerWidth / window.innerHeight;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(window.innerWidth, window.innerHeight);
  }

  render() {
    const delta = this.clock.getDelta();
    this.updateParticles(delta);
    this.renderer.render(this.scene, this.camera);
  }
}

// Physics Engine
class PhysicsEngine {
  constructor() {
    this.world = new CANNON.World();
    this.world.gravity.set(0, -9.82, 0);
    this.world.broadphase = new CANNON.NaiveBroadphase();
    this.world.solver.iterations = 10;

    this.ballBody = null;
    this.groundBody = null;
    this.backboardBody = null;
    this.rimBody = null;

    this.init();
  }

  init() {
    // Ground
    const groundShape = new CANNON.Plane();
    this.groundBody = new CANNON.Body({ mass: 0 });
    this.groundBody.addShape(groundShape);
    this.groundBody.quaternion.setFromAxisAngle(new CANNON.Vec3(1, 0, 0), -Math.PI / 2);
    this.world.addBody(this.groundBody);

    // Ball
    const ballShape = new CANNON.Sphere(0.12);
    this.ballBody = new CANNON.Body({
      mass: 0.62,
      shape: ballShape,
      material: new CANNON.Material({ restitution: 0.7, friction: 0.3 })
    });
    this.world.addBody(this.ballBody);

    // Backboard
    const backboardShape = new CANNON.Box(new CANNON.Vec3(0.9, 0.6, 0.025));
    this.backboardBody = new CANNON.Body({ mass: 0, shape: backboardShape });
    this.backboardBody.position.set(0, 3.9, -4.5);
    this.world.addBody(this.backboardBody);

    // Rim (simplified as a ring of small bodies)
    const rimRadius = 0.23;
    const rimSegments = 8;
    for (let i = 0; i < rimSegments; i++) {
      const angle = (i / rimSegments) * Math.PI * 2;
      const rimSegmentShape = new CANNON.Sphere(0.03);
      const rimBody = new CANNON.Body({ mass: 0, shape: rimSegmentShape });
      rimBody.position.set(
        Math.cos(angle) * rimRadius,
        3.05,
        -4.2 + Math.sin(angle) * rimRadius
      );
      this.world.addBody(rimBody);
    }
  }

  shoot(power, angle, wind, direction = -1) {
    const forceMagnitude = power * 0.8;
    const angleRad = (angle * Math.PI) / 180;

    const vx = Math.sin(angleRad) * forceMagnitude * 0.3;
    const vy = Math.cos(angleRad) * forceMagnitude;
    const vz = direction * Math.cos(angleRad) * forceMagnitude * 0.6 + wind;

    this.ballBody.velocity.set(vx, vy, vz);
    this.ballBody.angularVelocity.set(
      (Math.random() - 0.5) * 5,
      (Math.random() - 0.5) * 2,
      direction * 10
    );

    return this.ballBody.velocity;
  }

  update(delta) {
    this.world.step(1/60, delta, 3);
  }

  syncWithScene(ballMesh) {
    ballMesh.position.copy(this.ballBody.position);
    ballMesh.quaternion.copy(this.ballBody.quaternion);
  }

  resetBall(position) {
    this.ballBody.position.copy(position);
    this.ballBody.velocity.set(0, 0, 0);
    this.ballBody.angularVelocity.set(0, 0, 0);
  }

  checkScore(ballPos) {
    // Check if ball passes through the hoop
    const hoopCenter = { x: 0, y: 3.05, z: -4.2 };
    const radius = 0.23;

    const dx = ballPos.x - hoopCenter.x;
    const dz = ballPos.z - hoopCenter.z;
    const horizontalDist = Math.sqrt(dx * dx + dz * dz);

    // Ball is going through the rim area
    if (horizontalDist < radius &&
        ballPos.y < hoopCenter.y + 0.1 &&
        ballPos.y > hoopCenter.y - 0.05 &&
        this.ballBody.velocity.y < 0) {
      return true;
    }
    return false;
  }

  checkBackboard() {
    return this.ballBody.position.z < -4.4 && this.ballBody.position.z > -4.55;
  }
}

// AI Controller
class AIController {
  constructor(difficulty) {
    this.settings = DIFFICULTY_SETTINGS[difficulty];
    this.thinking = false;
    this.shotPlanned = false;
  }

  planShot(state) {
    this.thinking = true;

    return new Promise((resolve) => {
      setTimeout(() => {
        const level = LEVELS[state.currentLevel - 1];
        const basePower = 55 + (level.id * 2);
        const baseAngle = 45 + (level.id * 1);

        // Add variance based on difficulty
        const powerVariance = this.settings.aiPowerVariance;
        const power = basePower + (Math.random() - 0.5) * powerVariance * 2;
        const angle = baseAngle + (Math.random() - 0.5) * (20 - this.settings.aiAccuracy * 20);

        this.thinking = false;
        this.shotPlanned = true;

        resolve({ power, angle });
      }, this.settings.aiReactionTime);
    });
  }

  shouldShoot(state) {
    if (!this.shotPlanned) return false;
    // AI accuracy check
    return Math.random() < this.settings.aiAccuracy;
  }
}

// Sound Manager (using Web Audio API)
class SoundManager {
  constructor() {
    this.audioContext = null;
    this.sounds = {};
    this.enabled = true;
  }

  init() {
    this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
    this.createSounds();
  }

  createSounds() {
    // Bounce sound
    this.sounds.bounce = () => this.playTone(200, 0.1, 'square');
    // Score sound
    this.sounds.score = () => {
      this.playTone(523, 0.15, 'sine');
      setTimeout(() => this.playTone(659, 0.15, 'sine'), 100);
      setTimeout(() => this.playTone(784, 0.2, 'sine'), 200);
    };
    // Miss sound
    this.sounds.miss = () => this.playTone(150, 0.3, 'sawtooth');
    // Shoot sound
    this.sounds.shoot = () => this.playTone(300, 0.1, 'triangle');
  }

  playTone(frequency, duration, type = 'sine') {
    if (!this.enabled || !this.audioContext) return;

    const oscillator = this.audioContext.createOscillator();
    const gainNode = this.audioContext.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(this.audioContext.destination);

    oscillator.frequency.value = frequency;
    oscillator.type = type;

    gainNode.gain.setValueAtTime(0.3, this.audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + duration);

    oscillator.start();
    oscillator.stop(this.audioContext.currentTime + duration);
  }

  play(soundName) {
    if (this.sounds[soundName]) {
      this.sounds[soundName]();
    }
  }
}

// Main Game Class
class BasketballGame {
  constructor() {
    this.state = new GameState();
    this.scene = null;
    this.physics = null;
    this.ai = null;
    this.sound = new SoundManager();
    this.shotClockInterval = null;
    this.animationFrame = null;

    this.keys = {};
    this.lastShotResult = null;

    this.init();
  }

  init() {
    this.setupEventListeners();
    this.loadProgress();
  }

  setupEventListeners() {
    // Menu buttons
    document.getElementById('twoPlayerBtn').addEventListener('click', () => {
      this.state.mode = 'twoPlayer';
      this.startLevelSelect();
    });

    document.getElementById('vsComputerBtn').addEventListener('click', () => {
      this.state.mode = 'vsComputer';
      document.getElementById('difficultySelector').classList.remove('hidden');
    });

    document.querySelectorAll('.diff-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.state.difficulty = btn.dataset.difficulty;
        this.startLevelSelect();
      });
    });

    document.getElementById('levelSelectBtn').addEventListener('click', () => this.startLevelSelect());
    document.getElementById('leaderboardBtn').addEventListener('click', () => this.showLeaderboard());

    document.getElementById('backToMenu').addEventListener('click', () => this.showStartScreen());
    document.getElementById('backFromLeaderboard').addEventListener('click', () => this.showStartScreen());

    document.getElementById('resumeBtn').addEventListener('click', () => this.togglePause());
    document.getElementById('restartBtn').addEventListener('click', () => this.restartGame());
    document.getElementById('quitBtn').addEventListener('click', () => this.quitToMenu());

    document.getElementById('playAgainBtn').addEventListener('click', () => this.restartGame());
    document.getElementById('menuBtn').addEventListener('click', () => this.quitToMenu());

    // Keyboard controls
    document.addEventListener('keydown', (e) => this.handleKeyDown(e));
    document.addEventListener('keyup', (e) => this.handleKeyUp(e));
  }

  handleKeyDown(e) {
    if (!this.state.isPlaying || this.state.isPaused) {
      if (e.key === 'Escape' && this.state.isPlaying) {
        this.togglePause();
      }
      return;
    }

    e.preventDefault();
    this.keys[e.code] = true;

    // Player 1 controls
    if (this.state.currentPlayer === 1 && !this.state.ballInFlight) {
      if (e.code === 'KeyA') this.state.angle = Math.min(90, this.state.angle + 2);
      if (e.code === 'KeyD') this.state.angle = Math.max(0, this.state.angle - 2);
      if (e.code === 'KeyW') this.state.power = Math.min(100, this.state.power + 2);
      if (e.code === 'KeyS') this.state.power = Math.max(10, this.state.power - 2);
      if (e.code === 'Space') this.shoot();

      this.updateUI();
    }

    // Player 2 controls (for 2-player mode)
    if (this.state.mode === 'twoPlayer' && this.state.currentPlayer === 2 && !this.state.ballInFlight) {
      if (e.code === 'ArrowLeft') this.state.angle = Math.min(90, this.state.angle + 2);
      if (e.code === 'ArrowRight') this.state.angle = Math.max(0, this.state.angle - 2);
      if (e.code === 'ArrowUp') this.state.power = Math.min(100, this.state.power + 2);
      if (e.code === 'ArrowDown') this.state.power = Math.max(10, this.state.power - 2);
      if (e.code === 'Enter') this.shoot();

      this.updateUI();
    }

    // Pause
    if (e.code === 'Escape') {
      this.togglePause();
    }
  }

  handleKeyUp(e) {
    this.keys[e.code] = false;
  }

  startLevelSelect() {
    document.getElementById('startScreen').classList.add('hidden');
    document.getElementById('levelSelectScreen').classList.remove('hidden');
    this.renderLevelGrid();
  }

  renderLevelGrid() {
    const grid = document.getElementById('levelGrid');
    grid.innerHTML = '';

    LEVELS.forEach((level, index) => {
      const card = document.createElement('div');
      card.className = 'level-card';

      const isUnlocked = index < this.state.unlockedLevels;
      const isCompleted = index < this.state.unlockedLevels - 1;

      if (!isUnlocked) {
        card.classList.add('locked');
        card.innerHTML = `<span class="lock-icon">🔒</span>`;
      } else {
        if (isCompleted) card.classList.add('completed');

        const stars = isCompleted ? '★★★' : '☆☆☆';
        card.innerHTML = `
          <span class="level-number">${level.id}</span>
          <span class="level-name">${level.name}</span>
          <span class="level-stars">${stars}</span>
        `;

        card.addEventListener('click', () => {
          this.state.currentLevel = level.id;
          this.startGame();
        });
      }

      grid.appendChild(card);
    });
  }

  showLeaderboard() {
    document.getElementById('startScreen').classList.add('hidden');
    document.getElementById('leaderboardScreen').classList.remove('hidden');
    this.loadLeaderboard();
  }

  async loadLeaderboard() {
    const content = document.getElementById('leaderboardContent');
    content.innerHTML = '<div class="loading"><div class="loading-spinner"></div></div>';

    try {
      const { data, error } = await supabase
        .from('basketball_scores')
        .select('*')
        .order('score', { ascending: false })
        .limit(10);

      if (error) throw error;

      if (data && data.length > 0) {
        content.innerHTML = data.map((entry, index) => {
          const rankClass = index === 0 ? 'gold' : index === 1 ? 'silver' : index === 2 ? 'bronze' : '';
          return `
            <div class="leaderboard-entry">
              <span class="rank ${rankClass}">#${index + 1}</span>
              <div class="player-info">
                <span class="player-name">${entry.player_name}</span>
                <span class="player-level">Level ${entry.level}</span>
              </div>
              <span class="player-score">${entry.score}</span>
            </div>
          `;
        }).join('');
      } else {
        content.innerHTML = '<p style="text-align: center; padding: 2rem; color: var(--text-muted);">No scores yet. Be the first to play!</p>';
      }
    } catch (error) {
      content.innerHTML = '<p style="text-align: center; padding: 2rem; color: var(--error);">Failed to load leaderboard</p>';
    }
  }

  showStartScreen() {
    document.getElementById('levelSelectScreen').classList.add('hidden');
    document.getElementById('leaderboardScreen').classList.add('hidden');
    document.getElementById('gameContainer').classList.add('hidden');
    document.getElementById('gameOverScreen').classList.add('hidden');
    document.getElementById('startScreen').classList.remove('hidden');

    if (this.shotClockInterval) clearInterval(this.shotClockInterval);
    if (this.animationFrame) cancelAnimationFrame(this.animationFrame);
  }

  async startGame() {
    document.getElementById('levelSelectScreen').classList.add('hidden');
    document.getElementById('gameContainer').classList.remove('hidden');

    // Initialize sound on user interaction
    if (!this.sound.audioContext) {
      this.sound.init();
    }

    // Initialize 3D scene
    const canvas = document.getElementById('gameCanvas');
    this.scene = new BasketballScene(canvas);
    this.physics = new PhysicsEngine();

    // Initialize AI if needed
    if (this.state.mode === 'vsComputer') {
      this.ai = new AIController(this.state.difficulty);
      document.getElementById('player2Label').textContent = 'COMPUTER';
      document.getElementById('finalLabel2').textContent = 'COMPUTER';
    } else {
      document.getElementById('player2Label').textContent = 'PLAYER 2';
      document.getElementById('finalLabel2').textContent = 'PLAYER 2';
    }

    // Setup level
    const level = LEVELS[this.state.currentLevel - 1];
    this.state.totalRounds = level.rounds;
    this.state.wind = (Math.random() - 0.5) * level.wind * 2;
    this.state.reset();

    this.updateUI();
    this.startShotClock();
    this.gameLoop();
  }

  gameLoop() {
    if (!this.state.isPlaying || this.state.isPaused) {
      this.animationFrame = requestAnimationFrame(() => this.gameLoop());
      this.scene.render();
      return;
    }

    const delta = 1/60;

    // Update physics
    this.physics.update(delta);
    this.physics.syncWithScene(this.scene.ball);

    // Check for score or miss
    if (this.state.ballInFlight) {
      this.checkShotResult();
    }

    // AI logic
    if (this.state.mode === 'vsComputer' && this.state.currentPlayer === 2 && !this.state.ballInFlight) {
      this.handleAITurn();
    }

    this.scene.render();
    this.animationFrame = requestAnimationFrame(() => this.gameLoop());
  }

  async handleAITurn() {
    if (this.ai.thinking) return;

    const shot = await this.ai.planShot(this.state);
    this.state.power = shot.power;
    this.state.angle = shot.angle;
    this.updateUI();

    setTimeout(() => {
      if (this.ai.shouldShoot(this.state)) {
        this.shoot();
      }
    }, 500);
  }

  shoot() {
    if (this.state.ballInFlight) return;

    this.state.ballInFlight = true;
    this.sound.play('shoot');

    const direction = this.state.currentPlayer === 1 ? -1 : 1;
    this.physics.shoot(this.state.power, this.state.angle, this.state.wind, direction);

    // Clear shot clock
    if (this.shotClockInterval) clearInterval(this.shotClockInterval);
  }

  checkShotResult() {
    const ballPos = this.scene.ball.position;
    const level = LEVELS[this.state.currentLevel - 1];

    // Check if scored
    if (this.physics.checkScore(ballPos)) {
      this.handleScore();
      return;
    }

    // Check if ball has landed (missed)
    if (ballPos.y < 0.15 && Math.abs(this.physics.ballBody.velocity.y) < 0.5) {
      this.handleMiss();
    }

    // Check if ball went out of bounds
    if (Math.abs(ballPos.x) > 15 || ballPos.z > 20 || ballPos.z < -15) {
      this.handleMiss();
    }
  }

  handleScore() {
    const level = LEVELS[this.state.currentLevel - 1];
    const points = Math.round(2 + (this.state.power / 50) + (level.id * 0.5));

    if (this.state.currentPlayer === 1) {
      this.state.scores.player1 += points;
    } else {
      this.state.scores.player2 += points;
    }

    this.sound.play('score');
    this.showShotResult('SCORED!', `+${points}`, true);
    this.scene.createScoreParticles(this.scene.ball.position.clone());

    setTimeout(() => this.endTurn(), 1500);
  }

  handleMiss() {
    this.sound.play('miss');
    this.showShotResult('MISSED', '', false);

    setTimeout(() => this.endTurn(), 1500);
  }

  showShotResult(text, points, scored) {
    const resultEl = document.getElementById('shotResult');
    resultEl.classList.remove('hidden');
    resultEl.querySelector('.result-text').textContent = text;
    resultEl.querySelector('.result-text').className = `result-text ${scored ? 'scored' : 'missed'}`;
    resultEl.querySelector('.result-points').textContent = points;

    setTimeout(() => {
      resultEl.classList.add('hidden');
    }, 1500);
  }

  endTurn() {
    this.state.ballInFlight = false;
    this.state.currentPlayer = this.state.currentPlayer === 1 ? 2 : 1;

    // Check if round is complete
    if (this.state.currentPlayer === 1) {
      this.state.currentRound++;

      // Check end of game
      if (this.state.currentRound > this.state.totalRounds) {
        this.endGame();
        return;
      }
    }

    // Reset ball position and shot clock
    this.scene.resetBall(this.state.currentPlayer);
    this.physics.resetBall(this.scene.ball.position);

    const level = LEVELS[this.state.currentLevel - 1];
    this.state.shotClock = level.timePerShot;
    this.state.power = 50;
    this.state.angle = 45;
    this.state.wind = (Math.random() - 0.5) * level.wind * 2;

    this.updateUI();
    this.startShotClock();
  }

  startShotClock() {
    if (this.shotClockInterval) clearInterval(this.shotClockInterval);

    this.shotClockInterval = setInterval(() => {
      if (this.state.isPaused || this.state.ballInFlight) return;

      this.state.shotClock--;
      document.getElementById('shotClock').textContent = this.state.shotClock;

      if (this.state.shotClock <= 0) {
        // Auto shoot with random values
        this.state.power = 30 + Math.random() * 40;
        this.state.angle = 30 + Math.random() * 30;
        this.shoot();
      }
    }, 1000);
  }

  updateUI() {
    document.getElementById('currentLevel').textContent = `LEVEL ${this.state.currentLevel}`;
    document.getElementById('currentRound').textContent = `ROUND ${this.state.currentRound}/${this.state.totalRounds}`;
    document.getElementById('shotClock').textContent = this.state.shotClock;

    document.querySelector('#player1Score .score-value').textContent = this.state.scores.player1;
    document.querySelector('#player2Score .score-value').textContent = this.state.scores.player2;

    document.getElementById('powerValue').textContent = Math.round(this.state.power);
    document.getElementById('powerFill').style.height = `${this.state.power}%`;

    document.getElementById('angleValue').textContent = Math.round(this.state.angle);
    document.getElementById('angleArrow').style.transform = `translateX(-50%) rotate(-${this.state.angle}deg)`;

    document.getElementById('currentPlayerName').textContent = this.state.currentPlayer === 1 ? 'PLAYER 1' : (this.state.mode === 'vsComputer' ? 'COMPUTER' : 'PLAYER 2');
    document.getElementById('turnIndicator').className = this.state.currentPlayer === 1 ? 'player1' : 'player2';
  }

  togglePause() {
    this.state.isPaused = !this.state.isPaused;
    document.getElementById('pauseMenu').classList.toggle('hidden', !this.state.isPaused);
  }

  restartGame() {
    this.state.reset();
    document.getElementById('gameOverScreen').classList.add('hidden');
    document.getElementById('pauseMenu').classList.add('hidden');

    const level = LEVELS[this.state.currentLevel - 1];
    this.state.wind = (Math.random() - 0.5) * level.wind * 2;

    this.scene.resetBall(1);
    this.physics.resetBall(this.scene.ball.position);

    this.updateUI();
    this.startShotClock();
  }

  endGame() {
    this.state.isPlaying = false;
    if (this.shotClockInterval) clearInterval(this.shotClockInterval);

    const winner = this.state.scores.player1 > this.state.scores.player2 ? 'PLAYER 1' :
                   this.state.scores.player2 > this.state.scores.player1 ?
                   (this.state.mode === 'vsComputer' ? 'COMPUTER' : 'PLAYER 2') : 'TIE';

    document.getElementById('finalScore1').textContent = this.state.scores.player1;
    document.getElementById('finalScore2').textContent = this.state.scores.player2;

    const winnerEl = document.getElementById('winnerAnnouncement');
    if (winner === 'TIE') {
      winnerEl.textContent = "IT'S A TIE!";
    } else {
      winnerEl.textContent = `${winner} WINS!`;
    }

    document.getElementById('gameOverScreen').classList.remove('hidden');

    // Save score to Supabase
    this.saveScore();
  }

  async saveScore() {
    try {
      const winner = this.state.scores.player1 > this.state.scores.player2 ? 'Player 1' :
                     this.state.scores.player2 > this.state.scores.player1 ? 'Computer' : 'Draw';

      const highScore = Math.max(this.state.scores.player1, this.state.scores.player2);

      await supabase.from('basketball_scores').insert({
        player_name: this.state.mode === 'vsComputer' ? 'Player vs AI' : '2 Player Game',
        score: highScore,
        level: this.state.currentLevel,
        difficulty: this.state.difficulty,
        created_at: new Date().toISOString()
      });

      // Unlock next level if won
      if (winner === 'Player 1' || (this.state.mode === 'twoPlayer' && winner !== 'Computer')) {
        if (this.state.currentLevel >= this.state.unlockedLevels) {
          this.state.unlockedLevels = Math.min(LEVELS.length, this.state.currentLevel + 1);
          this.saveProgress();
        }
      }
    } catch (error) {
      console.log('Could not save score:', error);
    }
  }

  async saveProgress() {
    try {
      localStorage.setItem('basketball_unlocked_levels', this.state.unlockedLevels.toString());
    } catch (error) {
      console.log('Could not save progress');
    }
  }

  loadProgress() {
    try {
      const saved = localStorage.getItem('basketball_unlocked_levels');
      if (saved) {
        this.state.unlockedLevels = parseInt(saved, 10);
      }
    } catch (error) {
      this.state.unlockedLevels = 1;
    }
  }

  quitToMenu() {
    this.state.isPlaying = false;
    if (this.shotClockInterval) clearInterval(this.shotClockInterval);
    if (this.animationFrame) cancelAnimationFrame(this.animationFrame);

    document.getElementById('gameContainer').classList.add('hidden');
    document.getElementById('gameOverScreen').classList.add('hidden');
    document.getElementById('pauseMenu').classList.add('hidden');
    document.getElementById('startScreen').classList.remove('hidden');
  }
}

// Initialize database tables
async function initDatabase() {
  try {
    // Check if table exists, if not create it
    const { error } = await supabase.from('basketball_scores').select('id').limit(1);

    if (error && error.code === 'PGRST116') {
      // Table doesn't exist - it will be created via migration
      console.log('Database ready');
    }
  } catch (error) {
    console.log('Database check:', error);
  }
}

// Start the game
const game = new BasketballGame();
initDatabase();
