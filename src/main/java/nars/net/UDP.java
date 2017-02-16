package nars.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;

/**
 * Created by me on 2/15/17.
 */
public abstract class UDP implements Runnable {

    /** in bytes */
    static final int MAX_PACKET_SIZE = 4096;

    private final DatagramSocket in;
    public final Thread thread;
    private boolean running = true;
    private static final Logger logger = LoggerFactory.getLogger(UDP.class);

    //final BidiMap<UUID,IntObjectPair<InetAddress>> who = new DualHashBidiMap<>();

    public UDP(int port) throws SocketException {
        in = new DatagramSocket(port);

        this.thread = new Thread(this);

        logger.info("{} start on port", this, port);

        thread.start();
    }

    @Override
    public void run() {
        byte[] receiveData = new byte[MAX_PACKET_SIZE];
        while (running) {
            DatagramPacket p = new DatagramPacket(receiveData, receiveData.length);
            try {
                in.receive(p);
                in(Arrays.copyOfRange(p.getData(), p.getOffset(), p.getLength()), p.getSocketAddress());
            } catch (IOException e) {
                logger.error("{}",e);
            }
        }
    }

    public void stop() {
        running = false;
        thread.stop();
    }

    public boolean out(String data, String host, int port) throws UnknownHostException {
        return out(data.getBytes(), host, port);
    }


    public boolean out(String data, int port) {
        return out(data.getBytes(), port);
    }

    public boolean out(byte[] data, int port) {
        return out(data, new InetSocketAddress(port) );
    }


    public boolean out(byte[] data, String host, int port) throws UnknownHostException {
        return out(data, new InetSocketAddress(InetAddress.getByName(host), port) );
    }


    public boolean out(byte[] data, SocketAddress to) {
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, to);
        try {
            in.send(sendPacket);
            return true;
        } catch (IOException e) {
            logger.error("{}", e);
            return false;
        }
    }

    abstract protected void in(byte[] data, SocketAddress from);

//    static class UDPClient {
//        public static void main(String args[]) throws Exception {
//            BufferedReader inFromUser =
//                    new BufferedReader(new InputStreamReader(System.in));
//            DatagramSocket clientSocket = new DatagramSocket();
//            InetAddress IPAddress = InetAddress.getByName("localhost");
//            byte[] sendData = new byte[1024];
//            byte[] receiveData = new byte[1024];
//            String sentence = inFromUser.readLine();
//            sendData = sentence.getBytes();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
//            clientSocket.send(sendPacket);
//            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//            clientSocket.receive(receivePacket);
//            String modifiedSentence = new String(receivePacket.getData());
//            System.out.println("FROM SERVER:" + modifiedSentence);
//            clientSocket.close();
//        }
//    }
}
