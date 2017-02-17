package nars.testchamba.object;

import nars.testchamba.View;

/**
 * @author me
 */
public class Herb {

    public static class Catnip {

    }

    public static class Cannanip extends Geometric.Circle{

        public Cannanip(double radius) {
            super(radius);
            color(45, 255, 25 );

        }

        @Override
        public void update(View v, double dt) {
            super.update(v, dt);
            this.r = 50 + 50 * (float) Math.sin(dt);
        }
    }

    public static class Wetnip {

    }

    public static class Cidnip {

    }

}
