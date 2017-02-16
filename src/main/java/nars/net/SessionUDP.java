package nars.net;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * javascript UDP context
 */
public abstract class SessionUDP<S extends Consumer<byte[]>> extends UDP implements RemovalListener<SocketAddress,SessionUDP.Session<S>> {

    final static int MAX_SESSIONS = 32;

    static class Session<A> {
        final A api;
        private long last;

        Session(A api) {
            this.api = api;
            touch();
        }

        public long stale() {
            return stale(System.currentTimeMillis());
        }

        /** in ms */
        public long stale(long now) {
            return now - last;
        }

        public void touch() { this.last = System.currentTimeMillis(); }
    }

    final Cache<SocketAddress,Session<S>> sessions;

    public SessionUDP(int port) throws SocketException {
        super(port);
        sessions = Caffeine.newBuilder().maximumSize(MAX_SESSIONS).removalListener(this).build();
    }


    @Override
    public void onRemoval(@Nullable SocketAddress key, @Nullable Session<S> value, @Nonnull RemovalCause cause) {
        end(value.api, false);
    }

    /** explicitly disconnect a client */
    public void end(S s, boolean removeFromCache) {

        if (s instanceof Closeable) {
            try {
                ((Closeable) s).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (removeFromCache) {
            //HACK
            Set<Map.Entry<SocketAddress, Session<S>>> e = sessions.asMap().entrySet();
            for (Map.Entry<SocketAddress, Session<S>> ee : e) {
                if (ee.getValue() == s) {
                    sessions.invalidate(ee.getKey());
                }
            }
        }
    }

    @Override
    protected void in(byte[] data, SocketAddress from) {
        Session<S> ss = sessions.get(from, this::session);
        ss.touch();
        ss.api.accept(data);
    }

    abstract protected S get(SocketAddress socketAddress);

    protected Session<S> session(SocketAddress a) {
        return new Session<>(get(a));
    }

}
