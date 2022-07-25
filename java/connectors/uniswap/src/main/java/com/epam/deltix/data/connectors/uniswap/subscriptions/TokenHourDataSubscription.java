package com.epam.deltix.data.connectors.uniswap.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.uniswap.UniswapCollectionPoller;
import com.epam.deltix.data.uniswap.TokenDayData;
import com.epam.deltix.data.uniswap.TokenDayDataAction;
import com.epam.deltix.data.uniswap.TokenHourData;
import com.epam.deltix.data.uniswap.TokenHourDataAction;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TokenHourDataSubscription extends Subscription {
    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("tokenHourDatas");

    static {
        QUERY_TEMPLATE.arguments().withOrderBy("periodStartUnix");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("periodStartUnix");

        final GraphQlQuery.Object token = QUERY_TEMPLATE.withObject("token");
        token.withScalar("id");
        token.withScalar("symbol");

        QUERY_TEMPLATE.withScalar("volume");
        QUERY_TEMPLATE.withScalar("volumeUSD");
        QUERY_TEMPLATE.withScalar("untrackedVolumeUSD");
        QUERY_TEMPLATE.withScalar("totalValueLocked");
        QUERY_TEMPLATE.withScalar("totalValueLockedUSD");
        QUERY_TEMPLATE.withScalar("priceUSD");
        QUERY_TEMPLATE.withScalar("feesUSD");
        QUERY_TEMPLATE.withScalar("open");
        QUERY_TEMPLATE.withScalar("high");
        QUERY_TEMPLATE.withScalar("low");
        QUERY_TEMPLATE.withScalar("close");
    }

    public TokenHourDataSubscription(
            final String uri,
            final MessageOutput messageOutput,
            final Logger logger,
            final IdentifiedUniswapSymbol... symbols) {
        super(uri, messageOutput, logger, symbols);
    }

    @Override
    public Collection<HttpPoller> get() {
        long now = System.currentTimeMillis() / 1000;
        Predicate<JsonObject> filter = object -> true;

        if (symbols.length == 0) {
            return List.of(new UniswapCollectionPoller<>(
                    uri,
                    QUERY_TEMPLATE,
                    TokenDayData::new,
                    TokenDayDataAction::new,
                    messageOutput));
        }

        return Arrays.stream(IdentifiedUniswapSymbol.collectIds(symbols)).map(id -> {
            final GraphQlQuery.Query query = QUERY_TEMPLATE.copy();

            query.arguments().withWhere("token : \"" + id + "\", " +
                    "periodStartUnix_gte : " + now);

            return new UniswapCollectionPoller<>(
                    uri,
                    query,
                    filter,
                    TokenHourData::new,
                    TokenHourDataAction::new,
                    messageOutput,
                    false);
        }).collect(Collectors.toList());
    }
}
