package nars.testchamba;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.CircularForm;
import nars.testchamba.agent.GridAgent;
import nars.testchamba.map.Maze;
import nars.testchamba.state.Cell.Material;
import nars.testchamba.state.*;
import nars.testchamba.util.Video;
import processing.core.PVector;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Chamba {

    static {
        Video.init();
    }

    public static boolean staticInformation = false;

    public final View view;
    public static boolean executed_going = false;
    public static Spatial inventorybag = null;
    public static int keyn = -1;
    public static boolean active = true;


    public static boolean ComplexFeedback = true; //false is minimal feedback
    static String goal = "";
    static String opname = "";


    int renderPeriodMS = 25;
    int updatePeriodMS = 25;

    public PVector lasttarget = new PVector(5, 25); //not work
    public List<PVector> path = null;


    boolean invalid = false;

    public Chamba() {
        this(true);
    }

    public Chamba(boolean showWindow) {
        super();


        int w = 50;
        int h = 50;
        int water_threshold = 30;
        Hauto cells = new Hauto(w, h);
        cells.forEach(0, 0, w, h, c -> {
///c.setHeight((int)(Math.random() * 12 + 1));
            float smoothness = 10f;
            c.material = Material.GrassFloor;
            double n = SimplexNoise.noise(c.state.x / smoothness, c.state.y / smoothness);
            if ((n * 64) > water_threshold) {
                c.material = Material.Water;
            }
            c.setHeight((int) (Math.random() * 24 + 1));
        });


        Space space = new Space(cells);
        Maze.buildMaze(space, 3, 3, 23, 23);

        view = new View(space);
        view.frameRate(1000f/renderPeriodMS);
        view.setup();

        Timer s = new Timer();
        s.scheduleAtFixedRate(new TimerTask() {

            public long lastDrawn = System.nanoTime();

            @Override
            public void run() {

                long prev = lastDrawn;

                long now = System.nanoTime();
                double dtSeconds = (now - prev) / 1.0e9;

                view.update(dtSeconds);

                lastDrawn = now;
            }
        }, 0, updatePeriodMS);


        if (showWindow)
            view.newWindow(1000, 800, true);

        cells.forEach(16, 16, 18, 18, new Hauto.SetMaterial(Material.DirtFloor));

        for (int i = 0; i < 16; i++) {
            space.add(newDummy());
        }

    }

    protected GridAgent newDummy() {
        GridAgent a = new GridAgent(Math.random()*8, Math.random()*8) {

            {
                form(new CircularForm(2));
            }

            @Override
            public void update(View space, double dt) {
                vel(5.5 * (Math.random()-0.5), 0.5 * (Math.random()-0.5) );
                torque(0.5f * (Math.random()-0.5f));
            }


//            String lastgone = "";
//            @Override
//            public void update(Effect nextEffect, View view) {
//                if (active) {
//                    //nar.stop();
//                    if (path == null || path.size() <= 0 && !executed_going) {
//                        for (int i = 0; i < 5; i++) { //make thinking in testchamber bit faster
//                            //nar.step(1);
//
//                        }
//                    }
//
//                    /*
//                    if(needpizza) {
//                        hungry--;
//                        if(hungry<0) {
//                            hungry=250;
//                            nar.addInput("(&&,<#1 --> pizza>,<#1 --> [at]>)!"); //also works but better:
////                            for (GridObject obj : space.objects) {
////                                if (obj instanceof Pizza) {
////                                    nar.addInput("<" + ((Pizza) obj).doorname + "--> at>!");
////                                }
////                            }
//                        }
//                    }
//                    */
//                }
//// int a = 0;
//// if (Math.random() < 0.4) {
//// int randDir = (int)(Math.random()*4);
//// if (randDir == 0) a = LEFT;
//// else if (randDir == 1) a = RIGHT;
//// else if (randDir == 2) a = UP;
//// else if (randDir == 3) a = DOWN;
//// /*else if (randDir == 4) a = UPLEFT;
//// else if (randDir == 5) a = UPRIGHT;
//// else if (randDir == 6) a = DOWNLEFT;
//// else if (randDir == 7) a = DOWNRIGHT;*/
//// turn(a);
//// }
///*if (Math.random() < 0.2) {
//                 forward(1);
//                 }*/
//                lasttarget = view.target;
//
//                view.current.set((float)x(), (float)y());
//
//                // System.out.println(nextEffect);
//                if (nextEffect == null) {
//                    path = View.pathShortest(view, this, view.current, view.target);
//                    actions.clear();
//                    // System.out.println(path);
//                    if (path == null) {
//                        executed_going = false;
//                    }
//                    //if (path != null)
//                    {
//                        if (inventorybag != null) {
//                            inventorybag.pos(view.current.x, view.current.y);
//                        }
//                        if (inventorybag == null || !(inventorybag instanceof Key)) {
//                            keyn = -1;
//                        }
//                        if (path == null || path.size() <= 1) {
//
//                            view.target.set(view.current);
//
//                            active = true;
//                            executed_going = false;
//                            //System.out.println("at destination; didnt need to find path");
//                            if (goal != null && !goal.isEmpty()) {// && space.current.equals(space.target)) {
//                                //--nar.step(6);
//                                Spatial obi = null;
//                                if (opname != null && !opname.isEmpty()) {
//                                    for (Body gridi : view.space.getAll()) {
//                                        //TODO use epsilon float comparison
//                                        if (gridi instanceof Spatial && gridi.getName().equals(goal) &&
//                                                gridi.x() == (int) view.current.x &&
//                                                gridi.y() == (int) view.current.y) {
//                                            obi = (Spatial) gridi;
//                                            break;
//                                        }
//                                    }
//                                }
//                                if (obi != null || cells.read[(int) view.current.x][(int) view.current.y].name.equals(goal)) { //only possible for existing ones
//                                    if ("pick".equals(opname)) {
//                                        if (inventorybag != null && inventorybag instanceof Spatial) {
//                                            //we have to drop it
//                                            Spatial ob = inventorybag;
//                                            ob.pos( view.current.x, view.current.y);
//                                            view.space.add(ob);
//                                        }
//                                        inventorybag = obi;
//                                        if (obi != null && view.space.remove(obi)) {
//                                            if (inventorybag.getName().startsWith("{key")) {
//                                                keyn = Integer.parseInt(inventorybag.getName().replaceAll("key", "").replace("}", "").replace("{", ""));
//                                                for (int i = 0; i < cells.h; i++) {
//                                                    for (int j = 0; j < cells.w; j++) {
//                                                        if (Hauto.doornumber(cells.read[i][j]) == keyn) {
//                                                            cells.read[i][j].solid = false;
//                                                            cells.write[i][j].solid = false;
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        }
//                                        //nar.addInput("<"+goal+" --> hold>. :|:");
//                                    } else if ("deactivate".equals(opname)) {
//                                        for (int i = 0; i < cells.h; i++) {
//                                            for (int j = 0; j < cells.w; j++) {
//                                                if (cells.read[i][j].name.equals(goal)) {
//                                                    if (cells.read[i][j].logic == Logic.SWITCH) {
//                                                        cells.read[i][j].logic = Logic.OFFSWITCH;
//                                                        cells.write[i][j].logic = Logic.OFFSWITCH;
//                                                        cells.read[i][j].charge = 0.0f;
//                                                        cells.write[i][j].charge = 0.0f;
//                                                        /*if(ComplexFeedback)
//                                                            nar.addInput("(--,<"+goal+" --> [on]>). :|: %1.00;0.90%");*/
//                                                    }
//                                                }
//                                            }
//                                        }
//
//                                    } else if ("activate".equals(opname)) {
//                                        for (int i = 0; i < cells.h; i++) {
//                                            for (int j = 0; j < cells.w; j++) {
//                                                if (cells.read[i][j].name.equals(goal)) {
//                                                    if (cells.read[i][j].logic == Logic.OFFSWITCH) {
//                                                        cells.read[i][j].logic = Logic.SWITCH;
//                                                        cells.write[i][j].logic = Logic.SWITCH;
//                                                        cells.read[i][j].charge = 1.0f;
//                                                        cells.write[i][j].charge = 1.0f;
//                                                        /*if(ComplexFeedback)
//                                                            nar.addInput("<"+goal+" --> [on]>. :|:");*/
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                    if ("go-to".equals(opname)) {
//                                        executed_going = false;
//                                        /*if(!goal.equals(lastgone)) {
//                                            nar.addInput("<"+goal+" --> [at]>. :|:");
//                                        }*/
//                                        lastgone = goal;
//                                        if (goal.startsWith("{pizza")) {
//                                            Spatial toRemove = null;
//                                            for (Body obj : view.space.getAll()) { //remove pizza
//                                                if (obj instanceof Spatial) {
//                                                    Spatial obo = (Spatial) obj;
//                                                    if (obo.getName().equals(goal)) {
//                                                        toRemove = obo;
//                                                    }
//                                                }
//                                            }
//                                            if (toRemove != null) {
//                                                view.space.remove(toRemove);
//                                            }
//                                            //hungry = 500;
//                                            ////nar.addInput("<"+goal+" --> eat>. :|:"); //that is sufficient:
//                                            //nar.addInput("<"+goal+" --> [at]>. :|:");
//                                        }
//                                        active = true;
//                                    }
//                                }
//                            }
//                            opname = "";
//                            /*if(!executed && !executed_going)
//                                nar.step(1);*/
//                        } else {
//                            //nar.step(10);
//                            //nar.memory.setEnabled(false);
//
//                            executed_going = true;
//                            active = false;
//                            //nar.step(1);
//                            int numSteps = Math.min(10, path.size());
//                            float cx = (float)x();
//                            float cy = (float)y();
//                            for (int i = 1; i < numSteps; i++) {
//                                PVector next = path.get(i);
//                                int dx = (int) (next.x - cx);
//                                int dy = (int) (next.y - cy);
//                                if ((dx == 0) && (dy == 1)) {
//                                    turn(UP);
//                                    forward(1);
//                                }
//                                if ((dx == 1) && (dy == 0)) {
//                                    turn(RIGHT);
//                                    forward(1);
//                                }
//                                if ((dx == -1) && (dy == 0)) {
//                                    turn(LEFT);
//                                    forward(1);
//                                }
//                                if ((dx == 0) && (dy == -1)) {
//                                    turn(DOWN);
//                                    forward(1);
//                                }
//                                cx = next.x;
//                                cy = next.y;
//                            }
//                        }
//                    }
//                }
//            }

        };
//        Goto wu = new Goto(this, "^go-to");
//        nar.memory.addOperator(wu);
//        Pick wa = new Pick(this, "^pick");
//        nar.memory.addOperator(wa);
//        Activate waa = new Activate(this, "^activate");
//        nar.memory.addOperator(waa);
//        Deactivate waaa = new Deactivate(this, "^deactivate");
//        nar.memory.addOperator(waaa);

        return a;
    }

    public static void main(String[] args) {

        //set NAR architecture parameters:
        //builder...
//        Parameters.CONSIDER_NEW_OPERATION_BIAS = 1.0f; //not that much events in testchamber anyway
//        Parameters.SEQUENCE_BAG_SIZE = 100; //but many possible different ways to achieve certain things
//        NAR nar = new NAR();
//        nar.param.decisionThreshold.set(0.51);
        //set NAR runtime parmeters:

        /*for(NAR.PluginState pluginstate : nar.getPlugins()) {
            if(pluginstate.plugin instanceof InternalExperience || pluginstate.plugin instanceof FullInternalExperience) {
                nar.removePlugin(pluginstate);
            }
        }*/

        //nar.addPlugin(new TemporalParticlePlanner());

        //(nar.param).duration.set(10);
//        (nar.param).noiseLevel.set(0);
//        new NARSwing(nar);

        new Chamba();

        //nar.start(narUpdatePeriod);

    }


    public void operate(String arg, String opnamer) {
        opname = opnamer;
        Hauto cells = view.space.cells;
        goal = arg;
        for (int i = 0; i < cells.w; i++) {
            for (int j = 0; j < cells.h; j++) {
                if (cells.read[i][j].name.equals(arg)) {
                    if (opname.equals("go-to"))
                        view.target.set(i, j);
                }
            }
        }
        //if("pick".equals(opname)) {
        for (Body gridi : view.space.getAll()) {
            if (gridi instanceof Spatial && gridi.getName().equals(goal)) { //Key && ((Key)gridi).doorname.equals(goal)) {
                Spatial gridu = (Spatial) gridi;
                if (opname.equals("go-to"))
                    view.target.set((float)gridu.x(), (float)gridu.y());
            }
        }
        //}
    }


}