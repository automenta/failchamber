package notreal.form;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.text.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public class CircularGeom extends Geom {

    private double radius;
    private double angularMassFactor;

    public CircularGeom(double radius) {
        super(Shape.CIRCLE);

        if (Double.isNaN(radius) || Double.isInfinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException(String.format(
                    "Argument 'radius' should be positive finite number but got %s.", radius
            ));
        }

        setRadius(radius);
    }

    public void setRadius(double radius) {
        this.radius = radius;
        this.angularMassFactor = radius * radius / 2.0D;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public double radius() {
        return radius;
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
        return StringUtil.toString(this, false, "radius");
    }
}
