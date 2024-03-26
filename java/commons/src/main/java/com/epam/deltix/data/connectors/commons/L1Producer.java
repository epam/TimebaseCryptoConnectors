package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

public class L1Producer {
    private final PackageHeader l1Package = new PackageHeader();
    private final ObjectArrayList<BaseEntryInfo> entries = new ObjectArrayList<>();
    private final L1Entry bid = new L1Entry();
    private final L1Entry offer = new L1Entry();

    private final long sourceExchangeId;

    private final AlphanumericCodec alphanumericCodec = new AlphanumericCodec(10);

    private final MessageOutput output;

    public L1Producer(final long exchangeId, final MessageOutput output) {
        this.sourceExchangeId = exchangeId;
        this.output = output;

        bid.setSide(QuoteSide.BID);
        offer.setSide(QuoteSide.ASK);

        l1Package.setSourceId(exchangeId);
        l1Package.setEntries(entries);
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

        onSnapshot(
            symbol, originalTimestamp,
            bidPrice, bidSize, sourceExchangeId,
            offerPrice, offerSize, sourceExchangeId
        );
    }

    public void onSnapshot(
        final CharSequence symbol,
        final long originalTimestamp,
        final @Decimal long bidPrice,
        final @Decimal long bidSize,
        final String bidExchange,
        final @Decimal long offerPrice,
        final @Decimal long offerSize,
        final String offerExchange) {

        long bidExchangeId = bidExchange != null ?
            alphanumericCodec.encodeToLong(bidExchange) :
            TypeConstants.EXCHANGE_NULL;
        long offerExchangeId = offerExchange != null ?
            alphanumericCodec.encodeToLong(offerExchange) :
            TypeConstants.EXCHANGE_NULL;

        onSnapshot(
            symbol, originalTimestamp,
            bidPrice, bidSize, bidExchangeId,
            offerPrice, offerSize, offerExchangeId
        );
    }

    public void onSnapshot(
        final CharSequence symbol,
        final long originalTimestamp,
        final @Decimal long bidPrice,
        final @Decimal long bidSize,
        final long bidExchangeId,
        final @Decimal long offerPrice,
        final @Decimal long offerSize,
        final long offerExchangeId) {

        l1Package.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN);
        l1Package.setOriginalTimestamp(originalTimestamp);
        l1Package.setSymbol(symbol);
        l1Package.setPackageType(PackageType.VENDOR_SNAPSHOT);

        bid.setPrice(bidPrice);
        bid.setSize(bidSize);
        bid.setExchangeId(bidExchangeId);

        offer.setPrice(offerPrice);
        offer.setSize(offerSize);
        offer.setExchangeId(offerExchangeId);

        output.send(l1Package);
    }
}
