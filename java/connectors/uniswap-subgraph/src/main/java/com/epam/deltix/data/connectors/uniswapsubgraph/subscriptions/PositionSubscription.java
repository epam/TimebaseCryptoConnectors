package com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.uniswapsubgraph.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.Position;
import com.epam.deltix.data.uniswap.PositionAction;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PositionSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("positions");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("id");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("owner");
        QUERY_TEMPLATE.withScalar("liquidity");
        QUERY_TEMPLATE.withScalar("depositedToken0");
        QUERY_TEMPLATE.withScalar("depositedToken1");
        QUERY_TEMPLATE.withScalar("withdrawnToken0");
        QUERY_TEMPLATE.withScalar("withdrawnToken1");
        QUERY_TEMPLATE.withScalar("collectedFeesToken0");
        QUERY_TEMPLATE.withScalar("collectedFeesToken1");
        QUERY_TEMPLATE.withScalar("feeGrowthInside0LastX128");
        QUERY_TEMPLATE.withScalar("feeGrowthInside1LastX128");

        final GraphQlQuery.Object token0 = QUERY_TEMPLATE.withObject("token0");
        token0.withScalar("id");
        token0.withScalar("symbol");

        final GraphQlQuery.Object token1 = QUERY_TEMPLATE.withObject("token1");
        token1.withScalar("id");
        token1.withScalar("symbol");

        final GraphQlQuery.Object pool = QUERY_TEMPLATE.withObject("pool");
        pool.withScalar("id");

        final GraphQlQuery.Object transaction = QUERY_TEMPLATE.withObject("transaction");
        transaction.withScalar("id");
    }

    public PositionSubscription(
            final String uri,
            final MessageOutput messageOutput,
            final Logger logger,
            final IdentifiedUniswapSymbol... symbols) {
        super(uri, messageOutput, logger, symbols);
    }

    @Override
    public Collection<HttpPoller> get() {
        if (symbols.length == 0) {
            return List.of(new UniswapCollectionPoller<>(
                    uri,
                    QUERY_TEMPLATE,
                    Position::new,
                    PositionAction::new,
                    messageOutput));
        }

        return Arrays.stream(symbols).map(symbol -> {
            final GraphQlQuery.Query query = QUERY_TEMPLATE.copy();

            if (symbol.hasToken0() && symbol.hasToken1()) {
                query.arguments().withWhere("token0: \"" + symbol.token0Id() + "\", " +
                        "token1: \"" + symbol.token1Id() + "\"");
            } else if (symbol.hasToken0()) {
                query.arguments().withWhere("token0: \"" + symbol.token0Id() + "\"");
            } else if (symbol.hasToken1()) {
                query.arguments().withWhere("token1: \"" + symbol.token1Id() + "\"");
            }

            return new UniswapCollectionPoller<>(
                    uri,
                    query,
                    Position::new,
                    PositionAction::new,
                    messageOutput);
        }).collect(Collectors.toList());
    }
}
