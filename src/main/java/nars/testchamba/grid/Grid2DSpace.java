package nars.testchamba.grid;

import nars.testchamba.TestChamba;
import nars.testchamba.util.NWindow;
import nars.testchamba.particle.ParticleSystem;
import processing.core.PApplet;
import processing.core.PVector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Grid2DSpace extends PApplet {


    public static boolean world_used = false;

    public final PVector target = new PVector(25, 25); //need to be init equal else feedback will
    public final PVector current = new PVector(0, 0);

    ///////////////HAMLIB
    //processingjs compatibility layer
    int mouseScroll = 0;
    public final Hauto cells;
    public final Set<GridObject> objects = Collections.newSetFromMap(new ConcurrentHashMap<>());
    //ProcessingJs processingjs = new ProcessingJs();

    //Hnav 2D navigation system
    final Hnav hnav = new Hnav();

    //Object
    float selection_distance = 10;
    public float maxNodeSize = 40f;


    public int automataPeriod = 1; //how many cycles between each automata update
    public int agentPeriod = 1;  //how many cycles between each agent update

    boolean drawn = false;
    final Hsim hsim = new Hsim();
    final Hamlib hamlib = new Hamlib();

    float sx = 800;
    float sy = 800;
    long lasttime = -1;
    double realtime;
    public ParticleSystem particles;
    private Container contentPane;

    public Grid2DSpace(Hauto cells) {
        super();
        this.cells = cells;
        world_used = true;

        initSurface();
        startSurface();

    }


    public void add(GridObject g) {
        objects.add(g);
        g.init(this);
    }

    public NWindow newWindow(int width, int height, final boolean exitOnClose) {

        NWindow j = new NWindow("") {

            @Override
            protected void close() {
                stop();
                getContentPane().removeAll();

                if (exitOnClose)
                    System.exit(0);
            }
        };

        contentPane = j.getContentPane();
        contentPane.setLayout(new BorderLayout());

        //JPanel menu = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        
        /*final JCheckBox syntaxEnable = new JCheckBox("Syntax");
        syntaxEnable.addActionListener(new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
        }
        });
        menu.add(syntaxEnable);
         */

        EditorPanel editor = new EditorPanel(this);
        NWindow editorWindow = new NWindow("Edit", editor);
        editorWindow.setSize(200, 400);
        editorWindow.setVisible(true);


//        contentPane.add(menu, BorderLayout.NORTH);
        contentPane.add(component(), BorderLayout.CENTER);

        j.addWindowStateListener(e -> SwingUtilities.invokeLater(() -> resizeToParent(j)));
        j.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> resizeToParent(j));
            }
        });
        component().addMouseWheelListener(evt -> {
            mouseScroll = -evt.getWheelRotation();
            mouseScrolled();
        });

        j.setSize(width, height);//initial size of the window
        j.setVisible(true);

        resizeToParent(j);

        return j;
    }

    private void resizeToParent(Container bounds) {
        getSurface().setSize(bounds.getWidth(), bounds.getHeight());
    }

    private Component component() {
        return (Component) (getSurface().getNative());
    }


