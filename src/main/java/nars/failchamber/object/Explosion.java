package nars.failchamber.object;

import jcog.Util;
import nars.failchamber.Space;
import nars.failchamber.View;
import notreal.Body;
import notreal.listener.Collides;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/18/17.
 */
public class Explosion extends Geometric.Circle implements Collides {


    private final double minRadius;
    private final double maxRadius;
    final float tLife;
    float tRemain;

    public Explosion(double mass, double duration, double minRadius, double maxRadius) {
        super(minRadius);

        mass(mass);

        tRemain = tLife = (float)duration;

        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        r = 255f;
        g = 127f;
        b = 10f;
        a = 127f;
    }


    @Override
    public void update(View v, double dt) {

        tRemain -= dt;

        if (tRemain <= 0) {
            v.space.remove(this);
        } else {
            float p = (1f - tRemain/tLife);
            double nextRad = Util.lerp(p, maxRadius, minRadius);
            geom().setRadius(nextRad);
            r = (float)Math.random() * 50 + 200;
            g = (float)Math.random() * 30 + 180;
            a = 127 * (1f-p);
        }
    }

    @Override
    public boolean collide(@NotNull Body them, Space where, @NotNull Body me) {
        if (them instanceof Explosion)
            return false; //explosions dont collide with each other
        if (them instanceof Burnable) {
            double damage = (tRemain / tLife);
            if (damage > 0.01f)
                ((Burnable)them).burn((float)(mass() * damage));
        }
        return true;
    }
}
