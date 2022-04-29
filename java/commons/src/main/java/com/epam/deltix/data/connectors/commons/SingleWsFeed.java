package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.json.JsonWriter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class
 */
public abstract class SingleWsFeed extends MdFeed {
    private static final Logger LOG = Logger.getLogger(SingleWsFeed.class.getName());

    private final static int INITIAL_STATE = 0;
    private final static int STARTED_STATE = 1;
    private final static int CLOSED_STATE = 2;

    private final String uri;
    private final int idleTimeoutMillis;
    private final String[] symbols;

    // to prevent data races and race conditions we do all the management with
    // one single thread of a ScheduledExecutorService
    private final ScheduledExecutorService mgmtService;
    // to prevent data races and race conditions we do all the websocket event processing with
    // one single thread of an ExecutorService
    private final ExecutorService wsExecutorService;

    private final CountDownLatch waitForWsClose = new CountDownLatch(1);

    private WebSocket webSocket; // used by one single thread of mgmtService
    private int state = INITIAL_STATE; // used by one single thread of mgmtService

    private volatile long lastReceiveTime;

    private final PeriodicalJsonTask periodicalJsonTask;

    private final boolean skipGzipHeader;

    protected SingleWsFeed(
        final String uri,
        final int idleTimeoutMillis,
        final MdModel.Options selected,
        final CloseableMessageOutput output,
        final ErrorListener errorListener,
        final String... symbols) {

        this(uri, idleTimeoutMillis, selected, output, errorListener, null, symbols);
    }

    protected SingleWsFeed(
        final String uri,
        final int idleTimeoutMillis,
        final MdModel.Options selected,
        final CloseableMessageOutput output,
        final ErrorListener errorListener,
        final PeriodicalJsonTask periodicalJsonTask,
        final String... symbols) {

        this(uri, idleTimeoutMillis, selected, output, errorListener, periodicalJsonTask, false, symbols);
    }

    protected SingleWsFeed(
            final String uri,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final PeriodicalJsonTask periodicalJsonTask,
            final boolean skipGzipHeader,
            final String... symbols) {

        super(selected, output, errorListener);

        this.uri = uri;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.symbols = symbols;
        this.periodicalJsonTask = periodicalJsonTask;
        this.skipGzipHeader = skipGzipHeader;

        mgmtService =
                Executors.newSingleThreadScheduledExecutor(
                        r -> new Thread(r,"WebSocket Manager#" + uri)
                );

        wsExecutorService = Executors.newSingleThreadExecutor(
                r -> new Thread(r,"WebSocket Executor#" + uri)
        );
    }

    public void start() {
        mgmtService.execute(() -> {
            if (state != INITIAL_STATE) {
                return;
            }
            state = STARTED_STATE;

            final Runnable idleTimeoutWatchdog =
                    idleTimeoutMillis > 0 ?
                            () -> {
                                if (state != STARTED_STATE) {
                                    return;
                                }
                                if (System.nanoTime() - lastReceiveTime >= idleTimeoutMillis * 1_000_000L) {
                                    SingleWsFeed.this.onError(new TimeoutException("Idle timeout reached"));
                                }
                            } : null;

            final WebSocket.Listener wsListener = new WebSocket.Listener() {
                private final ZlibAsciiTextDecompressor decompressor = new ZlibAsciiTextDecompressor(skipGzipHeader); // TODO: configurable decoder?
                private WsJsonFrameSender jsonSender;

                @Override
                public void onOpen(final WebSocket webSocket) {
                    lastReceiveTime = System.nanoTime();
                    jsonSender = new WsJsonFrameSender(webSocket);

                    try {
                        subscribe(jsonSender, symbols);
                    } catch (final Throwable t) {
                        SingleWsFeed.this.onError(t);
                    }
                    WebSocket.Listener.super.onOpen(webSocket);

                    if (idleTimeoutWatchdog != null) {
                        mgmtService.scheduleWithFixedDelay(
                                idleTimeoutWatchdog,
                                idleTimeoutMillis,
                                idleTimeoutMillis,
                                TimeUnit.MILLISECONDS);
                    }

                    if (periodicalJsonTask != null) {
                        mgmtService.scheduleWithFixedDelay(
                            () -> {
                                if (state != STARTED_STATE) {
                                    return;
                                }
                                periodicalJsonTask.execute(jsonSender);
                            },
                            periodicalJsonTask.delayMillis(),
                            periodicalJsonTask.delayMillis(),
                            TimeUnit.MILLISECONDS);
                    }
                }

                @Override
                public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data, final boolean last) {
                    lastReceiveTime = System.nanoTime();

                    try {
                        onJson(data, last, jsonSender);
                    } catch (final Throwable t) {
                        SingleWsFeed.this.onError(t);
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }


                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                    lastReceiveTime = System.nanoTime();

                    try {
                        final CharSequence json = decompressor.decompress(data);
                        if (json != null) {
                            onJson(json, last, jsonSender);
                        }
                    } catch (final Throwable t) {
                        SingleWsFeed.this.onError(t);
                    }
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }

                @Override
                public CompletionStage<?> onPing(final WebSocket webSocket, final ByteBuffer message) {
                    lastReceiveTime = System.nanoTime();

                    return WebSocket.Listener.super.onPing(webSocket, message);
                }

                @Override
                public void onError(final WebSocket webSocket, final Throwable error) {
                    SingleWsFeed.this.onError(error);
                }

                @Override
                public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
                    waitForWsClose.countDown();
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }
            };
            try {
                webSocket = HttpClient.newBuilder()
                        .executor(wsExecutorService)
                        .build()
                        .newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .buildAsync(URI.create(uri),
                                wsListener).join();
            } catch (final Throwable t) {
                SingleWsFeed.this.onError(t);
            }
        });
    }

    @Override
    protected final void doClose() {
        mgmtService.execute(() -> {
            if (state != STARTED_STATE) {
                return;
            }
            state = CLOSED_STATE;

            try {
                onClose();
            } catch (final Throwable t) {
                LOG.log(Level.WARNING, "Unexpected error in onClose(): " + t.getLocalizedMessage(), t);
            }

            InterruptedException wasInterrupted = null;

            try {
                webSocket.sendClose(1000, "Bye-bye");
            } finally {
                try {
                    waitForWsClose.await(3, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    wasInterrupted = e;
                } finally {
                    wsExecutorService.shutdownNow();
                    try {
                        wsExecutorService.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                        wasInterrupted = e;
                    }
                }
                if (wasInterrupted != null) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        mgmtService.shutdown(); // to allow to execute 'close' logic
        try {
            mgmtService.awaitTermination(15, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void onClose() {};

    /**
     *
     * @param jsonWriter
     * @param symbols
     */
    protected abstract void subscribe(JsonWriter jsonWriter, String... symbols);

    /**
     *
     * @param data
     * @param last
     */
    protected abstract void onJson(CharSequence data, boolean last, JsonWriter jsonWriter);

}
