package nars.testchamba.client;

import notreal.collision.CollisionInfo;
import nars.testchamba.Space;
import nars.testchamba.View;
import nars.testchamba.agent.PacManAgent;
import nars.testchamba.util.Animation;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.List;

import static nars.testchamba.Space.f;

/**
 * client API (accessible thru JsServer)
 */
public class AgentAPI implements Closeable {

    private final Space space;
    private final PacManAgent agent;
    private final SocketAddress addr;

    public AgentAPI(SocketAddress a, Space s) {
        this.addr = a;
        this.space = s;

        this.agent = new PacManAgent(1f);
        agent.pos(s.spawnPoint());
        s.add(agent);
    }

    public float[] pos() { return f(agent.pos()); }
    public float[] vel() { return f(agent.vel()); }
    public void force(float x, float y) { agent.forceLocal(x, y);     }
    public void torque(float t) { agent.torque(t);     }

//    public static class VisionRay {
//        public final String seen;
//        public final float distance;
//
//        public VisionRay(String seen, float distance) {
//            this.seen = seen;
//            this.distance = distance;
//        }
//
//        @Override
//        public String toString() {
//            return "VisionRay{" +
//                    "seen='" + seen + '\'' +
//                    ", distance=" + distance +
//                    '}';
//        }
//    }

    @Nullable
    public CollisionInfo see(float angleRelativeToForward, float maxDistance) {
        double ang = agent.angle() + angleRelativeToForward;

        List<CollisionInfo> c = space.collisions(agent.pos(), ang, maxDistance, agent);

        CollisionInfo result;

        if (!c.isEmpty()) {
            if (c.size() > 1) {
                double X = agent.x();
                double Y = agent.y();

                //sort by nearest
                c.sort((a, b) -> {
                    double aDist = a.getPoint().getSquaredDistanceTo(X, Y);
                    double bDist = b.getPoint().getSquaredDistanceTo(X, Y);
                    return Double.compare(aDist, bDist);
                });
            }

            //System.out.println(agent + "\n" + Joiner.on('\n').join(c) + "\n");

            result = c.get(0);
        } else {
            result = null;
        }

        agent.animate(new Animation() {

            float opacity = 1f;
            double ang = angleRelativeToForward;
            double dist = result!=null ? result.getPoint().getDistanceTo(agent.pos()) : maxDistance;
            double dx = Math.cos(ang) * dist;
            double dy = Math.sin(ang) * dist;

            float r = result!=null ? 0f : 200f;
            float g = result!=null ? 200f : 0f;

            @Override
            public boolean draw(View v, double rt) {

                v.stroke(r, g, 10, 180 * opacity);
                opacity -= 0.1f;

                v.strokeWeight(0.1f);

                v.line( 0, 0, (float)dx, (float)dy );
                v.noStroke();

                return (opacity > 0);
            }
        });

        return result;
    }

    @Override
    public void close() {
        space.remove(agent);
    }

}
