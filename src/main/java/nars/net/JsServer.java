package nars.net;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 *
 * @param <A> API interface class
 */
public class JsServer<A> extends SessionUDP<JsServer<A>.JsSession> {

    private static final Logger logger = LoggerFactory.getLogger(JsServer.class);

    final static Executor exe = ForkJoinPool.commonPool();

    static transient final ScriptEngineManager engineManager = new ScriptEngineManager();
    static transient final NashornScriptEngine JS = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    private final Function<SocketAddress, A> apiBuilder;

    public JsServer(int port, Supplier<A> apiBuilder) throws SocketException {
        this(port, (a) -> apiBuilder.get());
    }

    public JsServer(int port, Function<SocketAddress, A> apiBuilder) throws SocketException {
        super(port);
        this.apiBuilder = apiBuilder;
    }

    @Override
    protected JsSession get(SocketAddress a) {
        return new JsSession(a, apiBuilder.apply(a));
    }


    class JsSession extends SimpleBindings implements Consumer<byte[]> {

        private final A context;
        private final SocketAddress host;

        public JsSession(SocketAddress s, A api) {
            super();
            this.host = s;
            this.context = api;
            put("i", api); //i.
        }

        @Override
        public void accept(byte[] codeByte) {
            String code = new String(codeByte);

            //END signal
            if (code.equals(";")) {
                end(this, true);
                return;
            }

            exe.execute(() -> {
                Object result = eval(code, this, JS);
                //System.out.println(result + " " + result.getClass());
                if (result != null) {

//                    try {
//                        MessageBufferPacker out = MessagePack.newDefaultBufferPacker();
//                        out.packString(result.toString());
//                        out(out.toByteArray(), host);
//                    } catch (IOException e) {
//                        logger.error("{}", e);
//                    }
                    out(result.toString().getBytes(), host);


                }
            });
        }

    }


    static Object eval(String code, SimpleBindings bindings, NashornScriptEngine engine) {
        Object o;
        //long start = System.currentTimeMillis();

        try {
            if (bindings == null)
                o = engine.eval(code);
            else
                o = engine.eval(code, bindings);

        } catch (Throwable t) {
            o = t;
        }

        return o;

//            if (o == null) {
//                //return null to avoid sending the execution summary
//                return null;
//            } else {
//                long end = System.currentTimeMillis();
//
//                onResult.accept(new JSExec(code, o, start, end));
//            }
    }

}
