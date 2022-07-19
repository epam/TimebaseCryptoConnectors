package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Factory implements Updatable {
    // factory address
    private String id;
    //amount of pools created
    private String poolCount;
    //amount of transactions all time
    private String txCount;
    //total volume all time in derived USD
    private String totalVolumeUSD;
    //total volume all time in derived ETH
    private String totalVolumeETH;
    //total swap fees all time in USD
    private String totalFeesUSD;
    //total swap fees all time in USD
    private String totalFeesETH;
    //all volume even through less reliable USD values
    private String untrackedVolumeUSD;
    //TVL derived in USD
    private String totalValueLockedUSD;
    //TVL derived in ETH
    private String totalValueLockedETH;
    // TVL derived in USD untracked
    private String totalValueLockedUSDUntracked;
    //TVL derived in ETH untracked
    private String totalValueLockedETHUntracked;
    //current owner of the factory
    private String owner;

    @Override
    public String getTbSymbol() {
        return "factory";
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
    public String getPoolCount() {
        return poolCount;
    }

    public void setPoolCount(final String poolCount) {
        this.poolCount = poolCount;
    }

    public boolean updatePoolCount(final String poolCount) {
        if (Util.equals(this.poolCount, poolCount)) {
            return false;
        }
        this.poolCount = poolCount;
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
    public String getTotalVolumeUSD() {
        return totalVolumeUSD;
    }

    public void setTotalVolumeUSD(final String totalVolumeUSD) {
        this.totalVolumeUSD = totalVolumeUSD;
    }

    public boolean updateTotalVolumeUSD(final String totalVolumeUSD) {
        if (Util.equals(this.totalVolumeUSD, totalVolumeUSD)) {
            return false;
        }
        this.totalVolumeUSD = totalVolumeUSD;
        return true;
    }

    @SchemaElement()
    public String getTotalVolumeETH() {
        return totalVolumeETH;
    }

    public void setTotalVolumeETH(final String totalVolumeETH) {
        this.totalVolumeETH = totalVolumeETH;
    }

    public boolean updateTotalVolumeETH(final String totalVolumeETH) {
        if (Util.equals(this.totalVolumeETH, totalVolumeETH)) {
            return false;
        }
        this.totalVolumeETH = totalVolumeETH;
        return true;
    }

    @SchemaElement()
    public String getTotalFeesUSD() {
        return totalFeesUSD;
    }

    public void setTotalFeesUSD(final String totalFeesUSD) {
        this.totalFeesUSD = totalFeesUSD;
    }

    public boolean updateTotalFeesUSD(final String totalFeesUSD) {
        if (Util.equals(this.totalFeesUSD, totalFeesUSD)) {
            return false;
        }
        this.totalFeesUSD = totalFeesUSD;
        return true;
    }

    @SchemaElement()
    public String getTotalFeesETH() {
        return totalFeesETH;
    }

    public void setTotalFeesETH(final String totalFeesETH) {
        this.totalFeesETH = totalFeesETH;
    }

    public boolean updateTotalFeesETH(final String totalFeesETH) {
        if (Util.equals(this.totalFeesETH, totalFeesETH)) {
            return false;
        }
        this.totalFeesETH = totalFeesETH;
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
    public String getTotalValueLockedUSD() {
        return totalValueLockedUSD;
    }

    public void setTotalValueLockedUSD(final String totalValueLockedUSD) {
        this.totalValueLockedUSD = totalValueLockedUSD;
    }

    public boolean updateTotalValueLockedUSD(final String totalValueLockedUSD) {
        if (Util.equals(this.totalValueLockedUSD, totalValueLockedUSD)) {
            return false;
        }
        this.totalValueLockedUSD = totalValueLockedUSD;
        return true;
    }

    @SchemaElement()
    public String getTotalValueLockedETH() {
        return totalValueLockedETH;
    }

    public void setTotalValueLockedETH(final String totalValueLockedETH) {
        this.totalValueLockedETH = totalValueLockedETH;
    }

    public boolean updateTotalValueLockedETH(final String totalValueLockedETH) {
        if (Util.equals(this.totalValueLockedETH, totalValueLockedETH)) {
            return false;
        }
        this.totalValueLockedETH = totalValueLockedETH;
        return true;
    }

    @SchemaElement()
    public String getTotalValueLockedUSDUntracked() {
        return totalValueLockedUSDUntracked;
    }

    public void setTotalValueLockedUSDUntracked(final String totalValueLockedUSDUntracked) {
        this.totalValueLockedUSDUntracked = totalValueLockedUSDUntracked;
    }

    public boolean updateTotalValueLockedUSDUntracked(final String totalValueLockedUSDUntracked) {
        if (Util.equals(this.totalValueLockedUSDUntracked, totalValueLockedUSDUntracked)) {
            return false;
        }
        this.totalValueLockedUSDUntracked = totalValueLockedUSDUntracked;
        return true;
    }

    @SchemaElement()
    public String getTotalValueLockedETHUntracked() {
        return totalValueLockedETHUntracked;
    }

    public void setTotalValueLockedETHUntracked(final String totalValueLockedETHUntracked) {
        this.totalValueLockedETHUntracked = totalValueLockedETHUntracked;
    }

    public boolean updateTotalValueLockedETHUntracked(final String totalValueLockedETHUntracked) {
        if (Util.equals(this.totalValueLockedETHUntracked, totalValueLockedETHUntracked)) {
            return false;
        }
        this.totalValueLockedETHUntracked = totalValueLockedETHUntracked;
        return true;
    }

    @SchemaElement()
    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public boolean updateOwner(final String owner) {
        if (Util.equals(this.owner, owner)) {
            return false;
        }
        this.owner = owner;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        result |= updateId(from.getString("id"));
        result |= updatePoolCount(from.getString("poolCount"));
        result |= updateTxCount(from.getString("txCount"));
        result |= updateTotalVolumeUSD(from.getString("totalVolumeUSD"));
        result |= updateTotalVolumeETH(from.getString("totalVolumeETH"));
        result |= updateTotalFeesUSD(from.getString("totalFeesUSD"));
        result |= updateTotalFeesETH(from.getString("totalFeesETH"));
        result |= updateUntrackedVolumeUSD(from.getString("untrackedVolumeUSD"));
        result |= updateTotalValueLockedUSD(from.getString("totalValueLockedUSD"));
        result |= updateTotalValueLockedETH(from.getString("totalValueLockedETH"));
        result |= updateTotalValueLockedUSDUntracked(from.getString("totalValueLockedUSDUntracked"));
        result |= updateTotalValueLockedETHUntracked(from.getString("totalValueLockedETHUntracked"));
        result |= updateOwner(from.getString("owner"));

        return result;
    }

    @Override
    public String toString() {
        return "Factory{" +
                "id='" + id + '\'' +
                "poolCount='" + poolCount + '\'' +
                "txCount='" + txCount + '\'' +
                "totalVolumeUSD='" + totalVolumeUSD + '\'' +
                "totalVolumeETH='" + totalVolumeETH + '\'' +
                "totalFeesUSD='" + totalFeesUSD + '\'' +
                "totalFeesETH='" + totalFeesETH + '\'' +
                "untrackedVolumeUSD='" + untrackedVolumeUSD + '\'' +
                "totalValueLockedUSD='" + totalValueLockedUSD + '\'' +
                "totalValueLockedETH='" + totalValueLockedETH + '\'' +
                "totalValueLockedUSDUntracked='" + totalValueLockedUSDUntracked + '\'' +
                "totalValueLockedETHUntracked='" + totalValueLockedETHUntracked + '\'' +
                "owner='" + owner + '\'' +
                '}';
    }
}
