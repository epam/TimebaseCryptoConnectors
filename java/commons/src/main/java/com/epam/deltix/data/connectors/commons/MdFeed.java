package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.timebase.messages.InstrumentMessage;

public abstract class MdFeed implements CloseableMessageOutput, ErrorListener {
    private final MdModel.Options selected;
    private final CloseableMessageOutput output;
    private final ErrorListener errorListener;

    private volatile Throwable error;
    private boolean closed; // guarded by this

    protected MdFeed(
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener) {
        this.selected = selected;
        this.output = output;
        this.errorListener = errorListener;
    }

    Throwable error() {
        return error;
    }

    public MdModel.Options selected() {
        return selected;
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
