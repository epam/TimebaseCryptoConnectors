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

public class UniswapDayDataSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("uniswapDayDatas");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("date");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("date");
        QUERY_TEMPLATE.withScalar("volumeETH");
        QUERY_TEMPLATE.withScalar("volumeUSD");
        QUERY_TEMPLATE.withScalar("volumeUSDUntracked");
        QUERY_TEMPLATE.withScalar("feesUSD");
        QUERY_TEMPLATE.withScalar("txCount");
        QUERY_TEMPLATE.withScalar("tvlUSD");
    }

    public UniswapDayDataSubscription(
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

        query.arguments().withWhere("date_gte: " + now);

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
