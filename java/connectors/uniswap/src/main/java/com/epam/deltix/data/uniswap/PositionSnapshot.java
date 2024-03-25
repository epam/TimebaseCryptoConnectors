package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class PositionSnapshot implements Updatable {
    private String id;
    private String owner;
    private String poolId;
    private String position;
    private String blockNumber;
    private String timestamp;
    private String liquidity;
    private String depositedToken0;
    private String depositedToken1;
    private String withdrawnToken0;
    private String withdrawnToken1;
    private String collectedFeesToken0;
    private String collectedFeesToken1;
    private String transactionId;
    private String feeGrowthInside0LastX128;
    private String feeGrowthInside1LastX128;

    @Override
    public String getTbSymbol() {
        return "positionSnapshot";
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
    public String getPosition() {
        return position;
    }

    public void setPosition(final String position) {
        this.position = position;
    }

    public boolean updatePosition(final String position) {
        if (Util.equals(this.position, position)) {
            return false;
        }
        this.position = position;
        return true;
    }

    @SchemaElement()
    public String getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(final String blockNumber) {
        this.blockNumber = blockNumber;
    }

    public boolean updateBlockNumber(final String blockNumber) {
        if (Util.equals(this.blockNumber, blockNumber)) {
            return false;
        }
        this.blockNumber = blockNumber;
        return true;
    }

    @SchemaElement()
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean updateTimestamp(final String timestamp) {
        if (Util.equals(this.timestamp, timestamp)) {
            return false;
        }
        this.timestamp = timestamp;
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

    @SchemaElement()
    public String getDepositedToken0() {
        return depositedToken0;
    }

    public void setDepositedToken0(final String depositedToken0) {
        this.depositedToken0 = depositedToken0;
    }

    public boolean updateDepositedToken0(final String depositedToken0) {
        if (Util.equals(this.depositedToken0, depositedToken0)) {
            return false;
        }
        this.depositedToken0 = depositedToken0;
        return true;
    }

    @SchemaElement()
    public String getDepositedToken1() {
        return depositedToken1;
    }

    public void setDepositedToken1(final String depositedToken1) {
        this.depositedToken1 = depositedToken1;
    }

    public boolean updateDepositedToken1(final String depositedToken1) {
        if (Util.equals(this.depositedToken1, depositedToken1)) {
            return false;
        }
        this.depositedToken1 = depositedToken1;
        return true;
    }

    @SchemaElement()
    public String getWithdrawnToken0() {
        return withdrawnToken0;
    }

    public void setWithdrawnToken0(final String withdrawnToken0) {
        this.withdrawnToken0 = withdrawnToken0;
    }

    public boolean updateWithdrawnToken0(final String withdrawnToken0) {
        if (Util.equals(this.withdrawnToken0, withdrawnToken0)) {
            return false;
        }
        this.withdrawnToken0 = withdrawnToken0;
        return true;
    }

    @SchemaElement()
    public String getWithdrawnToken1() {
        return withdrawnToken1;
    }

    public void setWithdrawnToken1(final String withdrawnToken1) {
        this.withdrawnToken1 = withdrawnToken1;
    }

    public boolean updateWithdrawnToken1(final String withdrawnToken1) {
        if (Util.equals(this.withdrawnToken1, withdrawnToken1)) {
            return false;
        }
        this.withdrawnToken1 = withdrawnToken1;
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
    public String getFeeGrowthInside0LastX128() {
        return feeGrowthInside0LastX128;
    }

    public void setFeeGrowthInside0LastX128(final String feeGrowthInside0LastX128) {
        this.feeGrowthInside0LastX128 = feeGrowthInside0LastX128;
    }

    public boolean updateFeeGrowthInside0LastX128(final String feeGrowthInside0LastX128) {
        if (Util.equals(this.feeGrowthInside0LastX128, feeGrowthInside0LastX128)) {
            return false;
        }
        this.feeGrowthInside0LastX128 = feeGrowthInside0LastX128;
        return true;
    }

    @SchemaElement()
    public String getFeeGrowthInside1LastX128() {
        return feeGrowthInside1LastX128;
    }

    public void setFeeGrowthInside1LastX128(final String feeGrowthInside1LastX128) {
        this.feeGrowthInside1LastX128 = feeGrowthInside1LastX128;
    }

    public boolean updateFeeGrowthInside1LastX128(final String feeGrowthInside1LastX128) {
        if (Util.equals(this.feeGrowthInside1LastX128, feeGrowthInside1LastX128)) {
            return false;
        }
        this.feeGrowthInside1LastX128 = feeGrowthInside1LastX128;
        return true;
    }

    @SchemaElement()
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean updateTransactionId(final String transactionId) {
        if (Util.equals(this.transactionId, transactionId)) {
            return false;
        }
        this.transactionId = transactionId;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        result |= updateId(from.getString("id"));
        result |= updateOwner(from.getString("owner"));

        //pool
        final JsonObject pool = from.getObject("pool");
        String poolId = pool.getString("id");
        result |= updatePoolId(poolId);

        result |= updatePosition(from.getString("position"));
        result |= updateBlockNumber(from.getString("blockNumber"));
        result |= updateTimestamp(from.getString("timestamp"));
        result |= updateLiquidity(from.getString("liquidity"));
        result |= updateDepositedToken0(from.getString("depositedToken0"));
        result |= updateDepositedToken1(from.getString("depositedToken1"));
        result |= updateWithdrawnToken0(from.getString("withdrawnToken0"));
        result |= updateWithdrawnToken1(from.getString("withdrawnToken1"));
        result |= updateCollectedFeesToken0(from.getString("collectedFeesToken0"));
        result |= updateCollectedFeesToken1(from.getString("collectedFeesToken1"));

        //transaction
        final JsonObject transaction = from.getObject("transaction");
        String transactionId = transaction.getString("id");
        result |= updateTransactionId(transactionId);

        result |= updateFeeGrowthInside0LastX128(from.getString("feeGrowthInside0LastX128"));
        result |= updateFeeGrowthInside1LastX128(from.getString("feeGrowthInside1LastX128"));

        return result;
    }
}
