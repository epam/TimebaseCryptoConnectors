package com.epam.deltix.data.connectors.polygon;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public abstract class PolygonIoFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final String restUrl;
    private final String apiKey;

    private final PolygonIoEndpoint endpoint;

    protected Map<Long, String> exchanges = new HashMap<>();

    public PolygonIoFeed(
            final PolygonIoConnectorSettings settings,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("POLYGON",
            settings.getWsUrl(),
            1,
            20000,
            selected,
            output,
            errorListener,
            logger,
            symbols);

        this.restUrl = settings.getRestUrl();

        this.apiKey = settings.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Api key is not specified");
        }

        this.endpoint = PolygonIoEndpoint.typeFromAddress(settings.getWsUrl());
        if (endpoint == null) {
            throw new RuntimeException("Invalid wsUrl: " + settings.getWsUrl());
        }
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        exchanges = requestExchanges();

        JsonValue authJson = JsonValue.newObject();
        JsonObject authBody = authJson.asObject();
        authBody.putString("action", "auth");
        authBody.putString("params", apiKey);
        authJson.toJsonAndEoj(jsonWriter);
    }

    private Map<Long, String> requestExchanges() {
        Map<Long, String> idToExchange = new HashMap<>();
        if (restUrl == null || restUrl.isEmpty()) {
            return idToExchange;
        }

        try {
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder(
                URI.create(restUrl + "/v3/reference/exchanges?asset_class=stocks&apiKey=" + apiKey)
            ).GET().build();
            HttpResponse<String> exchangesResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = exchangesResponse.body();

            jsonParser.parse(responseBody);
            JsonValue jsonValue = jsonParser.eoj();
            JsonObject object = jsonValue.asObject();
            String status = object.getString("status");
            if ("OK".equalsIgnoreCase(status)) {
                JsonArray array = object.getArray("results");
                if (array != null) {
                    for (int i = 0; i < array.size(); ++i) {
                        JsonObject exchangeInfo = array.getObject(i);
                        long id = exchangeInfo.getLong("id");
                        String name = exchangeInfo.getString("name");
                        idToExchange.put(id, name);
                    }
                }
            }
        } catch (Throwable t) {
            logger().warning("Failed to request exchanges list", t);
        }

        return idToExchange;
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonArray array = jsonValue.asArray();

        processPolygonData(array);
    }

    protected void processStatusEvent(JsonObject obj) {
        String status = obj.getString("status");
        String message = obj.getString("message");
        logger().info(() -> "Status event: " + status + "; message: " + message);
    }

    protected abstract void processPolygonData(JsonArray array);

}
