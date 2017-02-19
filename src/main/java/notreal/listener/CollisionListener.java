package notreal.listener;

import notreal.collision.CollisionInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 11.06.2015
 */
public interface CollisionListener extends Collides {

    /**
     * Physics engine iterates over all registered collision listeners in some order and invokes this method before
     * resolving collision. If any listener returns {@code false}, it cancels all remaining method calls and the
     * collision itself.
     *
     * @param collisionInfo collision information
     * @return {@code true} iff physics engine should continue to resolve collision
     */
    boolean beforeCollision(@NotNull CollisionInfo collisionInfo);

    /**
     * Physics engine iterates over all registered collision listeners in some order and invokes this method after
     * resolving collision.
     *
     * @param collisionInfo collision information
     */
    void afterCollision(@NotNull CollisionInfo collisionInfo);
}
