public class RailBuilder {
    private double x1;
    private double y;
    private double x2;

    public RailBuilder setX1(double x1) {
        this.x1 = x1;
        return this;
    }

    public RailBuilder setY(double y) {
        this.y = y;
        return this;
    }

    public RailBuilder setX2(double x2) {
        this.x2 = x2;
        return this;
    }

    public SkateboardingSimulation.Rail createRail() {
        return new SkateboardingSimulation.Rail(x1, y, x2, y);
    }
}