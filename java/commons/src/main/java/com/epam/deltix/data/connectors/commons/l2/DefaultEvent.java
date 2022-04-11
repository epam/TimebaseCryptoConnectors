package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;

/**
 * <p>Represents event, which can be processed with any book that stores default items.<br>
 * Usually book can change it's state in response of getting event object.<br>
 * This class aggregates such event fields as side, price and size,
 * that are used by book to change it's items state.</p>
 *
 * @see Book
 * @see PriceBook
 * @see DefaultItem
 */
public class DefaultEvent implements BookEvent {

    /**
     * Price of this event. This long value must be treated as decimal.
     */
    @Decimal
    protected long price = Decimal64Utils.NULL;

    /**
     * Size of this event. This long value must be treated as decimal.
     */
    @Decimal
    protected long size = Decimal64Utils.NULL;

    /**
     * Side of this event. True if side is offers, else - false.
     */
    protected boolean isOffer;

    @Override
    public @Decimal long getSize() {
        return size;
    }

    @Override
    public boolean isOffer() {
        return isOffer;
    }

    @Override
    public @Decimal long getPrice() {
        return price;
    }

    /**
     * <p>Sets fields of this event. This method is required to reuse event objects, that are stored in a pool. <br>
     * Should be called to fill event with data any time event is going to be reused.</p>
     *
     * @param isOffer side of this event. Must be true for offers, else - false.
     * @param price price of this event
     * @param size size of this event.
     * @see Pool
     */
    public void set(final boolean isOffer, final @Decimal long price, final @Decimal long size) {
        this.isOffer = isOffer;
        this.price = price;
        this.size = size;
    }

    /**
     * <p>Resets fields of this event.
     * All fields will be equal to the default value after this method is called.</p>
     */
    public void reset() {
        isOffer = false;
        price = Decimal64Utils.NULL;
        size = Decimal64Utils.NULL;
    }

    @Override
    public String toString() {
        return (isOffer ?
                "O" :
                "B") + "/" + Decimal64Utils.toString(price) + "/" + Decimal64Utils.toString(size);
    }
}
