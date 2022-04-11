package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.timebase.messages.universal.PackageHeader;
import com.epam.deltix.timebase.messages.universal.PackageType;
import com.epam.deltix.timebase.messages.universal.TradeEntry;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

public class TradeProducer {
    private final PackageHeader tradePackage = new PackageHeader();
    private final TradeEntry trade = new TradeEntry();

    private final MessageOutput output;

    public TradeProducer(final long exchangeId, final MessageOutput output) {
        this.output = output;

        tradePackage.setPackageType(PackageType.INCREMENTAL_UPDATE);
        tradePackage.setEntries(new ObjectArrayList<>());
        tradePackage.getEntries().add(trade);

        trade.setExchangeId(exchangeId);
    }

    public void onTrade(
            final CharSequence symbol,
            final @Decimal long price,
            final @Decimal long size)
    {
        tradePackage.setOriginalTimestamp(TimeStampedMessage.TIMESTAMP_UNKNOWN);
        tradePackage.setSymbol(symbol);

        trade.setPrice(price);
        trade.setSize(size);

        output.send(tradePackage);
    }

    public void onTrade(
        final long originalTimestamp,
        final CharSequence symbol,
        final @Decimal long price,
        final @Decimal long size)
    {
        tradePackage.setOriginalTimestamp(originalTimestamp);
        tradePackage.setSymbol(symbol);

        trade.setPrice(price);
        trade.setSize(size);

        output.send(tradePackage);
    }
}
