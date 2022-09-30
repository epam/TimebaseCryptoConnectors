package com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.uniswapsubgraph.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.PositionSnapshot;
import com.epam.deltix.data.uniswap.PositionSnapshotAction;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class PositionSnapshotSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("positionSnapshots");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("timestamp");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("owner");

        final GraphQlQuery.Object pool = QUERY_TEMPLATE.withObject("pool");
        pool.withScalar("id");

        QUERY_TEMPLATE.withScalar("position");
        QUERY_TEMPLATE.withScalar("blockNumber");
        QUERY_TEMPLATE.withScalar("timestamp");
        QUERY_TEMPLATE.withScalar("liquidity");
        QUERY_TEMPLATE.withScalar("depositedToken0");
        QUERY_TEMPLATE.withScalar("depositedToken1");
        QUERY_TEMPLATE.withScalar("withdrawnToken0");
        QUERY_TEMPLATE.withScalar("withdrawnToken1");
        QUERY_TEMPLATE.withScalar("collectedFeesToken0");
        QUERY_TEMPLATE.withScalar("collectedFeesToken1");

        final GraphQlQuery.Object transaction = QUERY_TEMPLATE.withObject("transaction");
        transaction.withScalar("id");

        QUERY_TEMPLATE.withScalar("feeGrowthInside0LastX128");
        QUERY_TEMPLATE.withScalar("feeGrowthInside1LastX128");
    }

    public PositionSnapshotSubscription(
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

        query.arguments().withWhere("timestamp_gte: \"" + now + "\"");

        return List.of(new UniswapCollectionPoller<>(
                uri,
                query,
                filter,
                PositionSnapshot::new,
                PositionSnapshotAction::new,
                messageOutput,
                false));
    }
}
