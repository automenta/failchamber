package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.LinearGeom;
import com.codegame.codeseries.notreal2d.form.RectangularGeom;
import com.codegame.codeseries.notreal2d.form.Shape;
import com.codegame.codeseries.notreal2d.util.GeometryUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.codeforces.commons.math.Math.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 19.06.2015
 */
public class LineAndRectangleCollider extends ColliderBase {
    public LineAndRectangleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        return bodyA.geom().shape == Shape.LINE && bodyB.geom().shape == Shape.RECTANGLE;
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        LinearGeom linearFormA = (LinearGeom) bodyA.geom();
        RectangularGeom rectangularFormB = (RectangularGeom) bodyB.geom();

        Point2D point1A = linearFormA.getPoint1(bodyA.pos(), bodyA.angle(), epsilon);
        Point2D point2A = linearFormA.getPoint2(bodyA.pos(), bodyA.angle(), epsilon);

        Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);

        if (lineA.getDistanceFrom(bodyB.pos()) > rectangularFormB.radius()) {
            return null;
        }

        Point2D[] pointsB = rectangularFormB.getPoints(bodyB.pos(), bodyB.angle(), epsilon);
        int pointBCount = pointsB.length;

        Line2D intersectionLineB = null;
        List<Point2D> intersectionPoints = new ArrayList<>(pointBCount);
        int intersectionCount = 0;

        for (int pointBIndex = 0; pointBIndex < pointBCount; ++pointBIndex) {
            Point2D point1B = pointsB[pointBIndex];
            Point2D point2B = pointsB[pointBIndex == pointBCount - 1 ? 0 : pointBIndex + 1];

            Line2D lineB = Line2D.getLineByTwoPoints(point1B, point2B);

            Point2D potentialIntersectionPoint = lineA.getIntersectionPoint(lineB, epsilon);
            if (potentialIntersectionPoint == null) {
                continue;
            }

            double left = max(min(point1A.getX(), point2A.getX()), min(point1B.getX(), point2B.getX()));
            double top = max(min(point1A.getY(), point2A.getY()), min(point1B.getY(), point2B.getY()));
            double right = min(max(point1A.getX(), point2A.getX()), max(point1B.getX(), point2B.getX()));
            double bottom = min(max(point1A.getY(), point2A.getY()), max(point1B.getY(), point2B.getY()));

            if (potentialIntersectionPoint.getX() <= left - epsilon
                    || potentialIntersectionPoint.getX() >= right + epsilon
                    || potentialIntersectionPoint.getY() <= top - epsilon
                    || potentialIntersectionPoint.getY() >= bottom + epsilon) {
                continue;
            }

            intersectionLineB = lineB;

            boolean alreadyAdded = false;

            for (Point2D intersectionPoint : intersectionPoints) {
                if (intersectionPoint.nearlyEquals(potentialIntersectionPoint, epsilon)) {
                    alreadyAdded = true;
                    break;
                }
            }

            if (!alreadyAdded) {
                intersectionPoints.add(potentialIntersectionPoint);
            }

            ++intersectionCount;
        }

        if (intersectionCount == 1 && linearFormA.isEndpointCollisionEnabled() && (
                !GeometryUtil.isPointOutsideConvexPolygon(point1A, pointsB, epsilon)
                        || !GeometryUtil.isPointOutsideConvexPolygon(point2A, pointsB, epsilon)
        )) {
            Vector2D collisionNormalB = new Vector2D(
                    bodyB.pos(), intersectionLineB.getProjectionOf(bodyB.pos())
            ).normalize();

            Line2D parallelLine1A = intersectionLineB.getParallelLine(point1A);
            double distance1AFromB = parallelLine1A.getDistanceFrom(bodyB.pos());

            Line2D parallelLine2A = intersectionLineB.getParallelLine(point2A);
            double distance2AFromB = parallelLine2A.getDistanceFrom(bodyB.pos());

            double depth = (
                    distance1AFromB < distance2AFromB ? parallelLine1A : parallelLine2A
            ).getDistanceFrom(intersectionLineB, epsilon);

            return new CollisionInfo(bodyA, bodyB, intersectionPoints.get(0), collisionNormalB, depth, epsilon);
        } else {
            Point2D pointBWithMinDistanceFromA = pointsB[0];
            double minDistanceBFromA = lineA.getSignedDistanceFrom(pointBWithMinDistanceFromA);

            Point2D pointBWithMaxDistanceFromA = pointBWithMinDistanceFromA;
            double maxDistanceBFromA = minDistanceBFromA;

            for (int pointBIndex = 1; pointBIndex < pointBCount; ++pointBIndex) {
                Point2D pointB = pointsB[pointBIndex];
                double distanceBFromA = lineA.getSignedDistanceFrom(pointB);

                if (distanceBFromA < minDistanceBFromA) {
                    minDistanceBFromA = distanceBFromA;
                    pointBWithMinDistanceFromA = pointB;
                }

                if (distanceBFromA > maxDistanceBFromA) {
                    maxDistanceBFromA = distanceBFromA;
                    pointBWithMaxDistanceFromA = pointB;
                }
            }

            if (minDistanceBFromA < 0.0D && maxDistanceBFromA < 0.0D
                    || minDistanceBFromA > 0.0D && maxDistanceBFromA > 0.0D) {
                return null;
            }

            if (intersectionPoints.isEmpty()) {
                return null; // TODO check line inside rectangle
            }

            Vector2D collisionNormalB;
            double depth;

            if (lineA.getSignedDistanceFrom(bodyB.pos()) > 0.0D) {
                collisionNormalB = lineA.getParallelLine(pointBWithMinDistanceFromA)
                        .getUnitNormalFrom(pointBWithMaxDistanceFromA);
                depth = abs(minDistanceBFromA);
            } else {
                collisionNormalB = lineA.getParallelLine(pointBWithMaxDistanceFromA)
                        .getUnitNormalFrom(pointBWithMinDistanceFromA);
                depth = maxDistanceBFromA;
            }

            double averageIntersectionX = 0.0D;
            double averageIntersectionY = 0.0D;

            for (int i = 0, intersectionPointsSize = intersectionPoints.size(); i < intersectionPointsSize; i++) {
                Point2D intersectionPoint = intersectionPoints.get(i);
                averageIntersectionX += intersectionPoint.getX() / intersectionPointsSize;
                averageIntersectionY += intersectionPoint.getY() / intersectionPointsSize;
            }

            return new CollisionInfo(
                    bodyA, bodyB, new Point2D(averageIntersectionX, averageIntersectionY), collisionNormalB,
                    depth, epsilon
            );
        }
    }
}
