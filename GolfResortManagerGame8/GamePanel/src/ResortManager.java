import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ResortManager {
    private ArrayList<GolfCourse> courses;
    private ArrayList<Guest> guests;
    private UIManager uiManager;

    public ResortManager() {
        courses = new ArrayList<>();
        guests = new ArrayList<>();
        uiManager = new UIManager();

        for (int i = 0; i < 3; i++) {
            courses.add(new GolfCourse(100 + i * 200, 300));
        }

        for (int i = 0; i < 5; i++) {
            guests.add(new Guest(50, 100 + i * 60));
        }
    }

    public void update() {
        for (Guest guest : guests) {
            guest.update();
        }
    }

    public void draw(Graphics g) {
        for (GolfCourse course : courses) {
            course.draw(g);
        }
        for (Guest guest : guests) {
            guest.draw(g);
        }
        uiManager.draw(g);
    }
}
