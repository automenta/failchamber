package nars.testchamba.grid;


public class CellState {
    public float light;
    public final int x;
    public final int y;
    boolean is_solid;

    //display color
    transient float cr, cg, cb, ca;


    public CellState(int x, int y) {
        this.x = x;
        this.y = y;

        this.light = 0;
    }

}
