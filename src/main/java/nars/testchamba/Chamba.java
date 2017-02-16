package nars.testchamba;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import nars.net.JsServer;
import nars.testchamba.client.AgentAPI;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class Chamba extends View {

    private static final Logger LOG;

    static {

        LOG = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        LoggerContext loggerContext = LOG.getLoggerContext();
        // we are not interested in auto-configuration
        loggerContext.reset();

        PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(loggerContext);
        //logEncoder.setPattern("\\( %highlight(%level),%green(%thread),%yellow(%logger{0}) \\): \"%message\".%n");
        logEncoder.setPattern("\\( %green(%thread),%highlight(%logger{0}) \\): \"%message\".%n");
        logEncoder.setImmediateFlush(false);
        logEncoder.start();


        {
            ConsoleAppender c = new ConsoleAppender();
            c.setContext(loggerContext);
            c.setEncoder(logEncoder);
            //c.setWithJansi(true);
            c.start();

            LOG.addAppender(c);
        }
    }

    protected final JsServer<AgentAPI> js;

    int renderPeriodMS = 25;
    int updatePeriodMS = 25;

    public Chamba(Space space, boolean showWindow, int port) throws SocketException {
        super(space);

        initSurface();
        startSurface();

        js = new JsServer<>(port, a -> new AgentAPI(a, this.space));

        frameRate(1000f/renderPeriodMS);
        setup();

        Timer s = new Timer();
        s.scheduleAtFixedRate(new TimerTask() {

            public long lastDrawn = System.nanoTime();

            @Override
            public void run() {

                long prev = lastDrawn;

                long now = System.nanoTime();
                double dtSeconds = (now - prev) / 1.0e9;

                Chamba.this.update(dtSeconds);

                lastDrawn = now;
            }
        }, 0, updatePeriodMS);


        if (showWindow)
            this.newWindow(1000, 800, true);
    }




}