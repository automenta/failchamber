package nars.lab.grid2d.main;

/**
 *
 * @author me
 */


public interface GridObject {

    void init(Grid2DSpace space);
    
    void update(Effect nextEffect);
    
    void draw();
    
}
