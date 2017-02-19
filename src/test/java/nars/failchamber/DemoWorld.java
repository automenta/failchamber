package nars.failchamber;

import jcog.Util;
import jcog.net.UDP;
import nars.failchamber.agent.SimpleHaiQClient;
import nars.failchamber.object.Pacman;
import nars.failchamber.client.AgentClient;
import nars.failchamber.map.Maze;
import nars.failchamber.object.Herb;
import nars.failchamber.object.Poison;
import nars.failchamber.state.Cell;
import nars.failchamber.state.Hauto;
import nars.failchamber.state.SimplexNoise;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.stream.IntStream;


public class DemoWorld extends Space {


    static final int w = 50;
    static final int h = 50;
    static final int water_threshold = 30;

    public DemoWorld() throws SocketException, UnknownHostException {
        super(new Hauto(w, h));

        cells.forEach(0, 0, w, h, c -> {
///c.setHeight((int)(Math.random() * 12 + 1));
            float smoothness = 10f;
            c.material = Cell.Material.GrassFloor;
            double n = SimplexNoise.noise(c.state.x / smoothness, c.state.y / smoothness);
            if ((n * 64) > water_threshold) {
                c.material = Cell.Material.Water;
            }
            c.setHeight((int) (Math.random() * 24 + 1));
        });


        Maze.buildMaze(this, 3, 3, 23, 23);

        cells.forEach(16, 16, 18, 18, new Hauto.SetMaterial(Cell.Material.DirtFloor));


        for (int i = 0; i < 75; i++) {
            add(new Herb.Cannanip(0.5f + Math.random() * 0.5f).pos(whereSpawns()));
            add(new Poison.Plutonium(0.5f + Math.random() * 0.5f).pos(whereSpawns()));
        }
    }

    public static void main(String[] args) throws Exception {
        new Chamba(
                new DemoWorld(),
                true,
                10000);

        new SimpleHaiQClient(15000, "localhost", 10000);
        new SimpleHaiQClient(15001, "localhost", 10000);

//        for (int i = 0; i < 4; i++) {
//            newDummyAgent(15000 + i);
//        }
    }

    protected static void newDummyAgent(int port) throws SocketException, UnknownHostException {
        Random random = new Random();

        UDP u = new AgentClient(port, "localhost", 10000) {

            double lookAngle = 0;

            @Override public void run() {
                while (running) {

                    switch (random.nextInt(3)) {
                        case 0:
                            float x = 30;// * random.nextFloat();
                            float y = 0; //100 * random.nextFloat();
                            force(x, y);
                            break;
                        case 1:
                            float t = 16 * (random.nextFloat() - 0.5f);
                            torque(t);
                            break;

                    }

                    see(25.0, IntStream.range(-5, 5).mapToDouble(i -> lookAngle + i * 0.1).toArray());

                    lookAngle += 0.05;

                    Util.pause(100);
                }
            }

        };

    }

    protected static Pacman newDummy() {
        Pacman a = new Pacman(1) {

            @Override
            public void update(View space, double dt) {
                vel(6.5 * (Math.random() - 0.5), 4.5 * (Math.random() - 0.5));
                torque(1.9f * (Math.random() - 0.5f));
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

}
