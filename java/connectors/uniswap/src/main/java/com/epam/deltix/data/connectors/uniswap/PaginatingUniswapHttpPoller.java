package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.GraphQlPagination;
import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.JsonObjectsListener;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Predicate;

public class PaginatingUniswapHttpPoller extends UniswapHttpPoller {
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final GraphQlPagination query;
    private final Predicate<JsonObject> objectFilter;
    private final JsonObjectsListener jsonListener;

    private boolean inObjectSequence;

    public PaginatingUniswapHttpPoller(
            final String graphQlUri,
            final GraphQlQuery.Query queryTemplate,
            final Predicate<JsonObject> objectFilter,
            final int pageSize,
            final JsonObjectsListener jsonListener) {
        this(graphQlUri, new GraphQlPagination(queryTemplate, pageSize), objectFilter, jsonListener);
    }

    public PaginatingUniswapHttpPoller(
            final String graphQlUri,
            final GraphQlPagination query,
            final Predicate<JsonObject> objectFilter,
            final JsonObjectsListener jsonListener) {
        super(graphQlUri);

        this.query = query;
        this.objectFilter = objectFilter;
        this.jsonListener = jsonListener;
    }

    @Override
    protected String firstRequestBody() {
        inObjectSequence = false;

        query.reset();

        final StringBuilder firstRequest = new StringBuilder();
        try {
            query.next().writeJson(firstRequest);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return firstRequest.toString();
    }

    @Override
    protected String nextRequestBody(final String response) {
        jsonParser.parseAndEoj(response);

        final JsonObject jsonValue = jsonParser.eoj().asObjectRequired();

        if (jsonValue.hasMember("error")) { // stop
            if (inObjectSequence) {
                jsonListener.onObjectsFinished();
            }
            return null;
        }

        final JsonObject data = jsonValue.getObject("data");

        if (data == null) {
            if (inObjectSequence) {
                jsonListener.onObjectsFinished();
            }
            return null;  // ??? TODO
        }

        data.forAnyArray((name, array) -> {
            if (!inObjectSequence) {
                jsonListener.onObjectsStarted();
                inObjectSequence = true;
            }
            array.items()
                    .map(item -> item.asObject())
                    .filter(Objects::nonNull)
                    .filter(objectFilter)
                    .forEach(jsonListener::onObject);
        });

        final StringBuilder nextRequest = new StringBuilder();
        try {
            query.next().writeJson(nextRequest);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return nextRequest.toString();
    }
}
