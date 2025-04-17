// Enemy class
class Enemy {
    private String name;
    private int health;
    private Rectangle sprite;
    private double x, y;

    public Enemy(String name, int health, double x, double y) {
        this.name = name;
        this.health = health;
        this.x = x;
        this.y = y;
        this.sprite = new Rectangle(x, y, 30, 30, Color.RED); // Placeholder
    }

    public Rectangle getSprite() {
        return sprite;
    }

    public void animate(Pane root) {
        TranslateTransition move = new TranslateTransition(Duration.seconds(2), sprite);
        move.setByX(random.nextInt(100) - 50);
        move.setByY(random.nextInt(100) - 50);
        move.setCycleCount(Timeline.INDEFINITE);
        move.setAutoReverse(true);
        move.play();
    }

    public void attack(Player player) {
        player.setHealth(player.getHealth() - 10);
    }

    public boolean isNearPlayer(Player player) {
        return Math.abs(player.getX() - x) < 50 && Math.abs(player.getY() - y) < 50;
    }
}

// Update Room class
class Room {
    // ... existing fields ...
    private Enemy enemy;

    public void setEnemy(Enemy enemy) {
        this.enemy = enemy;
    }

    public Enemy getEnemy() {
        return enemy;
    }
}

// Update GameController.initializeRooms()
private void initializeRooms() {
    // ... existing rooms ...
    Room basement = rooms.get("Basement");
    basement.setEnemy(new Enemy("Shadow", 50, 500, 500));
}

// Update GameController.updateRoom()
private void updateRoom() {
    Room room = rooms.get(currentRoomName);
    root.getChildren().clear();
    root.getChildren().add(room.getBackground());
    for (Item item : room.getItems()) {
        root.getChildren().add(item.getSprite());
    }
    if (room.getEnemy() != null) {
        root.getChildren().add(room.getEnemy().getSprite());
        room.getEnemy().animate(root);
    }
    root.getChildren().addAll(statusText, player.getSprite());
    animateBackground(room.getBackground());
}

// Update GameController.startGameLoop()
public void startGameLoop() {
    Timeline gameLoop = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
        if (gameRunning) {
            if (random.nextInt(100) < 30) {
                eventHandler.triggerEvent(player, root, statusText);
            }
            Room room = rooms.get(currentRoomName);
            if (room.getEnemy() != null && room.getEnemy().isNearPlayer(player)) {
                room.getEnemy().attack(player);
                statusText.setText("The shadow attacks! Health: " + player.getHealth());
            }
        }
    }));
    gameLoop.setCycleCount(Timeline.INDEFINITE);
    gameLoop.play();
}