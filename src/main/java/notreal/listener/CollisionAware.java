package notreal.listener;

import nars.testchamba.Space;
import notreal.Body;
import org.jetbrains.annotations.NotNull;


public interface CollisionAware {
    /**
     * Physics engine iterates over all registered collision listeners in some order and invokes this method before
     * gathering any collision information. Is is not guaranteed at this stage that bodies are really intersect. If any
     * listener returns {@code false}, it cancels all remaining method calls and the collision itself.
     *
     * @param them first body to collide, canonically the recipient of this invocation
     * @param me second body to collide, canonically the other involved in the collision
     *
     * @return {@code true} iff physics engine should continue to collide bodies
     */
    boolean collide(@NotNull Body them, Space where, @NotNull Body me);
}
