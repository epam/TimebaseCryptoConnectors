package com.epam.deltix.data.connectors.commons.l2;

/**
 * <p>Represents listener entity, which can be subscribed on a book.<br>
 * Listener gets notifications any time book changes its state.</p>
 *
 * <p>If you use this listener with no subscription on any book,
 *  than you should care about calling methods of listener by yourself.</p>
 *
 * @param <I> type of item, which is stored in the book, that notifies this listener.
 * @param <E> type of event, which can be applied to the book, that notifies this listener.
 * @see BookEvent
 * @see BookItem
 */
public interface BookListener<I extends BookItem<E>, E extends BookEvent> {

    boolean beforeInsert(Book<I, E> book, int depth, E event);

    /**
     * <p>Performs some actions when notifier gets an insert event.<br>
     * This method is usually called by notifier. If you use listener object without notifier,
     * than you should take care of calling this method yourself.</p>
     *
     * @param book book to which insert action was applied.
     * @param depth value of inserted into book item's depth.
     * @param item object which was inserted into book.
     */
    void onInsert(Book<I, E> book, int depth, I item);

    boolean beforeUpdate(Book<I, E> book, int depth, E event);

    /**
     * <p>Performs some actions when notifier gets an update event.<br>
     * This method is usually called by notifier. If you use listener object without notifier,
     * than you should take care of calling this method yourself.</p>
     *
     * @param book book to which update action was applied.
     * @param depth value of updated in book item's depth.
     * @param item object which was updated in book side.
     */
    void onUpdate(Book<I, E> book, int depth, I item);

    boolean beforeDelete(Book<I, E> book, int depth);

    /**
     * <p>Performs some actions when notifier gets an delete event.<br>
     * This method is usually called by notifier. If you use listener object without notifier,
     * than you should take care of calling this method yourself.</p>
     *
     * @param book book to which delete action was applied.
     * @param depth value of deleted in book item's depth.
     * @param item object which was updated in book side.
     */
    void onDelete(Book<I, E> book, int depth, I item);

    boolean beforeReset(Book<I, E> book);

    /**
     * <p>Performs some actions when notifier gets and reset event.<br>
     * This method is usually called by notifier. If you use listener object without notifier,
     * than you should take care of calling this method yourself.</p>
     *
     * @param book book to which reset action was applied.
     */
    void onReset(Book<I, E> book);
}
