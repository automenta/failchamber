package nars.testchamba;

import nars.net.JsServer;
import nars.net.ObjectUDP;

import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class Net {



    public static void main(String[] args) throws SocketException, InterruptedException {

        JsServer<Runtime> server = new JsServer<Runtime>(10000, Runtime::getRuntime);

        ObjectUDP client = new ObjectUDP(10001) {
            @Override protected void in(byte[] data, SocketAddress from) {
                System.out.println(from + ": " + data.length + " \"" + new String(data) + "\" = bytes:" + Arrays.toString(data) );
            }
        };

        client.out("i.freeMemory()", 10000);

        server.thread.join();
    }
}
