package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.ObjectPool;
import com.epam.deltix.timebase.messages.universal.BaseEntryInfo;
import com.epam.deltix.timebase.messages.universal.BookResetEntry;
import com.epam.deltix.timebase.messages.universal.BookResetEntryInterface;
import com.epam.deltix.timebase.messages.universal.BookUpdateAction;
import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.L2EntryNew;
import com.epam.deltix.timebase.messages.universal.L2EntryNewInterface;
import com.epam.deltix.timebase.messages.universal.L2EntryUpdate;
import com.epam.deltix.timebase.messages.universal.L2EntryUpdateInterface;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 *
 * @param <I>
 * @param <E>
 */
public class L2Producer<I extends BookItem<E>, E extends BookEvent>
        extends L2ListenerAdapter<I, E> {

    private final PackageHeader l2Package = new PackageHeader();
    private final ObjectArrayList<BaseEntryInfo> l2PackageEntries = new ObjectArrayList<>(128);

    private final BookResetEntryInterface resetAsksEntry = new BookResetEntry();
    private final BookResetEntryInterface resetBidsEntry = new BookResetEntry();

    private final ObjectPool<L2EntryNewInterface> newEntryPool = new ObjectPool<>(L2EntryNew::new);
    private final ObjectPool<L2EntryUpdateInterface> updateEntryPool = new ObjectPool<>(L2EntryUpdate::new);

    private final MessageOutput output;

    private long exchangeId;

    public L2Producer(final MessageOutput output) {
        this.output = output;

        l2Package.setEntries(l2PackageEntries);

        resetAsksEntry.setSide(QuoteSide.ASK);
        resetAsksEntry.setModelType(DataModelType.LEVEL_TWO);
        resetBidsEntry.setSide(QuoteSide.BID);
        resetBidsEntry.setModelType(DataModelType.LEVEL_TWO);
    }

    @Override
    public void onSnapshotStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        l2Package.setPackageType(PackageType.VENDOR_SNAPSHOT);

        exchangeId = instrumentBook.source();

        l2Package.setSymbol(instrumentBook.symbol());
        l2Package.setOriginalTimestamp(originalTimestamp);

        cleanupL2PackageEntries();
    }

    @Override
    public void onIncrementStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        l2Package.setPackageType(PackageType.INCREMENTAL_UPDATE);

        exchangeId = instrumentBook.source();

        l2Package.setSymbol(instrumentBook.symbol());
        l2Package.setOriginalTimestamp(originalTimestamp);

        cleanupL2PackageEntries();
    }

    @Override
    public void onNew(final Book<I, E> book, final int depth, final I item) {
        final L2EntryNewInterface insert = newEntryPool.borrow();

        insert.setExchangeId(exchangeId);
        insert.setLevel((short) depth);
        insert.setSide(book.isOffer ? QuoteSide.ASK : QuoteSide.BID);
        insert.setPrice(item.getPrice());
        insert.setSize(item.getSize());

        l2Package.getEntries().add(insert);
    }

    @Override
    public void onUpdate(final Book<I, E> book, final int depth, final I item) {
        final L2EntryUpdateInterface update = updateEntryPool.borrow();

        update.setExchangeId(exchangeId);
        update.setAction(BookUpdateAction.UPDATE);
        update.setLevel((short) depth);
        update.setSide(book.isOffer ? QuoteSide.ASK : QuoteSide.BID);
        update.setPrice(item.getPrice());
        update.setSize(item.getSize());

        l2Package.getEntries().add(update);
    }

    @Override
    public void onDelete(final Book<I, E> book, final int depth, final I item) {
        final L2EntryUpdateInterface delete = updateEntryPool.borrow();

        delete.setExchangeId(exchangeId);
        delete.setAction(BookUpdateAction.DELETE);
        delete.setLevel((short) depth);
        delete.setSide(book.isOffer ? QuoteSide.ASK : QuoteSide.BID);
        delete.setPrice(item.getPrice());
        delete.setSize(item.getSize());

        l2Package.getEntries().add(delete);
    }

    @Override
    public void onFinished(final InstrumentBooks<I, E> instrumentBook) {
        if (l2Package.getPackageType() == PackageType.VENDOR_SNAPSHOT &&
                l2PackageEntries.isEmpty()) {
            l2PackageEntries.add(resetAsksEntry);
            l2PackageEntries.add(resetBidsEntry);
        }

        output.send(l2Package);
    }

    private void cleanupL2PackageEntries() {
        for (int i = 0; i < l2PackageEntries.size(); i++) {
            final BaseEntryInfo entry = l2PackageEntries.get(i);

            if (entry instanceof L2EntryNewInterface) {
                newEntryPool.release((L2EntryNewInterface) entry);
                continue;
            }

            if (entry instanceof L2EntryUpdateInterface) {
                updateEntryPool.release((L2EntryUpdateInterface) entry);
                continue;
            }
        }
        l2PackageEntries.clear();
    }
}
