package nars.failchamber.object;

import nars.failchamber.Space;
import notreal.Body;
import notreal.listener.CollisionAware;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/17/17.
 */
public class Ammo {

    public static class Bullet extends Geometric.Rectangle implements CollisionAware {

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
                Explosion e = new Explosion(geom().radius() * 4);
                e.pos(pos());
                s.add(e);
            }

            return true;
        }
    }

    public static class Missile {

    }


}

