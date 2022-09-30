package com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.uniswapsubgraph.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.UniswapDayData;
import com.epam.deltix.data.uniswap.UniswapDayDataAction;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class TickSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("ticks");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("createdAtTimestamp");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("poolAddress");
        QUERY_TEMPLATE.withScalar("tickIdx");

        final GraphQlQuery.Object pool = QUERY_TEMPLATE.withObject("pool");
        pool.withScalar("id");

        QUERY_TEMPLATE.withScalar("liquidityGross");
        QUERY_TEMPLATE.withScalar("liquidityNet");
        QUERY_TEMPLATE.withScalar("price0");
        QUERY_TEMPLATE.withScalar("price1");
        QUERY_TEMPLATE.withScalar("volumeToken0");
        QUERY_TEMPLATE.withScalar("volumeToken1");
        QUERY_TEMPLATE.withScalar("volumeUSD");
        QUERY_TEMPLATE.withScalar("untrackedVolumeUSD");
        QUERY_TEMPLATE.withScalar("feesUSD");
        QUERY_TEMPLATE.withScalar("collectedFeesToken0");
        QUERY_TEMPLATE.withScalar("collectedFeesToken1");
        QUERY_TEMPLATE.withScalar("collectedFeesUSD");
        QUERY_TEMPLATE.withScalar("createdAtTimestamp");
        QUERY_TEMPLATE.withScalar("createdAtBlockNumber");
        QUERY_TEMPLATE.withScalar("liquidityProviderCount");
        QUERY_TEMPLATE.withScalar("feeGrowthOutside0X128");
        QUERY_TEMPLATE.withScalar("feeGrowthOutside1X128");
    }

    public TickSubscription(
            final String uri,
            final MessageOutput messageOutput,
            final Logger logger,
            final IdentifiedUniswapSymbol... symbols) {
        super(uri, messageOutput, logger, symbols);
    }

    @Override
    public Collection<HttpPoller> get() {
        long now = System.currentTimeMillis() / 1000;

        final GraphQlQuery.Query query = QUERY_TEMPLATE.copy();
        Predicate<JsonObject> filter = object -> true;

        query.arguments().withWhere("createdAtTimestamp: " + "\"" + now + "\"");

        return List.of(new UniswapCollectionPoller<>(
                uri,
                query,
                filter,
                UniswapDayData::new,
                UniswapDayDataAction::new,
                messageOutput,
                false));
        }
}
