package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.data.connectors.commons.ObjectPool;
import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Formatter;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>Represents an abstraction for processing L2 model events. This class aggregates two books (bids and offers),
 * provides opportunities for managing the state of the books processing L2 events.
 * <p>Also L2Processor stores properties associated with withInstrument and book sides.
 * <p>This class can have one listener which will be notified any time any of the books changes state
 * due to L2 events ({@link L2Listener L2Listener}).
 * <p>This class is not thread-safe.
 * If multiple threads access instance of this class concurrently, use a
 * {@link java.util.concurrent.locks.ReadWriteLock ReadWriteLock} returned by
 * the {@link #lock()} method.</p>
 *
 * @param <I> type of item which can be stored in books of this processor
 * @param <E> type of event which can be applied to books of this processor
 * @see L2Builder
 * @see L2Listener
 */
public class L2Processor<B extends Book<I, E>, I extends BookItem<E>, E extends BookEvent>
        implements InstrumentBooks<I, E> {

    public static final int UNLIMITED_BOOK_SIZE = Integer.MAX_VALUE;

    /**
     * Returns an instance of the builder class for this processor.
     * @return L2Processor builder with methods for setting mandatory fields
     */
    public static L2Builder builder() {
        return new L2Builder();
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final long source;
    private final String symbol;
    private final int fixedBookSize;
    private final int outputBookSize;
    private final B bids;
    private final B offers;
    private final L2Listener<I, E> l2Listener;

    private boolean isSnapshotMode;
    private boolean wasSnapshotProcessed;

    private boolean wasTopBidUpdated;
    private boolean wasTopOfferUpdated;

    private int incrementActionsNotified;

    private long timestamp;
    private long originalTimestamp;

    private L2Processor(
            final L2Builder builder,
            final B bids,
            final B offers,
            final L2Listener<I, E> listener) {
        this.source = builder.source;
        this.symbol = builder.symbol;

        this.fixedBookSize = builder.fixedBookSize;
        this.outputBookSize = builder.outputBookSize;

        if (outputBookSize > fixedBookSize) {
            throw new IllegalArgumentException("Output size > fixed size");
        }

        this.bids = bids;
        this.offers = offers;
        this.l2Listener = listener;
    }

    /**
     * Returns a lock for concurrent access to an instance of the processor
     * @return lock
     */
    public ReadWriteLock lock() {
        return lock;
    }

    /**
     * Returns the last timestamp passed to {@link #onSnapshotPackageStarted(long timestamp, long originalTimestamp)}
     * or {@link #onIncrementalPackageStarted(long timestamp, long originalTimestamp)}
     * @return timestamp
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns the last original timestamp passed to {@link #onSnapshotPackageStarted(long timestamp, long originalTimestamp)}
     * or {@link #onIncrementalPackageStarted(long timestamp, long originalTimestamp)}
     * @return original timestamp
     */
    public long originalTimestamp() {
        return originalTimestamp;
    }

    /**
     * Returns a flag that {@link #onSnapshotPackageStarted(long timestamp, long originalTimestamp)}
     * was called at least once
     * @return true if a snapshot was processed
     */
    public boolean wasSnapshotProcessed() {
        return wasSnapshotProcessed;
    }

    /**
     * Returns a flag that the top (best) of the bid side was updated
     * @return true if top bid changed
     */
    public boolean wasTopBidUpdated() {
        return wasTopBidUpdated;
    }

    /**
     * Returns a flag that the top (best) of the offer side was updated
     * @return true if top offer changed
     */
    public boolean wasTopOfferUpdated() {
        return wasTopOfferUpdated;
    }

    /**
     * Returns source identifier encoded with {@link com.epam.deltix.qsrv.hf.pub.ExchangeCodec ExchangeCodec}
     * @return encoded source
     */
    @Override
    public long source() {
        return source;
    }

    /**
     * Returns trading symbol this processor was built for
     * @return symbol
     */
    @Override
    public String symbol() {
        return symbol;
    }

    public int outputBookSize() {
        return outputBookSize;
    }

    /**
     * Returns the bid side of the book
     * @return bids
     */
    @Override
    public B bids() {
        return bids;
    }

    /**
     * Returns the offer side of the book
     * @return offers
     */
    @Override
    public B offers() {
        return offers;
    }

    public CharSequence dump() {
        return dump(outputBookSize);
    }

    public CharSequence dumpFull() {
        return dump(new StringBuilder(), Integer.MAX_VALUE);
    }

    public CharSequence dump(final int maxLevels) {
        return dump(new StringBuilder(), maxLevels);
    }

    /**
     * Dumps a text view of the book
     *
     * @param to to
     * @param <T>
     * @return a parameter 'to' passed
     */
    public <T extends Appendable> T dump(final T to, final int maxLevels) {
        try {
            to.append("Book ")
                    .append(symbol)
                    .append('@')
                    .append(ExchangeCodec.longToCode(source))
                    .append(Util.NATIVE_LINE_BREAK);

            final Formatter fmt = new Formatter(to);
            final String divider = "-----------------------------------------------------";

            to.append(divider)
                    .append(Util.NATIVE_LINE_BREAK);

            fmt.format("N | %22s | %22s |%n", "Ask", "Bid");

            to.append(divider)
                    .append(Util.NATIVE_LINE_BREAK);

            for (int i = 0; i < Math.min(Math.max(offers.size(), bids.size()), maxLevels); i++) {
                fmt.format("%2d| %22s | %22s |%n", i, (i < offers.size())
                        ? offers.getItem(i)
                        .dump(new StringBuilder())
                        .toString()
                        : "", (i < bids.size())
                        ? bids.getItem(i)
                        .dump(new StringBuilder())
                        .toString()
                        : "");
            }

            to.append(divider)
                    .append(Util.NATIVE_LINE_BREAK);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return to;
    }

    public void onSnapshotPackageStarted() {
        onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, TimeConstants.TIMESTAMP_UNKNOWN);
    }

    public void onSnapshotPackageStarted(final long timestamp, final long originalTimestamp) {
        isSnapshotMode = true;
        wasSnapshotProcessed = true;

        wasTopBidUpdated = false;
        wasTopOfferUpdated = false;

        incrementActionsNotified = 0;

        this.timestamp = timestamp;
        this.originalTimestamp = originalTimestamp;

        l2Listener.onSnapshotStarted(this, timestamp, originalTimestamp);

        bids().clear();
        offers().clear();
    }

    /**
     *
     */
    public void onIncrementalPackageStarted() {
        onIncrementalPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN);
    }

    /**
     *
     * @param originalTimestamp
     */
    public void onIncrementalPackageStarted(final long originalTimestamp) {
        onIncrementalPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, originalTimestamp);
    }

    /**
     *
     * @param timestamp
     * @param originalTimestamp
     */
    public void onIncrementalPackageStarted(final long timestamp, final long originalTimestamp) {
        isSnapshotMode = false;

        wasTopBidUpdated = false;
        wasTopOfferUpdated = false;

        incrementActionsNotified = 0;

        this.timestamp = timestamp;
        this.originalTimestamp = originalTimestamp;

        // we don't notify l2Listener with onIncrementalPackageStarted()
        // until 1st change in the visible area of the book happened
    }

    /**
     *
     * @param event
     */
    public void onEvent(final E event) {
        if (event.isOffer()) {
            offers.onEvent(event);
            return;
        }
        bids.onEvent(event);
    }

    /**
     *
     */
    public void onPackageFinished() {
        if (wasTopBidUpdated) {
            l2Listener.onTopBidUpdated(bids);
        }
        if (wasTopOfferUpdated) {
            l2Listener.onTopAskUpdated(offers);
        }

        if (isSnapshotMode) {
            snapshotSide(bids);
            snapshotSide(offers);
        }

        if (isSnapshotMode || incrementActionsNotified > 0) {
            l2Listener.onFinished(this);
        }

        if (fixedBookSize < UNLIMITED_BOOK_SIZE) {
            bids.trim(fixedBookSize);
            offers.trim(fixedBookSize);
        }
    }

    private void snapshotSide(final B side) {
        for (int i = 0; i < Math.min(outputBookSize, side.size()); i++) {
            l2Listener.onNew(side, i, side.getItem(i));
        }
    }

    /**
     * Represents builder for creating L2Processor objects.
     *
     * @see L2Processor
     */
    public static class L2Builder {
        private long source = ExchangeCodec.NULL;
        private String symbol;
        private int fixedBookSize = UNLIMITED_BOOK_SIZE;
        private int initialBookSize = 10;
        private int outputBookSize = UNLIMITED_BOOK_SIZE;

        L2Builder() {
        }

        public L2Builder withSource(final long source) {
            this.source = source;
            return this;
        }

        public L2Builder withInstrument(final String symbol) {
            this.symbol = symbol;
            return this;
        }

        public L2Builder withFixedBookSize(final int fixedBookSize) {
            this.fixedBookSize = fixedBookSize;
            return this;
        }

        public L2Builder withFixedBookSize(final int fixedBookSize, final int initialBookSize) {
            this.fixedBookSize = fixedBookSize;
            this.initialBookSize = initialBookSize;
            return this;
        }

        public L2Builder withInitialBookSize(final int initialBookSize) {
            this.initialBookSize = initialBookSize;
            return this;
        }

        /**
         * <p>Sets value of book output size. <br>
         * This value is used by book sides to store only necessary amount of items.</p>
         *
         * @param bookOutputSize the amount of items that are stored in L2processor's book sides
         * @return link to this builder to use it in so called fluent api
         */
        public L2Builder withBookOutputSize(final int bookOutputSize) {
            this.outputBookSize = bookOutputSize;
            return this;
        }

        /**
         * Returns new object of L2Processor parametrized with price items and events.
         *
         * @param listener listener object which will be subscribed on new L2Processor
         * @return new L2Processor object
         */
        public L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>
        buildWithPriceBook(final L2Listener<DefaultItem<DefaultEvent>, DefaultEvent> listener) {
            return build(new PriceBook<>(false), new PriceBook<>(true),
                    new ObjectPool<>(DefaultItem::new), listener);
        }

        <B extends Book<I, E>, I extends BookItem<E>, E extends BookEvent> L2Processor<B, I, E> build(
                final B bids,
                final B offers,
                final ObjectPool<I> pool,
                final L2Listener<I, E> listener) {
            final L2Processor<B, I, E> result = new L2Processor<>(this, bids, offers, listener);
            final com.epam.deltix.data.connectors.commons.l2.BookListener<I, E> newListener
                    = result.createListener();
            bids.init(initialBookSize, newListener, pool);
            offers.init(initialBookSize, newListener, pool);

            return result;
        }
    }

    private BookListener createListener() {
        return new BookListener();
    }

    private class BookListener
            implements com.epam.deltix.data.connectors.commons.l2.BookListener<I, E> {

        @Override
        public boolean beforeInsert(final Book<I, E> book, final int depth, final E event) {
            if (depth == 0) {
                if (book.isOffer()) {
                    wasTopOfferUpdated = true;
                } else {
                    wasTopBidUpdated = true;
                }
            }

            // for a snapshot we push all visible levels in onPackageFinished()
            if (isSnapshotMode) {
                return false;
            }

            if (depth >= outputBookSize) {
                return false;
            }

            if (incrementActionsNotified == 0) {
                l2Listener.onIncrementStarted(L2Processor.this, timestamp, originalTimestamp);
            }

            return true;
        }

        @Override
        public void onInsert(final Book<I, E> book, final int depth, final I item) {
            // remove last visible level the completely filled output book
            // book.size() now is the size after an insert just happened
            if (book.size() > outputBookSize) {
                incrementActionsNotified++;

                final int lastVisibleDepth = outputBookSize - 1;
                l2Listener.onDelete(book, lastVisibleDepth, book.getItem(outputBookSize));
            }

            incrementActionsNotified++;

            l2Listener.onNew(book, depth, item);
        }

        @Override
        public boolean beforeUpdate(final Book<I, E> book, final int depth, final E event) {
            if (depth == 0) {
                if (book.isOffer()) {
                    wasTopOfferUpdated = true;
                } else {
                    wasTopBidUpdated = true;
                }
            }

            // for a snapshot we push all visible levels in onPackageFinished()
            if (isSnapshotMode) {
                return false;
            }

            if (depth >= outputBookSize) {
                return false;
            }

            if (incrementActionsNotified == 0) {
                l2Listener.onIncrementStarted(L2Processor.this, timestamp, originalTimestamp);
            }

            return true;
        }

        @Override
        public void onUpdate(final Book<I, E> book, final int depth, final I item) {
            incrementActionsNotified++;

            l2Listener.onUpdate(book, depth, item);
        }

        @Override
        public boolean beforeDelete(final Book<I, E> book, final int depth) {
            if (depth == 0) {
                if (book.isOffer()) {
                    wasTopOfferUpdated = true;
                } else {
                    wasTopBidUpdated = true;
                }
            }

            // for a snapshot we push all visible levels in onPackageFinished()
            if (isSnapshotMode) {
                return false;
            }

            if (depth >= outputBookSize) {
                return false;
            }

            if (incrementActionsNotified == 0) {
                l2Listener.onIncrementStarted(L2Processor.this, timestamp, originalTimestamp);
            }

            return true;
        }

        @Override
        public void onDelete(final Book<I, E> book, final int depth, final I item) {
            incrementActionsNotified++;

            l2Listener.onDelete(book, depth, item);

            // add new for the known outputBookSize
            if (book.size() >= outputBookSize) {
                incrementActionsNotified++;

                // add insert for depth == outputSize() - 1
                // to restore last visible level
                final int lastVisibleDepth = outputBookSize - 1;
                l2Listener.onNew(book, lastVisibleDepth, book.getItem(lastVisibleDepth));
            }
        }

        @Override
        public boolean beforeReset(final Book<I, E> book) {
            assert isSnapshotMode;

            if (book.size() > 0) {
                if (book.isOffer()) {
                    wasTopOfferUpdated = true;
                } else {
                    wasTopBidUpdated = true;
                }
            }

            return true;
        }

        @Override
        public void onReset(final Book<I, E> book) {
            l2Listener.onReset(book);
        }
    }
}
