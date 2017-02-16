package nars.testchamba;

import nars.testchamba.agent.PacManAgent;
import nars.testchamba.particle.Particle;
import nars.testchamba.state.Cell;
import nars.testchamba.state.Hauto;
import nars.testchamba.state.ParticleSystem;
import nars.testchamba.util.NWindow;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;


public class View extends PApplet {

    public final Space space;

    public final PVector target = new PVector(25, 25); //need to be init equal else feedback will
    public final PVector current = new PVector(0, 0);

    private Menu editor;

    final Hnav hnav = new Hnav();
    final Hsim hsim = new Hsim();
    final Hamlib hamlib = new Hamlib();

    final float cellSize = 1;

    public int time = 0;

    int mouseScroll = 0;

    //float selection_distance = 10;
    float ambientLight = 0.5f;
    boolean drawn = false;
    double realtime;
    private Container contentPane;

    public View(Space space) {
        super();

        initSurface();

        this.space = space;
    }

//    public void start() {
//        startSurface();
//
//        textAlign(CENTER);
//        //textFont(new PFont(new Font(Font.MONOSPACED, Font.BOLD, 12), true));
//
//    }


    public String what(int x, int y) {
        return space.what(x, y);
    }

    static List<PVector> pathTaken(Map<PVector, PVector> parent, PVector start, PVector target) {
        List<PVector> path = new ArrayList<>();
        path.add(target);
        while (!path.get(path.size() - 1).equals(start)) {
            //since bfs found a solution, start is included for sure and thus this loop terminates for sure
            path.add(parent.get(path.get(path.size() - 1)));
        }
        Collections.reverse(path);
        return path;
    }

    public static List<PVector> pathShortest(View s, PacManAgent a, PVector start, PVector target) {
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
                return pathTaken(parent, start, target);
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


//    @Override
//    protected void resizeRenderer(int newWidth, int newHeight) {
//        super.resizeRenderer(newWidth, newHeight); //To change body of generated methods, choose Tools | Templates.
//        drawn = false;
//    }


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


        j.setIgnoreRepaint(true);
        contentPane.setIgnoreRepaint(true);

        //JPanel menu = new JPanel(new FlowLayout(FlowLayout.LEFT));


        /*final JCheckBox syntaxEnable = new JCheckBox("Syntax");
        syntaxEnable.addActionListener(new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
        }
        });
        menu.add(syntaxEnable);
         */

        editor = new Menu(this);
//        NWindow editorWindow = new NWindow("Edit", editor);
//        editorWindow.setSize(200, 400);
//        editorWindow.setVisible(true);


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
        component().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    editor.popup(component(), e.getPoint());
                }
            }
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
        if (mouseActive())
            hamlib.mouseReleased();
    }

    @Override
    public void mouseDragged() {
        if (mouseActive())
            hamlib.mouseDragged();
    }

    @Override
    public void mousePressed() {
        if (mouseActive())
            hamlib.mousePressed();
    }

    private boolean mouseActive() {
        return !editor.isVisible();
    }

    public void automataclicked(float x, float y) {
        if (x < 0 || y < 0) {
            return;
        }
        float realx = x / cellSize;
        float realy = y / cellSize;

        Hauto cells = space.cells;
        if (realx >= cells.w || realy >= cells.h) {
            return;
        }
        cells.clicked((int) realx, (int) realy, this);
    }

    public void update(double dt /* seconds */) {
        space.update(this, dt);
    }

    @Override
    public void draw() {

        strokeCap(PConstants.MITER);

        {
            fade();
        }

        pushMatrix();
        {
            hnav.Transform();

            drawGround();
            drawObjects();
            drawParticles();
        }
        popMatrix();

        {
            drawHUD();
        }
    }

    private void fade() {
        //background(50f, 0.1f);
        fill(0, 25f);
        rect(0, 0, width, height);
    }

    private void drawHUD() {
        HUD.drawCursorCrossHair(this, mouseX, mouseY, space.cells.label);
    }


    @Override
    public void setup() {

    }


//    enum MotionEffect {
//        Moved, PainfullyMoved, TooHigh, TooSolid /* collision, impenetrable, bump */, Stuck /* flypaper, quicksand */, TooFar
//    }

    public int getTime() {
        return time;
    }

    public double getRealtime() {
        return realtime;
    }

    public String whyNonTraversible(PacManAgent agent, int x, int y, int tx, int ty) {

        Hauto cells = space.cells;

        int dx = Math.abs(tx - x);
        int dy = Math.abs(ty - y);

        if (!((dx <= 1) && (dy <= 1)))
            return "Too far";

        if ((tx < 0) || (ty < 0) || (tx >= cells.w) || (ty >= cells.h))
            return "Out of bounds: " + tx + ' ' + ty;

        Cell to = cells.at(tx, ty);

        //System.out.println(to + " " + to.material);
        if ((to.material == Cell.Material.StoneWall) || to.solid || to.material == Cell.Material.Water || to.logic == Cell.Logic.BRIDGE || to.logic == Cell.Logic.UNCERTAINBRIDGE)
            return "Too solid";

        final float maxTraversableHeight = 8;

        Cell from = cells.at(x, y);
        float dHeight = to.height - from.height;
        //if (dHeight > maxTraversableHeight)
        //    return "Too high";

        return null;
    }

    public void drawObjects() {
        pushMatrix();

        //shift half a cell down and right so that when an object draws, it's centerd in the middle of a cell.
        translate(cellSize / 4f, cellSize / 4f);

//        float tx = hnav.WorldToScreenX(0);
//        float ty = hnav.WorldToScreenY(0);
//        float sx = (hnav.WorldToScreenX(1) - tx)/2f;


        space.draw(this);

        popMatrix();
    }

    public void drawParticles() {
        //PImage b = particles.particleImage;
        //this.blend(b, 0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), PImage.ADD);

        ParticleSystem particles = space.particles;

        particles.tick();
        for (Particle p : particles.p) {
            fill(p.rgba);
            rect(p.xPos, p.yPos, 0.1f, 0.1f);
        }
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

        Hauto cells = space.cells;

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

                        Cell c = cells.read[i][j];

                        c.draw(this, j == 0 || i == 0 || i == cells.w - 1 || j == cells.w - 1,
                                hnav.MouseToWorldCoordX(width / 2), hnav.MouseToWorldCoordY(height / 2),
                                x, y, hnav.zoom, ambientLight);


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

        final boolean keyToo = true;
        private final boolean EnableZooming = true;
        private final float scrollcamspeed = 1.1f;
        private final float camspeed = 0.25f;
        private final float scrollcammult = 0.92f;
        private float savepx = 0;
        private float savepy = 0;
        private int selID = 0;
        private float zoom = 43.0f;
        private float difx = -750;
        private float dify = -1300;
        private int lastscr = 0;
        private boolean md = false;

        float MouseToWorldCoordX(int x) {
            return (x - difx - width / 2f) / zoom;
        }

        float MouseToWorldCoordY(int y) {
            return (y - dify - height / 2f) / zoom;
        }

        float WorldToScreenX(float x) {
            return (x * zoom) + difx + width / 2f;
        }

        float WorldToScreenY(float y) {
            return (y * zoom) + dify + height / 2f;
        }

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

        boolean dragged = false;

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

}
