package com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.uniswapsubgraph.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.Flash;
import com.epam.deltix.data.uniswap.FlashAction;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class FlashSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("flashes");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("timestamp");
        QUERY_TEMPLATE.withScalar("id");

        final GraphQlQuery.Object trx = QUERY_TEMPLATE.withObject("transaction");
        trx.withScalar("id");

        QUERY_TEMPLATE.withScalar("timestamp");

        final GraphQlQuery.Object pool = QUERY_TEMPLATE.withObject("pool");
        pool.withScalar("id");

        QUERY_TEMPLATE.withScalar("sender");
        QUERY_TEMPLATE.withScalar("recipient");
        QUERY_TEMPLATE.withScalar("amount0");
        QUERY_TEMPLATE.withScalar("amount1");
        QUERY_TEMPLATE.withScalar("amountUSD");
        QUERY_TEMPLATE.withScalar("amount0Paid");
        QUERY_TEMPLATE.withScalar("amount1Paid");
        QUERY_TEMPLATE.withScalar("logIndex");
    }

    public FlashSubscription(
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
                Flash::new,
                FlashAction::new,
                messageOutput,
                false));
    }
}
