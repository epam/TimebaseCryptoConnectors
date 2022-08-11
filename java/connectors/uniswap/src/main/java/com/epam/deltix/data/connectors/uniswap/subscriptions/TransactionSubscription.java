package com.epam.deltix.data.connectors.uniswap.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.uniswap.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.Transaction;
import com.epam.deltix.data.uniswap.TransactionAction;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class TransactionSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("transactions");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("timestamp");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("blockNumber");
        QUERY_TEMPLATE.withScalar("timestamp");
        QUERY_TEMPLATE.withScalar("gasUsed");
        QUERY_TEMPLATE.withScalar("gasPrice");

        final GraphQlQuery.Object mints = QUERY_TEMPLATE.withObject("mints");
        mints.withScalar("id");

        final GraphQlQuery.Object burns = QUERY_TEMPLATE.withObject("burns");
        burns.withScalar("id");

        final GraphQlQuery.Object swaps = QUERY_TEMPLATE.withObject("swaps");
        swaps.withScalar("id");

        final GraphQlQuery.Object collects = QUERY_TEMPLATE.withObject("collects");
        collects.withScalar("id");

        final GraphQlQuery.Object flashed = QUERY_TEMPLATE.withObject("flashed");
        flashed.withScalar("id");
    }

    public TransactionSubscription(
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
                Transaction::new,
                TransactionAction::new,
                messageOutput,
                false));
    }
}
