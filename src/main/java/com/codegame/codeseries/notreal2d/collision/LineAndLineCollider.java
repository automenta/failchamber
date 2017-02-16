package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.Defaults;
import com.codegame.codeseries.notreal2d.form.LinearGeom;
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
        return bodyA.geom().shape == Shape.LINE && bodyB.geom().shape == Shape.LINE;
    }

 

    public static Point2D lineIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (Math.abs(denom) < Defaults.EPSILON) // Lines are parallel.
            return null;

        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3))/denom;

        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3))/denom;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f)
            return new Point2D((x1 + ua*(x2 - x1)), (y1 + ua*(y2 - y1)));

        return null;
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyComplexMethod"})
    @Nullable
    public CollisionInfo collideOneWayOLD(@NotNull Body bodyA, @NotNull Body bodyB) {
//        if (2 + 2 == 2 * 2) {
//            throw new NotImplementedException("Soon, very soon. Maybe...");
//        }
        return null;

//        LinearGeom linearFormA = (LinearGeom) bodyA.form();
//        LinearGeom linearFormB = (LinearGeom) bodyB.form();
//
//        Point2D point1A = linearFormA.getPoint1(bodyA.pos(), bodyA.angle(), epsilon);
//        Point2D point2A = linearFormA.getPoint2(bodyA.pos(), bodyA.angle(), epsilon);
//
//        Point2D point1B = linearFormB.getPoint1(bodyB.pos(), bodyB.angle(), epsilon);
//        Point2D point2B = linearFormB.getPoint2(bodyB.pos(), bodyB.angle(), epsilon);
//
//        Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);
//        Line2D lineB = Line2D.getLineByTwoPoints(point1B, point2B);
//
//        Point2D intersectionPoint = lineA.getIntersectionPoint(lineB, epsilon);
//        if (intersectionPoint == null) {
//            return null;
//        }
//
//        double leftA = min(point1A.getX(), point2A.getX());
//        double topA = min(point1A.getY(), point2A.getY());
//        double rightA = max(point1A.getX(), point2A.getX());
//        double bottomA = max(point1A.getY(), point2A.getY());
//
//        if (intersectionPoint.getX() <= leftA - epsilon
//                || intersectionPoint.getX() >= rightA + epsilon
//                || intersectionPoint.getY() <= topA - epsilon
//                || intersectionPoint.getY() >= bottomA + epsilon) {
//            return null;
//        }
//
//        double leftB = min(point1B.getX(), point2B.getX());
//        double topB = min(point1B.getY(), point2B.getY());
//        double rightB = max(point1B.getX(), point2B.getX());
//        double bottomB = max(point1B.getY(), point2B.getY());
//
//        if (intersectionPoint.getX() <= leftB - epsilon
//                || intersectionPoint.getX() >= rightB + epsilon
//                || intersectionPoint.getY() <= topB - epsilon
//                || intersectionPoint.getY() >= bottomB + epsilon) {
//            return null;
//        }
//
//        Vector2D collisionNormalB = lineA.getUnitNormalFrom(bodyB.pos()).multiply(-1.0D); // TODO wrong?
//        double depth = min(lineA.getDistanceFrom(point1B), lineA.getDistanceFrom(point2B));
//
//        return new CollisionInfo(bodyA, bodyB, intersectionPoint, collisionNormalB, depth, epsilon); // TODO negate normal?
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyComplexMethod"})
    @Nullable
    public CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {


        LinearGeom linearFormA = (LinearGeom) bodyA.geom();
        LinearGeom linearFormB = (LinearGeom) bodyB.geom();

        Point2D pa = bodyA.pos();
        double aa = bodyA.angle();
        Point2D p0 = linearFormA.getPoint1(pa, aa, epsilon);
        Point2D p1 = linearFormA.getPoint2(pa, aa, epsilon);

        Point2D pb = bodyB.pos();
        double ab = bodyB.angle();
        Point2D p2 = linearFormB.getPoint1(pb, ab, epsilon);
        Point2D p3 = linearFormB.getPoint2(pb, ab, epsilon);

        Point2D intersectionPoint = lineIntersect(
            p0.getX(), p0.getY(),
            p1.getX(), p1.getY(),
            p2.getX(), p2.getY(),
            p3.getX(), p3.getY()
        );

        if (intersectionPoint==null)
            return null;

        Line2D lineA = Line2D.getLineByTwoPoints(p0, p1);
        //Line2D lineB = Line2D.getLineByTwoPoints(p2, p3);

//        if (Math.min(
//            point1A.getSquaredDistanceTo(intersectionPoint),
//            point2A.getSquaredDistanceTo(intersectionPoint)
//        ) > point1A.getSquaredDistanceTo(point2A))
//            return null;
//        if (Math.min(
//                point1B.getSquaredDistanceTo(intersectionPoint),
//                point2B.getSquaredDistanceTo(intersectionPoint)
//        ) > point1B.getSquaredDistanceTo(point2B))
//            return null;
//
////        double leftA = min(point1A.getX(), point2A.getX());
////        double topA = min(point1A.getY(), point2A.getY());
////        double rightA = max(point1A.getX(), point2A.getX());
////        double bottomA = max(point1A.getY(), point2A.getY());
//
////        if (intersectionPoint.getX() <= leftA - epsilon
////                || intersectionPoint.getX() >= rightA + epsilon
////                || intersectionPoint.getY() <= topA - epsilon
////                || intersectionPoint.getY() >= bottomA + epsilon) {
////            return null;
////        }
//
////        double leftB = min(point1B.getX(), point2B.getX());
////        double topB = min(point1B.getY(), point2B.getY());
////        double rightB = max(point1B.getX(), point2B.getX());
////        double bottomB = max(point1B.getY(), point2B.getY());
////
////        if (intersectionPoint.getX() <= leftB - epsilon
////                || intersectionPoint.getX() >= rightB + epsilon
////                || intersectionPoint.getY() <= topB - epsilon
////                || intersectionPoint.getY() >= bottomB + epsilon) {
////            return null;
////        }

        Vector2D collisionNormalB = lineA.getUnitNormalFrom(pb).multiply(-1.0D); // TODO wrong?
        double depth = min(lineA.getDistanceFrom(p2), lineA.getDistanceFrom(p3));

        return new CollisionInfo(bodyA, bodyB, intersectionPoint, collisionNormalB, depth, epsilon); // TODO negate normal?
    }
}
