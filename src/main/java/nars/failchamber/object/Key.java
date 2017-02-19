package nars.failchamber.object;

import nars.failchamber.View;
import nars.failchamber.state.Spatial;

import java.awt.*;

/**
 * @author me
 */


public class Key extends Spatial {


    public Key(int x, int y, String name) {
        super(x, y);
        setName(name);
    }


    @Override
    public void draw(View space, long rt) {
        float scale = (float) Math.sin(space.getTime() / 7f) * 0.05f + 1.0f;
        double a = space.getTime() / 10;


        space.pushMatrix();
        space.rotate((float) a);
        space.scale(scale * 0.8f);

        space.fill(Color.GREEN.getRGB());
        space.rect(-0.4f, -0.15f / 2, 0.8f, 0.15f);
        space.rect(-0.5f, -0.2f, 0.3f, 0.4f);
        space.rect(0.3f, 0, 0.1f, 0.15f);
        space.rect(0.1f, 0, 0.1f, 0.15f);
        space.popMatrix();

        String doorname = getName();
        if (doorname != null && !doorname.isEmpty()) {
            space.textSize(0.2f);
            space.fill(255, 0, 0);
            space.pushMatrix();
            space.text(doorname, 0, 0);
            space.popMatrix();
        }


    }


}
