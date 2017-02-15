package nars.testchamba;

import nars.net.ObjectUDP;

import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;


public class Net {



    public static void main(String[] args) throws SocketException, InterruptedException {
        ObjectUDP x = new ObjectUDP(10000) {
            @Override
            protected void in(byte[] data, SocketAddress from) {
                System.out.println(from + ": " + data.length + " bytes: " + strstrMapFromBytes(data));
            }
        };

        ObjectUDP y = new ObjectUDP(10001) {

            @Override
            protected void in(byte[] data, SocketAddress from) {
                System.out.println(from + ": " + data.length + " bytes: " + strstrMapFromBytes(data));
            }
        };

        Map<String,String> m = new HashMap();
        m.put("key", "value");

        y.out(m, "localhost", 10000);

        x.thread.join();
    }
}
