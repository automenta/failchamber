package nars.net;

import org.msgpack.core.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;

/** https://github.com/msgpack/msgpack-java */
abstract public class ObjectUDP extends UDP {

    private static final Logger logger = LoggerFactory.getLogger(ObjectUDP.class);


    public ObjectUDP(int port) throws SocketException {
        super(port);
    }

//
//    public boolean out(Object x, String host, int port)  {
//        try {
//            return out(toBytes(x), host, port);
//        } catch (IOException e) {
//            logger.error("{}", e);
//            return false;
//        }
//    }

//    protected byte[] toBytes(Object x) throws IOException {
//        return msgpack.write(x);
//    }
//
//    protected <X> byte[] toBytes(X x, Template<X> t) throws IOException {
//        return msgpack.write(x, t);
//    }


    protected String stringFromBytes(byte[] x) {
        try {
            return MessagePack.newDefaultUnpacker(x).unpackString();
        } catch (IOException e) {
            logger.error("{}", e);
            return null;
        }

        //Templates.tList(Templates.TString)
//        System.out.println(dst1.get(0));
//        System.out.println(dst1.get(1));
//        System.out.println(dst1.get(2));
//
//// Or, Deserialze to Value then convert type.
//        Value dynamic = msgpack.read(raw);
//        List<String> dst2 = new Converter(dynamic)
//                .read(Templates.tList(Templates.TString));
//        System.out.println(dst2.get(0));
//        System.out.println(dst2.get(1));
//        System.out.println(dst2.get(2));

    }

}
