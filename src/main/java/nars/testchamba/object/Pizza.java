package nars.testchamba.object;

import nars.testchamba.View;
import nars.testchamba.state.Effect;
import nars.testchamba.state.Spatial;

import java.awt.*;

/**
 * @author me
 */
public class Pizza extends Spatial {

    final float animationLerpRate = 0.5f; //LERP interpolation rate

    public Pizza(int x, int y, String name) {
        super(x, y);
        setName(name);
    }

    @Override
    public void draw(View v) {
        String n = getName();

        //cx = (cx * (1.0f - animationLerpRate)) + (x * animationLerpRate);
        //cy = (cy * (1.0f - animationLerpRate)) + (y * animationLerpRate);
        //angle(angle() + (cheading * (1.0f - animationLerpRate / 2.0f)) );

        float scale = (float) Math.sin(Math.PI / 7f) * 0.05f + 1.0f;
        v.pushMatrix();
        v.scale(scale * 0.8f);
        v.fill(Color.ORANGE.getRGB(), 255);
        v.ellipse(0, 0, 1.0f, 1.0f);
        v.fill(Color.YELLOW.getRGB(), 255);
        v.ellipse(0, 0, 0.8f, 0.8f);

        v.popMatrix();
        if (n != null && !n.isEmpty()) {
            v.textSize(0.2f);
            v.fill(255, 0, 0);
            v.pushMatrix();
            v.text(n, 0, 0);
            v.popMatrix();
        }

        //eyes
        v.fill(Color.RED.getRGB(), 255);
        //v.rotate((float) (Math.PI / 180f * cheading));
        v.ellipse(-0.15f, 0.2f, 0.1f, 0.1f);
        v.ellipse(0.15f, 0.2f, 0.1f, 0.1f);
        v.ellipse(-0.2f, -0.2f, 0.1f, 0.1f);
        v.ellipse(0.2f, -0.2f, 0.1f, 0.1f);
        v.ellipse(0.0f, -0.0f, 0.1f, 0.1f);

    }
}
