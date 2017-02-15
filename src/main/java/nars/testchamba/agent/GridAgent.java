package nars.testchamba.agent;

import com.codegame.codeseries.notreal2d.form.CircularForm;
import nars.testchamba.View;
import nars.testchamba.state.Action;
import nars.testchamba.state.Effect;
import nars.testchamba.state.Spatial;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;


public class GridAgent extends Spatial {

    public final ArrayDeque<Action> actions = new ArrayDeque(); //pending
    public final ArrayDeque<Effect> effects = new ArrayDeque(); //results
    //public final Set<Object> inventory = new HashSet();

    //final float animationLerpRate = 0.5f; //LERP interpolation rate


    public GridAgent(double x, double y) {
        super(x, y);
    }

    public void act(Action a) {
        actions.add(a);
    }

    public void perceive(Effect e) {
        effects.add(e);
    }

    public Effect perceiveNext() {
        if (effects.isEmpty())
            return null;

        return effects.pop();
    }

    public void forward(int steps) {
        act(new Action.Forward(steps));
    }

    public void turn(int angle) {
        act(new Action.Turn(angle));
    }

    public void update(Effect nextEffect, View space) {

    }

    @Override
    public void update(View view, double dt) {
        Effect e = perceiveNext();
        update(e, view);
    }

    @Override
    public void draw(View space) {

//        cx = (cx * (1.0f - animationLerpRate)) + (x * animationLerpRate);
//        cy = (cy * (1.0f - animationLerpRate)) + (y * animationLerpRate);
//        cheading = (cheading * (1.0f - animationLerpRate / 2.0f)) + (heading * animationLerpRate / 2.0f);

        float baseScale = (float) ((CircularForm)form()).radius();

        float breathingScale = baseScale * (1f - (float) Math.sin(space.getTime() / 7f) * 0.05f);

//        space.pushMatrix();
//        space.translate(cx, cy);
        //space.scale(breathingScale);
        
        /*if(!(nar.memory.executive.next.isEmpty())) {
            space.fill(Color.RED.getRGB(), 255);
        } else */
        {
            space.fill(Color.ORANGE.getRGB(), 255);
        }

        space.ellipse(xF(), yF(), breathingScale*2, breathingScale*2);

        //eyes
//        space.fill(Color.BLUE.getRGB(), 255);
//        //space.rotate((float) (Math.PI / 180f * cheading));
//        space.translate(-0.15f, 0.4f);
//        space.ellipse(0, 0, 0.2f, 0.2f);
//        space.translate(0.3f, 0.0f);
//        space.ellipse(0, 0, 0.2f, 0.2f);

//        space.popMatrix();
    }


}

