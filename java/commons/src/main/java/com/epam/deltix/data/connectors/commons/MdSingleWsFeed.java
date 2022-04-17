package com.epam.deltix.data.connectors.commons;

public abstract class MdSingleWsFeed extends SingleWsFeed {
    private final MdProcessor processor;

    protected MdSingleWsFeed(
            final String exchange,
            final String uri,
            final int depth,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {

        this(exchange, uri, depth, idleTimeoutMillis, selected, output, errorListener, null, symbols);
    }

    protected MdSingleWsFeed(
            final String exchange,
            final String uri,
            final int depth,
            final int idleTimeoutMillis,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final PeriodicalJsonTask periodicalJsonTask,
            final String... symbols) {

        super(uri, idleTimeoutMillis, selected, output, errorListener, periodicalJsonTask, symbols);

        processor = MdProcessor.create(exchange, output, selected, depth);
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
