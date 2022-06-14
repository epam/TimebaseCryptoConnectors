package com.epam.deltix.data.connectors.uniswap.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.uniswap.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.Pool;
import com.epam.deltix.data.uniswap.PoolAction;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PoolSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("pools");
    static {
        QUERY_TEMPLATE.arguments().withOrderBy("id");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("liquidity");
        QUERY_TEMPLATE.withScalar("liquidityProviderCount");
        final GraphQlQuery.Object token0 = QUERY_TEMPLATE.withObject("token0");
        token0.withScalar("id");
        token0.withScalar("symbol");
        final GraphQlQuery.Object token1 = QUERY_TEMPLATE.withObject("token1");
        token1.withScalar("id");
        token1.withScalar("symbol");
    }

    public PoolSubscription(
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
                    Pool::new,
                    PoolAction::new,
                    messageOutput));
        }

        return Arrays.stream(symbols).map(symbol -> {
            final GraphQlQuery.Query query = QUERY_TEMPLATE.copy();
            Predicate<JsonObject> filter = object -> true;

            if (symbol.hasToken0()) {
                query.arguments().withWhere("token0 : \"" + symbol.token0Id()  + "\"");
                if (symbol.hasToken1()) {
                    filter = object -> {
                        final JsonObject token1 = object.getObjectRequired("token1");
                        return symbol.token1Id().equals(token1.getStringRequired("id"));
                    };
                }
            } else {
                assert symbol.hasToken1();
                query.arguments().withWhere("token1 : \"" + symbol.token1Id()  + "\"");
            }

            return new UniswapCollectionPoller<>(
                    uri,
                    query,
                    filter,
                    Pool::new,
                    PoolAction::new,
                    messageOutput);
        }).collect(Collectors.toList());
    }
}
