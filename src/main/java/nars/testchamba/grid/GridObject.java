package nars.testchamba.grid;

import nars.testchamba.View;

/**
 * @author me
 */


public interface GridObject {

    void init(View space);

    void update(Effect nextEffect);

    void draw();

}
