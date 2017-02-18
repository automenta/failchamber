package nars.failchamber.object;

import nars.failchamber.View;

import java.awt.*;


public class Pacman extends Geometric.Circle {

//    public final ArrayDeque<Action> actions = new ArrayDeque(); //pending
//    public final ArrayDeque<Effect> effects = new ArrayDeque(); //results
    //public final Set<Object> inventory = new HashSet();

    //final float animationLerpRate = 0.5f; //LERP interpolation rate


    public Pacman(double rad) {
        super(rad);
        color(250, 120, 0);
    }

    @Override
    public void update(View view, double dt) {

    }

    @Override
    protected void drawShape(View view) {
        super.drawShape(view);

        //eyes

        view.fill(Color.BLUE.getRGB(), 255);

        float R = (float) geom().radius();
        float r = R / 2f;
        view.ellipse(R * 0.5f, 0.5f * R, r, r);
        view.ellipse(R * 0.5f, -0.5f * R, r, r);
    }

    public boolean canEat(Herb.Cannanip cannanip) {
        return true;
    }

//    public void act(Action a) {
//        actions.add(a);
//    }
//
//    public void perceive(Effect e) {
//        effects.add(e);
//    }
//
//    public Effect perceiveNext() {
//        if (effects.isEmpty())
//            return null;
//
//        return effects.pop();
//    }


        //    @Override
//    public void draw(View space) {
//
////        cx = (cx * (1.0f - animationLerpRate)) + (x * animationLerpRate);
////        cy = (cy * (1.0f - animationLerpRate)) + (y * animationLerpRate);
////        cheading = (cheading * (1.0f - animationLerpRate / 2.0f)) + (heading * animationLerpRate / 2.0f);
//
//        float baseScale = (float) ((CircularForm)form()).radius();
//
//        float breathingScale = baseScale * (1f - (float) Math.sin(space.getTime() / 7f) * 0.05f);
//
////        space.pushMatrix();
////        space.translate(cx, cy);
//        //space.scale(breathingScale);
//
//        /*if(!(nar.memory.executive.next.isEmpty())) {
//            space.fill(Color.RED.getRGB(), 255);
//        } else */
//        {
//            space.fill(Color.ORANGE.getRGB(), 255);
//        }
//
//        space.ellipse(xF(), yF(), breathingScale*2, breathingScale*2);
//
//        //eyes
////        space.fill(Color.BLUE.getRGB(), 255);
////        //space.rotate((float) (Math.PI / 180f * cheading));
////        space.translate(-0.15f, 0.4f);
////        space.ellipse(0, 0, 0.2f, 0.2f);
////        space.translate(0.3f, 0.0f);
////        space.ellipse(0, 0, 0.2f, 0.2f);
//
////        space.popMatrix();
//    }


}

