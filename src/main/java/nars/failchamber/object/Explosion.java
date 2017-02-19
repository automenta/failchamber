package nars.failchamber.object;

import nars.failchamber.Space;
import nars.failchamber.View;
import notreal.Body;
import notreal.listener.CollisionAware;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/18/17.
 */
public class Explosion extends Geometric.Circle implements CollisionAware {

    float ttl;

    public Explosion(double radius) {
        super(radius);
        ttl = 1f;
        r = 255f;
        g = 127f;
        b = 10f;
        a = 127f;
    }


    @Override
    public void update(View v, double dt) {

        ttl -= dt;

        a *= 0.9f;

        if (ttl < 0) {
            v.space.remove(this);
        }
    }

    @Override
    public boolean collide(@NotNull Body them, Space where, @NotNull Body me) {
        if (them instanceof Explosion)
            return false; //explosions dont collide with each other
        return true;
    }
}
