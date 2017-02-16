package com.codegame.codeseries.notreal2d.collision;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.Geom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.07.2015
 */
public abstract class ColliderBase implements Collider {
    @SuppressWarnings("ProtectedField")
    protected final double epsilon;

    protected ColliderBase(double epsilon) {
        this.epsilon = epsilon;
    }

    @Override
    public final boolean matches(@NotNull Body bodyA, @NotNull Body bodyB) {
        return matchesOneWay(bodyA, bodyB) || matchesOneWay(bodyB, bodyA);
    }

    @Nullable
    @Override
    public final CollisionInfo collide(@NotNull Body bodyA, @NotNull Body bodyB) {
        if (matchesOneWay(bodyA, bodyB)) {
            return collideOneWay(bodyA, bodyB);
        }

        if (matchesOneWay(bodyB, bodyA)) {
            CollisionInfo collisionInfo = collideOneWay(bodyB, bodyA);
            return collisionInfo == null ? null : new CollisionInfo(
                    bodyA, bodyB, collisionInfo.getPoint(), collisionInfo.getNormalB().negate(),
                    collisionInfo.getDepth(), epsilon
            );
        }

        throw new IllegalArgumentException(String.format(
                "Unsupported %s of %s or %s of %s.",
                Geom.toString(bodyA.geom()), bodyA, Geom.toString(bodyB.geom()), bodyB
        ));
    }

    protected abstract boolean matchesOneWay(@NotNull Body bodyA, @NotNull Body bodyB);

    @Nullable
    protected abstract CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB);
}
