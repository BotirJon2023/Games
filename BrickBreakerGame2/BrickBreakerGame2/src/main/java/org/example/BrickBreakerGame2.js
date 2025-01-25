<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Break Bricks Game</title>
<style>
body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; height: 100vh; background-color: #000; }
canvas { background-color: #333; border: 2px solid #fff; }
    </style>
</head>
<body>
    <canvas id="gameCanvas" width="480" height="320"></canvas>

    <script>
        const canvas = document.getElementById("gameCanvas");
        const ctx = canvas.getContext("2d");

        const paddleWidth = 75, paddleHeight = 10, ballRadius = 5;
let paddleX = (canvas.width - paddleWidth) / 2;
        let ballX = canvas.width / 2, ballY = canvas.height - 30;
        let ballSpeedX = 4, ballSpeedY = -4;
        let rightPressed = false, leftPressed = false;
        let score = 0, lives = 3;
        const brickRowCount = 5, brickColumnCount = 7;
        const brickWidth = 60, brickHeight = 20, brickPadding = 10, brickOffsetTop = 30, brickOffsetLeft = 30;
        const bricks = [];

        // Initialize bricks array
        for(let c = 0; c < brickColumnCount; c++) {
bricks[c] = [];
        for(let r = 0; r < brickRowCount; r++) {
bricks[c][r] = { x: 0, y: 0, status: 1 };
        }
        }

        // Event listeners for paddle movement
        document.addEventListener("keydown", keyDownHandler, false);
        document.addEventListener("keyup", keyUpHandler, false);

function keyDownHandler(e) {
    if(e.key === "Right" || e.key === "ArrowRight") {
        rightPressed = true;
    } else if(e.key === "Left" || e.key === "ArrowLeft") {
        leftPressed = true;
    }
}

function keyUpHandler(e) {
    if(e.key === "Right" || e.key === "ArrowRight") {
        rightPressed = false;
    } else if(e.key === "Left" || e.key === "ArrowLeft") {
        leftPressed = false;
    }
}

function collisionDetection() {
    for(let c = 0; c < brickColumnCount; c++) {
        for(let r = 0; r < brickRowCount; r++) {
            let b = bricks[c][r];
            if(b.status === 1) {
                if(ballX > b.x && ballX < b.x + brickWidth && ballY > b.y && ballY < b.y + brickHeight) {
                    ballSpeedY = -ballSpeedY;
                    b.status = 0;
                    score++;
                    if(score === brickRowCount * brickColumnCount) {
                        alert("You win!");
                        document.location.reload();
                    }
                }
            }
        }
    }
}

function drawBall() {
    ctx.beginPath();
    ctx.arc(ballX, ballY, ballRadius, 0, Math.PI * 2);
    ctx.fillStyle = "#0095DD";
    ctx.fill();
    ctx.closePath();
}

function drawPaddle() {
    ctx.beginPath();
    ctx.rect(paddleX, canvas.height - paddleHeight, paddleWidth, paddleHeight);
    ctx.fillStyle = "#0095DD";
    ctx.fill();
    ctx.closePath();
}

function drawBricks() {
    for(let c = 0; c < brickColumnCount; c++) {
        for(let r = 0; r < brickRowCount; r++) {
            if(bricks[c][r].status === 1) {
                let brickX = c * (brickWidth + brickPadding) + brickOffsetLeft;
                let brickY = r * (brickHeight + brickPadding) + brickOffsetTop;
                bricks[c][r].x = brickX;
                bricks[c][r].y = brickY;
                ctx.beginPath();
                ctx.rect(brickX, brickY, brickWidth, brickHeight);
                ctx.fillStyle = "#0095DD";
                ctx.fill();
                ctx.closePath();
            }
        }
    }
}

function drawScore() {
    ctx.font = "16px Arial";
    ctx.fillStyle = "#0095DD";
    ctx.fillText("Score: " + score, 8, 20);
}

function drawLives() {
    ctx.font = "16px Arial";
    ctx.fillStyle = "#0095DD";
    ctx.fillText("Lives: " + lives, canvas.width - 65, 20);
}

function updateBallPosition() {
    ballX += ballSpeedX;
    ballY += ballSpeedY;

    if(ballX + ballRadius > canvas.width || ballX - ballRadius < 0) {
        ballSpeedX = -ballSpeedX;
    }
    if(ballY - ballRadius < 0) {
        ballSpeedY = -ballSpeedY;
    } else if(ballY + ballRadius > canvas.height) {
        if(ballX > paddleX && ballX < paddleX + paddleWidth) {
            ballSpeedY = -ballSpeedY;
        } else {
            lives--;
            if(lives === 0) {
                alert("GAME OVER");
                document.location.reload();
            } else {
                ballX = canvas.width / 2;
                ballY = canvas.height - 30;
                ballSpeedX = 4;
                ballSpeedY = -4;
                paddleX = (canvas.width - paddleWidth) / 2;
            }
        }
    }
}

function updatePaddlePosition() {
    if(rightPressed && paddleX < canvas.width - paddleWidth) {
        paddleX += 7;
    } else if(leftPressed && paddleX > 0) {
        paddleX -= 7;
    }
}

function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    drawBricks();
    drawBall();
    drawPaddle();
    drawScore();
    drawLives();
    collisionDetection();
    updateBallPosition();
    updatePaddlePosition();

    requestAnimationFrame(draw);
}

draw();

public void main() {
}
    </script>
</body>
</html>