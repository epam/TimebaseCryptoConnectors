package com.epam.deltix.data.connectors.uniswap.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.uniswap.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.Factory;
import com.epam.deltix.data.uniswap.FactoryAction;

import java.util.Collection;
import java.util.List;

public class FactorySubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("factories");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("id");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("poolCount");
        QUERY_TEMPLATE.withScalar("txCount");
        QUERY_TEMPLATE.withScalar("totalVolumeUSD");
    }

    public FactorySubscription(
            final String uri,
            final MessageOutput messageOutput,
            final Logger logger,
            final IdentifiedUniswapSymbol... symbols) {
        super(uri, messageOutput, logger, symbols);
    }

    @Override
    public Collection<HttpPoller> get() {
        return List.of(new UniswapCollectionPoller<>(
                uri,
                QUERY_TEMPLATE,
                Factory::new,
                FactoryAction::new,
                messageOutput));
    }
}