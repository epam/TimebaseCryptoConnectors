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
        QUERY_TEMPLATE.withScalar("createdAtTimestamp");
        QUERY_TEMPLATE.withScalar("createdAtBlockNumber");

        final GraphQlQuery.Object token0 = QUERY_TEMPLATE.withObject("token0");
        token0.withScalar("id");
        token0.withScalar("symbol");

        final GraphQlQuery.Object token1 = QUERY_TEMPLATE.withObject("token1");
        token1.withScalar("id");
        token1.withScalar("symbol");

        QUERY_TEMPLATE.withScalar("feeTier");
        QUERY_TEMPLATE.withScalar("liquidity");
        QUERY_TEMPLATE.withScalar("sqrtPrice");
        QUERY_TEMPLATE.withScalar("feeGrowthGlobal0X128");
        QUERY_TEMPLATE.withScalar("feeGrowthGlobal1X128");
        QUERY_TEMPLATE.withScalar("token0Price");
        QUERY_TEMPLATE.withScalar("token1Price");
        QUERY_TEMPLATE.withScalar("tick");
        QUERY_TEMPLATE.withScalar("observationIndex");
        QUERY_TEMPLATE.withScalar("volumeToken0");
        QUERY_TEMPLATE.withScalar("volumeToken1");
        QUERY_TEMPLATE.withScalar("volumeUSD");
        QUERY_TEMPLATE.withScalar("untrackedVolumeUSD");
        QUERY_TEMPLATE.withScalar("feesUSD");
        QUERY_TEMPLATE.withScalar("txCount");
        QUERY_TEMPLATE.withScalar("collectedFeesToken0");
        QUERY_TEMPLATE.withScalar("collectedFeesToken1");
        QUERY_TEMPLATE.withScalar("collectedFeesUSD");
        QUERY_TEMPLATE.withScalar("totalValueLockedToken0");
        QUERY_TEMPLATE.withScalar("totalValueLockedToken1");
        QUERY_TEMPLATE.withScalar("totalValueLockedETH");
        QUERY_TEMPLATE.withScalar("totalValueLockedUSD");
        QUERY_TEMPLATE.withScalar("totalValueLockedUSDUntracked");
        QUERY_TEMPLATE.withScalar("liquidityProviderCount");

        final GraphQlQuery.Object poolHourDataId = QUERY_TEMPLATE.withObject("poolHourData");
        poolHourDataId.withScalar("id");

        final GraphQlQuery.Object poolDayData = QUERY_TEMPLATE.withObject("poolDayData");
        poolDayData.withScalar("id");

        final GraphQlQuery.Object mints = QUERY_TEMPLATE.withObject("mints");
        mints.withScalar("id");

        final GraphQlQuery.Object burns = QUERY_TEMPLATE.withObject("burns");
        burns.withScalar("id");

        final GraphQlQuery.Object swaps = QUERY_TEMPLATE.withObject("swaps");
        swaps.withScalar("id");

        final GraphQlQuery.Object collects = QUERY_TEMPLATE.withObject("collects");
        collects.withScalar("id");

        final GraphQlQuery.Object ticks = QUERY_TEMPLATE.withObject("ticks");
        ticks.withScalar("id");

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
                    filter,
                    Pool::new,
                    PoolAction::new,
                    messageOutput);
        }).collect(Collectors.toList());
    }
}
