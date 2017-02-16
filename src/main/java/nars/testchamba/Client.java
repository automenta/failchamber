package nars.testchamba;

import com.codeforces.commons.geometry.Vector2D;

/**
 * client API (accessible thru JsServer)
 */
public class Client {

    private final Space space;

    Client(Space s) {
        this.space = s;
    }

    public Vector2D pos() {
        return new Vector2D(0,0); //TODO
    }

}
