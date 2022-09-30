package com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IdentifiedUniswapSymbol extends UniswapSymbol {
    public static IdentifiedUniswapSymbol[] identify(
            final Map<String, TokenIdentifier.Info> symbolInfos,
            final UniswapSymbol... symbols) {
        if (symbols == null) {
            return new IdentifiedUniswapSymbol[] {};
        }
        return Arrays.stream(symbols).map(uniswapSymbol -> {
            final TokenIdentifier.Info token0Info = symbolInfos.get(uniswapSymbol.token0());
            final TokenIdentifier.Info token1Info = symbolInfos.get(uniswapSymbol.token1());
            return new IdentifiedUniswapSymbol(
                    uniswapSymbol,
                    token0Info != null ? token0Info.id() : null,
                    token1Info != null ? token1Info.id() : null
            );
        }).toArray(IdentifiedUniswapSymbol[]::new);
    }

    public static String[] collectIds(final IdentifiedUniswapSymbol... uniswapSymbols) {
        if (uniswapSymbols == null) {
            return new String[] {};
        }
        final Set<String> result = new HashSet<>();
        Arrays.stream(uniswapSymbols).forEach(uniswapSymbol -> uniswapSymbol.collectIds(result));
        return result.toArray(String[]::new);
    }

    private final String token0Id;
    private final String token1Id;

    public IdentifiedUniswapSymbol(
            final UniswapSymbol from,
            final String token0Id,
            final String token1Id) {
        super(from);

        if (hasToken0()) {
            if ((token0Id == null || token0Id.isBlank())) {
                throw new IllegalArgumentException("token0Id not specified");
            }
            this.token0Id = token0Id.trim();
        } else {
            this.token0Id = null;
        }

        if (hasToken1()) {
            if ((token1Id == null || token1Id.isBlank())) {
                throw new IllegalArgumentException("token0Id not specified");
            }
            this.token1Id = token1Id.trim();
        } else {
            this.token1Id = null;
        }
    }

    public String token0Id() {
        return token0Id;
    }

    public String token1Id() {
        return token1Id;
    }

    public void collectIds(final Collection<String> to) {
        if (hasToken0()) {
            to.add(token0Id());
        }
        if (hasToken1()) {
            to.add(token1Id());
        }
    }

    @Override
    public String toString() {
        return super.toString() + " [" +
                (hasToken0() ? token0Id() : "") + '/' +
                (hasToken1() ? token1Id() : "") + ']';
    }
}
