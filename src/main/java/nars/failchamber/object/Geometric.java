package nars.failchamber.object;

import notreal.form.CircularGeom;
import notreal.form.Geom;
import notreal.form.RectangularGeom;
import nars.failchamber.View;
import nars.failchamber.state.Spatial;
import nars.failchamber.util.Animation;

import java.util.Iterator;

/**
 * Created by me on 2/15/17.
 */
public abstract class Geometric<F extends Geom> extends Spatial {

    protected float r, g, b;

    public Geometric(F f) {
        super(f);
    }
    public Geometric(F f, double x, double y) {
        super(f, x, y);
    }

    public void color(float R, float G, float B) {
        r = R; g = G; b = B;
    }

    @Override
    public void draw(View v, long rt) {



        //normalizeAngle();
        v.pushMatrix();
        v.translate(xF(), yF());
        v.rotate((float)angle());

        v.fill(r, g, b);

        if (!animations.isEmpty()) {
            Iterator<Animation> ii = animations.iterator();
            while (ii.hasNext()) {
                Animation x = ii.next();
                if (!x.draw(v, rt))
                    ii.remove();
            }
        }

        drawShape(v);

        v.popMatrix();
    }

    @Override
    public F geom() {
        return (F) super.geom();
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
            RectangularGeom rr = geom();
            float w = (float) rr.width;
            float h = (float) rr.height;
            view.rect(-w/2f, -h/2f, w, h);
        }
    }

    public static class Circle extends Geometric<CircularGeom> {

        public Circle(double radius) {
            super(new CircularGeom(radius));
        }

        @Override
        protected void drawShape(View view) {
            CircularGeom rr = geom();
            float d = (float) rr.radius()*2f;
            view.ellipse(0, 0, d, d);
        }
    }
}
