package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Burn implements Updatable {
    private String id;
    private String thxId;
    private String thxBlockNumber;
    private String poolId;
    private String token0Id;
    private String token0Symbol;
    private String token1Id;
    private String token1Symbol;
    private String timestamp;
    private String owner;
    private String origin;
    private String amount;
    private String amount0;
    private String amount1;
    private String amountUSD;
    private String tickLower;
    private String tickUpper;
    private String logIndex;

    @Override
    public String getTbSymbol() {
        return token0Symbol + "/" + token1Symbol;
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
    public String getThxId() {
        return thxId;
    }

    public void setThxId(final String thxId) {
        this.thxId = thxId;
    }

    public boolean updateThxId(final String thxId) {
        if (Util.equals(this.thxId, thxId)) {
            return false;
        }
        this.thxId = thxId;
        return true;
    }

    @SchemaElement()
    public String getThxBlockNumber() {
        return thxBlockNumber;
    }

    public void setThxBlockNumber(final String thxBlockNumber) {
        this.thxBlockNumber = thxBlockNumber;
    }

    public boolean updateThxBlockNumber(final String thxBlockNumber) {
        if (Util.equals(this.thxBlockNumber, thxBlockNumber)) {
            return false;
        }
        this.thxBlockNumber = thxBlockNumber;
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
    public String getToken0Id() {
        return token0Id;
    }

    public void setToken0Id(final String token0Id) {
        this.token0Id = token0Id;
    }

    public boolean updateToken0Id(final String token0Id) {
        if (Util.equals(this.token0Id, token0Id)) {
            return false;
        }
        this.token0Id = token0Id;
        return true;
    }

    @SchemaElement()
    public String getToken0Symbol() {
        return token0Symbol;
    }

    public void setToken0Symbol(final String token0Symbol) {
        this.token0Symbol = token0Symbol;
    }

    public boolean updateToken0Symbol(final String token0Symbol) {
        if (Util.equals(this.token0Symbol, token0Symbol)) {
            return false;
        }
        this.token0Symbol = token0Symbol;
        return true;
    }

    @SchemaElement()
    public String getToken1Id() {
        return token1Id;
    }

    public void setToken1Id(final String token1Id) {
        this.token1Id = token1Id;
    }

    public boolean updateToken1Id(final String token1Id) {
        if (Util.equals(this.token1Id, token1Id)) {
            return false;
        }
        this.token1Id = token1Id;
        return true;
    }

    @SchemaElement()
    public String getToken1Symbol() {
        return token1Symbol;
    }

    public void setToken1Symbol(final String token1Symbol) {
        this.token1Symbol = token1Symbol;
    }

    public boolean updateToken1Symbol(final String token1Symbol) {
        if (Util.equals(this.token1Symbol, token1Symbol)) {
            return false;
        }
        this.token1Symbol = token1Symbol;
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
    public String getOrigin() {
        return origin;
    }

    public void setOrigin(final String origin) {
        this.origin = origin;
    }

    public boolean updateOrigin(final String origin) {
        if (Util.equals(this.origin, origin)) {
            return false;
        }
        this.origin = origin;
        return true;
    }

    @SchemaElement()
    public String getAmount() {
        return amount;
    }

    public void setAmount(final String amount) {
        this.amount = amount;
    }

    public boolean updateAmount(final String amount) {
        if (Util.equals(this.amount, amount)) {
            return false;
        }
        this.amount = amount;
        return true;
    }

    @SchemaElement()
    public String getAmount0() {
        return amount0;
    }

    public void setAmount0(final String amount0) {
        this.amount0 = amount0;
    }

    public boolean updateAmount0(final String amount0) {
        if (Util.equals(this.amount0, amount0)) {
            return false;
        }
        this.amount0 = amount0;
        return true;
    }

    @SchemaElement()
    public String getAmount1() {
        return amount1;
    }

    public void setAmount1(final String amount1) {
        this.amount1 = amount1;
    }

    public boolean updateAmount1(final String amount1) {
        if (Util.equals(this.amount1, amount1)) {
            return false;
        }
        this.amount1 = amount1;
        return true;
    }

    @SchemaElement()
    public String getAmountUSD() {
        return amountUSD;
    }

    public void setAmountUSD(final String amountUSD) {
        this.amountUSD = amountUSD;
    }

    public boolean updateAmountUSD(final String amountUSD) {
        if (Util.equals(this.amountUSD, amountUSD)) {
            return false;
        }
        this.amountUSD = amountUSD;
        return true;
    }

    @SchemaElement()
    public String getTickLower() {
        return tickLower;
    }

    public void setTickLower(final String tickLower) {
        this.tickLower = tickLower;
    }

    public boolean updateTickLower(final String tickLower) {
        if (Util.equals(this.tickLower, tickLower)) {
            return false;
        }
        this.tickLower = tickLower;
        return true;
    }

    @SchemaElement()
    public String getTickUpper() {
        return tickUpper;
    }

    public void setTickUpper(final String tickUpper) {
        this.tickUpper = tickUpper;
    }

    public boolean updateTickUpper(final String tickUpper) {
        if (Util.equals(this.tickUpper, tickUpper)) {
            return false;
        }
        this.tickUpper = tickUpper;
        return true;
    }

    @SchemaElement()
    public String getLogIndex() {
        return logIndex;
    }

    public void setLogIndex(final String logIndex) {
        this.logIndex = logIndex;
    }

    public boolean updateLogIndex(final String logIndex) {
        if (Util.equals(this.logIndex, logIndex)) {
            return false;
        }
        this.logIndex = logIndex;
        return true;
    }

    @Override
    public boolean update(final JsonObject from) {
        boolean result = false;
        //id
        result |= updateId(from.getString("id"));

        //transaction
        String trx_id = null;
        String thx_block_number = null;
        final JsonObject trx = from.getObject("transaction");
        if (trx != null) {
            trx_id = trx.getString("id");
            thx_block_number = trx.getString("blockNumber");
        }
        result |= updateThxId(trx_id);
        result |= updateThxBlockNumber(thx_block_number);

        //timestamp
        result |= updateTimestamp(from.getString("timestamp"));

        //pool
        String pool_id = null;
        final JsonObject pool = from.getObject("pool");
        if (pool != null) {
            pool_id = pool.getString("id");
        }
        result |= updatePoolId(pool_id);

        //token0
        String token0_id = null;
        String token0_symbol = null;
        final JsonObject token0 = from.getObject("token0");
        if (token0 != null) {
            token0_id = token0.getString("id");
            token0_symbol = token0.getString("symbol");
        }
        result |= updateToken0Id(token0_id);
        result |= updateToken0Symbol(token0_symbol);

        //token1
        String token1_id = null;
        String token1_symbol = null;
        final JsonObject token1 = from.getObject("token1");
        if (token1 != null) {
            token1_id = token1.getString("id");
            token1_symbol = token1.getString("symbol");
        }
        result |= updateToken1Id(token1_id);
        result |= updateToken1Symbol(token1_symbol);

        result |= updateAmount(from.getString("amount"));
        result |= updateOwner(from.getString("owner"));
        result |= updateOrigin(from.getString("origin"));
        result |= updateAmount0(from.getString("amount0"));
        result |= updateAmount1(from.getString("amount1"));
        result |= updateAmountUSD(from.getString("amountUSD"));
        result |= updateTickLower(from.getString("tickLower"));
        result |= updateTickUpper(from.getString("tickUpper"));
        result |= updateLogIndex(from.getString("logIndex"));

        return result;
    }
}
