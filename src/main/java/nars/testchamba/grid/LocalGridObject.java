package nars.testchamba.grid;

import nars.testchamba.View;

/**
 * GridObject with a specific position
 */
public abstract class LocalGridObject implements GridObject {

    public float cx, cy, cheading; //current drawn location, for animation

    public String doorname = "";
    public int x;
    public int y;
    public int heading; //in degrees
    public View space;

    public LocalGridObject(int x, int y) {
        setPosition(x, y);
    }

    /**
     * set by space when added
     */
    @Override
    public void init(View space) {
        this.space = space;
    }

    public void setPosition(int x, int y) {
        this.cx = this.x = x;
        this.cy = this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }


    public static int angle(int targetAngle) {
        while (targetAngle > 180) targetAngle -= 360;
        while (targetAngle <= -180) targetAngle += 360;
        return targetAngle;
    }

    /**
     * cell currently standing on
     */
    public Cell cellOn() {
        return space.cells.at(x, y);
    }

    public Cell cellAbsolute(int targetAngle) {
        int tx = x;
        int ty = y;
        switch (angle(targetAngle)) {
            case Hauto.UP:
                ty++;
                break;
            case Hauto.DOWN:
                ty--;
                break;
            case Hauto.LEFT:
                tx--;
                break;
            case Hauto.RIGHT:
                tx++;
                break;
            default:
                System.err.println("cellAbsolute(" + targetAngle + " from " + heading + ") = Invalid angle: " + targetAngle);
                return null;
        }
        return space.cells.at(tx, ty);
    }

    /**
     * @return
     */
    public Cell cellRelative(int dAngle) {
        int targetAngle = angle(heading + dAngle);
        return cellAbsolute(targetAngle);
    }
}
