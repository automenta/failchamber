package nars.failchamber.object;

import nars.failchamber.Space;
import nars.failchamber.View;

/**
 * Created by me on 2/15/17.
 */
public class Poison {

    /** permanently toxic (halflife in thousands of years at least) */
    public static class Plutonium extends Geometric.Circle implements Edible {

        public Plutonium(double v) {
            super(v);

            r = 255;
            g = 0;
            b = 0;
        }

        @Override
        public void update(View v, double dt) {
            super.update(v, dt);
            r = (float)(180f + Math.sin(v.time) * 30f);
        }

        @Override
        public Ingest eat(Space s) {
            return new Ingest(this, 0, 0.25f);
        }
    }
}
