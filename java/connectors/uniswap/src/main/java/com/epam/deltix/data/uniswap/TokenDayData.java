package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class TokenDayData implements Updatable {
    private String id;
    private String date;
    private String tokenId;
    private String tokenSymbol;
    private String volume;
    private String volumeUSD;
    private String untrackedVolumeUSD;
    private String totalValueLocked;
    private String priceUSD;
    private String feesUSD;
    private String open;
    private String high;
    private String low;
    private String close;

    @Override
    public String getTbSymbol() {
        return tokenSymbol;
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
    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public boolean updateDate(final String date) {
        if (Util.equals(this.date, date)) {
            return false;
        }
        this.date = date;
        return true;
    }

    @SchemaElement()
    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(final String tokenId) {
        this.tokenId = tokenId;
    }

    public boolean updateTokenId(final String tokenId) {
        if (Util.equals(this.tokenId, tokenId)) {
            return false;
        }
        this.tokenId = tokenId;
        return true;
    }

    @SchemaElement()
    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public void setTokenSymbol(final String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }

    public boolean updateTokenSymbol(final String tokenSymbol) {
        if (Util.equals(this.tokenSymbol, tokenSymbol)) {
            return false;
        }
        this.tokenSymbol = tokenSymbol;
        return true;
    }

    @SchemaElement()
    public String getVolume() {
        return volume;
    }

    public void setVolume(final String volume) {
        this.volume = volume;
    }

    public boolean updateVolume(final String volume) {
        if (Util.equals(this.volume, volume)) {
            return false;
        }
        this.volume = volume;
        return true;
    }

    @SchemaElement()
    public String getVolumeUSD() {
        return volumeUSD;
    }

    public void setVolumeUSD(final String volumeUSD) {
        this.volumeUSD = volumeUSD;
    }

    public boolean updateVolumeUSD(final String volumeUSD) {
        if (Util.equals(this.volumeUSD, volumeUSD)) {
            return false;
        }
        this.volumeUSD = volumeUSD;
        return true;
    }

    @SchemaElement()
    public String getUntrackedVolumeUSD() {
        return untrackedVolumeUSD;
    }

    public void setUntrackedVolumeUSD(final String untrackedVolumeUSD) {
        this.untrackedVolumeUSD = untrackedVolumeUSD;
    }

    public boolean updateUntrackedVolumeUSD(final String untrackedVolumeUSD) {
        if (Util.equals(this.untrackedVolumeUSD, untrackedVolumeUSD)) {
            return false;
        }
        this.untrackedVolumeUSD = untrackedVolumeUSD;
        return true;
    }

    @SchemaElement()
    public String getTotalValueLocked() {
        return totalValueLocked;
    }

    public void setTotalValueLocked(final String totalValueLocked) {
        this.totalValueLocked = totalValueLocked;
    }

    public boolean updateTotalValueLocked(final String totalValueLocked) {
        if (Util.equals(this.totalValueLocked, totalValueLocked)) {
            return false;
        }
        this.totalValueLocked = totalValueLocked;
        return true;
    }

    @SchemaElement()
    public String getPriceUSD() {
        return priceUSD;
    }

    public void setPriceUSD(final String priceUSD) {
        this.priceUSD = priceUSD;
    }

    public boolean updatePriceUSD(final String priceUSD) {
        if (Util.equals(this.priceUSD, priceUSD)) {
            return false;
        }
        this.priceUSD = priceUSD;
        return true;
    }

    @SchemaElement()
    public String getFeesUSD() {
        return feesUSD;
    }

    public void setFeesUSD(final String feesUSD) {
        this.feesUSD = feesUSD;
    }

    public boolean updateFeesUSD(final String feesUSD) {
        if (Util.equals(this.feesUSD, feesUSD)) {
            return false;
        }
        this.feesUSD = feesUSD;
        return true;
    }

    @SchemaElement()
    public String getOpen() {
        return open;
    }

    public void setOpen(final String open) {
        this.open = open;
    }

    public boolean updateOpen(final String open) {
        if (Util.equals(this.open, open)) {
            return false;
        }
        this.open = open;
        return true;
    }

    @SchemaElement()
    public String getHigh() {
        return high;
    }

    public void setHigh(final String high) {
        this.high = high;
    }

    public boolean updateHigh(final String high) {
        if (Util.equals(this.high, high)) {
            return false;
        }
        this.high = high;
        return true;
    }

    @SchemaElement()
    public String getLow() {
        return low;
    }

    public void setLow(final String low) {
        this.low = low;
    }

    public boolean updateLow(final String low) {
        if (Util.equals(this.low, low)) {
            return false;
        }
        this.low = low;
        return true;
    }

    @SchemaElement()
    public String getClose() {
        return close;
    }

    public void setClose(final String close) {
        this.close = close;
    }

    public boolean updateClose(final String close) {
        if (Util.equals(this.close, close)) {
            return false;
        }
        this.close = close;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;

        result |= updateId(from.getString("id"));
        result |= updateDate(from.getString("date"));

        final JsonObject token = from.getObject("token");
        result |= updateTokenId(token.getString("id"));
        result |= updateTokenSymbol(token.getString("symbol"));

        result |= updateVolume(from.getString("volume"));
        result |= updateVolumeUSD(from.getString("volumeUSD"));
        result |= updateUntrackedVolumeUSD(from.getString("untrackedVolumeUSD"));
        result |= updateTotalValueLocked(from.getString("totalValueLocked"));
        result |= updatePriceUSD(from.getString("priceUSD"));
        result |= updateFeesUSD(from.getString("feesUSD"));
        result |= updateOpen(from.getString("open"));
        result |= updateHigh(from.getString("high"));
        result |= updateLow(from.getString("low"));
        result |= updateClose(from.getString("close"));

        return result;
    }
}
