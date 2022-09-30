package com.epam.deltix.data.connectors.uniswap.quoter.quoter;

import org.json.JSONObject;

import java.math.BigDecimal;

public interface Quoter {
    BigDecimal getAsk(final BigDecimal amount);

    BigDecimal getBid(final BigDecimal amount);

    JSONObject buildLevel2(final BigDecimal maxAmount, final int levelsQuantity);
}
