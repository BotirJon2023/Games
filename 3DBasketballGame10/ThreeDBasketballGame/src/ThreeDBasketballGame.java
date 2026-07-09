import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class ThreeDBasketballGame extends ApplicationAdapter {

    enum GameMode { PVP, VS_AI }
    enum Difficulty { EASY, NORMAL, HARD }

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private ModelBuilder modelBuilder;

    private Model courtModel, hoopModel, ballModel, player1Model, player2Model;
    private ModelInstance court, hoop, ball, player1, player2;

    private AnimationController player1Anim, player2Anim;

    private GameMode mode = GameMode.VS_AI;
    private Difficulty difficulty = Difficulty.NORMAL;

    private Vector3 p1Pos = new Vector3(-5, 0, 0);
    private Vector3 p2Pos = new Vector3(5, 0, 0);
    private Vector3 ballPos = new Vector3(0, 1.2f, 0);
    private Vector3 ballVel = new Vector3();

    private boolean ballInAir = false;
    private int score1 = 0, score2 = 0;
    private float aiThinkTimer = 0f;
    private float shootCooldown = 0f;

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 12f, 18f);
        camera.lookAt(0f, 2f, 0f);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.55f, 0.6f, 1f));
        environment.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight()
                .set(1f, 1f, 1f, -0.4f, -1f, -0.2f));

        courtModel = modelBuilder.createBox(
                30f, 0.2f, 18f,
                new Material(ColorAttribute.createDiffuse(new Color(0.85f, 0.55f, 0.25f, 1f))),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        hoopModel = modelBuilder.createBox(
                0.5f, 4f, 0.5f,
                new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        ballModel = modelBuilder.createSphere(
                0.6f, 0.6f, 0.6f, 24, 24,
                new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        player1Model = modelBuilder.createCapsule(
                0.8f, 2.0f, 16,
                new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        player2Model = modelBuilder.createCapsule(
                0.8f, 2.0f, 16,
                new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        court = new ModelInstance(courtModel);
        court.transform.setToTranslation(0f, -0.1f, 0f);

        hoop = new ModelInstance(hoopModel);
        hoop.transform.setToTranslation(12f, 2f, 0f);

        ball = new ModelInstance(ballModel);
        player1 = new ModelInstance(player1Model);
        player2 = new ModelInstance(player2Model);

        if (Gdx.input != null) {
            Gdx.input.setInputProcessor(new InputAdapter() {
                @Override
                public boolean keyDown(int keycode) {
                    if (keycode == Input.Keys.NUM_1) mode = GameMode.PVP;
                    if (keycode == Input.Keys.NUM_2) mode = GameMode.VS_AI;
                    if (keycode == Input.Keys.NUM_3) difficulty = Difficulty.EASY;
                    if (keycode == Input.Keys.NUM_4) difficulty = Difficulty.NORMAL;
                    if (keycode == Input.Keys.NUM_5) difficulty = Difficulty.HARD;
                    return true;
                }
            });
        }
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        updateGame(dt);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.08f, 0.10f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        ball.transform.setToTranslation(ballPos.x, ballPos.y, ballPos.z);
        player1.transform.setToTranslation(p1Pos.x, p1Pos.y, p1Pos.z);
        player2.transform.setToTranslation(p2Pos.x, p2Pos.y, p2Pos.z);

        camera.position.lerp(new Vector3(p1Pos.x, 12f, 18f), 0.02f);
        camera.lookAt(0f, 2f, 0f);
        camera.update();

        modelBatch.begin(camera);
        modelBatch.render(court, environment);
        modelBatch.render(hoop, environment);
        modelBatch.render(ball, environment);
        modelBatch.render(player1, environment);
        modelBatch.render(player2, environment);
        modelBatch.end();
    }

    private void updateGame(float dt) {
        shootCooldown = Math.max(0f, shootCooldown - dt);
        if (!ballInAir) {
            ballPos.set((p1Pos.x + p2Pos.x) * 0.5f, 1.2f, 0f);
        } else {
            ballVel.y -= 12f * dt;
            ballPos.mulAdd(ballVel, dt);
            if (ballPos.y <= 0.3f) {
                ballPos.y = 0.3f;
                ballInAir = false;
                ballVel.setZero();
            }
        }

        float moveSpeed = 6f * dt;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) p1Pos.x -= moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) p1Pos.x += moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) p1Pos.z -= moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) p1Pos.z += moveSpeed;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && shootCooldown == 0f) shootFrom(p1Pos, true);

        p1Pos.x = MathUtils.clamp(p1Pos.x, -13f, 13f);
        p1Pos.z = MathUtils.clamp(p1Pos.z, -8f, 8f);

        if (mode == GameMode.PVP) {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) p2Pos.x -= moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) p2Pos.x += moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) p2Pos.z -= moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) p2Pos.z += moveSpeed;
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && shootCooldown == 0f) shootFrom(p2Pos, false);
        } else {
            aiThinkTimer += dt;
            float aiSpeed = difficulty == Difficulty.EASY ? 3.5f : difficulty == Difficulty.NORMAL ? 5.5f : 7.5f;
            float targetX = ballInAir ? ballPos.x : 12f;
            float targetZ = ballInAir ? ballPos.z : 0f;

            if (Math.abs(targetX - p2Pos.x) > 0.2f) p2Pos.x += Math.signum(targetX - p2Pos.x) * aiSpeed * dt;
            if (Math.abs(targetZ - p2Pos.z) > 0.2f) p2Pos.z += Math.signum(targetZ - p2Pos.z) * aiSpeed * dt;

            p2Pos.x = MathUtils.clamp(p2Pos.x, -13f, 13f);
            p2Pos.z = MathUtils.clamp(p2Pos.z, -8f, 8f);

            float shootChance = difficulty == Difficulty.EASY ? 0.01f : difficulty == Difficulty.NORMAL ? 0.02f : 0.035f;
            if (!ballInAir && aiThinkTimer > 0.5f && MathUtils.randomBoolean(shootChance) && shootCooldown == 0f) {
                shootFrom(p2Pos, false);
                aiThinkTimer = 0f;
            }
        }

        checkScore();
    }

    private void shootFrom(Vector3 shooter, boolean isP1) {
        ballInAir = true;
        shootCooldown = 0.7f;
        ballPos.set(shooter.x, 1.4f, shooter.z);

        Vector3 hoopTarget = new Vector3(12f, 3.5f, 0f);
        Vector3 dir = hoopTarget.cpy().sub(ballPos).nor();

        float power = difficulty == Difficulty.EASY ? 8.5f : difficulty == Difficulty.NORMAL ? 10.5f : 12.5f;
        if (isP1) power += 0.2f;

        float spread = difficulty == Difficulty.EASY ? 2.0f : difficulty == Difficulty.NORMAL ? 1.0f : 0.35f;
        dir.x += MathUtils.random(-spread, spread) * 0.05f;
        dir.y += MathUtils.random(0.0f, spread) * 0.04f;
        dir.z += MathUtils.random(-spread, spread) * 0.05f;
        dir.nor();

        ballVel.set(dir.scl(power));
        ballVel.y += 3.2f;
    }

    private void checkScore() {
        if (ballPos.dst(12f, 3.5f, 0f) < 0.8f && ballInAir) {
            if (ballPos.x < 0f) score1++;
            else score2++;
            ballInAir = false;
            ballVel.setZero();
            ballPos.set(0f, 1.2f, 0f);
        }
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        courtModel.dispose();
        hoopModel.dispose();
        ballModel.dispose();
        player1Model.dispose();
        player2Model.dispose();
    }
}