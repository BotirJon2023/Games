package main.java;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class CreepyDollHorrorAdventure {
    // Window dimensions
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    // Game state
    private enum GameState {MAIN_MENU, PLAYING, PAUSED, GAME_OVER, WIN}

    private GameState currentState = GameState.MAIN_MENU;

    // Player variables
    private float playerX = 0.0f;
    private float playerY = 0.0f;
    private float playerZ = 5.0f;
    private float playerRotation = 0.0f;
    private float playerHealth = 100.0f;
    private float sanity = 100.0f;

    // Doll enemy class
    private class CreepyDoll {
        float x, y, z;
        float rotation;
        boolean isActive;
        boolean isVisible;
        float movementSpeed;
        int dollType; // Different types of creepy dolls

        public CreepyDoll(float x, float y, float z, int type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotation = 0.0f;
            this.isActive = true;
            this.isVisible = false;
            this.movementSpeed = 0.005f;
            this.dollType = type;
        }

        public void update(float playerX, float playerZ) {
            if (!isActive) return;

            // Calculate direction to player
            float dx = playerX - x;
            float dz = playerZ - z;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            // Only move if player is within certain range
            if (distance < 10.0f) {
                isVisible = true;
                rotation = (float) Math.toDegrees(Math.atan2(dx, dz));

                // Move toward player
                if (distance > 1.5f) {
                    x += (dx / distance) * movementSpeed;
                    z += (dz / distance) * movementSpeed;
                } else {
                    // Attack player
                    playerHealth -= 0.1f;
                    sanity -= 0.2f;
                }
            } else {
                isVisible = false;
            }

            // Random creepy behavior
            if (Math.random() < 0.001) {
                teleportNearPlayer();
            }
        }

        private void teleportNearPlayer() {
            float angle = (float) (Math.random() * Math.PI * 2);
            float distance = 3.0f + (float) Math.random() * 4.0f;
            x = playerX + (float) Math.sin(angle) * distance;
            z = playerZ + (float) Math.cos(angle) * distance;
        }

        public void render() {
            if (!isActive || !isVisible) return;

            glPushMatrix();
            glTranslatef(x, y, z);
            glRotatef(rotation, 0, 1, 0);

            // Different doll types have different appearances
            switch (dollType) {
                case 0:
                    renderPorcelainDoll();
                    break;
                case 1:
                    renderClothDoll();
                    break;
                case 2:
                    renderWoodenDoll();
                    break;
                case 3:
                    renderBrokenDoll();
                    break;
            }

            glPopMatrix();
        }

        private void renderPorcelainDoll() {
            // Head
            glColor3f(0.95f, 0.9f, 0.85f);
            glPushMatrix();
            glTranslatef(0, 1.7f, 0);
            GLU.gluSphere(gluNewQuadric(), 0.3f, 32, 32);
            glPopMatrix();

            // Eyes (black voids)
            glColor3f(0, 0, 0);
            glPushMatrix();
            glTranslatef(0.1f, 1.75f, 0.28f);
            GLU.gluSphere(gluNewQuadric(), 0.05f, 16, 16);
            glTranslatef(-0.2f, 0, 0);
            GLU.gluSphere(gluNewQuadric(), 0.05f, 16, 16);
            glPopMatrix();

            // Body
            glColor3f(0.8f, 0.75f, 0.7f);
            glPushMatrix();
            glTranslatef(0, 1.2f, 0);
            glScalef(0.5f, 0.8f, 0.3f);
            GLU.gluSphere(gluNewQuadric(), 0.4f, 32, 32);
            glPopMatrix();

            // Arms
            glPushMatrix();
            glTranslatef(0.3f, 1.3f, 0);
            glRotatef(30, 0, 0, 1);
            glScalef(0.8f, 0.2f, 0.2f);
            GLU.gluSphere(gluNewQuadric(), 0.3f, 32, 32);
            glPopMatrix();

            glPushMatrix();
            glTranslatef(-0.3f, 1.3f, 0);
            glRotatef(-30, 0, 0, 1);
            glScalef(0.8f, 0.2f, 0.2f);
            GLU.gluSphere(gluNewQuadric(), 0.3f, 32, 32);
            glPopMatrix();

            // Dress
            glColor3f(0.4f, 0.2f, 0.3f);
            glPushMatrix();
            glTranslatef(0, 0.8f, 0);
            glRotatef(90, 1, 0, 0);
            GLU.gluCylinder(gluNewQuadric(), 0.5f, 0.7f, 0.8f, 32, 32);
            glPopMatrix();
        }

        private void renderClothDoll() {
            // Simplified cloth doll rendering
            glColor3f(0.6f, 0.4f, 0.3f);

            // Head
            glPushMatrix();
            glTranslatef(0, 1.5f, 0);
            GLU.gluSphere(gluNewQuadric(), 0.4f, 32, 32);
            glPopMatrix();

            // Button eyes
            glColor3f(0, 0, 0);
            glPushMatrix();
            glTranslatef(0.15f, 1.55f, 0.35f);
            GLU.gluSphere(gluNewQuadric(), 0.08f, 16, 16);
            glTranslatef(-0.3f, 0, 0);
            GLU.gluSphere(gluNewQuadric(), 0.08f, 16, 16);
            glPopMatrix();

            // Stitched mouth
            glBegin(GL_LINE_STRIP);
            for (int i = 0; i < 10; i++) {
                float x = -0.2f + i * 0.04f;
                float y = 1.45f - (float) Math.sin(i * 0.3f) * 0.03f;
                glVertex3f(x, y, 0.35f);
            }
            glEnd();

            // Body
            glColor3f(0.5f, 0.3f, 0.2f);
            glPushMatrix();
            glTranslatef(0, 1.0f, 0);
            glScalef(0.6f, 1.0f, 0.4f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 32, 32);
            glPopMatrix();
        }

        private void renderWoodenDoll() {
            // Wooden doll with jointed limbs
            glColor3f(0.5f, 0.35f, 0.2f);

            // Head
            glPushMatrix();
            glTranslatef(0, 1.6f, 0);
            glScalef(0.35f, 0.4f, 0.35f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 32, 32);
            glPopMatrix();

            // Carved eyes and mouth
            glColor3f(0.3f, 0.2f, 0.1f);
            // Eyes
            glPushMatrix();
            glTranslatef(0.15f, 1.65f, 0.3f);
            GLU.gluDisk(gluNewQuadric(), 0, 0.05f, 16, 1);
            glTranslatef(-0.3f, 0, 0);
            GLU.gluDisk(gluNewQuadric(), 0, 0.05f, 16, 1);
            glPopMatrix();

            // Mouth (carved smile)
            glBegin(GL_LINE_STRIP);
            for (int i = 0; i < 10; i++) {
                float angle = (float) (Math.PI * 0.8 * (i / 9.0f - 0.5f));
                glVertex3f((float) Math.sin(angle) * 0.2f, 1.55f + (float) Math.cos(angle) * 0.05f, 0.3f);
            }
            glEnd();

            // Body
            glColor3f(0.6f, 0.4f, 0.25f);
            glPushMatrix();
            glTranslatef(0, 1.2f, 0);
            glScalef(0.4f, 0.8f, 0.3f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 32, 32);
            glPopMatrix();

            // Jointed limbs
            renderWoodenLimb(0.3f, 1.3f, 0, 30, 0.2f, 0.7f); // Right arm
            renderWoodenLimb(-0.3f, 1.3f, 0, -30, 0.2f, 0.7f); // Left arm
            renderWoodenLimb(0.15f, 0.5f, 0, 0, 0.2f, 0.8f); // Right leg
            renderWoodenLimb(-0.15f, 0.5f, 0, 0, 0.2f, 0.8f); // Left leg
        }

        private void renderWoodenLimb(float x, float y, float z, float rotation, float thickness, float length) {
            glPushMatrix();
            glTranslatef(x, y, z);
            glRotatef(rotation, 0, 0, 1);

            // Upper part
            glPushMatrix();
            glScalef(thickness, length, thickness);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
            glPopMatrix();

            // Joint
            glColor3f(0.3f, 0.2f, 0.1f);
            glTranslatef(0, -length / 2, 0);
            GLU.gluSphere(gluNewQuadric(), thickness * 0.7f, 16, 16);

            // Lower part
            glColor3f(0.6f, 0.4f, 0.25f);
            glTranslatef(0, -length / 2, 0);
            glScalef(thickness, length, thickness);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);

            glPopMatrix();
        }

        private void renderBrokenDoll() {
            // Doll with broken parts and missing pieces
            glColor3f(0.7f, 0.6f, 0.6f);

            // Cracked head
            glPushMatrix();
            glTranslatef(0, 1.7f, 0);
            GLU.gluSphere(gluNewQuadric(), 0.3f, 32, 32);

            // Cracks
            glColor3f(0.3f, 0.3f, 0.3f);
            glBegin(GL_LINES);
            // Random cracks
            for (int i = 0; i < 5; i++) {
                float angle1 = (float) (Math.random() * Math.PI * 2);
                float angle2 = (float) (Math.random() * Math.PI * 2);
                glVertex3f((float) Math.sin(angle1) * 0.3f, (float) Math.cos(angle1) * 0.3f, (float) Math.cos(angle2) * 0.3f);
                glVertex3f((float) Math.sin(angle1 * 1.2f) * 0.3f, (float) Math.cos(angle1 * 1.2f) * 0.3f, (float) Math.cos(angle2 * 1.2f) * 0.3f);
            }
            glEnd();
            glPopMatrix();

            // Missing eye
            glColor3f(0, 0, 0);
            glPushMatrix();
            glTranslatef(0.1f, 1.75f, 0.28f);
            GLU.gluSphere(gluNewQuadric(), 0.05f, 16, 16);
            glPopMatrix();

            // Hollow eye socket
            glPushMatrix();
            glTranslatef(-0.1f, 1.75f, 0.28f);
            glColor3f(0.2f, 0.2f, 0.2f);
            GLU.gluDisk(gluNewQuadric(), 0, 0.08f, 16, 1);
            glPopMatrix();

            // Broken body
            glColor3f(0.6f, 0.5f, 0.5f);
            glPushMatrix();
            glTranslatef(0, 1.2f, 0);
            glScalef(0.5f, 0.8f, 0.3f);
            GLU.gluSphere(gluNewQuadric(), 0.4f, 32, 32);
            glPopMatrix();

            // Missing arm
            glPushMatrix();
            glTranslatef(-0.3f, 1.3f, 0);
            glRotatef(-30, 0, 0, 1);
            glScalef(0.8f, 0.2f, 0.2f);
            GLU.gluSphere(gluNewQuadric(), 0.3f, 32, 32);
            glPopMatrix();

            // Torn dress
            glColor3f(0.3f, 0.1f, 0.2f);
            glBegin(GL_TRIANGLE_FAN);
            glVertex3f(0, 0.8f, 0);
            for (int i = 0; i <= 10; i++) {
                float angle = (float) (i * Math.PI * 2 / 10);
                float radius = 0.5f + (float) Math.random() * 0.2f;
                glVertex3f((float) Math.sin(angle) * radius, 0.0f, (float) Math.cos(angle) * radius * 0.7f);
            }
            glEnd();
        }
    }

    // List of creepy dolls in the game
    private List<CreepyDoll> dolls = new ArrayList<>();

    // Environment variables
    private boolean flashlightOn = true;
    private float ambientLight = 0.1f;
    private float fogDensity = 0.01f;

    // Game items and objectives
    private class GameItem {
        float x, y, z;
        int itemType; // 0 = key, 1 = battery, 2 = note, 3 = doll part
        boolean collected;

        public GameItem(float x, float y, float z, int type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.itemType = type;
            this.collected = false;
        }

        public void render() {
            if (collected) return;

            glPushMatrix();
            glTranslatef(x, y, z);

            switch (itemType) {
                case 0:
                    renderKey();
                    break;
                case 1:
                    renderBattery();
                    break;
                case 2:
                    renderNote();
                    break;
                case 3:
                    renderDollPart();
                    break;
            }

            glPopMatrix();
        }

        private void renderKey() {
            glColor3f(0.8f, 0.7f, 0.1f);

            // Key bow
            glPushMatrix();
            glTranslatef(0, 0.2f, 0);
            GLU.gluDisk(gluNewQuadric(), 0.1f, 0.15f, 16, 1);
            glPopMatrix();

            // Key shaft
            glPushMatrix();
            glTranslatef(0, 0.1f, 0);
            glScalef(0.05f, 0.2f, 0.05f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
            glPopMatrix();

            // Key teeth
            glPushMatrix();
            glTranslatef(0, 0, 0);
            glScalef(0.15f, 0.05f, 0.05f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
            glPopMatrix();
        }

        private void renderBattery() {
            glColor3f(0.2f, 0.2f, 0.2f);

            // Main body
            glPushMatrix();
            glScalef(0.15f, 0.3f, 0.1f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
            glPopMatrix();

            // Positive end
            glColor3f(0.8f, 0.1f, 0.1f);
            glPushMatrix();
            glTranslatef(0, 0.2f, 0);
            glScalef(0.1f, 0.05f, 0.1f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
            glPopMatrix();
        }

        private void renderNote() {
            glColor3f(0.9f, 0.9f, 0.7f);

            // Paper
            glPushMatrix();
            glRotatef(45, 0, 1, 0);
            glScalef(0.2f, 0.01f, 0.3f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
            glPopMatrix();
        }

        private void renderDollPart() {
            glColor3f(0.8f, 0.6f, 0.6f);

            // Doll eye
            glPushMatrix();
            glTranslatef(0, 0.1f, 0);
            GLU.gluSphere(gluNewQuadric(), 0.1f, 16, 16);

            // Iris
            glColor3f(0.1f, 0.5f, 0.1f);
            glTranslatef(0, 0, 0.05f);
            GLU.gluSphere(gluNewQuadric(), 0.05f, 16, 16);
            glPopMatrix();
        }
    }

    // List of game items
    private List<GameItem> items = new ArrayList<>();

    // Inventory
    private boolean[] inventory = new boolean[10];
    private int keysCollected = 0;
    private int batteriesCollected = 0;
    private int dollPartsCollected = 0;

    // GLFW window handle
    private long window;

    // GLU quadric for rendering
    private GLU glu;

    // Timing variables
    private double lastTime;
    private float deltaTime;

    // Sound system (placeholder - would use OpenAL or similar in real implementation)
    private class SoundSystem {
        public void playSound(String sound) {
            // In a real implementation, this would play the specified sound
            System.out.println("Playing sound: " + sound);
        }

        public void playAmbient(String ambient) {
            // Loop ambient sounds
            System.out.println("Playing ambient: " + ambient);
        }
    }

    private SoundSystem soundSystem = new SoundSystem();

    // Shader program (simplified for this example)
    private class ShaderProgram {
        public void use() {
            // In a real implementation, this would activate the shader program
        }

        public void setUniform(String name, float value) {
            // Set uniform value
        }
    }

    private ShaderProgram mainShader = new ShaderProgram();

    // Initialize the game
    public void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Creepy Doll Horror Adventure", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Center the window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);
        // Make the window visible
        glfwShowWindow(window);

        // Initialize OpenGL
        GL.createCapabilities();
        glu = new GLU();

        // Set up initial OpenGL state
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Set up lighting
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);

        float[] lightPos = {0.0f, 5.0f, 5.0f, 1.0f};
        glLightfv(GL_LIGHT0, GL_POSITION, lightPos);

        float[] lightAmbient = {ambientLight, ambientLight, ambientLight, 1.0f};
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);

        float[] lightDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);

        // Set up fog
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_EXP);
        glFogf(GL_FOG_DENSITY, fogDensity);
        float[] fogColor = {0.1f, 0.1f, 0.1f, 1.0f};
        glFogfv(GL_FOG_COLOR, fogColor);

        // Initialize game objects
        initializeGameObjects();

        // Set up callbacks
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                togglePause();
            }

            if (currentState == GameState.PLAYING) {
                if (key == GLFW_KEY_F && action == GLFW_RELEASE) {
                    toggleFlashlight();
                }

                if (key == GLFW_KEY_E && action == GLFW_RELEASE) {
                    checkForItems();
                }
            } else if (currentState == GameState.MAIN_MENU) {
                if (key == GLFW_KEY_ENTER && action == GLFW_RELEASE) {
                    startGame();
                }
            } else if (currentState == GameState.PAUSED) {
                if (key == GLFW_KEY_ENTER && action == GLFW_RELEASE) {
                    togglePause();
                }
            } else if (currentState == GameState.GAME_OVER || currentState == GameState.WIN) {
                if (key == GLFW_KEY_ENTER && action == GLFW_RELEASE) {
                    resetGame();
                }
            }
        });

        // Initialize timing
        lastTime = glfwGetTime();
    }

    // Initialize game objects (dolls, items, etc.)
    private void initializeGameObjects() {
        // Create creepy dolls
        dolls.add(new CreepyDoll(5.0f, 0.0f, 5.0f, 0)); // Porcelain doll
        dolls.add(new CreepyDoll(-3.0f, 0.0f, 7.0f, 1)); // Cloth doll
        dolls.add(new CreepyDoll(2.0f, 0.0f, -4.0f, 2)); // Wooden doll
        dolls.add(new CreepyDoll(-5.0f, 0.0f, -3.0f, 3)); // Broken doll
        dolls.add(new CreepyDoll(0.0f, 0.0f, 8.0f, 0)); // Another porcelain doll

        // Create game items
        items.add(new GameItem(3.0f, 0.5f, 3.0f, 0)); // Key
        items.add(new GameItem(-2.0f, 0.5f, -1.0f, 1)); // Battery
        items.add(new GameItem(4.0f, 0.5f, -2.0f, 2)); // Note
        items.add(new GameItem(-3.0f, 0.5f, 4.0f, 3)); // Doll part
        items.add(new GameItem(1.0f, 0.5f, 6.0f, 1)); // Battery
        items.add(new GameItem(-4.0f, 0.5f, -5.0f, 0)); // Key
    }

    // Main game loop
    public void run() {
        while (!glfwWindowShouldClose(window)) {
            // Calculate delta time
            double currentTime = glfwGetTime();
            deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // Update game state
            update();

            // Render the game
            render();

            // Poll for window events
            glfwPollEvents();
        }
    }

    // Update game state
    private void update() {
        if (currentState != GameState.PLAYING) return;

        // Update player based on input
        updatePlayer();

        // Update all dolls
        for (CreepyDoll doll : dolls) {
            doll.update(playerX, playerZ);
        }

        // Update sanity (decreases over time, faster when dolls are near)
        float sanityDecrease = 0.01f;
        for (CreepyDoll doll : dolls) {
            if (doll.isVisible) {
                float dx = doll.x - playerX;
                float dz = doll.z - playerZ;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);
                sanityDecrease += 0.05f / (distance + 0.1f);
            }
        }
        sanity -= sanityDecrease * deltaTime * 60.0f;

        // Check for game over conditions
        if (playerHealth <= 0 || sanity <= 0) {
            currentState = GameState.GAME_OVER;
            soundSystem.playSound("game_over");
        }

        // Check for win condition (collect all keys and doll parts)
        if (keysCollected >= 2 && dollPartsCollected >= 1) {
            currentState = GameState.WIN;
            soundSystem.playSound("win");
        }

        // Random creepy sounds
        if (Math.random() < 0.001) {
            soundSystem.playSound("creepy_laugh");
        } else if (Math.random() < 0.001) {
            soundSystem.playSound("whisper");
        } else if (Math.random() < 0.001) {
            soundSystem.playSound("doll_giggle");
        }
    }

    // Update player position and rotation based on input
    private void updatePlayer() {
        // Rotation
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) {
            playerRotation += 1.5f * deltaTime * 60.0f;
        }
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) {
            playerRotation -= 1.5f * deltaTime * 60.0f;
        }

        // Movement
        float moveSpeed = 0.05f * deltaTime * 60.0f;
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) {
            playerX += (float) Math.sin(Math.toRadians(playerRotation)) * moveSpeed;
            playerZ += (float) Math.cos(Math.toRadians(playerRotation)) * moveSpeed;
        }
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) {
            playerX -= (float) Math.sin(Math.toRadians(playerRotation)) * moveSpeed;
            playerZ -= (float) Math.cos(Math.toRadians(playerRotation)) * moveSpeed;
        }

        // Strafe
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            playerX += (float) Math.sin(Math.toRadians(playerRotation - 90)) * moveSpeed;
            playerZ += (float) Math.cos(Math.toRadians(playerRotation - 90)) * moveSpeed;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            playerX += (float) Math.sin(Math.toRadians(playerRotation + 90)) * moveSpeed;
            playerZ += (float) Math.cos(Math.toRadians(playerRotation + 90)) * moveSpeed;
        }
    }

    // Check for items near player that can be collected
    private void checkForItems() {
        for (GameItem item : items) {
            if (item.collected) continue;

            float dx = item.x - playerX;
            float dz = item.z - playerZ;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);

            if (distance < 1.5f) {
                item.collected = true;
                soundSystem.playSound("item_pickup");

                switch (item.itemType) {
                    case 0: // Key
                        keysCollected++;
                        break;
                    case 1: // Battery
                        batteriesCollected++;
                        break;
                    case 2: // Note
                        // Would display note text in a real implementation
                        break;
                    case 3: // Doll part
                        dollPartsCollected++;
                        break;
                }
            }
        }
    }

    // Toggle flashlight
    private void toggleFlashlight() {
        flashlightOn = !flashlightOn;
        soundSystem.playSound("flashlight_toggle");

        if (flashlightOn) {
            float[] lightDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
            glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
        } else {
            float[] lightDiffuse = {0.1f, 0.1f, 0.1f, 1.0f};
            glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
        }
    }

    // Toggle pause state
    private void togglePause() {
        if (currentState == GameState.PLAYING) {
            currentState = GameState.PAUSED;
            soundSystem.playSound("pause");
        } else if (currentState == GameState.PAUSED) {
            currentState = GameState.PLAYING;
            soundSystem.playSound("unpause");
        }
    }

    // Start the game from main menu
    private void startGame() {
        currentState = GameState.PLAYING;
        soundSystem.playAmbient("creepy_ambient");
        soundSystem.playSound("game_start");
    }

    // Reset the game after game over or win
    private void resetGame() {
        playerX = 0.0f;
        playerY = 0.0f;
        playerZ = 5.0f;
        playerRotation = 0.0f;
        playerHealth = 100.0f;
        sanity = 100.0f;

        keysCollected = 0;
        batteriesCollected = 0;
        dollPartsCollected = 0;

        // Reset dolls
        dolls.clear();
        initializeGameObjects();

        // Reset items
        for (GameItem item : items) {
            item.collected = false;
        }

        currentState = GameState.MAIN_MENU;
    }

    // Render the game
    private void render() {
        // Clear the framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Set up projection matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GLU.gluPerspective(60.0f, (float) WIDTH / (float) HEIGHT, 0.1f, 100.0f);

        // Set up view matrix
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Apply player rotation and position
        glRotatef(playerRotation, 0.0f, 1.0f, 0.0f);
        glTranslatef(-playerX, -playerY, -playerZ);

        // Update light position to follow player (flashlight effect)
        float[] lightPos = {
                playerX + (float) Math.sin(Math.toRadians(playerRotation)) * 0.5f,
                playerY + 0.5f,
                playerZ + (float) Math.cos(Math.toRadians(playerRotation)) * 0.5f,
                1.0f
        };
        glLightfv(GL_LIGHT0, GL_POSITION, lightPos);

        // Render environment
        renderEnvironment();

        // Render dolls
        for (CreepyDoll doll : dolls) {
            doll.render();
        }

        // Render items
        for (GameItem item : items) {
            item.render();
        }

        // Render UI based on game state
        renderUI();

        // Swap the color buffers
        glfwSwapBuffers(window);
    }

    // Render the game environment
    private void renderEnvironment() {
        // Floor
        glColor3f(0.3f, 0.3f, 0.3f);
        glBegin(GL_QUADS);
        glVertex3f(-20.0f, 0.0f, -20.0f);
        glVertex3f(-20.0f, 0.0f, 20.0f);
        glVertex3f(20.0f, 0.0f, 20.0f);
        glVertex3f(20.0f, 0.0f, -20.0f);
        glEnd();

        // Walls (simplified for this example)
        glColor3f(0.4f, 0.4f, 0.4f);

        // North wall
        glBegin(GL_QUADS);
        glVertex3f(-20.0f, 0.0f, -20.0f);
        glVertex3f(-20.0f, 5.0f, -20.0f);
        glVertex3f(20.0f, 5.0f, -20.0f);
        glVertex3f(20.0f, 0.0f, -20.0f);
        glEnd();

        // South wall
        glBegin(GL_QUADS);
        glVertex3f(-20.0f, 0.0f, 20.0f);
        glVertex3f(20.0f, 0.0f, 20.0f);
        glVertex3f(20.0f, 5.0f, 20.0f);
        glVertex3f(-20.0f, 5.0f, 20.0f);
        glEnd();

        // West wall
        glBegin(GL_QUADS);
        glVertex3f(-20.0f, 0.0f, -20.0f);
        glVertex3f(-20.0f, 0.0f, 20.0f);
        glVertex3f(-20.0f, 5.0f, 20.0f);
        glVertex3f(-20.0f, 5.0f, -20.0f);
        glEnd();

        // East wall
        glBegin(GL_QUADS);
        glVertex3f(20.0f, 0.0f, -20.0f);
        glVertex3f(20.0f, 5.0f, -20.0f);
        glVertex3f(20.0f, 5.0f, 20.0f);
        glVertex3f(20.0f, 0.0f, 20.0f);
        glEnd();

        // Ceiling
        glColor3f(0.2f, 0.2f, 0.2f);
        glBegin(GL_QUADS);
        glVertex3f(-20.0f, 5.0f, -20.0f);
        glVertex3f(-20.0f, 5.0f, 20.0f);
        glVertex3f(20.0f, 5.0f, 20.0f);
        glVertex3f(20.0f, 5.0f, -20.0f);
        glEnd();

        // Some furniture or props to make the environment more interesting
        renderFurniture();
    }

    // Render furniture and props
    private void renderFurniture() {
        // A table
        glColor3f(0.5f, 0.35f, 0.2f);
        glPushMatrix();
        glTranslatef(3.0f, 0.5f, 4.0f);
        glScalef(1.5f, 0.1f, 1.5f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        // Table legs
        glPushMatrix();
        glTranslatef(2.4f, 0.0f, 3.4f);
        glScalef(0.1f, 0.5f, 0.1f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(3.6f, 0.0f, 3.4f);
        glScalef(0.1f, 0.5f, 0.1f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(2.4f, 0.0f, 4.6f);
        glScalef(0.1f, 0.5f, 0.1f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(3.6f, 0.0f, 4.6f);
        glScalef(0.1f, 0.5f, 0.1f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        // A chair
        glPushMatrix();
        glTranslatef(2.5f, 0.3f, 2.5f);
        glRotatef(180, 0, 1, 0);

        // Seat
        glPushMatrix();
        glTranslatef(0, 0.2f, 0);
        glScalef(0.8f, 0.1f, 0.8f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        // Back
        glPushMatrix();
        glTranslatef(0, 0.6f, -0.4f);
        glScalef(0.8f, 0.8f, 0.1f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        // Legs
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                glPushMatrix();
                glTranslatef(-0.3f + i * 0.6f, 0.0f, -0.3f + j * 0.6f);
                glScalef(0.1f, 0.2f, 0.1f);
                GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
                glPopMatrix();
            }
        }

        glPopMatrix();

        // A bookshelf
        glColor3f(0.4f, 0.3f, 0.2f);
        glPushMatrix();
        glTranslatef(-4.0f, 2.5f, -3.0f);
        glScalef(0.8f, 2.5f, 0.3f);
        GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
        glPopMatrix();

        // Some books
        for (int i = 0; i < 4; i++) {
            glColor3f(i * 0.1f, 0.2f, 0.3f + i * 0.1f);
            glPushMatrix();
            glTranslatef(-4.2f, 1.0f + i * 0.5f, -2.8f);
            glScalef(0.1f, 0.4f, 0.5f);
            GLU.gluSphere(gluNewQuadric(), 0.5f, 16, 16);
            glPopMatrix();
        }
    }

    // Render UI elements
    private void renderUI() {
        // Switch to orthographic projection for UI
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, WIDTH, HEIGHT, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Disable lighting for UI
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);

        // Render different UI based on game state
        switch (currentState) {
            case MAIN_MENU:
                renderMainMenu();
                break;
            case PLAYING:
                renderHUD();
                break;
            case PAUSED:
                renderHUD();
                renderPauseMenu();
                break;
            case GAME_OVER:
                renderGameOverScreen();
                break;
            case WIN:
                renderWinScreen();
                break;
        }

        // Restore state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    // Render main menu
    private void renderMainMenu() {
        // Dark background with slight transparency
        glColor4f(0.0f, 0.0f, 0.0f, 0.7f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(WIDTH, 0);
        glVertex2f(WIDTH, HEIGHT);
        glVertex2f(0, HEIGHT);
        glEnd();

        // Title
        glColor3f(0.8f, 0.1f, 0.1f);
        renderText("CREEPY DOLL HORROR ADVENTURE", WIDTH / 2 - 250, HEIGHT / 3, 24);

        // Instructions
        glColor3f(0.8f, 0.8f, 0.8f);
        renderText("Press ENTER to Start", WIDTH / 2 - 100, HEIGHT / 2, 18);
        renderText("Use Arrow Keys to Move", WIDTH / 2 - 120, HEIGHT / 2 + 40, 16);
        renderText("F to Toggle Flashlight", WIDTH / 2 - 110, HEIGHT / 2 + 70, 16);
        renderText("E to Interact", WIDTH / 2 - 70, HEIGHT / 2 + 100, 16);
        renderText("ESC to Pause", WIDTH / 2 - 70, HEIGHT / 2 + 130, 16);
    }

    // Render heads-up display during gameplay
    private void renderHUD() {
        // Health bar
        glColor3f(0.7f, 0.0f, 0.0f);
        glBegin(GL_QUADS);
        glVertex2f(20, 20);
        glVertex2f(20 + playerHealth * 2, 20);
        glVertex2f(20 + playerHealth * 2, 40);
        glVertex2f(20, 40);
        glEnd();

        // Health bar outline
        glColor3f(1.0f, 1.0f, 1.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(20, 20);
        glVertex2f(220, 20);
        glVertex2f(220, 40);
        glVertex2f(20, 40);
        glEnd();

        // Sanity meter
        glColor3f(0.4f, 0.4f, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(20, 50);
        glVertex2f(20 + sanity * 2, 50);
        glVertex2f(20 + sanity * 2, 70);
        glVertex2f(20, 70);
        glEnd();

        // Sanity meter outline
        glColor3f(1.0f, 1.0f, 1.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(20, 50);
        glVertex2f(220, 50);
        glVertex2f(220, 70);
        glVertex2f(20, 70);
        glEnd();

        // Inventory indicators
        glColor3f(0.8f, 0.8f, 0.0f);
        renderText("Keys: " + keysCollected + "/2", WIDTH - 150, 20, 16);

        glColor3f(0.8f, 0.6f, 0.6f);
        renderText("Doll Parts: " + dollPartsCollected + "/1", WIDTH - 150, 50, 16);

        // Flashlight indicator
        if (flashlightOn) {
            glColor3f(0.9f, 0.9f, 0.3f);
        } else {
            glColor3f(0.3f, 0.3f, 0.3f);
        }
        renderText("Flashlight: " + (flashlightOn ? "ON" : "OFF"), WIDTH - 150, 80, 16);

        // Crosshair
        glColor3f(1.0f, 0.0f, 0.0f);
        glBegin(GL_LINES);
        glVertex2f(WIDTH / 2 - 10, HEIGHT / 2);
        glVertex2f(WIDTH / 2 + 10, HEIGHT / 2);
        glVertex2f(WIDTH / 2, HEIGHT / 2 - 10);
        glVertex2f(WIDTH / 2, HEIGHT / 2 + 10);
        glEnd();
    }

    // Render pause menu
    private void renderPauseMenu() {
        // Dark overlay
        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(WIDTH, 0);
        glVertex2f(WIDTH, HEIGHT);
        glVertex2f(0, HEIGHT);
        glEnd();

        // Pause text
        glColor3f(0.8f, 0.8f, 0.8f);
        renderText("PAUSED", WIDTH / 2 - 50, HEIGHT / 3, 24);
        renderText("Press ENTER to Resume", WIDTH / 2 - 100, HEIGHT / 2, 18);
        renderText("Press ESC to Quit", WIDTH / 2 - 80, HEIGHT / 2 + 40, 16);
    }

    // Render game over screen
    private void renderGameOverScreen() {
        // Dark red overlay
        glColor4f(0.3f, 0.0f, 0.0f, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(WIDTH, 0);
        glVertex2f(WIDTH, HEIGHT);
        glVertex2f(0, HEIGHT);
        glEnd();

        // Game over text
        glColor3f(0.9f, 0.1f, 0.1f);
        renderText("GAME OVER", WIDTH / 2 - 100, HEIGHT / 3, 36);

        // Stats
        glColor3f(0.8f, 0.8f, 0.8f);
        renderText("You became another victim of the dolls...", WIDTH / 2 - 200, HEIGHT / 2, 18);
        renderText("Keys Collected: " + keysCollected + "/2", WIDTH / 2 - 80, HEIGHT / 2 + 40, 16);
        renderText("Doll Parts Found: " + dollPartsCollected + "/1", WIDTH / 2 - 100, HEIGHT / 2 + 70, 16);

        // Restart prompt
        glColor3f(0.9f, 0.9f, 0.3f);
        renderText("Press ENTER to Return to Main Menu", WIDTH / 2 - 180, HEIGHT - 100, 18);
    }

    // Render win screen
    private void renderWinScreen() {
        // Dark green overlay
        glColor4f(0.0f, 0.3f, 0.0f, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(WIDTH, 0);
        glVertex2f(WIDTH, HEIGHT);
        glVertex2f(0, HEIGHT);
        glEnd();

        // Win text
        glColor3f(0.1f, 0.9f, 0.1f);
        renderText("YOU ESCAPED!", WIDTH / 2 - 120, HEIGHT / 3, 36);

        // Stats
        glColor3f(0.8f, 0.8f, 0.8f);
        renderText("You found all the necessary items and escaped the haunted house.",
                WIDTH / 2 - 300, HEIGHT / 2, 18);
        renderText("But the dolls will be waiting for your return...",
                WIDTH / 2 - 220, HEIGHT / 2 + 30, 18);

        // Restart prompt
        glColor3f(0.9f, 0.9f, 0.3f);
        renderText("Press ENTER to Play Again", WIDTH / 2 - 150, HEIGHT - 100, 18);
    }

    // Simplified text rendering (would use proper font rendering in a real game)
    private void renderText(String text, float x, float y, int size) {
        // In a real implementation, this would use a proper font rendering system
        // For this example, we'll just print the text to console
        System.out.println("Rendering text: " + text + " at (" + x + "," + y + ") size " + size);
    }

    // Clean up resources
    public void cleanup() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    // Main method
    public static void main(String[] args) {
        CreepyDollHorrorAdventure game = new CreepyDollHorrorAdventure();

        try {
            game.init();
            game.run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            game.cleanup();
        }
    }
}