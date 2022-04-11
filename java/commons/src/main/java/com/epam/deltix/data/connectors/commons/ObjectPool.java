package com.epam.deltix.data.connectors.commons;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * An example of using:
 *
 * <pre>
 * // Pool for the byte arrays
 * public class BytePacketPool extends ObjectPool&lt;byte[]&gt; {
 *
 *        public BytePacketPool() {
 *        }
 *
 *        public BytePacketPool(int initialAllocSize) {
 *            super(initialAllocSize);
 *        }
 *
 *        &#64;Override
 *        protected byte[] createItem() {
 *            return new byte[512];
 *       }
 * }
 *
 * ...
 *
 * // let's use the new pool class
 *
 * final BytePacketPool pool = new BytePacketPool(128); // create new pool with 128 prebuilt instances of byte array
 * byte[] buffer = pool.borrow(); // borrow a buffer from the pool
 * try {
 *
 *     // do something with the buffer here...
 *
 * } finally {
 *     pool.release(buffer); // return the buffer back to the pool
 * }
 * </pre>
 * <p>
 * This class is not thread-safe. Use an appropriate lock if required.
 *
 * @param <T> type of the pooled objects
 */
public final class ObjectPool<T> {

    private final List<T> freeItems = new ArrayList<>();

    private int lastItem = -1;
    private final Supplier<T> supplier;

    /**
     * <p>
     * Creates empty pool with the supplier specified.
     * </p>
     * @param supplier to be used to prepare new instances
     */
    public ObjectPool(final Supplier<T> supplier) {
        this(0, supplier);
    }

    /**
     * <p>
     * Creates pool with a number of prepared objects.
     * </p>
     *
     * @param initialAllocSize how many objects should be prepared initially
     * @param supplier         object for items creation
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ObjectPool(final int initialAllocSize, final Supplier<T> supplier) {
        if (initialAllocSize < 0) {
            throw new IllegalArgumentException("Initial allocated size should be >= 0");
        }
        this.supplier = supplier;

        for (int i = 0; i < initialAllocSize; i++) {
            freeItems.add(createItem());
        }

        lastItem = freeItems.size() - 1;
    }

    /**
     * <p>
     * Gets available object instance from the pool or creates a new one.
     * </p>
     *
     * @return object instance from the pool or a just created new one
     */
    public T borrow() {
        return (lastItem < 0) ?
                createItem() :
                freeItems.get(lastItem--);
    }

    /**
     * Returns an object instance back to the pool.
     *
     * @param item the object instance to be returned back
     */
    public void release(final T item) {
        if (++lastItem >= freeItems.size()) {
            freeItems.add(item);
        } else {
            freeItems.set(lastItem, item);
        }
    }

    public void release(final List<T> items) {
        for (int i = 0; i < items.size(); i++) {
            release(items.get(i));
        }
    }

    /**
     * @return how many object instances have been created for pooling,
     * including the objects which were borrow and are not returned back yet
     */
    public int getAllocatedSize() {
        return freeItems.size();
    }

    /**
     * @return how many object instances were borrow from the pool and are not returned back yet
     */
    public int getUsedSize() {
        return lastItem + 1;
    }

    /**
     * <p>
     * Factory method which creates new instance of the object instance if the pool
     * don't have a free one to return from {@link #borrow() borrow()} method.
     * </p>
     *
     * @return new instance of the object
     */
    private T createItem() {
        return supplier.get();
    }
}
