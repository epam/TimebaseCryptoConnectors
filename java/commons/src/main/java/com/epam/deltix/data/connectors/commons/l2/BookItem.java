package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.dfp.Decimal;

import java.io.IOException;

/**
 * Represents item which is stored in a Book.
 *
 * @param <E> type of the event, which can be applied to this item.
 * @see BookEvent
 * @see Book
 */
public interface BookItem<E extends BookEvent> {

    /**
     * Returns the price of this item.
     *
     * @return price of this item. Returned long value should be treated as decimal.
     */
    @Decimal
    long getPrice();

    /**
     * Returns the size of this item.
     *
     * @return size of this item. Returned value should be treated as decimal.
     */
    @Decimal
    long getSize();

    /**
     * Sets the fields of this item according to information in given event.
     *
     * @param event event object with information to update the state of this item.
     */
    void set(E event);

    /**
     * Creates dump of this item's internal state. Dump is written into the given appendable storage.
     *
     * @param to object which dump should be written to.
     * @param <T> type of appendable object for saving internal state of this item.
     * @return storage object of type T filled with this item's internal state.
     * @throws IOException an exception if happened
     */
    <T extends Appendable> T dump(T to) throws IOException;
}
