package com.epam.deltix.data.connectors.commons.l2;

/**
 * <p>Represents book which has two sides (bids and offers) and handles events for specific withInstrument. <br>
 * This interface provides access to served withInstrument and two sides of the book.</p>
 *
 * @param <I> type of items that are stored in this book
 * @param <E> type of event which can be applied to items of this book
 * @see L2Listener
 */
public interface InstrumentBooks<I extends BookItem<E>, E extends BookEvent> {

    /**
     * Returns encoded Id of source (see {@link com.epam.deltix.qsrv.hf.pub.ExchangeCodec})
     * @return Id of the source
     */
    long source();

    /**
     * Returns withInstrument which is served by this book.
     *
     * @return withInstrument which is served by this book
     */
    String symbol();

    /**
     * Returns bids side of this withInstrument book.
     *
     * @return Returns bids side of this withInstrument book
     */
    Book<I, E> bids();

    /**
     * Returns offers side of this withInstrument book
     *
     * @return offers side of this withInstrument book
     */
    Book<I, E> offers();
}