package nars.failchamber.object;

import nars.failchamber.Space;
import notreal.Body;
import notreal.listener.Collides;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/17/17.
 */
public class Ammo {

    public static class Bullet extends Geometric.Rectangle implements Collides {

        float power;

        public Bullet(double x, double y, double w, double h) {
            super(x, y, w, h);

            power = 1f;

            r = 240f;
            g = 60f;
            b = 30f;

            setMovementFrictionFactor(0f);
            setMovementAirFrictionFactor(0f);
        }

        @Override
        public boolean collide(@NotNull Body them, Space s, @NotNull Body me) {
            if (them instanceof Explosion || them instanceof Bullet)
                return false;


            if (power > 0) {
                s.remove(this);
                power = 0;

                //create pressure wave
                double rad = geom().radius();
                Explosion e = new Explosion(mass(),1.5f, rad, rad * 20);
                e.pos(pos());
                s.add(e);
            }

            return true;
        }
    }

    public static class Missile {

    }


}

