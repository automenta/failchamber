package nars.net;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.net.SocketAddress;
import java.net.SocketException;
import java.util.function.Consumer;

/**
 * javascript UDP context
 */
public abstract class SessionUDP<S extends Consumer<byte[]>> extends ObjectUDP {

    final static int MAX_SESSIONS = 32;

    final Cache<SocketAddress,S> sessions = Caffeine.newBuilder().maximumSize(MAX_SESSIONS).build();

    public SessionUDP(int port) throws SocketException {
        super(port);
    }


    @Override
    protected void in(byte[] data, SocketAddress from) {
        sessions.get(from, this::get).accept(data);
    }

    abstract protected S get(SocketAddress socketAddress);

}
