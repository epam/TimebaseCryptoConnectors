package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.l2.L2Processor;

public abstract class MdSingleWsRestFeed extends SingleWsRestFeed {
    private final MdProcessor processor;

    protected MdSingleWsRestFeed(
            final String exchange,
            final String wsUrl,
            final String restUrl,
            final int depth,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final boolean isAuthRequired,
            final String... symbols) {

        this(exchange,
                wsUrl,
                restUrl,
                depth,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                null,
                isAuthRequired,
                symbols);
    }

    protected MdSingleWsRestFeed(
            final String exchange,
            final String wsUrl,
            final String restUrl,
            final int depth,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final PeriodicalJsonTask periodicalJsonTask,
            final boolean isAuthRequired,
            final String... symbols) {

        this(exchange,
                wsUrl,
                restUrl,
                depth,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                periodicalJsonTask,
                false,
                isAuthRequired,
                symbols
        );
    }

    protected MdSingleWsRestFeed(
            final String exchange,
            final String wsUrl,
            final String restUrl,
            final int depth,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final PeriodicalJsonTask periodicalJsonTask,
            final boolean skipGzipHeader,
            final boolean isAuthRequired,
            final String... symbols) {

        this(exchange,
                wsUrl,
                restUrl,
                depth,
                L2Processor.UNLIMITED_BOOK_SIZE,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                periodicalJsonTask,
                skipGzipHeader,
                isAuthRequired,
                symbols
        );
    }

    protected MdSingleWsRestFeed(
            final String exchange,
            final String wsUrl,
            final String restUrl,
            final int depth,
            final int fixedDepth,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final PeriodicalJsonTask periodicalJsonTask,
            final boolean skipGzipHeader,
            final boolean isAuthRequired,
            final String... symbols) {

        super(wsUrl,
                restUrl,
                idleTimeoutMillis,
                selected,
                output,
                errorListener,
                logger,
                periodicalJsonTask,
                skipGzipHeader,
                isAuthRequired,
                symbols);

        processor = MdProcessor.create(exchange, output, selected, depth, fixedDepth);
    }

    protected final MdProcessor processor() {
        return processor;
    }

    @Override
    protected final void onClose() {
        final Throwable lastError = error();
        processor.close(
                lastError != null ? lastError.getLocalizedMessage() : null
        );
    }
}
