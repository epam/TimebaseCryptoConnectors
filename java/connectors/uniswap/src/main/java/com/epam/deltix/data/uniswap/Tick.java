package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Tick implements Updatable {
    //tick address
    private String id;
    //pool address
    private String poolId;
    //tick index
    private String tickIdx;
    //total liquidity pool has as tick lower or upper
    private String liquidityGross;
    //how much liquidity changes when tick crossed
    private String liquidityNet;
    //calculated price of token0 of tick within this pool - constant
    private String price0;
    //calculated price of token1 of tick within this pool - constant
    private String price1;
    //lifetime volume of token0 with this tick in range
    private String volumeToken0;
    //lifetime volume of token1 with this tick in range
    private String volumeToken1;
    //lifetime volume in derived USD with this tick in range
    private String volumeUSD;
    //lifetime volume in untracked USD with this tick in range
    private String untrackedVolumeUSD;
    //fees in USD
    private String feesUSD;
    //all time collected fees in token0
    private String collectedFeesToken0;
    //all time collected fees in token1
    private String collectedFeesToken1;
    //all time collected fees in USD
    private String collectedFeesUSD;
    //created time
    private String createdAtTimestamp;
    //created block
    private String createdAtBlockNumber;
    //Fields used to help derived relationship
    private String liquidityProviderCount;
    private String feeGrowthOutside0X128;
    private String feeGrowthOutside1X128;

    @Override
    public String getTbSymbol() {
        return "tick";
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
    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(final String poolId) {
        this.poolId = poolId;
    }

    public boolean updatePoolId(final String poolId) {
        if (Util.equals(this.poolId, poolId)) {
            return false;
        }
        this.poolId = poolId;
        return true;
    }

    @SchemaElement()
    public String getTickIdx() {
        return tickIdx;
    }

    public void setTickIdx(final String tickIdx) {
        this.tickIdx = tickIdx;
    }

    public boolean updateTickIdx(final String tickIdx) {
        if (Util.equals(this.tickIdx, tickIdx)) {
            return false;
        }
        this.tickIdx = tickIdx;
        return true;
    }

    @SchemaElement()
    public String getLiquidityGross() {
        return liquidityGross;
    }

    public void setLiquidityGross(final String liquidityGross) {
        this.liquidityGross = liquidityGross;
    }

    public boolean updateLiquidityGross(final String liquidityGross) {
        if (Util.equals(this.liquidityGross, liquidityGross)) {
            return false;
        }
        this.liquidityGross = liquidityGross;
        return true;
    }

    @SchemaElement()
    public String getLiquidityNet() {
        return liquidityNet;
    }

    public void setLiquidityNet(final String liquidityNet) {
        this.liquidityNet = liquidityNet;
    }

    public boolean updateLiquidityNet(final String liquidityNet) {
        if (Util.equals(this.liquidityNet, liquidityNet)) {
            return false;
        }
        this.liquidityNet = liquidityNet;
        return true;
    }

    @SchemaElement()
    public String getPrice0() {
        return price0;
    }

    public void setPrice0(final String price0) {
        this.price0 = price0;
    }

    public boolean updatePrice0(final String price0) {
        if (Util.equals(this.price0, price0)) {
            return false;
        }
        this.price0 = price0;
        return true;
    }

    @SchemaElement()
    public String getPrice1() {
        return price1;
    }

    public void setPrice1(final String price1) {
        this.price1 = price1;
    }

    public boolean updatePrice1(final String price1) {
        if (Util.equals(this.price1, price1)) {
            return false;
        }
        this.price1 = price1;
        return true;
    }

    @SchemaElement()
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

    @SchemaElement()
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

    @SchemaElement()
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

    @SchemaElement()
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
    public String getFeeGrowthOutside0X128() {
        return feeGrowthOutside0X128;
    }

    public void setFeeGrowthOutside0X128(final String feeGrowthOutside0X128) {
        this.feeGrowthOutside0X128 = feeGrowthOutside0X128;
    }

    public boolean updateFeeGrowthOutside0X128(final String feeGrowthOutside0X128) {
        if (Util.equals(this.feeGrowthOutside0X128, feeGrowthOutside0X128)) {
            return false;
        }
        this.feeGrowthOutside0X128 = feeGrowthOutside0X128;
        return true;
    }

    @SchemaElement()
    public String getFeeGrowthOutside1X128() {
        return feeGrowthOutside1X128;
    }

    public void setFeeGrowthOutside1X128(final String feeGrowthOutside1X128) {
        this.feeGrowthOutside1X128 = feeGrowthOutside1X128;
    }

    public boolean updateFeeGrowthOutside1X128(final String feeGrowthOutside1X128) {
        if (Util.equals(this.feeGrowthOutside1X128, feeGrowthOutside1X128)) {
            return false;
        }
        this.feeGrowthOutside1X128 = feeGrowthOutside1X128;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        result |= updateId(from.getString("id"));
        result |= updateTickIdx(from.getString("tickIdx"));
        result |= updateLiquidityGross(from.getString("liquidityGross"));
        result |= updateLiquidityNet(from.getString("liquidityNet"));
        result |= updatePrice0(from.getString("price0"));
        result |= updatePrice1(from.getString("price1"));
        result |= updateVolumeToken0(from.getString("volumeToken0"));
        result |= updateVolumeToken1(from.getString("volumeToken1"));
        result |= updateVolumeUSD(from.getString("volumeUSD"));
        result |= updateUntrackedVolumeUSD(from.getString("untrackedVolumeUSD"));
        result |= updateFeesUSD(from.getString("feesUSD"));
        result |= updateCollectedFeesToken0(from.getString("collectedFeesToken0"));
        result |= updateCollectedFeesToken1(from.getString("collectedFeesToken1"));
        result |= updateCollectedFeesUSD(from.getString("collectedFeesUSD"));
        result |= updateCreatedAtTimestamp(from.getString("createdAtTimestamp"));
        result |= updateCreatedAtBlockNumber(from.getString("createdAtBlockNumber"));
        result |= updateLiquidityProviderCount(from.getString("liquidityProviderCount"));
        result |= updateFeeGrowthOutside0X128(from.getString("feeGrowthOutside0X128"));
        result |= updateFeeGrowthOutside1X128(from.getString("feeGrowthOutside1X128"));

        //pool
        final JsonObject pool = from.getObject("pool");
        String poolId = pool.getString("id");
        result |= updatePoolId(poolId);

        return result;
    }

    @Override
    public String toString() {
        return "Tick{" +
                "id='" + id + '\'' +
                ", poolAddress='" + poolId + '\'' +
                ", tickIdx='" + tickIdx + '\'' +
                ", liquidityGross='" + liquidityGross + '\'' +
                ", liquidityNet='" + liquidityNet + '\'' +
                ", price0='" + price0 + '\'' +
                ", price1='" + price1 + '\'' +
                ", volumeToken0='" + volumeToken0 + '\'' +
                ", volumeToken1='" + volumeToken1 + '\'' +
                ", volumeUSD='" + volumeUSD + '\'' +
                ", untrackedVolumeUSD='" + untrackedVolumeUSD + '\'' +
                ", feesUSD='" + feesUSD + '\'' +
                ", collectedFeesToken0='" + collectedFeesToken0 + '\'' +
                ", collectedFeesToken1='" + collectedFeesToken1 + '\'' +
                ", collectedFeesUSD='" + collectedFeesUSD + '\'' +
                ", createdAtTimestamp='" + createdAtTimestamp + '\'' +
                ", createdAtBlockNumber='" + createdAtBlockNumber + '\'' +
                ", liquidityProviderCount='" + liquidityProviderCount + '\'' +
                ", feeGrowthOutside0X128='" + feeGrowthOutside0X128 + '\'' +
                ", feeGrowthOutside1X128='" + feeGrowthOutside1X128 + '\'' +
                '}';
    }
}
