package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.dfp.Decimal;

public interface MarketDataProcessor {

    L2BookProcessor     onBookSnapshot(String instrument, long timestamp);

    L2BookProcessor     onBookUpdate(String instrument, long timestamp);

    void                onTrade(String instrument, long timestamp, @Decimal long price, @Decimal long size);

}
