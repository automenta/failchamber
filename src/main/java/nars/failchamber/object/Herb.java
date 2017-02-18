package nars.failchamber.object;

import nars.failchamber.Space;
import nars.failchamber.View;
import notreal.Body;
import notreal.listener.CollisionAware;
import org.jetbrains.annotations.NotNull;

/**
 * @author me
 */
public class Herb {


    public static class Catnip {

    }

    public static class Cannanip extends Geometric.Circle implements CollisionAware {

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
        public boolean collide(@NotNull Body them, Space s, @NotNull Body me) {

            if (them instanceof Pacman) { //HACK
                if (nutrients > 0.1f) {
                    synchronized (this) {
                        //System.err.println(this + " EATEN by " + them);
                        if (((Pacman) them).canEat(this)) {
                            nutrients *= 0.5f;
                            s.remove(this);
                        }
                    }
                }
            }

            return true;
        }
    }

    public static class Wetnip {

    }

    public static class Cidnip {

    }

}
