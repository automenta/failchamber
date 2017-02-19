package nars.failchamber.object;

import nars.failchamber.Space;

/**
 * Created by me on 2/18/17.
 */
public interface Edible {

    class Ingest {

        public final Edible what;
        public final float nutrients;
        public final float toxins;

        public Ingest(Edible what, float nutrients, float toxins) {
            this.what = what;
            this.nutrients = nutrients;
            this.toxins = toxins;
        }
    }

    Ingest eat(Space s);

}
