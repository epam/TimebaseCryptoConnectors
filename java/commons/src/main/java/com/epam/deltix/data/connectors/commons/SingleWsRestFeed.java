package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.commons.json.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * A class
 */
public abstract class SingleWsRestFeed extends MdFeed {
    private final static int INITIAL_STATE = 0;
    private final static int STARTED_STATE = 1;
    private final static int CLOSED_STATE = 2;

    private final String wsUrl;
    private final String restUrl;
    private final int idleTimeoutMillis;
    private final String[] symbols;

    // to prevent data races and race conditions we do all the management with
    // one single thread of a ScheduledExecutorService
    private final ScheduledExecutorService mgmtService;
    // to prevent data races and race conditions we do all the websocket event processing with
    // one single thread of an ExecutorService
    private final ExecutorService wsRestExecutorService;

    private final CountDownLatch waitForWsClose = new CountDownLatch(1);

    private HttpClient httpClient; // used by one single thread of mgmtService
    private WebSocket webSocket; // used by one single thread of mgmtService
    private int state = INITIAL_STATE; // used by one single thread of mgmtService

    private volatile long lastReceiveTime;

    private final PeriodicalJsonTask periodicalJsonTask;

    private final boolean skipGzipHeader;

    private WsJsonFrameSender jsonSender;

    private boolean isAuthRequired;

    protected SingleWsRestFeed(
            final String wsUrl,
            final String restUrl,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final boolean isAuthRequired,
            final String... symbols) {

        this(wsUrl,
                restUrl,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                null,
                isAuthRequired,
                symbols);
    }

    protected SingleWsRestFeed(
            final String wsUrl,
            final String restUrl,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final PeriodicalJsonTask periodicalJsonTask,
            final boolean isAuthRequired,
            final String... symbols) {

        this(wsUrl,
                restUrl,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                periodicalJsonTask,
                false,
                isAuthRequired,
                symbols);
    }

    protected SingleWsRestFeed(
            final String wsUrl,
            final String restUrl,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final PeriodicalJsonTask periodicalJsonTask,
            final boolean skipGzipHeader,
            final boolean isAuthRequired,
            final String... symbols) {

        super(selected, output, errorListener, logger);

        this.wsUrl = wsUrl;
        this.restUrl = restUrl;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.symbols = symbols;
        this.periodicalJsonTask = periodicalJsonTask;
        this.skipGzipHeader = skipGzipHeader;
        this.isAuthRequired = isAuthRequired;

        mgmtService =
                Executors.newSingleThreadScheduledExecutor(
                        r -> new Thread(r, SingleWsFeed.class.getSimpleName() + " Manager#" + wsUrl)
                );

        wsRestExecutorService = Executors.newSingleThreadExecutor(
                r -> new Thread(r, SingleWsFeed.class.getSimpleName() + " Executor#" + wsUrl)
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
                        onWsJson(data, last, jsonSender);
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

                            onWsJson(json, last, jsonSender);
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
                httpClient = HttpClient.newBuilder().
                        executor(wsRestExecutorService).build();

                String websocketUrl = wsUrl;
                if (isAuthRequired) {
                    websocketUrl = authenticate(wsUrl);
                }

                webSocket = httpClient.
                        newWebSocketBuilder().
                        connectTimeout(Duration.ofSeconds(10)).
                        buildAsync(URI.create(websocketUrl),
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
                    wsRestExecutorService.shutdownNow();
                    try {
                        wsRestExecutorService.awaitTermination(5, TimeUnit.SECONDS);
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

    protected void getAsync(String id, String relativeUrl) {
        mgmtService.execute(() -> {
            if (state != STARTED_STATE) {
                return;
            }
            String fullUrl = restUrl + relativeUrl;
            try {
                httpClient.sendAsync(
                                HttpRequest.newBuilder(new URI(fullUrl)).GET().build(),
                                HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(body -> onRestJson(id, body));
            } catch (URISyntaxException e) {
                logger().warning("Error: url: " + fullUrl + " is not valid", e);
            }
        });
    }

    protected CharSequence post(String relativeUrl) {
        CharSequence response = null;
        String fullUrl = restUrl + relativeUrl;

        try {
            response = httpClient.send(
                    HttpRequest.newBuilder(new URI(fullUrl)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
        } catch (URISyntaxException e) {
            logger().warning("Error: url: " + fullUrl + " is not valid", e);
        } catch (InterruptedException e) {
            logger().warning("Error: url: " + fullUrl + " InterruptedException", e);
        } catch (IOException e) {
            logger().warning("Error: url: " + fullUrl + " IOException", e);
        }

        return response;
    }

    /**
     * @param jsonWriter writer
     * @param symbols list of symbols
     */
    protected abstract void subscribe(JsonWriter jsonWriter, String... symbols);

    /**
     * @param data data
     * @param last is last portion
     * @param jsonWriter writer
     */
    protected abstract void onWsJson(CharSequence data, boolean last, JsonWriter jsonWriter);

    /**
     * @param id id of element
     * @param body text
     */
    protected abstract void onRestJson(String id, CharSequence body);

    /**
     * @param wsUrl authentication address
     * @return authentication url with token
     */
    protected abstract String authenticate(String wsUrl);
}
