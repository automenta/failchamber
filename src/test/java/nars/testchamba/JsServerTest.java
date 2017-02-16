package nars.testchamba;

import nars.net.JsServer;
import nars.net.UDP;

import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;


public class JsServerTest {



    public static void main(String[] args) throws SocketException, InterruptedException {

        JsServer<Runtime> server = new JsServer<Runtime>(10000, Runtime::getRuntime);

        UDP client = new UDP(10001) {
            @Override protected void in(byte[] data, SocketAddress from) {
                System.out.println(from + ": " + data.length + " \"" + new String(data) + "\" = bytes:" + Arrays.toString(data) );
            }
        };

        client.out("i.freeMemory()", 10000);

        server.thread.join();
    }
}
