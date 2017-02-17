package notreal.listener;

import nars.testchamba.Space;
import notreal.Body;
import notreal.collision.CollisionInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 11.06.2015
 */
public class CollisionListenerAdapter implements CollisionListener {

    @Override
    public boolean beforeCollision(@NotNull CollisionInfo collisionInfo) {
        return true;
    }

    @Override
    public void afterCollision(@NotNull CollisionInfo collisionInfo) {

    }

    @Override
    public boolean collide(@NotNull Body them, Space where, @NotNull Body me) {
        return true;
    }
}
