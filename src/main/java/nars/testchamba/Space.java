package nars.testchamba;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.pair.DoublePair;
import com.codegame.codeseries.notreal2d.Defaults;
import com.codegame.codeseries.notreal2d.World;
import com.codegame.codeseries.notreal2d.bodylist.SimpleBodyList;
import nars.testchamba.agent.PacManAgent;
import nars.testchamba.state.Effect;
import nars.testchamba.state.Hauto;
import nars.testchamba.state.ParticleSystem;
import nars.testchamba.state.Spatial;
import nars.testchamba.util.Animation;

import java.util.Collection;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

                if (g instanceof PacManAgent) {
                    PacManAgent gg = (PacManAgent) g;
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

    public static float[] f(DoublePair pos) {
        return new float[] { (float) pos.getFirst(), (float) pos.getSecond()};
    }


    static void process(PacManAgent agent, nars.testchamba.state.Action action, View view) {
        Effect e = action.process(view, agent);
        if (e != null) {
            agent.perceive(e);
        }
    }

    //public void forEach(Consumer<Spatial> s)
    public void draw(View v) {
        long rt = System.nanoTime();


        forEach(o -> {
            if (o instanceof Spatial) {
                ((Spatial) o).draw(v, rt);
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

//    public void operate(String arg, String opnamer) {
//        opname = opnamer;
//        Hauto cells = this.space.cells;
//        goal = arg;
//        for (int i = 0; i < cells.w; i++) {
//            for (int j = 0; j < cells.h; j++) {
//                if (cells.read[i][j].name.equals(arg)) {
//                    if (opname.equals("go-to"))
//                        this.target.set(i, j);
//                }
//            }
//        }
//        //if("pick".equals(opname)) {
//        for (Body gridi : this.space.getAll()) {
//            if (gridi instanceof Spatial && gridi.getName().equals(goal)) { //Key && ((Key)gridi).doorname.equals(goal)) {
//                Spatial gridu = (Spatial) gridi;
//                if (opname.equals("go-to"))
//                    this.target.set((float)gridu.x(), (float)gridu.y());
//            }
//        }
//        //}
//    }
}
