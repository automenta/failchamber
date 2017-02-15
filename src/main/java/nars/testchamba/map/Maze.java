package nars.testchamba.map;

import nars.testchamba.Space;
import nars.testchamba.object.Geometric;
import nars.testchamba.state.Cell;
import nars.testchamba.state.Hauto;
import nars.testchamba.state.Hauto.SetMaterial;

import static nars.testchamba.state.Cell.Material.StoneWall;
import static nars.testchamba.state.Hauto.irand;

/**
 * @author Tyrant
 */
public class Maze {


    public static void buildMaze(Space m, int x1, int y1, int x2, int y2) {
        m.cells.forEach(x1, y1, x2, y2, new SetMaterial(StoneWall));
        buildInnerMaze(m, x1 + 1, y1 + 1, x2 - 1, y2 - 1);
        m.cells.copyReadToWrite();
    }

    public static void buildInnerMaze(Space s, int x1, int y1, int x2, int y2) {

        Hauto m = s.cells;

        m.forEach(x1, y1, x2, y2, new SetMaterial(StoneWall));

        int w = x2 - x1 + 1;
        int rw = (w + 1) / 2;
        int h = y2 - y1 + 1;
        int rh = (h + 1) / 2;

        int sx = x1 + 2 * irand(rw);
        int sy = y1 + 2 * irand(rh);
        m.at(sx, sy, new SetMaterial(Cell.Material.DirtFloor));

        int finishedCount = 0;
        for (int i = 1; (i < (rw * rh * 1000)) && (finishedCount < (rw * rh)); i++) {
            int x = x1 + 2 * irand(rw);
            int y = y1 + 2 * irand(rh);
            if (m.at(x, y).material != StoneWall)
                continue;

            int dx = (irand(2) == 1) ? (irand(2) * 2 - 1) : 0;
            int dy = (dx == 0) ? (irand(2) * 2 - 1) : 0;
            int lx = x + dx * 2;
            int ly = y + dy * 2;
            if ((lx >= x1) && (lx <= x2) && (ly >= y1) && (ly <= y2)) {
                if (m.at(lx, ly).material != StoneWall) {
                    m.at(x, y, new SetMaterial(Cell.Material.DirtFloor));
                    m.at(x + dx, y + dy, new SetMaterial(Cell.Material.DirtFloor));
                    m.read[x][y].setHeight((int) (Math.random() * 24 + 1));
                    m.write[x][y].setHeight((int) (Math.random() * 24 + 1));
                    finishedCount++;
                }
            }
        }

        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                if (m.at(x, y).material==StoneWall) {
                    s.add(new Geometric.Rectangle(x, y, 0.9f, 0.9f, 10.0));
                }
            }
        }
    }
}