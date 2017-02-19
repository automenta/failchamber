package nars.failchamber.client;

import jcog.net.UDP;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by me on 2/15/17.
 */
public class AgentClient extends UDP implements Runnable {

    private final String remoteHost;
    private final int remotePort;


    public AgentClient(int localPort, String remoteHost, int remotePort) throws SocketException, UnknownHostException {
        super(localPort);
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }


    public void force(float x, float y) {
        out("i.force(" + x + "," + y + ")");
    }

    public void torque(float t) {
        out("i.torque(" + t + ")");
    }

    public void see(double distance, double... angles) {
        out("i.see(" + distance + ',' + Arrays.toString(angles) + ")" );
    }

    public void fire() {
        out("i.fire()");
    }

    public void out(String s) {
        out(s, remoteHost, remotePort);
    }

    @Override
    protected final void onStart() {
        new Thread(this).start();
    }

    @Override
    protected void in(byte[] data, InetSocketAddress from) {

        //System.out.println(from + ": " + new String(data));
    }

    @Override
    public void run() {

    }

}
