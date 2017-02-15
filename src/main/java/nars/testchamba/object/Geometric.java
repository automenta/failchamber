package nars.testchamba.object;

import com.codegame.codeseries.notreal2d.form.RectangularForm;
import nars.testchamba.View;
import nars.testchamba.state.Spatial;

/**
 * Created by me on 2/15/17.
 */
public enum Geometric { ;

    public static class Square extends Spatial {

        float r, g, b;

        public Square(double x, double y, double w, double h) {
            this(x, y, w, h, 1);
        }

        public Square(double x, double y, double w, double h, double density) {
            super(new RectangularForm(w, h), x, y);

            mass( w * h * density);

            r = g = b = 200f;
        }

        @Override
        public void draw(View view) {
            RectangularForm rr = (RectangularForm) form();
            float w = (float) rr.width;
            float h = (float) rr.height;

            //normalizeAngle();
            //view.rotate((float)angle());

            view.fill(r, g, b);
            view.rect(xF(), yF(), w, h);
        }

    }
}
