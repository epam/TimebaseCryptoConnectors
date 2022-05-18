package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.timebase.messages.InstrumentMessage;

public abstract class MdFeed implements CloseableMessageOutput, ErrorListener {
    private final MdModel.Options selected;
    private final CloseableMessageOutput output;
    private final ErrorListener errorListener;
    private final Logger logger;

    private volatile Throwable error;
    private boolean closed; // guarded by this

    protected MdFeed(
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger) {
        this.selected = selected;
        this.output = output;
        this.errorListener = errorListener;
        this.logger = logger;
    }

    Throwable error() {
        return error;
    }

    public MdModel.Options selected() {
        return selected;
    }

    public Logger logger() {
        return logger;
    }

    @Override
    public final void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        try {
            doClose();
        } finally {
            Util.closeQuiet(output);
        }
    }

    public final void send(final InstrumentMessage message) {
        try {
            output.send(message);
        } catch (final Throwable t) {
            onError(t);
        }
    }

    @Override
    public final void onError(final Throwable error) {
        this.error = error;
        errorListener.onError(error);
    }

    protected abstract void doClose();
}
