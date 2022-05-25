package com.epam.deltix.data.connectors.commons;

public class GraphQlPagination {
    private final GraphQlQuery.Query query;
    private final int pageSize;

    private int skip;

    public GraphQlPagination(final GraphQlQuery.Query template, final int pageSize) {
        this.query = template;
        this.pageSize = pageSize;

        query.arguments().withFirst(pageSize);
    }

    public void reset() {
        skip = 0;
        query.arguments().withNoSkip();
    }

    public GraphQlQuery.Query next() {
        if (skip > 0) {
            query.arguments().withSkip(skip);
        }

        skip += pageSize;

        return query;
    }
}
