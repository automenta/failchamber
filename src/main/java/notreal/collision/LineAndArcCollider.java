package notreal.collision;

import notreal.Body;
import notreal.form.Shape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.06.2015
 */
public class LineAndArcCollider extends ColliderBase {
    public LineAndArcCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        return bodyA.geom().shape == Shape.LINE && bodyB.geom().shape == Shape.ARC;
    }

    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        return null; // TODO
    }
}
