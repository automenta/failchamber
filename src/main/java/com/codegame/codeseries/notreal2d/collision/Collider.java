package com.codegame.codeseries.notreal2d.collision;

import com.codegame.codeseries.notreal2d.Body;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public interface Collider {
    boolean matches(@NotNull Body bodyA, @NotNull Body bodyB);

    @Nullable
    CollisionInfo collide(@NotNull Body bodyA, @NotNull Body bodyB);
}
