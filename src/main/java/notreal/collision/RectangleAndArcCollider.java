package notreal.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codeforces.commons.holder.Mutable;
import com.codeforces.commons.holder.SimpleMutable;
import com.codeforces.commons.pair.Pair;
import notreal.Body;
import notreal.form.ArcGeom;
import notreal.form.Geom;
import notreal.form.RectangularGeom;
import notreal.form.Shape;
import notreal.util.GeometryUtil;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.codeforces.commons.math.Math.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.06.2015
 */
public class RectangleAndArcCollider extends ColliderBase {
    public RectangleAndArcCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        return bodyA.geom().shape == Shape.RECTANGLE && bodyB.geom().shape == Shape.ARC;
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod", "ConstantConditions"})
    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@NotNull Body bodyA, @NotNull Body bodyB) {
        RectangularGeom rectangularFormA = (RectangularGeom) bodyA.geom();
        ArcGeom arcFormB = (ArcGeom) bodyB.geom();

        double radiusA = rectangularFormA.radius();
        double radiusB = arcFormB.getRadius();
        double distance = bodyA.pos().getDistanceTo(bodyB.pos());

        if (distance > radiusA + radiusB) {
            return null;
        }

        if (distance < abs(radiusA - radiusB)) {
            return null;
        }

        Point2D[] pointsA = rectangularFormA.getPoints(bodyA.pos(), bodyA.angle(), epsilon);
        int pointACount = pointsA.length;

        double squaredRadiusB = radiusB * radiusB;

        double startAngleB = bodyB.angle() + arcFormB.getAngle();
        double finishAngleB = startAngleB + arcFormB.getSector();

        Point2D point1B = bodyB.pos().copy().add(new Vector2D(radiusB, 0.0D).setAngle(startAngleB));
        Point2D point2B = bodyB.pos().copy().add(new Vector2D(radiusB, 0.0D).setAngle(finishAngleB));

        List<IntersectionInfo> intersectionInfos = new ArrayList<>();

        for (int pointAIndex = 0; pointAIndex < pointACount; ++pointAIndex) {
            Point2D point1A = pointsA[pointAIndex];
            Point2D point2A = pointsA[pointAIndex == pointACount - 1 ? 0 : pointAIndex + 1];

            Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);

            if (lineA.getSignedDistanceFrom(bodyA.pos()) > -epsilon) {
                throw new IllegalStateException(String.format("%s of %s is too small, " +
                                "does not represent a convex polygon, or its points are going in wrong order.",
                        Geom.toString(bodyA.geom()), bodyA
                ));
            }

            double distanceFromB = lineA.getSignedDistanceFrom(bodyB.pos());

            if (distanceFromB > radiusB) {
                continue;
            }

            double leftA = min(point1A.getX(), point2A.getX());
            double topA = min(point1A.getY(), point2A.getY());
            double rightA = max(point1A.getX(), point2A.getX());
            double bottomA = max(point1A.getY(), point2A.getY());

            Point2D projectionOfB = lineA.getProjectionOf(bodyB.pos());

            double offset = sqrt(squaredRadiusB - distanceFromB * distanceFromB);
            Vector2D offsetVector = new Vector2D(point1A, point2A).setLength(offset);

            Point2D intersectionPoint1 = projectionOfB.copy().add(offsetVector);

            if (doesPointBelongToAAndB(
                    intersectionPoint1, leftA, topA, rightA, bottomA, bodyB, startAngleB, finishAngleB
            )) {
                addIntersectionInfo(intersectionPoint1, point1A, point2A, lineA, intersectionInfos);
            }

            Point2D intersectionPoint2 = projectionOfB.copy().add(offsetVector.copy().negate());

            if (doesPointBelongToAAndB(
                    intersectionPoint2, leftA, topA, rightA, bottomA, bodyB, startAngleB, finishAngleB
            )) {
                addIntersectionInfo(intersectionPoint2, point1A, point2A, lineA, intersectionInfos);
            }
        }

        int intersectionCount = intersectionInfos.size();

        if (intersectionCount == 0) {
            // TODO check arc inside rectangle
            return null;
        } else if (intersectionCount == 1 && arcFormB.isEndpointCollisionEnabled() && (
                !GeometryUtil.isPointOutsideConvexPolygon(point1B, pointsA, epsilon)
                        || !GeometryUtil.isPointOutsideConvexPolygon(point2B, pointsA, epsilon)
        )) {
            IntersectionInfo intersectionInfo = intersectionInfos.get(0);
            int intersectionLineCount = intersectionInfo.intersectionLines.size();

            if (intersectionLineCount == 1 || intersectionLineCount == 2) { // TODO separate 1 and 2 ??
                Line2D intersectionLine = intersectionInfo.intersectionLines.get(0);

                double distanceFromPoint1B = intersectionLine.getSignedDistanceFrom(point1B);
                double distanceFromPoint2B = intersectionLine.getSignedDistanceFrom(point2B);

                for (int pointAIndex = 0; pointAIndex < pointACount; ++pointAIndex) {
                    Point2D point1A = pointsA[pointAIndex];
                    Point2D point2A = pointsA[pointAIndex == pointACount - 1 ? 0 : pointAIndex + 1];

                    Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);

                    if (lineA.getSignedDistanceFrom(point1B) >= epsilon) {
                        return new CollisionInfo(
                                bodyA, bodyB, point2B, intersectionLine.getUnitNormal().negate(),
                                -distanceFromPoint2B, epsilon
                        );
                    }

                    if (lineA.getSignedDistanceFrom(point2B) >= epsilon) {
                        return new CollisionInfo(
                                bodyA, bodyB, point1B, intersectionLine.getUnitNormal().negate(),
                                -distanceFromPoint1B, epsilon
                        );
                    }
                }

                if (distanceFromPoint1B < distanceFromPoint2B) {
                    return new CollisionInfo(
                            bodyA, bodyB, point1B, intersectionLine.getUnitNormal().negate(),
                            -distanceFromPoint1B, epsilon
                    );
                } else {
                    return new CollisionInfo(
                            bodyA, bodyB, point2B, intersectionLine.getUnitNormal().negate(),
                            -distanceFromPoint2B, epsilon
                    );
                }
            } else {
                throw new IllegalStateException(String.format("%s of %s is too small, " +
                                "does not represent a convex polygon, or its points are going in wrong order.",
                        Geom.toString(bodyA.geom()), bodyA
                ));
            }
        } else {
            Vector2D vectorCB = new Vector2D(intersectionInfos.get(0).intersectionPoint, bodyB.pos());
            Vector2D vectorCA = new Vector2D(intersectionInfos.get(0).intersectionPoint, bodyA.pos());

            if (distance > radiusB - epsilon && vectorCB.dotProduct(vectorCA) < 0.0D) {
                Mutable<Point2D> nearestPoint = new SimpleMutable<>();
                MutableDouble distanceToNearestPoint = new MutableDouble();

                for (IntersectionInfo intersectionInfo : intersectionInfos) {
                    updateNearestPoint(bodyB, intersectionInfo.intersectionPoint, nearestPoint, distanceToNearestPoint);

                    for (Pair<Point2D, Point2D> pointAndPoint : intersectionInfo.intersectionLinePointPairs) {
                        updateNearestPoint(bodyB, pointAndPoint.getFirst(), nearestPoint, distanceToNearestPoint);
                        updateNearestPoint(bodyB, pointAndPoint.getSecond(), nearestPoint, distanceToNearestPoint);
                    }
                }

                return nearestPoint.get() == null ? null : new CollisionInfo(
                        bodyA, bodyB, nearestPoint.get(),
                        new Vector2D(bodyB.pos(), nearestPoint.get()).normalize(),
                        radiusB - distanceToNearestPoint.doubleValue(), epsilon
                );
            } else {
                Mutable<Point2D> farthestPoint = new SimpleMutable<>();
                MutableDouble distanceToFarthestPoint = new MutableDouble();

                for (IntersectionInfo intersectionInfo : intersectionInfos) {
                    updateFarthestPoint(
                            bodyB, intersectionInfo.intersectionPoint, farthestPoint, distanceToFarthestPoint,
                            startAngleB, finishAngleB
                    );

                    for (Pair<Point2D, Point2D> pointAndPoint : intersectionInfo.intersectionLinePointPairs) {
                        updateFarthestPoint(
                                bodyB, pointAndPoint.getFirst(), farthestPoint, distanceToFarthestPoint,
                                startAngleB, finishAngleB
                        );
                        updateFarthestPoint(
                                bodyB, pointAndPoint.getSecond(), farthestPoint, distanceToFarthestPoint,
                                startAngleB, finishAngleB
                        );
                    }
                }

                return farthestPoint.get() == null ? null : new CollisionInfo(
                        bodyA, bodyB, farthestPoint.get(),
                        new Vector2D(farthestPoint.get(), bodyB.pos()).normalize(),
                        distanceToFarthestPoint.doubleValue() - radiusB, epsilon
                );
            }
        }
    }

    private void updateNearestPoint(
            @NotNull Body body, @NotNull Point2D point, @NotNull Mutable<Point2D> nearestPoint,
            @NotNull MutableDouble distanceToNearestPoint) {
        double distanceToPoint = body.getDistanceTo(point);

        if (distanceToPoint >= epsilon
                && (nearestPoint.get() == null || distanceToPoint < distanceToNearestPoint.doubleValue())) {
            nearestPoint.set(point);
            distanceToNearestPoint.setValue(distanceToPoint);
        }
    }

    private static void updateFarthestPoint(
            @NotNull Body body, @NotNull Point2D point, @NotNull Mutable<Point2D> farthestPoint,
            @NotNull MutableDouble distanceToFarthestPoint, double startAngle, double finishAngle) {
        double distanceToPoint = body.getDistanceTo(point);

        if (GeometryUtil.isAngleBetween(new Vector2D(body.pos(), point).getAngle(), startAngle, finishAngle)
                && (farthestPoint.get() == null || distanceToPoint > distanceToFarthestPoint.doubleValue())) {
            farthestPoint.set(point);
            distanceToFarthestPoint.setValue(distanceToPoint);
        }
    }

    private boolean doesPointBelongToAAndB(
            @NotNull Point2D point, double leftA, double topA, double rightA, double bottomA,
            @NotNull Body bodyB, double startAngleB, double finishAngleB) {
        boolean belongsToA = (point.getX() > leftA - epsilon)
                && (point.getX() < rightA + epsilon)
                && (point.getY() > topA - epsilon)
                && (point.getY() < bottomA + epsilon);

        double pointAngleB = new Vector2D(bodyB.pos(), point).getAngle();
        if (pointAngleB < startAngleB) {
            pointAngleB += DOUBLE_PI;
        }

        boolean belongsToB = pointAngleB >= startAngleB && pointAngleB <= finishAngleB;

        return belongsToA && belongsToB;
    }

    private void addIntersectionInfo(
            Point2D point, Point2D point1A, Point2D point2A, Line2D lineA, List<IntersectionInfo> intersectionInfos) {
        boolean alreadyAdded = false;

        for (IntersectionInfo intersectionInfo : intersectionInfos) {
            if (intersectionInfo.intersectionPoint.nearlyEquals(point, epsilon)) {
                intersectionInfo.intersectionLines.add(lineA);
                intersectionInfo.intersectionLinePointPairs.add(new Pair<>(point1A, point2A));
                alreadyAdded = true;
                break;
            }
        }

        if (!alreadyAdded) {
            IntersectionInfo intersectionInfo = new IntersectionInfo(point);
            intersectionInfo.intersectionLines.add(lineA);
            intersectionInfo.intersectionLinePointPairs.add(new Pair<>(point1A, point2A));
            intersectionInfos.add(intersectionInfo);
        }
    }

    @SuppressWarnings("PublicField")
    private static final class IntersectionInfo {
        public final Point2D intersectionPoint;
        public final List<Line2D> intersectionLines = new ArrayList<>();
        public final List<Pair<Point2D, Point2D>> intersectionLinePointPairs = new ArrayList<>();

        private IntersectionInfo(Point2D intersectionPoint) {
            this.intersectionPoint = intersectionPoint;
        }
    }
}
