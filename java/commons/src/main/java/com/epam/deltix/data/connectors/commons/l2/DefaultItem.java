package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;

import java.io.IOException;

/**
 * <p>Represents default item which can be stored in a book.</p>
 *
 * @param <E> type of event which can be applied to this item
 * @see Book
 * @see BookEvent
 */
public class DefaultItem<E extends BookEvent> implements BookItem<E> {
    @Decimal
    private long price;
    @Decimal
    private long size;

    @Override
    public @Decimal long getPrice() {
        return price;
    }

    @Override
    public @Decimal long getSize() {
        return size;
    }

    @Override
    public void set(final E event) {
        price = event.getPrice();
        size = event.getSize();
    }

    @Override
    public <T extends Appendable> T dump(final T to) throws IOException {
        to.append(Decimal64Utils.toString(price))
                .append('/')
                .append(Decimal64Utils.toString(size));

        return to;
    }

    @Override
    public String toString() {
        try {
            return dump(new StringBuilder()).toString();
        } catch (final IOException e) {
            return null;
        }
    }
}