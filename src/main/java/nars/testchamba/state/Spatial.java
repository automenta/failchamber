package nars.testchamba.state;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.CircularForm;
import com.codegame.codeseries.notreal2d.form.Form;
import nars.testchamba.View;
import nars.testchamba.agent.GridAgent;

/**
 * GridObject with a specific position
 */
public abstract class Spatial extends Body {


    @Deprecated public Spatial(double x, double y) {
        this(new CircularForm(0.5f), x, y);
    }

    public Spatial(Form form, double x, double y) {
        super();

        form(form);
        mass(1);
        pos(x, y);

        setMovementFrictionFactor(0.9f);
        //setMomentumTransferFactor(0.5D);

    }


    abstract public void draw(View v);

    public void update(View v, double dt) {

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
