package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.TradeEntry;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

public class TradeProducer {
    private final PackageHeader tradePackage = new PackageHeader();
    private final TradeEntry trade = new TradeEntry();

    private final long sourceExchangeId;

    private final AlphanumericCodec alphanumericCodec = new AlphanumericCodec(10);

    private final MessageOutput output;

    public TradeProducer(final long exchangeId, final MessageOutput output) {
        this.sourceExchangeId = exchangeId;
        this.output = output;

        tradePackage.setSourceId(exchangeId);
        tradePackage.setPackageType(PackageType.INCREMENTAL_UPDATE);
        tradePackage.setEntries(new ObjectArrayList<>());
        tradePackage.getEntries().add(trade);
    }

    public void onTrade(
            final CharSequence symbol,
            final @Decimal long price,
            final @Decimal long size) {

        onTrade(TimeStampedMessage.TIMESTAMP_UNKNOWN, symbol, price, size);
    }

    public void onTrade(
        final long originalTimestamp,
        final CharSequence symbol,
        final @Decimal long price,
        final @Decimal long size) {

        onTrade(originalTimestamp, symbol, price, size, null);
    }

    public void onTrade(
        final long originalTimestamp,
        final CharSequence symbol,
        final @Decimal long price,
        final @Decimal long size,
        final AggressorSide side) {

        onTrade(originalTimestamp, symbol, price, size, side, sourceExchangeId, null);
    }

    public void onTrade(
        final long originalTimestamp,
        final CharSequence symbol,
        final @Decimal long price,
        final @Decimal long size,
        final AggressorSide side,
        final String exchange,
        final String condition) {

        long exchangeId = exchange != null ? alphanumericCodec.encodeToLong(exchange) : TypeConstants.EXCHANGE_NULL;
        onTrade(
            originalTimestamp, symbol,
            price, size, side, exchangeId,
            condition
        );
    }

    public void onTrade(
        final long originalTimestamp,
        final CharSequence symbol,
        final @Decimal long price,
        final @Decimal long size,
        final AggressorSide side,
        final long exchangeId,
        final String condition) {

        tradePackage.setOriginalTimestamp(originalTimestamp);
        tradePackage.setSymbol(symbol);

        trade.setPrice(price);
        trade.setSize(size);
        trade.setExchangeId(exchangeId);
        trade.setCondition(condition);
        trade.setSide(side);

        output.send(tradePackage);
    }
}
