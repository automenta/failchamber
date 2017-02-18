package nars.failchamber.client;

import jcog.Texts;
import jcog.net.UDP;
import nars.failchamber.object.Herb;
import nars.failchamber.object.VisionRayAnimation;
import notreal.collision.CollisionInfo;
import nars.failchamber.Space;
import nars.failchamber.object.Pacman;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.stream.DoubleStream;

import static com.google.common.collect.Lists.newArrayList;
import static nars.failchamber.Space.ff;

/**
 * client API (accessible thru JsServer)
 */
public class AgentAPI implements Closeable {

    private final Space space;
    private final Pacman agent;
    private final InetSocketAddress remote;
    private final UDP udp;

    public AgentAPI(UDP u, InetSocketAddress a, Space s) {
        this.udp = u;
        this.remote = a;
        this.space = s;

        this.agent = new Pacman(1f) {
            @Override
            public boolean canEat(Herb.Cannanip what) {
                udp.outBytes(("[\"eat\",\"" + what.getClass().getSimpleName() + "\"]").getBytes(), remote);
                return true;
            }
        };
        agent.pos(s.whereSpawns());
        s.add(agent);
    }


    public float[] pos() { return ff(agent.pos()); }

    public float[] vel() { return ff(agent.vel()); }

    public void force(float x, float y) { agent.forceLocal(x, y);     }

    public void torque(float t) { agent.torque(t);     }



    public Object see(double maxDistance, double[] angles) {

        TreeMap<String,String> m = new TreeMap();

        DoubleStream.of(angles).forEach(a-> {
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

            double dist = result != null ? result.getPoint().getDistanceTo(agent.pos()) : maxDistance;

            agent.animate(new VisionRayAnimation(agent, (float)a, result, (float)dist));

            m.put(Texts.n2((float) a),

                    result!=null ?
                            result.getBodyB().getClass().getSimpleName() + "=" + Texts.n2(dist)
                            :
                            ""
            );
        });

        return new Object[] { "see", m };
    }

    @Override
    public void close() {
        space.remove(agent);
    }

}
