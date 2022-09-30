package com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UniswapSymbol {
    public static String[] collectSymbols(final UniswapSymbol... uniswapSymbols) {
        if (uniswapSymbols == null) {
            return new String[] {};
        }
        final Set<String> result = new HashSet<>();
        Arrays.stream(uniswapSymbols).forEach(uniswapSymbol -> uniswapSymbol.collectTokens(result));
        return result.toArray(String[]::new);
    }

    private final String token0;
    private final String token1;

    public UniswapSymbol(final String symbol) {
        final int delimIdx = symbol.indexOf('/');
        if (delimIdx == -1) {
            token0 = symbol.isBlank() ? null : symbol.trim();
            token1 = null;
        } else {
            final String t0 = symbol.substring(0, delimIdx);
            final String t1 = symbol.substring(delimIdx + 1);

            token0 = t0.isBlank() ? null : t0.trim();
            token1 = t1.isBlank() ? null : t1.trim();
        }

        if (token0 == null && token1 == null) {
            throw new IllegalArgumentException("Empty symbol '" + symbol + '\'');
        }
    }

    protected UniswapSymbol(final UniswapSymbol from) {
        this.token0 = from.token0;
        this.token1 = from.token1;
    }

    public String token0() {
        return token0;
    }

    public boolean hasToken0() {
        return token0 != null;
    }

    public String token1() {
        return token1;
    }

    public boolean hasToken1() {
        return token1 != null;
    }

    public void collectTokens(final Collection<String> to) {
        if (hasToken0()) {
            to.add(token0());
        }
        if (hasToken1()) {
            to.add(token1());
        }
    }

    @Override
    public String toString() {
        return (hasToken0() ? token0() : "") + '/' +
                (hasToken1() ? token1() : "");
    }
}
