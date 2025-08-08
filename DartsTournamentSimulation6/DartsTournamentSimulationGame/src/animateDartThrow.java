import javafx.Circle;
import javafx.TranslateTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public void animateDartThrow(Circle dart) {
    Object Duration = null;
    TranslateTransition transition = new TranslateTransition(Duration.seconds(1), dart);
    transition.setToX(300);
    transition.setToY(200);
    transition.play();
}

void main() {
}
