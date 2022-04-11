package com.epam.deltix.data.connectors.commons.l2;

/**
 * <p>Represents listener entity, which can be subscribed on L2Processor. <br>
 * Listener gets notifications any time book in L2Processor changes state. <br>
 * If you use this listener with no subscription on L2,
 * than you should care about calling methods of listener by yourself. </p>
 *
 * @param <I> type of book item, which is stored in L2Processor, that notifies this listener
 * @param <E> type of book event, which can be applied to the notifier's item
 * @see L2Processor
 */
public interface L2Listener<I extends BookItem<E>, E extends BookEvent> {

    /**
     * <p>Performs some actions when snapshot package is started. <br>
     * This method is usually called by notifier.</p>
     *
     * @param instrumentBook withInstrument book which notified this listener
     * @param timestamp
     * @param originalTimestamp
     */
    void onSnapshotStarted(InstrumentBooks<I, E> instrumentBook, long timestamp, long originalTimestamp);

    /**
     * <p>Performs some actions when incremental package is started. <br>
     * This method is usually called by notifier.</p>
     *
     * @param instrumentBook withInstrument book which notified this listener
     * @param timestamp
     * @param originalTimestamp
     */
    void onIncrementStarted(InstrumentBooks<I, E> instrumentBook, long timestamp, long originalTimestamp);

    /**
     * <p>Performs some actions when any side of notifier L2Processor changed state due to inserting event.</p>
     *
     * @param book book inside L2Processor which has changed state
     * @param depth depth of item which has been inserted into specified book
     * @param item item which has been inserted into specified book
     */
    void onNew(Book<I, E> book, int depth, I item);

    /**
     * <p>Performs some actions when any side of notifier L2Processor changed state due to update event.</p>
     *
     * @param book book inside L2Processor which has changed state
     * @param depth depth of item which has been changed in specified book
     * @param item item which has been changed in specified book
     */
    void onUpdate(Book<I, E> book, int depth, I item);

    /**
     * <p>Performs some actions when any side of notifier L2Processor changed state due to delete event.</p>
     *
     * @param book book inside L2Processor which has changed state
     * @param depth depth of item which has been deleted from specified book
     * @param item item which has been deleted from specified book
     */
    void onDelete(Book<I, E> book, int depth, I item);

    /**
     * <p>Performs some actions when any side of notifier L2Processor changed state due to reset event.</p>
     *
     * @param book book inside L2Processor which has been reset
     */
    void onReset(Book<I, E> book);

    void onTopBidUpdated(Book<I, E> book);

    void onTopAskUpdated(Book<I, E> book);

    /**
     * <p>Performs some actions when package is finished. <br>
     * This method is usually called by notifier.</p>
     *
     * @param instrumentBook withInstrument book which notified this listener
     */
    void onFinished(InstrumentBooks<I, E> instrumentBook);
}
