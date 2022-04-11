package com.epam.deltix.data.connectors.commons.l2;

public class L2ListenerAdapter<I extends BookItem<E>, E extends BookEvent>
        implements L2Listener<I, E> {
    @Override
    public void onSnapshotStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {

    }

    @Override
    public void onIncrementStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {

    }

    @Override
    public void onNew(final Book<I, E> book, final int depth, final I item) {

    }

    @Override
    public void onUpdate(final Book<I, E> book, final int depth, final I item) {

    }

    @Override
    public void onDelete(final Book<I, E> book, final int depth, final I item) {

    }

    @Override
    public void onReset(final Book<I, E> book) {

    }

    @Override
    public void onTopBidUpdated(final Book<I, E> book) {

    }

    @Override
    public void onTopAskUpdated(final Book<I, E> book) {

    }

    @Override
    public void onFinished(final InstrumentBooks<I, E> instrumentBook) {

    }
}
