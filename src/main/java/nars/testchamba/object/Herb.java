package nars.testchamba.object;

import nars.testchamba.Space;
import nars.testchamba.View;
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

        public Cannanip(double radius) {
            super(radius);
            color(45, Math.round(Math.random() * 70 + 125), 25 );


        }

        @Override
        public void update(View v, double dt) {
            super.update(v, dt);
            this.r = 50 + 50 * (float) Math.sin(dt);
        }


        @Override
        public boolean collide(@NotNull Body them, Space where, @NotNull Body me) {
            if (them instanceof Pacman) {
                System.err.println(this + " EATEN by " + them);
                where.remove(this);
                //return false;
            }
            return true;
        }
    }

    public static class Wetnip {

    }

    public static class Cidnip {

    }

}
