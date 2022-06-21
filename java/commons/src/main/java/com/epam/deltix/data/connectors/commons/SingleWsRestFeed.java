package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.json.JsonWriter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * A class
 */
public abstract class SingleWsRestFeed extends MdFeed {
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

    private WsJsonFrameSender jsonSender;

    protected SingleWsRestFeed(
            final String uri,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        this(uri,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                null,
                symbols);
    }

    protected SingleWsRestFeed(
            final String uri,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final PeriodicalJsonTask periodicalJsonTask,
            final String... symbols) {

        this(uri,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                periodicalJsonTask,
                false,
                symbols);
    }

    protected SingleWsRestFeed(
            final String uri,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final PeriodicalJsonTask periodicalJsonTask,
            final boolean skipGzipHeader,
            final String... symbols) {

        super(selected, output, errorListener, logger);

        this.uri = uri;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.symbols = symbols;
        this.periodicalJsonTask = periodicalJsonTask;
        this.skipGzipHeader = skipGzipHeader;

        mgmtService =
                Executors.newSingleThreadScheduledExecutor(
                        r -> new Thread(r, SingleWsFeed.class.getSimpleName() + " Manager#" + uri)
                );

        wsExecutorService = Executors.newSingleThreadExecutor(
                r -> new Thread(r, SingleWsFeed.class.getSimpleName() + " Executor#" + uri)
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
                                    SingleWsRestFeed.this.onError(new TimeoutException("Idle timeout reached"));
                                }
                            } : null;

            final WebSocket.Listener wsListener = new WebSocket.Listener() {
                private final ZlibAsciiTextDecompressor decompressor = new ZlibAsciiTextDecompressor(skipGzipHeader); // TODO: configurable decoder?

                @Override
                public void onOpen(final WebSocket webSocket) {
                    final Logger logger = logger();
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket onOpen");
                    }

                    lastReceiveTime = System.nanoTime();
                    jsonSender = new WsJsonFrameSender(webSocket);

                    try {
                        subscribe(jsonSender, symbols);
                    } catch (final Throwable t) {
                        SingleWsRestFeed.this.onError(t);
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
                    final Logger logger = logger();
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket onText (last=" + last + ") " + data);
                    }

                    lastReceiveTime = System.nanoTime();

                    try {
                        onJson(data, last, jsonSender);
                    } catch (final Throwable t) {
                        SingleWsRestFeed.this.onError(t);
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }


                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                    lastReceiveTime = System.nanoTime();

                    try {
                        final CharSequence json = decompressor.decompress(data);
                        if (json != null) {
                            final Logger logger = logger();
                            if (logger.isDebugEnabled()) {
                                logger.debug("WebSocket onBinary (last=" + last + ") [DECODED] " + json);
                            }

                            onJson(json, last, jsonSender);
                        }
                    } catch (final Throwable t) {
                        SingleWsRestFeed.this.onError(t);
                    }
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }

                @Override
                public CompletionStage<?> onPing(final WebSocket webSocket, final ByteBuffer message) {
                    final Logger logger = logger();
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket onPing " + message);
                    }

                    lastReceiveTime = System.nanoTime();

                    return WebSocket.Listener.super.onPing(webSocket, message);
                }

                @Override
                public void onError(final WebSocket webSocket, final Throwable error) {
                    final Logger logger = logger();
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket onError " + error.getLocalizedMessage());
                    }

                    SingleWsRestFeed.this.onError(error);
                }

                @Override
                public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
                    final Logger logger = logger();
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket onClose [" + statusCode + "] " + reason);
                    }

                    waitForWsClose.countDown();
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }
            };
            try {
                webSocket = HttpClient.newBuilder().
                        executor(wsExecutorService).
                        build().
                        newWebSocketBuilder().
                        connectTimeout(Duration.ofSeconds(10)).
                        buildAsync(URI.create(uri),
                                wsListener).join();
            } catch (final Throwable t) {
                SingleWsRestFeed.this.onError(t);
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

            InterruptedException wasInterrupted = null;

            try {
                onClose();
            } catch (final Throwable t) {
                if (t instanceof InterruptedException) {
                    wasInterrupted = (InterruptedException) t;
                } else {
                    logger().warning(() -> "Unexpected error in onClose(): " + t.getLocalizedMessage(), t);
                }
            }

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

    protected void onClose() {
    }

    /**
     * @param targetsMap
     */

    protected void initBookSnapshots(Map<String, URI> targetsMap) {
        mgmtService.execute(() -> {
            HttpClient client = HttpClient.newHttpClient();
            targetsMap.keySet().stream().forEach(symbol -> {
                client.sendAsync(
                                HttpRequest.newBuilder(targetsMap.get(symbol)).GET().build(),
                                HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(body -> processBookSnapshot(body, symbol));
            });
        });
    }

    /**
     * @param jsonWriter
     * @param symbols
     */
    protected abstract void subscribe(JsonWriter jsonWriter, String... symbols);

    /**
     * @param data
     * @param last
     */
    protected abstract void onJson(CharSequence data, boolean last, JsonWriter jsonWriter);

    /**
     * @param body
     * @param symbol
     */
    protected abstract void processBookSnapshot(String body, String symbol);
}
