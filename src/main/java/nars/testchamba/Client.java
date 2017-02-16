package nars.testchamba;

import com.codeforces.commons.geometry.Point2D;

import nars.testchamba.agent.PacManAgent;

import java.io.Closeable;
import java.net.SocketAddress;

import static nars.testchamba.Space.f;

/**
 * client API (accessible thru JsServer)
 */
public class Client implements Closeable {

    private final Space space;
    private final PacManAgent agent;
    private final SocketAddress addr;

    Client(SocketAddress a, Space s) {
        this.addr = a;
        this.space = s;

        this.agent = new PacManAgent(1f, 0, 0);
        s.add(agent);
    }

    public float[] pos() { return f(agent.pos()); }
    public float[] vel() { return f(agent.vel()); }
    public void force(float x, float y) { agent.forceLocal(x, y);     }
    public void torque(float t) { agent.torque(t);     }


    @Override
    public void close() {
        space.remove(agent);
    }

}
