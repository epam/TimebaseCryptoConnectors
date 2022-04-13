package com.epam.deltix.data.connectors.commons;

/**
 *
 * @param <T>
 */
public abstract class DataConnector<T extends DataConnectorSettings> implements AutoCloseable {
    private final String tbUrl;
    private final String stream;
    private final MdModel model;

    protected DataConnector(T settings, MdModel model) {
        this.tbUrl = settings.getTbUrl();
        this.stream = settings.getStream();
        this.model = model;
    }

    private Retrier<MdFeed> retrier; // guarded by this
    private boolean closed; // guarded by this

    /**
     *
     * @return
     */
    public MdModel model() {
        return model;
    }

    /**
     *
     * @param selected
     * @param symbols
     */
    public final synchronized void subscribe(final MdModel.Options selected, final String... symbols) {
        if (closed) {
            throw new IllegalStateException("Closed");
        }

        Util.closeQuiet(retrier);

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Empty model selection");
        }

        retrier = new Retrier<>(doSubscribe(
                selected,
                //new TbMessageOutputFactory(tbUrl, stream, selected.types()),

                () -> new CloseableMessageOutput() {
                    @Override
                    public void close() {
                        System.out.println("CLOSE");
                    }

                    @Override
                    public void send(final com.epam.deltix.timebase.messages.InstrumentMessage message) {
                        System.out.println(message);
                    }
                },

                symbols
        ), 10_000);

        retrier.start();
    }

    /**
     *
     */
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }

        Util.closeQuiet(retrier);
    }

    /**
     *
     * @param selected
     * @param outputFactory
     * @param symbols
     * @return
     */
    protected abstract RetriableFactory<MdFeed> doSubscribe(
            MdModel.Options selected,
            CloseableMessageOutputFactory outputFactory,
            String... symbols);
}
