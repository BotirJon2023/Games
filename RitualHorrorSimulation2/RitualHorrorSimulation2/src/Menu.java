class GameMenu {
    private Pane menuPane;
    private Text startText;

    public GameMenu(Runnable startGame) {
        menuPane = new Pane();
        startText = new Text(300, 300, "Ritual Horror Simulation\nPress ENTER to Start");
        startText.setFont(Font.font("Verdana", 24));
        startText.setFill(Color.WHITE);
        menuPane.getChildren().add(startText);
        animateText();
        menuPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                startGame.run();
            }
        });
    }

    private void animateText() {
        FadeTransition fade = new FadeTransition(Duration.seconds(1), startText);
        fade.setFromValue(1.0);
        fade.setToValue(0.5);
        fade.setCycleCount(Timeline.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }

    public Pane getPane() {
        return menuPane;
    }
}