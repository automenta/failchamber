package com.codegame.codeseries.notreal2d.form;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.text.StringUtil;
import com.codegame.codeseries.notreal2d.Body;
import org.jetbrains.annotations.NotNull;

import static com.codeforces.commons.math.Math.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public class LinearGeom extends ThinGeom {
    private final double length;
    private final double halfLength;
    private final double angularMassFactor;

    private double lastAngle;
    private double lastEpsilon;
    private Double lastXOffset;
    private Double lastYOffset;

    public LinearGeom(double length, boolean endpointCollisionEnabled) {
        super(Shape.LINE, endpointCollisionEnabled);

        if (Double.isNaN(length) || Double.isInfinite(length) || length <= 0.0D) {
            throw new IllegalArgumentException(String.format(
                    "Argument 'length' should be positive finite number but got %s.", length
            ));
        }

        this.length = length;
        this.halfLength = length / 2.0D;
        this.angularMassFactor = length * length / 12.0D;
    }

    public static Body line(double x1, double y1, double x2, double y2) {
        Point2D a = new Point2D(x1, y1);
        Point2D b = new Point2D(x2, y2);
        Point2D ab = new Point2D(0.5 * (x1 + x2), 0.5 * (y1 + y2));
        double ang = Math.atan2( (y2 - y1) , (x2 - x1) );
        return new Body(new LinearGeom(a.getDistanceTo(b), true), new Point2D(ab), ang);
    }

    public LinearGeom(double length) {
        this(length, true);
    }

    public double getLength() {
        return length;
    }

    @NotNull
    public Point2D getPoint1(@NotNull Point2D position, double angle, double epsilon) {
        updateLastOffsets(angle, epsilon);
        return new Point2D(position.getX() - lastXOffset, position.getY() - lastYOffset);
    }

    @NotNull
    public Point2D getPoint2(@NotNull Point2D position, double angle, double epsilon) {
        updateLastOffsets(angle, epsilon);
        return new Point2D(position.getX() + lastXOffset, position.getY() + lastYOffset);
    }

    @Override
    public double radius() {
        return halfLength;
    }

    @NotNull
    @Override
    public Point2D centerOfMass(@NotNull Point2D position, double angle) {
        return position;
    }

    @Override
    public double getAngularMass(double mass) {
        return mass * angularMassFactor;
    }

    @Override
    public String toString() {
        return StringUtil.toString(this, false, "length");
    }

    @SuppressWarnings("FloatingPointEquality")
    private void updateLastOffsets(double angle, double epsilon) {
        if (lastXOffset == null || lastYOffset == null || angle != lastAngle || epsilon != lastEpsilon) {
            if (Double.isNaN(angle) || Double.isInfinite(angle)) {
                throw new IllegalArgumentException("Argument 'angle' is not a finite number.");
            }

            if (Double.isNaN(epsilon) || Double.isInfinite(epsilon) || epsilon < 1.0E-100D || epsilon > 1.0D) {
                throw new IllegalArgumentException("Argument 'epsilon' should be between 1.0E-100 and 1.0.");
            }

            lastAngle = angle;
            lastEpsilon = epsilon;

            if (abs(length) < epsilon) {
                lastXOffset = 0.0D;
                lastYOffset = 0.0D;
            } else {
                if (abs(HALF_PI - abs(angle)) < epsilon) {
                    lastXOffset = 0.0D;
                } else {
                    lastXOffset = normalizeSinCos(cos(angle), epsilon) * halfLength;
                }

                if (abs(PI - abs(angle)) < epsilon || abs(angle) < epsilon) {
                    lastYOffset = 0.0D;
                } else {
                    lastYOffset = normalizeSinCos(sin(angle), epsilon) * halfLength;
                }
            }
        }
    }
}
