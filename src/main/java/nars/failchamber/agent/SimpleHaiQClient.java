package nars.failchamber.agent;

import com.google.gson.JsonSyntaxException;
import jcog.Util;
import jcog.learn.ql.HaiQAgent;
import nars.failchamber.client.AgentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

public class SimpleHaiQClient extends AgentClient {

    private static final Logger logger = LoggerFactory.getLogger(SimpleHaiQClient.class);

    private final HaiQAgent hai;


    float reward;
    private final float[] input;

    long updatePeriodMS = 50;
    double SEE_DIST = 25.0;


    int retinas = 11;
    int actions = 6; //LRUD
    private short whatBits = 6;

    private float rewardDecay = 0.9f;

    public SimpleHaiQClient(int port, String remoteHost, int remotePort) throws SocketException, UnknownHostException {
        super(port, remoteHost, remotePort);

        int inputs = retinas * (1 + whatBits);

        this.input = new float[inputs];

        hai = new jcog.learn.ql.HaiQAgent();
        hai.setQ(0.05F, 0.5F, 0.9F, 0.05F);

        hai.start(inputs, actions);
    }


    @Override
    protected synchronized void in(byte[] data, InetSocketAddress from) {

        String s = new String(data);
        final int[] i = {0};
        try {
            Object[] m = this.json.fromJson(s, Object[].class);

            switch (m[0].toString()) {
//                case "reward":
//                    reward = Float.parseFloat(m[1].toString());
//                    break;

                case "eat":
                    System.out.println("EAT");
                    reward += 1f;
                    break;

                case "see":
                    Map<String, String> seen = (Map) m[1];
                    seen.forEach((angle, whatDistStr) -> {
                        float a = Float.parseFloat(angle);

                        int retina = i[0]++;
                        int stride = 1 + whatBits;
                        int ii = retina * stride;

                        if (whatDistStr.isEmpty()) {

                            Arrays.fill(input, ii, ii + stride, 0f);

                        } else {
                            String[] whatDist = whatDistStr.split("=");
                            int what = (int) (Util.hashELF(whatDist[0]) & ((1 << whatBits) - 1));

                            float dist = Float.parseFloat(whatDist[1]);

                            input[ii] = (float) (1f - (dist / SEE_DIST));
                            for (int b = 0; b < whatBits; b++) {
                                input[1 + ii + b] = (what & (1 << b)) > 0 ? 1f : 0f;
                            }
                        }
                    });
                    break;
            }

        } catch (JsonSyntaxException e) {
            logger.error("unparsed: {}", s);
        }
    }

    @Override
    public void run() {
        while (true) {

            double lookAngle = 0;
            double angleSep = 0.15;

            int retinasHalf = retinas / 2;
            if ((retinasHalf * 2 + 1 /* 0 */) != retinas) {
                throw new RuntimeException("invalid # retinas");
            }

            see(SEE_DIST, IntStream.range(-retinasHalf, retinasHalf+1).mapToDouble(i -> lookAngle + i * angleSep).toArray());

            Util.pause(updatePeriodMS);

            int a = hai.act(reward, input);

            reward *= rewardDecay;

            //System.out.println(Arrays.toString(input) + " ==> " + a);

            switch (a) {
                case 0:
                    //DO NOTHING
                    break;
                case 1:
                    torque(+12);
                    break;
                case 2:
                    torque(-12);
                    break;
                case 3:
                    force(+60, 0);
                    break;
                case 4:
                    force(-60, 0);
                    break;

                case 5:
                    fire();
                    break;
            }

        }
    }


    public static void main(String[] args) throws SocketException, UnknownHostException {
        new SimpleHaiQClient(16661, "localhost", 10000);
    }
}
