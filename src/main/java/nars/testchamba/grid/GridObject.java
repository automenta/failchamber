package nars.testchamba.grid;

/**
 * @author me
 */


public interface GridObject {

    void init(Grid2DSpace space);

    void update(Effect nextEffect);

    void draw();

}
