package nars.testchamba;

import com.codegame.codeseries.notreal2d.Defaults;
import com.codegame.codeseries.notreal2d.World;
import com.codegame.codeseries.notreal2d.bodylist.SimpleBodyList;
import nars.testchamba.agent.GridAgent;
import nars.testchamba.state.*;

/**
 * Created by me on 2/15/17.
 */
public class Space extends World {

    public final Hauto cells;
    public final ParticleSystem particles;

    public Space(Hauto cells) {
        super(Defaults.ITERATION_COUNT_PER_STEP, 60, Defaults.EPSILON,
            //new CellSpaceBodyList(2, 4) //not working yet
            new SimpleBodyList()
        );

        this.cells = cells;
        this.particles = new ParticleSystem(cells);


//        Body body = new Body();
//        body.form(new CircularForm(1.0D));
//        body.mass(1.0D);
//        body.force(5, 5);
//        add(body);
//
//        body.state().registerPositionListener(new PositionListener() {
//
//            @Override
//            public boolean beforeChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
//                System.out.println(newPosition);
//                return true;
//            }
//
//            @Override
//            public void afterChangePosition(@NotNull Point2D oldPosition, @NotNull Point2D newPosition) {
//
//            }
//        }, "x");
    }


    public boolean add(Spatial g) {
        super.add(g);
        return true;
    }

    public boolean remove(Spatial g) {
        super.remove(g);
        return true;
    }

    public void update(View view, double dt) {

        //realtime = System.nanoTime() / 1.0e9;

        //if (time % automataPeriod == 0 || Chamba.executed) {
        cells.update();
        //}

        //if (time % agentPeriod == 0 || Chamba.executed) {
        //try {
        forEach(b -> {
            if (b instanceof Spatial) {
                Spatial g = (Spatial) b;
                g.update(view, dt);

                if (g instanceof GridAgent) {
                    GridAgent gg = (GridAgent) g;
                    if (!gg.actions.isEmpty()) {
                        nars.testchamba.state.Action a = gg.actions.pop();
                        //if (a != null) {
                        process(gg, a, view);
                        //}
                    }

                }
            }
        });

//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
        //}

        super.next();

    }


    static void process(GridAgent agent, nars.testchamba.state.Action action, View view) {
        Effect e = action.process(view, agent);
        if (e != null) {
            agent.perceive(e);
        }
    }

    //public void forEach(Consumer<Spatial> s)
    public void draw(View v) {
        forEach(o -> {
            if (o instanceof Spatial) {
                Spatial s = (Spatial) o;
                s.draw(v);
            }
        });
    }

    public String what(int x, int y) {
        //TODO convert space to grid coordinates correctly

//        for (Body o : getAll()) {
//            if (o instanceof Spatial) {
//                Spatial s = (Spatial) o;
//                if (s.x == x && s.y == y && s.doorname != null && !s.doorname.isEmpty())
//                    return s.doorname;
//            }
//        }
        return "";

    }
}
