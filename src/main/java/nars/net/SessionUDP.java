package nars.net;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * javascript UDP context
 */
public abstract class SessionUDP<S extends Consumer<byte[]>> extends ObjectUDP implements RemovalListener<SocketAddress,S> {

    final static int MAX_SESSIONS = 32;

    final Cache<SocketAddress,S> sessions;

    public SessionUDP(int port) throws SocketException {
        super(port);
        sessions = Caffeine.newBuilder().maximumSize(MAX_SESSIONS).removalListener(this).build();
    }


    @Override
    public void onRemoval(@Nullable SocketAddress key, @Nullable S value, @Nonnull RemovalCause cause) {

    }

    /** explicitly disconnect a client */
    public boolean end(S s) {
        //HACK
        Set<Map.Entry<SocketAddress, S>> e = sessions.asMap().entrySet();
        for (Map.Entry<SocketAddress, S> ee : e) {
            if (ee.getValue() == s) {
                sessions.invalidate(ee.getKey());
                return true;
            }
        }
        return false;
    }

    @Override
    protected void in(byte[] data, SocketAddress from) {
        sessions.get(from, this::get).accept(data);
    }

    abstract protected S get(SocketAddress socketAddress);

}
