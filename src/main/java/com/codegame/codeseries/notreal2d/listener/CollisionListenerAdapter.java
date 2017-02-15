package com.codegame.codeseries.notreal2d.listener;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.collision.CollisionInfo;
import org.jetbrains.annotations.NotNull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 11.06.2015
 */
public class CollisionListenerAdapter implements CollisionListener {
    @Override
    public boolean beforeStartingCollision(@NotNull Body bodyA, @NotNull Body bodyB) {
        return true;
    }

    @Override
    public boolean beforeResolvingCollision(@NotNull CollisionInfo collisionInfo) {
        return true;
    }

    @Override
    public void afterResolvingCollision(@NotNull CollisionInfo collisionInfo) {
        // No operations.
    }
}
