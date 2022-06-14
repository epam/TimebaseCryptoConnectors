package com.epam.deltix.data.connectors.uniswap.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.uniswap.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.Token;
import com.epam.deltix.data.uniswap.TokenAction;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TokenSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("tokens");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("id");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("symbol");
        QUERY_TEMPLATE.withScalar("name");
        QUERY_TEMPLATE.withScalar("decimals");
        QUERY_TEMPLATE.withScalar("totalSupply");
        QUERY_TEMPLATE.withScalar("volume");
        QUERY_TEMPLATE.withScalar("volumeUSD");
        QUERY_TEMPLATE.withScalar("untrackedVolumeUSD");
        QUERY_TEMPLATE.withScalar("feesUSD");
        QUERY_TEMPLATE.withScalar("txCount");
        QUERY_TEMPLATE.withScalar("poolCount");
        QUERY_TEMPLATE.withScalar("totalValueLocked");
        QUERY_TEMPLATE.withScalar("totalValueLockedUSD");
        QUERY_TEMPLATE.withScalar("totalValueLockedUSDUntracked");
        QUERY_TEMPLATE.withScalar("derivedETH");
    }

    public TokenSubscription(
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
                    Token::new,
                    TokenAction::new,
                    messageOutput));
        }

        return Arrays.stream(IdentifiedUniswapSymbol.collectIds(symbols)).map(id -> {
            final GraphQlQuery.Query query = QUERY_TEMPLATE.copy();

            query.arguments().withWhere("id : \"" + id  + "\"");

            return new UniswapCollectionPoller<>(
                    uri,
                    query,
                    Token::new,
                    TokenAction::new,
                    messageOutput);
        }).collect(Collectors.toList());
    }
}