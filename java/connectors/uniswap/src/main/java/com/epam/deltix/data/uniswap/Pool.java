package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.timebase.messages.SchemaElement;

public class Pool implements Updatable {
    private String id;
    private String liquidity;
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
        result |= updateLiquidity(from.getString("liquidity"));
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
                ", liquidity='" + liquidity + '\'' +
                ", liquidityProviderCount='" + liquidityProviderCount + '\'' +
                ", token0_id='" + token0_id + '\'' +
                ", token0_symbol='" + token0_symbol + '\'' +
                ", token1_id='" + token1_id + '\'' +
                ", token1_symbol='" + token1_symbol + '\'' +
                '}';
    }
}
