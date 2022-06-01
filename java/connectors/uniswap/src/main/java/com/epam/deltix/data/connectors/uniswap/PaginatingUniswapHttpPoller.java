package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.GraphQlPagination;
import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.JsonObjectsListener;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;

public class PaginatingUniswapHttpPoller extends UniswapHttpPoller {
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final GraphQlPagination query;
    private final JsonObjectsListener jsonListener;

    private boolean inObjectSequence;

    public PaginatingUniswapHttpPoller(
            final String graphQlUri,
            final GraphQlQuery.Query query,
            final int pageSize,
            final JsonObjectsListener jsonListener) throws URISyntaxException {
        this(graphQlUri, new GraphQlPagination(query, pageSize), jsonListener);
    }

    public PaginatingUniswapHttpPoller(
            final String graphQlUri,
            final GraphQlPagination query,
            final JsonObjectsListener jsonListener) throws URISyntaxException {
        super(graphQlUri);

        this.query = query;
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
                    .filter(item -> item.asObject() != null)
                    .forEach(item -> jsonListener.onObject(item.asObject()));
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
