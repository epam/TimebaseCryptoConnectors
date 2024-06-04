package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.ObjectPool;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
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
import com.epam.deltix.util.collections.CharSequenceToLongMap;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.time.TimeKeeper;

/**
 * L2 Data Producer
 *
 */
public class L2Producer<I extends BookItem<E>, E extends BookEvent>
        extends L2ListenerAdapter<I, E> {

    private static final int OFFER_MASK = 1;
    private static final int BID_MASK = 2;

    private final PackageHeader l2Package = new PackageHeader();
    private final ObjectArrayList<BaseEntryInfo> entries = new ObjectArrayList<>(128);

    private final BookResetEntryInterface resetAsksEntry = new BookResetEntry();
    private final BookResetEntryInterface resetBidsEntry = new BookResetEntry();

    private final ObjectPool<L2EntryNewInterface> newEntryPool = new ObjectPool<>(L2EntryNew::new);
    private final ObjectPool<L2EntryUpdateInterface> updateEntryPool = new ObjectPool<>(L2EntryUpdate::new);

    private final CharSequenceToLongMap lastSnapshotTime = new CharSequenceToLongMap();

    private final MessageOutput output;

    private long exchangeId;

    private int updatedSides; // a mask for bids and asks processed

    public L2Producer(final MessageOutput output) {
        this.output = output;

        l2Package.setEntries(entries);

        resetAsksEntry.setSide(QuoteSide.ASK);
        resetAsksEntry.setModelType(DataModelType.LEVEL_TWO);
        resetBidsEntry.setSide(QuoteSide.BID);
        resetBidsEntry.setModelType(DataModelType.LEVEL_TWO);
    }

    @Override
    public void onSnapshotStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        exchangeId = instrumentBook.source();

        l2Package.setSymbol(instrumentBook.symbol());
        l2Package.setPackageType(PackageType.VENDOR_SNAPSHOT);
        l2Package.setOriginalTimestamp(originalTimestamp);

        cleanupL2PackageEntries();

        lastSnapshotTime.put(instrumentBook.symbol(), TimeKeeper.currentTime);
    }

    @Override
    public void onIncrementStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        exchangeId = instrumentBook.source();

        l2Package.setSymbol(instrumentBook.symbol());

        final long lastTime = lastSnapshotTime.get(instrumentBook.symbol(), 0);
        if (TimeKeeper.currentTime - lastTime > 10_000 /* TODO: configurable? */) {
            sendPeriodicalSnapshot(instrumentBook);
            lastSnapshotTime.put(instrumentBook.symbol(), TimeKeeper.currentTime); // store the most actual time
        }

        l2Package.setPackageType(PackageType.INCREMENTAL_UPDATE);
        l2Package.setOriginalTimestamp(originalTimestamp);

        cleanupL2PackageEntries();
    }

    private void sendPeriodicalSnapshot(final InstrumentBooks<I, E> instrumentBook) {
        l2Package.setPackageType(PackageType.PERIODICAL_SNAPSHOT);
        l2Package.setOriginalTimestamp(TimeConstants.TIMESTAMP_UNKNOWN);

        cleanupL2PackageEntries();

        final int maxOutputSize = (instrumentBook instanceof L2Processor) ?
                ((L2Processor) instrumentBook).outputBookSize() : Integer.MAX_VALUE;

        final Book<I, E> bids = instrumentBook.bids();
        for (int i = 0; i < Math.min(maxOutputSize, bids.size()); i++) {
            onNew(bids, i, bids.getItem(i));
        }
        final Book<I, E> offers = instrumentBook.offers();
        for (int i = 0; i < Math.min(maxOutputSize, offers.size()); i++) {
            onNew(offers, i, offers.getItem(i));
        }

        output.send(l2Package);
    }

    @Override
    public void onNew(final Book<I, E> book, final int depth, final I item) {
        final L2EntryNewInterface insert = newEntryPool.borrow();

        insert.setExchangeId(exchangeId);
        insert.setLevel((short) depth);
        insert.setPrice(item.getPrice());
        insert.setSize(item.getSize());
        if (book.isOffer) {
            insert.setSide(QuoteSide.ASK);
            updatedSides |= OFFER_MASK;
        } else {
            insert.setSide(QuoteSide.BID);
            updatedSides |= BID_MASK;
        }

        entries.add(insert);
    }

    @Override
    public void onUpdate(final Book<I, E> book, final int depth, final I item) {
        final L2EntryUpdateInterface update = updateEntryPool.borrow();

        update.setExchangeId(exchangeId);
        update.setAction(BookUpdateAction.UPDATE);
        update.setLevel((short) depth);
        update.setPrice(item.getPrice());
        update.setSize(item.getSize());
        if (book.isOffer) {
            update.setSide(QuoteSide.ASK);
            updatedSides |= OFFER_MASK;
        } else {
            update.setSide(QuoteSide.BID);
            updatedSides |= BID_MASK;
        }

        entries.add(update);
    }

    @Override
    public void onDelete(final Book<I, E> book, final int depth, final I item) {
        final L2EntryUpdateInterface delete = updateEntryPool.borrow();

        delete.setExchangeId(exchangeId);
        delete.setAction(BookUpdateAction.DELETE);
        delete.setLevel((short) depth);
        delete.setPrice(item.getPrice());
        delete.setSize(item.getSize());
        if (book.isOffer) {
            delete.setSide(QuoteSide.ASK);
            updatedSides |= OFFER_MASK;
        } else {
            delete.setSide(QuoteSide.BID);
            updatedSides |= BID_MASK;
        }

        entries.add(delete);
    }

    @Override
    public void onFinished(final InstrumentBooks<I, E> instrumentBook) {
        switch (l2Package.getPackageType()) {
            case VENDOR_SNAPSHOT:
            case PERIODICAL_SNAPSHOT:
                if ((updatedSides & OFFER_MASK) == 0) {
                    resetAsksEntry.setExchangeId(exchangeId);
                    entries.add(resetAsksEntry);
                }
                if ((updatedSides & BID_MASK) == 0) {
                    resetBidsEntry.setExchangeId(exchangeId);
                    entries.add(resetBidsEntry);
                }
                break;
        }

        output.send(l2Package);
    }

    private void cleanupL2PackageEntries() {
        for (int i = 0; i < entries.size(); i++) {
            final BaseEntryInfo entry = entries.get(i);

            if (entry instanceof L2EntryNewInterface) {
                newEntryPool.release((L2EntryNewInterface) entry);
                continue;
            }

            if (entry instanceof L2EntryUpdateInterface) {
                updateEntryPool.release((L2EntryUpdateInterface) entry);
                continue;
            }
        }
        entries.clear();

        updatedSides = 0;
    }
}
