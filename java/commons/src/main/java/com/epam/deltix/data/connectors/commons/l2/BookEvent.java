package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.dfp.Decimal;

/**
 * <p>Represents event, which can be processed with book.<br>
 * Usually book can change it's state in response of getting event object.<br>
 * This interface gives access to such event fields as side, price and size,
 * that are used by book to change it's items state.</p>
 *
 * @see Book
 * @see BookItem
 */
public interface BookEvent {

    /**
     * Checks whether this event is an offer.
     *
     * @return true if this event has type offer, else - false.
     */
    boolean isOffer();

    /**
     * Returns the price of this event. This price can be used to update price property of book item.
     *
     * @return price value of this event. Returned long value should be treated as decimal.
     */
    @Decimal
    long getPrice();

    /**
     * Returns the size of this event. This size can be used to update size property of book item.
     *
     * @return size value of this event. Returned long value should be treated as decimal.
     */
    @Decimal
    long getSize();
}
