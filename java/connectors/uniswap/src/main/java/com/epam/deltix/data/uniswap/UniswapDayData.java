package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class UniswapDayData implements Updatable {
    private String id;
    private String date;
    private String volumeETH;
    private String volumeUSD;
    private String volumeUSDUntracked;
    private String feesUSD;
    private String txCount;
    private String tvlUSD;

    @Override
    public String getTbSymbol() {
        return "uniswapDayData";
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
    public String getVolumeETH() {
        return volumeETH;
    }

    public void setVolumeETH(final String volumeETH) {
        this.volumeETH = volumeETH;
    }

    public boolean updateVolumeETH(final String volumeETH) {
        if (Util.equals(this.volumeETH, volumeETH)) {
            return false;
        }
        this.volumeETH = volumeETH;
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
    public String getVolumeUSDUntracked() {
        return volumeUSDUntracked;
    }

    public void setVolumeUSDUntracked(final String volumeUSDUntracked) {
        this.volumeUSDUntracked = volumeUSDUntracked;
    }

    public boolean updateVolumeUSDUntracked(final String volumeUSDUntracked) {
        if (Util.equals(this.volumeUSDUntracked, volumeUSDUntracked)) {
            return false;
        }
        this.volumeUSDUntracked = volumeUSDUntracked;
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
    public String getTxCount() {
        return txCount;
    }

    public void setTxCount(final String txCount) {
        this.txCount = txCount;
    }

    public boolean updateTxCount(final String txCount) {
        if (Util.equals(this.txCount, txCount)) {
            return false;
        }
        this.txCount = txCount;
        return true;
    }

    @SchemaElement()
    public String getTvlUSD() {
        return tvlUSD;
    }

    public void setTvlUSD(final String tvlUSD) {
        this.tvlUSD = tvlUSD;
    }

    public boolean updateTvlUSD(final String tvlUSD) {
        if (Util.equals(this.tvlUSD, tvlUSD)) {
            return false;
        }
        this.tvlUSD = tvlUSD;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        //id
        result |= updateId(from.getString("id"));
        result |= updateDate(from.getString("date"));
        result |= updateVolumeETH(from.getString("volumeETH"));
        result |= updateVolumeUSD(from.getString("volumeUSD"));
        result |= updateVolumeUSDUntracked(from.getString("volumeUSDUntracked"));
        result |= updateFeesUSD(from.getString("feesUSD"));
        result |= updateTxCount(from.getString("txCount"));
        result |= updateTvlUSD(from.getString("tvlUSD"));

        return result;
    }
}
