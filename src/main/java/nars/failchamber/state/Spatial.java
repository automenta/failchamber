package nars.failchamber.state;

import nars.failchamber.Space;
import notreal.Body;
import notreal.form.CircularGeom;
import notreal.form.Geom;
import nars.failchamber.View;
import nars.failchamber.util.Animation;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * GridObject with a specific position
 */
public abstract class Spatial extends Body {

    final static int MAX_ANIMATIONS = 8;

    public final Deque<Animation> animations = new ConcurrentLinkedDeque<>();

    @Deprecated public Spatial(double x, double y) {
        this(new CircularGeom(0.5f), x, y);
    }

    public Spatial(Geom geom) {
        super();

        geom(geom);
        mass(1);

        setMovementFrictionFactor(0.75f);
        setMovementAirFrictionFactor(0.75f);
        setRotationFrictionFactor(0.75f);
        setRotationAirFrictionFactor(0.75f);

        //setMomentumTransferFactor(0.5D);
    }

    public Spatial(Geom geom, double x, double y) {
        this(geom);
        pos(x, y);
    }

    public void start(Space space) {

    }

    public void stop(Space space) {
        
    }

    abstract public void draw(View v, long rt);

    public void update(View v, double dt) {

    }

    public void animate(Animation a) {
        while (animations.size() >= MAX_ANIMATIONS) //FIFO
            animations.removeFirst();

        animations.add(a);
    }


//    public Cell cellAbsolute(int targetAngle) {
//        int tx = x;
//        int ty = y;
//        switch (angle(targetAngle)) {
//            case Hauto.UP:
//                ty++;
//                break;
//            case Hauto.DOWN:
//                ty--;
//                break;
//            case Hauto.LEFT:
//                tx--;
//                break;
//            case Hauto.RIGHT:
//                tx++;
//                break;
//            default:
//                System.err.println("cellAbsolute(" + targetAngle + " from " + heading + ") = Invalid angle: " + targetAngle);
//                return null;
//        }
//        return space.cells.at(tx, ty);
//    }

//    /**
//     * @return
//     */
//    public Cell cellRelative(int dAngle) {
//        int targetAngle = angle(heading + dAngle);
//        return cellAbsolute(targetAngle);
//    }
}
