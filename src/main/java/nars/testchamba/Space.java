package nars.testchamba;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.pair.DoublePair;
import notreal.World;
import notreal.bodylist.CellSpaceBodyList;
import notreal.bodylist.SimpleBodyList;
import notreal.form.LinearGeom;
import nars.testchamba.agent.PacManAgent;
import nars.testchamba.state.Effect;
import nars.testchamba.state.Hauto;
import nars.testchamba.state.ParticleSystem;
import nars.testchamba.state.Spatial;

/**
 * Created by me on 2/15/17.
 */
public class Space extends World {

    public final Hauto cells;
    public final ParticleSystem particles;
    double boundsX, boundsY;

    public Space(Hauto cells) {
        super(5, 60, 1E-4,
            new CellSpaceBodyList(8, 8)
            //new SimpleBodyList()
        );

        this.cells = cells;
        this.particles = new ParticleSystem(cells);

        boundsX = cells.w;
        boundsY = cells.h;

        add(LinearGeom.line(0, 0, boundsX, 0).statik());
        add(LinearGeom.line(0, boundsY, boundsX, boundsY).statik());
        add(LinearGeom.line(0, 0, 0, boundsY).statik());
        add(LinearGeom.line(boundsX, 0, boundsX, boundsY).statik());




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

    public static float[] ff(DoublePair pos) {
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

    public Point2D whereSpawns() {
        return new Point2D(Math.random()*boundsX, Math.random()*boundsY);
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
