package com.codegame.codeseries.notreal2d.form;

import com.codeforces.commons.geometry.Point2D;
import com.codegame.codeseries.notreal2d.Body;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.codeforces.commons.math.Math.abs;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public abstract class Geom {

    @NotNull
    public final Shape shape;

    protected Geom(@NotNull Shape shape) {
        this.shape = shape;
    }

    public abstract double radius();

    @NotNull
    public abstract Point2D centerOfMass(@NotNull Point2D position, double angle);

    @NotNull
    public final Point2D centerOfMass(@NotNull Body body) {
        return centerOfMass(body.pos(), body.angle());
    }

    public abstract double getAngularMass(double mass);

    @Override
    public abstract String toString();

    @NotNull
    public static String toString(@Nullable Geom geom) {
        return geom == null ? "Form {null}" : geom.toString();
    }

    //@Contract(pure = true)
    protected static double normalizeSinCos(double value, double epsilon) {
        return abs(value) < epsilon ? 0.0D
                : abs(1.0D - value) < epsilon ? 1.0D
                : abs(-1.0D - value) < epsilon ? -1.0D
                : value;
    }
}