//    @Override
//    protected void resizeRenderer(int newWidth, int newHeight) {
//        super.resizeRenderer(newWidth, newHeight); //To change body of generated methods, choose Tools | Templates.
//        drawn = false;
//    }

    public void mouseScrolled() {
        hamlib.mouseScrolled();
    }

    @Override
    public void keyPressed() {
        hamlib.keyPressed();
    }

    @Override
    public void mouseMoved() {
        hamlib.mouseMoved();
    }

    @Override
    public void mouseReleased() {
        hamlib.mouseReleased();
    }

    @Override
    public void mouseDragged() {
        hamlib.mouseDragged();
    }

    @Override
    public void mousePressed() {
        hamlib.mousePressed();
    }

    public void automataclicked(float x, float y) {
        if (x < 0 || y < 0) {
            return;
        }
        float realx = x / cellSize;
        float realy = y / cellSize;
        if (realx >= cells.w || realy >= cells.h) {
            return;
        }
        cells.clicked((int) realx, (int) realy, this);
    }

    public void updateAutomata() {
        cells.Exec();
    }

    public void update(TestChamba s) {
        realtime = System.nanoTime() / 1.0e9;

        if (time % automataPeriod == 0 || TestChamba.executed) {
            updateAutomata();
        }
        if (time % agentPeriod == 0 || TestChamba.executed) {
            try {
                for (GridObject g : objects) {
                    Effect e = (g instanceof GridAgent) ? ((GridAgent) g).perceiveNext() : null;
                    g.update(e);

                    if (g instanceof GridAgent) {
                        GridAgent b = (GridAgent) g;
                        if (b.actions.size() > 0) {
                            Action a = b.actions.pop();
                            if (a != null) {
                                process(b, a);
                            }
                        }

                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        TestChamba.executed = false;
    }

    @Override
    public void draw() {

        //background(50f, 0.1f);
        fill(0, 25f);
        rect(0, 0, width, height);

        pushMatrix();

        hnav.Transform();


        drawGround();
        drawObjects();
        drawParticles();

        //popMatrix();


        popMatrix();
        //hrend_DrawGUI();
    }


    public void process(GridAgent agent, Action action) {
        Effect e = action.process(this, agent);
        if (e != null) {
            agent.perceive(e);
        }
    }




    public void hrend_DrawGUI() {
        //fill(255);
        // rect(10,10,10,10);
    }

    @Override
    public void setup() {
        particles = new ParticleSystem(this);
    }

    int time = 0;
    final float cellSize = 1;

    public int getTime() {
        return time;
    }

    public double getRealtime() {
        return realtime;
    }


//    enum MotionEffect {
//        Moved, PainfullyMoved, TooHigh, TooSolid /* collision, impenetrable, bump */, Stuck /* flypaper, quicksand */, TooFar
//    }

    public String whyNonTraversible(GridAgent agent, int x, int y, int tx, int ty) {
        int dx = Math.abs(tx - x);
        int dy = Math.abs(ty - y);

        if (!((dx <= 1) && (dy <= 1)))
            return "Too far";

        if ((tx < 0) || (ty < 0) || (tx >= cells.w) || (ty >= cells.h))
            return "Out of bounds: " + tx + ' ' + ty;

        Cell from = cells.at(x, y);
        Cell to = cells.at(tx, ty);

        //System.out.println(to + " " + to.material);
        if ((to.material == Cell.Material.StoneWall) || to.is_solid || to.material == Cell.Material.Water || to.logic == Cell.Logic.BRIDGE || to.logic == Cell.Logic.UNCERTAINBRIDGE)
            return "Too solid";

        final float maxTraversableHeight = 8;
        float dHeight = to.height - from.height;
        //if (dHeight > maxTraversableHeight)
        //    return "Too high";

        return null;
    }


    public Effect getMotionEffect(GridAgent agent, Action a, int x, int y, int tx, int ty) {
        String reason = whyNonTraversible(agent, x, y, tx, ty);
        if (reason == null) {
            return new Effect(a, true, getTime(), "Moved");
        } else {
            return new Effect(a, false, getTime(), reason);
        }


    }

    public void drawObjects() {
        pushMatrix();

        //shift half a cell down and right so that when an object draws, it's centerd in the middle of a cell.
        translate(cellSize / 4f, cellSize / 4f);

        for (GridObject object : objects) object.draw();
        popMatrix();
    }

    public void drawParticles() {
        //PImage b = particles.particleImage;
        //this.blend(b, 0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), PImage.ADD);
        
       /* particles.tick(); //crashes
        for (Particle p : particles.p) {
            fill(p.rgba);
            rect(p.xPos, p.yPos, 0.1f,0.1f);
        }*/
    }

    public void drawGround() {
        time++;

        //for speed:
        //strokeCap(SQUARE);
        //strokeJoin(PROJECT);

        noStroke();
        //Hauto h=cells;

        float W = width;
        float H = height;

        pushMatrix();

        float gw = hnav.WorldToScreenX(cellSize) - hnav.WorldToScreenX(0);

        for (int j = 0; j < cells.h; j++) {

            float y = j * cellSize;

            float sy = hnav.WorldToScreenY(y);
            if ((sy >= -gw) && (sy <= (H + gw))) {

                pushMatrix();

                float tx = 0; //batched x translation

                for (int i = 0; i < cells.w; i++) {

                    float x = i * cellSize;

                    float sx = hnav.WorldToScreenX(x);
                    if ((sx >= -gw) && (sx <= W + gw)) {

                        if (tx > 0) {
                            translate(tx, 0);
                            tx = 0;
                        }

                        Cell c = cells.readCells[i][j];

                        c.draw(this, j == 0 || i == 0 || i == cells.w - 1 || j == cells.w - 1,
                                hnav.MouseToWorldCoordX(width / 2), hnav.MouseToWorldCoordY(height / 2),
                                x, y, hnav.zoom);


                    }

                    tx += cellSize;

                }

                popMatrix();
            }

            translate(0, cellSize);


        }
        popMatrix();

    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    class Hnav {

        private float savepx = 0;
        private float savepy = 0;
        private int selID = 0;
        private float zoom = 43.0f;
        private float difx = -750;
        private float dify = -1300;
        private int lastscr = 0;
        private final boolean EnableZooming = true;
        private final float scrollcamspeed = 1.1f;

        float MouseToWorldCoordX(int x) {
            return (x - difx - width/2f) / zoom;
        }

        float MouseToWorldCoordY(int y) { return (y - dify - height/2f) / zoom;        }

        float WorldToScreenX(float x) {
            return (x*zoom) + difx + width/2f;
        }

        float WorldToScreenY(float y) {
            return (y*zoom) + dify + height/2f;
        }

        private boolean md = false;

        void mousePressed() {
            md = true;
            if (mouseButton == RIGHT) {
                savepx = mouseX;
                savepy = mouseY;
            }
            drawn = false;
        }

        void mouseReleased() {
            md = false;
        }

        void mouseDragged() {
            if (mouseButton == RIGHT) {
                difx += (mouseX - savepx);
                dify += (mouseY - savepy);
                savepx = mouseX;
                savepy = mouseY;
            }
            drawn = false;
        }

        private final float camspeed = 0.25f;
        private final float scrollcammult = 0.92f;
        final boolean keyToo = true;

        void keyPressed() {
            if ((keyToo && key == 'w') || keyCode == UP) {
                dify += (camspeed);
            }
            if ((keyToo && key == 's') || keyCode == DOWN) {
                dify += (-camspeed);
            }
            if ((keyToo && key == 'a') || keyCode == LEFT) {
                difx += (camspeed);
            }
            if ((keyToo && key == 'd') || keyCode == RIGHT) {
                difx += (-camspeed);
            }
            if (!EnableZooming) {
                return;
            }
            if (key == '-' || key == '#') {
                float zoomBefore = zoom;
                zoom *= scrollcammult;
                difx = (difx) * (zoom / zoomBefore);
                dify = (dify) * (zoom / zoomBefore);
            }
            if (key == '+') {
                float zoomBefore = zoom;
                zoom /= scrollcammult;
                difx = (difx) * (zoom / zoomBefore);
                dify = (dify) * (zoom / zoomBefore);
            }
            drawn = false;
        }

        void Init() {
            difx = -width / 2;
            dify = -height / 2;
        }

        void mouseScrolled() {
            if (!EnableZooming) {
                return;
            }
            float zoomBefore = zoom;
            if (mouseScroll > 0) {
                zoom *= scrollcamspeed;
            } else {
                zoom /= scrollcamspeed;
            }
            difx = (difx) * (zoom / zoomBefore);
            dify = (dify) * (zoom / zoomBefore);
            drawn = false;
        }

        void Transform() {
            translate(difx + 0.5f * width, dify + 0.5f * height);
            scale(zoom, zoom);
        }
    }

    ////Object management - dragging etc.
    class Hsim {

        //ArrayList obj = new ArrayList();

        void Init() {
            smooth();
        }

        void mousePressed() {
            if (mouseButton == LEFT) {
                checkSelect();
                float x = hnav.MouseToWorldCoordX(mouseX);
                float y = hnav.MouseToWorldCoordY(mouseY);
                automataclicked(x, y);
            }
        }

        boolean dragged = false;

        void mouseDragged() {
            if (mouseButton == LEFT) {
                dragged = true;
                dragElems();
            }
            mousePressed();
        }

        void mouseReleased() {
            dragged = false;
            //selected = null;
        }

        void dragElems() {
            /*
            if (dragged && selected != null) {
            selected.x = hnav.MouseToWorldCoordX(mouseX);
            selected.y = hnav.MouseToWorldCoordY(mouseY);
            hsim_ElemDragged(selected);
            }
             */
        }

        void checkSelect() {
            /*
            double selection_distanceSq = selection_distance*selection_distance;
            if (selected == null) {
            for (int i = 0; i < obj.size(); i++) {
            Vertex oi = (Vertex) obj.get(i);
            float dx = oi.x - hnav.MouseToWorldCoordX(mouseX);
            float dy = oi.y - hnav.MouseToWorldCoordY(mouseY);
            float distanceSq = (dx * dx + dy * dy);
            if (distanceSq < (selection_distanceSq)) {
            selected = oi;
            hsim_ElemClicked(oi);
            return;
            }
            }
            }
             */
        }
    }

    //Hamlib handlers
    class Hamlib {

        void Init() {
            noStroke();
            hnav.Init();
            hsim.Init();
        }

        void mousePressed() {
            hnav.mousePressed();
            hsim.mousePressed();
        }

        void mouseDragged() {
            hnav.mouseDragged();
            hsim.mouseDragged();
        }

        void mouseReleased() {
            hnav.mouseReleased();
            hsim.mouseReleased();
        }

        public void mouseMoved() {
        }

        void keyPressed() {
            hnav.keyPressed();
        }

        void mouseScrolled() {
            hnav.mouseScrolled();
        }

        void Camera() {
        }
    }

    static List<PVector> Reconstruct_Taken_Path(Map<PVector, PVector> parent, PVector start, PVector target) {
        List<PVector> path = new ArrayList<>();
        path.add(target);
        while (!path.get(path.size() - 1).equals(start)) {
            //since bfs found a solution, start is included for sure and thus this loop terminates for sure
            path.add(parent.get(path.get(path.size() - 1)));
        }
        Collections.reverse(path);
        return path;
    }

    public static List<PVector> Shortest_Path(Grid2DSpace s, GridAgent a, PVector start, PVector target) {
        Set<PVector> avoid = new HashSet<>();
        Map<PVector, PVector> parent = new HashMap<>();
        ArrayDeque<PVector> queue = new ArrayDeque<>();
        queue.add(start);


        while (!queue.isEmpty()) {
            PVector active = queue.removeFirst();

            if (avoid.contains(active))
                continue;

            avoid.add(active);


            if (active.equals(target)) {
                return Reconstruct_Taken_Path(parent, start, target);
            }

            for (int i = 0; i < 4; i++) {
                PVector x = new PVector();
                switch (i) {
                    case 0:
                        x.set(active.x + 1, active.y);
                        break;
                    case 1:
                        x.set(active.x - 1, active.y);
                        break;
                    case 2:
                        x.set(active.x, active.y + 1);
                        break;
                    case 3:
                        x.set(active.x, active.y - 1);
                        break;
                }


                if (avoid.contains(x) ||
                        s.whyNonTraversible(a, (int) active.x, (int) active.y, (int) x.x, (int) x.y) != null)
                    continue;

                parent.put(x, active);
                queue.add(x);
            }
        }
        return null;
    }

}
