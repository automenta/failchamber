package nars.lab.grid2d.main;




public class CellState {
    public float light;
    public int x, y;
    boolean is_solid;
    
    //display color
    transient float cr, cg, cb, ca;
    

    public CellState(int x, int y) {
        this.x = x;
        this.y = y;

        this.light = 0;
    }
    
}
