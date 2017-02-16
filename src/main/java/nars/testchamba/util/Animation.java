package nars.testchamba.util;

import nars.testchamba.View;


@FunctionalInterface public interface Animation {

    /** return true to continue another call in the next frame, or false to destroy this animation
     * @param rt absolute realtime time in nanosec
     * */
    boolean draw(View v, double rt);

}
