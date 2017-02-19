package nars.failchamber.object;

import nars.failchamber.Space;
import nars.failchamber.View;

/**
 * @author me
 */
public class Herb {


    public static class Catnip {

    }

    public static class Cannanip extends Geometric.Circle implements Edible {

        float nutrients = 1;

        public Cannanip(double radius) {
            super(radius);
            color(45, Math.round(Math.random() * 70 + 125), 25 );
        }

        @Override
        public void update(View v, double dt) {
            super.update(v, dt);
            //this.r = 50 + 50 * (float) Math.sin(dt);
            this.g = 220 * nutrients;
        }

        @Override
        public Edible.Ingest eat(Space s) {
            if (nutrients > 0.1f) {
                nutrients -= 0.25f;
                return new Edible.Ingest(this, 0.5f, 0);
            } else {
                s.remove(this);
                return null;
            }
        }
    }

    public static class Wetnip {

    }

    public static class Cidnip {

    }

}
