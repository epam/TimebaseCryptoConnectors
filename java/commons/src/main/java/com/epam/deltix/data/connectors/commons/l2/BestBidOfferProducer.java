package com.epam.deltix.data.connectors.commons.l2;

import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.timebase.messages.universal.BookResetEntry;
import com.epam.deltix.timebase.messages.universal.DataModelType;
import com.epam.deltix.timebase.messages.universal.L1Entry;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.QuoteSide;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

public class BestBidOfferProducer<I extends BookItem<E>, E extends BookEvent>
        extends L2ListenerAdapter<I, E> {
    private final MessageOutput output;

    private final PackageHeader bboPackage = new PackageHeader();
    private final L1Entry bid = new L1Entry();
    private final L1Entry offer = new L1Entry();
    private final BookResetEntry resetBid = new BookResetEntry();
    private final BookResetEntry resetOffer = new BookResetEntry();

    private long exchangeId;

    public BestBidOfferProducer(final MessageOutput output) {
        this.output = output;

        bboPackage.setPackageType(PackageType.VENDOR_SNAPSHOT);
        bboPackage.setEntries(new ObjectArrayList<>());

        bid.setSide(QuoteSide.BID);

        offer.setSide(QuoteSide.ASK);

        resetBid.setModelType(DataModelType.LEVEL_ONE);
        resetBid.setSide(QuoteSide.BID);

        resetOffer.setModelType(DataModelType.LEVEL_ONE);
        resetOffer.setSide(QuoteSide.ASK);
    }

    @Override
    public void onSnapshotStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        exchangeId = instrumentBook.source();

        bboPackage.setSymbol(instrumentBook.symbol());
        bboPackage.setOriginalTimestamp(originalTimestamp);

        bboPackage.getEntries().clear();
    }

    @Override
    public void onIncrementStarted(final InstrumentBooks<I, E> instrumentBook, final long timestamp, final long originalTimestamp) {
        exchangeId = instrumentBook.source();

        bboPackage.setSymbol(instrumentBook.symbol());
        bboPackage.setOriginalTimestamp(originalTimestamp);

        bboPackage.getEntries().clear();
    }

    @Override
    public void onTopBidUpdated(final Book<I, E> book) {
        if (book.isEmpty()) {
            resetBid.setExchangeId(exchangeId);
            bboPackage.getEntries().add(resetBid);
        } else {
            bid.setExchangeId(exchangeId);
            final I best = book.getItem(0);
            bid.setPrice(best.getPrice());
            bid.setSize(best.getSize());
            bboPackage.getEntries().add(bid);
        }
    }

    @Override
    public void onTopAskUpdated(final Book<I, E> book) {
        if (book.isEmpty()) {
            resetOffer.setExchangeId(exchangeId);
            bboPackage.getEntries().add(resetOffer);
        } else {
            offer.setExchangeId(exchangeId);
            final I best = book.getItem(0);
            offer.setPrice(best.getPrice());
            offer.setSize(best.getSize());
            bboPackage.getEntries().add(offer);
        }
    }

    @Override
    public void onFinished(final InstrumentBooks<I, E> instrumentBook) {
        if (!bboPackage.hasEntries() ||
                bboPackage.getEntries().isEmpty()) {
            return;
        }
        output.send(bboPackage);
    }
}
