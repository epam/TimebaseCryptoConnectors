package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;

/**
 * <p>Represents type of book which stores items based on price. <br>
 * This book stores all items sorted, according to side of the book. <br>
 * When event is processed this book uses binary search to find item to be changed.</p>
 *
 * <p>This class is not thread-safe. If multiple threads access instance of this class concurrently, it must be
 * synchronized externally.</p>
 *
 * @param <I> type of item which can be stored in this book
 * @param <E> type of event which can be applied to items in this book
 */
public class PriceBook<I extends BookItem<E>, E extends BookEvent> extends Book<I, E> {

    /**
     * Constructs new PriceBook object with specified side.
     *
     * @param isOffer side of new book. Must be true if the side is offers, else - false.
     */
    PriceBook(final boolean isOffer) {
        super(isOffer);
    }

    @Override
    protected void onEvent(final E event) {
        final int n = binarySearchPrice(event.getPrice());

        final boolean delete = event.getSize() == TypeConstants.DECIMAL_NULL;

        if (n < 0) {
            if (!delete) {    // skip delete for unknown price level
                insert(-n - 1, event);
            }
        } else {
            if (delete) {
                delete(n);
            } else {
                update(n, event);
            }
        }
    }

    /**
     * <p>Finds and returns item by it's price.
     * This method is based on binary search.</p>
     *
     * @param price price of the item to be found
     * @return positive index of the item if it is found, else - negative value
     */
    protected int binarySearchPrice(@Decimal final long price) {
        int low = 0;
        int high = currentSize - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final long midVal = items[mid].getPrice();
            final int cmp = (isOffer ?
                    1 :
                    -1) * Decimal64Utils.compareTo(midVal, price); // isOffer ? midVal - price : price - midVal;

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // price found
            }
        }

        return -(low + 1); // price not found
    }
}
