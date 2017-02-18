package nars.failchamber.state;


public class CellState {

    public final int x;
    public final int y;
    public float light;
    //boolean is_solid;
    /**
     * display color
     */
    transient float cr, cg, cb, ca;

    //disintegration, toxicity, heat, charge, wind pressure etc


    public CellState(int x, int y) {
        this.x = x;
        this.y = y;

        this.light = 0;
    }

}
