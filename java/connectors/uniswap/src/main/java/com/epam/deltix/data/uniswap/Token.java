package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Token implements Updatable {
    // token address
    private String id;
    // token symbol
    private String symbol;
    // token name
    private String name;
    // token decimals
    private String decimals;
    // token total supply
    private String totalSupply;
    // volume in token units
    private String volume;
    // volume in derived USD
    private String volumeUSD;
    // volume in USD even on pools with less reliable USD values
    private String untrackedVolumeUSD;
    // fees in USD
    private String feesUSD;
    // transactions across all pools that include this token
    private String txCount;
    // number of pools containing this token
    private String poolCount;
    // liquidity across all pools in token units
    private String totalValueLocked;
    // liquidity across all pools in derived USD
    private String totalValueLockedUSD;
    // TVL derived in USD untracked
    private String totalValueLockedUSDUntracked;
    // derived price in ETH
    private String derivedETH;
    // pools token is in that are white listed for USD pricing
    private String whitelistPoolsIds;
    private String tokenDayDataIds;

    public Token() {
    }

    @Override
    public String getTbSymbol() {
        return symbol;
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
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public boolean updateSymbol(final String symbol) {
        if (Util.equals(this.symbol, symbol)) {
            return false;
        }
        this.symbol = symbol;
        return true;
    }

    @SchemaElement()
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean updateName(final String name) {
        if (Util.equals(this.name, name)) {
            return false;
        }
        this.name = name;
        return true;
    }

    @SchemaElement()
    public String getDecimals() {
        return decimals;
    }

    public void setDecimals(final String decimals) {
        this.decimals = decimals;
    }

    public boolean updateDecimals(final String decimals) {
        if (Util.equals(this.decimals, decimals)) {
            return false;
        }
        this.decimals = decimals;
        return true;
    }

    @SchemaElement()
    public String getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(final String totalSupply) {
        this.totalSupply = totalSupply;
    }

    public boolean updateTotalSupply(final String totalSupply) {
        if (Util.equals(this.totalSupply, totalSupply)) {
            return false;
        }
        this.totalSupply = totalSupply;
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
    public String getDerivedETH() {
        return derivedETH;
    }

    public void setDerivedETH(final String derivedETH) {
        this.derivedETH = derivedETH;
    }

    public boolean updateDerivedETH(final String derivedETH) {
        if (Util.equals(this.derivedETH, derivedETH)) {
            return false;
        }
        this.derivedETH = derivedETH;
        return true;
    }

    @SchemaElement
    public String getWhitelistPoolsIds() {
        return whitelistPoolsIds;
    }

    public void setWhitelistPoolsIds(final String whitelistPoolsIds) {
        this.whitelistPoolsIds = whitelistPoolsIds;
    }

    public boolean updateWhitelistPoolsIds(final String whitelistPoolsIds) {
        if (Util.equals(this.whitelistPoolsIds, whitelistPoolsIds)) {
            return false;
        }
        this.whitelistPoolsIds = whitelistPoolsIds;
        return true;
    }

    @SchemaElement
    public String getTokenDayDataIds() {
        return tokenDayDataIds;
    }

    public void setTokenDayDataIds(final String tokenDayDataIds) {
        this.tokenDayDataIds = tokenDayDataIds;
    }

    public boolean updateTokenDayDataIds(final String tokenDayDataIds) {
        if (Util.equals(this.tokenDayDataIds, tokenDayDataIds)) {
            return false;
        }
        this.tokenDayDataIds = tokenDayDataIds;
        return true;
    }

    private String parseJsonArray(JsonArray array) {
        StringBuilder itemIdsList = new StringBuilder();
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                final JsonObject item = array.getObject(i);
                String id = item.getString("id");
                itemIdsList.append(id);
                if (i != array.size() - 1) {
                    itemIdsList.append(",");
                }
            }
        }

        return itemIdsList.toString();
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        result |= updateId(from.getString("id"));
        result |= updateSymbol(from.getString("symbol"));
        result |= updateName(from.getString("name"));
        result |= updateDecimals(from.getString("decimals"));
        result |= updateTotalSupply(from.getString("totalSupply"));
        result |= updateVolume(from.getString("volume"));
        result |= updateVolumeUSD(from.getString("volumeUSD"));
        result |= updateUntrackedVolumeUSD(from.getString("untrackedVolumeUSD"));
        result |= updateFeesUSD(from.getString("feesUSD"));
        result |= updateTxCount(from.getString("txCount"));
        result |= updatePoolCount(from.getString("poolCount"));
        result |= updateTotalValueLocked(from.getString("totalValueLocked"));
        result |= updateTotalValueLockedUSD(from.getString("totalValueLockedUSD"));
        result |= updateTotalValueLockedUSDUntracked(from.getString("totalValueLockedUSDUntracked"));
        result |= updateDerivedETH(from.getString("derivedETH"));

        //whitelistPools
        final JsonArray whitelistPools = from.getArray("whitelistPools");
        result |= updateWhitelistPoolsIds(parseJsonArray(whitelistPools));

        //tokenDayData
        final JsonArray tokenDayData = from.getArray("tokenDayData");
        result |= updateTokenDayDataIds(parseJsonArray(tokenDayData));

        return result;
    }

    @Override
    public String toString() {
        return "Token{" +
                "id='" + id + '\'' +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", decimals='" + decimals + '\'' +
                ", totalSupply='" + totalSupply + '\'' +
                ", volume='" + volume + '\'' +
                ", volumeUSD='" + volumeUSD + '\'' +
                ", untrackedVolumeUSD='" + untrackedVolumeUSD + '\'' +
                ", feesUSD='" + feesUSD + '\'' +
                ", txCount='" + txCount + '\'' +
                ", poolCount='" + poolCount + '\'' +
                ", totalValueLocked='" + totalValueLocked + '\'' +
                ", totalValueLockedUSD='" + totalValueLockedUSD + '\'' +
                ", totalValueLockedUSDUntracked='" + totalValueLockedUSDUntracked + '\'' +
                ", derivedETH='" + derivedETH + '\'' +
                '}';
    }
}
