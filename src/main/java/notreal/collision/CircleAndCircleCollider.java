package notreal.collision;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import notreal.Body;
import notreal.form.CircularGeom;
import notreal.form.Shape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public class CircleAndCircleCollider extends ColliderBase {
    public CircleAndCircleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        return bodyA.geom().shape == Shape.CIRCLE && bodyB.geom().shape == Shape.CIRCLE;
    }

    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        CircularGeom circularFormA = (CircularGeom) bodyA.geom();
        CircularGeom circularFormB = (CircularGeom) bodyB.geom();

        double radiusA = circularFormA.getRadius();
        double radiusB = circularFormB.getRadius();
        double distance = bodyA.pos().getDistanceTo(bodyB.pos());

        if (distance > radiusA + radiusB) {
            return null;
        }

        Vector2D collisionNormalB;
        Point2D collisionPoint;

        if (distance >= epsilon) {
            Vector2D vectorBA = new Vector2D(bodyB.pos(), bodyA.pos());
            collisionNormalB = vectorBA.copy().normalize();
            collisionPoint = bodyB.pos().copy().add(vectorBA.copy().multiply(radiusB / (radiusA + radiusB)));
        } else {
            Vector2D relativeVelocityB = bodyB.vel().copy().subtract(bodyA.vel());

            if (relativeVelocityB.getLength() >= epsilon) {
                collisionNormalB = relativeVelocityB.normalize();
            } else if (bodyB.vel().getLength() >= epsilon) {
                collisionNormalB = bodyB.vel().copy().normalize();
            } else {
                collisionNormalB = new Vector2D(1.0D, 0.0D);
            }

            collisionPoint = bodyB.pos().copy();
        }

        return new CollisionInfo(bodyA, bodyB, collisionPoint, collisionNormalB, radiusA + radiusB - distance, epsilon);
    }
}
