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

    private final String apiKey;

    private final PolygonIoEndpoint endpoint;

    private final StringBuilder sb = new StringBuilder();

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
        JsonValue authJson = JsonValue.newObject();
        JsonObject authBody = authJson.asObject();
        authBody.putString("action", "auth");
        authBody.putString("params", apiKey);
        authJson.toJsonAndEoj(jsonWriter);
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

    protected String readConditions(JsonArray conditions) {
        if (conditions == null) {
            return null;
        }

        sb.setLength(0);
        for (int i = 0; i < conditions.size(); ++i) {
            if (i > 0) {
                sb.append(",");
            }

            sb.append(String.valueOf(conditions.getLong(i)));
        }

        return sb.toString();
    }
}
