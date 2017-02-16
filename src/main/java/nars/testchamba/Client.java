package nars.testchamba;

import com.codeforces.commons.geometry.Vector2D;

import nars.net.JsServer;
import nars.testchamba.agent.PacManAgent;

import java.io.Closeable;

/**
 * client API (accessible thru JsServer)
 */
public class Client implements Closeable {

    private final Space space;
    private final PacManAgent agent;

    Client(Space s) {
        this.space = s;

        this.agent = new PacManAgent(1f, 0, 0);
        s.add(agent);
    }

    public Vector2D pos() {
        return new Vector2D(0,0); //TODO
    }

    @Override
    public void close() {
        space.remove(agent);
    }

}
