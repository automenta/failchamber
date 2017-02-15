package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.LinearForm;
import com.codegame.codeseries.notreal2d.form.Shape;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.codeforces.commons.math.Math.max;
import static com.codeforces.commons.math.Math.min;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public class LineAndLineCollider extends ColliderBase {
    public LineAndLineCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        return bodyA.form().shape == Shape.LINE && bodyB.form().shape == Shape.LINE;
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyComplexMethod"})
    @Nullable
    @Override
    public CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        if (2 + 2 == 2 * 2) {
            throw new NotImplementedException("Soon, very soon. Maybe...");
        }

        LinearForm linearFormA = (LinearForm) bodyA.form();
        LinearForm linearFormB = (LinearForm) bodyB.form();

        Point2D point1A = linearFormA.getPoint1(bodyA.pos(), bodyA.angle(), epsilon);
        Point2D point2A = linearFormA.getPoint2(bodyA.pos(), bodyA.angle(), epsilon);

        Point2D point1B = linearFormB.getPoint1(bodyB.pos(), bodyB.angle(), epsilon);
        Point2D point2B = linearFormB.getPoint2(bodyB.pos(), bodyB.angle(), epsilon);

        Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);
        Line2D lineB = Line2D.getLineByTwoPoints(point1B, point2B);

        Point2D intersectionPoint = lineA.getIntersectionPoint(lineB, epsilon);
        if (intersectionPoint == null) {
            return null;
        }

        double leftA = min(point1A.getX(), point2A.getX());
        double topA = min(point1A.getY(), point2A.getY());
        double rightA = max(point1A.getX(), point2A.getX());
        double bottomA = max(point1A.getY(), point2A.getY());

        if (intersectionPoint.getX() <= leftA - epsilon
                || intersectionPoint.getX() >= rightA + epsilon
                || intersectionPoint.getY() <= topA - epsilon
                || intersectionPoint.getY() >= bottomA + epsilon) {
            return null;
        }

        double leftB = min(point1B.getX(), point2B.getX());
        double topB = min(point1B.getY(), point2B.getY());
        double rightB = max(point1B.getX(), point2B.getX());
        double bottomB = max(point1B.getY(), point2B.getY());

        if (intersectionPoint.getX() <= leftB - epsilon
                || intersectionPoint.getX() >= rightB + epsilon
                || intersectionPoint.getY() <= topB - epsilon
                || intersectionPoint.getY() >= bottomB + epsilon) {
            return null;
        }

        Vector2D collisionNormalB = lineA.getUnitNormalFrom(bodyB.pos()).multiply(-1.0D); // TODO wrong?
        double depth = min(lineA.getDistanceFrom(point1B), lineA.getDistanceFrom(point2B));

        return new CollisionInfo(bodyA, bodyB, intersectionPoint, collisionNormalB, depth, epsilon); // TODO negate normal?
    }
}
