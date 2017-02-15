package nars.testchamba.object;

import com.codegame.codeseries.notreal2d.form.CircularGeom;
import com.codegame.codeseries.notreal2d.form.Geom;
import com.codegame.codeseries.notreal2d.form.RectangularGeom;
import nars.testchamba.View;
import nars.testchamba.state.Spatial;

/**
 * Created by me on 2/15/17.
 */
public abstract class Geometric<F extends Geom> extends Spatial {

    protected float r, g, b;

    public Geometric(F f, double x, double y) {
        super(f, x, y);
    }

    public void color(float R, float G, float B) {
        r = R; g = G; b = B;
    }

    @Override
    public void draw(View view) {



        //normalizeAngle();
        view.pushMatrix();
        view.translate(xF(), yF());
        view.rotate((float)angle());

        view.fill(r, g, b);

        drawShape(view);

        view.popMatrix();
    }

    @Override
    public F form() {
        return (F) super.form();
    }

    protected abstract void drawShape(View view);

    public static class Rectangle extends Geometric<RectangularGeom> {


        public Rectangle(double x, double y, double w, double h) {
            this(x, y, w, h, 1);
        }

        public Rectangle(double x, double y, double w, double h, double density) {
            super(new RectangularGeom(w, h), x, y);

            mass( w * h * density);

            r = g = b = 200f;
        }

        @Override
        protected void drawShape(View view) {
            RectangularGeom rr = form();
            float w = (float) rr.width;
            float h = (float) rr.height;
            view.rect(-w/2f, -h/2f, w, h);
        }
    }

    public static class Circle extends Geometric<CircularGeom> {

        public Circle(double radius, double x, double y) {
            super(new CircularGeom(radius), x, y);
        }

        @Override
        protected void drawShape(View view) {
            CircularGeom rr = form();
            float d = (float) rr.radius()*2f;
            view.ellipse(0, 0, d, d);
        }
    }
}