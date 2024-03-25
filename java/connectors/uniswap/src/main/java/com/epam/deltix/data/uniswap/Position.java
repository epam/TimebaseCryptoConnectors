package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Position implements Updatable {
    // token address
    private String id;
    //owner of the NFT
    private String owner;
    //pool position is within
    private String poolId;
    private String liquidity;
    //amount of token 0 ever deposited to position
    private String depositedToken0;
    //amount of token 1 ever deposited to position
    private String depositedToken1;
    //amount of token 0 ever withdrawn from position (without fees)
    private String withdrawnToken0;
    //amount of token 1 ever withdrawn from position (without fees)
    private String withdrawnToken1;
    //all time collected fees in token0
    private String collectedFeesToken0;
    //all time collected fees in token1
    private String collectedFeesToken1;
    //tx in which the position was initialized
    private String transactionId;
    //vars needed for fee computation
    private String feeGrowthInside0LastX128;
    private String feeGrowthInside1LastX128;
    private String token0_id;
    private String token0_symbol;
    private String token1_id;
    private String token1_symbol;

    @Override
    public String getTbSymbol() {
        return token0_symbol + '/' + token1_symbol;
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
        result |= updateLiquidity(from.getString("liquidity"));
        result |= updateDepositedToken0(from.getString("depositedToken0"));
        result |= updateDepositedToken1(from.getString("depositedToken1"));
        result |= updateWithdrawnToken0(from.getString("withdrawnToken0"));
        result |= updateWithdrawnToken1(from.getString("withdrawnToken1"));
        result |= updateCollectedFeesToken0(from.getString("collectedFeesToken0"));
        result |= updateCollectedFeesToken1(from.getString("collectedFeesToken1"));
        result |= updateFeeGrowthInside0LastX128(from.getString("feeGrowthInside0LastX128"));
        result |= updateFeeGrowthInside1LastX128(from.getString("feeGrowthInside1LastX128"));

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

        //pool
        final JsonObject pool = from.getObject("pool");
        String poolId = pool.getString("id");
        result |= updatePoolId(poolId);

        //transaction
        final JsonObject transaction = from.getObject("transaction");
        String transactionId = transaction.getString("id");
        result |= updateTransactionId(transactionId);

        return result;
    }

    @Override
    public String toString() {
        return "Position{" +
                "id='" + id + '\'' +
                ", owner='" + owner + '\'' +
                ", liquidity='" + liquidity + '\'' +
                ", depositedToken0='" + depositedToken0 + '\'' +
                ", depositedToken1='" + depositedToken1 + '\'' +
                ", withdrawnToken0='" + withdrawnToken0 + '\'' +
                ", withdrawnToken1='" + withdrawnToken1 + '\'' +
                ", collectedFeesToken0='" + collectedFeesToken0 + '\'' +
                ", collectedFeesToken1='" + collectedFeesToken1 + '\'' +
                ", feeGrowthInside0LastX128='" + feeGrowthInside0LastX128 + '\'' +
                ", feeGrowthInside1LastX128='" + feeGrowthInside1LastX128 + '\'' +
                ", token0_id='" + token0_id + '\'' +
                ", token0_symbol='" + token0_symbol + '\'' +
                ", token1_id='" + token1_id + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", poolId='" + poolId + '\'' +
                '}';
    }
}
