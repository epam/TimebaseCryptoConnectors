package com.epam.deltix.data.connectors.uniswap.subscriptions;

import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TokenIdentifier {
    public static Map<String, Info> identify(
            final String uri,
            final String... tokenSymbols) throws Exception {
        return new TokenIdentifier(uri).identify(tokenSymbols);
    }

    public class Info {
        private final String id;
        private final String symbol;
        private final String name;
        private final String txCount;

        private Info(final String id, final String symbol, final String name, final String txCount) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.txCount = txCount;
        }

        public String id() {
            return id;
        }

        public String symbol() {
            return symbol;
        }

        public String name() {
            return name;
        }

        public String txCount() {
            return txCount;
        }

        @Override
        public String toString() {
            return symbol + " [" + id + "] (" + name + ')';
        }
    }

    private static final GraphQlQuery.Query QUERY_TEMPLATE = GraphQlQuery.query("tokens");
    static {
        QUERY_TEMPLATE.arguments().withOrderBy("symbol");
        QUERY_TEMPLATE.withScalar("id");
        QUERY_TEMPLATE.withScalar("symbol");
        QUERY_TEMPLATE.withScalar("name");
        QUERY_TEMPLATE.withScalar("txCount");
    }

    private final String uri;

    public TokenIdentifier(final String uri) {
        this.uri = uri;
    }

    /**
     * Maps tokens' symbols to their ids.
     * @param tokenSymbols token symbols to map
     * @return the map 'symbol -&gt; id'
     * @throws Exception if any error happened, including HTTP response code was not 200
     */
    public Map<String, Info> identify(final String... tokenSymbols) throws Exception {
        if (tokenSymbols == null || tokenSymbols.length == 0) {
            return Map.of();
        }

        final GraphQlQuery.Query query = QUERY_TEMPLATE.copy();
        query.arguments().withWhere(
                "symbol_in : [" +
                        Arrays.stream(tokenSymbols)
                                .map(s -> '"' + s + '"')
                                .collect(Collectors.joining(" ,"))
                        + ']');

        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(query.toJson()))
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        final int statusCode = response.statusCode();
        if (statusCode != 200) {
            throw new IOException("Unexpected status code of the response: " + statusCode);
        }

        final String responseBody = response.body();
        if (responseBody == null) {
            return Map.of();
        }

        final JsonObject root = new JsonValueParser().parseAndEoj(responseBody).asObjectRequired();
        final JsonObject data = root.getObjectRequired("data");
        final JsonArray tokens = data.getArrayRequired("tokens");

        final Map<String, Info> result = new HashMap<>();
        tokens.items()
                .map(JsonValue::asObject)
                .filter(Objects::nonNull)
                .forEach(object -> {
                    final String id = object.getStringRequired("id");
                    final String symbol = object.getStringRequired("symbol");
                    final String name = object.getStringRequired("name");
                    final String txCount = object.getStringRequired("txCount");

                    final Info info = result.get(symbol);
                    final Info newInfo = new Info(id, symbol, name, txCount);

                    if (info != null && Integer.parseInt(info.txCount()) > Integer.parseInt(txCount)) {
                        System.out.println("Symbol " +
                                info + " was found. Skipping the duplicating one " + newInfo); // TODO: logging
                        return;
                    }

                    result.put(symbol, newInfo);
                });
        return result;
    }
}
