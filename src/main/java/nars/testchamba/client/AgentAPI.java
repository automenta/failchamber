package nars.testchamba.client;

import nars.testchamba.object.VisionRayAnimation;
import notreal.collision.CollisionInfo;
import nars.testchamba.Space;
import nars.testchamba.object.Pacman;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;

import static nars.testchamba.Space.ff;

/**
 * client API (accessible thru JsServer)
 */
public class AgentAPI implements Closeable {

    private final Space space;
    private final Pacman agent;
    private final SocketAddress addr;

    public AgentAPI(SocketAddress a, Space s) {
        this.addr = a;
        this.space = s;

        this.agent = new Pacman(1f);
        agent.pos(s.whereSpawns());
        s.add(agent);
    }

    public float[] pos() { return ff(agent.pos()); }

    public float[] vel() { return ff(agent.vel()); }

    public void force(float x, float y) { agent.forceLocal(x, y);     }

    public void torque(float t) { agent.torque(t);     }


    public CollisionInfo[] see(double maxDistance, double[] angles) {
        return DoubleStream.of(angles).mapToObj( a-> {
            double ang = agent.angle() + a;

            List<CollisionInfo> c = space.collisions(agent.pos(), ang, maxDistance, agent);

            CollisionInfo result;

            if (!c.isEmpty()) {
                if (c.size() > 1) {
                    double X = agent.x();
                    double Y = agent.y();

                    //sort by nearest
                    c.sort((e, f) -> {
                        double aDist = e.getPoint().getSquaredDistanceTo(X, Y);
                        double bDist = f.getPoint().getSquaredDistanceTo(X, Y);
                        return Double.compare(aDist, bDist);
                    });
                }

                //System.out.println(agent + "\n" + Joiner.on('\n').join(c) + "\n");

                result = c.get(0);
            } else {
                result = null;
            }

            agent.animate(new VisionRayAnimation(agent, (float)a, result, (float)maxDistance));

            return result;
        }).filter(Objects::nonNull).toArray(CollisionInfo[]::new);
    }

    @Override
    public void close() {
        space.remove(agent);
    }

}
