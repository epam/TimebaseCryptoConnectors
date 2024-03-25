package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Bundle implements Updatable {
    // bundle address
    private String id;
    //price of ETH in usd
    private String ethPriceUSD;

    @Override
    public String getTbSymbol() {
        return "bundle";
    }

    @SchemaElement()
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public boolean updateId(final String id) {
        if (Util.equals(this.id, id)) {
            return false;
        }
        this.id = id;
        return true;
    }

    @SchemaElement()
    public String getEthPriceUSD() {
        return ethPriceUSD;
    }

    public void setEthPriceUSD(final String ethPriceUSD) {
        this.ethPriceUSD = ethPriceUSD;
    }

    public boolean updateEthPriceUSD(final String ethPriceUSD) {
        if (Util.equals(this.ethPriceUSD, ethPriceUSD)) {
            return false;
        }
        this.ethPriceUSD = ethPriceUSD;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        result |= updateId(from.getString("id"));
        result |= updateEthPriceUSD(from.getString("ethPriceUSD"));
        return result;
    }

    @Override
    public String toString() {
        return "Bundle{" +
                "id='" + id + '\'' +
                ", ethPriceUSD='" + ethPriceUSD + '\'' +
                '}';
    }
}
