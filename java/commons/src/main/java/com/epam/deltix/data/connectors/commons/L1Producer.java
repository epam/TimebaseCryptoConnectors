package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

public class L1Producer {
    private final PackageHeader l1Package = new PackageHeader();
    private final ObjectArrayList<BaseEntryInfo> entries = new ObjectArrayList<>();
    private final L1Entry bid = new L1Entry();
    private final L1Entry offer = new L1Entry();

    private final MessageOutput output;

    public L1Producer(final long exchangeId, final MessageOutput output) {
        this.output = output;

        l1Package.setEntries(entries);
        bid.setSide(QuoteSide.BID);
        bid.setExchangeId(exchangeId);
        offer.setSide(QuoteSide.ASK);
        offer.setExchangeId(exchangeId);
        l1Package.getEntries().add(bid);
        l1Package.getEntries().add(offer);
    }

    public void onSnapshot(
        final CharSequence symbol,
        final long originalTimestamp,
        final @Decimal long bidPrice,
        final @Decimal long bidSize,
        final @Decimal long offerPrice,
        final @Decimal long offerSize) {

        l1Package.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN);
        l1Package.setOriginalTimestamp(originalTimestamp);
        l1Package.setSymbol(symbol);
        l1Package.setPackageType(PackageType.VENDOR_SNAPSHOT);

        bid.setPrice(bidPrice);
        bid.setSize(bidSize);

        offer.setPrice(offerPrice);
        offer.setSize(offerSize);

        output.send(l1Package);
    }

}
