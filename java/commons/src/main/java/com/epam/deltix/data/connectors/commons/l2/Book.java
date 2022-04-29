package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.data.connectors.commons.ObjectPool;

import java.lang.reflect.Array;

/**
 * <p>Represents basic book for one side (bid or offer). <br>
 * The main aim of this class is to give basic structure of book
 * and set of operations to manage it. To create a specific type of book developer should extend this class.</p>
 *
 * <p>This class is not thread-safe. If multiple threads access instance of this class concurrently, it must be
 * synchronized externally.</p>
 *
 * @param <I> type of item, which can be stored in this book.
 * @param <E> type of event, which can be applied to items stored in this book.
 *
 * @see BookEvent
 * @see BookItem
 */
public abstract class Book<I extends BookItem<E>, E extends BookEvent> {

    /**
     * <p>Maximal acceptable size of book.</p>
     */
    protected static final int BOOK_SIZE_LIMIT = 64000;

    /**
     * <p>Type of book, true if type is offer, else - false.</p>
     */
    protected final boolean isOffer;

    /**
     * <p>Reusable objects pool with book items. Used to avoid collecting of items by GC.</p>
     */
    protected ObjectPool<I> pool;

    /**
     * <p>Array of items, that are stored in this book.</p>
     */
    protected I[] items;

    /**
     * <p>Book side event listener. Must be notified when book side is changing item's state.</p>
     */
    protected BookListener<I, E> listener;

    /**
     * <p>Current amount of items, that are stored in this book.</p>
     */
    protected int currentSize;

    /**
     * <p>Constructs book with specified side value.</p>
     *
     * @param isOffer boolean value, which is used to determine side. Must be true if side is offer, else - false.
     */
    protected Book(final boolean isOffer) {
        this.isOffer = isOffer;
    }

    /**
     * <p>Initializes book side with specified listener and object pool,
     * creates array of items with specified initial size. <br>
     * This method should be called after constructor call to finish book creation.</p>
     *
     * @param initialSize initial size, which is used to create array of items
     * @param listener an object, which must be notified any time book changes item's state
     * @param pool a pool of reusable objects (items). Used to avoid collecting items by GC.
     */
    @SuppressWarnings("unchecked")
    protected void init(final int initialSize, final BookListener<I, E> listener, final ObjectPool<I> pool) {
        this.listener = listener;
        this.pool = pool;

        final I o = pool.borrow();
        pool.release(o);
        this.items = (I[]) Array.newInstance(o.getClass(), initialSize); // unchecked
    }

    /**
     * <p>Makes this book to process the given event. <br>
     * Usually book changes it's state in response of getting an event. <br>
     * If you try to apply event with invalid value of price, size etc. book will ignore this event.</p>
     *
     * @param event event to be applied to this book side
     */
    protected abstract void onEvent(E event);

    /**
     * Checks if book has type offers or bids.
     *
     * @return true if book side has type offer, else - false.
     */
    public boolean isOffer() {
        return isOffer;
    }

    /**
     * <p>Returns all stored items back to the object pool. <br>
     * After calling this method book side will be empty.</p>
     */
    public void clear() {
        final boolean notifyReset = listener != null ?
                listener.beforeReset(this) : false;

        for (int i = 0; i < currentSize; i++) {
            pool.release(items[i]);
        }

        currentSize = 0;

        if (notifyReset) {
            listener.onReset(this);
        }
    }

    /**
     * Checks if this book has no items.
     *
     * @return true if current book size is zero or less.
     */
    public boolean isEmpty() {
        return currentSize < 1;
    }

    /**
     * Returns the number of items in this book.
     *
     * @return the number of items in this book.
     */
    public int size() {
        return currentSize;
    }

    /**
     * Returns the item at the specified depth in this book.
     *
     * @param depth depth of index to return
     * @return item at the specified depth
     * @throws ArrayIndexOutOfBoundsException if depth is less than zero or greater than current size of this book.
     */
    public I getItem(final int depth) {
        if (depth >= currentSize || depth < 0) {
            throw new ArrayIndexOutOfBoundsException(depth + " of " + currentSize);
        }

        return items[depth];
    }

    /**
     * <p>Inserts new item with fields filled accordingly to a specified event into this book.<br>
     * Depth should be grater than zero, otherwise call of this method returns immediately.<br>
     * Before inserting new item, this method shifts all items with depth greater or equals to the specified
     * on one position down.<br>
     * If it is necessary capacity of the array with items will be increased. If new capacity is grater than
     * limit of size exception will be thrown.</p>
     *
     * @param depth the depth of item to be inserted
     * @param event an event with fields for new item to be created
     * @throws IllegalStateException if current capacity of this book is grater than book size limit.
     */
    @SuppressWarnings("unchecked")
    protected void insert(final int depth, final E event) {
        if (depth < 0) {
            return;
        }

        final boolean notifyInsert = listener != null ?
            listener.beforeInsert(this, depth, event) : false;

        if (currentSize == items.length) {
            if (items.length >= BOOK_SIZE_LIMIT) {
                throw new IllegalStateException("Unbelievable book size " + items.length);
            }

            final I[] newItems = (I[]) Array.newInstance(items.getClass().getComponentType(),
                    items.length << 1); // unchecked
            System.arraycopy(items, 0, newItems, 0, items.length);
            items = newItems;
        }

        int tail = currentSize - depth;
        if (tail > 0) {
            if (currentSize == items.length) {
                tail--;
            }

            System.arraycopy(items, depth, items, depth + 1, tail);
        }

        final I item = pool.borrow();

        item.set(event);
        items[depth] = item;
        currentSize++;

        if (notifyInsert) {
            listener.onInsert(this, depth, item);
        }
    }

    /**
     * <p>Updates the item at the specified depth with fields accordingly to specified event. <br>
     * Depth should be grater than zero, otherwise call of this method returns immediately.</p>
     *
     * @param depth the depth of item to be updated
     * @param event event with field for update of item
     */
    protected void update(final int depth, final E event) {
        if (depth < 0) {
            return;
        }

        final I item = items[depth];

        if (item == null) {
            return;
        }

        final boolean notifyUpdate = listener != null ?
                listener.beforeUpdate(this, depth, event) : false;

        item.set(event);

        if (notifyUpdate) {
            listener.onUpdate(this, depth, item);
        }
    }

    /**
     * <p>Deletes the item at the specified depth in this book. <br>
     * Depth should be grater than zero, otherwise call of this method returns immediately.</p>
     *
     * @param depth the depth of the item to be deleted
     */
    protected void delete(final int depth) {
        if (depth < 0) {
            return;
        }

        final I item = items[depth];

        if (item == null) {
            return;
        }

        final boolean notifyDelete = listener != null ?
                listener.beforeDelete(this, depth) : false;

        final int tail = currentSize - depth - 1;

        if (tail > 0) {
            System.arraycopy(items, depth + 1, items, depth, tail);
        }

        currentSize--;

        try {
            if (notifyDelete) {
                listener.onDelete(this, depth, item);
            }
        } finally {
            pool.release(item);
        }
    }
}