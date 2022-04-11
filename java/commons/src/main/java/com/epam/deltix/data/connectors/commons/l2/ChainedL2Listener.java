package com.epam.deltix.data.connectors.commons.l2;

import java.util.ArrayList;
import java.util.List;

public class ChainedL2Listener<I extends BookItem<E>, E extends BookEvent> implements L2Listener<I, E> {
    public static <I extends BookItem<E>, E extends BookEvent> L2Listener<I, E> chain(final L2Listener<I, E>... listeners) {
        if (listeners == null || listeners.length == 0) {
            return null;
        }
        if (listeners.length == 1) {
            return listeners[0];
        }
        return new ChainedL2Listener(listeners);
    }

    public static <I extends BookItem<E>, E extends BookEvent> Builder<I, E> builder() {
        return new Builder<>();
    }

    public static class Builder<I extends BookItem<E>, E extends BookEvent> {
        private final List<L2Listener<I, E>> listeners = new ArrayList<>();

        public Builder<I, E> with(final L2Listener<I, E> listener) {
            listeners.add(listener);
            return this;
        }

        public L2Listener<I, E> build() {
            return chain(listeners.toArray(new L2Listener[] {}));
        }
    }

    private final L2Listener<I, E>[] listeners;

    public ChainedL2Listener(final L2Listener<I, E>... listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onSnapshotStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onSnapshotStarted(instrumentBook, timestamp, originalTimestamp);
        }
    }

    @Override
    public void onIncrementStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onIncrementStarted(instrumentBook, timestamp, originalTimestamp);
        }
    }

    @Override
    public void onNew(final Book<I, E> book, final int depth, final I item) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onNew(book, depth, item);
        }
    }

    @Override
    public void onUpdate(final Book<I, E> book, final int depth, final I item) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onUpdate(book, depth, item);
        }
    }

    @Override
    public void onDelete(final Book<I, E> book, final int depth, final I item) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onDelete(book, depth, item);
        }
    }

    @Override
    public void onReset(final Book<I, E> book) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onReset(book);
        }
    }

    @Override
    public void onTopBidUpdated(final Book<I, E> book) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onTopBidUpdated(book);
        }
    }

    @Override
    public void onTopAskUpdated(final Book<I, E> book) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onTopAskUpdated(book);
        }
    }

    @Override
    public void onFinished(final InstrumentBooks<I, E> instrumentBook) {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onFinished(instrumentBook);
        }
    }
}
