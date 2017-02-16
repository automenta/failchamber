package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.CircularGeom;
import com.codegame.codeseries.notreal2d.form.LinearGeom;
import com.codegame.codeseries.notreal2d.form.Shape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.codeforces.commons.math.Math.max;
import static com.codeforces.commons.math.Math.min;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public class LineAndCircleCollider extends ColliderBase {
    public LineAndCircleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        return bodyA.geom().shape == Shape.LINE && bodyB.geom().shape == Shape.CIRCLE;
    }

    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        LinearGeom linearFormA = (LinearGeom) bodyA.geom();
        CircularGeom circularFormB = (CircularGeom) bodyB.geom();

        Point2D baPos = bodyA.pos();
        double baAngle = bodyA.angle();
        Point2D point1A = linearFormA.getPoint1(baPos, baAngle, epsilon);
        Point2D point2A = linearFormA.getPoint2(baPos, baAngle, epsilon);

        return collideOneWay(bodyA, bodyB, point1A, point2A, circularFormB, epsilon);
    }

    @SuppressWarnings("OverlyLongMethod")
    @Nullable
    static CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB,
                                       @NotNull Point2D point1A, @NotNull Point2D point2A,
                                       @NotNull CircularGeom circularFormB, double epsilon) {
        Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);

        double distanceFromB = lineA.getDistanceFrom(bodyB.pos());
        double radiusB = circularFormB.getRadius();

        if (distanceFromB > radiusB) {
            return null;
        }

        double leftA = min(point1A.getX(), point2A.getX());
        double topA = min(point1A.getY(), point2A.getY());
        double rightA = max(point1A.getX(), point2A.getX());
        double bottomA = max(point1A.getY(), point2A.getY());

        Point2D projectionOfB = lineA.getProjectionOf(bodyB.pos());

        boolean projectionOfBBelongsToA = (projectionOfB.getX() > leftA - epsilon)
                && (projectionOfB.getX() < rightA + epsilon)
                && (projectionOfB.getY() > topA - epsilon)
                && (projectionOfB.getY() < bottomA + epsilon);

        if (projectionOfBBelongsToA) {
            Vector2D collisionNormalB;

            if (distanceFromB >= epsilon) {
                collisionNormalB = new Vector2D(bodyB.pos(), projectionOfB).normalize();
            } else {
                Vector2D unitNormalA = lineA.getUnitNormal();
                Vector2D relativeVelocityB = bodyB.vel().copy().subtract(bodyA.vel());

                if (relativeVelocityB.getLength() >= epsilon) {
                    collisionNormalB = relativeVelocityB.dotProduct(unitNormalA) >= epsilon
                            ? unitNormalA : unitNormalA.negate();
                } else if (bodyB.vel().getLength() >= epsilon) {
                    collisionNormalB = bodyB.vel().dotProduct(unitNormalA) >= epsilon
                            ? unitNormalA : unitNormalA.negate();
                } else {
                    collisionNormalB = unitNormalA;
                }
            }

            return new CollisionInfo(bodyA, bodyB, projectionOfB, collisionNormalB, radiusB - distanceFromB, epsilon);
        }

        double distanceToPoint1A = bodyB.getDistanceTo(point1A);
        double distanceToPoint2A = bodyB.getDistanceTo(point2A);

        Point2D nearestPointA;
        double distanceToNearestPointA;

        if (distanceToPoint1A < distanceToPoint2A) {
            nearestPointA = point1A;
            distanceToNearestPointA = distanceToPoint1A;
        } else {
            nearestPointA = point2A;
            distanceToNearestPointA = distanceToPoint2A;
        }

        if (distanceToNearestPointA > radiusB) {
            return null;
        }

        return new CollisionInfo(
                bodyA, bodyB, nearestPointA, new Vector2D(bodyB.pos(), nearestPointA).normalize(),
                radiusB - distanceToNearestPointA, epsilon
        );
    }
}
