package nars.testchamba.state;

import nars.testchamba.Chamba;
import nars.testchamba.View;
import nars.testchamba.object.Key;
import nars.testchamba.object.Pizza;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Hauto {

    final public static int RIGHT = -90;
    final public static int DOWN = 180;
    final public static int LEFT = 90;
    final public static int UP = 0;
    final public static int UPLEFT = (UP + LEFT) / 2;
    final public static int UPRIGHT = (UP + RIGHT) / 2;
    final public static int DOWNLEFT = (DOWN + LEFT) / 2;
    final public static int DOWNRIGHT = (DOWN + RIGHT) / 2;
    public static AtomicInteger entityID = new AtomicInteger(0);
    public static boolean allow_imitating = false;
    public final int w;
    public final int h;
    final Cell selected = new Cell();
    private final Random random = new Random();
    public String oper = "";
    public String label = "";
    public String wish = "";
    public int t = 0;
    public Cell[][] read; //2D-array(**) of Cell objects(*)
    public Cell[][] write; //2D-array(**) of Cell objects(*)
    String doorname = "";

    public Hauto(int w, int h) {

        this.w = w;
        this.h = h;
        read = new Cell[w][h];
        write = new Cell[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                CellState s = new CellState(i, j);
                read[i][j] = new Cell(s);
                write[i][j] = new Cell(s);

                if ((i == 0) || (i == w - 1))
                    read[i][j].setBoundary();
                else if ((j == 0) || (j == h - 1))
                    read[i][j].setBoundary();
            }
        }

        copyReadToWrite();
        click("StoneWall", "", "");
    }

    static boolean is_logic(Cell c) {
        return (c.logic == Cell.Logic.OR || c.logic == Cell.Logic.XOR || c.logic == Cell.Logic.AND || c.logic == Cell.Logic.NOT);
    }

    public static int doornumber(Cell c) {
        if (c.material != Cell.Material.Door) {
            return -2;
        }
        return Integer.parseInt(c.name.replaceAll("door", "").replaceAll("\\}", "").replaceAll("\\{", ""));
    }

    public static boolean bridge(Cell.Logic c) {
        return c == Cell.Logic.UNCERTAINBRIDGE || c == Cell.Logic.BRIDGE;
    }

    public static float Neighbor_Value(Cell c, String mode, float data) {
        if ("having_charge".equals(mode)) {
            if (c.logic != Cell.Logic.WIRE)
                return -1.0f; //not a charge
            return c.charge == data ? 1.0f : 0.0f;
        }
        if ("just_getcharge".equals(mode)) {
            return c.charge;
        }
        if ("get_light".equals(mode) && (data == 1 || !c.solid && !(c.material == Cell.Material.StoneWall))) {
            return Math.max(c.charge * 0.2f, c.light);
        }
        return 0.0f;
    }

    public static int irand(int max) {
        return (int) (Math.random() * max);
    }

    public static Cell FirstNeighbor(int i, int j, Cell[][] readCells, String Condition, float data) {
        int k;
        int l;
        for (k = i - 1; k <= i + 1; k++) {
            for (l = j - 1; l <= j + 1; l++) {
                if (!(k == i && j == l)) {
                    if (Neighbor_Value(readCells[k][l], Condition, data) != 0) {
                        return readCells[k][l];
                    }
                }
            }
        }
        return null;
    }

    public static float NeighborsValue(String op, int i, int j, Cell[][] readCells, String Condition, float data) {
        return Op(op, Op(op, Op(op, Op(op, Op(op, Op(op, Op(op, Neighbor_Value(readCells[i + 1][j], Condition, data), Neighbor_Value(readCells[i - 1][j], Condition, data)), Neighbor_Value(readCells[i + 1][j + 1], Condition, data)), Neighbor_Value(readCells[i - 1][j - 1], Condition, data)), Neighbor_Value(readCells[i][j + 1], Condition, data)), Neighbor_Value(readCells[i][j - 1], Condition, data)), Neighbor_Value(readCells[i - 1][j + 1], Condition, data)), Neighbor_Value(readCells[i + 1][j - 1], Condition, data));
    }

    public static float NeighborsValue2(String op, int i, int j, Cell[][] readCells, String Condition, float data) {
        return Op(op, Op(op, Op(op, Neighbor_Value(readCells[i + 1][j], Condition, data), Neighbor_Value(readCells[i - 1][j], Condition, data)), Neighbor_Value(readCells[i][j + 1], Condition, data)), Neighbor_Value(readCells[i][j - 1], Condition, data));
    }

    public static float Op(String op, float a, float b) {
        switch (op) {
            case "op_or":
                return (a == 1.0f || b == 1.0f) ? 1.0f : 0.0f;
            case "op_and":
                return (a == 1.0f && b == 1.0f) ? 1.0f : 0.0f;
            case "op_max":
                return Math.max(a, b);
            case "op_min":
                return Math.min(a, b);
            case "op_plus":
                return a + b;
            case "op_mul":
                return a * b;
            default:
                return 0;
        }
    }

    //put to beginning because we will need this one most often
    public void update(int t, int i, int j, Cell w, Cell r, Cell left, Cell right, Cell up,
                       Cell down, Cell left_up, Cell left_down, Cell right_up, Cell right_down, Cell[][] readcells) {
        w.charge = r.charge;
        w.value = r.value;
        w.value2 = r.value2;
        w.solid = r.solid;
        w.chargeFront = false;
        //
//        if ((r.machine == Cell.Machine.Light || r.machine == Cell.Machine.Turret) && r.charge == 1) {
//            if (r.light != 1.0f) {
//                boolean nope = false;
//                if (r.machine == Cell.Machine.Turret) {
//                    for (GridObject gr : Chamba.view.objects) {
//                        if (gr instanceof LocalGridObject) {
//                            LocalGridObject o = (LocalGridObject) gr;
//                            if (o.x == i && o.y == j) {
//                                nope = true;
//                            }
//                        }
//                    }
////                    if(!nope) {
////                        TestChamba.space.add(new Pizza((int)i, (int)j, "{pizza"+entityID.toString()+"}"));
////                        if(TestChamba.staticInformation)
////                        nar.addInput("<{pizza"+entityID.toString()+"} --> pizza>.");
////                        entityID++;
////                    }
//                }
//                nar.addInput("<"+r.name+" --> [on]>. :|:");
//            }
//            w.light = 1.0f;


        //} else {

        w.light = NeighborsValue2("op_max", i, j, readcells, "get_light", (r.solid || r.material == Cell.Material.StoneWall) ? 1 : 0) / 1.1f; //1.1

        ///door
        if (r.material == Cell.Material.Door) {
            if (NeighborsValue2("op_or", i, j, readcells, "having_charge", 1.0f) != 0) {
                w.solid = false;
                if (r.solid) {
//                    nar.addInput("<"+r.name+" --> [opened]>. :|:");
                }
            } else {
//                if (!r.solid && Chamba.keyn != doornumber(r)) {
//                    w.solid = true;
////                    nar.addInput("(--,<"+r.name+" --> [opened]>). :|: %1.00;0.90%");
//                }
            }
        }
        //////// WIRE / CURRENT PULSE FLOW /////////////////////////////////////////////////////////////
        if (r.logic == Cell.Logic.WIRE) {
            if (!r.chargeFront && r.charge == 0.0 && NeighborsValue2("op_or", i, j, readcells, "having_charge", 1.0f) != 0) {
                w.charge = 1.0f;
                w.chargeFront = true;    //it's on the front of the wave of change
            }
            if (!r.chargeFront && r.charge == 1.0 && NeighborsValue2("op_or", i, j, readcells, "having_charge", 0.0f) != 0) {
                w.charge = 0.0f;
                w.chargeFront = true;    //it's on the front of the wave of change
            }
            if (!r.chargeFront && r.charge == 0 && (up.logic == Cell.Logic.SWITCH || down.logic == Cell.Logic.SWITCH || (left.logic == Cell.Logic.SWITCH || (is_logic(left) && left.value == 1)) || (right.logic == Cell.Logic.SWITCH || (is_logic(right) && right.value == 1)))) {
                w.charge = 1.0f;
                w.chargeFront = true;   //it's on the front of the wave of change
            }
            if (!r.chargeFront && r.charge == 1 && (up.logic == Cell.Logic.OFFSWITCH || down.logic == Cell.Logic.OFFSWITCH || (left.logic == Cell.Logic.OFFSWITCH || (is_logic(left) && left.value == 0)) || (right.logic == Cell.Logic.OFFSWITCH || (is_logic(right) && right.value == 0)))) {
                w.charge = 0.0f;
                w.chargeFront = true;    //it's on the front of the wave of change
            }
        }
        //////////// LOGIC ELEMENTS ////////////////////////////////////////////////////////////////////
        if (r.logic == Cell.Logic.NOT && (up.charge == 0 || up.charge == 1))
            w.value = (up.charge == 0 ? 1 : 0); //eval state from input connection
        if (r.logic == Cell.Logic.NOT && (down.charge == 0 || down.charge == 1))
            w.value = (down.charge == 0 ? 1 : 0); //eval state from input connection
        if (r.logic == Cell.Logic.AND)
            w.value = (up.charge == 1 && down.charge == 1) ? 1.0f : 0.0f; //eval state from input connections
        if (r.logic == Cell.Logic.OR)
            w.value = (up.charge == 1 || down.charge == 1) ? 1.0f : 0.0f; //eval state from input connections
        if (r.logic == Cell.Logic.XOR)
            w.value = (up.charge == 1 ^ down.charge == 1) ? 1.0f : 0.0f;  //eval state from input connections

        //ADD BIDIRECTIONAL LOGIC BRIDGE TO OVERCOME 2D TOPOLOGY
        if (r.logic == Cell.Logic.BRIDGE || (r.logic == Cell.Logic.UNCERTAINBRIDGE && random.nextDouble() > 0.5)) {
            if (left.chargeFront && left.logic == Cell.Logic.WIRE)
                w.value = left.charge;
            else if (right.chargeFront && right.logic == Cell.Logic.WIRE)
                w.value = right.charge;

            if (up.chargeFront && up.logic == Cell.Logic.WIRE)
                w.value2 = up.charge;
            else if (down.chargeFront && down.logic == Cell.Logic.WIRE)
                w.value2 = down.charge;
        }

        if (!r.chargeFront && r.charge == 0 && (((bridge(right.logic) && right.value == 1) || (bridge(left.logic) && left.value == 1)) || ((bridge(down.logic) && down.value2 == 1)))) {
            w.charge = 1;
            w.chargeFront = true;
        }
        if (!r.chargeFront && r.charge == 1 && (((bridge(right.logic) && right.value == 0) || (bridge(left.logic) && left.value == 0)) || ((bridge(down.logic) && down.value2 == 0)))) {
            w.charge = 0;
            w.chargeFront = true;
        }

        if (r.logic == Cell.Logic.Load) {
            w.charge = Math.max(up.charge, Math.max(down.charge, Math.max(left.charge, right.charge)));
            w.chargeFront = false;
        }
        if (r.machine == Cell.Machine.Light || r.machine == Cell.Machine.Turret) {
            if (r.light == 1.0f && w.light != 1.0f) { //changed
                //nar.addInput("(--,<"+r.name+" --> [on]>). :|: %1.00;0.90%");
            }
        }
        //w.charge *= w.conductivity;
    }

    public void clicked(int x, int y, View view) {
        if (x == 0 || y == 0 || x == w - 1 || y == h - 1)
            return;

        if (!doorname.isEmpty() && !doorname.contains("{")) {
            doorname = '{' + doorname + '}';
        }

        if (oper.equals("perceive")) {
            read[x][y].name = "place" + entityID.toString();
            write[x][y].name = "place" + entityID.toString();

            /*
            nar.addInput("<"+"{place"+entityID.toString()+"} --> place>.");

            if(TestChamba.curiousity) {
                space.nar.addInput("<(^go-to," + "place"+entityID.toString() + ") =/> <Self --> [curious]>>.");
            }
            */
            //    entityID.getAndIncrement();
            return;
        }

        if (!oper.isEmpty()) {
            if (read[x][y].name != null && !read[x][y].name.isEmpty() && !"pick".equals(oper)) {
                /*
                if(allow_imitating) {
                    nar.addInput("(^" + oper + ","+readCells[x][y].name+")! :|:"); //we will force the action
                }
                else {
                    nar.addInput("(^" + oper + ","+readCells[x][y].name+"). :|:");
                    TestChamba.operateObj(readCells[x][y].name, oper);
                }
                */

                //nar.addInput("(^" + oper + ","+readCells[x][y].name+"). :|:"); //in order to make NARS an observer
                //--nar.step(1);
                //.operateObj(readCells[x][y].name, oper);
            }
            String s = view.what(x, y);
            if (!s.isEmpty()) {
                /*
                if(allow_imitating) {
                    nar.addInput("(^" + oper + ","+s+")! :|:");
                }
                else {
                    nar.addInput("(^" + oper + ","+s+"). :|:");
                    TestChamba.operateObj(s, oper);
                }
                */

                //nar.executeDummyDecision("(^" + oper + ","+s+")");
                //--nar.step(1);
                // TestChamba.operateObj(s, oper);
            }
            return;
        }

        if (wish != null && !wish.isEmpty()) {
            boolean inverse = false;
            if (wish.equals("closed") || wish.equals("off")) {
                inverse = true;
            }
            String wishreal = wish.replace("closed", "opened").replace("off", "on");
            if (read[x][y].name != null && !read[x][y].name.isEmpty()) {
                //nar.addInput("(^" + oper + ","+readCells[x][y].name+")!"); //we will force the action
                /*
                if(!inverse) {
                    nar.addInput("<" + readCells[x][y].name+" --> ["+wishreal+"]>! :|:"); //in order to make NARS an observer
                } else {
                    nar.addInput("(--,<" + readCells[x][y].name+" --> ["+wishreal+"]>)! :|: %1.00;0.90%");
                }
                */
                //--nar.step(1);
            }
            String s = view.what(x, y);
            if (!s.isEmpty()) {
                //ar.addInput("(^" + oper + ","+s+")!");
                /*
                if(!inverse) {
                    nar.addInput("<" + s +" --> ["+wishreal+"]>! :|:"); //in order to make NARS an observer
                } else {
                    nar.addInput("(--,<" + s +" --> ["+wishreal+"]>)! :|: %1.00;0.90%");
                }
                */

                //--nar.step(1);
            }
            return;
        }

        if (doorname != null && !doorname.isEmpty() && selected.material == Cell.Material.Door) {
            view.space.add(new Key(x, y, doorname.replace("door", "key")));
            /*
            if (TestChamba.staticInformation)
                nar.addInput("<" + doorname.replace("door", "key") + " --> key>.");
            if (TestChamba.curiousity) {
                space.nar.addInput("<(^go-to," + doorname.replace("door", "key") + ") =/> <Self --> [curious]>>.");
                space.nar.addInput("<(^pick," + doorname.replace("door", "key") + ") =/> <Self --> [curious]>>.");
            }
            */
            doorname = "";
            return;
        }
        if (selected.material == Cell.Material.Pizza) {
            doorname = "{pizza" + entityID.toString() + '}';
        }
        if (doorname != null && !doorname.isEmpty() && selected.material == Cell.Material.Pizza) {
            view.space.add(new Pizza(x, y, doorname));
            /*
            if (TestChamba.staticInformation)
                nar.addInput("<" + doorname + " --> pizza>.");
            if (TestChamba.curiousity) {
                space.nar.addInput("<(^go-to," + doorname + ") =/> <Self --> [curious]>>.");
            }
            */
            entityID.getAndIncrement();
            doorname = "";
            return;
        }
        if (!(selected.material == Cell.Material.Door) && !(selected.material == Cell.Material.Pizza))
            doorname = "";

        read[x][y].charge = selected.charge;
        write[x][y].charge = selected.charge;
        read[x][y].logic = selected.logic;
        write[x][y].logic = selected.logic;
        read[x][y].material = selected.material;
        write[x][y].material = selected.material;
        read[x][y].machine = selected.machine;
        write[x][y].machine = selected.machine;

        if (selected.material == Cell.Material.Pizza || selected.material == Cell.Material.Door || selected.logic == Cell.Logic.OFFSWITCH || selected.logic == Cell.Logic.SWITCH || selected.machine == Cell.Machine.Light || selected.machine == Cell.Machine.Turret) //or other entity...
        {
            String name = "";
            if (selected.material == Cell.Material.Door) {
                name = "door";
            }
            if (selected.logic == Cell.Logic.SWITCH || selected.logic == Cell.Logic.OFFSWITCH)
                name = "switch";
            if (selected.machine == Cell.Machine.Light)
                name = "light";
            if (selected.machine == Cell.Machine.Turret)
                name = "firework";
            String Klass = name;
            name += (entityID.toString());
            if (selected.material == Cell.Material.Door) {
                doorname = name;
            }

            name = '{' + name + '}';
            //if it has name already, dont allow overwrite

            if (read[x][y].name.isEmpty()) {
                /*
                if (TestChamba.staticInformation)
                    nar.addInput("<" + name + " --> " + Klass + ">.");
                    */
                read[x][y].name = name;
                write[x][y].name = name;
                /*
                if (selected.logic == OFFSWITCH) {
                    nar.addInput("(--,<" + name + " --> " + "[on]>). :|: %1.00;0.90%");
                    if (TestChamba.curiousity) {
                        space.nar.addInput("<(^go-to," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^activate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^deactivate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                    }
                }

                if (selected.logic == SWITCH) {
                    nar.addInput("<" + name + " --> " + "[on]>. :|:");
                    if (TestChamba.curiousity) {
                        space.nar.addInput("<(^go-to," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^activate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^deactivate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                    }
                }
                */
            } else {
                /*
                if (selected.logic == OFFSWITCH) { //already has a name so use this one
                    nar.addInput("<" + readCells[x][y].name + " --> " + "[off]>. :|:");
                    if (TestChamba.curiousity) {
                        space.nar.addInput("<(^go-to," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^activate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^deactivate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                    }
                }
                if (selected.logic == SWITCH) {
                    nar.addInput("<" + readCells[x][y].name + " --> " + "[on]>. :|:");
                    if (TestChamba.curiousity) {
                        space.nar.addInput("<(^go-to," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^activate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                        space.nar.addInput("<(^deactivate," + readCells[x][y].name + ") =/> <Self --> [curious]>>.");
                    }
                }
                */
            }

            entityID.getAndIncrement();
        }
    }

    public void click(String label, String oper, String wish) {
        this.label = label;
        this.oper = oper;
        this.wish = wish;
        if (label != null && label.isEmpty()) {
            return;
        }
        switch (label) {
            case "StoneWall":
                selected.material = Cell.Material.StoneWall;
                selected.solid = true;
                selected.logic = Cell.Logic.NotALogicBlock;
                selected.charge = 0;
                break;
            case "Water":
                selected.material = Cell.Material.Water;
                selected.logic = Cell.Logic.NotALogicBlock;
                selected.charge = 0;
                break;
            //TODO
        }


        if ("DirtFloor".equals(label)) {
            selected.material = Cell.Material.DirtFloor;
            selected.logic = Cell.Logic.NotALogicBlock;
            selected.charge = 0;
        }

        if ("GrassFloor".equals(label)) {
            selected.material = Cell.Material.GrassFloor;
            selected.logic = Cell.Logic.NotALogicBlock;
            selected.charge = 0;
        }

        selected.machine = null;

        if ("NOT".equals(label)) {
            selected.setLogic(Cell.Logic.NOT, 0);
        }
        if ("AND".equals(label)) {
            selected.setLogic(Cell.Logic.AND, 0);
        }
        if ("OR".equals(label)) {
            selected.setLogic(Cell.Logic.OR, 0);
        }
        if ("XOR".equals(label)) {
            selected.setLogic(Cell.Logic.XOR, 0);
        }
        if ("bridge".equals(label)) {
            selected.setLogic(Cell.Logic.BRIDGE, 0);
        }
        if ("uncertainbridge".equals(label)) {
            selected.setLogic(Cell.Logic.UNCERTAINBRIDGE, 0);
        }
        if ("OnWire".equals(label)) {
            selected.setLogic(Cell.Logic.WIRE, 1.0f);
            selected.chargeFront = true;
        }
        if ("OffWire".equals(label)) {
            selected.setLogic(Cell.Logic.WIRE, 0);
            selected.chargeFront = true;
        }
        if ("onswitch".equals(label)) {
            selected.setLogic(Cell.Logic.SWITCH, 1.0f);
        }
        if ("offswitch".equals(label)) {
            selected.setLogic(Cell.Logic.OFFSWITCH, 0);
        }

        if ("Pizza".equals(label)) {
            selected.logic = Cell.Logic.NotALogicBlock;
            selected.material = Cell.Material.Pizza;
        }
        if ("Door".equals(label)) {
            selected.logic = Cell.Logic.NotALogicBlock;
            selected.charge = 0;
            selected.material = Cell.Material.Door;
            selected.solid = true;
        }
        if ("Light".equals(label)) {
            selected.logic = Cell.Logic.Load;
            selected.material = Cell.Material.Machine;
            selected.machine = Cell.Machine.Light;
            selected.solid = true;
        }
        if ("Turret".equals(label)) {
            selected.logic = Cell.Logic.Load;
            selected.material = Cell.Material.Machine;
            selected.machine = Cell.Machine.Turret;
            selected.solid = true;
        }

    }

    public void copyReadToWrite() {
        for (int i = 0; i < w; i++) {
            Cell[] wi = write[i];
            Cell[] ri = read[i];
            for (int j = 0; j < h; j++) {
                wi[j].copyFrom(ri[j]);
            }
        }
    }

    public void update() {
        this.t++;
        for (int i = 1; i < this.w - 1; i++) {
            for (int j = 1; j < this.h - 1; j++) {
                update(this.t, i, j, this.write[i][j], this.read[i][j], this.read[i - 1][j], this.read[i + 1][j], this.read[i][j + 1], this.read[i][j - 1], this.read[i - 1][j + 1], this.read[i - 1][j - 1], this.read[i + 1][j + 1], this.read[i + 1][j - 1], this.read);
            }
        }
        //change write to read and read to write
        Cell[][] temp2 = this.read;
        this.read = this.write;
        this.write = temp2;
    }

    public void forEach(int x1, int y1, int x2, int y2, Consumer<Cell> c) {
        x1 = Math.max(1, x1);
        x2 = Math.min(w - 1, x2);
        x2 = Math.max(x1, x2);
        y1 = Math.max(1, y1);
        y2 = Math.min(h - 1, y2);
        y2 = Math.max(y1, y2);

        for (int tx = x1; tx < x2; tx++) {
            Cell[] rx = read[tx];
            for (int ty = y1; ty < y2; ty++) {
                c.accept(rx[ty]);
            }
        }
        copyReadToWrite();
    }

    public void at(int x, int y, Consumer<Cell> c) {
        c.accept(read[x][y]);
        //copyReadToWrite();
    }

    public Cell at(int x, int y) {
        return read[x][y];
    }

    public static class SetMaterial implements Consumer<Cell> {
        private final Cell.Material material;

        public SetMaterial(Cell.Material m) {
            this.material = m;
        }

        @Override
        public void accept(Cell cell) {
            cell.material = material;
            cell.height = (material == Cell.Material.StoneWall) ? 64 : 1;
            cell.solid = material == Cell.Material.StoneWall;
        }

    }
}
