package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.dfp.Decimal;

public interface L2BookProcessor {

    void onQuote(@Decimal long price, @Decimal long size, boolean isAsk);

    void onFinish();

}
