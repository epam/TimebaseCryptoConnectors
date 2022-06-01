package com.epam.deltix.data.connectors.commons;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpFeed extends MdFeed {
    private final static int INITIAL_STATE = 0;
    private final static int STARTED_STATE = 1;
    private final static int CLOSED_STATE = 2;

    // to prevent data races and race conditions we do all the management with
    // one single thread of a ScheduledExecutorService
    private final ScheduledExecutorService mgmtService;
    // to prevent data races and race conditions we do all the http event processing with
    // one single thread of an ExecutorService
    private final ExecutorService httpExecutorService;

    private int state = INITIAL_STATE; // used by one single thread of mgmtService

    private HttpClient httpClient;

    public HttpFeed(
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger) {

        super(selected, output, errorListener, logger);

        mgmtService =
                Executors.newSingleThreadScheduledExecutor(
                        r -> new Thread(r,  HttpFeed.class.getSimpleName() + " Manager")
                );

        httpExecutorService = Executors.newSingleThreadExecutor(
                r -> new Thread(r, HttpFeed.class.getSimpleName() + " Executor")
        );
    }

    public void start() {

        mgmtService.execute(() -> {
            if (state != INITIAL_STATE) {
                return;
            }
            state = STARTED_STATE;

            try {
                httpClient = HttpClient.newBuilder().
                        executor(httpExecutorService).
                        connectTimeout(Duration.ofSeconds(10)).
                        build();
            } catch (final Throwable t) {
                HttpFeed.this.onError(t);
            }
        });
    }

    public void schedule(
            final HttpPoller poller,
            final int periodMillis) {

        mgmtService.execute(() -> {
            if (state != STARTED_STATE) {
                return;
            }

            mgmtService.schedule(() -> {
                if (state != STARTED_STATE) {
                    return;
                }

                final ErrorListener retrier = error -> {
                    error.printStackTrace(); // TODO: log?
                    schedule(poller, 10_000); // re-schedule after an error: TODO: timeout
                };

                try {
                    poller.poll(httpClient, () -> {
                        schedule(poller, periodMillis);
                    }, retrier);
                } catch (final Throwable t) {
                    if (t instanceof InterruptedException) {
                        return;
                    }

                    retrier.onError(t);
                }
            }, periodMillis, TimeUnit.MILLISECONDS);
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
                httpExecutorService.shutdownNow();
                try {
                    httpExecutorService.awaitTermination(5, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    wasInterrupted = e;
                }
            } catch (final Throwable t) {
                //???
            }
            if (wasInterrupted != null) {
                Thread.currentThread().interrupt();
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
}
