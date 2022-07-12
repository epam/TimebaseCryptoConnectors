package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Pool implements Updatable {
    private String id;
    private String createdAtTimestamp;
    private String createdAtBlockNumber;
    private String feeTier;
    private String liquidity;
    private String sqrtPrice;
    private String feeGrowthGlobal0X128;
    private String feeGrowthGlobal1X128;
    private String token0Price;
    private String token1Price;
    private String tick;
    private String observationIndex;
    private String volumeToken0;
    private String volumeToken1;
    private String volumeUSD;
    private String untrackedVolumeUSD;
    private String feesUSD;
    private String txCount;
    private String collectedFeesToken0;
    private String collectedFeesToken1;
    private String collectedFeesUSD;
    private String totalValueLockedToken0;
    private String totalValueLockedToken1;
    private String totalValueLockedETH;
    private String totalValueLockedUSD;
    private String totalValueLockedUSDUntracked;
    private String liquidityProviderCount;
    private String token0_id;
    private String token0_symbol;
    private String token1_id;
    private String token1_symbol;

    @Override
    public String getTbSymbol() {
        return token0_symbol + '/' + token1_symbol + '-' + id;
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
    public String getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }

    public void setCreatedAtTimestamp(final String createdAtTimestamp) {
        this.createdAtTimestamp = createdAtTimestamp;
    }

    public boolean updateCreatedAtTimestamp(final String createdAtTimestamp) {
        if (Util.equals(this.createdAtTimestamp, createdAtTimestamp)) {
            return false;
        }
        this.createdAtTimestamp = createdAtTimestamp;
        return true;
    }

    @SchemaElement()
    public String getCreatedAtBlockNumber() {
        return createdAtBlockNumber;
    }

    public void setCreatedAtBlockNumber(final String createdAtBlockNumber) {
        this.createdAtBlockNumber = createdAtBlockNumber;
    }

    public boolean updateCreatedAtBlockNumber(final String createdAtBlockNumber) {
        if (Util.equals(this.createdAtBlockNumber, createdAtBlockNumber)) {
            return false;
        }
        this.createdAtBlockNumber = createdAtBlockNumber;
        return true;
    }

    @SchemaElement
    public String getFeeTier() {
        return feeTier;
    }

    public void setFeeTier(final String feeTier) {
        this.feeTier = feeTier;
    }

    public boolean updateFeeTier(final String feeTier) {
        if (Util.equals(this.feeTier, feeTier)) {
            return false;
        }
        this.feeTier = feeTier;
        return true;
    }

    @SchemaElement()
    public String getLiquidity() {
        return liquidity;
    }

    public void setLiquidity(final String liquidity) {
        this.liquidity = liquidity;
    }

    public boolean updateLiquidity(final String liquidity) {
        if (Util.equals(this.liquidity, liquidity)) {
            return false;
        }
        this.liquidity = liquidity;
        return true;
    }

    @SchemaElement
    public String getSqrtPrice() {
        return sqrtPrice;
    }

    public void setSqrtPrice(final String sqrtPrice) {
        this.sqrtPrice = sqrtPrice;
    }

    public boolean updateSqrtPrice(final String sqrtPrice) {
        if (Util.equals(this.sqrtPrice, sqrtPrice)) {
            return false;
        }
        this.sqrtPrice = sqrtPrice;
        return true;
    }

    @SchemaElement
    public String getFeeGrowthGlobal0X128() {
        return feeGrowthGlobal0X128;
    }

    public void setFeeGrowthGlobal0X128(final String feeGrowthGlobal0X128) {
        this.feeGrowthGlobal0X128 = feeGrowthGlobal0X128;
    }

    public boolean updateFeeGrowthGlobal0X128(final String feeGrowthGlobal0X128) {
        if (Util.equals(this.feeGrowthGlobal0X128, feeGrowthGlobal0X128)) {
            return false;
        }
        this.feeGrowthGlobal0X128 = feeGrowthGlobal0X128;
        return true;
    }

    @SchemaElement
    public String getFeeGrowthGlobal1X128() {
        return feeGrowthGlobal1X128;
    }

    public void setFeeGrowthGlobal1X128(final String feeGrowthGlobal1X128) {
        this.feeGrowthGlobal1X128 = feeGrowthGlobal1X128;
    }

    public boolean updateFeeGrowthGlobal1X128(final String feeGrowthGlobal1X128) {
        if (Util.equals(this.feeGrowthGlobal1X128, feeGrowthGlobal1X128)) {
            return false;
        }
        this.feeGrowthGlobal1X128 = feeGrowthGlobal1X128;
        return true;
    }

    @SchemaElement
    public String getToken0Price() {
        return token0Price;
    }

    public void setToken0Price(final String token0Price) {
        this.token0Price = token0Price;
    }

    public boolean updateToken0Price(final String token0Price) {
        if (Util.equals(this.token0Price, token0Price)) {
            return false;
        }
        this.token0Price = token0Price;
        return true;
    }

    @SchemaElement
    public String getToken1Price() {
        return token1Price;
    }

    public void setToken1Price(final String token1Price) {
        this.token1Price = token1Price;
    }

    public boolean updateToken1Price(final String token1Price) {
        if (Util.equals(this.token1Price, token1Price)) {
            return false;
        }
        this.token1Price = token1Price;
        return true;
    }

    @SchemaElement
    public String getTick() {
        return tick;
    }

    public void setTick(final String tick) {
        this.tick = tick;
    }

    public boolean updateTick(final String tick) {
        if (Util.equals(this.tick, tick)) {
            return false;
        }
        this.tick = tick;
        return true;
    }

    @SchemaElement
    public String getObservationIndex() {
        return observationIndex;
    }

    public void setObservationIndex(final String observationIndex) {
        this.observationIndex = observationIndex;
    }

    public boolean updateObservationIndex(final String observationIndex) {
        if (Util.equals(this.observationIndex, observationIndex)) {
            return false;
        }
        this.observationIndex = observationIndex;
        return true;
    }

    @SchemaElement
    public String getVolumeToken0() {
        return volumeToken0;
    }

    public void setVolumeToken0(final String volumeToken0) {
        this.volumeToken0 = volumeToken0;
    }

    public boolean updateVolumeToken0(final String volumeToken0) {
        if (Util.equals(this.volumeToken0, volumeToken0)) {
            return false;
        }
        this.volumeToken0 = volumeToken0;
        return true;
    }

    @SchemaElement
    public String getVolumeToken1() {
        return volumeToken1;
    }

    public void setVolumeToken1(final String volumeToken1) {
        this.volumeToken1 = volumeToken1;
    }

    public boolean updateVolumeToken1(final String volumeToken1) {
        if (Util.equals(this.volumeToken1, volumeToken1)) {
            return false;
        }
        this.volumeToken1 = volumeToken1;
        return true;
    }

    @SchemaElement
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

    @SchemaElement
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

    @SchemaElement
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

    @SchemaElement
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

    @SchemaElement
    public String getCollectedFeesToken0() {
        return collectedFeesToken0;
    }

    public void setCollectedFeesToken0(final String collectedFeesToken0) {
        this.collectedFeesToken0 = collectedFeesToken0;
    }

    public boolean updateCollectedFeesToken0(final String collectedFeesToken0) {
        if (Util.equals(this.collectedFeesToken0, collectedFeesToken0)) {
            return false;
        }
        this.collectedFeesToken0 = collectedFeesToken0;
        return true;
    }

    @SchemaElement
    public String getCollectedFeesToken1() {
        return collectedFeesToken1;
    }

    public void setCollectedFeesToken1(final String collectedFeesToken1) {
        this.collectedFeesToken1 = collectedFeesToken1;
    }

    public boolean updateCollectedFeesToken1(final String collectedFeesToken1) {
        if (Util.equals(this.collectedFeesToken1, collectedFeesToken1)) {
            return false;
        }
        this.collectedFeesToken1 = collectedFeesToken1;
        return true;
    }

    @SchemaElement
    public String getCollectedFeesUSD() {
        return collectedFeesUSD;
    }

    public void setCollectedFeesUSD(final String collectedFeesUSD) {
        this.collectedFeesUSD = collectedFeesUSD;
    }

    public boolean updateCollectedFeesUSD(final String collectedFeesUSD) {
        if (Util.equals(this.collectedFeesUSD, collectedFeesUSD)) {
            return false;
        }
        this.collectedFeesUSD = collectedFeesUSD;
        return true;
    }

    @SchemaElement
    public String getTotalValueLockedToken0() {
        return totalValueLockedToken0;
    }

    public void setTotalValueLockedToken0(final String totalValueLockedToken0) {
        this.totalValueLockedToken0 = totalValueLockedToken0;
    }

    public boolean updateTotalValueLockedToken0(final String totalValueLockedToken0) {
        if (Util.equals(this.totalValueLockedToken0, totalValueLockedToken0)) {
            return false;
        }
        this.totalValueLockedToken0 = totalValueLockedToken0;
        return true;
    }

    @SchemaElement
    public String getTotalValueLockedToken1() {
        return totalValueLockedToken1;
    }

    public void setTotalValueLockedToken1(final String totalValueLockedToken1) {
        this.totalValueLockedToken1 = totalValueLockedToken1;
    }

    public boolean updateTotalValueLockedToken1(final String totalValueLockedToken1) {
        if (Util.equals(this.totalValueLockedToken1, totalValueLockedToken1)) {
            return false;
        }
        this.totalValueLockedToken1 = totalValueLockedToken1;
        return true;
    }

    @SchemaElement
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

    @SchemaElement
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

    @SchemaElement
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
    public String getLiquidityProviderCount() {
        return liquidityProviderCount;
    }

    public void setLiquidityProviderCount(final String liquidityProviderCount) {
        this.liquidityProviderCount = liquidityProviderCount;
    }

    public boolean updateLiquidityProviderCount(final String liquidityProviderCount) {
        if (Util.equals(this.liquidityProviderCount, liquidityProviderCount)) {
            return false;
        }
        this.liquidityProviderCount = liquidityProviderCount;
        return true;
    }

    @SchemaElement()
    public String getToken0_id() {
        return token0_id;
    }

    public void setToken0_id(final String token0_id) {
        this.token0_id = token0_id;
    }

    public boolean updateToken0_id(final String token0_id) {
        if (Util.equals(this.token0_id, token0_id)) {
            return false;
        }
        this.token0_id = token0_id;
        return true;
    }

    @SchemaElement()
    public String getToken0_symbol() {
        return token0_symbol;
    }

    public void setToken0_symbol(final String token0_symbol) {
        this.token0_symbol = token0_symbol;
    }

    public boolean updateToken0_symbol(final String token0_symbol) {
        if (Util.equals(this.token0_symbol, token0_symbol)) {
            return false;
        }
        this.token0_symbol = token0_symbol;
        return true;
    }

    @SchemaElement()
    public String getToken1_id() {
        return token1_id;
    }

    public void setToken1_id(final String token1_id) {
        this.token1_id = token1_id;
    }

    public boolean updateToken1_id(final String token1_id) {
        if (Util.equals(this.token1_id, token1_id)) {
            return false;
        }
        this.token1_id = token1_id;
        return true;
    }

    @SchemaElement()
    public String getToken1_symbol() {
        return token1_symbol;
    }

    public void setToken1_symbol(final String token1_symbol) {
        this.token1_symbol = token1_symbol;
    }

    public boolean updateToken1_symbol(final String token1_symbol) {
        if (Util.equals(this.token1_symbol, token1_symbol)) {
            return false;
        }
        this.token1_symbol = token1_symbol;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        result |= updateId(from.getString("id"));
        result |= updateCreatedAtTimestamp(from.getString("createdAtTimestamp"));
        result |= updateCreatedAtBlockNumber(from.getString("createdAtBlockNumber"));
        result |= updateFeeTier(from.getString("feeTier"));
        result |= updateLiquidity(from.getString("liquidity"));
        result |= updateSqrtPrice(from.getString("sqrtPrice"));
        result |= updateFeeGrowthGlobal0X128(from.getString("feeGrowthGlobal0X128"));
        result |= updateFeeGrowthGlobal1X128(from.getString("feeGrowthGlobal1X128"));
        result |= updateToken0Price(from.getString("token0Price"));
        result |= updateToken1Price(from.getString("token1Price"));
        result |= updateTick(from.getString("tick"));
        result |= updateObservationIndex(from.getString("observationIndex"));
        result |= updateVolumeToken0(from.getString("volumeToken0"));
        result |= updateVolumeToken1(from.getString("volumeToken1"));
        result |= updateVolumeUSD(from.getString("volumeUSD"));
        result |= updateUntrackedVolumeUSD(from.getString("untrackedVolumeUSD"));
        result |= updateFeesUSD(from.getString("feesUSD"));
        result |= updateTxCount(from.getString("txCount"));
        result |= updateCollectedFeesToken0(from.getString("collectedFeesToken0"));
        result |= updateCollectedFeesToken1(from.getString("collectedFeesToken1"));
        result |= updateCollectedFeesUSD(from.getString("collectedFeesUSD"));
        result |= updateTotalValueLockedToken0(from.getString("totalValueLockedToken0"));
        result |= updateTotalValueLockedToken1(from.getString("totalValueLockedToken1"));
        result |= updateTotalValueLockedETH(from.getString("totalValueLockedETH"));
        result |= updateTotalValueLockedUSD(from.getString("totalValueLockedUSD"));
        result |= updateTotalValueLockedUSDUntracked(from.getString("totalValueLockedUSDUntracked"));
        result |= updateLiquidityProviderCount(from.getString("liquidityProviderCount"));

        String token0_id_json = null;
        String token0_symbol_json = null;
        final JsonObject token0 = from.getObject("token0");
        if (token0 != null) {
            token0_id_json = token0.getString("id");
            token0_symbol_json = token0.getString("symbol");
        }
        result |= updateToken0_id(token0_id_json);
        result |= updateToken0_symbol(token0_symbol_json);


        String token1_id_json = null;
        String token1_symbol_json = null;
        final JsonObject token1 = from.getObject("token1");
        if (token1 != null) {
            token1_id_json = token1.getString("id");
            token1_symbol_json = token1.getString("symbol");
        }
        result |= updateToken1_id(token1_id_json);
        result |= updateToken1_symbol(token1_symbol_json);

        return result;
    }

    @Override
    public String toString() {
        return "Pool{" +
                "id='" + id + '\'' +
                ", createdAtTimestamp='" + createdAtTimestamp + '\'' +
                ", createdAtBlockNumber='" + createdAtBlockNumber + '\'' +
                ", feeTier='" + feeTier + '\'' +
                ", liquidity='" + liquidity + '\'' +
                ", sqrtPrice='" + sqrtPrice + '\'' +
                ", feeGrowthGlobal0X128='" + feeGrowthGlobal0X128 + '\'' +
                ", feeGrowthGlobal1X128='" + feeGrowthGlobal1X128 + '\'' +
                ", token0Price='" + token0Price + '\'' +
                ", token1Price='" + token1Price + '\'' +
                ", tick='" + tick + '\'' +
                ", observationIndex='" + observationIndex + '\'' +
                ", volumeToken0='" + volumeToken0 + '\'' +
                ", volumeToken1='" + volumeToken1 + '\'' +
                ", volumeUSD='" + volumeUSD + '\'' +
                ", untrackedVolumeUSD='" + untrackedVolumeUSD + '\'' +
                ", feesUSD='" + feesUSD + '\'' +
                ", txCount='" + txCount + '\'' +
                ", collectedFeesToken0='" + collectedFeesToken0 + '\'' +
                ", collectedFeesToken1='" + collectedFeesToken1 + '\'' +
                ", collectedFeesUSD='" + collectedFeesUSD + '\'' +
                ", totalValueLockedToken0='" + totalValueLockedToken0 + '\'' +
                ", totalValueLockedToken1='" + totalValueLockedToken1 + '\'' +
                ", totalValueLockedETH='" + totalValueLockedETH + '\'' +
                ", totalValueLockedUSD='" + totalValueLockedUSD + '\'' +
                ", totalValueLockedUSDUntracked='" + totalValueLockedUSDUntracked + '\'' +
                ", liquidityProviderCount='" + liquidityProviderCount + '\'' +
                ", token0_id='" + token0_id + '\'' +
                ", token0_symbol='" + token0_symbol + '\'' +
                ", token1_id='" + token1_id + '\'' +
                ", token1_symbol='" + token1_symbol + '\'' +
                '}';
    }
}
